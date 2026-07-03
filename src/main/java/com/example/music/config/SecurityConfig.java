package com.example.music.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 安全配置
 * <p>
 * 提供 BCrypt 密码加密器，用于用户密码的加密存储和校验。
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 强度 4（平衡安全与性能）：强度每 +1 加密耗时约翻倍
        // 强度 10 时登录耗时 ~1s（高并发瓶颈），4 时约 50ms
        // 生产环境建议 6-8 之间
        return new BCryptPasswordEncoder(4);
    }
}