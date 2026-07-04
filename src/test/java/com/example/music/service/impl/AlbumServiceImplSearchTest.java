package com.example.music.service.impl;

import com.example.music.entity.Album;
import com.example.music.mapper.AlbumMapper;
import com.example.music.mapper.ArtistMapper;
import com.example.music.mapper.SongMapper;
import com.example.music.vo.AlbumVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AlbumServiceImpl 搜索功能单元测试
 * <p>
 * 验证专辑模糊搜索（按专辑名 + 艺人名）是否仅返回有 ACTIVE 歌曲的专辑。
 * AlbumMapper.searchByNameWithActiveSongs 使用 EXISTS 子查询过滤，
 * 且 SQL 已加入分页参数（原实现无分页，本次新增）。
 */
@ExtendWith(MockitoExtension.class)
class AlbumServiceImplSearchTest {

    @Mock private AlbumMapper albumMapper;
    @Mock private ArtistMapper artistMapper;
    @Mock private SongMapper songMapper;

    private AlbumServiceImpl albumService;

    @BeforeEach
    void setUp() {
        albumService = new AlbumServiceImpl(albumMapper, artistMapper, songMapper);
    }

    /**
     * 辅助方法：创建测试用 Album 实体
     */
    private Album createAlbum(Long id, String title, Long artistId) {
        Album a = new Album();
        a.setId(id);
        a.setTitle(title);
        a.setArtistId(artistId);
        a.setCoverUrl("/covers/" + id + ".jpg");
        a.setDescription(title + " 的描述");
        a.setReleaseDate(LocalDate.of(2020, 1, 1));
        a.setCreatedTime(LocalDateTime.now());
        return a;
    }

    // ==================== 按专辑名搜索 ====================

    @Test
    @DisplayName("按专辑名模糊搜索应返回匹配的专辑（仅包含有 ACTIVE 歌曲的专辑）")
    void testSearchByAlbumTitle() {
        // 模拟搜索 "范特西" → 命中专辑名 "范特西"
        Album album = createAlbum(1L, "范特西", 1L);
        when(albumMapper.searchByNameWithActiveSongs("范特西", 0, 20))
                .thenReturn(Collections.singletonList(album));
        when(albumMapper.countSearchWithActiveSongs("范特西")).thenReturn(1L);

        // 模拟 setArtistName 填充艺人名
        com.example.music.entity.Artist artist = new com.example.music.entity.Artist();
        artist.setId(1L);
        artist.setName("周杰伦");
        when(artistMapper.selectById(1L)).thenReturn(artist);

        List<AlbumVO> results = albumService.searchAlbums("范特西", 1, 20);
        long total = albumService.countSearchAlbums("范特西");

        assertEquals(1, results.size(), "应命中 1 张专辑");
        assertEquals("范特西", results.get(0).getTitle(), "专辑名应匹配");
        assertEquals("周杰伦", results.get(0).getArtistName(), "VO 应正确填充艺人名");
        assertEquals(1, total, "搜索结果总数应为 1");
    }

    // ==================== 按艺人名搜索 ====================

    @Test
    @DisplayName("按艺人名模糊搜索应返回匹配的专辑（SQL 已扩展 LEFT JOIN artist）")
    void testSearchByArtistName() {
        // 搜索 "周杰伦" → 通过 SQL 的 LEFT JOIN artist ON al.artist_id = a.id
        // 匹配到 a.name LIKE '%周杰伦%' 的专辑
        Album album = createAlbum(1L, "十一月的萧邦", 1L);
        when(albumMapper.searchByNameWithActiveSongs("周杰伦", 0, 20))
                .thenReturn(Collections.singletonList(album));

        com.example.music.entity.Artist artist = new com.example.music.entity.Artist();
        artist.setId(1L);
        artist.setName("周杰伦");
        when(artistMapper.selectById(1L)).thenReturn(artist);

        List<AlbumVO> results = albumService.searchAlbums("周杰伦", 1, 20);

        assertEquals(1, results.size(), "按艺人名搜索应命中专辑");
        assertEquals("十一月的萧邦", results.get(0).getTitle());
        assertEquals("周杰伦", results.get(0).getArtistName());
    }

    // ==================== 空关键词 / 无结果 ====================

    @Test
    @DisplayName("搜索关键词为空时应返回空列表（不调用 Mapper）")
    void testSearchWithBlankKeyword() {
        assertTrue(albumService.searchAlbums("", 1, 20).isEmpty(), "空关键词应返回空列表");
        assertTrue(albumService.searchAlbums("   ", 1, 20).isEmpty(), "空白字符串应返回空列表");
        assertEquals(0, albumService.countSearchAlbums(""), "空关键词计数应为 0");
        assertEquals(0, albumService.countSearchAlbums("   "), "空白字符串计数应为 0");

        verify(albumMapper, never()).searchByNameWithActiveSongs(anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("搜索无匹配时返回空列表")
    void testSearchWithNoMatch() {
        when(albumMapper.searchByNameWithActiveSongs("不存在的专辑", 0, 20))
                .thenReturn(Collections.emptyList());
        when(albumMapper.countSearchWithActiveSongs("不存在的专辑")).thenReturn(0L);

        List<AlbumVO> results = albumService.searchAlbums("不存在的专辑", 1, 20);

        assertTrue(results.isEmpty());
        assertEquals(0, albumService.countSearchAlbums("不存在的专辑"));
    }

    // ==================== ACTIVE 状态过滤 ====================

    @Test
    @DisplayName("【关键回归】搜索仅返回有 ACTIVE 歌曲的专辑（通过 SQL EXISTS 子查询过滤）")
    void testSearchOnlyReturnsAlbumsWithActiveSongs() {
        // searchByNameWithActiveSongs 在 SQL 中使用 EXISTS 子查询：
        // WHERE ... AND EXISTS (SELECT 1 FROM song s WHERE s.album_id = al.id AND s.status = 'ACTIVE')
        // 因此对于没有 ACTIVE 歌曲的专辑，Mapper 返回空
        when(albumMapper.searchByNameWithActiveSongs("test", 0, 20))
                .thenReturn(Collections.emptyList());

        assertTrue(albumService.searchAlbums("test", 1, 20).isEmpty());
    }

    // ==================== 分页 ====================

    @Test
    @DisplayName("【关键回归】专辑搜索支持分页（原实现无分页，本次新增 LIMIT/OFFSET）")
    void testSearchWithPagination() {
        // 第 2 页，每页 10 条 → offset = 10
        Album album1 = createAlbum(1L, "专辑A", 1L);
        Album album2 = createAlbum(2L, "专辑B", 2L);
        when(albumMapper.searchByNameWithActiveSongs("专辑", 10, 10))
                .thenReturn(Arrays.asList(album1, album2));
        when(albumMapper.countSearchWithActiveSongs("专辑")).thenReturn(15L);

        com.example.music.entity.Artist artist = new com.example.music.entity.Artist();
        artist.setId(1L);
        artist.setName("艺人A");
        when(artistMapper.selectById(1L)).thenReturn(artist);

        List<AlbumVO> results = albumService.searchAlbums("专辑", 2, 10);

        // 验证分页参数已正确传递
        verify(albumMapper).searchByNameWithActiveSongs(eq("专辑"), eq(10), eq(10));
        assertEquals(2, results.size(), "第 2 页应返回 2 条记录");
        assertEquals(15, albumService.countSearchAlbums("专辑"), "总数为 15");
    }

    @Test
    @DisplayName("专辑搜索结果应包含艺人名")
    void testSearchResultHasArtistName() {
        Album album = createAlbum(1L, "Jay", 1L);
        when(albumMapper.searchByNameWithActiveSongs("Jay", 0, 20))
                .thenReturn(Collections.singletonList(album));

        com.example.music.entity.Artist artist = new com.example.music.entity.Artist();
        artist.setId(1L);
        artist.setName("周杰伦");
        when(artistMapper.selectById(1L)).thenReturn(artist);

        List<AlbumVO> results = albumService.searchAlbums("Jay", 1, 20);

        assertEquals("周杰伦", results.get(0).getArtistName(), "搜索结果应带上艺人名");
    }

    // ==================== 多结果 ====================

    @Test
    @DisplayName("搜索返回多个专辑时全部可枚举")
    void testSearchReturnsMultiple() {
        Album a1 = createAlbum(1L, "幻想", 1L);
        Album a2 = createAlbum(2L, "空想", 2L);
        when(albumMapper.searchByNameWithActiveSongs("想", 0, 20))
                .thenReturn(Arrays.asList(a1, a2));
        when(albumMapper.countSearchWithActiveSongs("想")).thenReturn(2L);

        List<AlbumVO> results = albumService.searchAlbums("想", 1, 20);

        assertEquals(2, results.size(), "应返回 2 张专辑");
        assertEquals(2, albumService.countSearchAlbums("想"), "总数应为 2");
    }
}
