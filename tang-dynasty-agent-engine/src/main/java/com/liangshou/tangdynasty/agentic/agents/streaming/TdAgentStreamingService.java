package com.liangshou.tangdynasty.agentic.agents.streaming;

import com.liangshou.tangdynasty.agentic.adapter.controller.dto.ChatRequest;
import com.liangshou.tangdynasty.agentic.adapter.controller.dto.ChatResponse;
import com.liangshou.tangdynasty.agentic.adapter.controller.dto.ToolApprovalActionRequest;
import com.liangshou.tangdynasty.agentic.agents.ConversationSessionContext;
import com.liangshou.tangdynasty.agentic.agents.TdAgentFactory;
import com.liangshou.tangdynasty.agentic.agents.guard.approval.ToolApprovalService;
import com.liangshou.tangdynasty.agentic.agents.session.AgentSessionStateService;
import com.liangshou.tangdynasty.agentic.common.config.TdAgentProperties;
import com.liangshou.tangdynasty.agentic.common.enums.TdAgentStreamEventType;

import com.liangshou.tangdynasty.agentic.domain.document.ToolApprovalDocument;
import com.liangshou.tangdynasty.agentic.service.ChatCommandService;
import com.liangshou.tangdynasty.agentic.service.TdAgentChatService;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.message.*;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Agent 流式响应服务，提供基于 SSE（Server-Sent Events）的实时流式对话能力。
 * <p>
 * 主要功能包括：
 * <ul>
 *   <li>启动流式聊天请求，实时推送 Agent 的推理和工具调用过程</li>
 *   <li>支持工具调用审批流程，允许用户批准后继续执行</li>
 *   <li>支持工具调用拒绝流程，向 Agent 返回拒绝信息并继续执行</li>
 *   <li>提供会话中断功能，允许用户随时停止正在执行的 Agent 任务</li>
 *   <li>管理活动会话注册表，跟踪正在执行的流式会话</li>
 *   <li>处理各种事件类型（推理、工具结果、最终结果、错误等）</li>
 *   <li>自动保存会话状态，支持暂停和恢复</li>
 * </ul>
 * </p>
 * <p>
 * 该服务与同步聊天服务不同，它通过 Flux 响应式流和 SSE 技术实现实时推送，
 * 为用户提供更好的交互体验，特别是在长时间运行的任务中。
 * </p>
 *
 * @author LiangshouX
 */
@Service
public class TdAgentStreamingService {

    private final TdAgentFactory agentFactory;
    private final TdAgentChatService chatService;
    private final ChatCommandService chatCommandService;
    private final AgentSessionStateService agentSessionStateService;
    private final ToolApprovalService toolApprovalService;
    private final TdAgentActiveSessionRegistry activeSessionRegistry;
    private final TdAgentProperties properties;

    /**
     * 执行相关操作。
     * @param agentFactory Agent 工厂
     * @param chatService 聊天服务
     * @param chatCommandService 命令服务
     * @param agentSessionStateService 会话状态服务
     * @param toolApprovalService 工具审批服务
     * @param activeSessionRegistry 活动会话注册表
     * @param properties 外部化配置
     */
    public TdAgentStreamingService(
            TdAgentFactory agentFactory,
            TdAgentChatService chatService,
            ChatCommandService chatCommandService,
            AgentSessionStateService agentSessionStateService,
            ToolApprovalService toolApprovalService,
            TdAgentActiveSessionRegistry activeSessionRegistry,
            TdAgentProperties properties) {
        this.agentFactory = agentFactory;
        this.chatService = chatService;
        this.chatCommandService = chatCommandService;
        this.agentSessionStateService = agentSessionStateService;
        this.toolApprovalService = toolApprovalService;
        this.activeSessionRegistry = activeSessionRegistry;
        this.properties = properties;
    }

    /**
     * 启动流式请求。
     * @param request 请求对象
     * @return 返回结果
     */
    public SseEmitter stream(ChatRequest request) {
        ConversationSessionContext context = chatService.buildContext(request);
        ChatResponse commandResponse = chatCommandService.handleCommand(context, request.getMessage());
        if (commandResponse != null) {
            return singleResponseEmitter(context, commandResponse);
        }
        ReActAgent agent = agentFactory.createAgent(context);
        agentSessionStateService.restore(context, agent);
        Msg userMessage = Msg.builder()
                .name(request.getUserName() == null ? "user" : request.getUserName())
                .role(MsgRole.USER)
                .textContent(request.getMessage())
                .build();
        return execute(
                context,
                agent,
                agent.stream(userMessage, streamOptions()),
                true);
    }

    /**
     * 在批准后继续执行。
     * @param request 请求对象
     * @return 返回结果
     */
    public SseEmitter approveAndResume(ToolApprovalActionRequest request) {
        ConversationSessionContext context =
                chatService.buildContext(request.getUserId(), request.getSessionId(), request.getTitle());
        toolApprovalService.approve(request.getApprovalIds(), request.getComment());
        ReActAgent agent = agentFactory.createAgent(context);
        agentSessionStateService.restore(context, agent);
        return execute(context, agent, agent.stream(streamOptions()), false);
    }

    /**
     * 在拒绝后继续执行。
     * @param request 请求对象
     * @return 返回结果
     */
    public SseEmitter rejectAndResume(ToolApprovalActionRequest request) {
        ConversationSessionContext context =
                chatService.buildContext(request.getUserId(), request.getSessionId(), request.getTitle());
        List<ToolApprovalDocument> approvals =
                toolApprovalService.reject(request.getApprovalIds(), request.getComment());
        ReActAgent agent = agentFactory.createAgent(context);
        agentSessionStateService.restore(context, agent);
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
        return execute(context, agent, agent.stream(toolResponse, streamOptions()), false);
    }

    /**
     * 中断当前执行。
     * @param userId 用户标识
     * @param sessionId 会话标识
     * @return 返回结果
     */
    public boolean interrupt(String userId, String sessionId) {
        return activeSessionRegistry.interrupt(key(userId, sessionId));
    }

    private SseEmitter execute(
            ConversationSessionContext context,
            ReActAgent agent,
            Flux<Event> eventFlux,
            boolean registerActive) {
        SseEmitter emitter = new SseEmitter(0L);
        AtomicReference<Msg> finalMessage = new AtomicReference<>();
        if (registerActive) {
            activeSessionRegistry.register(key(context), agent);
        }
        eventFlux.subscribe(
                event -> {
                    if (event.getType() == EventType.AGENT_RESULT && event.getMessage() != null) {
                        finalMessage.set(event.getMessage());
                    }
                    send(
                            emitter,
                            toStreamEvent(
                                    context,
                                    event.getType() == EventType.AGENT_RESULT
                                            ? TdAgentStreamEventType.RESULT
                                            : event.getType() == EventType.REASONING
                                                    ? TdAgentStreamEventType.REASONING
                                                    : event.getType() == EventType.TOOL_RESULT
                                                            ? TdAgentStreamEventType.TOOL_RESULT
                                                            : TdAgentStreamEventType.MESSAGE,
                                    event.getMessage(),
                                    event.isLast(),
                                    Map.of("eventType", event.getType().name())));
                },
                throwable -> {
                    try {
                        send(
                                emitter,
                                TdAgentStreamEvent.builder()
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
                        send(
                                emitter,
                                TdAgentStreamEvent.builder()
                                        .type(TdAgentStreamEventType.APPROVAL_REQUIRED)
                                        .sessionId(context.getSessionId())
                                        .userId(context.getUserId())
                                        .content("检测到高风险工具调用，当前会话已暂停，等待审批。")
                                        .last(true)
                                        .metadata(
                                                Map.of(
                                                        "pendingApprovals",
                                                        toolApprovalService.listPending(
                                                                context.getUserId(),
                                                                context.getSessionId())))
                                        .build());
                    }
                    send(
                            emitter,
                            TdAgentStreamEvent.builder()
                                    .type(TdAgentStreamEventType.DONE)
                                    .sessionId(context.getSessionId())
                                    .userId(context.getUserId())
                                    .content("done")
                                    .last(true)
                                    .metadata(
                                            Map.of(
                                                    "paused", paused,
                                                    "timestamp", Instant.now().toString()))
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
        agentSessionStateService.save(context, agent, paused);
        activeSessionRegistry.unregister(key(context));
        try {
            emitter.complete();
        } catch (Exception ignored) {
        }
    }

    private TdAgentStreamEvent toStreamEvent(
            ConversationSessionContext context,
            TdAgentStreamEventType type,
            Msg msg,
            boolean last,
            Map<String, Object> metadata) {
        return TdAgentStreamEvent.builder()
                .type(type)
                .sessionId(context.getSessionId())
                .userId(context.getUserId())
                .messageId(msg == null ? null : msg.getId())
                .content(msg == null ? "" : msg.getTextContent())
                .last(last)
                .metadata(metadata)
                .build();
    }

    private StreamOptions streamOptions() {
        return StreamOptions.builder()
                .eventTypes(EventType.ALL)
                .incremental(properties.getStreaming().isIncremental())
                .build();
    }

    private SseEmitter singleResponseEmitter(
            ConversationSessionContext context, ChatResponse response) {
        SseEmitter emitter = new SseEmitter(0L);
        send(
                emitter,
                TdAgentStreamEvent.builder()
                        .type(TdAgentStreamEventType.RESULT)
                        .sessionId(context.getSessionId())
                        .userId(context.getUserId())
                        .content(response.getMessage())
                        .last(true)
                        .metadata(response.getMetadata())
                        .build());
        send(
                emitter,
                TdAgentStreamEvent.builder()
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
            throw new IllegalStateException("Failed to emit SSE event.", ex);
        }
    }

    private String key(ConversationSessionContext context) {
        return key(context.getUserId(), context.getSessionId());
    }

    private String key(String userId, String sessionId) {
        return userId + ":" + sessionId;
    }
}
