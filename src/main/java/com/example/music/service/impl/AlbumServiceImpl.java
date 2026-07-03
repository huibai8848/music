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

        // 获取专辑内歌曲
        List<SongVO> songs = songMapper.selectByAlbumId(id).stream()
                .map(SongVO::fromEntity)
                .collect(Collectors.toList());
        vo.setSongs(songs);
        vo.setSongCount(songs.size());

        return vo;
    }

    @Override
    public List<AlbumVO> listAlbums(int page, int size) {
        int offset = (page - 1) * size;
        List<Album> albums = albumMapper.selectList(offset, size);

        return albums.stream().map(album -> {
            AlbumVO vo = AlbumVO.fromEntity(album);
            if (album.getArtistId() != null) {
                var artist = artistMapper.selectById(album.getArtistId());
                if (artist != null) {
                    vo.setArtistName(artist.getName());
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public long countAlbums() {
        return albumMapper.countTotal();
    }

    @Override
    public List<AlbumVO> searchAlbums(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return Collections.emptyList();
        }
        return albumMapper.searchByName(keyword).stream()
                .map(AlbumVO::fromEntity)
                .collect(Collectors.toList());
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