package com.atguigu.tingshu.common.util;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * 获取当前用户信息帮助类
 */
public class AuthContextHolder {

    //非异步场景下使用，如果采用异步 无法获取到数据
    //private static ThreadLocal<Long> userId = new ThreadLocal<Long>();
    //单独手动开启线程 Thread().start() InheritableThreadLocal可以父子线程之间传递 问题：无法使用到线程池场景
    //private static ThreadLocal<Long> userId = new InheritableThreadLocal<Long>();
    //采用线程池  解决办法采用阿里开源
    private static ThreadLocal<Long> userId = new TransmittableThreadLocal<>();

    public static void setUserId(Long _userId) {
        userId.set(_userId);
    }

    public static Long getUserId() {
        return userId.get();
    }

    public static void removeUserId() {
        userId.remove();
    }

}
