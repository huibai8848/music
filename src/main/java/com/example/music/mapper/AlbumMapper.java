package com.example.music.mapper;

import com.example.music.entity.Album;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 专辑 Mapper
 * <p>
 * 提供专辑的 CRUD、搜索、并按 ACTIVE 状态过滤歌曲等功能。
 */
@Mapper
public interface AlbumMapper {

    /** 根据 ID 查询 */
    Album selectById(@Param("id") Long id);

    /** 分页查询全部专辑（无状态过滤，管理端使用） */
    List<Album> selectList(@Param("offset") int offset,
                           @Param("limit") int limit);

    /** 统计全部专辑总数 */
    long countTotal();

    /** 按艺人查询专辑 */
    List<Album> selectByArtistId(@Param("artistId") Long artistId);

    /** 按标题 + 艺人精确查找（用于会员上传时查重） */
    Album selectByTitleAndArtist(@Param("title") String title,
                                  @Param("artistId") Long artistId);

    /** 按专辑名模糊搜索（仅搜索专辑名，无 ACTIVE 过滤） */
    List<Album> searchByName(@Param("keyword") String keyword);

    /** 新增专辑 */
    int insert(Album album);

    /** 更新专辑 */
    int update(Album album);

    /** 删除专辑 */
    int deleteById(@Param("id") Long id);

    // ==================== 公开查询（仅包含有 ACTIVE 歌曲的专辑） ====================

    /**
     * 分页查询有 ACTIVE 歌曲的专辑列表
     * <p>
     * 使用 EXISTS 子查询过滤：仅返回专辑下至少有一首 status='ACTIVE' 歌曲的专辑。
     * 避免 PENDING/REJECTED 歌曲导致空专辑出现在前端。
     */
    List<Album> selectListWithActiveSongs(@Param("offset") int offset,
                                          @Param("limit") int limit);

    /** 统计有 ACTIVE 歌曲的专辑总数 */
    long countWithActiveSongs();

    /**
     * 模糊搜索专辑（分页，包含分页参数）
     * <p>
     * 同时模糊匹配专辑标题和所属艺人名称。
     * 仅返回有 ACTIVE 歌曲的专辑。
     *
     * @param keyword 搜索关键词
     * @param offset  分页偏移量
     * @param limit   每页条数
     */
    List<Album> searchByNameWithActiveSongs(@Param("keyword") String keyword,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);

    /**
     * 统计模糊搜索的命中数（仅 ACTIVE 歌曲的专辑）
     */
    long countSearchWithActiveSongs(@Param("keyword") String keyword);
}
