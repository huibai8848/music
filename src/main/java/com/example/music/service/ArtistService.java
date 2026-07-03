package com.example.music.service;

import com.example.music.entity.Artist;
import com.example.music.vo.ArtistVO;

import java.util.List;

/**
 * 艺人服务接口
 */
public interface ArtistService {

    /** 根据 ID 获取艺人详情（含作品列表和专辑列表） */
    ArtistVO getArtistDetail(Long id);

    /** 分页查询艺人列表 */
    List<ArtistVO> listArtists(int page, int size);

    /** 统计艺人总数 */
    long countArtists();

    /** 搜索艺人 */
    List<ArtistVO> searchArtists(String keyword, int page, int size);

    /** 统计搜索结果数 */
    long countSearch(String keyword);

    /** 新增艺人 */
    ArtistVO createArtist(Artist artist);

    /** 更新艺人 */
    ArtistVO updateArtist(Artist artist);

    /** 删除艺人 */
    void deleteArtist(Long id);
}