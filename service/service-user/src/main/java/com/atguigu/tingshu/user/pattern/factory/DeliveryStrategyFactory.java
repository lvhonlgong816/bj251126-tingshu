package com.atguigu.tingshu.user.pattern.factory;

import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.user.pattern.DeliveryStrategy;
import com.atguigu.tingshu.user.pattern.impl.AlbumDeliveryStrategy;
import com.atguigu.tingshu.user.pattern.impl.VIPDeliveryStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 自动发现所有策略实现类对象；对外提供根据类型获取不同策略实现类对象
 *
 * @author: atguigu
 * @create: 2026-05-07 14:46
 */
@Slf4j
@Component
public class DeliveryStrategyFactory {


    /**
     * 采用Spring提供自动注入；将DeliveryStrategy接口下所有实现类对象注入到Map中
     * Map中Key就是实现类对象Bean的名称；Map中Value就是具体实现类对象
     */
    @Autowired
    private Map<String, DeliveryStrategy> deliveryStrategyMap;


    @Autowired
    private List<DeliveryStrategy> deliveryStrategyList;


    /**
     * 根据购买项目类型自动从工厂中获取实现类对象
     * @param itemType
     * @return
     */
    public DeliveryStrategy getDeliveryStrategy(String itemType){
        if(deliveryStrategyMap.containsKey(itemType)){
            return deliveryStrategyMap.get(itemType);
        }
        throw new GuiguException(500, "暂不支持处理该策略类型");

    }
}
