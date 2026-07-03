package com.example.music.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 歌房成员视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomMemberVO {

    /** 用户 ID */
    private Long userId;

    /** 昵称 */
    private String nickname;

    /** 头像 URL */
    private String avatar;

    /** 是否房主 */
    private Boolean isOwner;

    /** 是否在线 */
    private Boolean isOnline;
}
