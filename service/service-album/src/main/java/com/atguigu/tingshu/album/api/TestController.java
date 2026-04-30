package com.atguigu.tingshu.album.api;

import cn.hutool.core.collection.CollUtil;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author: atguigu
 * @create: 2026-04-29 11:42
 */
@Slf4j
@RestController
@RequestMapping("/api/album/test")
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

    //@Autowired
    //private RedissonClient redissonClient;

    @Autowired
    private AlbumInfoService albumInfoService;

    @GetMapping("/bloomfilter/add")
    public void addDataToBloomFilter() {
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        List<AlbumInfo> list = albumInfoService.list(
                new LambdaQueryWrapper<AlbumInfo>()
                        .eq(AlbumInfo::getStatus, SystemConstant.ALBUM_STATUS_PASS)
                        .select(AlbumInfo::getId)
        );
        if (CollUtil.isNotEmpty(list)) {
            for (AlbumInfo albumInfo : list) {
                bloomFilter.add(albumInfo.getId());
            }
        }
    }


    /**
     * 读读允许并发
     * 读写，写读，写写都不允许并发
     *
     * @return
     */
    @GetMapping("/read/{id}")
    public Result readLock(@PathVariable Long id) {
        //1.创建读写锁对象
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("lock:" + id);
        //2.获取读锁对象
        RLock rLock = readWriteLock.readLock();
        //3.获取读锁成功 执行读操作业务  持有读锁期间，其他线程无法获取写锁
        rLock.lock(5, TimeUnit.SECONDS);
        try {
            log.info("执行查询数据库业务：{}", id);
            return Result.ok("读成功：" + id);
        } finally {
            //4.释放读锁
            //rLock.unlock();  //读锁在5s会自动释放
        }
    }

    @GetMapping("/write/{id}")
    public Result writeLock(@PathVariable Long id) {
        //1.创建读写锁对象
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("lock:" + id);
        //2.获取写锁对象
        RLock rLock = readWriteLock.writeLock();

        //3.获取写锁成功 执行写操作业务  持有写锁期间，其他线程无法获取读锁，写锁
        rLock.lock(5, TimeUnit.SECONDS);
        try {
            log.info("执行更新数据库业务：{}", id);
            return Result.ok("写成功：" + id);
        } finally {
            //4.释放写锁
            //rLock.unlock();  //写锁在5s会自动释放
        }
    }

}
