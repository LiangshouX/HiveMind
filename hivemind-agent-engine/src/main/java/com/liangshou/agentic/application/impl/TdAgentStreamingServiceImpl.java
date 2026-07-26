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
import com.liangshou.agentic.application.IConversationPersistenceService;
import com.liangshou.agentic.common.config.TdAgentProperties;
import com.liangshou.agentic.common.enums.TdAgentStreamEventType;
import com.liangshou.agentic.domain.tool.model.ToolApprovalDocument;
import com.liangshou.agentic.application.IChatCommandService;
import com.liangshou.agentic.application.ITdAgentChatService;
import com.liangshou.agentic.application.ITdAgentStreamingService;
import com.liangshou.agentic.application.dto.ChatRequest;
import com.liangshou.agentic.application.dto.ChatResponse;
import com.liangshou.agentic.application.dto.ToolApprovalActionRequest;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.event.*;
import io.agentscope.core.message.*;
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
            IConversationPersistenceService conversationPersistenceService) {
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
        // 缓存最近的 ToolCallStartEvent 信息，用于在 TOOL_RESULT 事件时填充 metadata
        AtomicReference<ToolCallStartEvent> lastToolCallStart = new AtomicReference<>();
        // 缓存 ThinkingBlock 文本
        StringBuilder thinkingBuffer = new StringBuilder();
        // 缓存 TextBlock 文本
        StringBuilder textBuffer = new StringBuilder();

        if (registerActive) {
            activeSessionRegistry.register(sessionKey, agent, providerId, modelId);
        }

        eventFlux.subscribe(
                event -> {
                    // 处理 AgentResultEvent — 最终结果
                    if (event instanceof AgentResultEvent resultEvent) {
                        finalMessage.set(resultEvent.getResult());
                        recordTokenUsage(context, resultEvent.getResult());
                        send(emitter, toStreamEvent(context, TdAgentStreamEventType.RESULT,
                                resultEvent.getResult(), true, Map.of()));
                        return;
                    }

                    // 处理 ThinkingBlockDeltaEvent — 推理过程（流式增量）
                    if (event instanceof ThinkingBlockDeltaEvent thinkingDelta) {
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
                        lastToolCallStart.set(toolCallStart);
                        send(emitter, toStreamEvent(context, TdAgentStreamEventType.MESSAGE,
                                null, false, Map.of(
                                        "toolCallId", toolCallStart.getToolCallId(),
                                        "toolName", toolCallStart.getToolCallName(),
                                        "phase", "TOOL_CALL_START")));
                        return;
                    }

                    // 处理 ToolResultEndEvent — 工具执行结果
                    if (event instanceof ToolResultEndEvent toolResultEnd) {
                        ToolCallStartEvent cachedStart = lastToolCallStart.getAndSet(null);
                        Map<String, Object> metadata = new LinkedHashMap<>();
                        metadata.put("toolCallId", toolResultEnd.getToolCallId());
                        metadata.put("toolName", toolResultEnd.getToolCallName());
                        if (cachedStart != null) {
                            metadata.put("toolCallName", cachedStart.getToolCallName());
                        }
                        send(emitter, toStreamEvent(context, TdAgentStreamEventType.TOOL_RESULT,
                                null, true, metadata));
                        return;
                    }

                    // 处理 TextBlockDeltaEvent — 文本增量（最终回复）
                    if (event instanceof TextBlockDeltaEvent textDelta) {
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
                        cleanup(context, agent, finalMessage.get(), false, emitter);
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

                    send(emitter, TdAgentStreamEvent.builder()
                            .type(TdAgentStreamEventType.DONE)
                            .sessionId(context.getSessionId())
                            .userId(context.getUserId())
                            .content("done")
                            .last(true)
                            .metadata(Map.of("paused", paused, "timestamp", Instant.now().toString()))
                            .build());

                    cleanup(context, agent, result, paused, emitter);
                });

        return emitter;
    }

    private void cleanup(
            ConversationSessionContext context,
            ReActAgent agent,
            Msg finalMessage,
            boolean paused,
            SseEmitter emitter) {
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

        try {
            emitter.complete();
        } catch (Exception e) {
            log.warn("[流式清理] 完成 Emitter 时发生异常 - error: {}", e.getMessage());
        }
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
