package com.example.music.mapper;

import com.example.music.entity.Likes;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 喜欢（红心）Mapper
 */
@Mapper
public interface LikesMapper {

    /** 查询单条喜欢记录（用于幂等检查） */
    Likes selectOne(@Param("userId") Long userId,
                    @Param("targetType") String targetType,
                    @Param("targetId") Long targetId);

    /** 新增喜欢 */
    int insert(Likes likes);

    /** 取消喜欢 */
    int delete(@Param("userId") Long userId,
               @Param("targetType") String targetType,
               @Param("targetId") Long targetId);

    /** 统计某个目标被喜欢的次数 */
    long countByTarget(@Param("targetType") String targetType,
                       @Param("targetId") Long targetId);

    /** 查询用户是否已喜欢 */
    boolean isLiked(@Param("userId") Long userId,
                    @Param("targetType") String targetType,
                    @Param("targetId") Long targetId);
}
