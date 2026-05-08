package com.atguigu.tingshu.order.service;

import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface OrderInfoService extends IService<OrderInfo> {


    /**
     * 订单结算（会员套餐、专辑、声音）
     * @param tradeVo 交易vo信息 包含：购买项目类型、项目ID、购买声音数量
     * @return 订单VO信息
     */
    OrderInfoVo trade(Long userId, TradeVo tradeVo);

    /**
     * 提交/结算订单（处理余额支付逻辑）
     * @param userId 用户ID
     * @param orderInfoVo 订单vo信息
     * @return {"orderNo":"本次订单保存后订单编号"} 用于后续对接微信支付或者展示订单详情
     */
    Map<String, String> submitOrder(Long userId, OrderInfoVo orderInfoVo);

    /**
     * 保存订单信息
     * @param userId 用户ID
     * @param orderInfoVo 订单VO信息
     * @return 订单对象
     */
    OrderInfo saveOrderInfo(Long userId, OrderInfoVo orderInfoVo);

    /**
     * 根据订单编号查询订单详情（包含订单明细列表，减免列表）
     * @param orderNo
     * @return
     */
    OrderInfo getOrderInfo(String orderNo);

    /**
     * 分页查询订单(包含订单明细、减免列表)
     * @param pageInfo
     * @param userId
     * @return
     */
    Page<OrderInfo> findUserPage(Page<OrderInfo> pageInfo, Long userId);
}
