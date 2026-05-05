package com.liangshou.tangdynasty.agentic.agents.guard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liangshou.tangdynasty.agentic.application.dto.ToolConfigDTO;
import com.liangshou.tangdynasty.agentic.common.config.TdAgentProperties;
import com.liangshou.tangdynasty.agentic.common.util.ToolConfigProvider;
import com.liangshou.tangdynasty.agentic.domain.shared.enums.ToolRiskLevel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 工具防护引擎 - 评估 Agent 工具调用的安全风险并生成决策。
 *
 * <p>该引擎通过以下规则对工具调用进行安全评估：</p>
 * <ul>
 *     <li><b>工具启用检查</b>：检查工具是否被用户禁用，禁用则直接拒绝</li>
 *     <li><b>危险命令检测</b>：检查工具输入中是否包含用户配置的拒绝模式，命中则直接拒绝</li>
 *     <li><b>高风险工具识别</b>：根据用户配置的 approvalRequired 决定是否需要人工审批</li>
 *     <li><b>默认放行</b>：未命中任何风险规则的普通工具调用，标记为低风险并允许执行</li>
 * </ul>
 *
 * <p>评估结果以 {@link ToolGuardDecision} 形式返回，包含风险等级、决策原因和命中的模式标识。</p>
 *
 * @author LiangshouX
 */
@Component
public class ToolGuardEngine {

    private static final List<String> SENSITIVE_PATH_PATTERNS =
            List.of(".env", ".git", "id_rsa", "known_hosts", "pom.xml", "application.yml");

    private final TdAgentProperties properties;
    private final ObjectMapper objectMapper;
    private final ToolConfigProvider configProvider;

    /**
     * 构造器
     *
     * @param properties      外部化配置
     * @param objectMapper    ObjectMapper
     * @param configProvider  工具配置提供者
     */
    public ToolGuardEngine(TdAgentProperties properties, ObjectMapper objectMapper, ToolConfigProvider configProvider) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.configProvider = configProvider;
    }

    /**
     * 执行工具调用评估。
     *
     * @param toolName 工具名称
     * @param input    工具输入
     * @param userId   用户唯一标识
     * @return 评估结果
     */
    public ToolGuardDecision evaluate(String toolName, Map<String, Object> input, String userId) {
        if (!properties.getToolGuard().isEnabled()) {
            return allow(toolName, ToolRiskLevel.LOW, "Tool guard disabled.");
        }

        // 1. 获取用户工具配置
        ToolConfigDTO config = configProvider.getConfig(toolName, userId);

        // 2. 检查启用状态
        if (config != null && !config.getEnabled()) {
            return ToolGuardDecision.builder()
                    .toolName(toolName)
                    .allowed(false)
                    .requiresApproval(false)
                    .riskLevel(ToolRiskLevel.CRITICAL)
                    .reason("工具已禁用：" + toolName)
                    .matchedPattern("disabled")
                    .build();
        }

        // 3. 危险命令检测（使用用户配置的 denyPatterns）
        String normalized = stringify(input).toLowerCase(Locale.ROOT);
        List<String> denyPatterns = (config != null && config.getDenyPatterns() != null)
                ? config.getDenyPatterns()
                : List.of();

        for (String pattern : denyPatterns) {
            if (normalized.contains(pattern.toLowerCase())) {
                return ToolGuardDecision.builder()
                        .toolName(toolName)
                        .allowed(false)
                        .requiresApproval(false)
                        .riskLevel(ToolRiskLevel.CRITICAL)
                        .reason("命中拒绝模式：" + pattern)
                        .matchedPattern(pattern)
                        .build();
            }
        }

        // 4. 敏感路径检测（在严格模式下）
        if (containsSensitivePath(normalized) && properties.getToolGuard().isStrictMode()) {
            return ToolGuardDecision.builder()
                    .toolName(toolName)
                    .allowed(false)
                    .requiresApproval(false)
                    .riskLevel(ToolRiskLevel.CRITICAL)
                    .reason("命中敏感文件或目录策略，已拒绝执行。")
                    .matchedPattern("sensitive_path")
                    .build();
        }

        // 5. 高风险工具审批（使用用户配置的 approvalRequired 和 riskLevel）
        boolean requiresApproval = (config != null && config.getApprovalRequired() != null)
                ? config.getApprovalRequired()
                : false;

        ToolRiskLevel riskLevel = (config != null && config.getRiskLevel() != null)
                ? config.getRiskLevel()
                : ToolRiskLevel.LOW;

        if (requiresApproval) {
            return ToolGuardDecision.builder()
                    .toolName(toolName)
                    .allowed(true)
                    .requiresApproval(true)
                    .riskLevel(riskLevel)
                    .reason("高风险工具，需要人工审批后才能执行。")
                    .matchedPattern(toolName)
                    .build();
        }

        // 6. 默认放行
        return allow(toolName, riskLevel, "工具调用通过安全检查。");
    }

    /**
     * 执行工具调用评估（无用户 ID，使用系统默认配置）。
     *
     * @param toolName 工具名称
     * @param input    工具输入
     * @return 评估结果
     * @deprecated 使用 {@link #evaluate(String, Map, String)} 代替
     */
    @Deprecated
    public ToolGuardDecision evaluate(String toolName, Map<String, Object> input) {
        return evaluate(toolName, input, "system");
    }

    private ToolGuardDecision allow(String toolName, ToolRiskLevel level, String reason) {
        return ToolGuardDecision.builder()
                .toolName(toolName)
                .allowed(true)
                .requiresApproval(false)
                .riskLevel(level)
                .reason(reason)
                .build();
    }

    private boolean containsSensitivePath(String normalizedInput) {
        return SENSITIVE_PATH_PATTERNS.stream().anyMatch(normalizedInput::contains);
    }

    private String stringify(Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(input);
        } catch (JsonProcessingException ex) {
            return input.toString();
        }
    }
}

