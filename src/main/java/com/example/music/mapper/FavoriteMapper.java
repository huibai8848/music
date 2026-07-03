package com.example.music.mapper;

import com.example.music.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 收藏 Mapper
 */
@Mapper
public interface FavoriteMapper {

    /** 查询用户的收藏列表 */
    List<Favorite> selectByUserId(@Param("userId") Long userId,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);

    /** 统计用户收藏总数 */
    long countByUserId(@Param("userId") Long userId);

    /** 查询单条收藏记录（用于幂等检查） */
    Favorite selectOne(@Param("userId") Long userId,
                       @Param("targetType") String targetType,
                       @Param("targetId") Long targetId);

    /** 新增收藏 */
    int insert(Favorite favorite);

    /** 取消收藏 */
    int delete(@Param("userId") Long userId,
               @Param("targetType") String targetType,
               @Param("targetId") Long targetId);

    /** 查询某个目标被收藏的次数 */
    long countByTarget(@Param("targetType") String targetType,
                       @Param("targetId") Long targetId);
}
