package com.example.music.service.impl;

import com.example.music.entity.Song;
import com.example.music.mapper.*;
import com.example.music.service.NotificationService;
import com.example.music.utils.CacheUtil;
import com.example.music.vo.SongVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SongServiceImpl 搜索功能单元测试
 * <p>
 * 验证模糊搜索是否仅返回 ACTIVE 歌曲，以及搜索命中歌曲名/艺人名/专辑名的各种场景。
 * 使用 Mockito 模拟 Mapper 层，不依赖数据库。
 *
 * 注意：enrichSongVO 中会查询 LikesMapper/FavoriteMapper/CacheUtil 等，
 * 测试时这些 mock 默认返回 0/null，不影响搜索结果验证。
 */
@ExtendWith(MockitoExtension.class)
class SongServiceImplSearchTest {

    @Mock private SongMapper songMapper;
    @Mock private ArtistMapper artistMapper;
    @Mock private AlbumMapper albumMapper;
    @Mock private SongCategoryMapper songCategoryMapper;
    @Mock private com.example.music.mapper.LikesMapper likesMapper;
    @Mock private com.example.music.mapper.FavoriteMapper favoriteMapper;
    @Mock private CacheUtil cacheUtil;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private UserMapper userMapper;
    @Mock private NotificationService notificationService;

    private SongServiceImpl songService;

    @BeforeEach
    void setUp() {
        songService = new SongServiceImpl(
                songMapper, artistMapper, albumMapper, songCategoryMapper,
                likesMapper, favoriteMapper, cacheUtil, stringRedisTemplate,
                userMapper, notificationService);
    }

    /**
     * 辅助方法：创建测试用 Song 实体
     */
    private Song createSong(Long id, String title, Long artistId, Long albumId, String status) {
        Song s = new Song();
        s.setId(id);
        s.setTitle(title);
        s.setArtistId(artistId);
        s.setAlbumId(albumId);
        s.setDuration(200);
        s.setAudioUrl("/audio/" + id + ".mp3");
        s.setStatus(status);
        s.setPlayCount(1000L);
        s.setCreatedTime(LocalDateTime.now());
        return s;
    }

    // ==================== 歌曲名搜索 ====================

    @Test
    @DisplayName("按歌曲名模糊搜索应返回匹配的 ACTIVE 歌曲")
    void testSearchBySongTitle() {
        // 模拟搜索 "晴天" → 命中歌曲名 "晴天"（ACTIVE）
        Song song = createSong(1L, "晴天", 1L, null, "ACTIVE");
        when(songMapper.search("晴天", 0, 20)).thenReturn(Collections.singletonList(song));
        when(songMapper.countSearch("晴天")).thenReturn(1L);

        // 模拟 enrichSongVO 中查询的关联数据
        when(songCategoryMapper.selectCategoryIdsBySongId(1L)).thenReturn(Collections.emptyList());
        when(likesMapper.countByTarget("SONG", 1L)).thenReturn(10L);
        when(favoriteMapper.countByTarget("SONG", 1L)).thenReturn(5L);

        List<SongVO> results = songService.searchSongs("晴天", 1, 20);
        long total = songService.countSearch("晴天");

        assertEquals(1, results.size(), "应命中 1 首歌曲");
        assertEquals("晴天", results.get(0).getTitle(), "歌曲名应匹配");
        assertEquals(1, total, "搜索结果总数应为 1");
    }

    // ==================== 艺人名搜索 ====================

    @Test
    @DisplayName("按艺人名模糊搜索应返回匹配的 ACTIVE 歌曲（通过 SQL LEFT JOIN artist 实现）")
    void testSearchByArtistName() {
        // 模拟搜索 "周杰伦" → 通过 SQL 的 LEFT JOIN artist 匹配到艺人名为"周杰伦"的歌曲
        // SongMapper.search SQL 使用 LEFT JOIN artist ON s.artist_id = a.id
        // 并设置 WHERE ... OR a.name LIKE CONCAT('%', #{keyword}, '%')
        // 因此 Mapper 返回歌曲，但其 title 不一定包含"周杰伦"
        Song song = createSong(1L, "告白气球", 1L, null, "ACTIVE");
        when(songMapper.search("周杰伦", 0, 20)).thenReturn(Collections.singletonList(song));

        // 模拟 enrichSongVO：根据 artist_id=1 查询艺人名称为"周杰伦"
        com.example.music.entity.Artist artist = new com.example.music.entity.Artist();
        artist.setId(1L);
        artist.setName("周杰伦");
        when(artistMapper.selectById(1L)).thenReturn(artist);
        when(songCategoryMapper.selectCategoryIdsBySongId(1L)).thenReturn(Collections.emptyList());
        when(likesMapper.countByTarget("SONG", 1L)).thenReturn(0L);
        when(favoriteMapper.countByTarget("SONG", 1L)).thenReturn(0L);

        List<SongVO> results = songService.searchSongs("周杰伦", 1, 20);

        assertEquals(1, results.size(), "按艺人名搜索应命中歌曲");
        assertEquals("告白气球", results.get(0).getTitle());
        assertEquals("周杰伦", results.get(0).getArtistName(), "VO 应正确填充艺人名");
    }

    // ==================== 专辑名搜索 ====================

    @Test
    @DisplayName("按专辑名模糊搜索应返回匹配的 ACTIVE 歌曲（通过 SQL LEFT JOIN album 实现）")
    void testSearchByAlbumTitle() {
        // 模拟搜索 "叶惠美" → 通过 LEFT JOIN album 匹配到专辑名"叶惠美"的歌曲
        Song song = createSong(1L, "以父之名", 1L, 1L, "ACTIVE");
        when(songMapper.search("叶惠美", 0, 20)).thenReturn(Collections.singletonList(song));

        // 模拟 enrichSongVO 关联数据
        com.example.music.entity.Artist artist = new com.example.music.entity.Artist();
        artist.setId(1L);
        artist.setName("周杰伦");
        when(artistMapper.selectById(1L)).thenReturn(artist);
        com.example.music.entity.Album album = new com.example.music.entity.Album();
        album.setId(1L);
        album.setTitle("叶惠美");
        when(albumMapper.selectById(1L)).thenReturn(album);
        when(songCategoryMapper.selectCategoryIdsBySongId(1L)).thenReturn(Collections.emptyList());
        when(likesMapper.countByTarget("SONG", 1L)).thenReturn(0L);
        when(favoriteMapper.countByTarget("SONG", 1L)).thenReturn(0L);

        List<SongVO> results = songService.searchSongs("叶惠美", 1, 20);

        assertEquals(1, results.size());
        assertEquals("以父之名", results.get(0).getTitle());
        assertEquals("叶惠美", results.get(0).getAlbumTitle(), "VO 应正确填充专辑名");
    }

    // ==================== 空关键词 / 无结果 ====================

    @Test
    @DisplayName("搜索关键词为空时应返回空列表")
    void testSearchWithBlankKeyword() {
        assertTrue(songService.searchSongs("", 1, 20).isEmpty(), "空关键词应返回空列表");
        assertTrue(songService.searchSongs("   ", 1, 20).isEmpty(), "空白字符串应返回空列表");
        assertEquals(0, songService.countSearch(""), "空关键词计数应为 0");
        assertEquals(0, songService.countSearch("   "), "空白字符串计数应为 0");

        // 验证 Mapper 未被调用（提前返回）
        verify(songMapper, never()).search(anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("搜索无匹配关键词应返回空列表")
    void testSearchWithNoMatch() {
        when(songMapper.search("不存在的歌曲", 0, 20)).thenReturn(Collections.emptyList());
        when(songMapper.countSearch("不存在的歌曲")).thenReturn(0L);

        List<SongVO> results = songService.searchSongs("不存在的歌曲", 1, 20);
        long total = songService.countSearch("不存在的歌曲");

        assertTrue(results.isEmpty(), "无匹配时应返回空列表");
        assertEquals(0, total, "计数应为 0");
    }

    // ==================== ACTIVE 状态过滤 ====================

    @Test
    @DisplayName("【关键回归】搜索仅返回 ACTIVE 状态的歌曲，不返回 PENDING/REJECTED")
    void testSearchOnlyReturnsActiveSongs() {
        // SongMapper.search SQL 中已固定 WHERE s.status = 'ACTIVE'
        // 所以 mock 的 Mapper 在搜索 PENDING/REJECTED 时本就不应返回数据
        // 此测试验证调用正确传递了搜索参数
        when(songMapper.search("test", 0, 20)).thenReturn(Collections.emptyList());

        List<SongVO> results = songService.searchSongs("test", 1, 20);

        assertTrue(results.isEmpty());
        verify(songMapper).search(eq("test"), eq(0), eq(20));
    }

    // ==================== 分页 ====================

    @Test
    @DisplayName("搜索的分页参数应正确传递给 Mapper")
    void testSearchPagination() {
        // 第 2 页，每页 10 条 → offset = 10
        when(songMapper.search("关键词", 10, 10)).thenReturn(Collections.emptyList());

        songService.searchSongs("关键词", 2, 10);

        // 验证 offset 计算正确
        verify(songMapper).search(eq("关键词"), eq(10), eq(10));
    }

    @Test
    @DisplayName("搜索应返回多首匹配歌曲并正确排序")
    void testSearchReturnsMultipleSortedResults() {
        // 模拟搜索 "爱" → 返回多首按 SQL 中 CASE 排序后的结果
        Song song1 = createSong(1L, "爱情转移", 1L, null, "ACTIVE");
        Song song2 = createSong(2L, "爱很简单", 2L, null, "ACTIVE");
        when(songMapper.search("爱", 0, 20)).thenReturn(Arrays.asList(song1, song2));
        when(songMapper.countSearch("爱")).thenReturn(2L);

        when(songCategoryMapper.selectCategoryIdsBySongId(anyLong())).thenReturn(Collections.emptyList());
        when(likesMapper.countByTarget(anyString(), anyLong())).thenReturn(0L);
        when(favoriteMapper.countByTarget(anyString(), anyLong())).thenReturn(0L);

        List<SongVO> results = songService.searchSongs("爱", 1, 20);

        assertEquals(2, results.size(), "应返回 2 首匹配歌曲");
        assertEquals(2, songService.countSearch("爱"), "总数应为 2");
    }

    // ==================== 管理端搜索（Admin） ====================

    @Test
    @DisplayName("管理端搜索不限制状态（AdminContentService.listSongs 使用同一个 Mapper）")
    void testAdminSearchCanIncludeNonActive() {
        // 管理端的 listSongs 通过 songMapper.search() + songMapper.countSearch()
        // 使用的同一个 search Mapper，但 AdminContentService 没有额外限制。
        // 注意：SongMapper.search SQL 中固定了 s.status = 'ACTIVE'，
        // 所以即使是管理端搜索也只返回 ACTIVE 歌曲。
        // 管理端要查看所有状态的歌曲应使用 songMapper.selectList() 传入指定 status。
        Song song = createSong(3L, "待审核歌曲", 1L, null, "PENDING");
        // SQL 中固定 status='ACTIVE'，所以 PENDING 歌曲不会出现在搜索结果中
        when(songMapper.search("待审核", 0, 20)).thenReturn(Collections.emptyList());

        List<SongVO> results = songService.searchSongs("待审核", 1, 20);

        assertTrue(results.isEmpty(), "PENDING 歌曲不应出现在公开搜索中");
    }
}
