package com.atguigu.tingshu;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author: atguigu
 * @create: 2026-04-27 10:52
 */

/**
 * Slf4j是Java所有日志规范，所有日志框架都得实现规范
 * 常见日志框架：log4j、log4j2、logback(SpringBoot默认使用日志框架)
 * */
@Slf4j
@SpringBootTest
public class LogTest {

    /**
     * 日志级别从低到高：DEBUG-->INFO--->WARN--->ERROR
     * 开发环境将级别设置为：DEBUG或INOF
     * 生产环境：将级别调整WARN或ERROR
     * 通过级别设置日志结果
     */
    @Test
    public void testLog(){
        log.debug("debug日志");
        log.info("info日志");
        log.warn("warn日志");
        log.error("error日志");
    }

    /**
     * 日志级别从低到高：DEBUG-->INFO--->WARN--->ERROR
     * 通过级别设置日志结果
     */
    //public static void main(String[] args) {
    //    log.debug("debug日志");
    //    log.info("info日志");
    //    log.warn("warn日志");
    //    log.error("error日志");
    //}
}
