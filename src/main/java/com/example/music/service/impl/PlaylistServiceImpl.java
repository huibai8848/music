package com.example.music.service.impl;

import com.example.music.constant.ErrorCode;
import com.example.music.dto.PlaylistDTO;
import com.example.music.entity.Playlist;
import com.example.music.entity.PlaylistSong;
import com.example.music.exception.BusinessException;
import com.example.music.mapper.ArtistMapper;
import com.example.music.mapper.PlaylistMapper;
import com.example.music.mapper.PlaylistSongMapper;
import com.example.music.mapper.SongMapper;
import com.example.music.mapper.UserMapper;
import com.example.music.service.PlaylistService;
import com.example.music.vo.PlaylistVO;
import com.example.music.vo.SongVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 歌单服务实现
 * <p>
 * 核心逻辑：
 * 1. 歌单封面默认取第一首歌的封面
 * 2. 每个歌单最多 500 首歌（由 system_config 配置）
 * 3. 隐藏歌单仅创建者可见
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistServiceImpl implements PlaylistService {

    private final PlaylistMapper playlistMapper;
    private final PlaylistSongMapper playlistSongMapper;
    private final SongMapper songMapper;
    private final UserMapper userMapper;
    private final ArtistMapper artistMapper;

    /** 歌单最大歌曲数 */
    private static final int MAX_SONGS = 500;

    @Override
    public Map<String, Object> listPublicPlaylists(int page, int size) {
        int offset = (page - 1) * size;
        List<Playlist> playlists = playlistMapper.selectPublic(offset, size);
        long total = playlistMapper.countPublic();

        List<PlaylistVO> voList = playlists.stream()
                .map(this::toPlaylistVO)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", voList);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    public List<PlaylistVO> getUserPlaylists(Long userId) {
        return playlistMapper.selectByUserId(userId).stream()
                .map(this::toPlaylistVO)
                .collect(Collectors.toList());
    }

    @Override
    public PlaylistVO getPlaylistDetail(Long id, Long currentUserId) {
        Playlist playlist = playlistMapper.selectById(id);
        if (playlist == null) {
            throw new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND);
        }

        // 隐藏歌单仅创建者或管理员可见
        if (!playlist.getIsPublic() && !playlist.getUserId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        PlaylistVO vo = toPlaylistVO(playlist);
        vo.setSongs(getPlaylistSongs(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlaylistVO createPlaylist(Long userId, PlaylistDTO dto) {
        Playlist playlist = new Playlist();
        playlist.setUserId(userId);
        playlist.setTitle(dto.getTitle());
        playlist.setDescription(dto.getDescription());
        playlist.setCoverUrl(dto.getCoverUrl());
        playlist.setIsPublic(dto.getIsPublic() != null ? dto.getIsPublic() : true);
        playlist.setSongCount(0);

        playlistMapper.insert(playlist);
        log.info("创建歌单: id={}, title={}", playlist.getId(), dto.getTitle());
        return toPlaylistVO(playlist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlaylistVO updatePlaylist(Long userId, Long playlistId, PlaylistDTO dto) {
        Playlist playlist = playlistMapper.selectById(playlistId);
        if (playlist == null) {
            throw new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND);
        }
        if (!playlist.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        playlist.setTitle(dto.getTitle());
        playlist.setDescription(dto.getDescription());
        playlist.setCoverUrl(dto.getCoverUrl());
        if (dto.getIsPublic() != null) {
            playlist.setIsPublic(dto.getIsPublic());
        }

        playlistMapper.update(playlist);
        return toPlaylistVO(playlist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlaylist(Long userId, Long playlistId) {
        Playlist playlist = playlistMapper.selectById(playlistId);
        if (playlist == null) {
            throw new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND);
        }
        if (!playlist.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 先删除关联数据
        playlistSongMapper.deleteByPlaylistId(playlistId);
        // 再删除歌单
        playlistMapper.deleteById(playlistId);
        log.info("删除歌单: id={}", playlistId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSongToPlaylist(Long userId, Long playlistId, Long songId) {
        Playlist playlist = playlistMapper.selectById(playlistId);
        if (playlist == null) {
            throw new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND);
        }
        if (!playlist.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 检查歌曲是否存在
        if (songMapper.selectById(songId) == null) {
            throw new BusinessException(ErrorCode.SONG_NOT_FOUND);
        }

        // 检查是否已存在
        if (playlistSongMapper.exist(playlistId, songId) > 0) {
            return; // 幂等
        }

        // 检查数量限制
        int currentCount = playlistSongMapper.countByPlaylistId(playlistId);
        if (currentCount >= MAX_SONGS) {
            throw new BusinessException(ErrorCode.PLAYLIST_FULL);
        }

        // 添加关联
        PlaylistSong ps = new PlaylistSong();
        ps.setPlaylistId(playlistId);
        ps.setSongId(songId);
        ps.setSortOrder(playlistSongMapper.selectMaxSortOrder(playlistId) + 1);
        playlistSongMapper.insert(ps);

        // 更新计数
        playlistMapper.updateSongCount(playlistId, currentCount + 1);
        log.info("歌单添加歌曲: playlistId={}, songId={}", playlistId, songId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeSongFromPlaylist(Long userId, Long playlistId, Long songId) {
        Playlist playlist = playlistMapper.selectById(playlistId);
        if (playlist == null) {
            throw new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND);
        }
        if (!playlist.getUserId().equals(userId) && !"ADMIN".equals(
                com.example.music.utils.RequestContext.getUserRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        playlistSongMapper.delete(playlistId, songId);

        // 更新计数
        int currentCount = playlistSongMapper.countByPlaylistId(playlistId);
        playlistMapper.updateSongCount(playlistId, currentCount);
        log.info("歌单移除歌曲: playlistId={}, songId={}", playlistId, songId);
    }

    @Override
    public List<SongVO> getPlaylistSongs(Long playlistId) {
        List<Long> songIds = playlistSongMapper.selectSongIdsByPlaylistId(playlistId);
        if (songIds.isEmpty()) {
            return Collections.emptyList();
        }
        return songIds.stream()
                .map(songMapper::selectById)
                .filter(Objects::nonNull)
                .map(song -> {
                    SongVO vo = SongVO.fromEntity(song);
                    // 填充艺人名
                    if (song.getArtistId() != null) {
                        var artist = artistMapper.selectById(song.getArtistId());
                        if (artist != null) {
                            vo.setArtistName(artist.getName());
                        }
                    }
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 将 Playlist 实体转换为 VO（填充创建者昵称）
     */
    private PlaylistVO toPlaylistVO(Playlist playlist) {
        PlaylistVO vo = PlaylistVO.fromEntity(playlist);
        if (playlist.getUserId() != null) {
            var user = userMapper.selectById(playlist.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
            }
        }
        return vo;
    }
}
