package com.atguigu.tingshu.account.receiver;

import cn.hutool.core.collection.CollUtil;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
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

import java.util.Map;
import java.util.Objects;

/**
 * @author: atguigu
 * @create: 2026-04-21 14:30
 */
@Slf4j
@Component
public class AccountReceiver {

    //创建交换机 队列 绑定 方式 1.web管理页面 2.注册对象方式 3.监听器注解

    @Autowired
    private UserAccountService userAccountService;

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = MqConst.EXCHANGE_USER, durable = "true"),
            value = @Queue(value = MqConst.QUEUE_USER_REGISTER, durable = "true"),
            key = MqConst.ROUTING_USER_REGISTER
    ))
    public void initUserAccount(Map<String, Object> msgData, Channel channel, Message message){
        if(CollUtil.isNotEmpty(msgData)){
            log.info("【账户服务】监听到初始化账户消息：{}", msgData);
            userAccountService.initUserAccount(msgData);
        }
        //手动确定消息 p1:消息派发标签值（依次累加） p2:批量确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }


}
