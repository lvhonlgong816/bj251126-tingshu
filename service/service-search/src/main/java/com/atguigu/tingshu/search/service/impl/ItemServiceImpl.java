package com.atguigu.tingshu.search.service.impl;

import cn.hutool.core.lang.Assert;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.search.service.ItemService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class ItemServiceImpl implements ItemService {

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private AlbumFeignClient albumFeignClient;

    /**
     * required a single bean, but 2 were found:原因 自定义线程池对象、框架自带线程池对象
     * 解决办法：
     * 1.@Primary在声明对象指定主要Bean
     * 2.@Autowired先按类型注入,再按Bean名称注入 eg：Executor threadPoolTaskExecutor;
     * 3.@Autowired+@Qualifier(指定Bean的ID)
     * 4.@Resource 先按Bean名称注入，再按类注入
     */
    //@Autowired
    //@Qualifier("threadPoolTaskExecutor")
    @Resource
    private Executor threadPoolTaskExecutor;


    /**
     * 专辑详情页数据汇总
     *
     * @param albumId
     * @return {"announcer":用户对象,"albumInfo":专辑对象,"albumStatVo":专辑统计对象,"baseCategoryView":分类对象}
     */
    @Override
    public Map<String, Object> getItem(Long albumId) {
        //1.创建Map封装四项数据 如果是多线程并发写Map HashMap是线程不安全  故采用线程安全ConcurrentHashMap
        Map<String, Object> map = new ConcurrentHashMap<>();

        //2.远程调用专辑服务获取专辑信息
        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
            Assert.notNull(albumInfo, "专辑：{}不存在", albumId);
            //子线程写入Map
            map.put("albumInfo", albumInfo);
            return albumInfo;
        }, threadPoolTaskExecutor);

        //3.远程调用专辑服务获取分类信息
        CompletableFuture<Void> categoryCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            BaseCategoryView baseCategoryView = albumFeignClient.getCategoryView(albumInfo.getCategory3Id()).getData();
            Assert.notNull(baseCategoryView, "专辑{}分类{}不存在", albumId, albumInfo.getCategory3Id());
            //子线程写入Map
            map.put("baseCategoryView", baseCategoryView);
        }, threadPoolTaskExecutor);

        //4.远程调用专辑服务获取统计信息
        CompletableFuture<Void> statCompletableFuture = CompletableFuture.runAsync(() -> {
            AlbumStatVo albumStatVo = albumFeignClient.getAlbumStatVo(albumId).getData();
            Assert.notNull(albumStatVo, "专辑{}统计不存在", albumId);
            //子线程写入Map
            map.put("albumStatVo", albumStatVo);
        }, threadPoolTaskExecutor);

        //5.远程调用用户服务获取主播信息
        CompletableFuture<Void> userCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(albumInfo.getUserId()).getData();
            Assert.notNull(userInfoVo, "专辑{}主播：{}信息缺失", albumId, albumInfo.getUserId());
            //子线程写入Map
            map.put("announcer", userInfoVo);
        }, threadPoolTaskExecutor);


        //6.组合所有异步任务
        CompletableFuture.allOf(
                albumInfoCompletableFuture,
                statCompletableFuture,
                categoryCompletableFuture,
                userCompletableFuture
        ).orTimeout(1, TimeUnit.SECONDS)
                .join();
        //6.返回map
        return map;
    }


    public static void main(String[] args) {
        Map<String, String> map = new ConcurrentHashMap<>();
        for (int i = 0; i < 100; i++) {
            int finalI = i;
            new Thread(()->{
                map.put("a"+ finalI, UUID.randomUUID().toString());
                System.out.println(map);
            }).start();
        }
    }


}
