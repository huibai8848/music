package com.example.music.service;

import com.example.music.entity.Song;
import com.example.music.vo.SongVO;

import java.util.List;

/**
 * 歌曲服务接口
 */
public interface SongService {

    /** 根据 ID 获取歌曲详情（包含艺人名、专辑名） */
    SongVO getSongDetail(Long id);

    /** 分页查询歌曲列表 */
    List<SongVO> listSongs(int page, int size, String genre, String status);

    /** 统计歌曲总数 */
    long countSongs(String genre, String status);

    /** 搜索歌曲 */
    List<SongVO> searchSongs(String keyword, int page, int size);

    /** 统计搜索结果数 */
    long countSearch(String keyword);

    /** 获取热门歌曲 */
    List<SongVO> getHotSongs(int limit);

    /** 获取某艺人的歌曲 */
    List<SongVO> getSongsByArtist(Long artistId);

    /** 获取某上传者的歌曲 */
    List<SongVO> getSongsByUploader(Long uploaderId);

    /** 获取某专辑的歌曲 */
    List<SongVO> getSongsByAlbum(Long albumId);

    /** 获取 LRC 歌词 JSON */
    String getLyrics(Long songId);

    /** 上报播放量 */
    void reportPlay(Long songId);

    /** 新增歌曲 */
    SongVO createSong(Song song);

    /**
     * 提交歌曲审核（会员上传）
     * <p>
     * 在事务中依次完成：艺人创建/查找 → 专辑创建/查找 → 歌曲创建。
     * 所有步骤在同一事务中，任一失败则全部回滚。
     *
     * @param song    歌曲实体（需包含 audioUrl 等文件 URL）
     * @param artist  艺人实体（若提供 ID 则按 ID 查找，否则按 name 创建）
     * @param album   专辑实体（若提供 ID 或 title 则关联，否则不关联）
     * @return 丰富后的歌曲 VO
     */
    SongVO submitSongForReview(Song song, com.example.music.entity.Artist artist, com.example.music.entity.Album album);

    /** 更新歌曲 */
    SongVO updateSong(Song song);

    /** 删除歌曲 */
    void deleteSong(Long id);

    /** 审核歌曲 */
    void auditSong(Long id, String status);

    /** 获取歌曲分类 ID 列表 */
    List<Long> getSongCategoryIds(Long songId);

    /** 更新歌曲分类 */
    void updateSongCategories(Long songId, List<Long> categoryIds);
}