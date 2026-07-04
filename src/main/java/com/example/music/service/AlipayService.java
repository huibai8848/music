package com.example.music.service;

import java.util.Map;

/**
 * 支付宝支付服务
 */
public interface AlipayService {

    /**
     * 创建支付宝支付订单，返回自动提交的表单数据
     *
     * @param userId  当前用户 ID
     * @param plan    套餐类型：MONTHLY / QUARTERLY / YEARLY
     * @return { actionUrl, formFields } 用于前端构造表单提交
     */
    Map<String, Object> createPayment(Long userId, String plan) throws Exception;

    /**
     * 处理支付宝异步通知
     *
     * @param params 支付宝 POST 过来的参数
     * @return "success" 或 "fail"
     */
    String handleNotify(Map<String, String> params) throws Exception;
}
