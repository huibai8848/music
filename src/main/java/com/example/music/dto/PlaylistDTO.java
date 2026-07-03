package com.example.music.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建/编辑歌单请求 DTO
 */
@Data
public class PlaylistDTO {

    /** 歌单标题 */
    @NotBlank(message = "歌单标题不能为空")
    @Size(max = 100, message = "歌单标题不能超过 100 个字符")
    private String title;

    /** 歌单描述 */
    @Size(max = 500, message = "歌单描述不能超过 500 个字符")
    private String description;

    /** 封面图 URL */
    private String coverUrl;

    /** 是否公开 */
    private Boolean isPublic;
}