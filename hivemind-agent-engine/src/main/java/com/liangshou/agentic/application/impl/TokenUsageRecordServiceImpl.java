package com.liangshou.agentic.application.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liangshou.agentic.application.ITokenUsageRecordService;
import com.liangshou.agentic.application.dto.SessionTokenUsageDTO;
import com.liangshou.agentic.infrastructure.mysql.po.TokenUsagePO;
import com.liangshou.agentic.infrastructure.mysql.support.TokenPricing;
import com.liangshou.agentic.infrastructure.mysql.support.TokenUsageSupport;
import com.liangshou.agentic.infrastructure.mysql.support.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import io.agentscope.core.model.ChatUsage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Token 使用量记录与查询服务实现
 */
@Service
@Slf4j
public class TokenUsageRecordServiceImpl implements ITokenUsageRecordService {

    private final TokenUsageSupport tokenUsageSupport;
    private final ObjectMapper objectMapper;
    private final TokenPricing tokenPricing;

    public TokenUsageRecordServiceImpl(TokenUsageSupport tokenUsageSupport,
                                       ObjectMapper objectMapper,
                                       TokenPricing tokenPricing) {
        this.tokenUsageSupport = tokenUsageSupport;
        this.objectMapper = objectMapper;
        this.tokenPricing = tokenPricing;
    }

    @Override
    public void record(String userId, String sessionId, String messageId,
                       String modelConfig, Map<String, Object> metadata) {
        try {
            // 1. 解析 metadata 中的 _chat_usage
            ChatUsageData chatUsage = extractChatUsage(metadata);
            if (chatUsage == null) {
                log.debug("[TokenUsage] 未找到 _chat_usage 信息，跳过记录 - messageId: {}", messageId);
                return;
            }

            // 2. 解析模型配置
            ModelInfo modelInfo = parseModelConfig(modelConfig);

            // 3. 构建 PO 并保存
            TokenUsagePO po = TokenUsagePO.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .messageId(messageId)
                    .modelProvider(modelInfo.provider())
                    .modelName(modelInfo.modelName())
                    .inputTokens(chatUsage.inputTokens())
                    .outputTokens(chatUsage.outputTokens())
                    .totalTokens(chatUsage.totalTokens())
                    .cachedTokens(chatUsage.cachedTokens())
                    .usageTime(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();

            tokenUsageSupport.save(po);
            log.info("[TokenUsage] 记录成功 - userId: {}, model: {}:{}, tokens: {}",
                    userId, modelInfo.provider(), modelInfo.modelName(), chatUsage.totalTokens());

        } catch (Exception e) {
            // 记录失败不影响主流程，仅记录日志
            log.error("[TokenUsage] 记录失败 - userId: {}, sessionId: {}, error: {}",
                    userId, sessionId, e.getMessage(), e);
        }
    }

    @Override
    public TokenUsageSummaryDTO getSummary(String userId, LocalDate startDate, LocalDate endDate) {
        TokenUsageSummaryDTO dto = tokenUsageSupport.getSummary(userId, startDate, endDate);

        // 汇总费用按各模型单价累加（汇总行没有单一模型，需逐模型计算后求和）
        BigDecimal totalCost = BigDecimal.ZERO;
        for (TokenUsageByModelDTO byModel : tokenUsageSupport.getByModel(userId, startDate, endDate)) {
            totalCost = totalCost.add(costOf(byModel.getModelProvider(), byModel.getModelName(),
                    byModel.getInputTokens(), byModel.getOutputTokens()));
        }
        dto.setEstimatedCost(totalCost.setScale(2, RoundingMode.HALF_UP));
        return dto;
    }

    @Override
    public List<TokenUsageByModelDTO> getByModel(String userId, LocalDate startDate, LocalDate endDate) {
        List<TokenUsageByModelDTO> list = tokenUsageSupport.getByModel(userId, startDate, endDate);
        for (TokenUsageByModelDTO dto : list) {
            dto.setEstimatedCost(costOf(dto.getModelProvider(), dto.getModelName(),
                    dto.getInputTokens(), dto.getOutputTokens()));
        }
        return list;
    }

    @Override
    public List<TokenUsageByProviderDTO> getByProvider(String userId, LocalDate startDate, LocalDate endDate) {
        return tokenUsageSupport.getByProvider(userId, startDate, endDate);
    }

    @Override
    public List<TokenUsageByDateDTO> getByDate(String userId, LocalDate startDate, LocalDate endDate) {
        return tokenUsageSupport.getByDate(userId, startDate, endDate);
    }

    @Override
    public SessionTokenUsageDTO summarizeSession(String userId, String sessionId) {
        try {
            List<TokenUsagePO> records = tokenUsageSupport.lambdaQuery()
                    .eq(TokenUsagePO::getUserId, userId)
                    .eq(TokenUsagePO::getSessionId, sessionId)
                    .list();
            long input = 0L;
            long output = 0L;
            long total = 0L;
            long cached = 0L;
            for (TokenUsagePO po : records) {
                input += po.getInputTokens() == null ? 0L : po.getInputTokens();
                output += po.getOutputTokens() == null ? 0L : po.getOutputTokens();
                total += po.getTotalTokens() == null ? 0L : po.getTotalTokens();
                cached += po.getCachedTokens() == null ? 0L : po.getCachedTokens();
            }
            return SessionTokenUsageDTO.builder()
                    .calls(records.size())
                    .inputTokens(input)
                    .outputTokens(output)
                    .totalTokens(total)
                    .cachedTokens(cached)
                    .build();
        } catch (Exception e) {
            log.warn("[TokenUsage] 汇总会话 Token 用量失败 - userId: {}, sessionId: {}, error: {}",
                    userId, sessionId, e.getMessage());
            return SessionTokenUsageDTO.builder().build();
        }
    }

    /**
     * 计算一组 Token 的估算费用。
     */
    private BigDecimal costOf(String provider, String model, Long inputTokens, Long outputTokens) {
        return tokenPricing.estimate(provider, model,
                inputTokens == null ? 0L : inputTokens,
                outputTokens == null ? 0L : outputTokens);
    }

    /**
     * 从 metadata 中提取 _chat_usage
     */
    private ChatUsageData extractChatUsage(Map<String, Object> metadata) {
        if (metadata == null || !metadata.containsKey("_chat_usage")) {
            return null;
        }

        Object usageObj = metadata.get("_chat_usage");

        // 情况 1: AgentScope 的 ChatUsage 对象
        if (usageObj instanceof ChatUsage) {
            ChatUsage chatUsage = (ChatUsage) usageObj;
            // ChatUsage 只有 inputTokens, outputTokens, totalTokens 三个字段
            return new ChatUsageData(
                    chatUsage.getInputTokens(),
                    chatUsage.getOutputTokens(),
                    chatUsage.getTotalTokens(),
                    0  // cachedTokens 不在 ChatUsage 中
            );
        }

        // 情况 2: Map 结构（降级兼容）
        if (usageObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> usage = (Map<String, Object>) usageObj;
            return new ChatUsageData(
                    getIntValue(usage, "inputTokens", 0),
                    getIntValue(usage, "outputTokens", 0),
                    getIntValue(usage, "totalTokens", 0),
                    getIntValue(usage, "cachedTokens", 0)
            );
        }

        log.warn("[TokenUsage] 未知的 _chat_usage 类型: {}", usageObj.getClass().getName());
        return null;
    }

    /**
     * 解析模型配置 JSON
     */
    private ModelInfo parseModelConfig(String modelConfigJson) {
        try {
            JsonNode node = objectMapper.readTree(modelConfigJson);
            String provider = node.has("provider") ? node.get("provider").asText() : "unknown";
            String modelName = node.has("modelName") ? node.get("modelName").asText() : "unknown";
            
            return new ModelInfo(provider, modelName);
        } catch (JsonProcessingException e) {
            log.warn("[TokenUsage] 解析模型配置失败: {}", e.getMessage());
            return new ModelInfo("unknown", "unknown");
        }
    }

    private Integer getIntValue(Map<String, Object> map, String key, Integer defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }

    /**
     * Chat 使用量记录（内部数据传输）
     */
    private record ChatUsageData(int inputTokens, int outputTokens, int totalTokens, int cachedTokens) {
    }

    /**
     * 模型信息
     */
    private record ModelInfo(String provider, String modelName) {
    }
}
