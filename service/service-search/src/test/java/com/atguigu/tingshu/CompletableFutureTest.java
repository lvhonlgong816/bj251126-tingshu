package com.atguigu.tingshu;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author: atguigu
 * @create: 2026-04-24 10:13
 */
@Slf4j
public class CompletableFutureTest {

    public static void main(String[] args) {
        log.info("主线程start,{}",Thread.currentThread().getId());
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

        //2.对异步任务进行编排
        //2.1 创建异步任务A A有返回值 提供给BC任务使用
        CompletableFuture<String> completableFutureA = CompletableFuture.supplyAsync(() -> {
            log.info("线程信息：{}，异步任务A执行", Thread.currentThread().getId());
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return "aResult";
        }, threadPoolExecutor);

        //2.2 创建任务D 独立异步任务  跟A可以并行
        CompletableFuture<Void> completableFutureD = CompletableFuture.runAsync(() -> {
            log.info("线程信息：{}，异步任务D执行", Thread.currentThread().getId());
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, threadPoolExecutor);

        //2.3 在异步任务A执行后
        //2.3.1 基于异步任务A创建异步任务B
        CompletableFuture<Void> completableFutureB = completableFutureA.thenAcceptAsync(a -> {
            log.info("线程信息：{}，异步任务B执行，得到A的结果：{} ", Thread.currentThread().getId(), a);
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, threadPoolExecutor);
        //2.3.2 基于异步任务A创建异步任务C
        CompletableFuture<Void> completableFutureC = completableFutureA.thenAcceptAsync(a -> {
            log.info("线程信息：{}，异步任务C执行，得到A的结果：{} ", Thread.currentThread().getId(), a);
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, threadPoolExecutor);

        //3.让所有异步任务都执行完毕，主线程继续执行
        CompletableFuture.allOf(
                completableFutureA,
                completableFutureB,
                completableFutureC,
                completableFutureD
        ).join();
        log.info("主线程end,{}",Thread.currentThread().getId());
    }
}
