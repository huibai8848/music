package com.example.music.service.impl;

import cn.hutool.core.util.StrUtil;
import com.example.music.constant.ErrorCode;
import com.example.music.entity.Album;
import com.example.music.exception.BusinessException;
import com.example.music.mapper.AlbumMapper;
import com.example.music.mapper.ArtistMapper;
import com.example.music.mapper.SongMapper;
import com.example.music.service.AlbumService;
import com.example.music.vo.AlbumVO;
import com.example.music.vo.SongVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 专辑服务实现
 * <p>
 * 提供专辑的 CRUD、列表分页、模糊搜索（专辑名+艺人名）。
 * 公开查询仅返回有 ACTIVE 歌曲的专辑，避免 PENDING/REJECTED 歌曲导致空专辑展示。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlbumServiceImpl implements AlbumService {

    private final AlbumMapper albumMapper;
    private final ArtistMapper artistMapper;
    private final SongMapper songMapper;

    @Override
    public AlbumVO getAlbumDetail(Long id) {
        Album album = albumMapper.selectById(id);
        if (album == null) {
            throw new BusinessException(ErrorCode.ALBUM_NOT_FOUND);
        }

        AlbumVO vo = AlbumVO.fromEntity(album);

        // 设置艺人名
        if (album.getArtistId() != null) {
            var artist = artistMapper.selectById(album.getArtistId());
            if (artist != null) {
                vo.setArtistName(artist.getName());
            }
        }

        // 获取专辑内歌曲（含所有状态，PENDING 歌曲的艺人名在 enrichSongVO 中已屏蔽）
        List<SongVO> songs = songMapper.selectAllByAlbumId(id).stream()
                .map(SongVO::fromEntity)
                .collect(Collectors.toList());
        vo.setSongs(songs);
        vo.setSongCount(songs.size());

        return vo;
    }

    @Override
    public List<AlbumVO> listAlbums(int page, int size) {
        int offset = (page - 1) * size;
        // 使用 EXISTS 子查询过滤：仅返回有 ACTIVE 歌曲的专辑
        List<Album> albums = albumMapper.selectListWithActiveSongs(offset, size);

        return albums.stream().map(album -> {
            AlbumVO vo = AlbumVO.fromEntity(album);
            // 填充艺人名（列表页展示用），失败不阻断整体
            if (album.getArtistId() != null) {
                try {
                    var artist = artistMapper.selectById(album.getArtistId());
                    if (artist != null) {
                        vo.setArtistName(artist.getName());
                    }
                } catch (Exception e) {
                    log.warn("填充列表艺人名失败, albumId={}, artistId={}", album.getId(), album.getArtistId(), e);
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public long countAlbums() {
        return albumMapper.countWithActiveSongs();
    }

    @Override
    public List<AlbumVO> searchAlbums(String keyword, int page, int size) {
        if (StrUtil.isBlank(keyword)) {
            return Collections.emptyList();
        }

        int offset = (page - 1) * size;
        // 模糊搜索：同时匹配专辑名和艺人名，仅返回有 ACTIVE 歌曲的专辑
        // SQL 已按相关性排序：专辑名匹配优先于艺人名匹配
        List<Album> albums;
        try {
            albums = albumMapper.searchByNameWithActiveSongs(keyword, offset, size);
        } catch (Exception e) {
            log.error("专辑搜索查询失败, keyword={}, page={}, size={}", keyword, page, size, e);
            return Collections.emptyList();
        }

        return albums.stream().map(album -> {
            AlbumVO vo = AlbumVO.fromEntity(album);
            // 填充艺人名（搜索结果展示用），失败不阻断整体搜索
            if (album.getArtistId() != null) {
                try {
                    var artist = artistMapper.selectById(album.getArtistId());
                    if (artist != null) {
                        vo.setArtistName(artist.getName());
                    }
                } catch (Exception e) {
                    log.warn("填充专辑艺人名失败, albumId={}, artistId={}", album.getId(), album.getArtistId(), e);
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public long countSearchAlbums(String keyword) {
        if (StrUtil.isBlank(keyword)) return 0;
        try {
            return albumMapper.countSearchWithActiveSongs(keyword);
        } catch (Exception e) {
            log.error("专辑搜索计数查询失败, keyword={}", keyword, e);
            return 0;
        }
    }

    @Override
    public List<AlbumVO> getAlbumsByArtist(Long artistId) {
        return albumMapper.selectByArtistId(artistId).stream()
                .map(AlbumVO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlbumVO createAlbum(Album album) {
        if (StrUtil.isBlank(album.getTitle())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "专辑名称不能为空");
        }
        albumMapper.insert(album);
        return AlbumVO.fromEntity(album);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlbumVO updateAlbum(Album album) {
        Album existing = albumMapper.selectById(album.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.ALBUM_NOT_FOUND);
        }
        albumMapper.update(album);
        return AlbumVO.fromEntity(albumMapper.selectById(album.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAlbum(Long id) {
        if (albumMapper.selectById(id) == null) {
            throw new BusinessException(ErrorCode.ALBUM_NOT_FOUND);
        }
        albumMapper.deleteById(id);
        log.info("删除专辑: id={}", id);
    }
}
