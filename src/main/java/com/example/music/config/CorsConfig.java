package com.example.music.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置（CORS）
 * <p>
 * 允许前端开发服务器（如 Vite 默认 5173 端口）跨域访问后端 API。
 * 生产环境请将 allowedOrigins 替换为具体的上线域名，避免安全风险。
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        // 1. 创建 CORS 配置对象
        CorsConfiguration config = new CorsConfiguration();

        // 允许携带凭证（Cookie / Authorization 请求头）
        config.setAllowCredentials(true);

        // 允许的域名（开发环境允许本地前端服务器跨域）
        config.addAllowedOriginPattern("*");

        // 允许的 HTTP 方法
        config.addAllowedMethod("*");

        // 允许的请求头
        config.addAllowedHeader("*");

        // 暴露给前端的响应头（JWT Token 通过 Authorization 头传递）
        config.addExposedHeader("Authorization");

        // 预检请求缓存时间（单位：秒），减少 OPTIONS 请求次数
        config.setMaxAge(3600L);

        // 2. 注册到所有路由
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        // 3. 返回 CorsFilter（优先级高于拦截器，确保 OPTIONS 预检请求能通过）
        return new CorsFilter(source);
    }
}