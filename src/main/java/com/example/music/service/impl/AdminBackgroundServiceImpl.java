package com.example.music.service.impl;

import com.example.music.constant.ErrorCode;
import com.example.music.exception.BusinessException;
import com.example.music.service.AdminBackgroundService;
import com.example.music.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 后台背景图管理服务实现
 * <p>
 * 核心逻辑：
 * 1. listBackgrounds — 递归扫描 data/music/background/ 目录，列出所有图片文件
 * 2. uploadBackground — 委托 FileService.upload 处理文件存储，返回访问 URL
 * 3. deleteBackground — 按文件名删除文件
 * <p>
 * 文件存储结构：
 * <pre>
 * data/music/background/{yyyy/mm}/{uuid}.jpg
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBackgroundServiceImpl implements AdminBackgroundService {

    private static final String DATA_DIR = "E:/07_Data/IdeaProjects/music/data/music";
    private static final String BACKGROUND_DIR = DATA_DIR + "/background";

    private final FileService fileService;

    @Override
    public List<Map<String, Object>> listBackgrounds() {
        File dir = new File(BACKGROUND_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            return Collections.emptyList();
        }

        List<File> bgFiles = listBgFiles(dir);

        List<Map<String, Object>> result = bgFiles.stream().map(f -> {
            Map<String, Object> item = new HashMap<>();
            // 构造相对于 DATA_DIR 的路径作为 URL
            String relativePath = f.getAbsolutePath()
                    .replace(DATA_DIR, "")
                    .replace("\\", "/");
            item.put("filename", f.getName());
            item.put("url", "/api/files" + relativePath);
            item.put("size", f.length());
            item.put("lastModified", f.lastModified());
            return item;
        }).collect(Collectors.toList());

        // 按修改时间降序排列
        result.sort((a, b) -> Long.compare(
                (Long) b.get("lastModified"),
                (Long) a.get("lastModified")
        ));

        return result;
    }

    @Override
    public Map<String, Object> uploadBackground(MultipartFile file, Long adminId) {
        String url = fileService.upload(file, "background", null);

        Map<String, Object> result = new HashMap<>();
        result.put("url", url);
        result.put("filename", file.getOriginalFilename());
        return result;
    }

    @Override
    public void deleteBackground(String filename) {
        File dir = new File(BACKGROUND_DIR);
        if (!dir.exists()) return;

        // 在所有子目录中查找匹配的文件
        List<File> matches = listBgFiles(dir).stream()
                .filter(f -> f.getName().equals(filename))
                .collect(Collectors.toList());

        if (matches.isEmpty()) {
            log.warn("删除背景图失败，文件不存在: {}", filename);
            return;
        }

        for (File file : matches) {
            if (file.delete()) {
                log.info("删除背景图: {}", file.getAbsolutePath());
            } else {
                throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "删除背景图失败: " + filename);
            }
        }
    }

    /**
     * 递归列出目录下所有图片文件
     */
    private List<File> listBgFiles(File dir) {
        List<File> result = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files == null) return result;
        for (File f : files) {
            if (f.isDirectory()) {
                result.addAll(listBgFiles(f));
            } else {
                String name = f.getName().toLowerCase();
                if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")) {
                    result.add(f);
                }
            }
        }
        return result;
    }
}