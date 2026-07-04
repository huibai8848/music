package com.example.music.service.impl;

import cn.hutool.core.codec.Base64;
import com.example.music.config.AlipayProperties;
import com.example.music.constant.ErrorCode;
import com.example.music.entity.RechargeRecord;
import com.example.music.entity.User;
import com.example.music.exception.BusinessException;
import com.example.music.mapper.RechargeRecordMapper;
import com.example.music.mapper.UserMapper;
import com.example.music.service.AlipayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 支付宝支付服务实现（基于 Hutool + 原生 RSA 签名，不依赖官方 SDK）
 *
 * 与支付宝沙箱环境交互，完成订单创建和通知验证。
 * 使用 RSA2（SHA256withRSA）签名算法。
 *
 * 密钥格式要求：PKCS8 PEM 格式（以 -----BEGIN PRIVATE KEY----- 开头）
 * 通过环境变量 ALIPAY_MERCHANT_PRIVATE_KEY 设置。
 *
 * PowerShell 设置方式（使用 here-string 保留换行）：
 *   $env:ALIPAY_MERCHANT_PRIVATE_KEY=@"
 *   -----BEGIN PRIVATE KEY-----
 *   MIIEvgIBADANBg...
 *   -----END PRIVATE KEY-----
 *   "@
 */

/**
 * 支付宝支付服务实现（基于 Hutool + 原生 RSA 签名，不依赖官方 SDK）
 *
 * 与支付宝沙箱环境交互，完成订单创建和通知验证。
 * 使用 RSA2（SHA256withRSA）签名算法。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayServiceImpl implements AlipayService {

    private final AlipayProperties alipayProperties;
    private final RechargeRecordMapper rechargeRecordMapper;
    private final UserMapper userMapper;

    /** 日期时间格式 */
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createPayment(Long userId, String plan) throws Exception {
        // ===== 0. 校验支付宝配置是否有效 =====
        if (alipayProperties.getAppId() == null || alipayProperties.getAppId().isEmpty()
                || alipayProperties.getAppId().contains("${")) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "支付宝 APP_ID 未配置，请检查 application.yml 或环境变量");
        }
        if (alipayProperties.getMerchantPrivateKey() == null || alipayProperties.getMerchantPrivateKey().isEmpty()
                || alipayProperties.getMerchantPrivateKey().contains("${")) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "支付宝商户私钥未配置，请从沙箱环境获取并设置");
        }
        if (alipayProperties.getAlipayPublicKey() == null || alipayProperties.getAlipayPublicKey().isEmpty()
                || alipayProperties.getAlipayPublicKey().contains("${")) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "支付宝公钥未配置，请从沙箱环境获取并设置");
        }
        if (alipayProperties.getNotifyUrl() == null || alipayProperties.getNotifyUrl().isEmpty()
                || alipayProperties.getNotifyUrl().contains("${")) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "支付宝异步通知地址(notify-url)未配置");
        }
        if (alipayProperties.getReturnUrl() == null || alipayProperties.getReturnUrl().isEmpty()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "支付宝同步回跳地址(return-url)未配置");
        }

        // 检查 notify-url 是否包含 ngrok URL（free 域名随时变化）
        String notifyUrl = alipayProperties.getNotifyUrl();
        if (notifyUrl.contains("ngrok") && !notifyUrl.contains("localhost")) {
            log.warn("⚠️ notify-url({}) 使用了 ngrok 地址，请确保 ngrok 正在运行且地址未过期", notifyUrl);
        }

        // ===== 1. 解析套餐 =====
        String planUpper = plan.toUpperCase();
        int days;
        BigDecimal amount;
        String subject;

        switch (planUpper) {
            case "MONTHLY":
                days = 30;
                amount = new BigDecimal("15.00");
                subject = "音乐平台月度VIP会员";
                break;
            case "QUARTERLY":
                days = 90;
                amount = new BigDecimal("40.00");
                subject = "音乐平台季度VIP会员";
                break;
            case "YEARLY":
                days = 365;
                amount = new BigDecimal("120.00");
                subject = "音乐平台年度VIP会员";
                break;
            default:
                throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的套餐类型");
        }

        // ===== 2. 计算会员过期时间 =====
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime;
        if (user.getVipExpireTime() != null && user.getVipExpireTime().isAfter(now)) {
            expireTime = user.getVipExpireTime().plusDays(days);
        } else {
            expireTime = now.plusDays(days);
        }

        // ===== 3. 创建充值记录（状态：PENDING） =====
        String outTradeNo = "MUSIC_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();

        RechargeRecord record = new RechargeRecord();
        record.setUserId(userId);
        record.setPlan(planUpper);
        record.setAmount(amount);
        record.setDurationDays(days);
        record.setExpireTime(expireTime);
        record.setStatus("PENDING");
        record.setOutTradeNo(outTradeNo);
        rechargeRecordMapper.insert(record);

        log.info("创建支付订单: outTradeNo={}, userId={}, plan={}, amount={}",
                outTradeNo, userId, planUpper, amount);

        // ===== 4. 构建支付宝请求参数 =====
        String bizContent = String.format(
                "{\"out_trade_no\":\"%s\",\"total_amount\":\"%s\",\"subject\":\"%s\",\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}",
                outTradeNo, amount.toPlainString(), subject
        );

        // 4a. 构造所有参数（按 key 排序）
        // sign_type 和 sign 不参与签名原文，但作为表单字段提交给支付宝
        Map<String, String> signParams = new TreeMap<>();
        signParams.put("app_id", alipayProperties.getAppId());
        signParams.put("biz_content", bizContent);
        signParams.put("charset", alipayProperties.getCharset());
        signParams.put("format", alipayProperties.getFormat());
        signParams.put("method", "alipay.trade.page.pay");
        signParams.put("notify_url", alipayProperties.getNotifyUrl());
        signParams.put("return_url", alipayProperties.getReturnUrl());
        signParams.put("timestamp", LocalDateTime.now().format(DTF));
        signParams.put("version", "1.0");
        // sign_type 须加入签名原文 —— 新版沙箱网关验签字符串包含 sign_type=RSA2
        signParams.put("sign_type", alipayProperties.getSignType());

        // 4b. 生成签名
        String signContent = buildSignContent(signParams);
        String sign = rsa2Sign(signContent, alipayProperties.getMerchantPrivateKey());

        // 4c. 构建完整参数（含 sign，用于表单提交）
        TreeMap<String, String> allParams = new TreeMap<>(signParams);
        allParams.put("sign", sign);

        log.debug("支付宝签名原文: {}", signContent);
        log.debug("支付宝签名结果: {}", sign);

        // ===== 5. 返回表单数据（前端用动态表单 POST 提交） =====
        log.info("支付宝支付表单已生成: outTradeNo={}", outTradeNo);

        Map<String, Object> result = new HashMap<>();
        result.put("tradeNo", outTradeNo);
        // charset 必须在网关 URL 查询字符串中，以便支付宝正确解码 POST body 参数
        String actionUrl = alipayProperties.getGatewayUrl() + "?charset=" + alipayProperties.getCharset();
        result.put("actionUrl", actionUrl);
        result.put("formFields", allParams);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleNotify(Map<String, String> params) throws Exception {
        log.info("收到支付宝异步通知: outTradeNo={}, tradeStatus={}",
                params.get("out_trade_no"), params.get("trade_status"));

        // 1. 签名验证
        String sign = params.remove("sign");
        String signType = params.remove("sign_type");
        if (sign == null) {
            log.error("支付宝通知缺少签名参数");
            return "fail";
        }

        // 使用 TreeMap 确保 key 按字典序排列
        TreeMap<String, String> sortedParams = new TreeMap<>(params);

        String signContent = buildSignContent(sortedParams);
        boolean signVerified = rsa2Verify(signContent, sign, alipayProperties.getAlipayPublicKey());

        if (!signVerified) {
            log.error("支付宝通知签名验证失败");
            return "fail";
        }

        log.info("支付宝通知签名验证成功");

        // 2. 验证业务状态
        String tradeStatus = params.get("trade_status");
        String outTradeNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");       // 支付宝交易号
        String totalAmount = params.get("total_amount");

        // 只处理交易完成状态
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            log.info("忽略非完成状态通知: tradeStatus={}", tradeStatus);
            return "success";
        }

        // 3. 查询本地订单
        RechargeRecord record = rechargeRecordMapper.selectByOutTradeNo(outTradeNo);
        if (record == null) {
            log.error("订单不存在: outTradeNo={}", outTradeNo);
            return "fail";
        }

        // 4. 防止重复处理
        if ("SUCCESS".equals(record.getStatus())) {
            log.info("订单已处理，忽略重复通知: outTradeNo={}", outTradeNo);
            return "success";
        }

        // 5. 验证金额是否一致
        BigDecimal paidAmount = new BigDecimal(totalAmount);
        if (paidAmount.compareTo(record.getAmount()) != 0) {
            log.error("金额不匹配: 预期={}, 实际={}", record.getAmount(), paidAmount);
            return "fail";
        }

        // 6. 更新订单状态
        RechargeRecord updateRecord = new RechargeRecord();
        updateRecord.setOutTradeNo(outTradeNo);
        updateRecord.setTradeNo(tradeNo);
        updateRecord.setStatus("SUCCESS");
        rechargeRecordMapper.updateByOutTradeNo(updateRecord);

        // 7. 更新用户 VIP 状态
        User user = userMapper.selectById(record.getUserId());
        if (user == null) {
            log.error("用户不存在: userId={}", record.getUserId());
            return "fail";
        }
        userMapper.updateVip(record.getUserId(), "VIP", record.getExpireTime());

        log.info("会员充值成功: userId={}, outTradeNo={}, tradeNo={}, expireTime={}",
                record.getUserId(), outTradeNo, tradeNo, record.getExpireTime());

        return "success";
    }

    // ==================== 私有方法 ====================

    /**
     * 构建签名原文：将参数按 key=value 拼接，用 & 连接
     */
    private String buildSignContent(Map<String, String> sortedParams) {
        StringBuilder content = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key != null && value != null && !value.isEmpty()) {
                content.append(key).append("=").append(value).append("&");
            }
        }
        // 去掉最后一个 &
        if (content.length() > 0) {
            content.setLength(content.length() - 1);
        }
        return content.toString();
    }

    /**
     * 清理 PEM 格式密钥的辅助方法
     * <p>
     * 移除 PEM 头尾标记（BEGIN/END 行），然后移除所有空白字符（包括实际换行符和字面 \n 转义）。
     * <p>
     * 兼容以下输入来源：
     * - 环境变量读取的 PEM 文件（含实际换行符 `\n` / `\r\n`）
     * - YAML 内联字符串（含字面 `\n`）
     * - 手动输入的一行式 Base64
     */
    private String cleanPemKey(String pem) {
        return pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\r\n", "\n")   // 标准化 Windows 换行
                .replace("\n", "")       // 移除实际换行符
                .replace("\\n", "")      // 移除 YAML 字面 \n
                .replace("\\r", "")      // 移除 YAML 字面 \r
                .replace(" ", "");       // 移除剩余空格
    }

    /**
     * RSA2 签名（SHA256withRSA）
     *
     * @param content  待签名字符串
     * @param privateKeyPem PKCS8 格式的私钥（带 PEM 头尾标记）
     * @return Base64 编码的签名
     */
    private String rsa2Sign(String content, String privateKeyPem) throws Exception {
        byte[] keyBytes = Base64.decode(cleanPemKey(privateKeyPem));
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(content.getBytes(StandardCharsets.UTF_8));
        byte[] signed = signature.sign();

        return Base64.encode(signed);
    }

    /**
     * RSA2 签名验证（SHA256withRSA）
     *
     * @param content      待验证的字符串
     * @param sign         Base64 编码的签名
     * @param alipayPublicKeyPem 支付宝公钥（带 PEM 头尾标记）
     * @return true 如果签名验证通过
     */
    private boolean rsa2Verify(String content, String sign, String alipayPublicKeyPem) throws Exception {
        byte[] keyBytes = Base64.decode(cleanPemKey(alipayPublicKeyPem));
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        java.security.PublicKey publicKey = keyFactory.generatePublic(keySpec);

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(content.getBytes(StandardCharsets.UTF_8));
        byte[] signBytes = Base64.decode(sign);

        return signature.verify(signBytes);
    }
}
