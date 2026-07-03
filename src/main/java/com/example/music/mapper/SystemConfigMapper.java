package com.example.music.mapper;

import com.example.music.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 系统配置 Mapper
 * <p>
 * 读取 system_config 表的键值对配置。
 * 所有 SQL 均使用 #{ } 预编译，杜绝 SQL 注入风险。
 */
@Mapper
public interface SystemConfigMapper {

    /**
     * 根据配置键查询
     *
     * @param configKey 配置键
     * @return 系统配置实体（不存在时返回 null）
     */
    SystemConfig selectByKey(@Param("configKey") String configKey);
}