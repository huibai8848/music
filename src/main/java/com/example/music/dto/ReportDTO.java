package com.example.music.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交举报 DTO
 * <p>
 * 前端提交举报时传递的参数。
 */
@Data
public class ReportDTO {

    /** 目标类型: COMMENT / SONG / PLAYLIST */
    @NotBlank(message = "目标类型不能为空")
    private String targetType;

    /** 目标 ID */
    @NotNull(message = "目标 ID 不能为空")
    private Long targetId;

    /** 举报原因: PORNOGRAPHY / AD / ABUSE / COPYRIGHT / OTHER */
    @NotBlank(message = "举报原因不能为空")
    private String reason;

    /** 补充说明（可选，最多 500 字） */
    @Size(max = 500, message = "补充说明不能超过 500 字")
    private String description;
}