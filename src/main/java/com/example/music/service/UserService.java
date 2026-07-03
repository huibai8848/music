package com.example.music.service;

import com.example.music.dto.LoginDTO;
import com.example.music.dto.PasswordDTO;
import com.example.music.dto.RegisterDTO;
import com.example.music.entity.User;
import com.example.music.vo.LoginVO;
import com.example.music.vo.UserVO;

/**
 * 用户服务接口
 */
public interface UserService {

    /** 注册 */
    LoginVO register(RegisterDTO dto);

    /** 登录 */
    LoginVO login(LoginDTO dto);

    /** 刷新 Access Token */
    LoginVO refreshToken(String refreshToken);

    /** 登出 */
    void logout(String accessToken);

    /** 获取当前用户信息 */
    UserVO getCurrentUser(Long userId);

    /** 修改个人信息 */
    UserVO updateProfile(Long userId, User user);

    /** 修改密码 */
    void changePassword(Long userId, PasswordDTO dto);

    /** 充值会员 */
    UserVO rechargeMembership(Long userId, String plan, String paymentId);

    /** 发送密码重置验证码 */
    void sendPasswordResetCode(String email);

    /** 验证并重置密码 */
    void resetPassword(String email, String code, String newPassword);

    /** 获取公开主页 */
    UserVO getPublicProfile(Long userId);
}