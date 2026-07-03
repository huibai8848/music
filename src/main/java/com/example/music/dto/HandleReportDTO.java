package com.example.music.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 处理举报请求 DTO
 * <p>
 * 管理员在后台处理举报时使用，指定处理结果和备注。
 */
@Data
public class HandleReportDTO {

    /** 处理状态: RESOLVED（已处理）/ DISMISSED（驳回） */
    @NotBlank(message = "处理状态不能为空")
    private String status;

    /** 处理备注（最多 500 字） */
    @Size(max = 500, message = "处理备注不能超过 500 字")
    private String handleNote;
}