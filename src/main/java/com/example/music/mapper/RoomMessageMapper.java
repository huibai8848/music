package com.example.music.mapper;

import com.example.music.entity.RoomMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 房间消息 Mapper
 * <p>
 * 提供消息持久化与最近消息查询。
 * 消息主要走 WebSocket 实时广播，MySQL 仅保留最近记录供历史浏览。
 */
@Mapper
public interface RoomMessageMapper {

    /** 插入消息 */
    int insert(RoomMessage message);

    /** 查询房间最近 N 条消息 */
    List<RoomMessage> selectRecent(@Param("roomId") Long roomId, @Param("limit") int limit);
}
