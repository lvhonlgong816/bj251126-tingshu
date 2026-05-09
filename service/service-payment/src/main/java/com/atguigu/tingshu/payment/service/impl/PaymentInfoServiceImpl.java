package com.atguigu.tingshu.payment.service.impl;

import com.atguigu.tingshu.account.AccountFeignClient;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.atguigu.tingshu.order.client.OrderFeignClient;
import com.atguigu.tingshu.payment.mapper.PaymentInfoMapper;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wechat.pay.java.service.payments.model.Transaction;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@SuppressWarnings({"all"})
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {

    @Autowired
    private OrderFeignClient orderFeignClient;

    @Autowired
    private AccountFeignClient accountFeignClient;

    /**
     * 保存本地交易记录
     *
     * @param paymentType:支付类型 1301-订单 1302-充值
     * @param orderNo:         订单编号
     * @return 本地交易记录对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentInfo savePaymentInfo(String paymentType, String orderNo) {
        //1.根据订单编号查询本地交易记录，如果存在返回
        PaymentInfo paymentInfo = this.getOne(
                new LambdaQueryWrapper<PaymentInfo>()
                        .eq(PaymentInfo::getOrderNo, orderNo)
        );
        if (paymentInfo != null) {
            return paymentInfo;
        }
        //2.封装本地交易记录
        paymentInfo = new PaymentInfo();
        //2.0 封装基本属性：支付类型 订单编号 状态：未支付 支付状态：1401-未支付 1402-已支付 付款方式：微信
        paymentInfo.setOrderNo(orderNo);
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setPaymentStatus(SystemConstant.PAYMENT_STATUS_UNPAID);
        paymentInfo.setPayWay(SystemConstant.ORDER_PAY_WAY_WEIXIN);


        //TODO 微信支付交易号、回调时间、回调内容,收到微信支付异步回调后才更新
        //paymentInfo.setOutTradeNo();
        //paymentInfo.setCallbackTime();
        //paymentInfo.setCallbackContent();

        //2.1 如果支付类型是：订单 获取订单信息封装内容跟交易金额
        if (SystemConstant.PAYMENT_TYPE_ORDER.equals(paymentType)) {
            //2.1.1 远程调用 “订单服务“ 获取订单信息
            OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderNo).getData();
            //2.1.2 判断订单状态如果是已支付或已取消 则业务终止
            String orderStatus = orderInfo.getOrderStatus();
            if (SystemConstant.ORDER_STATUS_UNPAID.equals(orderStatus)) {
                paymentInfo.setUserId(orderInfo.getUserId());
                paymentInfo.setAmount(orderInfo.getOrderAmount());
                paymentInfo.setContent(orderInfo.getOrderTitle());
            } else {
                throw new GuiguException(500, "订单状态有误");
            }

        }
        //2.2 如果支付类型是：充值 获取充值信息封装内容跟交易金额
        if (SystemConstant.PAYMENT_TYPE_RECHARGE.equals(paymentType)) {
            //2.2.1 远程调用 “账户”服务获取充值记录
            RechargeInfo rechargeInfo = accountFeignClient.getRechargeInfo(orderNo).getData();
            String rechargeStatus = rechargeInfo.getRechargeStatus();
            //2.2.2 验证充值记录状态
            if (SystemConstant.ORDER_STATUS_UNPAID.equals(rechargeStatus)) {
                //2.2.3 封装属性
                paymentInfo.setUserId(rechargeInfo.getUserId());
                paymentInfo.setAmount(rechargeInfo.getRechargeAmount());
                paymentInfo.setContent("充值" + rechargeInfo.getRechargeAmount());
            } else {
                throw new GuiguException(500, "充值状态有误");
            }
        }
        //2.3 保存本地交易记录，返回本地交易记录对象
        this.save(paymentInfo);
        return paymentInfo;
    }


    /**
     * 用户支付成功后，处理核心业务
     *
     * @param transaction 微信交易对象
     */
    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void updatePaymentInfo(Transaction transaction) {
        //1.更新本地交易记录信息 状态、绑定微信交易订单号、回调时间、回调内容
        String orderNo = transaction.getOutTradeNo();

        //根据状态更新
        boolean update = this.update(
                new LambdaUpdateWrapper<PaymentInfo>()
                        .eq(PaymentInfo::getOrderNo, orderNo)
                        .eq(PaymentInfo::getPaymentStatus, SystemConstant.PAYMENT_STATUS_UNPAID)
                        .set(PaymentInfo::getPaymentStatus, SystemConstant.PAYMENT_STATUS_PAID)
                        .set(PaymentInfo::getOutTradeNo, transaction.getTransactionId())
                        .set(PaymentInfo::getCallbackTime, new Date())
                        .set(PaymentInfo::getCallbackContent, transaction.toString())
        );
        if (update) {
            PaymentInfo paymentInfo = this.getOne(
                    new LambdaQueryWrapper<PaymentInfo>()
                            .eq(PaymentInfo::getOrderNo, orderNo)
            );
            //支付类型：1301-订单 1302-充值
            String paymentType = paymentInfo.getPaymentType();
            //2.处理支付类型是：订单 修改订单状态：已支付、购买权益发放业务
            if (SystemConstant.PAYMENT_TYPE_ORDER.equals(paymentType)) {
                // 2.1 远程调用订单服务 更新订单状态及虚拟物品发货
                Result result = orderFeignClient.orderPaySuccess(orderNo);
                //2.2 判断远程调用业务状态码
                if (result.getCode().intValue() != 200) {
                    throw new GuiguException(result.getCode(), result.getMessage());
                }
            }

            //3.TODO 处理支付类型是：充值 充值状态：已支付、余额充值业务
            if (SystemConstant.PAYMENT_TYPE_RECHARGE.equals(paymentType)) {

            }
        }

    }
}
