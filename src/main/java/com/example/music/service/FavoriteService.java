package com.example.music.service;

import com.example.music.vo.R;

import java.util.List;
import java.util.Map;

/**
 * 收藏服务接口
 */
public interface FavoriteService {

    /** 收藏（幂等） */
    void addFavorite(Long userId, String targetType, Long targetId);

    /** 取消收藏 */
    void removeFavorite(Long userId, String targetType, Long targetId);

    /** 收藏列表 */
    Map<String, Object> listFavorites(Long userId, int page, int size);

    /** 查询某个目标的收藏数 */
    long countFavorites(String targetType, Long targetId);
}
