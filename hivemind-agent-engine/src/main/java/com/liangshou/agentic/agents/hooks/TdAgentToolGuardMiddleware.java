package com.liangshou.agentic.agents.hooks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liangshou.agentic.agents.ConversationSessionContext;
import com.liangshou.agentic.agents.guard.ToolGuardDecision;
import com.liangshou.agentic.agents.guard.ToolGuardEngine;
import com.liangshou.agentic.agents.guard.approval.ToolApprovalService;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.*;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.middleware.ReasoningInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
 *     <li>收集工具调用信息（{@link ToolCallStartEvent}、{@link ToolCallDeltaEvent}、{@link ToolCallEndEvent}）</li>
 *     <li>在推理完成后（{@link ModelCallEndEvent} 等非工具调用事件），评估工具调用风险</li>
 *     <li>如果存在需要审批的工具，创建审批记录并发送 {@link RequestStopEvent}</li>
 *     <li>如果存在被拒绝的工具，发送拒绝消息</li>
 * </ol>
 *
 * @author LiangshouX
 */
public class TdAgentToolGuardMiddleware implements MiddlewareBase {

    private static final Logger log = LoggerFactory.getLogger(TdAgentToolGuardMiddleware.class);

    /** 共享 ObjectMapper（配置完成后线程安全），避免每次解析工具输入都重新构造 */
    private static final ObjectMapper MAPPER = new ObjectMapper();

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

        // 收集工具调用信息的缓冲区。框架保证同一模型调用内工具块串行发射
        // （新工具块开始前会先 flush 上一个），因此单个 StringBuilder 足以累积当前输入
        StringBuilder currentToolInput = new StringBuilder();
        List<ToolCallInfo> collectedToolCalls = new ArrayList<>();

        return next.apply(input)
                .doOnNext(event -> {
                    // 收集工具调用开始事件
                    if (event instanceof ToolCallStartEvent) {
                        currentToolInput.setLength(0);
                    }
                    // 收集工具调用输入增量
                    else if (event instanceof ToolCallDeltaEvent delta) {
                        currentToolInput.append(delta.getDelta());
                    }
                    // 工具调用结束，记录完整信息（输入只解析一次，评估与审批复用）
                    else if (event instanceof ToolCallEndEvent end) {
                        String rawInput = currentToolInput.toString();
                        collectedToolCalls.add(new ToolCallInfo(
                                end.getToolCallId(),
                                end.getToolCallName(),
                                rawInput,
                                parseInput(rawInput)));
                        currentToolInput.setLength(0);
                    }
                })
                .concatMap(event -> {
                    // 在推理完成后（第一个非工具调用事件，通常是 ModelCallEndEvent），执行安全检查
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
                            Map<String, Object> inputMap = toolCall.input();
                            if (inputMap == null) {
                                // Fail-closed：输入无法解析时无法评估风险，一律转人工审批
                                log.warn("[ToolGuard] 工具输入解析失败，转人工审批 - tool: {}, rawInput: {}",
                                        toolCall.name(), toolCall.rawInput());
                                needsApproval.add(toolCall);
                                continue;
                            }
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
                                            // 解析失败时用空参数占位：审批单仍可展示工具名供人工决策
                                            .input(tc.input() == null ? Map.of() : tc.input())
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
     *
     * @return 解析后的参数 Map；空输入返回空 Map（合法的无参调用）；
     *         解析失败返回 {@code null}（调用方按 fail-closed 处理）
     */
    private static Map<String, Object> parseInput(String inputJson) {
        if (inputJson == null || inputJson.isBlank()) {
            return Map.of();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = MAPPER.readValue(inputJson, Map.class);
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 工具调用信息记录。
     *
     * @param id       工具调用 ID
     * @param name     工具名
     * @param rawInput 原始输入 JSON（用于日志）
     * @param input    解析后的输入；解析失败为 {@code null}
     */
    private record ToolCallInfo(
            String id, String name, String rawInput, Map<String, Object> input) {}
}
