package com.example.music.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.example.music.constant.ErrorCode;
import com.example.music.exception.BusinessException;
import com.example.music.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * 文件服务实现
 * <p>
 * 核心逻辑：
 * 1. 校验文件类型（MIME + 魔数双重验证）
 * 2. 校验文件大小（不同类型不同限制）
 * 3. UUID 重命名 + 按日期分目录存储
 * 4. 返回可访问的 URL 路径
 * <p>
 * 存储结构：
 * <pre>
 * data/music/
 * ├── audio/{yyyy/mm}/{uuid}.mp3      # 音频文件（MP3/AAC，≤50MB）
 * ├── cover/{yyyy/mm}/{uuid}.jpg      # 封面图（≤5MB）
 * ├── lyric/{yyyy/mm}/{uuid}.lrc      # 歌词文件（≤100KB）
 * ├── avatar/{yyyy/mm}/{uuid}.jpg     # 用户头像（≤5MB）
 * ├── background/{yyyy/mm}/{uuid}.jpg # 背景图（≤5MB）
 * └── voice/{roomId}/{uuid}.webm      # 语音消息（≤5MB）
 * </pre>
 */
@Slf4j
@Service
public class FileServiceImpl implements FileService {

    /** 文件存储根目录（从配置注入，与 WebMvcConfig 静态资源映射保持一致） */
    @Value("${music.file.upload-dir:data/music}")
    private String uploadDir;

    @jakarta.annotation.PostConstruct
    public void init() {
        // 确保 uploadDir 是绝对路径
        File dir = new File(uploadDir);
        if (!dir.isAbsolute()) {
            uploadDir = new File(System.getProperty("user.dir"), uploadDir).getAbsolutePath();
        }
        log.info("文件上传根目录: {}", uploadDir);
    }

    /** 允许的文件类型集合 */
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "audio", "cover", "lyric", "avatar", "background", "voice"
    );

    /** 各类型允许的 MIME 类型（大小写不敏感，统一用小写匹配） */
    private static final Set<String> AUDIO_MIME = Set.of(
            "audio/mpeg", "audio/mp3",       // MP3
            "audio/aac", "audio/x-aac",       // AAC raw / ADTS
            "audio/mp4", "audio/x-m4a",       // AAC in MP4/M4A container
            "application/octet-stream");       // 兜底（客户端未发送 Content-Type 时）
    private static final Set<String> IMAGE_MIME = Set.of("image/jpeg", "image/png", "application/octet-stream");
    private static final Set<String> LYRIC_MIME = Set.of("text/plain", "text/lrc", "application/octet-stream");
    private static final Set<String> VOICE_MIME = Set.of("audio/webm", "audio/ogg", "audio/wav");

    /** 各类型文件大小限制（字节） */
    private static final long MAX_AUDIO_SIZE = 50 * 1024 * 1024L;     // 50MB
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024L;      // 5MB
    private static final long MAX_LYRIC_SIZE = 100 * 1024L;           // 100KB
    private static final long MAX_VOICE_SIZE = 5 * 1024 * 1024L;      // 5MB

    /** 各类型对应的扩展名（音频统一用 .mp3 存储，不影响实际编码格式） */
    private static final String EXT_AUDIO = ".mp3";
    private static final String EXT_COVER = ".jpg";
    private static final String EXT_LYRIC = ".lrc";
    private static final String EXT_AVATAR = ".jpg";
    private static final String EXT_BACKGROUND = ".jpg";
    private static final String EXT_VOICE = ".webm";

    /**
     * 上传文件
     *
     * @param file   上传的文件
     * @param type   文件类型
     * @param roomId 语音消息的房间 ID
     * @return 文件访问相对 URL
     */
    @Override
    public String upload(MultipartFile file, String type, Long roomId) {
        // 1. 校验文件类型
        if (!ALLOWED_TYPES.contains(type)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "不支持的文件类型: " + type);
        }

        // 2. 检查文件是否为空
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();

        // 3. 校验扩展名（大小写不敏感：.MP3 和 .mp3 均接受；空格/引号不影响存储）
        validateFileExtension(type, originalFilename);

        // 4. 校验 MIME 类型（忽略参数部分）
        validateMimeType(type, contentType);

        // 5. 校验文件大小
        validateFileSize(type, file.getSize());

        // 6. 读取文件字节（用于魔数校验和写入）
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("读取文件字节失败: {}", originalFilename, e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "读取文件失败");
        }

        // 7. 魔数校验
        validateMagicNumber(type, bytes);

        // 8. 生成存储路径（使用 UUID 重命名，避免原始文件名中的空格/引号对文件系统造成影响）
        String relativePath = buildStoragePath(type, originalFilename, roomId);

        // 9. 写入磁盘
        String absolutePath = uploadDir + "/" + relativePath;
        try {
            FileUtil.writeBytes(bytes, absolutePath);
            log.info("文件上传成功: type={}, size={}, path={}", type, file.getSize(), absolutePath);
        } catch (Exception e) {
            log.error("文件写入失败: {}", absolutePath, e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "文件写入磁盘失败");
        }

        // 10. 返回访问 URL
        return "/api/files/" + relativePath.replace("\\", "/");
    }

    // ==================== 魔数校验 ====================

    /**
     * 校验文件魔数（Magic Number），防止篡改扩展名
     * <p>
     * 音频格式检测：
     * <ul>
     *   <li>MP3 — 扫描 MPEG 同步字 0xFFF（跳过 ID3v2 标签头）</li>
     *   <li>AAC — 检测 MP4 容器的 ftyp box（偏移 4 处）</li>
     * </ul>
     */
    private void validateMagicNumber(String type, byte[] bytes) {
        switch (type) {
            case "audio":
                // 需要至少 2 字节来校验同步字
                // MP3 可能带 ID3v2 标签头（"ID3"开头），需要跳过后再找同步字
                int mp3Offset = 0;
                if (bytes.length >= 3 && bytes[0] == 0x49 && bytes[1] == 0x44 && bytes[2] == 0x33) {
                    // ID3v2 标签头: "ID3"(3) + version(2) + flags(1) + size(4 synchsafe) = 10 字节
                    if (bytes.length >= 10) {
                        int id3Size = ((bytes[6] & 0x7F) << 21) |
                                      ((bytes[7] & 0x7F) << 14) |
                                      ((bytes[8] & 0x7F) << 7) |
                                      (bytes[9] & 0x7F);
                        mp3Offset = 10 + id3Size;
                    } else {
                        mp3Offset = 10; // 头不完整，保守跳过
                    }
                }
                // 在跳过标签后的位置校验 MP3 同步字: 0xFF 0xFB（或 0xFF 0xF3 / 0xFF 0xF2）
                boolean isMp3 = mp3Offset + 1 < bytes.length
                        && bytes[mp3Offset] == (byte) 0xFF
                        && (bytes[mp3Offset + 1] & 0xF0) == 0xF0;
                // 不是 MP3 则检查是否为 AAC（MP4/M4A 容器）
                // MP4 文件以 ftyp（File Type）box 开头，位于偏移 4 处
                boolean isAacMp4 = !isMp3 && bytes.length >= 8
                        && bytes[4] == 0x66  // 'f'
                        && bytes[5] == 0x74  // 't'
                        && bytes[6] == 0x79  // 'y'
                        && bytes[7] == 0x70; // 'p'
                if (!isMp3 && !isAacMp4) {
                    throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "音频文件格式无效，仅支持 MP3 / AAC");
                }
                break;

            case "cover":
            case "avatar":
            case "background":
                // 图片至少需要 4 字节来校验魔数
                if (bytes.length < 4) {
                    throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "图片文件内容无效");
                }
                // JPEG: FF D8 FF
                if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) {
                    return; // JPEG 通过
                }
                // PNG: 89 50 4E 47
                if (bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
                    return; // PNG 通过
                }
                throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "图片格式无效，仅支持 JPEG/PNG");

            case "lyric":
            case "voice":
                // 歌词文件（纯文本）和语音（WebM）不做魔数校验
                break;

            default:
                throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "不支持的文件类型");
        }
    }

    // ==================== 扩展名校验 ====================

    /**
     * 校验文件扩展名（大小写不敏感）
     * <p>
     * 与 MIME 校验和魔数校验形成三层防御。
     * 检查逻辑：
     * - 将原始文件名统一转为小写后比较，确保 .MP3 / .mp3 / .Mp3 大小写变体均被接受
     * - 同时支持 .AAC / .aac / .M4A / .m4a 扩展名
     * - 文件名中的空格、引号等特殊字符不影响校验结果
     * <p>
     * 命名安全：
     * - 数据库存储：所有 MyBatis SQL 均使用 #{ } 预编译（PreparedStatement），
     *   原始文件名中的空格、引号不会造成 SQL 注入风险
     * - 文件系统存储：实际存储使用 UUID 重命名（详见 buildStoragePath），
     *   原始文件名中的空格、引号等特殊字符不会对文件系统造成影响
     */
    private void validateFileExtension(String type, String originalFilename) {
        if (StrUtil.isBlank(originalFilename)) {
            return; // 无文件名时跳过扩展名校验（魔数校验兜底）
        }

        // 统一转为小写进行匹配，保证各扩展名的大小写变体均被正确识别
        // 音频：.mp3 / .MP3 / .aac / .AAC / .m4a / .M4A 均通过
        String lowerName = originalFilename.toLowerCase();

        boolean valid = switch (type) {
            // 音频支持 MP3 和 AAC 格式（大小写不敏感）
            case "audio" ->
                    lowerName.endsWith(".mp3") || lowerName.endsWith(".aac") || lowerName.endsWith(".m4a");
            // 封面/头像/背景支持 JPG/JPEG/PNG（大小写不敏感）
            case "cover", "avatar", "background" ->
                    lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png");
            // 歌词支持 LRC/TXT
            case "lyric" -> lowerName.endsWith(".lrc") || lowerName.endsWith(".txt");
            // 语音消息支持 WebM/OGG/WAV
            case "voice" -> lowerName.endsWith(".webm") || lowerName.endsWith(".ogg") || lowerName.endsWith(".wav");
            default -> true;
        };

        if (!valid) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED,
                    "文件扩展名不匹配（当前文件: " + originalFilename + "）");
        }
    }

    // ==================== MIME 校验 ====================

    /**
     * 校验 MIME 类型（大小写不敏感，忽略参数部分）
     * <p>
     * 某些客户端会携带参数，例如 {@code audio/mpeg; charset=utf-8} 或
     * {@code image/png; name="test.png"}，这里只取 {@code ;} 之前的主体部分进行匹配。
     */
    private void validateMimeType(String type, String contentType) {
        if (StrUtil.isBlank(contentType)) {
            return; // 某些客户端不发送 Content-Type，跳过 MIME 校验
        }

        // 只取 MIME 主体部分（去掉 ; 之后的参数）
        String lowerContentType = contentType.toLowerCase().split(";")[0].trim();

        boolean valid = switch (type) {
            case "audio" -> AUDIO_MIME.contains(lowerContentType);
            case "cover", "avatar", "background" -> IMAGE_MIME.contains(lowerContentType);
            case "lyric" -> LYRIC_MIME.contains(lowerContentType);
            case "voice" -> VOICE_MIME.contains(lowerContentType);
            default -> false;
        };

        if (!valid) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED,
                    "文件 MIME 类型不匹配: " + contentType);
        }
    }

    // ==================== 大小校验 ====================

    /**
     * 校验文件大小
     */
    private void validateFileSize(String type, long size) {
        long maxSize = switch (type) {
            case "audio" -> MAX_AUDIO_SIZE;
            case "cover", "avatar", "background" -> MAX_IMAGE_SIZE;
            case "lyric" -> MAX_LYRIC_SIZE;
            case "voice" -> MAX_VOICE_SIZE;
            default -> Long.MAX_VALUE;
        };

        if (size > maxSize) {
            String readableLimit;
            if (maxSize >= 1024 * 1024) {
                readableLimit = (maxSize / 1024 / 1024) + "MB";
            } else {
                readableLimit = (maxSize / 1024) + "KB";
            }
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEED,
                    "文件大小超过限制，允许最大 " + readableLimit);
        }
    }

    // ==================== 路径生成 ====================

    /**
     * 构建文件存储路径（相对于 UPLOAD_DIR）
     * <p>
     * 格式：{type}/{yyyy}/{mm}/{uuid}.{ext}
     * 语音特殊：voice/{roomId}/{uuid}.webm
     * <p>
     * 安全设计：
     * - 使用 UUID 重命名文件，完全丢弃原始文件名，
     *   避免原始文件名中的空格、引号、特殊字符对文件系统和数据库产生影响
     * - 按日期分目录（yyyy/MM）防止单目录文件过多
     * - 所有 MyBatis SQL 均使用 #{ } 预编译，杜绝 SQL 注入风险
     */
    private String buildStoragePath(String type, String originalFilename, Long roomId) {
        // 使用随机 UUID 作为文件名（去除连字符），完全避免原始文件名中的空格/引号问题
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String ext = getExtension(type);
        String datePath = DateUtil.format(new Date(), "yyyy/MM");

        if ("voice".equals(type)) {
            if (roomId == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "语音消息需要指定房间 ID");
            }
            return "voice/" + roomId + "/" + uuid + ext;
        }

        return type + "/" + datePath + "/" + uuid + ext;
    }

    /**
     * 根据文件类型获取扩展名
     */
    private String getExtension(String type) {
        return switch (type) {
            case "audio" -> EXT_AUDIO;
            case "cover" -> EXT_COVER;
            case "lyric" -> EXT_LYRIC;
            case "avatar" -> EXT_AVATAR;
            case "background" -> EXT_BACKGROUND;
            case "voice" -> EXT_VOICE;
            default -> "";
        };
    }
}
