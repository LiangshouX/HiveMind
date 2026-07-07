package com.liangshou.agentic.adapter.controller;

import com.liangshou.agentic.agents.guard.approval.ToolApprovalService;
import com.liangshou.agentic.application.ITdAgentStreamingService;
import com.liangshou.agentic.domain.memory.model.ConversationViewDocument;
import com.liangshou.agentic.domain.tool.model.ToolApprovalDocument;
import com.liangshou.agentic.application.ITdAgentChatService;
import com.liangshou.agentic.application.dto.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;
import java.util.List;

/**
 * Agent 聊天 REST API 控制器，提供同步和流式对话接口。
 * <p>
 * 提供的 API 端点包括：
 * <ul>
 *   <li>POST //v1/tdagent/chat - 同步聊天请求</li>
 *   <li>POST /api/v1/tdagent/chat/stream - 流式聊天请求（SSE）</li>
 *   <li>POST /api/v1/tdagent/approvals/approve - 批准工具调用并继续执行（SSE）</li>
 *   <li>POST /api/v1/tdagent/approvals/reject - 拒绝工具调用并继续执行（SSE）</li>
 *   <li>GET /api/v1/tdagent/approvals/{userId}/{sessionId} - 查询待审批的工具调用列表</li>
 *   <li>POST /api/v1/tdagent/chat/interrupt - 中断正在执行的流式会话</li>
 *   <li>GET /api/v1/tdagent/sessions/{userId} - 列出用户的所有会话</li>
 *   <li>GET /api/v1/tdagent/sessions/{userId}/{sessionId} - 获取会话历史记录</li>
 * </ul>
 * </p>
 * <p>
 * 该控制器整合了聊天服务、流式服务和工具审批服务，
 * 是客户端与 Agent 系统交互的主要入口。
 * </p>
 *
 * @author LiangshouX
 */
@RestController
@RequestMapping("/api/v1/tdagent")
@SuppressWarnings("unused")
public class TdAgentChatController {

    private static final Logger log = LoggerFactory.getLogger(TdAgentChatController.class);

    private final ITdAgentChatService chatService;
    private final ITdAgentStreamingService streamingService;
    private final ToolApprovalService toolApprovalService;

    /**
     * 构造器
     *
     * @param chatService         聊天服务
     * @param streamingService    流式服务
     * @param toolApprovalService 工具审批服务
     */
    public TdAgentChatController(
            ITdAgentChatService chatService,
            ITdAgentStreamingService streamingService,
            ToolApprovalService toolApprovalService) {
        this.chatService = chatService;
        this.streamingService = streamingService;
        this.toolApprovalService = toolApprovalService;
    }

    /**
     * 处理聊天请求。
     *
     * @param request 请求对象
     * @return 返回结果
     */
    @PostMapping("/chat")
    public ChatResponse chat(Principal principal, @Valid @RequestBody ChatRequest request) {
        applyCurrentUser(principal, request);
        return chatService.chat(request);
    }

    /**
     * 启动流式请求。
     *
     * @param request 请求对象
     * @return 返回结果
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Principal principal, @Valid @RequestBody ChatRequest request) {
        log.info("[流式聊天] 收到流式请求 - userId: {}, sessionId: {}, message: {}",
                principal != null ? principal.getName() : "unknown",
                request.getSessionId(),
                request.getMessage());
        applyCurrentUser(principal, request);
        SseEmitter emitter = streamingService.stream(request);
        log.info("[流式聊天] SSE Emitter 已创建并返回 - userId: {}, sessionId: {}",
                request.getUserId(), request.getSessionId());
        return emitter;
    }

    /**
     * 执行 approve 操作。
     *
     * @param request 请求对象
     * @return 返回结果
     */
    @PostMapping(value = "/approvals/approve", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter approve(Principal principal, @Valid @RequestBody ToolApprovalActionRequest request) {
        log.info("[工具审批] 收到批准请求 - userId: {}, sessionId: {}, approvalIds: {}",
                principal != null ? principal.getName() : "unknown",
                request.getSessionId(),
                request.getApprovalIds());
        request.setUserId(currentUserId(principal));
        return streamingService.approveAndResume(request);
    }

    /**
     * 执行 reject 操作。
     *
     * @param request 请求对象
     * @return 返回结果
     */
    @PostMapping(value = "/approvals/reject", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter reject(Principal principal, @Valid @RequestBody ToolApprovalActionRequest request) {
        log.info("[工具审批] 收到拒绝请求 - userId: {}, sessionId: {}, approvalIds: {}",
                principal != null ? principal.getName() : "unknown",
                request.getSessionId(),
                request.getApprovalIds());
        request.setUserId(currentUserId(principal));
        return streamingService.rejectAndResume(request);
    }

    /**
     * 执行 listApprovals 操作。
     *
     * @param sessionId 会话标识
     * @return 返回结果
     */
    @GetMapping("/approvals/me/{sessionId}")
    public List<ToolApprovalDocument> listApprovals(
            Principal principal,
            @PathVariable String sessionId
    ) {
        return toolApprovalService.listPending(currentUserId(principal), sessionId);
    }

    /**
     * 中断当前执行。
     *
     * @param request 请求对象
     * @return 返回结果
     */
    @PostMapping("/chat/interrupt")
    public ChatResponse interrupt(Principal principal, @Valid @RequestBody InterruptRequest request) {
        String userId = currentUserId(principal);
        log.info("[中断会话] 收到中断请求 - userId: {}, sessionId: {}", userId, request.getSessionId());
        request.setUserId(userId);
        boolean interrupted = streamingService.interrupt(userId, request.getSessionId());
        log.info("[中断会话] 中断结果 - userId: {}, sessionId: {}, success: {}",
                userId, request.getSessionId(), interrupted);
        return ChatResponse.builder()
                .success(interrupted)
                .commandHandled(false)
                .userId(userId)
                .sessionId(request.getSessionId())
                .message(interrupted ? "已发送中断信号。" : "当前没有活动中的流式会话。")
                .messageCount(0)
                .timestamp(java.time.Instant.now().toString())
                .metadata(java.util.Map.of("interrupted", interrupted))
                .build();
    }

    /**
     * 列出会话列表。
     *
     * @return 返回结果
     */
    @GetMapping("/sessions/me")
    public List<ConversationViewDocument> listSessions(Principal principal) {
        return chatService.listSessions(currentUserId(principal));
    }

    /**
     * 获取会话历史。
     *
     * @param sessionId 会话标识
     * @return 返回结果
     */
    @GetMapping("/sessions/me/{sessionId}")
    public SessionHistoryResponse getSessionHistory(
            Principal principal,
            @PathVariable String sessionId
    ) {
        return chatService.getSessionHistory(currentUserId(principal), sessionId);
    }

    /**
     * 删除会话。
     *
     * @param sessionId 会话标识
     */
    @DeleteMapping("/sessions/me/{sessionId}")
    public void deleteSession(
            Principal principal,
            @PathVariable String sessionId
    ) {
        chatService.deleteSession(currentUserId(principal), sessionId);
    }

    private void applyCurrentUser(Principal principal, ChatRequest request) {
        request.setUserId(currentUserId(principal));
        if (request.getUserName() == null || request.getUserName().isBlank()) {
            request.setUserName(principal.getName());
        }
    }

    private String currentUserId(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new IllegalArgumentException("未获取到当前登录用户");
        }
        return principal.getName();
    }
}
