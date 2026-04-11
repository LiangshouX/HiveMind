package com.liangshou.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.liangshou.common.utils.Result;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 * <p>用于统一处理系统中抛出的各种异常，返回标准化的错误响应。</p>
 * <p>支持的异常类型包括：
 * <ul>
 *     <li>{@link MethodArgumentNotValidException} - 参数验证异常</li>
 *     <li>{@link BindException} - 数据绑定异常</li>
 *     <li>{@link AccessDeniedException} - 访问拒绝异常</li>
 *     <li>{@link AuthenticationException} - 认证异常</li>
 *     <li>{@link Exception} - 其他系统异常</li>
 * </ul>
 * </p>
 *
 * @author liangshou
 * @version 1.0
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理参数验证异常。
     * <p>当使用 {@code @Valid} 或 {@code @Validated} 注解进行参数验证失败时调用此方法。</p>
     *
     * @param e 参数验证异常对象
     * @return 包含错误信息的响应实体，HTTP 状态码为 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<String>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Validation Error: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error(ErrorCode.PARAM_ERROR.getCode(), message));
    }

    /**
     * 处理数据绑定异常。
     * <p>当请求参数绑定到对象失败时调用此方法。</p>
     *
     * @param e 数据绑定异常对象
     * @return 包含错误信息的响应实体，HTTP 状态码为 400
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<String>> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Bind Error: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error(ErrorCode.PARAM_ERROR.getCode(), message));
    }

    /**
     * 处理访问拒绝异常。
     * <p>当用户没有权限访问某个资源时调用此方法。</p>
     *
     * @param e 访问拒绝异常对象
     * @return 包含错误信息的响应实体，HTTP 状态码为 403
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<String>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access Denied: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.error(403, "Access Denied"));
    }

    /**
     * 处理认证异常。
     * <p>当用户认证失败时调用此方法。</p>
     *
     * @param e 认证异常对象
     * @return 包含错误信息的响应实体，HTTP 状态码为 401
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<String>> handleAuthenticationException(AuthenticationException e) {
        log.warn("Authentication Error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.error(401, "Authentication Error"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<String>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Bad Request: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error(ErrorCode.PARAM_ERROR.getCode(), e.getMessage()));
    }

    /**
     * 处理系统异常。
     * <p>捕获所有未处理的异常，记录日志并返回统一的错误响应。</p>
     * <p>该方法会获取 MDC 中的 traceId 用于链路追踪。</p>
     *
     * @param e 系统异常对象
     * @return 包含错误信息和 traceId 的响应实体，HTTP 状态码为 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<String>> handleException(Exception e) {
        String traceId = MDC.get("traceId");
        log.error("System Error [TraceId: {}]", traceId, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Result.error(ErrorCode.SYSTEM_ERROR.getCode(), "Internal Server Error. TraceId: " + traceId));
    }
}
