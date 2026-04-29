package com.atguigu.tingshu.common.cache;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author: atguigu
 * @create: 2026-04-29 14:31
 */
@Slf4j
@Aspect
@Component
public class GuiGuCacheAspect {


    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 对自定义缓存注解进行增强
     *
     * @param pjp
     * @param guiGuCache
     * @return
     * @throws Throwable
     */
    @Around("@annotation(guiGuCache)")
    public Object doBasicProfiling(ProceedingJoinPoint pjp, GuiGuCache guiGuCache) throws Throwable {
        try {
            //1.优先从Redis缓存获取业务数据 如果命中则返回即可
            String param = "";
            //1.1 先获取到方法参数值
            List<Object> paramsList = Arrays.asList(pjp.getArgs());
            if (CollUtil.isNotEmpty(paramsList)) {
                param = paramsList.stream().map(p -> p.toString()).collect(Collectors.joining("_"));
            } else {
                //如果方法无参数 获取到方法名称
                param = pjp.getSignature().getName();
            }
            //1.2 定义业务数据Key 形式：注解中前缀值+方法参数值
            String dataKey = guiGuCache.prefix() + param;
            //1.3 查询Redis缓存
            Object result = redisTemplate.opsForValue().get(dataKey);
            if (result != null) {
                return result;
            }
            //2.获取分布式锁
            //2.1 创建锁Key 形式：业务数据Key+锁后缀
            String lockKey = dataKey + RedisConstant.CACHE_LOCK_SUFFIX;
            //2.2 创建锁对象
            RLock lock = redissonClient.getLock(lockKey);
            //2.3 尝试获取分布式锁
            boolean flag = lock.tryLock(RedisConstant.ALBUM_LOCK_WAIT_PX1, RedisConstant.CACHE_LOCK_EXPIRE_PX1, TimeUnit.SECONDS);
            //3.获取分布式锁成功，则执行业务（1.执行目标方法 2.将从DB获取业务数据存入Redis 3.释放锁）
            if (flag) {
                try {
                    //3.1 执行目标方法(查询数据库方法)
                    result = pjp.proceed();
                    //3.2 将业务数据放入Redis缓存 时间设置：基础时间+随机时间
                    if (result != null) {
                        long ttl = RedisConstant.ALBUM_TIMEOUT + RandomUtil.randomInt(300, 600);
                        redisTemplate.opsForValue().set(dataKey, result, ttl, TimeUnit.SECONDS);
                        //3.3 返回业务数据
                        return result;
                    }
                } finally {
                    //3.4 将锁释放
                    lock.unlock();
                }
            } else {
                //4.获取锁失败，则自旋（再次查询缓存命中缓存）
                TimeUnit.MILLISECONDS.sleep(50);
                return this.doBasicProfiling(pjp, guiGuCache);
            }
        } catch (Throwable e) {
            log.error("缓存切面异常：", e);
            // 如果Redis服务不可用，则执行兜底方案：查询DB
            return pjp.proceed();
        }
        return null;
    }
}
