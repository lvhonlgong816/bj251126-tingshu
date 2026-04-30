package com.atguigu.tingshu;

import com.atguigu.tingshu.common.constant.RedisConstant;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling  //开启定时任务
public class ServiceAlbumApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(ServiceAlbumApplication.class, args);
    }

    @Autowired
    private RedissonClient redissonClient;

    /**
     * Spring应用启动后会自动触发一次
     *
     * @param args incoming main method arguments
     * @throws Exception
     */
    @Override
    public void run(String... args) throws Exception {
        //1.判断布隆过滤器是否已经初始化完成
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        boolean exists = bloomFilter.isExists();
        //2.初始化布隆过滤器 误判率 期望数据规模 底层通过Redis Hash存放配置信息 1.数据规模  2.误判率 3.hash次数 4.bitmap长度
        if (!exists) {
            boolean flag = bloomFilter.tryInit(10000, 0.03);
            log.info("初始化布隆过滤器：{}", flag);
        }
    }
}
