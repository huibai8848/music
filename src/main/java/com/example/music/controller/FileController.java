package com.example.music.controller;

import com.example.music.constant.ErrorCode;
import com.example.music.exception.BusinessException;
import com.example.music.service.FileService;
import com.example.music.vo.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传控制器
 * <p>
 * 提供通用文件上传接口，支持音频/封面/歌词/头像/背景图/语音消息。
 * 文件上传后返回可访问的 URL 路径。
 * <p>
 * 访问已上传的文件通过静态资源映射：{@code /api/files/{type}/...}
 * 由 {@link com.example.music.config.WebMvcConfig} 配置。
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 通用文件上传
     * <p>
     * 请求方式：multipart/form-data
     *
     * @param file   上传的文件
     * @param type   文件类型（audio/cover/lyric/avatar/background/voice）
     * @param roomId 房间 ID（语音消息时必填）
     * @return 上传结果，包含文件 URL
     * <pre>
     * {
     *   "code": 200,
     *   "message": "上传成功",
     *   "data": {
     *     "url": "/api/files/audio/2026/07/uuid.mp3",
     *     "originalName": "song.mp3",
     *     "size": 1234567
     *   }
     * }
     * </pre>
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type,
            @RequestParam(value = "roomId", required = false) Long roomId) {

        if (file.isEmpty()) {
            return R.fail(ErrorCode.BAD_REQUEST.getCode(), "文件不能为空");
        }

        // 禁止通过通用上传接口上传音频文件 → 应使用 /api/members/upload-music 完成完整提交流程
        if ("audio".equals(type)) {
            return R.fail(ErrorCode.FILE_TYPE_NOT_ALLOWED.getCode(),
                    "音乐文件请通过「上传音乐」页面提交（含艺人/专辑元数据）");
        }

        String url = fileService.upload(file, type, roomId);

        Map<String, Object> data = new HashMap<>();
        data.put("url", url);
        data.put("originalName", file.getOriginalFilename());
        data.put("size", file.getSize());
        data.put("type", type);

        return R.ok("上传成功", data);
    }
}
