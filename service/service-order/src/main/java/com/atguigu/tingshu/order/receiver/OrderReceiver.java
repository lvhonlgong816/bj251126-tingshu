package com.atguigu.tingshu.order.receiver;

import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * @author: atguigu
 * @create: 2026-05-09 09:22
 */
@Slf4j
@Component
public class OrderReceiver {

    @Autowired
    private OrderInfoService orderInfoService;

    /**
     * 监听延迟关单消息
     * @param orderId 订单ID
     * @param channel
     * @param message
     */
    @SneakyThrows
    @RabbitListener(queues = {MqConst.QUEUE_CANCEL_ORDER})
    public void cancelOrder(Long orderId, Channel channel, Message message) {
        if (orderId != null) {
            log.info("监听延迟关单消息：{}", orderId);
            orderInfoService.cancelOrder(orderId);
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
