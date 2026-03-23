package com.liangshou.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置类。
 * <p>用于配置应用程序的安全策略，包括认证、授权、密码编码等。</p>
 * <p>主要配置项包括：
 * <ul>
 *     <li>禁用 CSRF 和 CORS（适用于无状态 API）</li>
 *     <li>配置会话管理为无状态模式</li>
 *     <li>设置公开访问的端点（如认证接口、Swagger 文档）</li>
 *     <li>添加 JWT 认证过滤器</li>
 * </ul>
 * </p>
 *
 * @author liangshou
 * @version 1.0
 * @see SecurityFilterChain
 * @see JwtAuthenticationFilter
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 配置密码编码器。
     * <p>使用 BCrypt 强哈希函数对密码进行加密存储，提高安全性。</p>
     *
     * @return BCrypt 密码编码器对象
     * @see BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置认证管理器。
     * <p>用于处理用户认证逻辑，从 Spring Security 的自动配置中获取认证管理器。</p>
     *
     * @param authConfig Spring Security 认证配置
     * @return 认证管理器对象
     * @throws Exception 配置异常
     * @see AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * 配置安全过滤器链。
     * <p>定义 HTTP 请求的安全规则和过滤器的执行顺序。</p>
     * <p>配置内容包括：
     * <ul>
     *     <li>禁用 CSRF 和 CORS 保护</li>
     *     <li>设置会话创建策略为无状态（STATELESS）</li>
     *     <li>配置请求授权规则：/api/v1/auth/**、Swagger 相关路径允许匿名访问</li>
     *     <li>其他所有请求需要认证</li>
     *     <li>在用户名密码认证过滤器之前添加 JWT 认证过滤器</li>
     * </ul>
     * </p>
     *
     * @param http HttpSecurity 配置对象
     * @return 构建完成的安全过滤器链
     * @throws Exception 配置异常
     * @see SecurityFilterChain
     * @see SessionCreationPolicy#STATELESS
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
