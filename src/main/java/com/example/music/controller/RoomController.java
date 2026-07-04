package com.example.music.controller;

import com.example.music.dto.CreateRoomDTO;
import com.example.music.dto.JoinRoomDTO;
import com.example.music.dto.RoomQueueDTO;
import com.example.music.service.RoomService;
import com.example.music.utils.RequestContext;
import com.example.music.vo.RoomMemberVO;
import com.example.music.vo.RoomMessageVO;
import com.example.music.vo.RoomVO;
import com.example.music.vo.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 歌房 REST 控制器
 * <p>
 * 提供歌房模块的 REST API：
 * - 房间 CRUD（创建/列表/详情）
 * - 成员管理（加入/离开/踢人/移交房主/解散）
 * - 队列管理（查看/添加/移除）
 * - 消息查询
 * <p>
 * WebSocket 连接走 /ws/room/{roomId}?token=xxx，见 {@link com.example.music.config.WebSocketConfig}。
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    // ==================== 房间管理 ====================

    /**
     * 创建歌房
     *
     * @param dto 创建参数（名称、公开/私密、密码、最大人数）
     * @return 创建的 RoomVO
     */
    @PostMapping
    public R<RoomVO> createRoom(@Valid @RequestBody CreateRoomDTO dto) {
        Long userId = RequestContext.getUserId();
        RoomVO room = roomService.createRoom(userId, dto);
        return R.ok("歌房创建成功", room);
    }

    /**
     * 获取公开房间列表
     * <p>
     * 返回所有公开房间，按创建时间降序。
     * 包含实时在线人数和当前播放状态。
     */
    @GetMapping
    public R<List<RoomVO>> listRooms() {
        return R.ok(roomService.listRooms());
    }

    /**
     * 获取房间详情
     * <p>
     * 包含房间信息、当前在线成员列表。
     *
     * @param id 房间 ID
     */
    @GetMapping("/{id}")
    public R<RoomVO> getRoomDetail(@PathVariable Long id) {
        return R.ok(roomService.getRoomDetail(id));
    }

    // ==================== 成员管理 ====================

    /**
     * 加入房间
     *
     * @param id  房间 ID
     * @param dto 加入参数（私密房间需要密码）
     */
    @PostMapping("/{id}/join")
    public R<Object> joinRoom(@PathVariable Long id,
                               @RequestBody(required = false) JoinRoomDTO dto) {
        Long userId = RequestContext.getUserId();
        String password = dto != null ? dto.getPassword() : null;
        roomService.joinRoom(userId, id, password);
        return R.ok("加入房间成功");
    }

    /**
     * 离开房间
     *
     * @param id 房间 ID
     */
    @PostMapping("/{id}/leave")
    public R<Object> leaveRoom(@PathVariable Long id) {
        Long userId = RequestContext.getUserId();
        roomService.leaveRoom(userId, id);
        return R.ok("已离开房间");
    }

    /**
     * 踢出成员（仅房主）
     *
     * @param id       房间 ID
     * @param targetId 被踢用户 ID
     */
    @PostMapping("/{id}/kick/{targetId}")
    public R<Object> kickMember(@PathVariable Long id, @PathVariable Long targetId) {
        Long userId = RequestContext.getUserId();
        roomService.kickMember(userId, id, targetId);
        return R.ok("成员已踢出");
    }

    /**
     * 移交房主（仅房主）
     *
     * @param id       房间 ID
     * @param targetId 新房主 ID
     */
    @PostMapping("/{id}/transfer/{targetId}")
    public R<Object> transferOwner(@PathVariable Long id, @PathVariable Long targetId) {
        Long userId = RequestContext.getUserId();
        roomService.transferOwner(userId, id, targetId);
        return R.ok("房主已移交");
    }

    /**
     * 解散房间（仅房主）
     *
     * @param id 房间 ID
     */
    @PostMapping("/{id}/dismiss")
    public R<Object> dismissRoom(@PathVariable Long id) {
        Long userId = RequestContext.getUserId();
        roomService.dismissRoom(userId, id);
        return R.ok("房间已解散");
    }

    // ==================== 成员与消息查询 ====================

    /**
     * 获取房间在线成员列表
     *
     * @param id 房间 ID
     */
    @GetMapping("/{id}/members")
    public R<List<RoomMemberVO>> getMembers(@PathVariable Long id) {
        return R.ok(roomService.getMembers(id));
    }

    /**
     * 获取房间最近消息
     *
     * @param id    房间 ID
     * @param limit 条数（默认 50）
     */
    @GetMapping("/{id}/messages")
    public R<List<RoomMessageVO>> getMessages(@PathVariable Long id,
                                               @RequestParam(defaultValue = "50") int limit) {
        return R.ok(roomService.getRecentMessages(id, limit));
    }

    // ==================== 播放队列管理 ====================

    /**
     * 获取房间播放队列
     *
     * @param id 房间 ID
     */
    @GetMapping("/{id}/queue")
    public R<List<Map<String, Object>>> getQueue(@PathVariable Long id) {
        return R.ok(roomService.getQueue(id));
    }

    /**
     * 添加歌曲到队列
     *
     * @param id  房间 ID
     * @param dto 歌曲信息
     */
    @PostMapping("/{id}/queue")
    public R<Object> addToQueue(@PathVariable Long id, @Valid @RequestBody RoomQueueDTO dto) {
        Long userId = RequestContext.getUserId();
        roomService.addToQueue(id, userId, dto.getSongId());
        return R.ok("已添加到队列");
    }

    /**
     * 从队列移除歌曲
     *
     * @param id  房间 ID
     * @param dto 歌曲信息
     */
    @DeleteMapping("/{id}/queue")
    public R<Object> removeFromQueue(@PathVariable Long id, @Valid @RequestBody RoomQueueDTO dto) {
        Long userId = RequestContext.getUserId();
        roomService.removeFromQueue(id, userId, dto.getSongId());
        return R.ok("已从队列移除");
    }

    /**
     * 加载歌单所有歌曲到房间队列（仅房主）
     *
     * @param id         房间 ID
     * @param playlistId 歌单 ID
     */
    @PostMapping("/{id}/queue/playlist/{playlistId}")
    public R<Object> addPlaylistToQueue(@PathVariable Long id, @PathVariable Long playlistId) {
        Long userId = RequestContext.getUserId();
        roomService.addPlaylistToQueue(id, userId, playlistId);
        return R.ok("歌单歌曲已添加到队列");
    }

    /**
     * 加载专辑所有歌曲到房间队列（仅房主）
     *
     * @param id      房间 ID
     * @param albumId 专辑 ID
     */
    @PostMapping("/{id}/queue/album/{albumId}")
    public R<Object> addAlbumToQueue(@PathVariable Long id, @PathVariable Long albumId) {
        Long userId = RequestContext.getUserId();
        roomService.addAlbumToQueue(id, userId, albumId);
        return R.ok("专辑歌曲已添加到队列");
    }

    /**
     * 加载用户收藏的歌曲到房间队列（仅房主）
     *
     * @param id 房间 ID
     */
    @PostMapping("/{id}/queue/favorites")
    public R<Object> addFavoriteSongsToQueue(@PathVariable Long id) {
        Long userId = RequestContext.getUserId();
        roomService.addFavoriteSongsToQueue(id, userId);
        return R.ok("收藏歌曲已添加到队列");
    }
}
