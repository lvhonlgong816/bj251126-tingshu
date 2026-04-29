package com.atguigu.tingshu.user.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.common.util.MongoUtil;
import com.atguigu.tingshu.model.user.UserListenProcess;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"all"})
public class UserListenProcessServiceImpl implements UserListenProcessService {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 获取用户某个声音播放进度
     *
     * @param userId
     * @param trackId
     * @return
     */
    @Override
    public BigDecimal getTrackBreakSecond(Long userId, Long trackId) {
        //1.获取指定用户播放进度集合名称
        String collectionName = this.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId);
        //2.构建查询条件对象
        Query query = new Query(Criteria.where("userId").is(userId).and("trackId").is(trackId));
        UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, collectionName);
        //3.返回声音播放进度
        if (userListenProcess != null) {
            return userListenProcess.getBreakSecond();
        }
        return BigDecimal.ZERO;
    }

    @Override
    public String getCollectionName(MongoUtil.MongoCollectionEnum collectionEnum, Long userId) {
        return collectionEnum.getCollectionPrefix() + "_" + userId;
    }

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 更新声音播放进度
     *
     * @param userId
     * @param userListenProcessVo
     */
    @Override
    public void updateListenProcess(Long userId, UserListenProcessVo userListenProcessVo) {
        //1.获取指定用户播放进度集合名称
        String collectionName = this.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId);
        //2.构建查询条件对象
        Query query = new Query(Criteria.where("userId").is(userId).and("trackId").is(userListenProcessVo.getTrackId()));
        UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, collectionName);
        //3.如果播放进度存在则更新播放数时间
        BigDecimal breakSeconds = userListenProcessVo.getBreakSecond().setScale(0, RoundingMode.HALF_UP);
        if (userListenProcess != null) {
            userListenProcess.setUpdateTime(new Date());
            userListenProcess.setBreakSecond(breakSeconds);
        } else {
            //4.如果播放进度不存在则新增记录
            userListenProcess = new UserListenProcess();
            userListenProcess.setUserId(userId);
            userListenProcess.setUpdateTime(new Date());
            userListenProcess.setAlbumId(userListenProcessVo.getAlbumId());
            userListenProcess.setCreateTime(new Date());
            userListenProcess.setTrackId(userListenProcessVo.getTrackId());
            userListenProcess.setBreakSecond(breakSeconds);
        }
        mongoTemplate.save(userListenProcess, collectionName);

        //5.更新声音/专辑播放进度  通过MQ实现 一方面是数据库声音/专辑统计表 第二方面索引库中专辑统计数值
        //5.1 业务限制 同一个用户对于某个声音统计数值当天内只能更新一次 生产消息幂等性
        //解决方案：利用Redis提供set ex nx 当key不存在才可以写入成功 发送自定义VO必须实现序列哈接口 设置序列哈版本
        String redisKey = RedisConstant.USER_TRACK_REPEAT_STAT_PREFIX
                + userId + ":" + userListenProcessVo.getAlbumId() + ":" + userListenProcessVo.getTrackId();
        long ttl = DateUtil.endOfDay(new Date()).getTime() - System.currentTimeMillis();
        Boolean flag = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, userListenProcessVo.getTrackId(), ttl, TimeUnit.MILLISECONDS);
        if (flag) {
            TrackStatMqVo trackStatMqVo = new TrackStatMqVo();
            String businessNo = IdUtil.randomUUID();
            trackStatMqVo.setBusinessNo(businessNo);
            trackStatMqVo.setAlbumId(userListenProcessVo.getAlbumId());
            trackStatMqVo.setTrackId(userListenProcessVo.getTrackId());
            trackStatMqVo.setStatType(SystemConstant.TRACK_STAT_PLAY);
            trackStatMqVo.setCount(1);
            //5.2 发送增量更新统计数值消息
            rabbitService.sendMessage(MqConst.EXCHANGE_TRACK, MqConst.ROUTING_TRACK_STAT_UPDATE, trackStatMqVo);
        }
    }

    /**
     * 查询当前用户最近播放专辑/声音
     *
     * @return {"albumId“："","trackId":""}
     */
    @Override
    public Map<String, Long> getLatelyTrack(Long userId) {
        //1.获取指定用户播放进度集合名称
        String collectionName = this.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId);
        //2.构建查询条件对象
        Query query = new Query(
                Criteria.where("userId").is(userId)
        );
        query.with(Sort.by(Sort.Direction.DESC, "updateTime"));
        UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, collectionName);
        if (userListenProcess != null) {
            return Map.of("albumId", userListenProcess.getAlbumId(), "trackId", userListenProcess.getTrackId());
        }
        return Map.of();
    }

    @Autowired
    private RabbitService rabbitService;


    public static void main(String[] args) {
        DateTime dateTime = DateUtil.endOfDay(new Date());
        System.out.println(dateTime.getTime());
    }
}
