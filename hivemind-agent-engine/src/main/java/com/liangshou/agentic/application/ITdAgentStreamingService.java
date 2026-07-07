package com.liangshou.agentic.application;

import com.liangshou.agentic.application.dto.ChatRequest;
import com.liangshou.agentic.application.dto.ToolApprovalActionRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Agent 流式响应服务，提供基于 SSE（Server-Sent Events）的实时流式对话能力。
 *
 * @author LiangshouX
 */
public interface ITdAgentStreamingService {

    /**
     * 启动流式请求。
     *
     * @param request {@link ChatRequest}请求对象
     * @return 返回结果
     */
    SseEmitter stream(ChatRequest request);

    /**
     * 在批准后继续执行。
     *
     * @param request {@link ToolApprovalActionRequest}请求对象
     * @return 返回结果
     */
    SseEmitter approveAndResume(ToolApprovalActionRequest request);

    /**
     * 在拒绝后继续执行。
     *
     * @param request 请求对象
     * @return 返回结果
     */
    SseEmitter rejectAndResume(ToolApprovalActionRequest request);

    /**
     * 中断当前执行。
     *
     * @param userId    用户标识
     * @param sessionId 会话标识
     * @return 返回结果
     */
    boolean interrupt(String userId, String sessionId);
}
