package com.liangshou.agentic.agents.hooks;

import com.liangshou.agentic.agents.ConversationSessionContext;
import com.liangshou.agentic.agents.guard.ToolGuardDecision;
import com.liangshou.agentic.agents.guard.ToolGuardEngine;
import com.liangshou.agentic.agents.guard.approval.ToolApprovalService;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.*;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.middleware.ActingInput;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.middleware.ReasoningInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Agent 工具防护中间件 - 在推理后对工具调用进行安全检查和审批控制。
 *
 * <p>使用 AgentScope 2.0 推荐的 {@link MiddlewareBase} 接口，
 * 通过 {@link #onReasoning} 拦截推理阶段的事件流，检查工具调用安全性。</p>
 *
 * <p>工作流程：</p>
 * <ol>
 *     <li>拦截推理阶段的事件流</li>
 *     <li>收集工具调用信息（{@link ToolCallStartEvent}、{@link ToolCallDeltaEvent}）</li>
 *     <li>在推理完成后（{@link TextBlockEndEvent} 或流结束），评估工具调用风险</li>
 *     <li>如果存在需要审批的工具，创建审批记录并发送 {@link RequestStopEvent}</li>
 *     <li>如果存在被拒绝的工具，发送拒绝消息</li>
 * </ol>
 *
 * @author LiangshouX
 */
public class TdAgentToolGuardMiddleware implements MiddlewareBase {

    private static final Logger log = LoggerFactory.getLogger(TdAgentToolGuardMiddleware.class);

    private final ConversationSessionContext context;
    private final ToolGuardEngine toolGuardEngine;
    private final ToolApprovalService toolApprovalService;

    public TdAgentToolGuardMiddleware(
            ConversationSessionContext context,
            ToolGuardEngine toolGuardEngine,
            ToolApprovalService toolApprovalService) {
        this.context = context;
        this.toolGuardEngine = toolGuardEngine;
        this.toolApprovalService = toolApprovalService;
    }

    @Override
    public Flux<AgentEvent> onReasoning(
            Agent agent,
            RuntimeContext ctx,
            ReasoningInput input,
            Function<ReasoningInput, Flux<AgentEvent>> next) {

        // 收集工具调用信息的缓冲区
        AtomicReference<String> currentToolCallId = new AtomicReference<>();
        AtomicReference<String> currentToolCallName = new AtomicReference<>();
        StringBuilder currentToolInput = new StringBuilder();
        List<ToolCallInfo> collectedToolCalls = new ArrayList<>();

        return next.apply(input)
                .doOnNext(event -> {
                    // 收集工具调用开始事件
                    if (event instanceof ToolCallStartEvent start) {
                        currentToolCallId.set(start.getToolCallId());
                        currentToolCallName.set(start.getToolCallName());
                        currentToolInput.setLength(0);
                    }
                    // 收集工具调用输入增量
                    else if (event instanceof ToolCallDeltaEvent delta) {
                        currentToolInput.append(delta.getDelta());
                    }
                    // 工具调用结束，记录完整信息
                    else if (event instanceof ToolCallEndEvent end) {
                        String toolName = end.getToolCallName();
                        String toolInput = currentToolInput.toString();
                        collectedToolCalls.add(new ToolCallInfo(
                                end.getToolCallId(), toolName, toolInput));
                        currentToolCallId.set(null);
                        currentToolCallName.set(null);
                        currentToolInput.setLength(0);
                    }
                })
                .concatMap(event -> {
                    // 在推理完成后（第一个非工具调用事件），执行安全检查
                    if (!collectedToolCalls.isEmpty()
                            && !(event instanceof ToolCallStartEvent)
                            && !(event instanceof ToolCallDeltaEvent)
                            && !(event instanceof ToolCallEndEvent)) {

                        List<ToolCallInfo> toCheck = new ArrayList<>(collectedToolCalls);
                        collectedToolCalls.clear();

                        // 评估每个工具调用
                        List<String> deniedReasons = new ArrayList<>();
                        List<ToolCallInfo> needsApproval = new ArrayList<>();

                        for (ToolCallInfo toolCall : toCheck) {
                            Map<String, Object> inputMap = parseInput(toolCall.input());
                            ToolGuardDecision decision = toolGuardEngine.evaluate(
                                    toolCall.name(), inputMap, context.getUserId());

                            if (!decision.isAllowed()) {
                                deniedReasons.add("工具 " + toolCall.name() + " 已被拒绝："
                                        + decision.getReason());
                            } else if (decision.isRequiresApproval()) {
                                needsApproval.add(toolCall);
                            }
                        }

                        // 如果有被拒绝的工具，返回拒绝消息
                        if (!deniedReasons.isEmpty()) {
                            String denialText = String.join("\n", deniedReasons);
                            log.warn("[ToolGuard] 工具调用被拒绝: {}", denialText);
                            return Flux.just(event,
                                    new RequestStopEvent(denialText));
                        }

                        // 如果需要审批，创建审批记录并暂停
                        if (!needsApproval.isEmpty()) {
                            List<ToolUseBlock> toolUseBlocks = needsApproval.stream()
                                    .map(tc -> ToolUseBlock.builder()
                                            .id(tc.id())
                                            .name(tc.name())
                                            .input(parseInput(tc.input()))
                                            .build())
                                    .toList();

                            toolApprovalService.createPendingApprovals(
                                    context, toolUseBlocks,
                                    toolUse -> toolGuardEngine.evaluate(
                                            toolUse.getName(), toolUse.getInput(),
                                            context.getUserId()));

                            log.info("[ToolGuard] 工具调用需要审批，暂停执行 - tools: {}",
                                    needsApproval.stream()
                                            .map(ToolCallInfo::name)
                                            .toList());

                            return Flux.just(event,
                                    new RequestStopEvent("工具调用需要审批"));
                        }
                    }

                    return Flux.just(event);
                });
    }

    /**
     * 解析工具输入 JSON。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseInput(String inputJson) {
        if (inputJson == null || inputJson.isBlank()) {
            return Map.of();
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(inputJson, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * 工具调用信息记录。
     */
    private record ToolCallInfo(String id, String name, String input) {}
}
