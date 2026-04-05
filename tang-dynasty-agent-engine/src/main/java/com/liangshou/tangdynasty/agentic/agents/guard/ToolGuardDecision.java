package com.liangshou.tangdynasty.agentic.agents.guard;

import com.liangshou.tangdynasty.agentic.common.enums.ToolRiskLevel;
import lombok.Builder;
import lombok.Getter;

/**
 * 工具防护决策结果 - 封装工具调用安全评估的决策信息。
 *
 * <p>该对象包含以下关键信息：</p>
 * <ul>
 *     <li>{@code toolName} - 被评估的工具名称</li>
 *     <li>{@code allowed} - 是否允许执行该工具调用</li>
 *     <li>{@code requiresApproval} - 是否需要人工审批才能执行</li>
 *     <li>{@code riskLevel} - 风险等级（LOW/MEDIUM/HIGH/CRITICAL）</li>
 *     <li>{@code reason} - 决策原因说明，用于向用户解释为何拒绝或需要审批</li>
 *     <li>{@code matchedPattern} - 命中的风险模式标识，用于审计和调试</li>
 * </ul>
 *
 * <p>该决策对象由 {@link ToolGuardEngine} 生成，作为工具执行前的安全检查结果。</p>
 *
 * @author LiangshouX
 */
@Getter
@Builder
public class ToolGuardDecision {

    private final String toolName;

    private final boolean allowed;

    private final boolean requiresApproval;

    private final ToolRiskLevel riskLevel;

    private final String reason;

    private final String matchedPattern;
}
