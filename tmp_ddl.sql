-- ============================================================
-- 2. 艺人表
-- ============================================================
CREATE TABLE IF NOT EXISTS artist (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    name            VARCHAR(100) NOT NULL                 COMMENT '艺人名',
    avatar          VARCHAR(255) DEFAULT NULL             COMMENT '头像URL',
    bio             TEXT         DEFAULT NULL             COMMENT '艺人简介',
    country         VARCHAR(50)  DEFAULT NULL             COMMENT '国籍/地区',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_artist_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='艺人表';

-- ============================================================
-- 3. 专辑表
-- ============================================================
CREATE TABLE IF NOT EXISTS album (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    title           VARCHAR(200) NOT NULL                 COMMENT '专辑名',
    artist_id       BIGINT       NOT NULL                 COMMENT '所属艺人ID',
    cover_url       VARCHAR(255) DEFAULT NULL             COMMENT '封面图URL',
    description     TEXT         DEFAULT NULL             COMMENT '专辑简介',
    release_date    DATE         DEFAULT NULL             COMMENT '发行日期',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_album_artist_id (artist_id),
    KEY idx_album_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='专辑表';

-- ============================================================
-- 4. 歌曲表
-- ============================================================
CREATE TABLE IF NOT EXISTS song (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    title           VARCHAR(200) NOT NULL                 COMMENT '歌曲名',
    artist_id       BIGINT       NOT NULL                 COMMENT '主艺人ID',
    album_id        BIGINT       DEFAULT NULL             COMMENT '所属专辑ID',
    duration        INT          NOT NULL DEFAULT 0       COMMENT '时长(秒)',
    audio_url       VARCHAR(255) NOT NULL                 COMMENT '音频文件路径',
    cover_url       VARCHAR(255) DEFAULT NULL             COMMENT '封面图路径',
    lyric_url       VARCHAR(255) DEFAULT NULL             COMMENT 'LRC歌词文件路径',
    lyrics          TEXT         DEFAULT NULL             COMMENT '解析后的歌词JSON',
    genre           VARCHAR(50)  DEFAULT NULL             COMMENT '风格',
    language        VARCHAR(20)  DEFAULT NULL             COMMENT '语种',
    release_year    INT          DEFAULT NULL             COMMENT '发行年份',
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/PENDING/REJECTED',
    uploader_id     BIGINT       DEFAULT NULL             COMMENT '上传者ID',
    play_count      BIGINT       NOT NULL DEFAULT 0       COMMENT '播放量',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_song_title (title),
    KEY idx_song_artist_id (artist_id),
    KEY idx_song_album_id (album_id),
    KEY idx_song_genre (genre),
    KEY idx_song_status (status),
    KEY idx_song_play_count (play_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='歌曲表';

-- ============================================================
-- 5. 分类表
-- ============================================================
CREATE TABLE IF NOT EXISTS category (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    name            VARCHAR(50)  NOT NULL                 COMMENT '分类名称',
    type            VARCHAR(20)  NOT NULL                 COMMENT '分类类型: GENRE/LANGUAGE/YEAR',
    sort_order      INT          NOT NULL DEFAULT 0       COMMENT '排序序号',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_name_type (name, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类表';

-- ============================================================
-- 6. 歌曲分类关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS song_category (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    song_id         BIGINT       NOT NULL                 COMMENT '歌曲ID',
    category_id     BIGINT       NOT NULL                 COMMENT '分类ID',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_song_category (song_id, category_id),
    KEY idx_sc_category_id (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='歌曲分类关联表';

-- ============================================================
-- 7. 歌单表
-- ============================================================
CREATE TABLE IF NOT EXISTS playlist (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    user_id         BIGINT       NOT NULL                 COMMENT '创建者ID',
    title           VARCHAR(100) NOT NULL                 COMMENT '歌单标题',
    description     VARCHAR(500) DEFAULT NULL             COMMENT '歌单描述',
    cover_url       VARCHAR(255) DEFAULT NULL             COMMENT '封面图URL',
    is_public       TINYINT(1)   NOT NULL DEFAULT 1       COMMENT '是否公开 1=公开 0=隐藏',
    song_count      INT          NOT NULL DEFAULT 0       COMMENT '歌曲数量',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_playlist_user_id (user_id),
    KEY idx_playlist_is_public (is_public)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='歌单表';

-- ============================================================
-- 8. 歌单歌曲关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS playlist_song (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    playlist_id     BIGINT       NOT NULL                 COMMENT '歌单ID',
    song_id         BIGINT       NOT NULL                 COMMENT '歌曲ID',
    sort_order      INT          NOT NULL DEFAULT 0       COMMENT '排序序号',
    added_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_playlist_song (playlist_id, song_id),
    KEY idx_ps_song_id (song_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='歌单歌曲关联表';

-- ============================================================
-- 9. 评论表
-- ============================================================
CREATE TABLE IF NOT EXISTS comment (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    user_id         BIGINT       NOT NULL                 COMMENT '评论者ID',
    target_type     VARCHAR(20)  NOT NULL                 COMMENT '目标类型: SONG/PLAYLIST',
    target_id       BIGINT       NOT NULL                 COMMENT '目标ID',
    content         VARCHAR(500) NOT NULL                 COMMENT '评论内容(已HTML转义)',
    parent_id       BIGINT       DEFAULT NULL             COMMENT '回复的评论ID',
    status          VARCHAR(20)  NOT NULL DEFAULT 'VISIBLE' COMMENT '状态: VISIBLE/DELETED',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_comment_target (target_type, target_id),
    KEY idx_comment_user_id (user_id),
    KEY idx_comment_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- ============================================================
-- 10. 收藏表
-- ============================================================
CREATE TABLE IF NOT EXISTS favorite (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    user_id         BIGINT       NOT NULL                 COMMENT '用户ID',
    target_type     VARCHAR(20)  NOT NULL                 COMMENT '目标类型: SONG/PLAYLIST/ALBUM',
    target_id       BIGINT       NOT NULL                 COMMENT '目标ID',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_favorite_user_target (user_id, target_type, target_id),
    KEY idx_favorite_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- ============================================================
-- 11. 喜欢表
-- ============================================================
CREATE TABLE IF NOT EXISTS likes (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    user_id         BIGINT       NOT NULL                 COMMENT '用户ID',
    target_type     VARCHAR(20)  NOT NULL                 COMMENT '目标类型: SONG/PLAYLIST/COMMENT',
    target_id       BIGINT       NOT NULL                 COMMENT '目标ID',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_likes_user_target (user_id, target_type, target_id),
    KEY idx_likes_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='喜欢表';

-- ============================================================
-- 12. 充值记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS recharge_record (
    id              BIGINT           NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    user_id         BIGINT           NOT NULL                 COMMENT '用户ID',
    plan            VARCHAR(20)      NOT NULL                 COMMENT '套餐: MONTHLY/QUARTERLY/YEARLY',
    amount          DECIMAL(10,2)    NOT NULL                 COMMENT '支付金额',
    duration_days   INT              NOT NULL                 COMMENT '增加天数',
    expire_time     DATETIME         NOT NULL                 COMMENT '到期时间',
    status          VARCHAR(20)      NOT NULL DEFAULT 'SUCCESS' COMMENT '状态: SUCCESS/FAILED/REFUND',
    created_time    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_recharge_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='充值记录表';

-- ============================================================
-- 13. 房间消息表
-- ============================================================
CREATE TABLE IF NOT EXISTS room_message (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    room_id         BIGINT       NOT NULL                 COMMENT '房间ID',
    user_id         BIGINT       NOT NULL                 COMMENT '发送者ID',
    type            VARCHAR(20)  NOT NULL DEFAULT 'TEXT'  COMMENT '消息类型: TEXT/VOICE/SYSTEM',
    content         TEXT         NOT NULL                 COMMENT '消息内容/语音文件URL',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_room_message_room_id (room_id, created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='房间消息表';

-- ============================================================
-- 14. 通知表
-- ============================================================
CREATE TABLE IF NOT EXISTS notification (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    user_id         BIGINT       NOT NULL                 COMMENT '接收者ID',
    type            VARCHAR(30)  NOT NULL                 COMMENT '类型: REPLY/SYSTEM/MEMBERSHIP/REPORT',
    title           VARCHAR(200) NOT NULL                 COMMENT '通知标题',
    content         TEXT         DEFAULT NULL             COMMENT '通知内容',
    is_read         TINYINT(1)   NOT NULL DEFAULT 0       COMMENT '是否已读 0=未读 1=已读',
    related_type    VARCHAR(30)  DEFAULT NULL             COMMENT '关联类型',
    related_id      BIGINT       DEFAULT NULL             COMMENT '关联ID',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_notification_user_id (user_id, is_read),
    KEY idx_notification_created (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- ============================================================
-- 15. 举报表
-- ============================================================
CREATE TABLE IF NOT EXISTS report (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    reporter_id     BIGINT       NOT NULL                 COMMENT '举报人ID',
    target_type     VARCHAR(20)  NOT NULL                 COMMENT '目标类型: COMMENT/SONG/PLAYLIST',
    target_id       BIGINT       NOT NULL                 COMMENT '目标ID',
    reason          VARCHAR(30)  NOT NULL                 COMMENT '举报原因: PORNOGRAPHY/AD/ABUSE/COPYRIGHT/OTHER',
    description     VARCHAR(500) DEFAULT NULL             COMMENT '补充说明',
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/RESOLVED/DISMISSED',
    handler_id      BIGINT       DEFAULT NULL             COMMENT '处理人ID',
    handle_note     VARCHAR(500) DEFAULT NULL             COMMENT '处理备注',
    handled_time    DATETIME     DEFAULT NULL             COMMENT '处理时间',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_report_status (status),
    KEY idx_report_reporter (reporter_id),
    KEY idx_report_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='举报表';

-- ============================================================
-- 16. 轮播图表
-- ============================================================
CREATE TABLE IF NOT EXISTS banner (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    title           VARCHAR(100) DEFAULT NULL             COMMENT '标题(alt文本)',
    image_url       VARCHAR(255) NOT NULL                 COMMENT '图片URL',
    link_url        VARCHAR(255) DEFAULT NULL             COMMENT '跳转链接',
    sort_order      INT          NOT NULL DEFAULT 0       COMMENT '排序序号',
    is_active       TINYINT(1)   NOT NULL DEFAULT 1       COMMENT '是否启用 1=启用 0=禁用',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_banner_active_sort (is_active, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='轮播图表';

-- ============================================================
-- 17. 系统公告表
-- ============================================================
CREATE TABLE IF NOT EXISTS system_notice (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    title           VARCHAR(200) NOT NULL                 COMMENT '公告标题',
    content         TEXT         NOT NULL                 COMMENT '公告内容',
    type            VARCHAR(20)  NOT NULL DEFAULT 'SYSTEM' COMMENT '类型: SYSTEM/MAINTENANCE/ACTIVITY',
    is_active       TINYINT(1)   NOT NULL DEFAULT 1       COMMENT '是否生效',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_notice_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统公告表';

-- ============================================================
-- 18. 操作日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS operation_log (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    operator_id     BIGINT       NOT NULL                 COMMENT '操作人ID',
    action          VARCHAR(50)  NOT NULL                 COMMENT '操作类型',
    target_type     VARCHAR(30)  DEFAULT NULL             COMMENT '操作对象类型',
    target_id       BIGINT       DEFAULT NULL             COMMENT '操作对象ID',
    detail          TEXT         DEFAULT NULL             COMMENT '操作详情(JSON)',
    ip              VARCHAR(50)  DEFAULT NULL             COMMENT '操作人IP',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_oper_log_operator (operator_id),
    KEY idx_oper_log_action (action),
    KEY idx_oper_log_created (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ============================================================
-- 19. 播放历史表
-- ============================================================
CREATE TABLE IF NOT EXISTS play_history (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    user_id         BIGINT       NOT NULL                 COMMENT '用户ID',
    song_id         BIGINT       NOT NULL                 COMMENT '歌曲ID',
    played_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '播放时间',
    PRIMARY KEY (id),
    KEY idx_ph_user_time (user_id, played_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='播放历史表';

-- ============================================================
-- 20. 系统配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS system_config (
    id              BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    config_key      VARCHAR(100) NOT NULL                 COMMENT '配置键',
    config_value    TEXT         NOT NULL                 COMMENT '配置值',
    description     VARCHAR(255) DEFAULT NULL             COMMENT '配置说明',
    created_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';
