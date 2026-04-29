package com.atguigu.tingshu.common.threadpol;

import com.atguigu.tingshu.common.zipkin.ZipkinHelper;
import com.atguigu.tingshu.common.zipkin.ZipkinTaskDecorator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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
     *
     * @return
     */
    @Bean
    //@Primary
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

    @Autowired
    private ZipkinHelper zipkinHelper;


    /**
     * @return
     */
    @Bean
    public Executor threadPoolTaskExecutor() {
        int count = Runtime.getRuntime().availableProcessors();
        int threadCount = count*2+1;
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        // 核心池大小
        taskExecutor.setCorePoolSize(threadCount);
        // 最大线程数
        taskExecutor.setMaxPoolSize(threadCount);
        // 队列程度
        taskExecutor.setQueueCapacity(300);
        // 线程空闲时间
        taskExecutor.setKeepAliveSeconds(0);
        // 线程前缀名称
        taskExecutor.setThreadNamePrefix("async-tingshu-Executor--");
        // 该方法用来设置 线程池关闭 的时候 等待 所有任务都完成后，再继续 销毁 其他的 Bean，
        // 这样这些 异步任务 的 销毁 就会先于 数据库连接池对象 的销毁。
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        // 任务的等待时间 如果超过这个时间还没有销毁就 强制销毁，以确保应用最后能够被关闭，而不是阻塞住。
        taskExecutor.setAwaitTerminationSeconds(300);
        // 线程不够用时由调用的线程处理该任务
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //设置解决zipkin链路追踪不完整装饰器对象
        taskExecutor.setTaskDecorator(new ZipkinTaskDecorator(zipkinHelper));
        return taskExecutor;
    }
}
