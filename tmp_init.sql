-- 管理员初始账号（密码: admin123，BCrypt加密）
INSERT INTO user (email, password, nickname, role, status)
VALUES ('admin@music.com',
        '\\\',
        '管理员',
        'ADMIN',
        'ACTIVE')
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname);

-- 默认分类数据
INSERT IGNORE INTO category (name, type, sort_order) VALUES
('流行', 'GENRE', 1),
('摇滚', 'GENRE', 2),
('古典', 'GENRE', 3),
('电子', 'GENRE', 4),
('R&B', 'GENRE', 5),
('爵士', 'GENRE', 6),
('中文', 'LANGUAGE', 1),
('英文', 'LANGUAGE', 2),
('日文', 'LANGUAGE', 3),
('韩文', 'LANGUAGE', 4),
('80年代', 'YEAR', 1),
('90年代', 'YEAR', 2),
('00年代', 'YEAR', 3),
('10年代', 'YEAR', 4),
('20年代', 'YEAR', 5);

-- 默认系统配置
INSERT IGNORE INTO system_config (config_key, config_value, description) VALUES
('site_name', '在线音乐平台', '站点名称'),
('member_upload_limit', '5', '会员每日上传次数限制'),
('max_songs_per_playlist', '500', '歌单最大歌曲数'),
('history_max_count', '100', '最近播放最大记录数'),
('hot_songs_cache_ttl', '300', '热门缓存TTL(秒)'),
('default_page_size', '20', '默认分页大小');
