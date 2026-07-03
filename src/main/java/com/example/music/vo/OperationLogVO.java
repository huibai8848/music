package com.example.music.vo;

import com.example.music.entity.OperationLog;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志展示 VO
 */
@Data
public class OperationLogVO {

    private Long id;
    private Long operatorId;
    private String operatorNickname;
    private String action;
    private String targetType;
    private Long targetId;
    private String detail;
    private String ip;
    private LocalDateTime createdTime;

    /**
     * 从 OperationLog 实体转换
     *
     * @param log             操作日志实体
     * @param operatorNickname 操作人昵称（联表查询后注入）
     * @return 操作日志 VO
     */
    public static OperationLogVO fromEntity(OperationLog log, String operatorNickname) {
        if (log == null) return null;
        OperationLogVO vo = new OperationLogVO();
        vo.setId(log.getId());
        vo.setOperatorId(log.getOperatorId());
        vo.setOperatorNickname(operatorNickname);
        vo.setAction(log.getAction());
        vo.setTargetType(log.getTargetType());
        vo.setTargetId(log.getTargetId());
        vo.setDetail(log.getDetail());
        vo.setIp(log.getIp());
        vo.setCreatedTime(log.getCreatedTime());
        return vo;
    }
}