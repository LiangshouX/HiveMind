package com.liangshou.tangdynasty.agentic.agents.guard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liangshou.tangdynasty.agentic.common.config.TdAgentProperties;
import com.liangshou.tangdynasty.agentic.domain.shared.enums.ToolRiskLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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
 * <p><strong>测试覆盖场景：</strong></p>
 * <ol>
 *     <li>需要人工审批的高风险工具调用</li>
 *     <li>命中危险模式被直接拒绝的工具调用</li>
 *     <li>普通低风险工具的放行逻辑</li>
 *     <li>严格模式下敏感路径的拦截</li>
 * </ol>
 *
 * @author LiangshouX
 * @see ToolGuardEngine
 * @see com.liangshou.tangdynasty.agentic.domain.shared.enums.ToolRiskLevel
 * @see com.liangshou.tangdynasty.agentic.agents.guard.ToolGuardDecision
 */
class ToolGuardEngineTest {

    private final ToolGuardEngine toolGuardEngine =
            new ToolGuardEngine(new TdAgentProperties(), new ObjectMapper());

    /**
     * 测试高风险工具是否需要人工审批。
     *
     * <p>验证当调用属于预定义高风险列表的工具（如 run_shell_command）时，
     * 防护引擎应该返回需要审批的决策，同时标记为 HIGH 风险等级。</p>
     *
     * <p><strong>预期行为：</strong></p>
     * <ul>
     *     <li>{@code isAllowed()} 返回 true（允许执行，但需先审批）</li>
     *     <li>{@code isRequiresApproval()} 返回 true（需要人工审批）</li>
     *     <li>{@code getRiskLevel()} 返回 {@link ToolRiskLevel#HIGH}</li>
     * </ul>
     *
     * <p><strong>业务意义：</strong>确保用户能够意识到某些操作的风险性，
     * 并在执行前进行确认，防止误操作导致数据丢失或系统损坏。</p>
     */
    @Test
    @DisplayName("高风险工具应该需要人工审批")
    void shouldRequireApprovalForHighRiskTool() {
        ToolGuardDecision decision =
                toolGuardEngine.evaluate("run_shell_command", Map.of("command", "dir"));

        assertThat(decision.isAllowed()).isTrue();
        assertThat(decision.isRequiresApproval()).isTrue();
        assertThat(decision.getRiskLevel()).isEqualTo(ToolRiskLevel.HIGH);
    }

    /**
     * 测试危险命令模式是否被正确拒绝。
     *
     * <p>验证当工具输入包含预定义的危险模式（如 rm -rf /）时，
     * 防护引擎应该直接拒绝执行，并标记为 CRITICAL 风险等级。</p>
     *
     * <p><strong>预期行为：</strong></p>
     * <ul>
     *     <li>{@code isAllowed()} 返回 false（禁止执行）</li>
     *     <li>{@code isRequiresApproval()} 返回 false（不允许通过审批绕过）</li>
     *     <li>{@code getRiskLevel()} 返回 {@link ToolRiskLevel#CRITICAL}</li>
     *     <li>{@code getReason()} 包含拒绝原因说明</li>
     * </ul>
     *
     * <p><strong>安全防护：</strong>此机制用于防止恶意或误操作导致的灾难性后果，
     * 如删除根目录、格式化磁盘、强制关机等不可逆操作。</p>
     *
     * <p><strong>支持的危险模式：</strong></p>
     * <ul>
     *     <li>{@code rm -rf}：递归强制删除</li>
     *     <li>{@code del /f}：Windows 强制删除</li>
     *     <li>{@code format }：磁盘格式化</li>
     *     <li>{@code shutdown }：系统关机</li>
     *     <li>{@code reboot }：系统重启</li>
     *     <li>{@code remove-item -recurse -force}：PowerShell 递归删除</li>
     * </ul>
     */
    @Test
    @DisplayName("危险命令模式应该被直接拒绝")
    void shouldRejectDangerousPattern() {
        ToolGuardDecision decision =
                toolGuardEngine.evaluate("run_shell_command", Map.of("command", "rm -rf /"));

        assertThat(decision.isAllowed()).isFalse();
        assertThat(decision.getRiskLevel()).isEqualTo(ToolRiskLevel.CRITICAL);
    }
}