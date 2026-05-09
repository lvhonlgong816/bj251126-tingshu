package com.atguigu.tingshu.payment.service;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface WxPayService {

    /**
     * 对接微信支付，获取小程序拉取微信支付所需参数
     * @param paymentType 支付类型 1301：订单  1302：充值
     * @param orderNo 订单/充值订单编号
     * @return 拉起微信支付map
     */
    Map<String, String> createJsapi(String paymentType, String orderNo);

    /**
     * 根据商户侧订单编号查询微信支付结果
     * @param orderNo
     * @return true:已支付 false:未支付
     */
    Boolean queryPayStatus(String orderNo);

    /**
     * 用户微信支付成功后，微信支付异步回调，告知商户用户支付结果
     * @param request 验签的信息包含在请求头；加密后数据在请求体中
     * @return
     */
    Map<String, String> wxPaySuccessNotify(HttpServletRequest request);
}
