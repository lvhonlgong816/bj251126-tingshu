package com.atguigu.tingshu.order.pattern;

import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;

/**
 * 结算不同商品类型 策略接口
 * @author: atguigu
 * @create: 2025-08-05 08:52
 */
public interface TradeStrategy {

    /**
     * 对不同商品类型结算抽象方法
     * @param tradeVo
     * @param userId
     * @return
     */
    OrderInfoVo trade(TradeVo tradeVo, Long userId);

}
