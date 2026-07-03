package com.example.music.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 歌房视图对象
 * <p>
 * 从 Redis Hash 中读取房间状态，组装为 VO 返回给前端。
 * 不暴露密码等敏感信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomVO {

    /** 房间 ID */
    private Long id;

    /** 房间名称 */
    private String name;

    /** 房主 ID */
    private Long ownerId;

    /** 房主昵称 */
    private String ownerNickname;

    /** 房间状态：WAITING / PLAYING / PAUSED */
    private String status;

    /** 是否公开 */
    private Boolean isPublic;

    /** 是否有密码 */
    private Boolean hasPassword;

    /** 当前歌曲 ID */
    private Long currentSongId;

    /** 当前歌曲标题 */
    private String currentSongTitle;

    /** 当前歌曲封面 */
    private String currentSongCover;

    /** 当前歌曲艺人名 */
    private String currentSongArtist;

    /** 播放进度（毫秒） */
    private Integer progress;

    /** 是否正在播放 */
    private Boolean isPlaying;

    /** 播放模式 */
    private String playMode;

    /** 当前在线成员数 */
    private Integer memberCount;

    /** 最大成员数 */
    private Integer maxMembers;

    /** 创建时间（时间戳） */
    private Long createdAt;

    /**
     * 从 Redis Hash 数据构建 RoomVO
     *
     * @param id  房间 ID
     * @param map Redis Hash 数据
     * @return RoomVO
     */
    @SuppressWarnings("unchecked")
    public static RoomVO fromMap(Long id, Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        return RoomVO.builder()
                .id(id)
                .name((String) map.get("name"))
                .ownerId(toLong(map.get("ownerId")))
                .status((String) map.getOrDefault("status", "WAITING"))
                .isPublic(toBoolean(map.get("isPublic")))
                .hasPassword(map.get("password") != null && !((String) map.get("password")).isEmpty())
                .currentSongId(toLong(map.get("currentSongId")))
                .progress(toInt(map.get("progress")))
                .isPlaying(toBoolean(map.get("isPlaying")))
                .playMode((String) map.getOrDefault("playMode", "SEQUENCE"))
                .memberCount(toInt(map.get("memberCount")))
                .maxMembers(toInt(map.getOrDefault("maxMembers", 8)))
                .createdAt(toLong(map.get("createdAt")))
                .build();
    }

    private static Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(val.toString()); } catch (NumberFormatException e) { return null; }
    }

    private static Integer toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (NumberFormatException e) { return 0; }
    }

    private static Boolean toBoolean(Object val) {
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        return "true".equalsIgnoreCase(val.toString()) || "1".equals(val.toString());
    }
}
