package com.example.music.service.impl;

import com.example.music.entity.Artist;
import com.example.music.mapper.AlbumMapper;
import com.example.music.mapper.ArtistMapper;
import com.example.music.mapper.SongMapper;
import com.example.music.vo.ArtistVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ArtistServiceImpl 单元测试
 * <p>
 * 使用 Mockito 模拟 Mapper 层，重点验证歌曲数量（songCount）是否被正确填充，
 * 这是修复艺人列表页 songCount 始终为 0 的核心回归用例。
 */
@ExtendWith(MockitoExtension.class)
class ArtistServiceImplTest {

    @Mock
    private ArtistMapper artistMapper;
    @Mock
    private SongMapper songMapper;
    @Mock
    private AlbumMapper albumMapper;

    private ArtistServiceImpl artistService;

    @BeforeEach
    void setUp() {
        // 注入 Mock 依赖
        artistService = new ArtistServiceImpl(artistMapper, songMapper, albumMapper);
    }

    /**
     * 辅助方法：创建一个测试用 Artist 实体
     */
    private Artist createArtist(Long id, String name) {
        Artist a = new Artist();
        a.setId(id);
        a.setName(name);
        a.setAvatar("/avatars/" + id + ".jpg");
        a.setBio(name + " 的简介");
        a.setCountry("中国");
        a.setCreatedTime(LocalDateTime.now());
        return a;
    }

    /**
     * 辅助方法：构造 Mapper 返回的歌曲数量统计结果
     * 格式即 ArtistMapper.selectSongCountsByArtistIds() 返回的 Map 列表
     */
    private List<Map<String, Object>> createCountResult(Long artistId, int count) {
        Map<String, Object> row = new HashMap<>();
        row.put("artist_id", artistId);
        row.put("song_count", count);
        return Collections.singletonList(row);
    }

    // ==================== listArtists() 测试 ====================

    @Test
    @DisplayName("【关键回归】listArtists 应正确填充 songCount，而非默认为 0")
    void testListArtists_shouldSetSongCount() {
        // 模拟数据库中有 2 个艺人，分别有 3 首和 5 首 ACTIVE 歌曲
        Artist artist1 = createArtist(1L, "艺人A");
        Artist artist2 = createArtist(2L, "艺人B");

        when(artistMapper.selectListWithActiveSongs(0, 20)).thenReturn(Arrays.asList(artist1, artist2));

        // 模拟批量歌曲数查询：艺人 A 有 3 首歌，艺人 B 有 5 首歌
        List<Map<String, Object>> mockCounts = new ArrayList<>();
        mockCounts.addAll(createCountResult(1L, 3));
        mockCounts.addAll(createCountResult(2L, 5));
        when(artistMapper.selectSongCountsByArtistIds(Arrays.asList(1L, 2L))).thenReturn(mockCounts);

        // 执行
        List<ArtistVO> result = artistService.listArtists(1, 20);

        // 验证
        assertEquals(2, result.size(), "应返回 2 个艺人");

        // ★ 关键断言：之前 songCount 始终为 0，修复后应正确显示
        assertEquals(3, result.get(0).getSongCount(),
                "艺人A 应有 3 首 ACTIVE 歌曲（修复前为 0）");
        assertEquals(5, result.get(1).getSongCount(),
                "艺人B 应有 5 首 ACTIVE 歌曲（修复前为 0）");

        // 验证 Mapper 调用顺序：先查艺人列表 → 再批量查歌曲数
        verify(artistMapper).selectListWithActiveSongs(0, 20);
        verify(artistMapper).selectSongCountsByArtistIds(Arrays.asList(1L, 2L));
    }

    @Test
    @DisplayName("listArtists 当艺人有 0 首 ACTIVE 歌曲时 songCount 应为 0")
    void testListArtists_withZeroSongs() {
        // 模拟数据库中有 1 个艺人但没有 ACTIVE 歌曲
        Artist artist = createArtist(1L, "无歌艺人");
        when(artistMapper.selectListWithActiveSongs(0, 20)).thenReturn(Collections.singletonList(artist));

        // 模拟批量歌曲数查询返回空（该艺人没有 ACTIVE 歌曲，不在结果中）
        when(artistMapper.selectSongCountsByArtistIds(Collections.singletonList(1L)))
                .thenReturn(Collections.emptyList());

        List<ArtistVO> result = artistService.listArtists(1, 20);

        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getSongCount(),
                "无 ACTIVE 歌曲的艺人 songCount 应为 0");
    }

    @Test
    @DisplayName("listArtists 艺人列表为空时应返回空列表，不触发歌曲数查询")
    void testListArtists_withEmptyList() {
        when(artistMapper.selectListWithActiveSongs(0, 20)).thenReturn(Collections.emptyList());

        List<ArtistVO> result = artistService.listArtists(1, 20);

        assertEquals(0, result.size(), "空列表应返回空");
        // 不应调用歌曲数查询（因为艺人列表为空，setSongCounts 直接 return）
        verify(artistMapper, never()).selectSongCountsByArtistIds(anyList());
    }

    @Test
    @DisplayName("listArtists 分页参数应正确传递给 Mapper")
    void testListArtists_withPagination() {
        // 第 2 页，每页 10 条 → offset = 10
        when(artistMapper.selectListWithActiveSongs(10, 10)).thenReturn(Collections.emptyList());

        artistService.listArtists(2, 10);
        verify(artistMapper).selectListWithActiveSongs(10, 10);
    }

    // ==================== searchArtists() 测试 ====================

    @Test
    @DisplayName("【关键回归】searchArtists 应正确填充 songCount，而非默认为 0")
    void testSearchArtists_shouldSetSongCount() {
        // 模拟搜索到 1 个艺人（周杰伦），有 10 首 ACTIVE 歌曲
        Artist artist = createArtist(1L, "周杰伦");
        when(artistMapper.searchByNameWithActiveSongs("周杰伦", 0, 20))
                .thenReturn(Collections.singletonList(artist));
        when(artistMapper.selectSongCountsByArtistIds(Collections.singletonList(1L)))
                .thenReturn(createCountResult(1L, 10));

        List<ArtistVO> result = artistService.searchArtists("周杰伦", 1, 20);

        assertEquals(1, result.size());
        assertEquals(10, result.get(0).getSongCount(),
                "搜索周杰伦应返回 10 首歌曲（修复前为 0）");
    }

    @Test
    @DisplayName("searchArtists 无搜索结果时返回空列表")
    void testSearchArtists_withNoResult() {
        when(artistMapper.searchByNameWithActiveSongs("不存在的艺人", 0, 20))
                .thenReturn(Collections.emptyList());

        List<ArtistVO> result = artistService.searchArtists("不存在的艺人", 1, 20);

        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("seachArtists 关键词为空时应返回空列表（不调用 Mapper）")
    void testSearchArtists_withBlankKeyword() {
        assertTrue(artistService.searchArtists("", 1, 20).isEmpty());
        assertTrue(artistService.searchArtists("   ", 1, 20).isEmpty());
        verify(artistMapper, never()).searchByNameWithActiveSongs(anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("【关键回归】searchArtists 按歌曲名搜索也能命中艺人（SQL 已扩展 LEFT JOIN song）")
    void testSearchArtists_bySongTitle() {
        // 搜索"晴天" → SQL 通过 LEFT JOIN song s ON s.artist_id = a.id
        // 匹配到 s.title LIKE '%晴天%' 的歌曲所属艺人
        Artist artist = createArtist(1L, "周杰伦");
        when(artistMapper.searchByNameWithActiveSongs("晴天", 0, 20))
                .thenReturn(Collections.singletonList(artist));
        when(artistMapper.selectSongCountsByArtistIds(Collections.singletonList(1L)))
                .thenReturn(createCountResult(1L, 5));

        List<ArtistVO> results = artistService.searchArtists("晴天", 1, 20);

        assertEquals(1, results.size(), "按歌曲名搜索应命中艺人");
        assertEquals("周杰伦", results.get(0).getName());
        assertEquals(5, results.get(0).getSongCount(), "应显示该艺人的 ACTIVE 歌曲数");
    }

    @Test
    @DisplayName("searchArtists 分页参数应正确传递")
    void testSearchArtists_withPagination() {
        // 第 3 页，每页 5 条 → offset = 10
        when(artistMapper.searchByNameWithActiveSongs("关键词", 10, 5))
                .thenReturn(Collections.emptyList());

        artistService.searchArtists("关键词", 3, 5);
        verify(artistMapper).searchByNameWithActiveSongs(eq("关键词"), eq(10), eq(5));
    }

    @Test
    @DisplayName("searchArtists 仅返回有 ACTIVE 歌曲的艺人（SQL EXISTS 子查询过滤）")
    void testSearchArtists_onlyActiveSongs() {
        // 验证：SQL 中已固定 AND EXISTS (SELECT 1 FROM song s2 WHERE s2.artist_id = a.id AND s2.status = 'ACTIVE')
        // 所以即使搜索到"过气艺人"，如果其歌曲全是 PENDING/REJECTED，也不会返回
        when(artistMapper.searchByNameWithActiveSongs("过气艺人", 0, 20))
                .thenReturn(Collections.emptyList());
        when(artistMapper.countSearchWithActiveSongs("过气艺人")).thenReturn(0L);

        assertTrue(artistService.searchArtists("过气艺人", 1, 20).isEmpty());
        assertEquals(0, artistService.countSearch("过气艺人"), "无 ACTIVE 歌曲的艺人不应出现在搜索结果中");
    }

    @Test
    @DisplayName("searchArtists 返回多个艺人时全部可枚举")
    void testSearchArtists_multipleResults() {
        Artist a1 = createArtist(1L, "刘德华");
        Artist a2 = createArtist(2L, "张学友");
        when(artistMapper.searchByNameWithActiveSongs("华", 0, 20))
                .thenReturn(Arrays.asList(a1, a2));
        when(artistMapper.countSearchWithActiveSongs("华")).thenReturn(2L);
        when(artistMapper.selectSongCountsByArtistIds(Arrays.asList(1L, 2L)))
                .thenReturn(Collections.emptyList());

        List<ArtistVO> results = artistService.searchArtists("华", 1, 20);

        assertEquals(2, results.size(), "应返回 2 个匹配的艺人");
        assertEquals(2, artistService.countSearch("华"), "搜索结果总数应为 2");
    }

    // ==================== getArtistDetail() 测试（验证已有逻辑未被破坏） ====================

    @Test
    @DisplayName("getArtistDetail 应使用 songs.size() 设置 songCount（已有逻辑未被破坏）")
    void testGetArtistDetail_shouldSetSongCountFromSongsList() {
        Artist artist = createArtist(1L, "林俊杰");
        when(artistMapper.selectById(1L)).thenReturn(artist);
        // 模拟该艺人有 7 首歌曲（不受 ACTIVE 状态限制，详情页展示全部）
        when(songMapper.selectAllByArtistId(1L)).thenReturn(
                Arrays.asList(null, null, null, null, null, null, null) // 只关心数量
        );
        when(albumMapper.selectByArtistId(1L)).thenReturn(Collections.emptyList());

        ArtistVO result = artistService.getArtistDetail(1L);

        assertEquals(7, result.getSongCount(),
                "详情页 songCount 应等于全部歌曲数量（不受 ACTIVE 过滤）");
    }

    // ==================== createArtist() 简单测试 ====================

    @Test
    @DisplayName("createArtist 应返回无 songCount 的基础 VO（新建艺人无歌曲）")
    void testCreateArtist_shouldReturnBasicVO() {
        Artist artist = createArtist(3L, "新艺人");
        when(artistMapper.insert(artist)).thenReturn(1);

        ArtistVO result = artistService.createArtist(artist);

        assertEquals("新艺人", result.getName());
        assertEquals(0, result.getSongCount(), "新创建的艺人歌曲数应为 0");
    }
}
