package com.example.music.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 审核歌曲请求 DTO
 * <p>
 * 管理员审核会员上传的歌曲时使用。
 * 可审核通过（ACTIVE）或驳回（REJECTED），驳回时可附加原因。
 */
@Data
public class AuditSongDTO {

    /** 审核状态: ACTIVE（通过）/ REJECTED（驳回） */
    @NotBlank(message = "审核状态不能为空")
    private String status;

    /** 驳回原因（审核通过时可为空） */
    @Size(max = 500, message = "驳回原因不能超过 500 字")
    private String rejectReason;
}