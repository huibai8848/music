package com.example.music.service.impl;

import com.example.music.entity.OperationLog;
import com.example.music.entity.User;
import com.example.music.mapper.OperationLogMapper;
import com.example.music.mapper.UserMapper;
import com.example.music.service.AdminLogService;
import com.example.music.vo.OperationLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 操作日志服务实现
 * <p>
 * 提供管理员操作日志的分页查询，自动联表查询操作人昵称。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLogServiceImpl implements AdminLogService {

    private final OperationLogMapper operationLogMapper;
    private final UserMapper userMapper;

    @Override
    public Map<String, Object> listLogs(String action, Long operatorId, int page, int size) {
        int offset = (page - 1) * size;

        List<OperationLog> logs = operationLogMapper.selectList(offset, size, action, operatorId);
        long total = operationLogMapper.countList(action, operatorId);

        List<OperationLogVO> voList = logs.stream().map(logEntry -> {
            String nickname = "";
            if (logEntry.getOperatorId() != null) {
                User user = userMapper.selectById(logEntry.getOperatorId());
                nickname = user != null ? user.getNickname() : "未知";
            }
            return OperationLogVO.fromEntity(logEntry, nickname);
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", voList);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }
}
