package com.example.music.mapper;

import com.example.music.entity.SystemNotice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统公告 Mapper
 */
@Mapper
public interface SystemNoticeMapper {

    /** 根据 ID 查询 */
    SystemNotice selectById(@Param("id") Long id);

    /** 分页查询公告（按时间降序） */
    List<SystemNotice> selectList(@Param("offset") int offset,
                                  @Param("limit") int limit,
                                  @Param("type") String type);

    /** 统计总数 */
    long countList(@Param("type") String type);

    /** 查询所有已启用的公告（前台展示用） */
    List<SystemNotice> selectActive();

    /** 新增公告 */
    int insert(SystemNotice notice);

    /** 更新公告 */
    int update(SystemNotice notice);

    /** 删除公告 */
    int deleteById(@Param("id") Long id);
}
