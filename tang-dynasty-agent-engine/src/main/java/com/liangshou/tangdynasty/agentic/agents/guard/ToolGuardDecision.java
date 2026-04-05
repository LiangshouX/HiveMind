package com.liangshou.tangdynasty.agentic.agents.guard;

import com.liangshou.tangdynasty.agentic.common.enums.ToolRiskLevel;
import lombok.Builder;
import lombok.Getter;

/**
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
