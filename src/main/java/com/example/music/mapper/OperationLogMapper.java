package com.example.music.mapper;

import com.example.music.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 操作日志 Mapper
 * <p>
 * 提供操作日志的插入和分页查询功能。
 * 由 {@code @OperationLog} 注解 AOP 切面自动调用 insert 方法。
 */
@Mapper
public interface OperationLogMapper {

    /** 根据 ID 查询 */
    OperationLog selectById(@Param("id") Long id);

    /** 分页查询操作日志（按时间降序） */
    List<OperationLog> selectList(@Param("offset") int offset,
                                  @Param("limit") int limit,
                                  @Param("action") String action,
                                  @Param("operatorId") Long operatorId);

    /** 统计总数 */
    long countList(@Param("action") String action,
                   @Param("operatorId") Long operatorId);

    /** 新增操作日志 */
    int insert(OperationLog operationLog);
}
