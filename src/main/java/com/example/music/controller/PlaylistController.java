package com.example.music.controller;

import com.example.music.dto.PlaylistDTO;
import com.example.music.service.PlaylistService;
import com.example.music.utils.RequestContext;
import com.example.music.vo.PlaylistVO;
import com.example.music.vo.R;
import com.example.music.vo.SongVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 歌单控制器
 * <p>
 * 歌单 CRUD、歌曲增删、公开歌单浏览。
 */
@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;

    /**
     * 公开歌单列表（分页）
     */
    @GetMapping
    public R<Map<String, Object>> listPlaylists(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(playlistService.listPublicPlaylists(page, size));
    }

    /**
     * 获取当前用户的歌单列表
     */
    @GetMapping("/mine")
    public R<List<PlaylistVO>> myPlaylists() {
        Long userId = RequestContext.getUserId();
        return R.ok(playlistService.getUserPlaylists(userId));
    }

    /**
     * 歌单详情（含歌曲列表）
     */
    @GetMapping("/{id}")
    public R<PlaylistVO> getPlaylistDetail(@PathVariable Long id) {
        Long userId = RequestContext.getUserId();
        return R.ok(playlistService.getPlaylistDetail(id, userId));
    }

    /**
     * 创建歌单
     */
    @PostMapping
    public R<PlaylistVO> createPlaylist(@Valid @RequestBody PlaylistDTO dto) {
        Long userId = RequestContext.getUserId();
        return R.ok("创建成功", playlistService.createPlaylist(userId, dto));
    }

    /**
     * 编辑歌单
     */
    @PutMapping("/{id}")
    public R<PlaylistVO> updatePlaylist(@PathVariable Long id, @Valid @RequestBody PlaylistDTO dto) {
        Long userId = RequestContext.getUserId();
        return R.ok("更新成功", playlistService.updatePlaylist(userId, id, dto));
    }

    /**
     * 删除歌单
     */
    @DeleteMapping("/{id}")
    public R<Object> deletePlaylist(@PathVariable Long id) {
        Long userId = RequestContext.getUserId();
        playlistService.deletePlaylist(userId, id);
        return R.ok("删除成功");
    }

    /**
     * 歌单添加歌曲
     * <p>
     * 接受 JSON body: {"songId": 123} 或 query param: ?songId=123
     */
    @PostMapping("/{id}/songs")
    public R<Object> addSong(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Long songId = body.get("songId");
        if (songId == null) {
            return R.fail("缺少必需的参数: songId");
        }
        Long userId = RequestContext.getUserId();
        playlistService.addSongToPlaylist(userId, id, songId);
        return R.ok("添加成功");
    }

    /**
     * 歌单移除歌曲
     */
    @DeleteMapping("/{id}/songs/{songId}")
    public R<Object> removeSong(@PathVariable Long id, @PathVariable Long songId) {
        Long userId = RequestContext.getUserId();
        playlistService.removeSongFromPlaylist(userId, id, songId);
        return R.ok("已移除");
    }

    /**
     * 获取歌单内歌曲列表
     */
    @GetMapping("/{id}/songs")
    public R<List<SongVO>> getSongs(@PathVariable Long id) {
        return R.ok(playlistService.getPlaylistSongs(id));
    }
}
