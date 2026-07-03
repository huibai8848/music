package com.example.music.service.impl;

import cn.hutool.core.util.StrUtil;
import com.example.music.constant.ErrorCode;
import com.example.music.entity.Artist;
import com.example.music.exception.BusinessException;
import com.example.music.mapper.AlbumMapper;
import com.example.music.mapper.ArtistMapper;
import com.example.music.mapper.SongMapper;
import com.example.music.service.ArtistService;
import com.example.music.vo.AlbumVO;
import com.example.music.vo.ArtistVO;
import com.example.music.vo.SongVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 艺人服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArtistServiceImpl implements ArtistService {

    private final ArtistMapper artistMapper;
    private final SongMapper songMapper;
    private final AlbumMapper albumMapper;

    @Override
    public ArtistVO getArtistDetail(Long id) {
        Artist artist = artistMapper.selectById(id);
        if (artist == null) {
            throw new BusinessException(ErrorCode.ARTIST_NOT_FOUND);
        }

        ArtistVO vo = ArtistVO.fromEntity(artist);

        // 获取歌曲数量
        List<SongVO> songs = songMapper.selectByArtistId(id).stream()
                .map(SongVO::fromEntity)
                .collect(Collectors.toList());
        vo.setSongs(songs);
        vo.setSongCount(songs.size());

        // 获取专辑列表
        List<AlbumVO> albums = albumMapper.selectByArtistId(id).stream()
                .map(AlbumVO::fromEntity)
                .collect(Collectors.toList());
        vo.setAlbums(albums);

        return vo;
    }

    @Override
    public List<ArtistVO> listArtists(int page, int size) {
        int offset = (page - 1) * size;
        return artistMapper.selectList(offset, size).stream()
                .map(ArtistVO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long countArtists() {
        return artistMapper.countTotal();
    }

    @Override
    public List<ArtistVO> searchArtists(String keyword, int page, int size) {
        int offset = (page - 1) * size;
        return artistMapper.searchByName(keyword, offset, size).stream()
                .map(ArtistVO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long countSearch(String keyword) {
        return artistMapper.countSearch(keyword);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArtistVO createArtist(Artist artist) {
        if (StrUtil.isBlank(artist.getName())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "艺人名称不能为空");
        }
        artistMapper.insert(artist);
        return ArtistVO.fromEntity(artist);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArtistVO updateArtist(Artist artist) {
        Artist existing = artistMapper.selectById(artist.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.ARTIST_NOT_FOUND);
        }
        artistMapper.update(artist);
        return ArtistVO.fromEntity(artistMapper.selectById(artist.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArtist(Long id) {
        if (artistMapper.selectById(id) == null) {
            throw new BusinessException(ErrorCode.ARTIST_NOT_FOUND);
        }
        artistMapper.deleteById(id);
        log.info("删除艺人: id={}", id);
    }
}