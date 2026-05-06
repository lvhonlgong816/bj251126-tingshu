package com.atguigu.tingshu.order.service;

import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OrderInfoService extends IService<OrderInfo> {


    /**
     * 订单结算（会员套餐、专辑、声音）
     * @param tradeVo 交易vo信息 包含：购买项目类型、项目ID、购买声音数量
     * @return 订单VO信息
     */
    OrderInfoVo trade(Long userId, TradeVo tradeVo);
}
