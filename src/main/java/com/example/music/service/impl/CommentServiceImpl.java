package com.example.music.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import com.example.music.constant.ErrorCode;
import com.example.music.entity.Comment;
import com.example.music.exception.BusinessException;
import com.example.music.mapper.CommentMapper;
import com.example.music.mapper.UserMapper;
import com.example.music.service.CommentService;
import com.example.music.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 评论服务实现
 * <p>
 * 核心逻辑：
 * 1. XSS 过滤（HTML 转义存储）
 * 2. 楼中楼最多 2 级嵌套
 * 3. 软删除
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final UserMapper userMapper;

    /** 子评论预览条数 */
    private static final int REPLY_PREVIEW_LIMIT = 3;

    @Override
    public Map<String, Object> getComments(String targetType, Long targetId, int page, int size) {
        int offset = (page - 1) * size;

        // 查询一级评论
        List<Comment> comments = commentMapper.selectByTarget(targetType, targetId, offset, size);
        long total = commentMapper.countByTarget(targetType, targetId);

        // 转换 VO，填充用户信息
        List<CommentVO> voList = comments.stream()
                .map(this::toCommentVO)
                .collect(Collectors.toList());

        // 每个一级评论附带最近 3 条子评论
        voList.forEach(vo -> {
            List<Comment> replies = commentMapper.selectReplies(vo.getId(), REPLY_PREVIEW_LIMIT);
            List<CommentVO> replyVOs = replies.stream().map(this::toCommentVO).collect(Collectors.toList());
            vo.setReplies(replyVOs);
            vo.setReplyCount((int) commentMapper.countReplies(vo.getId()));
        });

        Map<String, Object> result = new HashMap<>();
        result.put("records", voList);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommentVO addComment(Long userId, String targetType, Long targetId,
                                String content, Long parentId) {
        // 1. XSS 过滤
        String safeContent = HtmlUtil.filter(content);
        if (StrUtil.isBlank(safeContent)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "评论内容不能为空");
        }

        // 2. 校验 parentId（不超过 2 级嵌套）
        if (parentId != null) {
            Comment parent = commentMapper.selectById(parentId);
            if (parent == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "回复的评论不存在");
            }
            if (parent.getParentId() != null) {
                // 不允许回复子评论的子评论（即只允许 2 级嵌套）
                throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持更深层次的回复");
            }
        }

        // 3. 构建评论
        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setTargetType(targetType.toUpperCase());
        comment.setTargetId(targetId);
        comment.setContent(safeContent);
        comment.setParentId(parentId);
        comment.setStatus("VISIBLE");

        commentMapper.insert(comment);
        log.info("发表评论: userId={}, targetType={}, targetId={}", userId, targetType, targetId);

        return toCommentVO(comment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId, Long userId, boolean isAdmin) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评论不存在");
        }
        // 非管理员只能删除自己的评论
        if (!isAdmin && !comment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        commentMapper.softDelete(commentId, userId);
        log.info("删除评论: id={}, byUserId={}", commentId, userId);
    }

    /**
     * 将 Comment 实体转换为 VO（填充用户昵称和头像）
     */
    private CommentVO toCommentVO(Comment comment) {
        CommentVO vo = CommentVO.fromEntity(comment);
        if (comment.getUserId() != null) {
            var user = userMapper.selectById(comment.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
            }
        }
        return vo;
    }
}
