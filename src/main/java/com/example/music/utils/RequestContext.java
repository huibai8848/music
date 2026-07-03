package com.example.music.utils;

/**
 * 请求上下文
 * <p>
 * 通过 ThreadLocal 存储当前请求的用户信息。
 * 由 JWT 鉴权拦截器在请求开始时注入，请求结束后清除防止内存泄漏。
 * <p>
 * 使用方式：
 * <pre>{@code
 *     // 在拦截器中设置
 *     RequestContext.setUserId(1L);
 *     RequestContext.setUserRole("ADMIN");
 *
 *     // 在 Service/Controller 中获取
 *     Long userId = RequestContext.getUserId();
 * }</pre>
 */
public class RequestContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_ROLE_HOLDER = new ThreadLocal<>();

    // ==================== userId ====================

    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    /**
     * @return 当前用户 ID（未登录时为 null）
     */
    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    // ==================== userRole ====================

    public static void setUserRole(String role) {
        USER_ROLE_HOLDER.set(role);
    }

    /**
     * @return 当前用户角色（未登录时为 null）
     */
    public static String getUserRole() {
        return USER_ROLE_HOLDER.get();
    }

    /**
     * @return true=当前请求已登录
     */
    public static boolean isLoggedIn() {
        return USER_ID_HOLDER.get() != null;
    }

    /**
     * @return true=当前请求是管理员
     */
    public static boolean isAdmin() {
        return "ADMIN".equals(USER_ROLE_HOLDER.get());
    }

    /**
     * 请求结束后清除 ThreadLocal（防止内存泄漏）
     * 在拦截器的 afterCompletion 中调用
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
        USER_ROLE_HOLDER.remove();
    }
}