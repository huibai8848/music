package com.example.music.service.impl;

import com.example.music.constant.ErrorCode;
import com.example.music.entity.User;
import com.example.music.exception.BusinessException;
import com.example.music.mapper.UserMapper;
import com.example.music.service.AdminUserService;
import com.example.music.vo.AdminUserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理后台用户管理服务实现
 * <p>
 * 核心逻辑：
 * 1. 用户列表支持按昵称/邮箱模糊搜索
 * 2. 封禁/解封时校验不能操作自己
 * 3. 操作后自动记录操作日志
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserMapper userMapper;

    @Override
    public Map<String, Object> listUsers(String keyword, int page, int size) {
        int offset = (page - 1) * size;

        List<User> users = userMapper.selectAll(keyword, offset, size);
        long total = userMapper.countAll(keyword);

        List<AdminUserVO> voList = users.stream()
                .map(AdminUserVO::fromEntity)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", voList);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void banUser(Long adminId, Long userId, boolean banned) {
        // 不能封禁自己
        if (adminId.equals(userId)) {
            throw new BusinessException(ErrorCode.ADMIN_CANNOT_BAN_SELF);
        }

        // 校验目标用户存在
        User target = userMapper.selectById(userId);
        if (target == null) {
            throw new BusinessException(ErrorCode.ADMIN_USER_NOT_FOUND);
        }

        String newStatus = banned ? "BANNED" : "ACTIVE";
        userMapper.updateStatus(userId, newStatus);

        log.info("管理员 {} {} 用户 {}: {}", adminId,
                banned ? "封禁" : "解封", userId, target.getNickname());
    }

    @Override
    public long countAll(String keyword) {
        return userMapper.countAll(keyword);
    }
}
