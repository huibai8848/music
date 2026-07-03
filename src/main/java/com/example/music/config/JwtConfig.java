package com.example.music.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性
 * <p>
 * 从 application.yml 的 music.jwt 前缀读取。
 * 生产环境应通过环境变量 {@code MUSIC_JWT_SECRET} 注入密钥，避免硬编码。
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "music.jwt")
public class JwtConfig {

    /** 签名密钥（至少 256 位，建议 512 位） */
    private String secret = "YourSuperSecretKeyForJWTTokenGeneration2024MusicPlatform!";

    /** Access Token 过期时间（毫秒，默认 2 小时） */
    private long accessTokenExpiration = 7200000L;

    /** Refresh Token 过期时间（毫秒，默认 7 天） */
    private long refreshTokenExpiration = 604800000L;

    /** 签发者 */
    private String issuer = "music-platform";

    /**
     * 初始化时尝试从环境变量覆盖密钥。
     * 环境变量名：MUSIC_JWT_SECRET
     * 如果未设置则使用 yml 中的默认值（开发环境使用）。
     */
    @PostConstruct
    public void init() {
        String envSecret = System.getenv("MUSIC_JWT_SECRET");
        if (envSecret != null && !envSecret.isEmpty()) {
            this.secret = envSecret;
            log.info("JWT 密钥已从环境变量 MUSIC_JWT_SECRET 加载");
        } else {
            log.warn("JWT 密钥使用配置文件中硬编码的默认值（生产环境建议设置 MUSIC_JWT_SECRET 环境变量）");
        }
    }
}