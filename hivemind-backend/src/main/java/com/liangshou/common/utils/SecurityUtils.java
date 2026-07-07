package com.liangshou.common.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 安全上下文工具类，用于从 Spring Security Context 中获取当前用户信息。
 * <p>
 * 在需要获取当前用户信息时，使用此类而不是从请求参数或 Body 中获取。
 * 这样可以确保用户信息的安全性，防止水平越权。
 * </p>
 *
 * @author liangshou
 * @version 1.0
 */
public final class SecurityUtils {

    private SecurityUtils() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 获取当前认证用户的用户名。
     * <p>
     * 从 Security Context 中获取 Authentication 对象，
     * 然后提取用户名（Principal）。
     * </p>
     *
     * @return 当前用户的用户名
     * @throws IllegalStateException 如果用户未认证或无法获取用户名
     */
    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("未获取到当前认证用户信息");
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

        throw new IllegalStateException("无法从认证上下文中获取用户标识");
    }

    /**
     * 获取当前的 Authentication 对象。
     * <p>
     * 适用于需要完整 Authentication 信息的场景（如获取权限列表等）。
     * </p>
     *
     * @return 当前的 Authentication 对象
     * @throws IllegalStateException 如果用户未认证
     */
    public static Authentication getCurrentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("未获取到当前认证用户信息");
        }
        return authentication;
    }
}
