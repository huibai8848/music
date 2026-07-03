package com.example.music.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务接口
 * <p>
 * 提供统一文件上传功能，支持音频/封面/歌词/头像/背景图/语音消息。
 * 文件存储于本地文件系统 {@code data/music/{type}/{yyyy/mm}/{uuid}.{ext}}，
 * 通过静态资源映射 {@code /api/files/**} 提供访问。
 */
public interface FileService {

    /**
     * 上传文件
     *
     * @param file   上传的文件（MultipartFile）
     * @param type   文件类型（audio/cover/lyric/avatar/background/voice）
     * @param roomId 房间 ID（语音消息时必填，其他类型可 null）
     * @return 文件访问相对 URL（如 /api/files/audio/2026/07/xxx.mp3）
     * @throws IllegalArgumentException 文件类型不支持或参数错误
     * @throws RuntimeException         文件写入失败
     */
    String upload(MultipartFile file, String type, Long roomId);
}
