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
 * 工具执行防护代理 - 以 Agent Tool 的形式提供工具调用安全控制。
 *
 * <p>该类包装原始 Agent 工具，在工具执行前进行安全检查：</p>
 * <ul>
 *     <li>通过 {@link ToolGuardEngine} 评估工具调用的风险等级</li>
 *     <li>如果工具被判定为不允许执行，直接返回错误结果</li>
 *     <li>如果需要人工审批且尚未批准，抛出 {@link io.agentscope.core.tool.ToolSuspendException} 暂停执行</li>
 *     <li>工具成功执行后，自动标记审批记录为 EXECUTED 状态</li>
 * </ul>
 *
 * <p>该设计实现了责任链模式，在不修改原始工具逻辑的前提下，统一添加工具安全防护层。</p>
 *
 * @author LiangshouX
 */
public class GuardedAgentTool implements AgentTool {

    private final AgentTool delegate;
    private final ConversationSessionContext context;
    private final ToolGuardEngine toolGuardEngine;
    private final ToolApprovalService toolApprovalService;

    /**
     * 构造器
     *
     * @param delegate            被包装的工具
     * @param context             会话上下文
     * @param toolGuardEngine     tool guard 引擎
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
        String userId = context.getUserId();
        ToolGuardDecision decision = toolGuardEngine.evaluate(getName(), param.getInput(), userId);
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

