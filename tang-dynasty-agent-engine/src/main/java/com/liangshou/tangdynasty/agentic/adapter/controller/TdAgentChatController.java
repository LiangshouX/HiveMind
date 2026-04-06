package com.liangshou.tangdynasty.agentic.adapter.controller;

import com.liangshou.tangdynasty.agentic.adapter.controller.dto.*;

import com.liangshou.tangdynasty.agentic.agents.guard.approval.ToolApprovalService;
import com.liangshou.tangdynasty.agentic.agents.streaming.TdAgentStreamingService;
import com.liangshou.tangdynasty.agentic.domain.document.ConversationViewDocument;
import com.liangshou.tangdynasty.agentic.domain.document.ToolApprovalDocument;
import com.liangshou.tangdynasty.agentic.service.TdAgentChatService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Agent 聊天 REST API 控制器，提供同步和流式对话接口。
 * <p>
 * 提供的 API 端点包括：
 * <ul>
 *   <li>POST /api/v1/tdagent/chat - 同步聊天请求</li>
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
public class TdAgentChatController {

    private final TdAgentChatService chatService;
    private final TdAgentStreamingService streamingService;
    private final ToolApprovalService toolApprovalService;

    /**
     * 执行相关操作。
     * @param chatService 聊天服务
     * @param streamingService 流式服务
     * @param toolApprovalService 工具审批服务
     */
    public TdAgentChatController(
            TdAgentChatService chatService,
            TdAgentStreamingService streamingService,
            ToolApprovalService toolApprovalService) {
        this.chatService = chatService;
        this.streamingService = streamingService;
        this.toolApprovalService = toolApprovalService;
    }

    /**
     * 处理聊天请求。
     * @param request 请求对象
     * @return 返回结果
     */
    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return chatService.chat(request);
    }

    /**
     * 启动流式请求。
     * @param request 请求对象
     * @return 返回结果
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody ChatRequest request) {
        return streamingService.stream(request);
    }

    /**
     * 执行 approve 操作。
     * @param request 请求对象
     * @return 返回结果
     */
    @PostMapping(value = "/approvals/approve", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter approve(@Valid @RequestBody ToolApprovalActionRequest request) {
        return streamingService.approveAndResume(request);
    }

    /**
     * 执行 reject 操作。
     * @param request 请求对象
     * @return 返回结果
     */
    @PostMapping(value = "/approvals/reject", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter reject(@Valid @RequestBody ToolApprovalActionRequest request) {
        return streamingService.rejectAndResume(request);
    }

    /**
     * 执行 listApprovals 操作。
     * @param userId 用户标识
     * @param sessionId 会话标识
     * @return 返回结果
     */
    @GetMapping("/approvals/{userId}/{sessionId}")
    public List<ToolApprovalDocument> listApprovals(
            @PathVariable String userId, @PathVariable String sessionId) {
        return toolApprovalService.listPending(userId, sessionId);
    }

    /**
     * 中断当前执行。
     * @param request 请求对象
     * @return 返回结果
     */
    @PostMapping("/chat/interrupt")
    public ChatResponse interrupt(@Valid @RequestBody InterruptRequest request) {
        boolean interrupted = streamingService.interrupt(request.getUserId(), request.getSessionId());
        return ChatResponse.builder()
                .success(interrupted)
                .commandHandled(false)
                .userId(request.getUserId())
                .sessionId(request.getSessionId())
                .message(interrupted ? "已发送中断信号。" : "当前没有活动中的流式会话。")
                .messageCount(0)
                .timestamp(java.time.Instant.now().toString())
                .metadata(java.util.Map.of("interrupted", interrupted))
                .build();
    }

    /**
     * 列出会话列表。
     * @param userId 用户标识
     * @return 返回结果
     */
    @GetMapping("/sessions/{userId}")
    public List<ConversationViewDocument> listSessions(@PathVariable String userId) {
        return chatService.listSessions(userId);
    }

    /**
     * 获取会话历史。
     * @param userId 用户标识
     * @param sessionId 会话标识
     * @return 返回结果
     */
    @GetMapping("/sessions/{userId}/{sessionId}")
    public SessionHistoryResponse getSessionHistory(
            @PathVariable String userId, @PathVariable String sessionId) {
        return chatService.getSessionHistory(userId, sessionId);
    }
}
