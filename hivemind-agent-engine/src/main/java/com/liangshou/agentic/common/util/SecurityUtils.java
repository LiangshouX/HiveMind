package com.liangshou.agentic.common.util;

import com.liangshou.agentic.common.exceptions.BizException;
import com.liangshou.agentic.common.exceptions.HmeErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 安全上下文工具类，用于从 Spring Security Context 中获取当前用户信息。
 * 
 * <p>在需要获取当前用户信息时，使用此类而不是从请求参数或 Body 中获取。
 * 这样可以确保用户信息的安全性，防止水平越权。</p>
 */
public final class SecurityUtils {

    private SecurityUtils() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 获取当前认证用户的用户名（作为用户ID）。
     * 
     * @return 当前用户的用户ID
     * @throws IllegalStateException 如果用户未认证或无法获取用户名
     */
    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new BizException(HmeErrorCode.AGENT_USER_NOT_FOUND);
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }

        if (principal instanceof String) {
            return (String) principal;
        }

        if (principal != null) {
            return principal.toString();
        }

        throw new BizException(HmeErrorCode.AGENT_USER_NOT_FOUND);
    }
}
