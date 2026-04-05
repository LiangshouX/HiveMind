package com.liangshou.tangdynasty.agentic.agents.guard;

import com.liangshou.tangdynasty.agentic.agents.ConversationSessionContext;
import com.liangshou.tangdynasty.agentic.agents.guard.approval.ToolApprovalService;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.ToolCallParam;
import io.agentscope.core.tool.ToolSuspendException;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 以 Agent Tool 的形式提供 工具执行防护
 *
 * @author LiangshouX
 */
public class GuardedAgentTool implements AgentTool {

    private final AgentTool delegate;
    private final ConversationSessionContext context;
    private final ToolGuardEngine toolGuardEngine;
    private final ToolApprovalService toolApprovalService;

    /**
     * 执行相关操作。
     * @param delegate 被包装的工具
     * @param context 会话上下文
     * @param toolGuardEngine tool guard 引擎
     * @param toolApprovalService 工具审批服务
     */
    public GuardedAgentTool(
            AgentTool delegate,
            ConversationSessionContext context,
            ToolGuardEngine toolGuardEngine,
            ToolApprovalService toolApprovalService) {
        this.delegate = delegate;
        this.context = context;
        this.toolGuardEngine = toolGuardEngine;
        this.toolApprovalService = toolApprovalService;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String getDescription() {
        return delegate.getDescription();
    }

    @Override
    public Map<String, Object> getParameters() {
        return delegate.getParameters();
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        ToolGuardDecision decision = toolGuardEngine.evaluate(getName(), param.getInput());
        if (!decision.isAllowed()) {
            return Mono.just(ToolResultBlock.error(decision.getReason()));
        }
        String toolCallId =
                param.getToolUseBlock() == null ? null : param.getToolUseBlock().getId();
        if (decision.isRequiresApproval()
                && toolCallId != null
                && !toolApprovalService.isApproved(context, toolCallId)) {
            return Mono.error(new ToolSuspendException(decision.getReason()));
        }
        return delegate.callAsync(param)
                .doOnSuccess(
                        result -> {
                            if (toolCallId != null) {
                                toolApprovalService.markExecuted(context, toolCallId);
                            }
                        });
    }
}

