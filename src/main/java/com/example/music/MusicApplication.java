package com.example.music;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 在线音乐平台 — 后端主启动类
 *
 * 技术栈：Spring Boot 3.2 + Java 17 + MyBatis + MySQL + Redis + WebSocket
 *
 * 包结构说明：
 * ├── controller/    — REST API 控制器
 * ├── service/       — 业务逻辑接口 + impl 实现
 * ├── mapper/        — MyBatis Mapper 接口（DAO 层）
 * ├── entity/        — 数据库实体类
 * ├── dto/           — 请求数据传输对象
 * ├── vo/            — 响应值对象
 * ├── config/        — 配置类（Redis/CORS/WebSocket 等）
 * ├── utils/         — 工具类
 * ├── aspect/        — AOP 切面（日志/限流/鉴权）
 * ├── exception/     — 自定义异常 + 全局异常处理
 * └── constant/      — 常量定义
 *
 * @EnableScheduling 开启定时任务支持（用于播放计数落库等定时任务）
 * @EnableAsync      开启异步方法支持（用于操作日志异步写入）
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class MusicApplication {

    public static void main(String[] args) {
        SpringApplication.run(MusicApplication.class, args);
    }

}