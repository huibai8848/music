package com.example.music.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建歌房请求 DTO
 */
@Data
public class CreateRoomDTO {

    /** 房间名称 */
    @NotBlank(message = "房间名称不能为空")
    @Size(max = 50, message = "房间名称最多 50 个字符")
    private String name;

    /** 是否公开（默认公开） */
    private Boolean isPublic = true;

    /** 房间密码（私密房间时需要） */
    @Size(max = 20, message = "密码最多 20 个字符")
    private String password;

    /** 最大成员数（默认 8） */
    private Integer maxMembers = 8;
}
