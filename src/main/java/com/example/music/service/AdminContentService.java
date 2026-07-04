package com.example.music.service;

import com.example.music.entity.Artist;
import com.example.music.entity.Song;
import com.example.music.vo.ArtistVO;
import com.example.music.vo.SongVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 管理后台内容管理服务接口
 * <p>
 * 提供歌曲/专辑/艺人的管理功能：列表查询、审核、删除、批量导入。
 */
public interface AdminContentService {

    // ==================== 歌曲管理 ====================

    /**
     * 分页查询歌曲列表（管理端，可包含所有状态）
     *
     * @param status 筛选状态（可选）
     * @param keyword 搜索关键字（可选）
     * @param page    页码
     * @param size    每页条数
     * @return 分页结果
     */
    Map<String, Object> listSongs(String status, String keyword, int page, int size);

    /**
     * 审核歌曲（会员上传审核）
     *
     * @param adminId     操作管理员 ID
     * @param songId      歌曲 ID
     * @param status      审核状态（ACTIVE=通过 / REJECTED=驳回）
     * @param rejectReason 驳回原因
     */
    void auditSong(Long adminId, Long songId, String status, String rejectReason);

    /**
     * 创建歌曲
     *
     * @param adminId 操作管理员 ID
     * @param song    歌曲信息
     * @return 创建的歌曲 VO
     */
    SongVO createSong(Long adminId, Song song);

    /**
     * 更新歌曲
     *
     * @param adminId 操作管理员 ID
     * @param id      歌曲 ID
     * @param song    更新的歌曲信息
     * @return 更新后的歌曲 VO
     */
    SongVO updateSong(Long adminId, Long id, Song song);

    /**
     * 获取歌曲详情（管理端，含艺人名）
     *
     * @param id 歌曲 ID
     * @return 歌曲 VO
     */
    SongVO getSongDetail(Long id);

    /**
     * 删除歌曲
     *
     * @param adminId 操作管理员 ID
     * @param songId  歌曲 ID
     */
    void deleteSong(Long adminId, Long songId);

    // ==================== 专辑管理 ====================

    /**
     * 分页查询专辑列表（支持按名称搜索）
     */
    Map<String, Object> listAlbums(String keyword, int page, int size);

    /**
     * 删除专辑
     */
    void deleteAlbum(Long adminId, Long albumId);

    // ==================== 艺人管理 ====================

    /**
     * 分页查询艺人列表（支持按名称搜索）
     */
    Map<String, Object> listArtists(String keyword, int page, int size);

    /**
     * 更新艺人信息（管理员修改艺人名称/头像/简介等）
     *
     * @param artist 艺人信息（含 id）
     * @return 更新后的艺人 VO
     */
    ArtistVO updateArtist(Artist artist);

    /**
     * 删除艺人
     */
    void deleteArtist(Long adminId, Long artistId);

    // ==================== 评论管理 ====================

    /**
     * 分页查询评论列表（管理端）
     *
     * @param targetType 筛选目标类型（可选）
     * @param page       页码
     * @param size       每页条数
     */
    Map<String, Object> listComments(String targetType, int page, int size);

    /**
     * 管理员强制删除评论
     *
     * @param adminId   操作管理员 ID
     * @param commentId 评论 ID
     */
    void deleteComment(Long adminId, Long commentId);

    // ==================== 批量导入 ====================

    /**
     * 批量导入歌曲（ZIP 上传）
     * <p>
     * 解压 ZIP 包，提取音频文件（MP3）、LRC 歌词和封面图片，自动创建歌曲记录。
     *
     * @param adminId 操作管理员 ID
     * @param file    上传的 ZIP 文件
     * @return 导入结果统计（total / success / failed / errors）
     */
    Map<String, Object> batchImportSongs(Long adminId, MultipartFile file);
}
