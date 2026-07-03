package com.example.music.service.impl;

import com.example.music.entity.Likes;
import com.example.music.mapper.LikesMapper;
import com.example.music.service.LikesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 喜欢（红心）服务实现
 * <p>
 * 与收藏语义不同：喜欢是轻量级的心动标记，计入推荐权重。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LikesServiceImpl implements LikesService {

    private final LikesMapper likesMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addLike(Long userId, String targetType, Long targetId) {
        Likes existing = likesMapper.selectOne(userId, targetType.toUpperCase(), targetId);
        if (existing != null) {
            return; // 幂等
        }

        Likes likes = new Likes();
        likes.setUserId(userId);
        likes.setTargetType(targetType.toUpperCase());
        likes.setTargetId(targetId);
        likesMapper.insert(likes);
        log.info("喜欢: userId={}, type={}, targetId={}", userId, targetType, targetId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeLike(Long userId, String targetType, Long targetId) {
        likesMapper.delete(userId, targetType.toUpperCase(), targetId);
        log.info("取消喜欢: userId={}, type={}, targetId={}", userId, targetType, targetId);
    }

    @Override
    public long countLikes(String targetType, Long targetId) {
        return likesMapper.countByTarget(targetType.toUpperCase(), targetId);
    }

    @Override
    public boolean isLiked(Long userId, String targetType, Long targetId) {
        return likesMapper.isLiked(userId, targetType.toUpperCase(), targetId);
    }
}
