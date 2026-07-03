package com.example.music.service.impl;

import com.example.music.constant.ErrorCode;
import com.example.music.entity.*;
import com.example.music.exception.BusinessException;
import com.example.music.mapper.*;
import com.example.music.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 收藏服务实现
 * <p>
 * 收藏是幂等的：重复收藏同一内容不会报错，只返回成功。
 * 查询时自动丰富收藏对象的名称和封面信息，便于前端直接展示。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final SongMapper songMapper;
    private final ArtistMapper artistMapper;
    private final AlbumMapper albumMapper;
    private final PlaylistMapper playlistMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addFavorite(Long userId, String targetType, Long targetId) {
        // 校验目标是否存在
        validateTargetExists(targetType, targetId);

        // 幂等：如果已收藏则直接返回
        Favorite existing = favoriteMapper.selectOne(userId, targetType.toUpperCase(), targetId);
        if (existing != null) {
            return;
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setTargetType(targetType.toUpperCase());
        favorite.setTargetId(targetId);
        favoriteMapper.insert(favorite);
        log.info("收藏: userId={}, type={}, targetId={}", userId, targetType, targetId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFavorite(Long userId, String targetType, Long targetId) {
        favoriteMapper.delete(userId, targetType.toUpperCase(), targetId);
        log.info("取消收藏: userId={}, type={}, targetId={}", userId, targetType, targetId);
    }

    @Override
    public Map<String, Object> listFavorites(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        List<Favorite> favorites = favoriteMapper.selectByUserId(userId, offset, size);
        long total = favoriteMapper.countByUserId(userId);

        // 丰富收藏数据：填充目标名称、封面等信息
        List<Map<String, Object>> enrichedRecords = favorites.stream()
                .map(this::enrichFavorite)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", enrichedRecords);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    public long countFavorites(String targetType, Long targetId) {
        return favoriteMapper.countByTarget(targetType.toUpperCase(), targetId);
    }

    /**
     * 校验收藏目标是否存在
     */
    private void validateTargetExists(String targetType, Long targetId) {
        String type = targetType.toUpperCase();
        switch (type) {
            case "SONG":
                if (songMapper.selectById(targetId) == null)
                    throw new BusinessException(ErrorCode.SONG_NOT_FOUND);
                break;
            case "ARTIST":
                if (artistMapper.selectById(targetId) == null)
                    throw new BusinessException(ErrorCode.ARTIST_NOT_FOUND);
                break;
            case "ALBUM":
                if (albumMapper.selectById(targetId) == null)
                    throw new BusinessException(ErrorCode.ALBUM_NOT_FOUND);
                break;
            case "PLAYLIST":
                if (playlistMapper.selectById(targetId) == null)
                    throw new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND);
                break;
            default:
                throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的收藏类型: " + type);
        }
    }

    /**
     * 丰富单条收藏记录：填充目标名称、封面等信息
     */
    private Map<String, Object> enrichFavorite(Favorite fav) {
        if (fav == null) return null;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", fav.getId());
        result.put("targetType", fav.getTargetType());
        result.put("targetId", fav.getTargetId());
        result.put("createdTime", fav.getCreatedTime());

        String type = fav.getTargetType();
        Long targetId = fav.getTargetId();

        switch (type) {
            case "SONG": {
                Song song = songMapper.selectById(targetId);
                if (song != null) {
                    result.put("targetName", song.getTitle());
                    result.put("coverUrl", song.getCoverUrl());
                    result.put("artistId", song.getArtistId());
                    // 填充艺人名
                    if (song.getArtistId() != null) {
                        Artist artist = artistMapper.selectById(song.getArtistId());
                        result.put("artistName", artist != null ? artist.getName() : null);
                    }
                } else {
                    result.put("targetName", "歌曲已删除");
                }
                break;
            }
            case "ARTIST": {
                Artist artist = artistMapper.selectById(targetId);
                if (artist != null) {
                    result.put("targetName", artist.getName());
                    result.put("coverUrl", artist.getAvatar());
                } else {
                    result.put("targetName", "艺人已删除");
                }
                break;
            }
            case "ALBUM": {
                Album album = albumMapper.selectById(targetId);
                if (album != null) {
                    result.put("targetName", album.getTitle());
                    result.put("coverUrl", album.getCoverUrl());
                } else {
                    result.put("targetName", "专辑已删除");
                }
                break;
            }
            case "PLAYLIST": {
                Playlist playlist = playlistMapper.selectById(targetId);
                if (playlist != null) {
                    result.put("targetName", playlist.getTitle());
                    result.put("coverUrl", playlist.getCoverUrl());
                } else {
                    result.put("targetName", "歌单已删除");
                }
                break;
            }
            default:
                result.put("targetName", "未知");
        }

        return result;
    }
}