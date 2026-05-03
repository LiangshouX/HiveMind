package com.liangshou.tangdynasty.agentic.application.impl;

import com.liangshou.tangdynasty.agentic.agents.ConversationSessionContext;
import com.liangshou.tangdynasty.agentic.agents.TdAgentFactory;
import com.liangshou.tangdynasty.agentic.agents.guard.approval.ToolApprovalService;
import com.liangshou.tangdynasty.agentic.agents.session.AgentSessionStateService;
import com.liangshou.tangdynasty.agentic.agents.streaming.TdAgentActiveSessionRegistry;
import com.liangshou.tangdynasty.agentic.agents.streaming.TdAgentStreamEvent;
import com.liangshou.tangdynasty.agentic.common.config.TdAgentProperties;
import com.liangshou.tangdynasty.agentic.common.enums.TdAgentStreamEventType;
import com.liangshou.tangdynasty.agentic.domain.tool.model.ToolApprovalDocument;
import com.liangshou.tangdynasty.agentic.application.IChatCommandService;
import com.liangshou.tangdynasty.agentic.application.ITdAgentChatService;
import com.liangshou.tangdynasty.agentic.application.ITdAgentStreamingService;
import com.liangshou.tangdynasty.agentic.application.dto.ChatRequest;
import com.liangshou.tangdynasty.agentic.application.dto.ChatResponse;
import com.liangshou.tangdynasty.agentic.application.dto.ToolApprovalActionRequest;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.message.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    /**
     * 构造器
     *
     * @param agentFactory             Agent 工厂
     * @param chatService              聊天服务
     * @param IChatCommandService      命令服务
     * @param agentSessionStateService 会话状态服务
     * @param toolApprovalService      工具审批服务
     * @param activeSessionRegistry    活动会话注册表
     * @param properties               外部化配置
     */
    public TdAgentStreamingServiceImpl(
            TdAgentFactory agentFactory,
            ITdAgentChatService chatService,
            IChatCommandService IChatCommandService,
            AgentSessionStateService agentSessionStateService,
            ToolApprovalService toolApprovalService,
            TdAgentActiveSessionRegistry activeSessionRegistry,
            TdAgentProperties properties) {
        this.agentFactory = agentFactory;
        this.chatService = chatService;
        this.IChatCommandService = IChatCommandService;
        this.agentSessionStateService = agentSessionStateService;
        this.toolApprovalService = toolApprovalService;
        this.activeSessionRegistry = activeSessionRegistry;
        this.properties = properties;
    }

    /**
     * 启动流式请求。
     *
     * @param request {@link ChatRequest}请求对象
     * @return 返回结果
     */
    @Override
    public SseEmitter stream(ChatRequest request) {
        log.info("[Streaming Service] 开始处理流式请求 - userId: {}, sessionId: {}",
                request.getUserId(), request.getSessionId());
        ConversationSessionContext context = chatService.buildContext(request);
        log.debug("[Streaming Service] 会话上下文已构建 - context: {}", context);

        ChatResponse commandResponse = IChatCommandService.handleCommand(context, request.getMessage());
        if (commandResponse != null) {
            log.info("[Streaming Service] 检测到命令请求，直接返回 - command: {}", request.getMessage());
            return singleResponseEmitter(context, commandResponse);
        }

        log.info("[Streaming Service] 创建 ReActAgent 实例");

        long startTime = System.nanoTime();
        ReActAgent agent = agentFactory.createAgent(context);
        long duration = System.nanoTime() - startTime;
        log.info("[Streaming Service] ReActAgent 创建成功 - agentName: {}, 耗时: {} ms",
                agent.getName(), String.format("%.3f", duration / 1_000_000.0));

        log.debug("[Streaming Service] 恢复会话状态");
        agentSessionStateService.restore(context, agent);

        Msg userMessage = Msg.builder()
                .name(request.getUserName() == null ? "user" : request.getUserName())
                .role(MsgRole.USER)
                .textContent(request.getMessage())
                .build();
        log.info("[Streaming Service] 用户消息已构建 - messageId: {}, content length: {}",
                userMessage.getId(),
                request.getMessage().length());

        log.info("[Streaming Service] 开始执行流式调用");
        return execute(
                context,
                agent,
                agent.stream(userMessage, streamOptions()),
                true);
    }

    /**
     * 在批准后继续执行。
     *
     * @param request {@link ToolApprovalActionRequest}请求对象
     * @return 返回结果
     */
    @Override
    public SseEmitter approveAndResume(ToolApprovalActionRequest request) {
        log.info("[Streaming Service-批准] 开始处理批准恢复 - userId: {}, sessionId: {}",
                request.getUserId(), request.getSessionId());
        ConversationSessionContext context =
                chatService.buildContext(request.getUserId(), request.getSessionId(), request.getTitle());
        List<ToolApprovalDocument> approvals =
                toolApprovalService.approve(request.getApprovalIds(), request.getComment());
        log.info("[Streaming Service-批准] 工具审批已通过 - approvalIds: {}, count: {}",
                request.getApprovalIds(), approvals.size());

        ReActAgent agent = agentFactory.createAgent(context);
        agentSessionStateService.restore(context, agent);

        // 构建工具响应消息，告知 Agent 这些工具调用已获批准，可以继续执行
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
                                                                        .text("工具调用已获批准，请继续执行")
                                                                        .build()))
                                        .toList())
                        .build();
        log.info("[Streaming Service-批准] 构建批准响应消息，继续执行");
        return execute(context, agent, agent.stream(toolResponse, streamOptions()), false);
    }

    /**
     * 在拒绝后继续执行。
     *
     * @param request 请求对象
     * @return 返回结果
     */
    @Override
    public SseEmitter rejectAndResume(ToolApprovalActionRequest request) {
        log.info("[Streaming Service-拒绝] 开始处理拒绝恢复 - userId: {}, sessionId: {}",
                request.getUserId(), request.getSessionId());
        ConversationSessionContext context =
                chatService.buildContext(request.getUserId(), request.getSessionId(), request.getTitle());
        List<ToolApprovalDocument> approvals =
                toolApprovalService.reject(request.getApprovalIds(), request.getComment());
        log.info("[Streaming Service-拒绝] 工具审批已拒绝 - approvalIds: {}, count: {}",
                request.getApprovalIds(), approvals.size());

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
        log.info("[Streaming Service-拒绝] 构建拒绝响应消息，继续执行");
        return execute(context, agent, agent.stream(toolResponse, streamOptions()), false);
    }

    /**
     * 中断当前执行。
     *
     * @param userId    用户标识
     * @param sessionId 会话标识
     * @return 返回结果
     */
    @Override
    public boolean interrupt(String userId, String sessionId) {
        String key = key(userId, sessionId);
        log.info("[Streaming Service-中断] 尝试中断会话 - key: {}", key);
        boolean result = activeSessionRegistry.interrupt(key);
        log.info("[Streaming Service-中断] 中断结果 - key: {}, success: {}", key, result);
        return result;
    }

    private SseEmitter execute(
            ConversationSessionContext context,
            ReActAgent agent,
            Flux<Event> eventFlux,
            boolean registerActive) {
        String sessionKey = key(context);
        log.info("[流式执行] 开始执行 - sessionKey: {}, registerActive: {}", sessionKey, registerActive);

        SseEmitter emitter = new SseEmitter(0L);
        AtomicReference<Msg> finalMessage = new AtomicReference<>();

        if (registerActive) {
            activeSessionRegistry.register(sessionKey, agent);
            log.info("[流式执行] 会话已注册到活动列表 - sessionKey: {}", sessionKey);
        }

        log.info("[流式执行] 开始订阅事件流");
        eventFlux.subscribe(
                event -> {
                    log.debug("[流式执行-事件] 收到事件 - type: {}, isLast: {}, messageId: {}",
                            event.getType(),
                            event.isLast(),
                            event.getMessage() != null ? event.getMessage().getId() : "null");

                    if (event.getType() == EventType.AGENT_RESULT && event.getMessage() != null) {
                        finalMessage.set(event.getMessage());
                        log.info("[流式执行-事件] 收到最终结果 - content length: {}",
                                event.getMessage().getTextContent().length());
                    }

                    TdAgentStreamEventType streamEventType = event.getType() == EventType.AGENT_RESULT
                            ? TdAgentStreamEventType.RESULT
                            : event.getType() == EventType.REASONING
                            ? TdAgentStreamEventType.REASONING
                            : event.getType() == EventType.TOOL_RESULT
                            ? TdAgentStreamEventType.TOOL_RESULT
                            : TdAgentStreamEventType.MESSAGE;

                    TdAgentStreamEvent streamEvent = toStreamEvent(
                            context,
                            streamEventType,
                            event.getMessage(),
                            event.isLast(),
                            Map.of("eventType", event.getType().name()));

                    log.debug("[流式执行-事件] 准备发送 SSE 事件 - type: {}, last: {}",
                            streamEventType, event.isLast());
                    send(emitter, streamEvent);
                    log.debug("[流式执行-事件] SSE 事件已发送");
                },
                throwable -> {
                    log.error("[流式执行-错误] 事件流发生异常 - sessionKey: {}, error: {}",
                            sessionKey,
                            throwable.getMessage(),
                            throwable);
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
                    log.info("[流式执行-完成] 事件流已完成 - sessionKey: {}", sessionKey);
                    Msg result = finalMessage.get();
                    boolean paused = isPaused(result) || agentSessionStateService.isPaused(context);
                    log.info("[流式执行-完成] 检查暂停状态 - paused: {}", paused);

                    if (paused) {
                        log.info("[流式执行-完成] 检测到需要审批的工具调用，发送 APPROVAL_REQUIRED 事件");
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

                    log.info("[流式执行-完成] 发送 DONE 事件");
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
                    log.info("[流式执行-完成] 清理工作已完成");
                });

        log.info("[流式执行] 事件订阅已完成，返回 Emitter");
        return emitter;
    }

    private void cleanup(
            ConversationSessionContext context,
            ReActAgent agent,
            Msg finalMessage,
            boolean paused,
            SseEmitter emitter) {
        String sessionKey = key(context);
        log.info("[流式清理] 开始清理工作 - sessionKey: {}, paused: {}", sessionKey, paused);

        try {
            // 保存会话状态（必须在 unregister 之前执行）
            agentSessionStateService.save(context, agent, paused);
            log.debug("[流式清理] 会话状态已保存");
        } catch (Exception e) {
            log.error("[流式清理] 保存会话状态失败 - error: {}", e.getMessage(), e);
        }

        // 注销活动会话（恢复执行，避免内存泄漏）
        try {
            activeSessionRegistry.unregister(sessionKey);
            log.debug("[流式清理] 会话已从活动列表注销");
        } catch (Exception e) {
            log.warn("[流式清理] 注销会话时发生异常 - error: {}", e.getMessage());
        }

        try {
            emitter.complete();
            log.info("[流式清理] SSE Emitter 已完成");
        } catch (Exception e) {
            log.warn("[流式清理] 完成 Emitter 时发生异常 - error: {}", e.getMessage());
        }
        
        log.info("[流式清理] 清理工作已完成 - sessionKey: {}", sessionKey);
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
            log.trace("[SSE发送] 发送事件 - type: {}, sessionId: {}, content length: {}",
                    event.getType(),
                    event.getSessionId(),
                    event.getContent() != null ? event.getContent().length() : 0);
            emitter.send(
                    SseEmitter.event()
                            .name(event.getType().name().toLowerCase())
                            .data(event));
            log.trace("[SSE发送] 事件发送成功 - type: {}", event.getType());
        } catch (IOException ex) {
            // 客户端断开连接是正常情况（如用户中断），使用 WARN 级别
            log.warn("[SSE发送] 客户端可能已断开 - type: {}, error: {}",
                    event.getType(), ex.getMessage());
            throw new IllegalStateException("Failed to emit SSE event.", ex);
        } catch (Exception ex) {
            // 其他异常使用 ERROR 级别
            log.error("[SSE发送] 发送 SSE 事件失败 - type: {}, error: {}",
                    event.getType(), ex.getMessage(), ex);
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
