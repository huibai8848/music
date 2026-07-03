package com.example.music.vo;

import com.example.music.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理后台用户展示 VO
 * <p>
 * 管理员查看用户列表时使用，不暴露密码字段。
 */
@Data
public class AdminUserVO {

    private Long id;
    private String email;
    private String nickname;
    private String avatar;
    private String bio;
    private String role;
    private LocalDateTime vipExpireTime;
    private String status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    /**
     * 从 User 实体转换（排除密码字段）
     *
     * @param user 用户实体
     * @return 管理员视图的用户对象
     */
    public static AdminUserVO fromEntity(User user) {
        if (user == null) return null;
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setEmail(user.getEmail());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setBio(user.getBio());
        vo.setRole(user.getRole());
        vo.setVipExpireTime(user.getVipExpireTime());
        vo.setStatus(user.getStatus());
        vo.setCreatedTime(user.getCreatedTime());
        vo.setUpdatedTime(user.getUpdatedTime());
        return vo;
    }
}