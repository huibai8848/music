package com.example.music.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 管理后台背景图管理服务接口
 * <p>
 * 提供背景图的列表查询、上传和删除功能。
 * 背景图文件存储在 data/music/background/{yyyy/mm}/{uuid}.jpg 目录下。
 */
public interface AdminBackgroundService {

    /**
     * 获取背景图列表（按上传时间降序）
     *
     * @return 背景图信息列表，每项包含 filename/url/size/lastModified
     */
    List<Map<String, Object>> listBackgrounds();

    /**
     * 上传背景图
     *
     * @param file   上传的图片文件（JPG/PNG，≤5MB）
     * @param adminId 操作管理员 ID
     * @return 上传结果（url + filename）
     */
    Map<String, Object> uploadBackground(MultipartFile file, Long adminId);

    /**
     * 删除背景图
     *
     * @param filename 文件名（如 uuid.jpg）
     */
    void deleteBackground(String filename);
}