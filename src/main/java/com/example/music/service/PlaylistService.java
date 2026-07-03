package com.example.music.service;

import com.example.music.dto.PlaylistDTO;
import com.example.music.vo.PlaylistVO;
import com.example.music.vo.SongVO;

import java.util.List;
import java.util.Map;

/**
 * 歌单服务接口
 */
public interface PlaylistService {

    /** 查询公开歌单列表 */
    Map<String, Object> listPublicPlaylists(int page, int size);

    /** 获取用户的所有歌单 */
    List<PlaylistVO> getUserPlaylists(Long userId);

    /** 获取歌单详情（含歌曲列表） */
    PlaylistVO getPlaylistDetail(Long id, Long currentUserId);

    /** 创建歌单 */
    PlaylistVO createPlaylist(Long userId, PlaylistDTO dto);

    /** 更新歌单 */
    PlaylistVO updatePlaylist(Long userId, Long playlistId, PlaylistDTO dto);

    /** 删除歌单 */
    void deletePlaylist(Long userId, Long playlistId);

    /** 添加歌曲到歌单 */
    void addSongToPlaylist(Long userId, Long playlistId, Long songId);

    /** 从歌单移除歌曲 */
    void removeSongFromPlaylist(Long userId, Long playlistId, Long songId);

    /** 获取歌单内歌曲列表 */
    List<SongVO> getPlaylistSongs(Long playlistId);
}
