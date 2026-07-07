package com.liangshou.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 链路追踪 ID 过滤器。
 * <p>用于为每个 HTTP 请求生成唯一的追踪 ID，并将其放入 MDC（Mapped Diagnostic Context）中。</p>
 * <p>主要功能包括：
 * <ul>
 *     <li>从请求头中获取已存在的 Trace ID（如果存在）</li>
 *     <li>如果不存在则生成新的 UUID 作为 Trace ID</li>
 *     <li>将 Trace ID 设置到 MDC 中，便于日志记录时追踪</li>
 *     <li>在响应头中返回 Trace ID 给客户端</li>
 *     <li>请求处理完成后清理 MDC 中的 Trace ID</li>
 * </ul>
 * </p>
 * <p>此过滤器具有最高优先级，确保在所有其他过滤器之前执行。</p>
 *
 * @author liangshou
 * @version 1.0
 * @see OncePerRequestFilter
 * @see org.slf4j.MDC
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_MDC = "traceId";

    /**
     * 执行过滤逻辑，处理链路追踪 ID。
     * <p>为当前请求生成或获取 Trace ID，设置到 MDC 和响应头中，并在请求完成后清理。</p>
     *
     * @param request HTTP 请求对象
     * @param response HTTP 响应对象
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 执行异常
     * @throws IOException IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        
        MDC.put(TRACE_ID_MDC, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_MDC);
        }
    }
}
