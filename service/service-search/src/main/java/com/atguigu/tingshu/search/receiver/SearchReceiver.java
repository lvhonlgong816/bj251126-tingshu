package com.atguigu.tingshu.search.receiver;

import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.search.service.SearchService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
}
