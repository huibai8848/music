-- =============================================================
-- 支付宝支付功能 - 数据库迁移
-- 为 recharge_record 表添加支付宝交易号字段
-- =============================================================

ALTER TABLE `recharge_record`
    ADD COLUMN `out_trade_no` VARCHAR(64) DEFAULT NULL COMMENT '商户订单号（用于支付宝交易追踪）' AFTER `status`,
    ADD COLUMN `trade_no` VARCHAR(64) DEFAULT NULL COMMENT '支付宝交易号' AFTER `out_trade_no`,
    ADD INDEX `idx_out_trade_no` (`out_trade_no`);
