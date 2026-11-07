package com.liangshou.agentic.application.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liangshou.agentic.agents.ConversationSessionContext;
import com.liangshou.agentic.common.exceptions.BizException;
import com.liangshou.agentic.common.exceptions.HmeErrorCode;
import com.liangshou.agentic.agents.TdAgentFactory;
import com.liangshou.agentic.agents.TdAgentModelFactory;
import com.liangshou.agentic.agents.TdAgentToolkitFactory;
import com.liangshou.agentic.agents.guard.approval.ToolApprovalService;
import com.liangshou.agentic.agents.provider.TdAgentResolvedModelConfig;
import com.liangshou.agentic.agents.session.AgentSessionStateService;
import com.liangshou.agentic.agents.streaming.TdAgentActiveSessionRegistry;
import com.liangshou.agentic.agents.streaming.TdAgentStreamEvent;
import com.liangshou.agentic.application.ITokenUsageRecordService;
import com.liangshou.agentic.application.IContextUsageService;
import com.liangshou.agentic.application.IConversationPersistenceService;
import com.liangshou.agentic.agents.memory.compaction.TokenMeter;
import com.liangshou.agentic.common.config.TdAgentProperties;
import com.liangshou.agentic.common.enums.TdAgentStreamEventType;
import com.liangshou.agentic.domain.memory.model.ConversationContextStats;
import com.liangshou.agentic.domain.tool.model.ToolApprovalDocument;
import com.liangshou.agentic.application.IChatCommandService;
import com.liangshou.agentic.application.ITdAgentChatService;
import com.liangshou.agentic.application.ITdAgentStreamingService;
import com.liangshou.agentic.application.dto.ChatRequest;
import com.liangshou.agentic.application.dto.ChatResponse;
import com.liangshou.agentic.application.dto.SessionContextUsageResponse;
import com.liangshou.agentic.application.dto.ToolApprovalActionRequest;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.event.*;
import io.agentscope.core.message.*;
import io.agentscope.core.model.ChatUsage;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.state.AgentState;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.ToolCallParam;
import io.agentscope.core.tool.Toolkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Agent 流式响应服务，提供基于 SSE（Server-Sent Events）的实时流式对话能力。
 *
 * <p>使用 AgentScope 2.0 的 {@code streamEvents()} API 和 {@link AgentEvent} 事件体系，
 * 提供细粒度的流式事件推送。</p>
 *
 * @author LiangshouX
 */
@Service
@SuppressWarnings("unused")
public class TdAgentStreamingServiceImpl implements ITdAgentStreamingService {

    private static final Logger log = LoggerFactory.getLogger(TdAgentStreamingServiceImpl.class);

    private final TdAgentFactory agentFactory;
    private final ITdAgentChatService chatService;
    private final IChatCommandService IChatCommandService;
    private final AgentSessionStateService agentSessionStateService;
    private final ToolApprovalService toolApprovalService;
    private final TdAgentActiveSessionRegistry activeSessionRegistry;
    private final TdAgentProperties properties;
    private final TdAgentModelFactory modelFactory;
    private final ITokenUsageRecordService tokenUsageRecordService;
    private final TdAgentToolkitFactory toolkitFactory;
    private final ObjectMapper objectMapper;
    private final IConversationPersistenceService conversationPersistenceService;
    private final IContextUsageService contextUsageService;
    private final TokenMeter tokenMeter;

    public TdAgentStreamingServiceImpl(
            TdAgentFactory agentFactory,
            ITdAgentChatService chatService,
            IChatCommandService IChatCommandService,
            AgentSessionStateService agentSessionStateService,
            ToolApprovalService toolApprovalService,
            TdAgentActiveSessionRegistry activeSessionRegistry,
            TdAgentProperties properties,
            TdAgentModelFactory modelFactory,
            ITokenUsageRecordService tokenUsageRecordService,
            TdAgentToolkitFactory toolkitFactory,
            ObjectMapper objectMapper,
            IConversationPersistenceService conversationPersistenceService,
            IContextUsageService contextUsageService,
            TokenMeter tokenMeter) {
        this.agentFactory = agentFactory;
        this.chatService = chatService;
        this.IChatCommandService = IChatCommandService;
        this.agentSessionStateService = agentSessionStateService;
        this.toolApprovalService = toolApprovalService;
        this.activeSessionRegistry = activeSessionRegistry;
        this.properties = properties;
        this.modelFactory = modelFactory;
        this.tokenUsageRecordService = tokenUsageRecordService;
        this.toolkitFactory = toolkitFactory;
        this.objectMapper = objectMapper;
        this.conversationPersistenceService = conversationPersistenceService;
        this.contextUsageService = contextUsageService;
        this.tokenMeter = tokenMeter;
    }

    @Override
    public SseEmitter stream(ChatRequest request) {
        log.info("[Streaming Service] 开始处理流式请求 - userId: {}, sessionId: {}",
                request.getUserId(), request.getSessionId());
        ConversationSessionContext context = chatService.buildContext(request);

        ChatResponse commandResponse = IChatCommandService.handleCommand(context, request.getMessage());
        if (commandResponse != null) {
            return singleResponseEmitter(context, commandResponse);
        }

        ReActAgent agent = agentFactory.createAgent(context);

        // AgentScope 2.0: 状态由 AgentStateStore 自动管理
        Msg userMessage = Msg.builder()
                .name(request.getUserName() == null ? "user" : request.getUserName())
                .role(MsgRole.USER)
                .textContent(request.getMessage())
                .build();

        return execute(
                context,
                agent,
                agent.streamEvents(userMessage),
                true,
                request.getProviderId(),
                request.getModelId());
    }

    @Override
    public SseEmitter approveAndResume(ToolApprovalActionRequest request) {
        log.info("[Streaming Service-批准] 开始处理批准恢复 - userId: {}, sessionId: {}",
                request.getUserId(), request.getSessionId());
        ConversationSessionContext context =
                chatService.buildContext(request.getUserId(), request.getSessionId(), request.getTitle());
        List<ToolApprovalDocument> approvals =
                toolApprovalService.approve(request.getApprovalIds(), request.getComment());

        conversationPersistenceService.appendApprovalMessages(context, approvals);

        ReActAgent agent = agentFactory.createAgent(context);

        List<ContentBlock> toolResults = approvals.stream()
                .map(approval -> executeApprovedTool(context, approval))
                .toList();

        Msg toolResponse = Msg.builder()
                .role(MsgRole.TOOL)
                .content(toolResults)
                .build();
        return execute(context, agent, agent.streamEvents(toolResponse), false, null, null);
    }

    @Override
    public SseEmitter rejectAndResume(ToolApprovalActionRequest request) {
        log.info("[Streaming Service-拒绝] 开始处理拒绝恢复 - userId: {}, sessionId: {}",
                request.getUserId(), request.getSessionId());
        ConversationSessionContext context =
                chatService.buildContext(request.getUserId(), request.getSessionId(), request.getTitle());
        List<ToolApprovalDocument> approvals =
                toolApprovalService.reject(request.getApprovalIds(), request.getComment());

        conversationPersistenceService.appendApprovalMessages(context, approvals);

        ReActAgent agent = agentFactory.createAgent(context);

        Msg toolResponse =
                Msg.builder()
                        .role(MsgRole.TOOL)
                        .content(
                                approvals.stream()
                                        .<io.agentscope.core.message.ContentBlock>map(
                                                approval ->
                                                        new ToolResultBlock(
                                                                approval.getToolCallId(),
                                                                approval.getToolName(),
                                                                TextBlock.builder()
                                                                        .text(
                                                                                "工具调用未通过审批："
                                                                                        + (approval.getReviewComment() == null
                                                                                        ? "已拒绝"
                                                                                        : approval.getReviewComment()))
                                                                        .build()))
                                        .toList())
                        .build();
        return execute(context, agent, agent.streamEvents(toolResponse), false, null, null);
    }

    @Override
    public boolean interrupt(String userId, String sessionId) {
        String key = key(userId, sessionId);
        log.info("[Streaming Service-中断] 尝试中断会话 - key: {}", key);
        boolean result = activeSessionRegistry.interrupt(key);
        return result;
    }

    /**
     * 执行流式调用，处理 AgentEvent 事件流。
     */
    private SseEmitter execute(
            ConversationSessionContext context,
            ReActAgent agent,
            Flux<AgentEvent> eventFlux,
            boolean registerActive,
            String providerId,
            String modelId) {
        String sessionKey = key(context);

        SseEmitter emitter = new SseEmitter(0L);
        AtomicReference<Msg> finalMessage = new AtomicReference<>();
        // 计时起点：用于统计本轮模型耗时与首 token 延迟（TTFT）
        long startNanos = System.nanoTime();
        AtomicLong firstTokenNanos = new AtomicLong(0L);
        // 缓存最近的 ToolCallStartEvent 信息，用于在 TOOL_RESULT 事件时填充 metadata
        AtomicReference<ToolCallStartEvent> lastToolCallStart = new AtomicReference<>();
        // 缓存 ThinkingBlock 文本
        StringBuilder thinkingBuffer = new StringBuilder();
        // 缓存 TextBlock 文本
        StringBuilder textBuffer = new StringBuilder();
        // 按 toolCallId 累积工具执行输出的文本增量，用于向前端推送"工具结果"内容
        Map<String, StringBuilder> toolResultBuffers = new LinkedHashMap<>();

        if (registerActive) {
            activeSessionRegistry.register(sessionKey, agent, providerId, modelId);
        }

        eventFlux.subscribe(
                event -> {
                    // 处理 AgentResultEvent — 最终结果
                    if (event instanceof AgentResultEvent resultEvent) {
                        finalMessage.set(resultEvent.getResult());
                        recordTokenUsage(context, resultEvent.getResult());
                        markFirstToken(firstTokenNanos);
                        recordContextStats(context, agent, resultEvent.getResult(),
                                startNanos, firstTokenNanos);
                        send(emitter, toStreamEvent(context, TdAgentStreamEventType.RESULT,
                                resultEvent.getResult(), true, Map.of()));
                        return;
                    }

                    // 处理 ThinkingBlockDeltaEvent — 推理过程（流式增量）
                    if (event instanceof ThinkingBlockDeltaEvent thinkingDelta) {
                        markFirstToken(firstTokenNanos);
                        thinkingBuffer.append(thinkingDelta.getDelta());
                        send(emitter, toStreamEvent(context, TdAgentStreamEventType.REASONING,
                                null, false, Map.of("delta", thinkingDelta.getDelta())));
                        return;
                    }

                    // 处理 ThinkingBlockEndEvent — 推理过程结束
                    if (event instanceof ThinkingBlockEndEvent) {
                        String thinking = thinkingBuffer.toString();
                        thinkingBuffer.setLength(0);
                        if (!thinking.isEmpty()) {
                            send(emitter, toStreamEvent(context, TdAgentStreamEventType.REASONING,
                                    null, true, Map.of("thinking", thinking)));
                        }
                        return;
                    }

                    // 处理 ToolCallStartEvent — 工具调用开始
                    if (event instanceof ToolCallStartEvent toolCallStart) {
                        markFirstToken(firstTokenNanos);
                        lastToolCallStart.set(toolCallStart);
                        send(emitter, toStreamEvent(context, TdAgentStreamEventType.TOOL_USE,
                                null, false, Map.of(
                                        "toolCallId", toolCallStart.getToolCallId(),
                                        "toolName", toolCallStart.getToolCallName(),
                                        "phase", "TOOL_CALL_START")));
                        return;
                    }

                    // 处理 ToolResultTextDeltaEvent — 工具执行输出的文本增量（先累积，收尾时统一推送）
                    if (event instanceof ToolResultTextDeltaEvent toolTextDelta) {
                        toolResultBuffers
                                .computeIfAbsent(toolTextDelta.getToolCallId(), key -> new StringBuilder())
                                .append(toolTextDelta.getDelta());
                        return;
                    }

                    // 处理 ToolResultEndEvent — 工具执行结果
                    if (event instanceof ToolResultEndEvent toolResultEnd) {
                        ToolCallStartEvent cachedStart = lastToolCallStart.getAndSet(null);
                        String toolCallId = toolResultEnd.getToolCallId();
                        StringBuilder buffer = toolResultBuffers.remove(toolCallId);
                        String output = buffer == null ? "" : buffer.toString();
                        Map<String, Object> metadata = new LinkedHashMap<>();
                        metadata.put("toolCallId", toolCallId);
                        metadata.put("toolName", toolResultEnd.getToolCallName());
                        metadata.put("state",
                                toolResultEnd.getState() == null ? "" : toolResultEnd.getState().name());
                        if (cachedStart != null) {
                            metadata.put("toolCallName", cachedStart.getToolCallName());
                        }
                        send(emitter, TdAgentStreamEvent.builder()
                                .type(TdAgentStreamEventType.TOOL_RESULT)
                                .sessionId(context.getSessionId())
                                .userId(context.getUserId())
                                .content(truncateToolOutput(output))
                                .last(true)
                                .metadata(metadata)
                                .build());
                        return;
                    }

                    // 处理 TextBlockDeltaEvent — 文本增量（最终回复）
                    if (event instanceof TextBlockDeltaEvent textDelta) {
                        markFirstToken(firstTokenNanos);
                        textBuffer.append(textDelta.getDelta());
                        return;
                    }

                    // 处理 TextBlockEndEvent — 文本块结束
                    if (event instanceof TextBlockEndEvent) {
                        textBuffer.setLength(0);
                        return;
                    }

                    // 处理 RequireUserConfirmEvent — 需要用户确认（HITL）
                    if (event instanceof RequireUserConfirmEvent confirmEvent) {
                        send(emitter, TdAgentStreamEvent.builder()
                                .type(TdAgentStreamEventType.APPROVAL_REQUIRED)
                                .sessionId(context.getSessionId())
                                .userId(context.getUserId())
                                .content("检测到高风险工具调用，需要审批")
                                .last(true)
                                .metadata(Map.of("event", "REQUIRE_USER_CONFIRM"))
                                .build());
                        return;
                    }
                },
                throwable -> {
                    log.error("[流式执行-错误] 事件流发生异常 - sessionKey: {}, error: {}",
                            sessionKey, throwable.getMessage(), throwable);
                    try {
                        send(emitter, TdAgentStreamEvent.builder()
                                .type(TdAgentStreamEventType.ERROR)
                                .sessionId(context.getSessionId())
                                .userId(context.getUserId())
                                .content(throwable.getMessage())
                                .last(true)
                                .metadata(Map.of("timestamp", Instant.now().toString()))
                                .build());
                    } finally {
                        cleanup(context, agent, finalMessage.get(), false);
                        completeEmitter(emitter);
                    }
                },
                () -> {
                    Msg result = finalMessage.get();
                    boolean paused = isPaused(result) || agentSessionStateService.isPaused(context);

                    if (paused) {
                        send(emitter, TdAgentStreamEvent.builder()
                                .type(TdAgentStreamEventType.APPROVAL_REQUIRED)
                                .sessionId(context.getSessionId())
                                .userId(context.getUserId())
                                .content("检测到高风险工具调用，当前会话已暂停，等待审批。")
                                .last(true)
                                .metadata(Map.of(
                                        "pendingApprovals",
                                        toolApprovalService.listPending(
                                                context.getUserId(),
                                                context.getSessionId())))
                                .build());
                    }

                    // 先落库再统计上下文用量，保证 DONE 里携带的是本轮结束后的真实占用
                    cleanup(context, agent, result, paused);

                    Map<String, Object> doneMetadata = new LinkedHashMap<>();
                    doneMetadata.put("paused", paused);
                    doneMetadata.put("timestamp", Instant.now().toString());
                    doneMetadata.put("contextUsage", buildContextUsage(context));

                    send(emitter, TdAgentStreamEvent.builder()
                            .type(TdAgentStreamEventType.DONE)
                            .sessionId(context.getSessionId())
                            .userId(context.getUserId())
                            .content("done")
                            .last(true)
                            .metadata(doneMetadata)
                            .build());

                    completeEmitter(emitter);
                });

        return emitter;
    }

    /**
     * 构建上下文用量快照，供前端实时展示上下文窗口占用。
     */
    private Map<String, Object> buildContextUsage(ConversationSessionContext context) {
        try {
            SessionContextUsageResponse usage =
                    contextUsageService.getUsage(context.getUserId(), context.getSessionId());
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("usedTokens", usage.getUsedTokens());
            snapshot.put("contextWindow", usage.getContextWindow());
            snapshot.put("usageRatio", usage.getUsageRatio());
            snapshot.put("thresholdRatio", usage.getThresholdRatio());
            snapshot.put("thresholdTokens", usage.getThresholdTokens());
            snapshot.put("outputReserveTokens", usage.getOutputReserveTokens());
            snapshot.put("systemPromptTokens", usage.getSystemPromptTokens());
            snapshot.put("toolDefinitionTokens", usage.getToolDefinitionTokens());
            snapshot.put("conversationTokens", usage.getConversationTokens());
            snapshot.put("modelDurationMs", usage.getModelDurationMs());
            snapshot.put("ttftMs", usage.getTtftMs());
            snapshot.put("tokensPerSecond", usage.getTokensPerSecond());
            snapshot.put("cacheHitRatio", usage.getCacheHitRatio());
            snapshot.put("uncachedInputTokens", usage.getUncachedInputTokens());
            snapshot.put("cachedInputTokens", usage.getCachedInputTokens());
            snapshot.put("outputTokens", usage.getOutputTokens());
            snapshot.put("modelCalls", usage.getModelCalls());
            snapshot.put("modelDisplayName", usage.getModelDisplayName());
            snapshot.put("reasoningCount", usage.getReasoningCount());
            snapshot.put("toolCallCount", usage.getToolCallCount());
            snapshot.put("messageCount", usage.getMessageCount());
            snapshot.put("compactionCount", usage.getCompactionCount());
            snapshot.put("nearThreshold", usage.isNearThreshold());
            return snapshot;
        } catch (Exception e) {
            log.debug("[流式执行] 构建上下文用量失败 - error: {}", e.getMessage());
            return Map.of();
        }
    }

    private void cleanup(
            ConversationSessionContext context,
            ReActAgent agent,
            Msg finalMessage,
            boolean paused) {
        String sessionKey = key(context);

        try {
            agentSessionStateService.save(context, agent, paused);
        } catch (Exception e) {
            log.error("[流式清理] 保存会话状态失败 - error: {}", e.getMessage(), e);
        }

        // 将 Agent 的对话上下文持久化到 ConversationMemoryDocument，
        // 确保前端会话列表和标题生成能读取到最新消息
        try {
            AgentState agentState = agent.getAgentState();
            if (agentState != null) {
                List<Msg> messages = agentState.getContext();
                if (messages != null && !messages.isEmpty()) {
                    conversationPersistenceService.replaceMessages(
                            context, messages, "", null, false);
                    log.debug("[流式清理] 对话消息已持久化 - sessionId: {}, messageCount: {}",
                            context.getSessionId(), messages.size());
                }
            }
        } catch (Exception e) {
            log.error("[流式清理] 持久化对话消息失败 - error: {}", e.getMessage(), e);
        }

        try {
            activeSessionRegistry.unregister(sessionKey);
        } catch (Exception e) {
            log.warn("[流式清理] 注销会话时发生异常 - error: {}", e.getMessage());
        }
    }

    /**
     * 完成 SSE Emitter（与落库解耦，确保 DONE 事件能先发出去）。
     */
    private void completeEmitter(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception e) {
            log.warn("[流式清理] 完成 Emitter 时发生异常 - error: {}", e.getMessage());
        }
    }

    /**
     * 工具输出可能非常大（整份文件、批量检索结果），SSE 侧只推送截断后的内容，
     * 完整内容仍以 MongoDB 会话历史为准（刷新后可完整查看）。
     */
    private static final int TOOL_OUTPUT_MAX_LENGTH = 4000;

    private String truncateToolOutput(String output) {
        if (output == null || output.isEmpty()) {
            return "";
        }
        if (output.length() <= TOOL_OUTPUT_MAX_LENGTH) {
            return output;
        }
        return output.substring(0, TOOL_OUTPUT_MAX_LENGTH) + "\n…（输出过长，已截断，完整内容见会话历史）";
    }

    private TdAgentStreamEvent toStreamEvent(
            ConversationSessionContext context,
            TdAgentStreamEventType type,
            Msg msg,
            boolean last,
            Map<String, Object> metadata) {
        String content = "";
        if (msg != null) {
            content = msg.getTextContent();
        }
        return TdAgentStreamEvent.builder()
                .type(type)
                .sessionId(context.getSessionId())
                .userId(context.getUserId())
                .messageId(msg == null ? null : msg.getId())
                .content(content)
                .last(last)
                .metadata(metadata)
                .build();
    }

    private SseEmitter singleResponseEmitter(
            ConversationSessionContext context, ChatResponse response) {
        SseEmitter emitter = new SseEmitter(0L);
        send(emitter, TdAgentStreamEvent.builder()
                .type(TdAgentStreamEventType.RESULT)
                .sessionId(context.getSessionId())
                .userId(context.getUserId())
                .content(response.getMessage())
                .last(true)
                .metadata(response.getMetadata())
                .build());
        send(emitter, TdAgentStreamEvent.builder()
                .type(TdAgentStreamEventType.DONE)
                .sessionId(context.getSessionId())
                .userId(context.getUserId())
                .content("done")
                .last(true)
                .metadata(Map.of("commandHandled", true))
                .build());
        emitter.complete();
        return emitter;
    }

    private boolean isPaused(Msg result) {
        return result != null
                && result.getGenerateReason() != null
                && (result.getGenerateReason() == GenerateReason.REASONING_STOP_REQUESTED
                || result.getGenerateReason() == GenerateReason.ACTING_STOP_REQUESTED
                || result.getGenerateReason() == GenerateReason.TOOL_SUSPENDED);
    }

    private void send(SseEmitter emitter, TdAgentStreamEvent event) {
        try {
            emitter.send(
                    SseEmitter.event()
                            .name(event.getType().name().toLowerCase())
                            .data(event));
        } catch (IOException ex) {
            log.warn("[SSE发送] 客户端可能已断开 - type: {}, error: {}",
                    event.getType(), ex.getMessage());
            throw new BizException(HmeErrorCode.AGENT_SSE_EMIT_ERROR, ex);
        } catch (Exception ex) {
            log.error("[SSE发送] 发送 SSE 事件失败 - type: {}, error: {}",
                    event.getType(), ex.getMessage(), ex);
            throw new BizException(HmeErrorCode.AGENT_SSE_EMIT_ERROR, ex);
        }
    }

    private String key(ConversationSessionContext context) {
        return key(context.getUserId(), context.getSessionId());
    }

    private String key(String userId, String sessionId) {
        return userId + ":" + sessionId;
    }

    private ContentBlock executeApprovedTool(ConversationSessionContext context, ToolApprovalDocument approval) {
        String toolName = approval.getToolName();
        String toolCallId = approval.getToolCallId();

        try {
            Toolkit toolkit = toolkitFactory.createToolkit(context);
            AgentTool tool = toolkit.getTool(toolName);
            if (tool == null) {
                return new ToolResultBlock(
                        toolCallId, toolName,
                        TextBlock.builder().text("工具执行失败：工具 " + toolName + " 未找到").build());
            }

            Map<String, Object> inputMap = parseToolInput(approval.getToolInputJson());

            ToolUseBlock toolUseBlock = ToolUseBlock.builder()
                    .id(toolCallId)
                    .name(toolName)
                    .input(inputMap)
                    .build();
            ToolCallParam param = ToolCallParam.builder()
                    .toolUseBlock(toolUseBlock)
                    .build();

            ToolResultBlock result = tool.callAsync(param).block();

            if (result == null) {
                return new ToolResultBlock(
                        toolCallId, toolName,
                        TextBlock.builder().text("工具执行完成，无输出").build());
            }

            List<ContentBlock> output = result.getOutput();
            return new ToolResultBlock(toolCallId, toolName, output);
        } catch (Exception e) {
            log.error("[审批执行] 工具执行异常 - toolName: {}, error: {}", toolName, e.getMessage(), e);
            return new ToolResultBlock(
                    toolCallId, toolName,
                    TextBlock.builder().text("工具执行异常：" + e.getMessage()).build());
        }
    }

    private Map<String, Object> parseToolInput(String toolInputJson) {
        if (toolInputJson == null || toolInputJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(toolInputJson, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("[审批执行] 解析工具输入参数失败: {}", e.getMessage());
            return Map.of();
        }
    }

    /**
     * 标记"首 token"到达时刻。
     *
     * <p>只有携带实际内容的事件（推理增量 / 文本增量 / 工具调用 / 最终结果）
     * 才算首 token；框架的即时状态事件不算，否则 TTFT 会失真为毫秒级。</p>
     */
    private void markFirstToken(AtomicLong firstTokenNanos) {
        firstTokenNanos.compareAndSet(0L, System.nanoTime());
    }

    /**
     * 记录本轮上下文统计快照。
     *
     * <p>采集三类指标，供前端"上下文面板"展示：</p>
     * <ul>
     *   <li>工具定义（Tool Schema）开销 — 每次请求都要随提示词送给模型</li>
     *   <li>模型总耗时与首 token 延迟（TTFT）</li>
     *   <li>输出速度（TPS）</li>
     * </ul>
     */
    private void recordContextStats(ConversationSessionContext context,
                                    ReActAgent agent,
                                    Msg result,
                                    long startNanos,
                                    AtomicLong firstTokenNanos) {
        try {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
            long ttftMs = firstTokenNanos.get() > 0L
                    ? (firstTokenNanos.get() - startNanos) / 1_000_000L
                    : durationMs;
            int outputTokens = extractOutputTokens(result == null ? null : result.getMetadata());
            long generationMs = Math.max(1L, durationMs - ttftMs);
            double tps = (double) outputTokens / (generationMs / 1000d);

            ConversationContextStats stats = ConversationContextStats.builder()
                    .toolDefinitionTokens(estimateToolDefinitionTokens(agent))
                    .modelDurationMs(durationMs)
                    .ttftMs(ttftMs)
                    .outputTokens(outputTokens)
                    .tokensPerSecond(Math.round(tps * 10d) / 10d)
                    .measuredAt(Instant.now())
                    .build();
            conversationPersistenceService.recordContextStats(context, stats);
        } catch (Exception e) {
            log.debug("[流式执行] 记录上下文统计失败 - error: {}", e.getMessage());
        }
    }

    /**
     * 估算工具定义（Tool Schema）占用的 token 数。
     */
    private int estimateToolDefinitionTokens(ReActAgent agent) {
        try {
            Toolkit toolkit = agent.getToolkit();
            if (toolkit == null) {
                return 0;
            }
            List<ToolSchema> schemas = toolkit.getToolSchemas();
            if (schemas == null || schemas.isEmpty()) {
                return 0;
            }
            StringBuilder sb = new StringBuilder();
            for (ToolSchema schema : schemas) {
                sb.append(schema.getName()).append('\n');
                if (schema.getDescription() != null) {
                    sb.append(schema.getDescription()).append('\n');
                }
                if (schema.getParameters() != null) {
                    sb.append(objectMapper.writeValueAsString(schema.getParameters())).append('\n');
                }
            }
            return tokenMeter.countTokens(sb.toString());
        } catch (Exception e) {
            log.debug("[流式执行] 估算工具定义开销失败 - error: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 从消息 metadata 中提取本轮输出 token 数。
     */
    private int extractOutputTokens(Map<String, Object> metadata) {
        if (metadata == null) {
            return 0;
        }
        Object usage = metadata.get("_chat_usage");
        if (usage instanceof ChatUsage chatUsage) {
            return chatUsage.getOutputTokens();
        }
        if (usage instanceof Map<?, ?> map) {
            Object value = map.get("outputTokens");
            if (value instanceof Number number) {
                return number.intValue();
            }
        }
        return 0;
    }

    @SuppressWarnings("deprecation")
    private void recordTokenUsage(ConversationSessionContext context, Msg message) {
        try {
            TdAgentResolvedModelConfig modelConfig = context.getResolvedModelConfig();
            if (modelConfig == null) {
                modelConfig = modelFactory.currentConfig();
            }

            String modelConfigJson = String.format(
                    "{\"provider\":\"%s\",\"modelName\":\"%s\"}",
                    modelConfig.getProviderId(),
                    modelConfig.getModelId()
            );

            tokenUsageRecordService.record(
                    context.getUserId(),
                    context.getSessionId(),
                    message.getId(),
                    modelConfigJson,
                    message.getMetadata()
            );
        } catch (Exception e) {
            log.warn("[流式执行-Token记录] Token 使用量记录失败 - userId: {}, sessionId: {}, error: {}",
                    context.getUserId(), context.getSessionId(), e.getMessage());
        }
    }
}
