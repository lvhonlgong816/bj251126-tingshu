package com.atguigu.tingshu.album.receiver;

import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author: atguigu
 * @create: 2026-04-28 15:14
 */
@Slf4j
@Component
public class AlbumReceiver {

    @Autowired
    private TrackInfoService trackInfoService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 监听到增量更新统计数值消息
     *
     * @param mqVo    消息VO
     * @param channel
     * @param message
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = MqConst.EXCHANGE_TRACK, durable = "true"),
            value = @Queue(value = MqConst.QUEUE_TRACK_STAT_UPDATE, durable = "true"),
            key = MqConst.ROUTING_TRACK_STAT_UPDATE
    ))
    public void updateStat(TrackStatMqVo mqVo, Channel channel, Message message) {
        if (mqVo != null) {
            log.info("监听到增量更新统计消息：{}", mqVo);
            //采用Redis解决消息幂等性问题 同一个消息及时被同时或多次投递，只处理一次
            String redisKey = "key:biz:db:" + mqVo.getBusinessNo();
            boolean flag = redisTemplate.opsForValue().setIfAbsent(redisKey, null, 5, TimeUnit.MINUTES);
            if (flag) {
                trackInfoService.updateStat(mqVo);
            }
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }


}
