package com.example.music.controller;

import com.example.music.dto.CommentDTO;
import com.example.music.service.CommentService;
import com.example.music.utils.RequestContext;
import com.example.music.vo.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 评论控制器
 * <p>
 * 支持对歌曲和歌单发表评论，以及楼中楼回复。
 */
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 获取评论列表（分页）
     *
     * @param type 目标类型: song / playlist
     * @param id   目标 ID
     */
    @GetMapping("/{type}/{id}")
    public R<Map<String, Object>> getComments(
            @PathVariable String type,
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(commentService.getComments(type.toUpperCase(), id, page, size));
    }

    /**
     * 发表评论
     *
     * @param type 目标类型: song / playlist
     * @param id   目标 ID
     */
    @PostMapping("/{type}/{id}")
    public R<Object> addComment(
            @PathVariable String type,
            @PathVariable Long id,
            @Valid @RequestBody CommentDTO dto) {
        Long userId = RequestContext.getUserId();
        commentService.addComment(userId, type.toUpperCase(), id, dto.getContent(), dto.getParentId());
        return R.ok("评论成功");
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/{id}")
    public R<Object> deleteComment(@PathVariable Long id) {
        Long userId = RequestContext.getUserId();
        boolean isAdmin = RequestContext.isAdmin();
        commentService.deleteComment(id, userId, isAdmin);
        return R.ok("删除成功");
    }
}
