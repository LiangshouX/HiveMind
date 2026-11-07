package com.liangshou.agentic.application.impl;

import com.liangshou.agentic.agents.ConversationSessionContext;
import com.liangshou.agentic.agents.memory.TdAgentMemoryManager;
import com.liangshou.agentic.agents.memory.compaction.CompactionResult;
import com.liangshou.agentic.agents.memory.compaction.ContextWindowManager;
import com.liangshou.agentic.agents.memory.compaction.TokenMeter;
import com.liangshou.agentic.agents.session.AgentSessionStateService;
import com.liangshou.agentic.application.IContextUsageService;
import com.liangshou.agentic.application.IConversationPersistenceService;
import com.liangshou.agentic.application.ITokenUsageRecordService;
import com.liangshou.agentic.application.dto.CompactContextResponse;
import com.liangshou.agentic.application.dto.SessionContextUsageResponse;
import com.liangshou.agentic.application.dto.SessionTokenUsageDTO;
import com.liangshou.agentic.common.util.MessageMapper;
import com.liangshou.agentic.domain.memory.model.ConversationContextStats;
import com.liangshou.agentic.domain.memory.model.ConversationMemoryDocument;
import com.liangshou.agentic.domain.memory.model.StoredMessage;
import com.liangshou.agentic.domain.memory.model.StoredMessageContent;
import io.agentscope.core.message.Msg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 会话上下文用量服务实现。
 *
 * <p>统计口径与压缩器保持一致：使用同一套 {@link TokenMeter} 估算，
 * 避免"界面显示 30% 但压缩器认为已满"的割裂。</p>
 *
 * @author LiangshouX
 */
@Service
public class ContextUsageServiceImpl implements IContextUsageService {

    private static final Logger log = LoggerFactory.getLogger(ContextUsageServiceImpl.class);

    /**
     * "临近阈值"判定比例：进入压缩阈值的最后 15% 区间即视为临近。
     */
    private static final double NEAR_THRESHOLD_RATIO = 0.85;

    private final IConversationPersistenceService persistenceService;
    private final TokenMeter tokenMeter;
    private final MessageMapper messageMapper;
    private final ContextWindowManager windowManager;
    private final AgentSessionStateService agentSessionStateService;
    private final TdAgentMemoryManager memoryManager;
    private final ITokenUsageRecordService tokenUsageRecordService;

    public ContextUsageServiceImpl(
            IConversationPersistenceService persistenceService,
            TokenMeter tokenMeter,
            MessageMapper messageMapper,
            ContextWindowManager windowManager,
            AgentSessionStateService agentSessionStateService,
            TdAgentMemoryManager memoryManager,
            ITokenUsageRecordService tokenUsageRecordService) {
        this.persistenceService = persistenceService;
        this.tokenMeter = tokenMeter;
        this.messageMapper = messageMapper;
        this.windowManager = windowManager;
        this.agentSessionStateService = agentSessionStateService;
        this.memoryManager = memoryManager;
        this.tokenUsageRecordService = tokenUsageRecordService;
    }

    @Override
    public SessionContextUsageResponse getUsage(String userId, String sessionId) {
        ConversationMemoryDocument document =
                persistenceService.getSessionHistory(userId, sessionId).orElse(null);
        List<StoredMessage> messages =
                document == null || document.getMessages() == null ? List.of() : document.getMessages();
        String summary = document == null || document.getCompressedSummary() == null
                ? "" : document.getCompressedSummary();

        ConversationSessionContext context = buildContext(userId, sessionId);

        // 过滤历史遗留的系统消息（Agent sysPrompt 每轮重建，不属于对话历史）
        List<StoredMessage> conversationMessages = messages.stream()
                .filter(msg -> !isFrameworkSystemMessage(msg))
                .toList();

        // 系统提示词固定部分每次调用都占用上下文窗口，计入总占用
        int systemPromptTokens = memoryManager.systemPromptOverheadTokens(context);

        // 工具定义（Tool Schema）同样每次请求都会送入模型
        ConversationContextStats stats = document == null ? null : document.getContextStats();
        int toolDefinitionTokens = stats == null || stats.getToolDefinitionTokens() == null
                ? 0 : stats.getToolDefinitionTokens();

        int conversationTokens = countTokens(conversationMessages) + tokenMeter.countTokens(summary);
        int usedTokens = conversationTokens + systemPromptTokens + toolDefinitionTokens;
        int reasoningCount = countType(conversationMessages, "thinking");
        int toolCallCount = countType(conversationMessages, "tool_use");

        int contextWindow = windowManager.getContextWindowSize(context);
        int thresholdTokens = windowManager.getCompactionThreshold(context);
        double thresholdRatio = windowManager.getThresholdRatio();
        double usageRatio = contextWindow > 0 ? Math.min(1.0, (double) usedTokens / contextWindow) : 0.0;

        // 会话维度的 Token 用量（缓存命中 / 未缓存输入 / 缓存读取 / 输出）
        SessionTokenUsageDTO tokenUsage =
                tokenUsageRecordService.summarizeSession(userId, sessionId);

        return SessionContextUsageResponse.builder()
                .usedTokens(usedTokens)
                .contextWindow(contextWindow)
                .usageRatio(usageRatio)
                .thresholdRatio(thresholdRatio)
                .thresholdTokens(thresholdTokens)
                .outputReserveTokens(windowManager.getOutputReserve())
                .systemPromptTokens(systemPromptTokens)
                .toolDefinitionTokens(toolDefinitionTokens)
                .conversationTokens(conversationTokens)
                .messageCount(conversationMessages.size())
                .reasoningCount(reasoningCount)
                .toolCallCount(toolCallCount)
                .compactionCount(document == null || document.getCompactionCount() == null
                        ? 0L : document.getCompactionCount())
                .compressedSummaryChars(summary.length())
                .providerId(context.getProviderId())
                .modelId(context.getModelId())
                .modelDurationMs(stats == null || stats.getModelDurationMs() == null
                        ? 0L : stats.getModelDurationMs())
                .ttftMs(stats == null || stats.getTtftMs() == null ? 0L : stats.getTtftMs())
                .tokensPerSecond(stats == null || stats.getTokensPerSecond() == null
                        ? 0d : stats.getTokensPerSecond())
                .cacheHitRatio(tokenUsage.cacheHitRatio())
                .uncachedInputTokens(tokenUsage.uncachedInputTokens())
                .cachedInputTokens(tokenUsage.getCachedTokens())
                .outputTokens(tokenUsage.getOutputTokens())
                .modelCalls(tokenUsage.getCalls())
                .modelDisplayName(context.getModelId())
                .nearThreshold(isNearThreshold(usedTokens, thresholdTokens))
                .build();
    }

    /**
     * 判断是否为框架注入的系统消息（Agent sysPrompt）。
     */
    private boolean isFrameworkSystemMessage(StoredMessage message) {
        return message != null
                && "SYSTEM".equalsIgnoreCase(message.getRole())
                && "system".equals(message.getName());
    }

    /**
     * 判断是否"临近"压缩阈值。
     *
     * <p>与真正的触发阈值区分开：达到阈值会立即自动压缩，
     * 而临近阈值只用于界面提示，让用户有机会手动触发压缩。</p>
     */
    private boolean isNearThreshold(int usedTokens, int thresholdTokens) {
        if (thresholdTokens <= 0) {
            return false;
        }
        // 进入阈值前 15% 的区间即视为"临近"
        int nearBoundary = (int) (thresholdTokens * NEAR_THRESHOLD_RATIO);
        return usedTokens >= nearBoundary;
    }

    @Override
    public CompactContextResponse compact(String userId, String sessionId) {
        ConversationSessionContext context = buildContext(userId, sessionId);
        CompactionResult result = memoryManager.forceCompact(context, persistenceService);
        if (result == null) {
            return CompactContextResponse.builder()
                    .compacted(false)
                    .summary("当前会话消息数过少，无需压缩。")
                    .build();
        }
        String summary = String.format(
                "压缩完成：消息 %d → %d（压缩 %d 条），tokens %d → %d（减少 %.1f%%），策略 %s，耗时 %dms",
                result.getMessagesBefore(),
                result.getMessagesAfter(),
                result.getMiddleCompressed(),
                result.getTokensBefore(),
                result.getTokensAfter(),
                result.compressionRatio() * 100,
                result.getStrategy(),
                result.getDurationMs());
        log.info("[ContextUsageService] 手动压缩完成 - sessionId: {}, {}", sessionId, summary);
        return CompactContextResponse.builder()
                .compacted(true)
                .tokensBefore(result.getTokensBefore())
                .tokensAfter(result.getTokensAfter())
                .messagesBefore(result.getMessagesBefore())
                .messagesAfter(result.getMessagesAfter())
                .middleCompressed(result.getMiddleCompressed())
                .strategy(result.getStrategy())
                .durationMs(result.getDurationMs())
                .compressionRatio(result.compressionRatio())
                .summary(summary)
                .build();
    }

    /**
     * 构建会话上下文（带上会话已绑定的供应商/模型，用于解析上下文窗口）。
     */
    private ConversationSessionContext buildContext(String userId, String sessionId) {
        ConversationSessionContext base = ConversationSessionContext.builder()
                .userId(userId)
                .sessionId(sessionId)
                .build();
        return ConversationSessionContext.builder()
                .userId(userId)
                .sessionId(sessionId)
                .providerId(agentSessionStateService.getStoredProviderId(base))
                .modelId(agentSessionStateService.getStoredModelId(base))
                .build();
    }

    /**
     * 统计消息占用的 token 数，优先按 AgentScope 消息结构计量，失败时回退为原始文本计量。
     */
    private int countTokens(List<StoredMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        List<Msg> converted = new ArrayList<>(messages.size());
        for (StoredMessage stored : messages) {
            try {
                converted.add(messageMapper.toMsg(stored));
            } catch (Exception ignored) {
                // 忽略无法转换的消息，回退到原始文本计量
            }
        }
        if (!converted.isEmpty()) {
            return tokenMeter.countTotalTokens(converted);
        }
        int total = 0;
        for (StoredMessage stored : messages) {
            total += countRawTokens(stored);
        }
        return total;
    }

    private int countRawTokens(StoredMessage message) {
        int tokens = 0;
        if (message.getContent() == null) {
            return tokens;
        }
        for (StoredMessageContent content : message.getContent()) {
            tokens += tokenMeter.countTokens(content.getText());
            tokens += tokenMeter.countTokens(content.getName());
            tokens += tokenMeter.countTokens(content.getInputRaw());
            if (content.getInputRaw() == null) {
                tokens += tokenMeter.countTokens(content.getInput());
            }
        }
        return tokens;
    }

    /**
     * 统计指定类型的消息块数量。
     */
    private int countType(List<StoredMessage> messages, String type) {
        if (messages == null) {
            return 0;
        }
        int count = 0;
        for (StoredMessage message : messages) {
            if (message.getContent() == null) {
                continue;
            }
            for (StoredMessageContent content : message.getContent()) {
                if (type.equals(content.getType())) {
                    count++;
                }
            }
        }
        return count;
    }
}
