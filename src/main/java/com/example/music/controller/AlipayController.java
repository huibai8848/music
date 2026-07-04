package com.example.music.controller;

import com.example.music.config.AlipayProperties;
import com.example.music.service.AlipayService;
import com.example.music.utils.JwtUtil;
import com.example.music.utils.RequestContext;
import com.example.music.vo.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝支付控制器
 *
 * 处理会员升级的支付宝支付流程。
 *
 * 接口说明：
 * - GET  /api/payment/alipay/pay     — 跳转到自动提交的支付页面（无需 Cookie/Token，通过 URL 参数鉴权）
 * - POST /api/payment/alipay/create  — 创建订单，返回表单数据（API 调用用）
 * - POST /api/payment/alipay/notify  — 异步通知（支付宝 → 后端，无需认证）
 * - GET  /api/payment/alipay/return   — 同步跳转（支付宝 → 前端，无需认证）
 */
@Slf4j
@RestController
@RequestMapping("/api/payment/alipay")
@RequiredArgsConstructor
public class AlipayController {

    private final AlipayService alipayService;
    private final JwtUtil jwtUtil;
    private final AlipayProperties alipayProperties;

    /**
     * 创建支付宝支付订单
     *
     * @param plan 套餐类型：MONTHLY / QUARTERLY / YEARLY
     * @return { tradeNo, actionUrl, formFields } 前端据此构建表单提交到支付宝
     */
    @PostMapping("/create")
    public R<Map<String, Object>> createPayment(@RequestParam String plan) {
        Long userId = RequestContext.getUserId();
        if (userId == null) {
            return R.fail("请先登录");
        }
        try {
            Map<String, Object> result = alipayService.createPayment(userId, plan);
            return R.ok(result);
        } catch (Exception e) {
            log.error("创建支付订单失败: userId={}, plan={}", userId, plan, e);
            return R.fail(e.getMessage());
        }
    }

    /**
     * 跳转到支付宝支付页面（白名单 GET，通过 token 参数鉴权）
     *
     * 使用方式：前端先从 sessionStorage 中获取 access_token，
     * 然后打开此地址: /api/payment/alipay/pay-page?plan=MONTHLY&token=xxx
     * 后端会返回一个自动提交到支付宝的 HTML 页面。
     */
    @GetMapping("/pay-page")
    public void payPage(@RequestParam String plan,
                         @RequestParam String token,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        try {
            // 1. 验证 token 并提取 userId
            if (!jwtUtil.validateToken(token)) {
                response.setContentType("text/html;charset=utf-8");
                response.getWriter().write("<h3>登录已过期，请刷新页面重新登录</h3>");
                return;
            }
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId == null) {
                response.setContentType("text/html;charset=utf-8");
                response.getWriter().write("<h3>登录已过期，请刷新页面重新登录</h3>");
                return;
            }

            // 2. 创建支付订单
            Map<String, Object> payment = alipayService.createPayment(userId, plan);
            String actionUrl = (String) payment.get("actionUrl");
            @SuppressWarnings("unchecked")
            Map<String, String> formFields = (Map<String, String>) payment.get("formFields");

            // 3. 渲染自动提交的 HTML 表单（含手动点击备用）
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">");
            html.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">");
            html.append("<title>正在跳转到支付宝...</title>");
            html.append("<style>");
            html.append("body{font-family:'Microsoft YaHei',sans-serif;background:#f5f5f5;display:flex;justify-content:center;align-items:center;min-height:100vh;margin:0;padding:20px;}");
            html.append(".pay-card{background:#fff;border-radius:12px;padding:40px;max-width:500px;width:100%;text-align:center;box-shadow:0 2px 16px rgba(0,0,0,0.08);}");
            html.append("h3{color:#333;font-size:18px;margin:0 0 8px;}");
            html.append("p{color:#999;font-size:14px;margin:0 0 24px;}");
            html.append(".btn-pay{display:inline-block;background:#1565C0;color:#fff;border:none;padding:14px 40px;border-radius:8px;font-size:16px;cursor:pointer;text-decoration:none;transition:background 0.2s;}");
            html.append(".btn-pay:hover{background:#0D47A1;}");
            html.append(".loading{display:inline-block;width:24px;height:24px;border:3px solid #e0e0e0;border-top-color:#1565C0;border-radius:50%;animation:spin 0.8s linear infinite;margin-bottom:16px;}");
            html.append("@keyframes spin{to{transform:rotate(360deg);}}");
            html.append("</style></head><body>");
            html.append("<div class=\"pay-card\">");
            html.append("<div class=\"loading\"></div>");
            html.append("<h3>正在跳转到支付宝...</h3>");
            html.append("<p>如果页面没有自动跳转，请点击下方按钮</p>");
            html.append("<form id=\"alipayForm\" method=\"POST\" accept-charset=\"utf-8\" action=\"")
                .append(htmlEncode(actionUrl)).append("\">");
            for (Map.Entry<String, String> entry : formFields.entrySet()) {
                html.append("<input type=\"hidden\" name=\"")
                    .append(htmlEncode(entry.getKey())).append("\" value=\"")
                    .append(htmlEncode(entry.getValue())).append("\"/>");
            }
            html.append("<button type=\"submit\" class=\"btn-pay\" id=\"payBtn\">前往支付宝支付</button>");
            html.append("</form>");
            html.append("</div>");
            html.append("<script>");
            html.append("(function(){");
            html.append("var f=document.getElementById('alipayForm');");
            html.append("var btn=document.getElementById('payBtn');");
            html.append("if(f){");
            html.append("setTimeout(function(){");
            html.append("try{f.submit();}catch(e){btn.textContent='点击跳转到支付宝';}");
            html.append("},1500);");
            html.append("btn.addEventListener('click',function(){btn.disabled=true;btn.textContent='跳转中...';});");
            html.append("}");
            html.append("})();");
            html.append("</script>");
            html.append("</body></html>");

            response.setContentType("text/html;charset=utf-8");
            response.getWriter().write(html.toString());
        } catch (Exception e) {
            log.error("生成支付页面失败", e);
            try {
                response.setContentType("text/html;charset=utf-8");
                response.getWriter().write("<h3>生成支付页面失败: " + e.getMessage() + "</h3>");
            } catch (Exception ignored) {}
        }
    }

    /**
     * HTML 转义（防止特殊字符破坏页面结构）
     */
    private String htmlEncode(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
    }

    /**
     * 支付宝异步通知
     *
     * 支付宝在用户支付完成后，会以 POST 方式通知此地址。
     * 此接口无需用户认证，支付宝使用签名验证身份。
     */
    @PostMapping("/notify")
    public String handleNotify(HttpServletRequest request) {
        // 收集支付宝参数
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            params.put(entry.getKey(), entry.getValue()[0]);
        }

        log.info("接收支付宝异步通知: outTradeNo={}, tradeStatus={}",
                params.get("out_trade_no"), params.get("trade_status"));

        try {
            return alipayService.handleNotify(params);
        } catch (Exception e) {
            log.error("处理支付宝通知失败", e);
            return "fail";
        }
    }

    /**
     * 支付宝同步跳转
     *
     * 用户支付完成后，浏览器被重定向到此地址。
     * 此地址直接跳转回前端页面，由前端处理成功状态展示。
     *
     * 注意：同步跳转仅作展示用途，真正的订单状态更新以异步通知为准。
     * 支付宝会携带 out_trade_no, trade_no, total_amount, sign 等参数返回，
     * 此处对参数进行 URL 编码后传递给前端。
     */
    @GetMapping("/return")
    public void handleReturn(HttpServletRequest request, HttpServletResponse response) {
        String outTradeNo = request.getParameter("out_trade_no");
        String tradeNo = request.getParameter("trade_no");
        String totalAmount = request.getParameter("total_amount");

        log.info("用户从支付宝返回: outTradeNo={}, tradeNo={}, totalAmount={}",
                outTradeNo, tradeNo, totalAmount);

        // 重定向到前端 profile 页面，携带支付成功标记
        try {
            String baseUrl = alipayProperties.getFrontendUrl() + "/profile";
            String redirectUrl = baseUrl + "?payment=success"
                    + "&out_trade_no=" + URLEncoder.encode(outTradeNo != null ? outTradeNo : "", StandardCharsets.UTF_8)
                    + "&trade_no=" + URLEncoder.encode(tradeNo != null ? tradeNo : "", StandardCharsets.UTF_8);
            log.info("重定向到前端: {}", redirectUrl);
            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            log.error("重定向失败", e);
        }
    }
}
