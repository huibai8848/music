package com.example.music.constant;

/**
 * 业务错误码枚举
 * <p>
 * 编码规则：
 * - 1xxx：用户模块错误
 * - 2xxx：音乐模块错误
 * - 3xxx：歌房模块错误
 * - 4xxx：文件模块错误
 * - 5xxx：会员模块错误
 * - 6xxx：通知模块错误
 * - 7xxx：举报模块错误
 * - 8xxx：管理员模块错误
 * - 9xxx：系统通用错误
 */
public enum ErrorCode {

    // ==================== 通用（9xxx） ====================
    /** 请求成功 */
    SUCCESS(200, "操作成功"),
    /** 请求参数错误 */
    BAD_REQUEST(400, "请求参数错误"),
    /** 未登录或 Token 已过期 */
    UNAUTHORIZED(401, "请先登录"),
    /** 无权限访问 */
    FORBIDDEN(403, "权限不足"),
    /** 资源不存在 */
    NOT_FOUND(404, "资源不存在"),
    /** 请求方法不支持 */
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    /** 数据冲突（如重复注册） */
    CONFLICT(409, "数据冲突"),
    /** 参数校验失败 */
    PARAM_INVALID(422, "参数校验失败"),
    /** 请求频率过高 */
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),
    /** 服务器内部异常 */
    INTERNAL_ERROR(500, "服务器内部错误"),
    /** 服务不可用 */
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),

    // ==================== 用户模块（1xxx） ====================
    /** 用户已存在 */
    USER_ALREADY_EXISTS(1001, "该邮箱已被注册"),
    /** 用户名或密码错误 */
    USER_LOGIN_FAILED(1002, "邮箱或密码错误"),
    /** 旧密码不正确 */
    USER_OLD_PASSWORD_WRONG(1003, "旧密码不正确"),
    /** 账号已被封禁 */
    USER_BANNED(1004, "账号已被封禁，请联系管理员"),
    /** 验证码错误 */
    USER_CODE_WRONG(1005, "验证码错误"),
    /** 用户不存在 */
    USER_NOT_FOUND(1006, "用户不存在"),
    /** 邮箱未注册 */
    USER_EMAIL_NOT_FOUND(1009, "该邮箱未注册"),
    /** 验证码错误或已过期 */
    USER_VERIFICATION_CODE_WRONG(1010, "验证码错误或已过期"),
    /** Token 无效 */
    TOKEN_INVALID(1007, "Token 无效或已过期"),
    /** Token 已失效（被登出） */
    TOKEN_BLACKLISTED(1008, "Token 已失效，请重新登录"),

    // ==================== 音乐模块（2xxx） ====================
    /** 歌曲不存在 */
    SONG_NOT_FOUND(2001, "歌曲不存在"),
    /** 歌单不存在 */
    PLAYLIST_NOT_FOUND(2002, "歌单不存在"),
    /** 专辑不存在 */
    ALBUM_NOT_FOUND(2003, "专辑不存在"),
    /** 艺人不存在 */
    ARTIST_NOT_FOUND(2004, "艺人不存在"),
    /** 歌曲已存在 */
    SONG_ALREADY_EXISTS(2005, "歌曲已存在"),
    /** 歌单已满 */
    PLAYLIST_FULL(2006, "歌单已满"),

    // ==================== 歌房模块（3xxx） ====================
    /** 歌房不存在 */
    ROOM_NOT_FOUND(3001, "歌房不存在"),
    /** 歌房已满 */
    ROOM_FULL(3002, "歌房已满员"),
    /** 不是房主 */
    ROOM_NOT_OWNER(3003, "您不是房主，无此权限"),
    /** 房间密码错误 */
    ROOM_PASSWORD_WRONG(3004, "房间密码错误"),
    /** 已在房间中 */
    ROOM_ALREADY_JOINED(3005, "您已在房间中"),
    /** 无权限创建歌房 */
    ROOM_PERMISSION_DENIED(3006, "仅 VIP 会员和管理员可创建歌房"),

    // ==================== 文件模块（4xxx） ====================
    /** 文件类型不支持 */
    FILE_TYPE_NOT_ALLOWED(4001, "文件类型不支持"),
    /** 文件大小超限 */
    FILE_SIZE_EXCEED(4002, "文件大小超过限制"),
    /** 文件上传失败 */
    FILE_UPLOAD_FAILED(4003, "文件上传失败"),

    // ==================== 会员模块（5xxx） ====================
    /** 会员已过期 */
    MEMBERSHIP_EXPIRED(5001, "会员已过期，请续费"),
    /** 非会员限制 */
    MEMBERSHIP_REQUIRED(5002, "该功能仅限会员使用"),
    /** 上传次数超限 */
    UPLOAD_LIMIT_EXCEEDED(5003, "今日上传次数已达上限"),
    /** 会员存储空间不足 */
    STORAGE_LIMIT_EXCEEDED(5004, "会员存储空间不足，最多可存储 500MB"),

    // ==================== 通知模块（6xxx） ====================
    /** 通知不存在 */
    NOTIFICATION_NOT_FOUND(6001, "通知不存在"),
    /** 系统公告不存在 */
    NOTICE_NOT_FOUND(6002, "系统公告不存在"),

    // ==================== 举报模块（7xxx） ====================
    /** 重复举报（24h内不可重复举报同一内容） */
    REPORT_DUPLICATE(7001, "您已举报过该内容"),
    /** 举报提交过于频繁 */
    REPORT_TOO_FREQUENT(7002, "举报提交过于频繁，请稍后再试"),
    /** 举报原因无效 */
    REPORT_REASON_INVALID(7003, "举报原因无效"),

    // ==================== 管理员模块（8xxx） ====================
    /** 不能封禁自己 */
    ADMIN_CANNOT_BAN_SELF(8001, "不能封禁自己"),
    /** 目标用户不存在 */
    ADMIN_USER_NOT_FOUND(8002, "目标用户不存在"),
    /** 举报记录不存在 */
    ADMIN_REPORT_NOT_FOUND(8003, "举报记录不存在"),
    /** 评论不存在 */
    ADMIN_COMMENT_NOT_FOUND(8004, "评论不存在"),
    /** 操作日志不存在 */
    ADMIN_LOG_NOT_FOUND(8005, "操作日志不存在"),
    /** 举报已被处理 */
    ADMIN_REPORT_ALREADY_HANDLED(8006, "该举报已被处理，请刷新后重试"),
    /** 歌曲已被审核 */
    ADMIN_SONG_ALREADY_AUDITED(8007, "该歌曲已被审核，请刷新后重试");

    /** HTTP 状态码（辅助前端/网关判断） */
    private final int httpStatus;

    /** 业务码（返回给前端） */
    private final int code;

    /** 默认错误消息 */
    private final String message;

    ErrorCode(int code, String message) {
        // 默认 4xx 以下用 200 状态码，4xx 及以上用业务码的百位作为 http 状态
        this.httpStatus = code >= 4000 ? 500 : (code >= 1000 ? 200 : code);
        this.code = code;
        this.message = message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
