package com.example.music.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.example.music.aspect.OperationLog;
import com.example.music.mapper.OperationLogMapper;
import com.example.music.utils.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志 AOP 切面
 * <p>
 * 拦截所有标注 {@link OperationLog @OperationLog} 的方法，
 * 自动记录操作人、操作类型、目标对象、请求参数、响应结果、IP 地址到 operation_log 表。
 * <p>
 * 日志记录异步执行（@Async），不阻塞主业务流程。
 * 任何日志记录异常被吞没，不影响业务正常执行。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogMapper operationLogMapper;

    /**
     * 环绕通知：执行方法并记录操作日志
     */
    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 执行原方法
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            // 异常也记录日志，然后继续抛出
            try {
                saveLog(joinPoint, operationLog, null, e, System.currentTimeMillis() - startTime);
            } catch (Exception ignored) {
                // 日志记录异常不影响主流程
            }
            throw e;
        }

        // 异步记录操作日志
        try {
            saveLog(joinPoint, operationLog, result, null, System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.warn("记录操作日志失败（不影响主流程）: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 异步保存操作日志
     */
    @Async
    public void saveLog(ProceedingJoinPoint joinPoint, OperationLog annotation,
                        Object result, Throwable error, long costMs) {
        try {
            com.example.music.entity.OperationLog logEntry = new com.example.music.entity.OperationLog();

            // 1. 操作人
            Long operatorId = RequestContext.getUserId();
            if (operatorId == null) {
                // 未登录的操作不记录日志（如白名单接口）
                return;
            }
            logEntry.setOperatorId(operatorId);

            // 2. 操作类型（action）
            String action = annotation.value();
            if (StrUtil.isBlank(action)) {
                action = joinPoint.getSignature().getName();
            }
            logEntry.setAction(action);

            // 3. 目标对象类型和 ID（从方法参数推断）
            Object[] args = joinPoint.getArgs();
            String targetType = annotation.targetType();
            if (StrUtil.isBlank(targetType)) {
                targetType = inferTargetType(action);
            }
            logEntry.setTargetType(targetType);
            logEntry.setTargetId(inferTargetId(args));

            // 4. 操作详情（请求参数 JSON）
            Map<String, Object> detailMap = new HashMap<>();
            detailMap.put("args", buildArgsSummary(args));
            detailMap.put("result", result != null ? safeToString(result) : null);
            if (error != null) {
                detailMap.put("error", error.getMessage());
            }
            detailMap.put("costMs", costMs);
            logEntry.setDetail(JSONUtil.toJsonStr(detailMap));

            // 5. IP 地址
            logEntry.setIp(getClientIp());

            // 6. 创建时间
            logEntry.setCreatedTime(LocalDateTime.now());

            // 7. 写入数据库
            operationLogMapper.insert(logEntry);

        } catch (Exception e) {
            log.warn("操作日志异步写入失败: {}", e.getMessage());
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 从 action 名称推断 targetType
     */
    private String inferTargetType(String action) {
        if (action.contains("用户") || action.contains("USER") || action.contains("User")) {
            return "USER";
        }
        if (action.contains("歌曲") || action.contains("SONG") || action.contains("Song")) {
            return "SONG";
        }
        if (action.contains("评论") || action.contains("COMMENT") || action.contains("Comment")) {
            return "COMMENT";
        }
        if (action.contains("举报") || action.contains("REPORT") || action.contains("Report")) {
            return "REPORT";
        }
        if (action.contains("专辑") || action.contains("ALBUM") || action.contains("Album")) {
            return "ALBUM";
        }
        if (action.contains("艺人") || action.contains("ARTIST") || action.contains("Artist")) {
            return "ARTIST";
        }
        if (action.contains("轮播") || action.contains("BANNER") || action.contains("Banner")) {
            return "BANNER";
        }
        if (action.contains("公告") || action.contains("NOTICE") || action.contains("Notice")) {
            return "NOTICE";
        }
        return "OTHER";
    }

    /**
     * 从方法参数中推断目标 ID
     */
    private Long inferTargetId(Object[] args) {
        if (args == null) return null;
        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
            if (arg instanceof Integer) {
                return ((Integer) arg).longValue();
            }
            if (arg instanceof String && StrUtil.isNumeric((String) arg)) {
                return Long.parseLong((String) arg);
            }
        }
        return null;
    }

    /**
     * 构建参数摘要（避免记录敏感信息和超大参数）
     */
    private Map<String, Object> buildArgsSummary(Object[] args) {
        Map<String, Object> summary = new HashMap<>();
        if (args == null) return summary;
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) continue;
            // 跳过 HttpServletRequest/Response 等基础设施
            if (args[i] instanceof jakarta.servlet.ServletRequest
                    || args[i] instanceof jakarta.servlet.ServletResponse
                    || args[i] instanceof org.springframework.web.multipart.MultipartFile) {
                continue;
            }
            String key = "arg" + i;
            String value = safeToString(args[i]);
            // 超长参数截断
            if (value.length() > 500) {
                value = value.substring(0, 500) + "...";
            }
            summary.put(key, value);
        }
        return summary;
    }

    /**
     * 安全 toString（防止 toString 抛出异常）
     */
    private String safeToString(Object obj) {
        try {
            return JSONUtil.toJsonStr(obj);
        } catch (Exception e) {
            try {
                return obj.toString();
            } catch (Exception e2) {
                return "[无法序列化: " + obj.getClass().getSimpleName() + "]";
            }
        }
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "unknown";

            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
                // 取第一个 IP（客户端真实 IP）
                return ip.split(",")[0].trim();
            }
            ip = request.getHeader("X-Real-IP");
            if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
