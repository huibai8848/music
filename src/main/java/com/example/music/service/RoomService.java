package com.example.music.service;

import com.example.music.dto.CreateRoomDTO;
import com.example.music.vo.RoomMemberVO;
import com.example.music.vo.RoomMessageVO;
import com.example.music.vo.RoomVO;

import java.util.List;
import java.util.Map;

/**
 * 歌房服务接口
 * <p>
 * 歌房模块核心业务逻辑：
 * 1. 房间 CRUD（创建/列表/信息查询）
 * 2. 成员管理（加入/离开/踢人/移交房主/解散）
 * 3. 播放队列管理
 * 4. 消息记录查询
 * <p>
 * 房间状态全部存储在 Redis 中，无 MySQL 持久化。
 */
public interface RoomService {

    /**
     * 创建歌房
     *
     * @param userId 创建者 ID
     * @param dto    创建参数
     * @return 创建的 RoomVO
     */
    RoomVO createRoom(Long userId, CreateRoomDTO dto);

    /**
     * 获取公开房间列表
     *
     * @return 房间列表
     */
    List<RoomVO> listRooms();

    /**
     * 获取房间详情（含当前在线成员）
     *
     * @param roomId 房间 ID
     * @return RoomVO
     */
    RoomVO getRoomDetail(Long roomId);

    /**
     * 加入房间
     *
     * @param userId   加入者 ID
     * @param roomId   房间 ID
     * @param password 房间密码（私密房间需要）
     */
    void joinRoom(Long userId, Long roomId, String password);

    /**
     * 离开房间
     *
     * @param userId 离开者 ID
     * @param roomId 房间 ID
     */
    void leaveRoom(Long userId, Long roomId);

    /**
     * 踢出成员（仅房主可操作）
     *
     * @param operatorId 操作人 ID（房主）
     * @param roomId     房间 ID
     * @param targetId   被踢用户 ID
     */
    void kickMember(Long operatorId, Long roomId, Long targetId);

    /**
     * 移交房主（仅房主可操作）
     *
     * @param operatorId 操作人 ID（房主）
     * @param roomId     房间 ID
     * @param targetId   新房主用户 ID
     */
    void transferOwner(Long operatorId, Long roomId, Long targetId);

    /**
     * 解散房间（仅房主可操作）
     *
     * @param operatorId 操作人 ID（房主）
     * @param roomId     房间 ID
     */
    void dismissRoom(Long operatorId, Long roomId);

    /**
     * 获取房间在线成员列表
     *
     * @param roomId 房间 ID
     * @return 成员列表
     */
    List<RoomMemberVO> getMembers(Long roomId);

    /**
     * 获取房间最近消息
     *
     * @param roomId 房间 ID
     * @param limit  条数限制（默认 50）
     * @return 消息列表
     */
    List<RoomMessageVO> getRecentMessages(Long roomId, int limit);

    /**
     * 保存聊天消息到数据库
     *
     * @param roomId  房间 ID
     * @param userId  发送者 ID
     * @param type    消息类型
     * @param content 消息内容
     */
    void saveMessage(Long roomId, Long userId, String type, String content);

    /**
     * 更新房间播放状态（同步）
     *
     * @param roomId    房间 ID
     * @param isPlaying 是否播放
     * @param songId    当前歌曲 ID
     * @param progress  进度（毫秒）
     */
    void updatePlayback(Long roomId, Long userId, Boolean isPlaying, Long songId, Integer progress);

    /**
     * 添加歌曲到队列
     *
     * @param roomId 房间 ID
     * @param userId 操作者 ID
     * @param songId 歌曲 ID
     */
    void addToQueue(Long roomId, Long userId, Long songId);

    /**
     * 从队列移除歌曲
     *
     * @param roomId 房间 ID
     * @param userId 操作者 ID
     * @param songId 歌曲 ID
     */
    void removeFromQueue(Long roomId, Long userId, Long songId);

    /**
     * 获取房间播放队列
     *
     * @param roomId 房间 ID
     * @return 队列列表 [{songId, addedBy}]
     */
    List<Map<String, Object>> getQueue(Long roomId);
}
