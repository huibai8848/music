package com.example.music.mapper;

import com.example.music.entity.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 举报 Mapper
 * <p>
 * 支持提交举报、按举报人查询、按状态查询（管理端）。
 */
@Mapper
public interface ReportMapper {

    /** 根据 ID 查询 */
    Report selectById(@Param("id") Long id);

    /** 查询某个举报人最近一次对某目标的举报（用于 24h 重复判断） */
    Report selectLatestByReporterAndTarget(@Param("reporterId") Long reporterId,
                                           @Param("targetType") String targetType,
                                           @Param("targetId") Long targetId);

    /** 查询某个用户的举报记录（按时间降序） */
    List<Report> selectByReporterId(@Param("reporterId") Long reporterId,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit);

    /** 统计某个用户的举报总数 */
    long countByReporterId(@Param("reporterId") Long reporterId);

    /** 按状态查询所有举报（管理端，分页） */
    List<Report> selectByStatus(@Param("status") String status,
                                @Param("offset") int offset,
                                @Param("limit") int limit);

    /** 统计各状态的举报数量 */
    long countByStatus(@Param("status") String status);

    /** 提交举报 */
    int insert(Report report);

    /** 处理举报（管理端） */
    int handle(@Param("id") Long id,
               @Param("status") String status,
               @Param("handlerId") Long handlerId,
               @Param("handleNote") String handleNote,
               @Param("handledTime") LocalDateTime handledTime);
}