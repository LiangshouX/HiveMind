package com.liangshou.common;

import com.liangshou.common.utils.Result;
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

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<String>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Validation Error: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error(ErrorCode.PARAM_ERROR.getCode(), message));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<String>> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Bind Error: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.error(ErrorCode.PARAM_ERROR.getCode(), message));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<String>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access Denied: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.error(403, "Access Denied"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<String>> handleAuthenticationException(AuthenticationException e) {
        log.warn("Authentication Error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.error(401, "Authentication Error"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<String>> handleException(Exception e) {
        String traceId = MDC.get("traceId");
        log.error("System Error [TraceId: {}]", traceId, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Result.error(ErrorCode.SYSTEM_ERROR.getCode(), "Internal Server Error. TraceId: " + traceId));
    }
}
