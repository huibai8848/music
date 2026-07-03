package com.example.music.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统配置实体
 * <p>
 * 对应数据库表 {@code system_config}，提供键值对形式的系统配置存储。
 * 配置内容通过 Redis 缓存减少数据库查询压力。
 */
@Data
public class SystemConfig {

    /** 主键 ID */
    private Long id;

    /** 配置键（唯一索引） */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 配置说明 */
    private String description;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}