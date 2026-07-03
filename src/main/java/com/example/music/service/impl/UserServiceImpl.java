package com.example.music.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.example.music.constant.ErrorCode;
import com.example.music.constant.RedisKeys;
import com.example.music.dto.LoginDTO;
import com.example.music.dto.PasswordDTO;
import com.example.music.dto.RegisterDTO;
import com.example.music.entity.RechargeRecord;
import com.example.music.entity.User;
import com.example.music.exception.BusinessException;
import com.example.music.mapper.RechargeRecordMapper;
import com.example.music.mapper.UserMapper;
import com.example.music.service.UserService;
import com.example.music.utils.CacheUtil;
import com.example.music.utils.JwtUtil;
import com.example.music.vo.LoginVO;
import com.example.music.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CacheUtil cacheUtil;
    private final RechargeRecordMapper rechargeRecordMapper;

    // ==================== 注册 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(RegisterDTO dto) {
        // 1. 检查邮箱是否已注册
        User exist = userMapper.selectByEmail(dto.getEmail());
        if (exist != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        // 2. 创建用户
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setRole("USER");
        user.setStatus("ACTIVE");

        userMapper.insert(user);

        // 3. 返回用户信息（不生成 Token，注册后需要重新登录）
        return LoginVO.builder()
                .user(UserVO.fromEntity(user))
                .build();
    }

    // ==================== 登录 ====================

    @Override
    public LoginVO login(LoginDTO dto) {
        // 1. 查询用户
        User user = userMapper.selectByEmail(dto.getEmail());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_LOGIN_FAILED);
        }

        // 2. 校验密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.USER_LOGIN_FAILED);
        }

        // 3. 检查是否被封禁
        if ("BANNED".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_BANNED);
        }

        // 4. 生成 Token
        return generateLoginVO(user);
    }

    // ==================== 刷新 Token ====================

    @Override
    public LoginVO refreshToken(String refreshToken) {
        // 1. 校验 refresh_token 合法性
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        // 2. 提取用户 ID
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        if (userId == null) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        // 3. 检查 Redis 中的 refresh_token 是否被吊销
        String key = RedisKeys.REFRESH_TOKEN + userId;
        String storedToken = cacheUtil.get(key);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        // 4. 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if ("BANNED".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_BANNED);
        }

        // 5. 生成新的 access_token（refresh_token 不变）
        String jti = IdUtil.fastSimpleUUID();
        String newAccessToken = jwtUtil.generateAccessToken(userId, user.getRole(), jti);

        return LoginVO.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .user(UserVO.fromEntity(user))
                .build();
    }

    // ==================== 登出 ====================

    @Override
    public void logout(String accessToken) {
        // 1. 解析 JTI
        String jti = jwtUtil.getJtiFromToken(accessToken);
        if (jti == null) return;

        // 2. 获取 token 剩余有效期
        long remaining = jwtUtil.getRemainingTime(accessToken);
        if (remaining <= 0) return;

        // 3. 加入黑名单
        String blacklistKey = RedisKeys.BL_TOKEN + jti;
        cacheUtil.set(blacklistKey, "1", remaining, TimeUnit.MILLISECONDS);

        // 4. 删除 refresh_token
        Long userId = jwtUtil.getUserIdFromToken(accessToken);
        if (userId != null) {
            cacheUtil.delete(RedisKeys.REFRESH_TOKEN + userId);
        }

        log.info("【用户登出】userId={}, jti={}", userId, jti);
    }

    // ==================== 获取当前用户 ====================

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if ("BANNED".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_BANNED);
        }
        return UserVO.fromEntity(user);
    }

    // ==================== 修改个人信息 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateProfile(Long userId, User updateUser) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 只更新非空字段
        user.setNickname(updateUser.getNickname() != null ? updateUser.getNickname() : user.getNickname());
        user.setAvatar(updateUser.getAvatar() != null ? updateUser.getAvatar() : user.getAvatar());
        user.setBio(updateUser.getBio() != null ? updateUser.getBio() : user.getBio());
        user.setBackground(updateUser.getBackground() != null ? updateUser.getBackground() : user.getBackground());

        userMapper.update(user);
        return UserVO.fromEntity(user);
    }

    // ==================== 修改密码 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, PasswordDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 校验旧密码
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.USER_OLD_PASSWORD_WRONG);
        }

        // 更新新密码
        userMapper.updatePassword(userId, passwordEncoder.encode(dto.getNewPassword()));
    }

    // ==================== 忘记密码 ====================

    /** 密码重置验证码 Redis 前缀 */
    private static final String PWD_RESET_CODE_PREFIX = "pwd_reset_code:";
    /** 验证码过期时间（5 分钟） */
    private static final long PWD_RESET_CODE_TTL = 300;

    @Override
    public void sendPasswordResetCode(String email) {
        // 1. 检查邮箱是否注册
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_EMAIL_NOT_FOUND);
        }

        // 2. 生成 6 位验证码
        String code = RandomUtil.randomNumbers(6);
        String key = PWD_RESET_CODE_PREFIX + email;

        // 3. 存入 Redis（5 分钟有效期）
        cacheUtil.set(key, code, PWD_RESET_CODE_TTL, TimeUnit.SECONDS);
        log.info("【密码重置】验证码已发送: email={}, code={}", email, code);

        // TODO: 集成邮件服务后，将验证码通过邮件发送
        // 当前开发阶段仅日志输出，后续接入 EmailService.send(email, "验证码", code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String email, String code, String newPassword) {
        // 1. 校验验证码
        String key = PWD_RESET_CODE_PREFIX + email;
        String storedCode = cacheUtil.get(key);
        if (storedCode == null || !storedCode.equals(code)) {
            throw new BusinessException(ErrorCode.USER_VERIFICATION_CODE_WRONG);
        }

        // 2. 查询用户
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_EMAIL_NOT_FOUND);
        }

        // 3. 更新密码
        userMapper.updatePassword(user.getId(), passwordEncoder.encode(newPassword));

        // 4. 删除已使用的验证码
        cacheUtil.delete(key);
        log.info("【密码重置】密码重置成功: email={}", email);
    }

    // ==================== 充值会员 ====================

    /** 是否跳过支付校验（开发模式跳过，生产环境需集成支付网关） */
    @org.springframework.beans.factory.annotation.Value("${music.membership.skip-payment:true}")
    private boolean skipPayment;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO rechargeMembership(Long userId, String plan, String paymentId) {
        // 生产环境下需校验支付凭证
        if (!skipPayment && (paymentId == null || paymentId.isEmpty())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "支付凭证不能为空，请先完成支付");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 计算套餐天数和金额
        int days;
        BigDecimal amount;
        switch (plan.toUpperCase()) {
            case "MONTHLY":
                days = 30;
                amount = new BigDecimal("15.00");
                break;
            case "QUARTERLY":
                days = 90;
                amount = new BigDecimal("40.00");
                break;
            case "YEARLY":
                days = 365;
                amount = new BigDecimal("120.00");
                break;
            default:
                throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的套餐类型");
        }

        // 计算新过期时间（续费在原有基础上叠加）
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newExpire;
        if (user.getVipExpireTime() != null && user.getVipExpireTime().isAfter(now)) {
            newExpire = user.getVipExpireTime().plusDays(days);
        } else {
            newExpire = now.plusDays(days);
        }

        // 更新会员状态
        userMapper.updateVip(userId, "VIP", newExpire);
        user.setRole("VIP");
        user.setVipExpireTime(newExpire);

        // 写入充值记录
        RechargeRecord record = new RechargeRecord();
        record.setUserId(userId);
        record.setPlan(plan.toUpperCase());
        record.setAmount(amount);
        record.setDurationDays(days);
        record.setExpireTime(newExpire);
        record.setStatus("SUCCESS");
        rechargeRecordMapper.insert(record);

        log.info("会员充值成功: userId={}, plan={}, amount={}, expireTime={}", userId, plan, amount, newExpire);

        return UserVO.fromEntity(user);
    }

    // ==================== 公开主页 ====================

    @Override
    public UserVO getPublicProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        // 公开主页返回基本信息
        return UserVO.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .background(user.getBackground())
                .build();
    }

    // ==================== 私有方法 ====================

    /**
     * 生成登录响应 VO（包含 tokens + 用户信息）
     */
    private LoginVO generateLoginVO(User user) {
        String jti = IdUtil.fastSimpleUUID();
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole(), jti);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // refresh_token 存入 Redis（有效期 7 天）
        String refreshKey = RedisKeys.REFRESH_TOKEN + user.getId();
        cacheUtil.set(refreshKey, refreshToken, 7, TimeUnit.DAYS);

        return LoginVO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(UserVO.fromEntity(user))
                .build();
    }
}