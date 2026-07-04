package com.example.music.controller;

import com.example.music.aspect.OperationLog;
import com.example.music.dto.AuditSongDTO;
import com.example.music.dto.BannerDTO;
import com.example.music.dto.HandleReportDTO;
import com.example.music.dto.NoticeDTO;
import com.example.music.entity.Artist;
import com.example.music.entity.Song;
import com.example.music.service.*;
import com.example.music.utils.RequestContext;
import com.example.music.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 管理后台控制器
 * <p>
 * 所有接口需要管理员权限（JwtInterceptor 拦截器自动校验 role = ADMIN）。
 * 统一的 REST 设计：GET 查询、POST 创建、PUT 更新/操作、DELETE 删除。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminDashboardService adminDashboardService;
    private final AdminUserService adminUserService;
    private final AdminContentService adminContentService;
    private final AdminReportService adminReportService;
    private final AdminBannerService adminBannerService;
    private final AdminNoticeService adminNoticeService;
    private final AdminLogService adminLogService;

    // ==================== 数据看板 ====================

    /**
     * 获取数据看板概览
     * <p>
     * 返回平台核心统计数据：用户数、歌曲数、播放量、待处理事项等。
     */
    @GetMapping("/dashboard")
    public R<DashboardVO> getDashboard() {
        return R.ok(adminDashboardService.getDashboard());
    }

    // ==================== 用户管理 ====================

    /**
     * 用户列表（分页，支持按昵称/邮箱搜索）
     *
     * @param keyword 搜索关键字（可选）
     * @param page    页码
     * @param size    每页条数
     */
    @GetMapping("/users")
    public R<Map<String, Object>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(adminUserService.listUsers(keyword, page, size));
    }

    /**
     * 封禁/解封用户
     *
     * @param id     目标用户 ID
     * @param banned true=封禁, false=解封
     */
    @OperationLog(value = "封禁/解封用户", targetType = "USER")
    @PutMapping("/users/{id}/ban")
    public R<Object> banUser(@PathVariable Long id,
                             @RequestParam(defaultValue = "true") boolean banned) {
        Long adminId = RequestContext.getUserId();
        adminUserService.banUser(adminId, id, banned);
        return R.ok(banned ? "用户已封禁" : "用户已解封");
    }

    // ==================== 歌曲管理 ====================

    /**
     * 歌曲列表（管理端，可查看所有状态）
     *
     * @param status  筛选状态（可选：ACTIVE / PENDING / REJECTED）
     * @param keyword 搜索关键字（可选）
     * @param page    页码
     * @param size    每页条数
     */
    @GetMapping("/songs")
    public R<Map<String, Object>> listSongs(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(adminContentService.listSongs(status, keyword, page, size));
    }

    /**
     * 获取歌曲详情（管理端，含艺人名）
     */
    @GetMapping("/songs/{id}")
    public R<SongVO> getSong(@PathVariable Long id) {
        return R.ok(adminContentService.getSongDetail(id));
    }

    /**
     * 创建歌曲（管理员直接添加）
     */
    @OperationLog(value = "创建歌曲", targetType = "SONG")
    @PostMapping("/songs")
    public R<SongVO> createSong(@RequestBody Song song) {
        Long adminId = RequestContext.getUserId();
        return R.ok("歌曲已创建", adminContentService.createSong(adminId, song));
    }

    /**
     * 更新歌曲信息
     */
    @OperationLog(value = "更新歌曲", targetType = "SONG")
    @PutMapping("/songs/{id}")
    public R<SongVO> updateSong(@PathVariable Long id,
                                 @RequestBody Song song) {
        Long adminId = RequestContext.getUserId();
        return R.ok("歌曲已更新", adminContentService.updateSong(adminId, id, song));
    }

    /**
     * 审核歌曲（会员上传审核）
     *
     * @param id  歌曲 ID
     * @param dto 审核参数（status + rejectReason）
     */
    @OperationLog(value = "审核歌曲", targetType = "SONG")
    @PutMapping("/songs/{id}/audit")
    public R<Object> auditSong(@PathVariable Long id,
                               @Valid @RequestBody AuditSongDTO dto) {
        Long adminId = RequestContext.getUserId();
        adminContentService.auditSong(adminId, id, dto.getStatus(), dto.getRejectReason());
        String msg = "ACTIVE".equals(dto.getStatus()) ? "歌曲审核通过" : "歌曲审核驳回";
        return R.ok(msg);
    }

    /**
     * 删除歌曲
     */
    @OperationLog(value = "删除歌曲", targetType = "SONG")
    @DeleteMapping("/songs/{id}")
    public R<Object> deleteSong(@PathVariable Long id) {
        Long adminId = RequestContext.getUserId();
        adminContentService.deleteSong(adminId, id);
        return R.ok("歌曲已删除");
    }

    /**
     * 批量导入歌曲（ZIP 上传）
     * <p>
     * ZIP 包内可包含：
     * - MP3 音频文件（自动提取作为歌曲源文件）
     * - 同名的 .lrc / .txt 歌词文件
     * - cover.jpg / cover.png 封面图片
     */
    @OperationLog(value = "批量导入歌曲", targetType = "SONG")
    @PostMapping("/songs/batch")
    public R<Map<String, Object>> batchImportSongs(@RequestParam("file") MultipartFile file) {
        Long adminId = RequestContext.getUserId();
        Map<String, Object> result = adminContentService.batchImportSongs(adminId, file);
        return R.ok("导入完成", result);
    }

    // ==================== 专辑管理 ====================

    /**
     * 专辑列表（管理端）
     */
    @GetMapping("/albums")
    public R<Map<String, Object>> listAlbums(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(adminContentService.listAlbums(keyword, page, size));
    }

    /**
     * 删除专辑
     */
    @OperationLog(value = "删除专辑", targetType = "ALBUM")
    @DeleteMapping("/albums/{id}")
    public R<Object> deleteAlbum(@PathVariable Long id) {
        Long adminId = RequestContext.getUserId();
        adminContentService.deleteAlbum(adminId, id);
        return R.ok("专辑已删除");
    }

    // ==================== 艺人管理 ====================

    /**
     * 艺人列表（管理端）
     */
    @GetMapping("/artists")
    public R<Map<String, Object>> listArtists(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(adminContentService.listArtists(keyword, page, size));
    }

    /**
     * 更新艺人信息（名称 / 头像 / 简介 / 国籍）
     * <p>
     * 管理员可修改艺人基本信息，头像 URL 由前端上传后传入。
     */
    @OperationLog(value = "更新艺人", targetType = "ARTIST")
    @PutMapping("/artists/{id}")
    public R<ArtistVO> updateArtist(@PathVariable Long id,
                                    @RequestBody Artist artist) {
        artist.setId(id);
        ArtistVO vo = adminContentService.updateArtist(artist);
        return R.ok("艺人信息已更新", vo);
    }

    /**
     * 删除艺人
     */
    @OperationLog(value = "删除艺人", targetType = "ARTIST")
    @DeleteMapping("/artists/{id}")
    public R<Object> deleteArtist(@PathVariable Long id) {
        Long adminId = RequestContext.getUserId();
        adminContentService.deleteArtist(adminId, id);
        return R.ok("艺人已删除");
    }

    // ==================== 评论管理 ====================

    /**
     * 评论列表（管理端，可筛选目标类型）
     *
     * @param targetType 目标类型（可选：SONG / PLAYLIST）
     */
    @GetMapping("/comments")
    public R<Map<String, Object>> listComments(
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(adminContentService.listComments(targetType, page, size));
    }

    /**
     * 管理员强制删除评论
     */
    @OperationLog(value = "删除评论", targetType = "COMMENT")
    @DeleteMapping("/comments/{id}")
    public R<Object> deleteComment(@PathVariable Long id) {
        Long adminId = RequestContext.getUserId();
        adminContentService.deleteComment(adminId, id);
        return R.ok("评论已删除");
    }

    // ==================== 举报管理 ====================

    /**
     * 举报列表（管理端，可按状态筛选）
     *
     * @param status 筛选状态（可选：PENDING / RESOLVED / DISMISSED）
     */
    @GetMapping("/reports")
    public R<Map<String, Object>> listReports(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(adminReportService.listReports(status, page, size));
    }

    /**
     * 处理举报
     *
     * @param id  举报 ID
     * @param dto 处理参数（status + handleNote）
     */
    @OperationLog(value = "处理举报", targetType = "REPORT")
    @PutMapping("/reports/{id}")
    public R<Object> handleReport(@PathVariable Long id,
                                  @Valid @RequestBody HandleReportDTO dto) {
        Long adminId = RequestContext.getUserId();
        adminReportService.handleReport(adminId, id, dto.getStatus(), dto.getHandleNote());
        String msg = "RESOLVED".equals(dto.getStatus()) ? "举报已确认处理" : "举报已驳回";
        return R.ok(msg);
    }

    // ==================== 轮播图管理 ====================

    /**
     * 轮播图列表（管理端，包含未启用的）
     */
    @GetMapping("/banners")
    public R<List<BannerVO>> listBanners() {
        return R.ok(adminBannerService.listBanners());
    }

    /**
     * 新增轮播图
     */
    @OperationLog(value = "新增轮播图", targetType = "BANNER")
    @PostMapping("/banners")
    public R<BannerVO> createBanner(@Valid @RequestBody BannerDTO dto) {
        return R.ok(adminBannerService.createBanner(dto));
    }

    /**
     * 更新轮播图
     */
    @OperationLog(value = "更新轮播图", targetType = "BANNER")
    @PutMapping("/banners/{id}")
    public R<BannerVO> updateBanner(@PathVariable Long id,
                                    @Valid @RequestBody BannerDTO dto) {
        BannerVO vo = adminBannerService.updateBanner(id, dto);
        if (vo == null) {
            return R.fail(404, "轮播图不存在");
        }
        return R.ok(vo);
    }

    /**
     * 删除轮播图
     */
    @OperationLog(value = "删除轮播图", targetType = "BANNER")
    @DeleteMapping("/banners/{id}")
    public R<Object> deleteBanner(@PathVariable Long id) {
        adminBannerService.deleteBanner(id);
        return R.ok("轮播图已删除");
    }

    // ==================== 系统公告管理 ====================

    /**
     * 公告列表（管理端，可按类型筛选）
     *
     * @param type 公告类型（可选：SYSTEM / MAINTENANCE / ACTIVITY）
     */
    @GetMapping("/notices")
    public R<Map<String, Object>> listNotices(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(adminNoticeService.listNotices(type, page, size));
    }

    /**
     * 新增公告
     */
    @OperationLog(value = "新增公告", targetType = "NOTICE")
    @PostMapping("/notices")
    public R<SystemNoticeVO> createNotice(@Valid @RequestBody NoticeDTO dto) {
        return R.ok(adminNoticeService.createNotice(dto));
    }

    /**
     * 更新公告
     */
    @OperationLog(value = "更新公告", targetType = "NOTICE")
    @PutMapping("/notices/{id}")
    public R<SystemNoticeVO> updateNotice(@PathVariable Long id,
                                          @Valid @RequestBody NoticeDTO dto) {
        SystemNoticeVO vo = adminNoticeService.updateNotice(id, dto);
        if (vo == null) {
            return R.fail(404, "公告不存在");
        }
        return R.ok(vo);
    }

    /**
     * 删除公告
     */
    @OperationLog(value = "删除公告", targetType = "NOTICE")
    @DeleteMapping("/notices/{id}")
    public R<Object> deleteNotice(@PathVariable Long id) {
        adminNoticeService.deleteNotice(id);
        return R.ok("公告已删除");
    }

    // ==================== 操作日志 ====================

    /**
     * 操作日志列表（分页，可按操作类型/操作人筛选）
     *
     * @param action     操作类型（可选）
     * @param operatorId 操作人 ID（可选）
     */
    @GetMapping("/logs")
    public R<Map<String, Object>> listLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(adminLogService.listLogs(action, operatorId, page, size));
    }

    // ==================== 统计报表 ====================

    /**
     * 统计报表（目前返回看板相同数据，后续可扩展趋势图数据）
     */
    @GetMapping("/statistics")
    public R<DashboardVO> getStatistics() {
        return R.ok(adminDashboardService.getDashboard());
    }
}
