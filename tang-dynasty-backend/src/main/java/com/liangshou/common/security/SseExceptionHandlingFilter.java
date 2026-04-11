package com.liangshou.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * SSE（Server-Sent Events）异常处理过滤器。
 * <p>用于在 SSE 流式响应中捕获和处理异常，防止在响应已提交后尝试写入错误信息。</p>
 * <p>主要功能包括：
 * <ul>
 *     <li>检测 SSE 请求（通过 Accept: text/event-stream 头）</li>
 *     <li>包装响应对象，跟踪响应是否已提交</li>
 *     <li>在发生异常时安全地处理，避免重复写入响应</li>
 * </ul>
 * </p>
 *
 * @author liangshou
 * @version 1.0
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
public class SseExceptionHandlingFilter extends OncePerRequestFilter {

    private static final String SSE_CONTENT_TYPE = "text/event-stream";

    /**
     * 执行过滤逻辑，处理 SSE 请求的异常。
     *
     * @param request     HTTP 请求对象
     * @param response    HTTP 响应对象
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 执行异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // 检查是否为 SSE 请求
        boolean isSseRequest = isSseRequest(request);
        
        if (isSseRequest) {
            log.debug("[SSE过滤器] 检测到 SSE 请求 - URI: {}", request.getRequestURI());
        }
        
        try {
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            // 如果响应已经提交，记录异常但不尝试写入错误响应
            if (response.isCommitted()) {
                log.warn("[SSE过滤器] 响应已提交，无法写入错误信息 - URI: {}, Error: {}", 
                        request.getRequestURI(), ex.getMessage());
            } else {
                // 响应未提交，让异常继续传播到全局异常处理器
                log.error("[SSE过滤器] SSE 请求处理异常 - URI: {}", request.getRequestURI(), ex);
                throw ex;
            }
        }
    }

    /**
     * 检查请求是否为 SSE 请求。
     *
     * @param request HTTP 请求对象
     * @return 如果是 SSE 请求返回 true，否则返回 false
     */
    private boolean isSseRequest(HttpServletRequest request) {
        String acceptHeader = request.getHeader("Accept");
        return acceptHeader != null && acceptHeader.contains(SSE_CONTENT_TYPE);
    }
}
