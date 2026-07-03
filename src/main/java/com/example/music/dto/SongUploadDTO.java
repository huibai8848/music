package com.example.music.dto;

import lombok.Data;

/**
 * 统一歌曲上传 DTO
 * <p>
 * 作为 multipart 请求中的 JSON 参数 {@code songInfo}，
 * 携带歌曲元数据、艺人信息、专辑信息，与音频/封面/歌词文件一同提交。
 */
@Data
public class SongUploadDTO {

    /** 歌曲名（必填） */
    private String title;

    /** 艺人 ID（与 artistName 二选一，优先使用 ID） */
    private Long artistId;

    /** 艺人名（新建艺人时使用，与 artistId 二选一） */
    private String artistName;

    /** 艺人简介（新建艺人时可选） */
    private String artistBio;

    /** 专辑 ID（与 albumTitle 二选一，优先使用 ID） */
    private Long albumId;

    /** 专辑名（新建专辑时使用，与 albumId 二选一） */
    private String albumTitle;

    /** 风格 */
    private String genre;

    /** 语种 */
    private String language;

    /** 发行年份 */
    private Integer releaseYear;
}
