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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        // 获取歌曲（含所有状态，PENDING 歌曲由前端控制展示，enrichSongVO 中已屏蔽艺人名）
        List<SongVO> songs = songMapper.selectAllByArtistId(id).stream()
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
        // 1. 分页查询有 ACTIVE 歌曲的艺人基本信息
        List<Artist> artists = artistMapper.selectListWithActiveSongs(offset, size);
        List<ArtistVO> vos = artists.stream()
                .map(ArtistVO::fromEntity)
                .collect(Collectors.toList());

        // 2. 批量填充歌曲数量，避免 N+1 查询（原来缺失此步骤导致 songCount 始终为 0）
        setSongCounts(vos);

        return vos;
    }

    @Override
    public long countArtists() {
        return artistMapper.countWithActiveSongs();
    }

    @Override
    public List<ArtistVO> searchArtists(String keyword, int page, int size) {
        if (StrUtil.isBlank(keyword)) {
            return Collections.emptyList();
        }
        int offset = (page - 1) * size;
        try {
            // 1. 按名称/歌曲名搜索有 ACTIVE 歌曲的艺人
            List<Artist> artists = artistMapper.searchByNameWithActiveSongs(keyword, offset, size);
            List<ArtistVO> vos = artists.stream()
                    .map(ArtistVO::fromEntity)
                    .collect(Collectors.toList());

            // 2. 批量填充歌曲数量，避免 N+1 查询（原来缺失此步骤导致 songCount 始终为 0）
            setSongCounts(vos);

            return vos;
        } catch (Exception e) {
            log.error("艺人搜索查询失败, keyword={}, page={}, size={}", keyword, page, size, e);
            return Collections.emptyList();
        }
    }

    @Override
    public long countSearch(String keyword) {
        if (StrUtil.isBlank(keyword)) return 0;
        try {
            return artistMapper.countSearchWithActiveSongs(keyword);
        } catch (Exception e) {
            log.error("艺人搜索计数查询失败, keyword={}", keyword, e);
            return 0;
        }
    }

    /**
     * 批量设置艺人列表的歌曲数量
     * <p>
     * 通过一次 SQL 查询所有艺人的 ACTIVE 歌曲数（GROUP BY artist_id），
     * 避免逐条查询的 N+1 性能问题。
     * 艺人详情页 {@link #getArtistDetail(Long)} 直接使用 songs.size()，
     * 而列表页用此方法批量设置。
     */
    private void setSongCounts(List<ArtistVO> vos) {
        if (vos.isEmpty()) return;

        // 收集所有艺人 ID → 一次 IN 查询获取歌曲数量
        List<Long> artistIds = vos.stream()
                .map(ArtistVO::getId)
                .collect(Collectors.toList());
        List<Map<String, Object>> counts = artistMapper.selectSongCountsByArtistIds(artistIds);

        // 构建 artistId → songCount 映射
        Map<Long, Integer> countMap = new HashMap<>();
        for (Map<String, Object> row : counts) {
            Long artistId = ((Number) row.get("artist_id")).longValue();
            int songCount = ((Number) row.get("song_count")).intValue();
            countMap.put(artistId, songCount);
        }

        // 逐个填充（没有记录的艺人表示歌曲数为 0）
        for (ArtistVO vo : vos) {
            vo.setSongCount(countMap.getOrDefault(vo.getId(), 0));
        }
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