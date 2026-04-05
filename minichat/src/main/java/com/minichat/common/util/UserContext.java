package com.minichat.common.util;
/**
 * 存储当前登录用户的上下文信息
 */
public class UserContext {
    // ThreadLocal 存储当前登录用户的id
    private static final ThreadLocal<Long> CURRENT_USER = new ThreadLocal<>();

    // 设置当前登录用户的用户名
    public static void setCurUserId(Long userId) {
        CURRENT_USER.set(userId);
    }

    // 获取当前登录用户的用户名
    public static Long getCurUserId() {
        return CURRENT_USER.get();
    }

    public static void remove() {
        CURRENT_USER.remove();
    }
}
