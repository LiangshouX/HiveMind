package com.liangshou.tangdynasty.agentic.agents.guard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liangshou.tangdynasty.agentic.common.config.TdAgentProperties;
import com.liangshou.tangdynasty.agentic.common.enums.ToolRiskLevel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 工具防护引擎 - 评估 Agent 工具调用的安全风险并生成决策。
 *
 * <p>该引擎通过以下规则对工具调用进行安全评估：</p>
 * <ul>
 *     <li><b>危险命令检测</b>：检查工具输入中是否包含高危命令模式（如 rm -rf、format、shutdown 等），命中则直接拒绝</li>
 *     <li><b>敏感路径检测</b>：在严格模式下，检查是否访问敏感文件或目录（如 .env、.git、id_rsa 等），命中则拒绝</li>
 *     <li><b>高风险工具识别</b>：识别需要人工审批的工具（如执行 shell 命令、文件写入、浏览器操作等），标记为 requiresApproval</li>
 *     <li><b>默认放行</b>：未命中任何风险规则的普通工具调用，标记为低风险并允许执行</li>
 * </ul>
 *
 * <p>评估结果以 {@link ToolGuardDecision} 形式返回，包含风险等级、决策原因和命中的模式标识。</p>
 *
 * @author LiangshouX
 */
@Component
public class ToolGuardEngine {

    private static final List<String> APPROVAL_TOOLS =
            List.of(
                    "run_shell_command",
                    "run_ipython_cell",
                    "fs_write_file",
                    "edit_file",
                    "move_file",
                    "browser_click",
                    "browser_type",
                    "browser_navigate");

    private static final List<String> SHELL_DENY_PATTERNS =
            List.of("rm -rf", "del /f", "format ", "shutdown ", "reboot ", "remove-item -recurse -force");

    private static final List<String> SENSITIVE_PATH_PATTERNS =
            List.of(".env", ".git", "id_rsa", "known_hosts", "pom.xml", "application.yml");

    private final TdAgentProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 构造器
     *
     * @param properties   外部化配置
     * @param objectMapper ObjectMapper
     */
    public ToolGuardEngine(TdAgentProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行工具调用评估。
     *
     * @param toolName 工具名称
     * @param input    工具输入
     * @return 返回结果
     */
    public ToolGuardDecision evaluate(String toolName, Map<String, Object> input) {
        if (!properties.getToolGuard().isEnabled()) {
            return allow(toolName, ToolRiskLevel.LOW, "Tool guard disabled.");
        }
        String normalized = stringify(input).toLowerCase(Locale.ROOT);
        for (String pattern : SHELL_DENY_PATTERNS) {
            if (normalized.contains(pattern)) {
                return ToolGuardDecision.builder()
                        .toolName(toolName)
                        .allowed(false)
                        .requiresApproval(false)
                        .riskLevel(ToolRiskLevel.CRITICAL)
                        .reason("命中高风险命令模式，已拒绝执行。")
                        .matchedPattern(pattern)
                        .build();
            }
        }
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
        if (APPROVAL_TOOLS.contains(toolName)) {
            return ToolGuardDecision.builder()
                    .toolName(toolName)
                    .allowed(true)
                    .requiresApproval(true)
                    .riskLevel(ToolRiskLevel.HIGH)
                    .reason("该工具属于高风险操作，需要人工审批后才能执行。")
                    .matchedPattern(toolName)
                    .build();
        }
        return allow(toolName, ToolRiskLevel.LOW, "工具调用通过安全检查。");
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

