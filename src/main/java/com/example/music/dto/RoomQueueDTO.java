package com.example.music.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 歌房播放队列操作 DTO
 * <p>
 * 用于向歌房队列添加或移除歌曲。
 */
@Data
public class RoomQueueDTO {

    /** 歌曲 ID */
    @NotNull(message = "歌曲 ID 不能为空")
    private Long songId;
}
