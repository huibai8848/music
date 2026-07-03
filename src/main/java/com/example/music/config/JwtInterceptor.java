package com.example.music.config;

import com.example.music.constant.ErrorCode;
import com.example.music.constant.RedisKeys;
import com.example.music.exception.BusinessException;
import com.example.music.utils.CacheUtil;
import com.example.music.utils.JwtUtil;
import com.example.music.utils.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

/**
 * JWT 鉴权拦截器
 * <p>
 * 拦截所有 /api/** 请求（排除公共接口），解析 Token 后注入用户信息到 {@link RequestContext}。
 * <p>
 * 拦截规则：
 * - 白名单接口（注册/登录/公开接口）不校验 Token
 * - 其他接口需要有效的 access_token
 * - 管理员接口额外校验 role = ADMIN
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final CacheUtil cacheUtil;

    /** 白名单路径（不需要登录即可访问，精确匹配） */
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/public-key",
            "/api/banners",
            "/api/rankings",
            "/api/categories"
    );

    /** GET 请求白名单前缀（仅 GET 方法放行，写操作需认证） */
    private static final List<String> WHITE_LIST_GET_PREFIXES = Arrays.asList(
            "/api/songs",
            "/api/albums",
            "/api/artists",
            "/api/playlists",
            "/api/rooms",
            "/api/comments",
            "/api/likes",
            "/api/users/"
    );

    /** 管理员接口前缀 */
    private static final String ADMIN_PREFIX = "/api/admin";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 请求结束后清除上下文（防止内存泄漏）
        RequestContext.clear();

        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 1. OPTIONS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // 2. 白名单路径直接放行（不注入用户信息）
        if (isWhiteList(uri, method)) {
            return true;
        }

        // 3. 从请求头获取 Token
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 去掉 Bearer 前缀
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 4. 校验 Token 有效性
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        // 5. 检查 Token 是否在黑名单中（已登出）
        String jti = jwtUtil.getJtiFromToken(token);
        if (jti != null && cacheUtil.exists(RedisKeys.BL_TOKEN + jti)) {
            throw new BusinessException(ErrorCode.TOKEN_BLACKLISTED);
        }

        // 6. 提取用户信息并注入上下文
        Long userId = jwtUtil.getUserIdFromToken(token);
        String role = jwtUtil.getRoleFromToken(token);

        if (userId == null) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        RequestContext.setUserId(userId);
        RequestContext.setUserRole(role);

        // 7. 管理员接口校验
        if (uri.startsWith(ADMIN_PREFIX) && !"ADMIN".equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束后清除 ThreadLocal，防止内存泄漏
        RequestContext.clear();
    }

    /**
     * 判断是否是白名单路径（无需登录）
     * <p>
     * 规则：
     * 1. 静态资源直接放行（/data/、/error、/api/files/ 文件访问）
     * 2. 精确路径白名单（任何方法均可，如 /api/auth/login）
     * 3. GET 请求白名单前缀（仅 GET 方法放行，如 /api/songs GET 公开但 POST/PUT/DELETE 需认证）
     */
    private boolean isWhiteList(String uri, String method) {
        // 静态资源路径放行（已上传的文件，不包括上传接口本身）
        if (uri.startsWith("/data/") || uri.startsWith("/error")) {
            return true;
        }
        // /api/files/{type}/... 是已上传文件的访问路径，放行
        // /api/files/upload 是上传接口，需要认证
        if (uri.startsWith("/api/files/") && !uri.equals("/api/files/upload")) {
            return true;
        }
        // 精确匹配白名单（所有方法放行）
        for (String white : WHITE_LIST) {
            if (uri.equals(white)) {
                return true;
            }
        }
        // GET 请求白名单前缀（仅 GET 方法放行）
        if ("GET".equalsIgnoreCase(method)) {
            // 以下 GET 路径需登录（即使在前缀白名单中也不放行）
            if (uri.equals("/api/users/me")
                    || uri.equals("/api/playlists/mine")) {
                return false;
            }
            for (String prefix : WHITE_LIST_GET_PREFIXES) {
                if (uri.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }
}