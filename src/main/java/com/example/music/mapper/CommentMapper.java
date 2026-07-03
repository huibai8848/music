package com.example.music.mapper;

import com.example.music.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评论 Mapper
 * <p>
 * 支持多态查询（按 targetType + targetId）、楼中楼查询、软删除。
 */
@Mapper
public interface CommentMapper {

    /** 根据 ID 查询 */
    Comment selectById(@Param("id") Long id);

    /** 查询某个目标对象的一级评论（parentId IS NULL），按时间降序 */
    List<Comment> selectByTarget(@Param("targetType") String targetType,
                                 @Param("targetId") Long targetId,
                                 @Param("offset") int offset,
                                 @Param("limit") int limit);

    /** 统计某个目标对象的评论总数 */
    long countByTarget(@Param("targetType") String targetType,
                       @Param("targetId") Long targetId);

    /** 查询某条评论的子评论（楼中楼），最近 3 条 */
    List<Comment> selectReplies(@Param("parentId") Long parentId,
                                @Param("limit") int limit);

    /** 统计子评论总数 */
    long countReplies(@Param("parentId") Long parentId);

    /** 发表评论 */
    int insert(Comment comment);

    /** 软删除（将 status 设为 DELETED） */
    int softDelete(@Param("id") Long id, @Param("userId") Long userId);

    /** 查询所有评论（管理端，分页） */
    List<Comment> selectAllForAdmin(@Param("offset") int offset,
                                    @Param("limit") int limit,
                                    @Param("targetType") String targetType);

    /** 统计评论总数（管理端） */
    long countAllForAdmin(@Param("targetType") String targetType);

    /** 管理员强制删除评论（直接设置 status = 'DELETED'，不校验 userId） */
    int adminDelete(@Param("id") Long id);
}