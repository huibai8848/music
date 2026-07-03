package com.example.music.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发表评论请求 DTO
 */
@Data
public class CommentDTO {

    /** 评论内容（最长 500 字） */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容不能超过 500 个字符")
    private String content;

    /** 回复的评论 ID（可选，用于楼中楼回复） */
    private Long parentId;
}