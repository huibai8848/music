package com.example.music.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 系统公告请求 DTO
 * <p>
 * 管理员创建/更新系统公告时使用。
 */
@Data
public class NoticeDTO {

    /** 公告标题 */
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 200, message = "公告标题不能超过 200 字")
    private String title;

    /** 公告内容 */
    @NotBlank(message = "公告内容不能为空")
    @Size(max = 5000, message = "公告内容不能超过 5000 字")
    private String content;

    /** 公告类型: SYSTEM / MAINTENANCE / ACTIVITY */
    @NotBlank(message = "公告类型不能为空")
    private String type;

    /** 是否启用 */
    private Boolean isActive;
}