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
     * 执行相关操作。
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

