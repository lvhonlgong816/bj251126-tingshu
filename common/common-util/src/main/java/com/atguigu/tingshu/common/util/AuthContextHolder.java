package com.atguigu.tingshu.common.util;

/**
 * 获取当前用户信息帮助类
 */
public class AuthContextHolder {

    private static ThreadLocal<Long> userId = new ThreadLocal<Long>();

    public static void setUserId(Long _userId) {
        userId.set(_userId);
    }

    public static Long getUserId() {
        //return userId.get();
        //TODO 暂时将当前用户ID硬编码为：1 后续完成登录功能后，再从ThreadLocal中获取当前用户ID
        return 1L;
    }

    public static void removeUserId() {
        userId.remove();
    }

}
