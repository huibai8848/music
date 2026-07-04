package com.example.music.service.impl;

import com.example.music.constant.ErrorCode;
import com.example.music.constant.RedisChannels;
import com.example.music.constant.RedisKeys;
import com.example.music.dto.CreateRoomDTO;
import com.example.music.entity.RoomMessage;
import com.example.music.entity.User;
import com.example.music.exception.BusinessException;
import com.example.music.entity.Artist;
import com.example.music.entity.Song;
import com.example.music.mapper.ArtistMapper;
import com.example.music.mapper.FavoriteMapper;
import com.example.music.mapper.PlaylistSongMapper;
import com.example.music.mapper.RoomMessageMapper;
import com.example.music.mapper.SongMapper;
import com.example.music.mapper.UserMapper;
import com.example.music.service.RoomService;
import com.example.music.vo.RoomMemberVO;
import com.example.music.vo.RoomMessageVO;
import com.example.music.vo.RoomVO;
import com.example.music.vo.WebSocketMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 歌房服务实现
 * <p>
 * 房间状态全部存储在 Redis 中：
 * - Hash room:{roomId} → 房间基本状态
 * - Set room:{roomId}:members → 在线成员 ID 集合
 * - String room:{roomId}:queue → 播放队列（JSON 数组）
 * <p>
 * Redis Key 格式见 {@link RedisKeys}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RoomMessageMapper roomMessageMapper;
    private final UserMapper userMapper;
    private final SongMapper songMapper;
    private final ArtistMapper artistMapper;
    private final PlaylistSongMapper playlistSongMapper;
    private final FavoriteMapper favoriteMapper;

    /** 房间 TTL（12 小时无活跃自动过期） */
    private static final long ROOM_TTL_SECONDS = 43200;

    /** 房间成员 TTL */
    private static final long MEMBER_TTL_SECONDS = 43200;

    /** 加入顺序 List key 后缀 */
    private static final String JOIN_ORDER_SUFFIX = ":joinOrder";

    /** Snowflake 工作节点 ID */
    private static final long SNOWFLAKE_WORKER_ID = 1;
    /** Snowflake 数据中心 ID */
    private static final long SNOWFLAKE_DATACENTER_ID = 1;
    private long sequence = 0L;
    private long lastTimestamp = -1L;
    private static final long EPOCH = 1704067200000L;

    // ==================== 房间 CRUD ====================

    @Override
    public RoomVO createRoom(Long userId, CreateRoomDTO dto) {
        // 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 检查角色权限：仅 VIP 和管理员可创建歌房
        String role = user.getRole();
        if (!"VIP".equals(role) && !"ADMIN".equals(role)) {
            throw new BusinessException(ErrorCode.ROOM_PERMISSION_DENIED);
        }

        // 生成房间 ID（Snowflake 算法，保证全局唯一且趋势递增）
        long roomId = nextSnowflakeId();

        // 构建房间 Redis Hash
        Map<String, Object> roomMap = new HashMap<>();
        roomMap.put("name", dto.getName());
        roomMap.put("ownerId", String.valueOf(userId));
        roomMap.put("status", "WAITING");
        roomMap.put("isPublic", dto.getIsPublic() != null && dto.getIsPublic() ? "1" : "0");
        roomMap.put("password", dto.getPassword() != null ? dto.getPassword() : "");
        roomMap.put("currentSongId", "");
        roomMap.put("progress", "0");
        roomMap.put("isPlaying", "0");
        roomMap.put("playMode", "SEQUENCE");
        roomMap.put("maxMembers", String.valueOf(dto.getMaxMembers() != null ? dto.getMaxMembers() : 8));
        roomMap.put("createdAt", String.valueOf(System.currentTimeMillis()));

        String roomKey = RedisKeys.ROOM + roomId;
        stringRedisTemplate.opsForHash().putAll(roomKey, convertToStringMap(roomMap));
        stringRedisTemplate.expire(roomKey, ROOM_TTL_SECONDS, TimeUnit.SECONDS);

        // 将创建者加入成员 Set
        String memberKey = String.format(RedisKeys.ROOM_MEMBERS, roomId);
        stringRedisTemplate.opsForSet().add(memberKey, String.valueOf(userId));
        stringRedisTemplate.expire(memberKey, MEMBER_TTL_SECONDS, TimeUnit.SECONDS);

        // 初始化播放队列
        String queueKey = roomKey + ":queue";
        stringRedisTemplate.opsForValue().set(queueKey, "[]", ROOM_TTL_SECONDS, TimeUnit.SECONDS);

        log.info("创建歌房: roomId={}, name={}, ownerId={}", roomId, dto.getName(), userId);
        return getRoomDetail(roomId);
    }

    @Override
    public List<RoomVO> listRooms() {
        // 使用 SCAN 遍历所有 room:* 前缀的 key（避免 KEYS 命令阻塞 Redis，大数据量时性能显著提升）
        List<RoomVO> rooms = new ArrayList<>();
        String pattern = RedisKeys.ROOM + "*";

        try (Cursor<byte[]> cursor = redisTemplate.getConnectionFactory()
                .getConnection()
                .scan(ScanOptions.scanOptions()
                        .match(pattern)
                        .count(100)
                        .build())) {

            while (cursor.hasNext()) {
                String key = new String(cursor.next());
                // 只处理 room:{数字} 格式，排除 room:{数字}:members / :queue 等
                if (key.matches(RedisKeys.ROOM + "\\d+$")) {
                    Long id = Long.parseLong(key.substring(RedisKeys.ROOM.length()));
                    try {
                        RoomVO vo = getRoomDetail(id);
                        if (vo != null) {
                            rooms.add(vo);
                        }
                    } catch (BusinessException e) {
                        // 房间可能在 SCAN 和 HGETALL 之间被删除，跳过
                        log.debug("歌房已解散，跳过: roomId={}", id);
                    }
                }
            }
        } catch (Exception e) {
            log.error("【歌房】SCAN 遍历房间列表异常", e);
        }

        // 按创建时间降序排列
        rooms.sort((a, b) -> Long.compare(
                b.getCreatedAt() != null ? b.getCreatedAt() : 0,
                a.getCreatedAt() != null ? a.getCreatedAt() : 0
        ));

        return rooms;
    }

    @Override
    public RoomVO getRoomDetail(Long roomId) {
        String roomKey = RedisKeys.ROOM + roomId;
        Map<Object, Object> rawMap = stringRedisTemplate.opsForHash().entries(roomKey);
        if (rawMap == null || rawMap.isEmpty()) {
            throw new BusinessException(ErrorCode.ROOM_NOT_FOUND);
        }

        // 转换 key 为 String
        Map<String, Object> map = new HashMap<>();
        rawMap.forEach((k, v) -> map.put((String) k, v));

        RoomVO vo = RoomVO.fromMap(roomId, map);

        // 补充实时数据
        String memberKey = String.format(RedisKeys.ROOM_MEMBERS, roomId);
        Long memberCount = stringRedisTemplate.opsForSet().size(memberKey);
        vo.setMemberCount(memberCount != null ? memberCount.intValue() : 0);

        // 补充房主昵称
        if (vo.getOwnerId() != null) {
            User owner = userMapper.selectById(vo.getOwnerId());
            if (owner != null) {
                vo.setOwnerNickname(owner.getNickname());
            }
        }

        // 补充当前歌曲信息
        if (vo.getCurrentSongId() != null) {
            Song song = songMapper.selectById(vo.getCurrentSongId());
            if (song != null) {
                vo.setCurrentSongTitle(song.getTitle());
                vo.setCurrentSongCover(song.getCoverUrl());
                // 补充艺人名
                if (song.getArtistId() != null) {
                    Artist artist = artistMapper.selectById(song.getArtistId());
                    if (artist != null) {
                        vo.setCurrentSongArtist(artist.getName());
                    }
                }
            }
        }

        return vo;
    }

    // ==================== 成员管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinRoom(Long userId, Long roomId, String password) {
        String roomKey = RedisKeys.ROOM + roomId;
        String memberKey = String.format(RedisKeys.ROOM_MEMBERS, roomId);

        // 检查房间是否存在
        Boolean exists = stringRedisTemplate.hasKey(roomKey);
        if (Boolean.FALSE.equals(exists)) {
            throw new BusinessException(ErrorCode.ROOM_NOT_FOUND);
        }

        // 检查是否已在房间中
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(memberKey, String.valueOf(userId));
        if (Boolean.TRUE.equals(isMember)) {
            throw new BusinessException(ErrorCode.ROOM_ALREADY_JOINED);
        }

        // 检查密码
        Object pwdObj = stringRedisTemplate.opsForHash().get(roomKey, "password");
        String roomPwd = pwdObj != null ? pwdObj.toString() : "";
        if (!roomPwd.isEmpty()) {
            if (password == null || !password.equals(roomPwd)) {
                throw new BusinessException(ErrorCode.ROOM_PASSWORD_WRONG);
            }
        }

        // 检查房间是否已满
        Long memberCount = stringRedisTemplate.opsForSet().size(memberKey);
        Object maxObj = stringRedisTemplate.opsForHash().get(roomKey, "maxMembers");
        int maxMembers = maxObj != null ? Integer.parseInt(maxObj.toString()) : 8;
        if (memberCount != null && memberCount >= maxMembers) {
            throw new BusinessException(ErrorCode.ROOM_FULL);
        }

        // 加入房间
        stringRedisTemplate.opsForSet().add(memberKey, String.valueOf(userId));
        stringRedisTemplate.expire(memberKey, MEMBER_TTL_SECONDS, TimeUnit.SECONDS);

        // 记录加入顺序
        String joinOrderKey = roomKey + JOIN_ORDER_SUFFIX;
        stringRedisTemplate.opsForList().rightPush(joinOrderKey, String.valueOf(userId));
        stringRedisTemplate.expire(joinOrderKey, MEMBER_TTL_SECONDS, TimeUnit.SECONDS);

        log.info("加入歌房: roomId={}, userId={}", roomId, userId);

        // 发送系统消息
        User user = userMapper.selectById(userId);
        String nickname = user != null ? user.getNickname() : "未知用户";
        saveMessage(roomId, userId, "SYSTEM", nickname + " 加入了房间");

        // 更新 memberCount 冗余字段
        Long newCount = stringRedisTemplate.opsForSet().size(memberKey);
        stringRedisTemplate.opsForHash().put(roomKey, "memberCount", String.valueOf(newCount != null ? newCount : 0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveRoom(Long userId, Long roomId) {
        String roomKey = RedisKeys.ROOM + roomId;
        String memberKey = String.format(RedisKeys.ROOM_MEMBERS, roomId);
        String joinOrderKey = roomKey + JOIN_ORDER_SUFFIX;

        // 移除成员
        stringRedisTemplate.opsForSet().remove(memberKey, String.valueOf(userId));
        // 移除加入顺序记录
        stringRedisTemplate.opsForList().remove(joinOrderKey, 0, String.valueOf(userId));

        // 检查房间是否还有成员
        Long memberCount = stringRedisTemplate.opsForSet().size(memberKey);
        String roomName = getRoomField(roomKey, "name");

        if (memberCount == null || memberCount == 0) {
            // 没人在房间了，自动解散
            stringRedisTemplate.delete(roomKey);
            stringRedisTemplate.delete(memberKey);
            stringRedisTemplate.delete(roomKey + ":queue");
            stringRedisTemplate.delete(joinOrderKey);
            log.info("房间全员离开，自动解散: roomId={}, name={}", roomId, roomName);
        } else {
            // 检查离开者是不是房主
            Object ownerObj = stringRedisTemplate.opsForHash().get(roomKey, "ownerId");
            if (ownerObj != null && ownerObj.toString().equals(String.valueOf(userId))) {
                // 房主离开：直接解散房间，所有成员回到列表页
                stringRedisTemplate.delete(roomKey);
                stringRedisTemplate.delete(memberKey);
                stringRedisTemplate.delete(roomKey + ":queue");
                stringRedisTemplate.delete(joinOrderKey);
                log.info("房主离开，房间解散: roomId={}, name={}", roomId, roomName);
                saveMessage(roomId, 0L, "SYSTEM", "房主已离开，房间已解散");
                // 广播解散通知，让其他成员立即跳转
                broadcastDismiss(roomId);
                return; // 房间已删除，不再执行后续逻辑
            }

            // 更新 memberCount
            Long newCount = stringRedisTemplate.opsForSet().size(memberKey);
            stringRedisTemplate.opsForHash().put(roomKey, "memberCount",
                    String.valueOf(newCount != null ? newCount : 0));
        }

        log.info("离开歌房: roomId={}, userId={}", roomId, userId);

        // 如果房间还在且成员有变动，发送系统消息
        Boolean roomExists = stringRedisTemplate.hasKey(roomKey);
        if (Boolean.TRUE.equals(roomExists)) {
            User user = userMapper.selectById(userId);
            String nickname = user != null ? user.getNickname() : "未知用户";
            saveMessage(roomId, userId, "SYSTEM", nickname + " 离开了房间");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void kickMember(Long operatorId, Long roomId, Long targetId) {
        assertOwner(operatorId, roomId);
        assertNotSelf(operatorId, targetId);

        String memberKey = String.format(RedisKeys.ROOM_MEMBERS, roomId);
        stringRedisTemplate.opsForSet().remove(memberKey, String.valueOf(targetId));

        // 同时从加入顺序列表中移除
        String joinOrderKey = RedisKeys.ROOM + roomId + JOIN_ORDER_SUFFIX;
        stringRedisTemplate.opsForList().remove(joinOrderKey, 0, String.valueOf(targetId));

        // 更新 memberCount
        String roomKey = RedisKeys.ROOM + roomId;
        Long newCount = stringRedisTemplate.opsForSet().size(memberKey);
        stringRedisTemplate.opsForHash().put(roomKey, "memberCount",
                String.valueOf(newCount != null ? newCount : 0));

        log.info("踢出成员: roomId={}, targetId={}, operatorId={}", roomId, targetId, operatorId);

        User target = userMapper.selectById(targetId);
        String targetName = target != null ? target.getNickname() : "未知用户";
        saveMessage(roomId, operatorId, "SYSTEM", targetName + " 被踢出房间");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferOwner(Long operatorId, Long roomId, Long targetId) {
        assertOwner(operatorId, roomId);
        assertNotSelf(operatorId, targetId);

        String roomKey = RedisKeys.ROOM + roomId;
        stringRedisTemplate.opsForHash().put(roomKey, "ownerId", String.valueOf(targetId));

        log.info("移交房主: roomId={}, from={}, to={}", roomId, operatorId, targetId);

        User newOwner = userMapper.selectById(targetId);
        String newOwnerName = newOwner != null ? newOwner.getNickname() : "未知用户";
        saveMessage(roomId, operatorId, "SYSTEM", "房主已移交给 " + newOwnerName);
    }

    @Override
    public void dismissRoom(Long operatorId, Long roomId) {
        assertOwner(operatorId, roomId);

        String roomKey = RedisKeys.ROOM + roomId;
        String memberKey = String.format(RedisKeys.ROOM_MEMBERS, roomId);
        String queueKey = roomKey + ":queue";
        String joinOrderKey = roomKey + JOIN_ORDER_SUFFIX;

        stringRedisTemplate.delete(roomKey);
        stringRedisTemplate.delete(memberKey);
        stringRedisTemplate.delete(queueKey);
        stringRedisTemplate.delete(joinOrderKey);

        // 广播房间解散通知，让其他成员立即跳转
        broadcastDismiss(roomId);

        log.info("解散歌房: roomId={}, operatorId={}", roomId, operatorId);
    }

    /**
     * 广播房间解散消息给所有在线成员
     */
    private void broadcastDismiss(Long roomId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("action", "DISMISS");
            payload.put("roomId", roomId);
            WebSocketMessage msg = WebSocketMessage.builder()
                    .type("SYSTEM")
                    .payload(payload)
                    .timestamp(System.currentTimeMillis())
                    .build();
            String json = objectMapper.writeValueAsString(msg);
            stringRedisTemplate.convertAndSend(RedisChannels.ROOM_MEMBER, json);
            log.info("已广播解散通知: roomId={}", roomId);
        } catch (Exception e) {
            log.error("广播解散通知失败: roomId={}", roomId, e);
        }
    }

    // ==================== 成员列表 ====================

    @Override
    public List<RoomMemberVO> getMembers(Long roomId) {
        String memberKey = String.format(RedisKeys.ROOM_MEMBERS, roomId);
        Set<String> memberIds = stringRedisTemplate.opsForSet().members(memberKey);
        if (memberIds == null || memberIds.isEmpty()) {
            return Collections.emptyList();
        }

        String roomKey = RedisKeys.ROOM + roomId;
        Object ownerObj = stringRedisTemplate.opsForHash().get(roomKey, "ownerId");
        String ownerId = ownerObj != null ? ownerObj.toString() : "";

        List<RoomMemberVO> members = new ArrayList<>();
        for (String idStr : memberIds) {
            try {
                Long uid = Long.parseLong(idStr);
                User user = userMapper.selectById(uid);
                boolean isVip = false;
                if (user != null && "VIP".equals(user.getRole())) {
                    isVip = user.getVipExpireTime() == null
                            || user.getVipExpireTime().isAfter(java.time.LocalDateTime.now());
                }
                members.add(RoomMemberVO.builder()
                        .userId(uid)
                        .nickname(user != null ? user.getNickname() : "未知用户")
                        .avatar(user != null ? user.getAvatar() : null)
                        .isOwner(ownerId.equals(idStr))
                        .isOnline(true)
                        .isVip(isVip)
                        .build());
            } catch (NumberFormatException ignored) {
            }
        }

        return members;
    }

    // ==================== 消息相关 ====================

    @Override
    public List<RoomMessageVO> getRecentMessages(Long roomId, int limit) {
        if (limit <= 0) limit = 50;
        List<RoomMessage> messages = roomMessageMapper.selectRecent(roomId, limit);

        // 收集所有用户 ID 批量查询
        Set<Long> userIds = messages.stream()
                .map(RoomMessage::getUserId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());

        Map<Long, String> nicknameMap = new HashMap<>();
        for (Long uid : userIds) {
            User user = userMapper.selectById(uid);
            nicknameMap.put(uid, user != null ? user.getNickname() : "未知用户");
        }

        // 反转（按时间升序返回）
        List<RoomMessageVO> result = new ArrayList<>();
        for (int i = messages.size() - 1; i >= 0; i--) {
            RoomMessage msg = messages.get(i);
            result.add(RoomMessageVO.fromEntity(msg, nicknameMap.getOrDefault(msg.getUserId(), "未知用户")));
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMessage(Long roomId, Long userId, String type, String content) {
        RoomMessage message = new RoomMessage();
        message.setRoomId(roomId);
        message.setUserId(userId);
        message.setType(type);
        message.setContent(content);
        roomMessageMapper.insert(message);
    }

    // ==================== 播放控制 ====================

    @Override
    public void updatePlayback(Long roomId, Long userId, Boolean isPlaying, Long songId, Integer progress) {
        String roomKey = RedisKeys.ROOM + roomId;
        Boolean exists = stringRedisTemplate.hasKey(roomKey);
        if (Boolean.FALSE.equals(exists)) {
            throw new BusinessException(ErrorCode.ROOM_NOT_FOUND);
        }

        if (isPlaying != null) {
            stringRedisTemplate.opsForHash().put(roomKey, "isPlaying", isPlaying ? "1" : "0");
            stringRedisTemplate.opsForHash().put(roomKey, "status", isPlaying ? "PLAYING" : "PAUSED");
        }
        if (songId != null) {
            stringRedisTemplate.opsForHash().put(roomKey, "currentSongId", String.valueOf(songId));
        }
        if (progress != null) {
            stringRedisTemplate.opsForHash().put(roomKey, "progress", String.valueOf(progress));
        }
    }

    // ==================== 队列管理 ====================

    @Override
    public void addToQueue(Long roomId, Long userId, Long songId) {
        String queueKey = RedisKeys.ROOM + roomId + ":queue";
        String json = stringRedisTemplate.opsForValue().get(queueKey);
        try {
            List<Map<String, Object>> queue;
            if (json == null || json.isEmpty()) {
                queue = new ArrayList<>();
            } else {
                queue = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            }

            // 检查是否已在队列中
            boolean exists = queue.stream()
                    .anyMatch(item -> songId.equals(toLong(item.get("songId"))));
            if (!exists) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("songId", songId);
                entry.put("addedBy", userId);
                queue.add(entry);
                stringRedisTemplate.opsForValue().set(queueKey,
                        objectMapper.writeValueAsString(queue), ROOM_TTL_SECONDS, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.error("添加队列失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "添加队列失败");
        }
    }

    @Override
    public void removeFromQueue(Long roomId, Long userId, Long songId) {
        String queueKey = RedisKeys.ROOM + roomId + ":queue";
        String json = stringRedisTemplate.opsForValue().get(queueKey);
        try {
            if (json == null || json.isEmpty()) return;
            List<Map<String, Object>> queue = objectMapper.readValue(json,
                    new TypeReference<List<Map<String, Object>>>() {});
            queue.removeIf(item -> songId.equals(toLong(item.get("songId"))));
            stringRedisTemplate.opsForValue().set(queueKey,
                    objectMapper.writeValueAsString(queue), ROOM_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("移除队列失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "移除队列失败");
        }
    }

    @Override
    public List<Map<String, Object>> getQueue(Long roomId) {
        String queueKey = RedisKeys.ROOM + roomId + ":queue";
        String json = stringRedisTemplate.opsForValue().get(queueKey);
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> queue = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            // 丰富歌曲信息
            for (Map<String, Object> item : queue) {
                Object songIdObj = item.get("songId");
                if (songIdObj != null) {
                    Long sid = toLong(songIdObj);
                    if (sid != null) {
                        Song song = songMapper.selectById(sid);
                        if (song != null) {
                            item.put("songTitle", song.getTitle());
                            item.put("songArtist", song.getArtistId() != null ?
                                    Optional.ofNullable(artistMapper.selectById(song.getArtistId()))
                                            .map(Artist::getName).orElse(null) : null);
                            item.put("coverUrl", song.getCoverUrl());
                            item.put("duration", song.getDuration());
                            item.put("audioUrl", song.getAudioUrl());
                        } else {
                            item.put("songTitle", "歌曲已删除");
                        }
                    }
                }
            }
            return queue;
        } catch (Exception e) {
            log.error("获取队列失败", e);
            return Collections.emptyList();
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 批量添加歌曲 ID 到房间队列（去重）
     */
    private void batchAddToQueue(Long roomId, List<Long> songIds, Long userId) {
        String queueKey = RedisKeys.ROOM + roomId + ":queue";
        String json = stringRedisTemplate.opsForValue().get(queueKey);
        try {
            List<Map<String, Object>> queue;
            if (json == null || json.isEmpty()) {
                queue = new ArrayList<>();
            } else {
                queue = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            }

            // 收集现有 songId 集合
            Set<Long> existingIds = queue.stream()
                    .map(item -> toLong(item.get("songId")))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            for (Long songId : songIds) {
                if (songId != null && !existingIds.contains(songId)) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("songId", songId);
                    entry.put("addedBy", userId);
                    queue.add(entry);
                    existingIds.add(songId);
                }
            }

            stringRedisTemplate.opsForValue().set(queueKey,
                    objectMapper.writeValueAsString(queue), ROOM_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("批量添加队列失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "批量添加队列失败");
        }
    }

    // ==================== 批量队列加载 ====================

    @Override
    public void addPlaylistToQueue(Long roomId, Long userId, Long playlistId) {
        List<Long> songIds = playlistSongMapper.selectSongIdsByPlaylistId(playlistId);
        if (songIds.isEmpty()) return;
        batchAddToQueue(roomId, songIds, userId);
        log.info("加载歌单到队列: roomId={}, playlistId={}, count={}", roomId, playlistId, songIds.size());
    }

    @Override
    public void addAlbumToQueue(Long roomId, Long userId, Long albumId) {
        List<com.example.music.entity.Song> songs = songMapper.selectAllByAlbumId(albumId);
        List<Long> songIds = songs.stream()
                .map(com.example.music.entity.Song::getId)
                .collect(Collectors.toList());
        if (songIds.isEmpty()) return;
        batchAddToQueue(roomId, songIds, userId);
        log.info("加载专辑到队列: roomId={}, albumId={}, count={}", roomId, albumId, songIds.size());
    }

    @Override
    public void addFavoriteSongsToQueue(Long roomId, Long userId) {
        List<Long> songIds = favoriteMapper.selectSongIdsByUserId(userId);
        if (songIds.isEmpty()) return;
        batchAddToQueue(roomId, songIds, userId);
        log.info("加载收藏歌曲到队列: roomId={}, userId={}, count={}", roomId, userId, songIds.size());
    }

    @Override
    public boolean isOwner(Long roomId, Long userId) {
        String roomKey = RedisKeys.ROOM + roomId;
        String ownerField = getRoomField(roomKey, "ownerId");
        return ownerField != null && ownerField.equals(String.valueOf(userId));
    }

    // ==================== 私有方法 ====================

    /**
     * Snowflake ID 生成器
     * 64-bit: 1bit(0) | 41bit(timestamp) | 10bit(worker) | 12bit(sequence)
     * 保证全局唯一、趋势递增，避免时间戳+随机数的碰撞问题。
     */
    private synchronized long nextSnowflakeId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            log.warn("Snowflake 时钟回拨 {}ms，等待中", offset);
            while (timestamp < lastTimestamp) {
                timestamp = System.currentTimeMillis();
            }
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & 0xFFF;
            if (sequence == 0) {
                while (timestamp <= lastTimestamp) {
                    timestamp = System.currentTimeMillis();
                }
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << 22)
                | (SNOWFLAKE_DATACENTER_ID << 17)
                | (SNOWFLAKE_WORKER_ID << 12)
                | sequence;
    }

    /**
     * 校验操作人是否是房主
     */
    private void assertOwner(Long operatorId, Long roomId) {
        String roomKey = RedisKeys.ROOM + roomId;
        String ownerField = getRoomField(roomKey, "ownerId");
        if (ownerField == null) {
            throw new BusinessException(ErrorCode.ROOM_NOT_FOUND);
        }
        if (!ownerField.equals(String.valueOf(operatorId))) {
            throw new BusinessException(ErrorCode.ROOM_NOT_OWNER);
        }
    }

    /**
     * 校验不是操作自己
     */
    private void assertNotSelf(Long operatorId, Long targetId) {
        if (operatorId.equals(targetId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能操作自己");
        }
    }

    /**
     * 获取 Redis Hash 中某个字段的 String 值
     */
    private String getRoomField(String roomKey, String field) {
        Object val = stringRedisTemplate.opsForHash().get(roomKey, field);
        return val != null ? val.toString() : null;
    }

    /**
     * 将 Map 中所有值转为 String（Redis Hash 需要 String 类型）
     */
    private Map<String, String> convertToStringMap(Map<String, Object> input) {
        Map<String, String> result = new HashMap<>();
        input.forEach((k, v) -> result.put(k, v != null ? v.toString() : ""));
        return result;
    }

    /**
     * 安全转换为 Long
     */
    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(val.toString()); } catch (NumberFormatException e) { return null; }
    }
}
