package com.example.music.dto;

import lombok.Data;

/**
 * 加入歌房请求 DTO
 * <p>
 * 私密房间需要提供密码验证。
 */
@Data
public class JoinRoomDTO {

    /** 房间密码（私密房间时需要） */
    private String password;
}
