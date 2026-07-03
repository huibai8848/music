package com.example.music.vo;

import com.example.music.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论展示 VO
 * <p>
 * 包含评论者的昵称和头像信息，以及子评论列表（楼中楼）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentVO {

    private Long id;
    private Long userId;
    private String nickname;        // 评论者昵称
    private String avatar;          // 评论者头像
    private String targetType;
    private Long targetId;
    private String content;
    private Long parentId;
    private String status;
    private LocalDateTime createdTime;

    /** 子评论列表（楼中楼，最多展示最近 3 条） */
    private List<CommentVO> replies;

    /** 子评论总数（"查看更多 xx 条回复"用） */
    private int replyCount;

    /**
     * 从 Comment 实体构建基本 VO
     */
    public static CommentVO fromEntity(Comment comment) {
        if (comment == null) return null;
        return CommentVO.builder()
                .id(comment.getId())
                .userId(comment.getUserId())
                .targetType(comment.getTargetType())
                .targetId(comment.getTargetId())
                .content(comment.getStatus().equals("DELETED") ? "该评论已删除" : comment.getContent())
                .parentId(comment.getParentId())
                .status(comment.getStatus())
                .createdTime(comment.getCreatedTime())
                .build();
    }
}