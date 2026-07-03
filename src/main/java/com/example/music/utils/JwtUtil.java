package com.example.music.utils;

import com.example.music.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 * <p>
 * 基于 JJWT 0.12.x 库实现，使用 HMAC-SHA256 算法。
 * 提供 access_token 和 refresh_token 的生成、解析、校验功能。
 * <p>
 * Token 载荷结构：
 * <pre>
 * {
 *   "sub": "userId",        // 用户 ID
 *   "role": "USER",         // 用户角色
 *   "jti": "uuid",          // JWT ID（用于黑名单）
 *   "iat": 1234567890,      // 签发时间
 *   "exp": 1234567890       // 过期时间
 * }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtConfig jwtConfig;

    // 缓存 SecretKey 实例（重复解析避免每次都重新生成）
    private SecretKey cachedKey = null;

    /**
     * 获取 HMAC-SHA256 签名密钥
     * 基于配置中的 secret 字符串生成
     */
    private SecretKey getSigningKey() {
        if (cachedKey == null) {
            byte[] keyBytes = jwtConfig.getSecret().getBytes();
            // 确保密钥至少 256 位（32 字节）
            if (keyBytes.length < 32) {
                // 不足时补齐到 32 字节
                byte[] padded = new byte[32];
                System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
                keyBytes = padded;
            }
            cachedKey = Keys.hmacShaKeyFor(keyBytes);
        }
        return cachedKey;
    }

    /**
     * 生成 Access Token（短时效，2 小时）
     *
     * @param userId 用户 ID
     * @param role   用户角色
     * @param jti    JWT ID（用于黑名单）
     * @return JWT token 字符串
     */
    public String generateAccessToken(Long userId, String role, String jti) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtConfig.getAccessTokenExpiration());

        return Jwts.builder()
                .id(jti)                                    // jti: JWT ID
                .subject(String.valueOf(userId))            // sub: 用户 ID
                .claim("role", role)                        // 自定义声明：角色
                .issuer(jwtConfig.getIssuer())              // iss: 签发者
                .issuedAt(now)                              // iat: 签发时间
                .expiration(expiration)                     // exp: 过期时间
                .signWith(getSigningKey())                  // 签名
                .compact();
    }

    /**
     * 生成 Refresh Token（长效，7 天）
     *
     * @param userId 用户 ID
     * @return JWT token 字符串
     */
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtConfig.getRefreshTokenExpiration());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", "refresh")                   // 标记为 refresh_token
                .issuer(jwtConfig.getIssuer())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析 Token，返回 Claims
     *
     * @param token JWT token
     * @return Claims（解析成功时）
     * @throws JwtException 如果 token 无效、过期、签名错误
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())                // 验证签名
                .requireIssuer(jwtConfig.getIssuer())       // 要求正确的签发者
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 校验 Token 是否有效（不抛异常即为有效）
     *
     * @param token JWT token
     * @return true=有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("【JWT 校验失败】{}", e.getMessage());
            return false;
        }
    }

    /**
     * 从 Token 中提取用户 ID
     *
     * @param token JWT token
     * @return 用户 ID（失败时返回 null）
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return Long.valueOf(claims.getSubject());
        } catch (Exception e) {
            log.warn("【JWT 提取用户ID失败】{}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 Token 中提取角色
     *
     * @param token JWT token
     * @return 角色字符串（失败时返回 null）
     */
    public String getRoleFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("role", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 Token 中提取 JTI（JWT ID）
     *
     * @param token JWT token
     * @return JTI（失败时返回 null）
     */
    public String getJtiFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 Token 的剩余有效期（毫秒）
     *
     * @param token JWT token
     * @return 剩余毫秒数（过期或无效时返回 0）
     */
    public long getRemainingTime(String token) {
        try {
            Claims claims = parseToken(token);
            long diff = claims.getExpiration().getTime() - System.currentTimeMillis();
            return Math.max(diff, 0);
        } catch (Exception e) {
            return 0;
        }
    }
}