package com.atguigu.tingshu;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.PrimitiveIterator;
import java.util.concurrent.*;

/**
 * @author: atguigu
 * @create: 2026-04-22 15:51
 */
@Slf4j
public class ThreadPoolTest {

    /**
     * 线程池优点：
     * 1.核心线程一旦创建就不会销毁，不需要频繁创建跟销毁，实现线程复用
     * 2.任务执行，不需要等待线程创建，直接获取到空闲线程执行业务，效率自然提升
     * 线程池7个参数：
     * 1.核心线程数
     * 2.最大线程数
     * 3.空闲时间
     * 4.时间单位
     * 5.线程工厂
     * 6.拒绝策略
     * 7.阻塞队列
     * 创建线程池：
     * 方式一：采用工具类Executors 严禁使用，可能OOM问题
     * 方式二：自定义线程池 通过ThreadPoolExecutor设置线程池7个参数
     *
     * 线程数设置为多少合适： 一般建议核心数=最大线程数 避免非核心线程创建销毁带来性能开销
     * 一：结合应用类型（CPU密集型、IO密集型）设置初始数值
     *       CPU密集型：大数据计算、用户画像                 线程数=CPU核心数
     *       IO密集型（Java业务系统偏向）：网络IO、文件IO居多  线程数=CPU核心数*2
     * 二：通过压力测试进行压测得到合适值，达到最大吞吐量
     */
    public static void main(String[] args) throws ExecutionException, InterruptedException, TimeoutException {
        int i1 = Runtime.getRuntime().availableProcessors();
        System.out.println(i1);
        int nThreads = i1 * 2;
        //1.创建线程池 同一时间能处理任务数量： 最大线程数+阻塞队列
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                nThreads,
                nThreads,
                0,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(5),
                Executors.defaultThreadFactory(),
                //默认：任务会丢失，并且抛出异常
                //new ThreadPoolExecutor.AbortPolicy()
                //静默方式丢弃新提交任务
                //new ThreadPoolExecutor.DiscardPolicy()
                //静默方式丢弃阻塞队列队首任务，将新任务提交线程池
                //new ThreadPoolExecutor.DiscardOldestPolicy());
                //返回给调用者线程执行任务 任务不丢
                new ThreadPoolExecutor.CallerRunsPolicy());
        //默认核心线程是第一个任务提交，才创建核心线程
        //threadPoolExecutor.prestartCoreThread();
        threadPoolExecutor.prestartAllCoreThreads();
        //2.向线程池提交任务
        //Future<String> future = threadPoolExecutor.submit(() -> {
        //    log.info("线程信息：{}  执行", Thread.currentThread().getId());
        //    return "子线程结果";
        //});
        //String s = future.get(1, TimeUnit.SECONDS);
        //System.out.println(s);
        for (int i = 1; i <= 11; i++) {
            int finalI = i;
            threadPoolExecutor.submit(() -> {
                log.info("线程信息：{}，任务{} 执行", Thread.currentThread().getId(), finalI);
            });
        }

        //关闭线程池
        threadPoolExecutor.shutdown();
    }
}
