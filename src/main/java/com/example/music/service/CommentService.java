package com.example.music.service;

import com.example.music.vo.CommentVO;

import java.util.List;
import java.util.Map;

/**
 * 评论服务接口
 */
public interface CommentService {

    /** 分页获取某个目标的评论列表（含子评论预览） */
    Map<String, Object> getComments(String targetType, Long targetId, int page, int size);

    /** 发表评论 */
    CommentVO addComment(Long userId, String targetType, Long targetId, String content, Long parentId);

    /** 删除评论（本人或管理员） */
    void deleteComment(Long commentId, Long userId, boolean isAdmin);
}
