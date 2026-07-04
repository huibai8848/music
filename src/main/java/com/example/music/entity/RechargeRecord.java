package com.example.music.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值记录
 */
@Data
public class RechargeRecord {
    private Long id;
    private Long userId;
    private String plan;
    private BigDecimal amount;
    private Integer durationDays;
    private LocalDateTime expireTime;
    private String status;
    /** 商户订单号（用于支付宝交易追踪） */
    private String outTradeNo;
    /** 支付宝交易号 */
    private String tradeNo;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
