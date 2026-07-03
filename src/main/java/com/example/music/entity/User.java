package com.example.music.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
public class User {
    /** 主键 ID */
    private Long id;
    /** 邮箱（登录账号） */
    private String email;
    /** BCrypt 加密密码 */
    private String password;
    /** 昵称 */
    private String nickname;
    /** 头像 URL */
    private String avatar;
    /** 个人简介 */
    private String bio;
    /** 主页背景图 URL */
    private String background;
    /** 角色: USER / VIP / ADMIN */
    private String role;
    /** 会员过期时间 */
    private LocalDateTime vipExpireTime;
    /** 状态: ACTIVE / BANNED */
    private String status;
    /** 创建时间 */
    private LocalDateTime createdTime;
    /** 更新时间 */
    private LocalDateTime updatedTime;
}