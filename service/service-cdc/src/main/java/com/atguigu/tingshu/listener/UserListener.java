package com.atguigu.tingshu.listener;

import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.user.UserInfo;
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
@CanalListener(schemaName = "tingshu_user", tableName = "user_info", destination = "tingshuTopic")
public class UserListener implements EntryListener<UserInfo> {


    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void update(UserInfo before, UserInfo after, Set<String> fields) {
        log.info("监听到用户表更新：{}，字段变更：{}", after, fields);
        String key = "user:info:" + after.getId();
        redisTemplate.delete(key);
    }


}
