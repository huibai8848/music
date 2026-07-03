package com.example.music.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求 DTO
 * <p>
 * 支持 RSA 加密传输密码：前端可传入 encryptedPassword 代替 password，
 * 后端会自动解密后赋值给 password 字段。
 */
@Data
public class LoginDTO {

    @NotBlank(message = "邮箱不能为空")
    private String email;

    private String password;

    /** RSA 加密后的密码（可选，用于前端脱敏传输） */
    private String encryptedPassword;
}