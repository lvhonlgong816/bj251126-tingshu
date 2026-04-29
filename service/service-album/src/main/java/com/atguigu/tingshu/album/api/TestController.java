package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * @author: atguigu
 * @create: 2026-04-29 11:42
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 1.加锁解锁方法是框架提供好的，实现Lock接口加锁解锁方法
     * 2.可重入锁，一个线程加锁成功后，可以再次获取到锁
     * 3.业务超时，底层会自动对锁续期
     */
    @GetMapping("/testLock")
    public void testLock() {
        //for (int i = 0; i < 3; i++) {
        new Thread(() -> {
            log.info("线程：{},进入", Thread.currentThread().getName());
            //1.创建锁对象
            RLock lock = redissonClient.getLock("lock1");
            //2.获取锁
            lock.lock();
            log.info("线程：{},获取锁成功，执行业务", Thread.currentThread().getName());
            //当前线程第二次加锁
            lock.lock();
            log.info("线程：{},再次加锁成功", Thread.currentThread().getName());
            lock.unlock();


            System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");


            try {
                TimeUnit.SECONDS.sleep(600);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            lock.unlock();
        }, "thread-").start();
    }
    //}
}
