package com.example.music.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户信息 VO（脱敏，不返回密码等敏感字段）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {
    private Long id;
    private String email;
    private String nickname;
    private String avatar;
    private String bio;
    private String background;
    private String role;
    /** 是否为 VIP（JSON 输出为 "vip" 字段） */
    private Boolean vip;
    private LocalDateTime vipExpireTime;
    private String status;
    private LocalDateTime createdTime;

    /**
     * 从 User 实体构建 VO
     */
    public static UserVO fromEntity(com.example.music.entity.User user) {
        return UserVO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .background(user.getBackground())
                .role(user.getRole())
                .vip("VIP".equals(user.getRole())
                        && user.getVipExpireTime() != null
                        && user.getVipExpireTime().isAfter(LocalDateTime.now()))
                .vipExpireTime(user.getVipExpireTime())
                .status(user.getStatus())
                .createdTime(user.getCreatedTime())
                .build();
    }
}