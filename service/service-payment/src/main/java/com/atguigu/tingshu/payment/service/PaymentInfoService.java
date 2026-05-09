package com.atguigu.tingshu.payment.service;

import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wechat.pay.java.service.payments.model.Transaction;

public interface PaymentInfoService extends IService<PaymentInfo> {

    /**
     * 保存本地交易记录
     *
     * @param paymentType:支付类型 1301-订单 1302-充值
     * @param orderNo: 订单编号
     * @return 本地交易记录对象
     */
    PaymentInfo savePaymentInfo(String paymentType, String orderNo);

    /**
     * 用户支付成功后，处理核心业务
     * @param transaction 微信交易对象
     */
    void updatePaymentInfo(Transaction transaction);
}
