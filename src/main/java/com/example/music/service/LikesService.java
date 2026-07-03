package com.example.music.service;

/**
 * 喜欢（红心）服务接口
 */
public interface LikesService {

    /** 喜欢（幂等） */
    void addLike(Long userId, String targetType, Long targetId);

    /** 取消喜欢 */
    void removeLike(Long userId, String targetType, Long targetId);

    /** 查询某个目标的喜欢数 */
    long countLikes(String targetType, Long targetId);

    /** 查询用户是否已喜欢 */
    boolean isLiked(Long userId, String targetType, Long targetId);
}
