package com.atguigu.tingshu.common.threadpol;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author: atguigu
 * @create: 2026-04-24 10:32
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * JDK提供线程池对象ThreadPoolExecutor
     * @return
     */
    @Bean
    public Executor threadPoolExecutor() {
        int i = Runtime.getRuntime().availableProcessors();
        int nThreads = i * 2;
        //1.创建线程池 作用：执行任务
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                nThreads,
                nThreads,
                0,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(200),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        threadPoolExecutor.prestartCoreThread();
        return threadPoolExecutor;
    }
}
