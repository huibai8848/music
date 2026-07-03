package com.example.music.mapper;

import com.example.music.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通知 Mapper
 * <p>
 * 支持按用户分页查询、标记已读、未读计数。
 */
@Mapper
public interface NotificationMapper {

    /** 根据 ID 查询 */
    Notification selectById(@Param("id") Long id);

    /** 查询某个用户的通知列表（按时间降序） */
    List<Notification> selectByUserId(@Param("userId") Long userId,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);

    /** 统计某个用户的通知总数 */
    long countByUserId(@Param("userId") Long userId);

    /** 统计某个用户的未读通知数 */
    long countUnread(@Param("userId") Long userId);

    /** 插入通知 */
    int insert(Notification notification);

    /** 标记单条通知为已读（需校验接收者身份） */
    int markRead(@Param("id") Long id, @Param("userId") Long userId);

    /** 标记某个用户所有通知为已读 */
    int markAllRead(@Param("userId") Long userId);

    /** 批量插入通知（系统公告群发） */
    int insertBatch(@Param("list") List<Notification> list);
}