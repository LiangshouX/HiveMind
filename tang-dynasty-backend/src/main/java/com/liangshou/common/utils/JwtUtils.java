package com.liangshou.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类。
 * <p>提供 JWT（JSON Web Token）的生成、解析和验证功能。</p>
 * <p>此类为工具类，不可实例化，所有方法均为静态方法。</p>
 * <p>使用 HS256 算法进行签名，默认过期时间为 1 天（86400000 毫秒）。</p>
 *
 * @author liangshou
 * @version 1.0
 * @see io.jsonwebtoken.Jwts
 */
public final class JwtUtils {
    private JwtUtils() {}

    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final long EXPIRATION_TIME = 86400000L; // 1 day

    /**
     * 生成 JWT 令牌。
     * <p>创建一个包含用户名信息的 JWT 令牌，设置签发时间和过期时间。</p>
     *
     * @param username 用户名，将作为 JWT 的主题（subject）
     * @return 生成的 JWT 令牌字符串
     * @see #createToken(Map, String)
     */
    public static String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }

    /**
     * 创建 JWT 令牌。
     * <p>根据提供的声明信息和主题创建 JWT 令牌，设置签发时间、过期时间并使用密钥签名。</p>
     *
     * @param claims JWT 声明信息（自定义数据）
     * @param subject JWT 主题（通常为用户名）
     * @return 生成的 JWT 令牌字符串
     * @see Jwts.Builder
     */
    private static String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY)
                .compact();
    }

    /**
     * 验证 JWT 令牌的有效性。
     * <p>检查令牌是否过期，以及令牌中的用户名是否与提供的用户名匹配。</p>
     *
     * @param token 要验证的 JWT 令牌
     * @param username 用于验证的用户名
     * @return 如果令牌有效且用户名匹配则返回 true，否则返回 false
     * @see #extractUsername(String)
     * @see #isTokenExpired(String)
     */
    public static Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }

    /**
     * 从 JWT 令牌中提取用户名。
     * <p>解析 JWT 令牌并获取其中的主题（subject）字段，即用户名。</p>
     *
     * @param token JWT 令牌
     * @return 提取到的用户名
     * @see Claims#getSubject()
     */
    public static String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * 解析 JWT 令牌获取所有声明信息。
     * <p>使用预设的密钥解析 JWT 令牌，提取其中的所有声明数据。</p>
     *
     * @param token JWT 令牌
     * @return 包含所有声明信息的 Claims 对象
     * @see Claims
     */
    private static Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 判断 JWT 令牌是否已过期。
     * <p>通过比较令牌中的过期时间与当前系统时间来判断。</p>
     *
     * @param token JWT 令牌
     * @return 如果令牌已过期则返回 true，否则返回 false
     * @see Claims#getExpiration()
     */
    private static Boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }
}
