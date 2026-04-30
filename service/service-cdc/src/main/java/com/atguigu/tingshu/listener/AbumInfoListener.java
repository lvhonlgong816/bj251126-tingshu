package com.atguigu.tingshu.listener;

import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.model.album.AlbumInfo;
import io.xzxj.canal.core.annotation.CanalListener;
import io.xzxj.canal.core.listener.EntryListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * @author: atguigu
 * @create: 2026-04-30 11:44
 */
@Slf4j
@Component
@CanalListener(schemaName = "tingshu_album", tableName = "album_info", destination = "tingshuTopic")
public class AbumInfoListener implements EntryListener<AlbumInfo> {


    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 当原始数据库中专辑信息被修改，Canal服务端就会读取Binglog放入话题，当前Canal客户端读取话题中数据，将数据转为Java对象
     * 该方法
     * @param before 变更前数据
     * @param after 变后前数据
     * @param fields 变更字段信息
     */
    @Override
    public void update(AlbumInfo before, AlbumInfo after, Set<String> fields) {
        log.info("监听到专辑表更新：{}，字段变更：{}", after, fields);
        String key = RedisConstant.ALBUM_INFO_PREFIX+after.getId();
        redisTemplate.delete(key);
    }


}
