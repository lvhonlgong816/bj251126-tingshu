package com.atguigu.tingshu.search.receiver;

import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.search.service.SearchService;
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
 * @create: 2026-04-22 15:20
 */
@Slf4j
@Component
public class SearchReceiver {

    @Autowired
    private SearchService searchService;


    /**
     * 监听上架专辑队列
     * @param albumId 专辑ID
     * @param channel
     * @param message
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = MqConst.EXCHANGE_ALBUM, durable = "true"),
            value = @Queue(value = MqConst.QUEUE_ALBUM_UPPER, durable = "true"),
            key = MqConst.ROUTING_ALBUM_UPPER
    ))
    public void upperAlbum(Long albumId, Channel channel, Message message) {
        if (albumId != null) {
            log.info("[搜索服务]监听到上架专辑ID：{}", albumId);
            searchService.upperAlbum(albumId);
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    /**
     * 监听下架专辑队列
     * @param albumId 专辑ID
     * @param channel
     * @param message
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = MqConst.EXCHANGE_ALBUM, durable = "true"),
            value = @Queue(value = MqConst.QUEUE_ALBUM_LOWER, durable = "true"),
            key = MqConst.ROUTING_ALBUM_LOWER
    ))
    public void lowerAlbum(Long albumId, Channel channel, Message message) {
        if (albumId != null) {
            log.info("[搜索服务]监听到下架专辑ID：{}", albumId);
            searchService.lowerAlbum(albumId);
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 监听更新声音统计消息，更新ES中专辑统计数值
     *
     * @param mqVo
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = MqConst.EXCHANGE_TRACK, durable = "true"),
            value = @Queue(value = MqConst.QUEUE_ALBUM_ES_STAT_UPDATE, durable = "true"),
            key = MqConst.ROUTING_TRACK_STAT_UPDATE
    ))
    public void updateAlbumStat(TrackStatMqVo mqVo, Channel channel, Message message) {
        if (mqVo != null) {
            log.info("【搜索服务】监听到更新声音统计消息：{}", mqVo);
            //1.先对消费者进行幂等性处理
            //1.1 找出业务消息的标识 作为setnx中key
            String redisKey = RedisConstant.USER_TRACK_REPEAT_STAT_PREFIX + "es:" + mqVo.getBusinessNo();
            //1.2 尝试存入Redis
            Boolean flag = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", 10, TimeUnit.MINUTES);
            if (flag) {
                try {
                    //2.更新声音/专辑统计数值
                    searchService.updateAlbumStat(mqVo);
                } catch (Exception e) {
                    redisTemplate.delete(redisKey);
                    channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
                }
            }
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
