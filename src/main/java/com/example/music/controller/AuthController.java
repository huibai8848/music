package com.example.music.controller;

import cn.hutool.core.util.StrUtil;
import com.example.music.aspect.RateLimit;
import com.example.music.dto.LoginDTO;
import com.example.music.dto.RefreshTokenDTO;
import com.example.music.dto.RegisterDTO;
import com.example.music.service.UserService;
import com.example.music.utils.RsaUtil;
import com.example.music.vo.LoginVO;
import com.example.music.vo.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * <p>
 * 处理注册、登录、Token 刷新、登出等认证操作。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final RsaUtil rsaUtil;

    /**
     * 获取 RSA 公钥（用于前端加密密码）
     */
    @GetMapping("/public-key")
    public R<Map<String, String>> getPublicKey() {
        Map<String, String> data = new HashMap<>();
        data.put("publicKey", rsaUtil.getPublicKeyBase64());
        return R.ok(data);
    }

    /**
     * 注册
     * 限流：按 IP 限流，每分钟最多 5 次注册请求
     */
    @PostMapping("/register")
    @RateLimit(count = 5, time = 60, key = "register:")
    public R<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        LoginVO result = userService.register(dto);
        return R.ok("注册成功", result);
    }

    /**
     * 登录
     * 支持 RSA 加密密码传输（脱敏）：前端可传入 encryptedPassword，后端自动解密
     * 限流：按 IP 限流，每分钟最多 10 次登录请求
     */
    @PostMapping("/login")
    @RateLimit(count = 10, time = 60, key = "login:")
    public R<LoginVO> login(@RequestBody LoginDTO dto) {
        // 如果前端传了加密密码，自动解密
        if (StrUtil.isNotBlank(dto.getEncryptedPassword())) {
            String decryptedPassword = rsaUtil.decrypt(dto.getEncryptedPassword());
            dto.setPassword(decryptedPassword);
        }
        // 校验密码不为空
        if (StrUtil.isBlank(dto.getPassword())) {
            return R.fail(400, "密码不能为空");
        }
        if (StrUtil.isBlank(dto.getEmail())) {
            return R.fail(400, "邮箱不能为空");
        }
        LoginVO result = userService.login(dto);
        return R.ok("登录成功", result);
    }

    /**
     * 刷新 Access Token
     */
    @PostMapping("/refresh")
    public R<LoginVO> refreshToken(@Valid @RequestBody RefreshTokenDTO dto) {
        LoginVO result = userService.refreshToken(dto.getRefreshToken());
        return R.ok("Token 刷新成功", result);
    }

    /**
     * 登出
     * 将当前 access_token 加入黑名单，并清除 refresh_token
     */
    @PostMapping("/logout")
    public R<Object> logout(HttpServletRequest request) {
        String token = extractToken(request);
        userService.logout(token);
        return R.ok("登出成功");
    }

    /**
     * 发送密码重置验证码
     * POST /api/auth/forgot-password
     * Body: { "email": "user@example.com" }
     */
    @PostMapping("/forgot-password")
    @RateLimit(count = 3, time = 300) // 5分钟内最多3次
    public R<Object> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (StrUtil.isBlank(email)) {
            return R.fail(400, "邮箱不能为空");
        }
        userService.sendPasswordResetCode(email);
        return R.ok("验证码已发送到您的邮箱");
    }

    /**
     * 重置密码（使用验证码）
     * POST /api/auth/reset-password
     * Body: { "email": "user@example.com", "code": "123456", "newPassword": "newPass123" }
     */
    @PostMapping("/reset-password")
    public R<Object> resetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");
        String newPassword = body.get("newPassword");

        if (StrUtil.hasBlank(email, code, newPassword)) {
            return R.fail(400, "邮箱、验证码和新密码不能为空");
        }
        if (newPassword.length() < 6) {
            return R.fail(400, "密码长度不能少于6位");
        }

        userService.resetPassword(email, code, newPassword);
        return R.ok("密码重置成功");
    }

    /**
     * 从请求头中提取 Token
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return authHeader != null ? authHeader : "";
    }
}