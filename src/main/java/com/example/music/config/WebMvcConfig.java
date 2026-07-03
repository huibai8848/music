package com.example.music.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web MVC 配置
 * <p>
 * 注册 JWT 鉴权拦截器，配置静态资源映射（文件存储访问）。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    /** 文件存储根路径（从配置注入） */
    @Value("${music.file.upload-dir:data/music}")
    private String uploadDir;

    @jakarta.annotation.PostConstruct
    public void init() {
        // 确保 uploadDir 是绝对路径
        File dir = new File(uploadDir);
        if (!dir.isAbsolute()) {
            uploadDir = new File(System.getProperty("user.dir"), uploadDir).getAbsolutePath();
        }
        log.info("静态资源映射根目录: {}", uploadDir);
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WebMvcConfig.class);

    /**
     * 注册拦截器
     * 拦截 /api/** 路径，由 JwtInterceptor 处理白名单和鉴权逻辑
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/error");
    }

    /**
     * 配置静态资源映射
     * <p>
     * - /data/music/**  → 兼容旧路径的文件访问
     * - /api/files/**   → 文件模块的标准访问路径（主要使用）
     * <p>
     * 两者指向同一目录，保证新旧路径均可访问。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = "file:" + uploadDir.replace("\\", "/") + "/";
        registry.addResourceHandler("/data/music/**")
                .addResourceLocations(uploadPath);

        registry.addResourceHandler("/api/files/**")
                .addResourceLocations(uploadPath);
    }
}