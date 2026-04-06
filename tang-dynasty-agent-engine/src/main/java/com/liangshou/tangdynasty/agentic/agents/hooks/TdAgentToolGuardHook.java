package com.liangshou.tangdynasty.agentic.agents.hooks;

import com.liangshou.tangdynasty.agentic.agents.ConversationSessionContext;
import com.liangshou.tangdynasty.agentic.agents.guard.ToolGuardDecision;
import com.liangshou.tangdynasty.agentic.agents.guard.ToolGuardEngine;
import com.liangshou.tangdynasty.agentic.agents.guard.approval.ToolApprovalService;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostReasoningEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.ToolUseBlock;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * @author LiangshouX
 */
public class TdAgentToolGuardHook implements Hook {

    private final ConversationSessionContext context;
    private final ToolGuardEngine toolGuardEngine;
    private final ToolApprovalService toolApprovalService;

    /**
     * 执行相关操作。
     * @param context 会话上下文
     * @param toolGuardEngine tool guard 引擎
     * @param toolApprovalService 工具审批服务
     */
    public TdAgentToolGuardHook(
            ConversationSessionContext context,
            ToolGuardEngine toolGuardEngine,
            ToolApprovalService toolApprovalService) {
        this.context = context;
        this.toolGuardEngine = toolGuardEngine;
        this.toolApprovalService = toolApprovalService;
    }

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (!(event instanceof PostReasoningEvent postReasoningEvent)) {
            return Mono.just(event);
        }
        Msg reasoningMessage = postReasoningEvent.getReasoningMessage();
        if (reasoningMessage == null || !reasoningMessage.hasContentBlocks(ToolUseBlock.class)) {
            return Mono.just(event);
        }
        List<ToolUseBlock> toolUses = reasoningMessage.getContentBlocks(ToolUseBlock.class);
        List<Map.Entry<ToolUseBlock, ToolGuardDecision>> decisions =
                toolUses.stream()
                        .map(toolUse -> Map.entry(toolUse, toolGuardEngine.evaluate(toolUse.getName(), toolUse.getInput())))
                        .toList();
        List<Map.Entry<ToolUseBlock, ToolGuardDecision>> denied =
                decisions.stream().filter(entry -> !entry.getValue().isAllowed()).toList();
        if (!denied.isEmpty()) {
            String denialText =
                    denied.stream()
                            .map(
                                    entry ->
                                            "工具 "
                                                    + entry.getKey().getName()
                                                    + " 已被拒绝："
                                                    + entry.getValue().getReason())
                            .reduce((left, right) -> left + System.lineSeparator() + right)
                            .orElse("工具调用已被安全策略拒绝。");
            postReasoningEvent.setReasoningMessage(
                    Msg.builder().role(MsgRole.ASSISTANT).textContent(denialText).build());
            return Mono.just(event);
        }
        List<Map.Entry<ToolUseBlock, ToolGuardDecision>> approvals =
                decisions.stream().filter(entry -> entry.getValue().isRequiresApproval()).toList();
        if (!approvals.isEmpty()) {
            toolApprovalService.createPendingApprovals(
                    context,
                    approvals.stream().map(Map.Entry::getKey).toList(),
                    toolUse ->
                            approvals.stream()
                                    .filter(entry -> entry.getKey().getId().equals(toolUse.getId()))
                                    .map(Map.Entry::getValue)
                                    .findFirst()
                                    .orElse(toolGuardEngine.evaluate(toolUse.getName(), toolUse.getInput())));
            postReasoningEvent.stopAgent();
        }
        return Mono.just(event);
    }

    @Override
    public int priority() {
        return 20;
    }
}
