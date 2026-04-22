package com.atguigu.tingshu;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author: atguigu
 * @create: 2026-04-22 15:51
 */
@Slf4j
public class ThreadPoolTest {

    @Test
    public void testThreadPool() {
        //1.创建线程池
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                3,
                5,
                1,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(5),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );
        //2.向线程池提交任务
        for (int i = 1; i <= 5; i++) {
            int finalI = i;
            threadPoolExecutor.submit(() -> {
                log.info("线程信息：{}，任务{} 执行", Thread.currentThread().getId(), finalI);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public static void main(String[] args) {
        //1.创建线程池 同一时间能处理任务数量： 最大线程数+阻塞队列
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                3,
                5,
                1,
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

        //2.向线程池提交任务
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
