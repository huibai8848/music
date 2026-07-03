package com.example.music.service;

import com.example.music.entity.Album;
import com.example.music.vo.AlbumVO;

import java.util.List;

/**
 * 专辑服务接口
 */
public interface AlbumService {

    /** 根据 ID 获取专辑详情（含歌曲列表） */
    AlbumVO getAlbumDetail(Long id);

    /** 分页查询专辑列表 */
    List<AlbumVO> listAlbums(int page, int size);

    /** 统计专辑总数 */
    long countAlbums();

    /** 搜索专辑 */
    List<AlbumVO> searchAlbums(String keyword);

    /** 获取某艺人的专辑 */
    List<AlbumVO> getAlbumsByArtist(Long artistId);

    /** 新增专辑 */
    AlbumVO createAlbum(Album album);

    /** 更新专辑 */
    AlbumVO updateAlbum(Album album);

    /** 删除专辑 */
    void deleteAlbum(Long id);
}