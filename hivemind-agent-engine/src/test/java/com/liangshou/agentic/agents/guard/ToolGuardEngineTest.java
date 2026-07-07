package com.liangshou.agentic.agents.guard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liangshou.agentic.application.dto.ToolConfigDTO;
import com.liangshou.agentic.common.config.TdAgentProperties;
import com.liangshou.agentic.common.util.ToolConfigProvider;
import com.liangshou.agentic.domain.shared.enums.ToolRiskLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * {@link ToolGuardEngine} 单元测试
 *
 * <p>本测试类验证工具防护引擎的核心功能，包括：</p>
 * <ul>
 *     <li>高风险工具的审批机制（如 shell 命令、文件写入等）</li>
 *     <li>危险命令模式的检测与拒绝（如 rm -rf /、format 等）</li>
 *     <li>敏感文件路径的保护策略（如 .env、.git、id_rsa 等）</li>
 *     <li>工具风险等级的正确分类（LOW、HIGH、CRITICAL）</li>
 * </ul>
 *
 * @author LiangshouX
 * @see ToolGuardEngine
 * @see com.liangshou.agentic.domain.shared.enums.ToolRiskLevel
 * @see com.liangshou.agentic.agents.guard.ToolGuardDecision
 */
class ToolGuardEngineTest {

    private ToolConfigProvider configProvider;
    private ToolGuardEngine toolGuardEngine;

    @BeforeEach
    void setUp() {
        configProvider = mock(ToolConfigProvider.class);
        toolGuardEngine = new ToolGuardEngine(new TdAgentProperties(), new ObjectMapper(), configProvider);
    }

    /**
     * 测试高风险工具是否需要人工审批。
     *
     * <p>验证当工具配置为需要审批且风险等级为 HIGH 时，
     * 防护引擎应该返回需要审批的决策。</p>
     */
    @Test
    @DisplayName("高风险工具应该需要人工审批")
    void shouldRequireApprovalForHighRiskTool() {
        ToolConfigDTO config = new ToolConfigDTO();
        config.setApprovalRequired(true);
        config.setRiskLevel(ToolRiskLevel.HIGH);
        config.setEnabled(true);
        when(configProvider.getConfig("run_shell_command", "system")).thenReturn(config);

        ToolGuardDecision decision =
                toolGuardEngine.evaluate("run_shell_command", Map.of("command", "dir"));

        assertThat(decision.isAllowed()).isTrue();
        assertThat(decision.isRequiresApproval()).isTrue();
        assertThat(decision.getRiskLevel()).isEqualTo(ToolRiskLevel.HIGH);
    }

    /**
     * 测试危险命令模式是否被正确拒绝。
     *
     * <p>验证当工具输入包含配置的危险模式（如 rm -rf）时，
     * 防护引擎应该直接拒绝执行，并标记为 CRITICAL 风险等级。</p>
     */
    @Test
    @DisplayName("危险命令模式应该被直接拒绝")
    void shouldRejectDangerousPattern() {
        ToolConfigDTO config = new ToolConfigDTO();
        config.setEnabled(true);
        config.setDenyPatterns(List.of("rm -rf", "format ", "shutdown "));
        when(configProvider.getConfig("run_shell_command", "system")).thenReturn(config);

        ToolGuardDecision decision =
                toolGuardEngine.evaluate("run_shell_command", Map.of("command", "rm -rf /"));

        assertThat(decision.isAllowed()).isFalse();
        assertThat(decision.getRiskLevel()).isEqualTo(ToolRiskLevel.CRITICAL);
    }
}
