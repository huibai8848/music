package com.example.music.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付宝沙箱支付配置
 *
 * 使用方式：
 * 1. 登录支付宝开放平台 https://open.alipay.com/ → 控制台 → 沙箱环境
 * 2. 获取 APP_ID（沙箱应用）、应用私钥（RSA2）、支付宝公钥
 * 3. 填写到 application.yml 的 alipay.* 配置项
 * 4. 沙箱买家账号在沙箱环境页面可查看，用于测试支付
 */
@Data
@Component
@ConfigurationProperties(prefix = "alipay")
public class AlipayProperties {

    /**
     * 支付宝沙箱网关
     *
     * 注意：2025年起支付宝沙箱环境已升级，旧网关已废弃（返回502）。
     * - ❌ 旧版（已废弃）：https://openapi.alipaydev.com/gateway.do
     * - ✅ 新版（推荐）：https://openapi-sandbox.dl.alipaydev.com/gateway.do
     *
     * 使用新版网关前，需在支付宝开放平台沙箱控制台点击「升级沙箱环境」，
     * 获取新的 APP_ID 和应用私钥/支付宝公钥。
     */
    private String gatewayUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";

    /** 沙箱应用 APP_ID（必填，从支付宝开放平台沙箱环境获取） */
    private String appId;

    /** 商户应用私钥（PKCS8 格式，从支付宝开放平台生成或自行生成） */
    private String merchantPrivateKey;

    /** 支付宝公钥（用于验证通知签名，从支付宝开放平台获取） */
    private String alipayPublicKey;

    /** 异步通知地址（需公网可访问，使用 ngrok 穿透后的地址） */
    private String notifyUrl;

    /** 同步跳转地址（支付完成后支付宝重定向到后端，再由后端重定向到前端） */
    private String returnUrl;

    /** 前端页面地址（支付完成后后端重定向到此地址，默认 Vite 开发服务器） */
    private String frontendUrl = "http://localhost:5173";

    /** 签名类型（固定 RSA2） */
    private String signType = "RSA2";

    /** 字符集 */
    private String charset = "utf-8";

    /** 参数格式 */
    private String format = "json";
}
