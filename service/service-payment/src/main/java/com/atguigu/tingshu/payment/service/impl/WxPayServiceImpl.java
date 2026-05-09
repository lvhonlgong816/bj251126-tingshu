package com.atguigu.tingshu.payment.service.impl;

import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.atguigu.tingshu.payment.config.WxPayV3Config;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.atguigu.tingshu.payment.service.WxPayService;
import com.atguigu.tingshu.payment.util.PayUtil;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.model.TransactionAmount;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {

    @Autowired
    private PaymentInfoService paymentInfoService;

    @Autowired
    private RSAAutoCertificateConfig rsaAutoCertificateConfig;

    @Autowired
    private WxPayV3Config wxPayV3Config;

    /**
     * 对接微信支付，获取小程序拉取微信支付所需参数
     *
     * @param paymentType 支付类型 1301：订单  1302：充值
     * @param orderNo     订单/充值订单编号
     * @return 拉起微信支付map
     */
    @Override
    public Map<String, String> createJsapi(String paymentType, String orderNo) {
        try {
            //1.保存本地交易记录 再次之前 订单记录或充值记录已经保存
            PaymentInfo paymentInfo = paymentInfoService.savePaymentInfo(paymentType, orderNo);
            String paymentStatus = paymentInfo.getPaymentStatus();
            if (SystemConstant.PAYMENT_STATUS_PAID.equals(paymentStatus)) {
                throw new GuiguException(500, "本地交易记录已支付");
            }
            //2.对接微信支付获取小程序唤起微信支付所需要参数
            //2.1 创建小程序支付业务对象
            JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(rsaAutoCertificateConfig).build();
            //2.2 创建预支付请求对象
            PrepayRequest prepayRequest = new PrepayRequest();
            //2.2.1 为正式发布前，需要手动指定付款者信息（必须是应用开发者用户）
            Payer payer = new Payer();
            payer.setOpenid("odo3j4qp-wC3HVq9Z_D9C0cOr0Zs");
            prepayRequest.setPayer(payer);
            //2.2.2 设置支付金额、应用ID、商户ID、商品描述、回调地址、商户侧订单编号
            Amount amount = new Amount();
            amount.setTotal(1); //TODO 测试采用1分
            prepayRequest.setAmount(amount);
            prepayRequest.setAppid(wxPayV3Config.getAppid());
            prepayRequest.setMchid(wxPayV3Config.getMerchantId());
            prepayRequest.setNotifyUrl(wxPayV3Config.getNotifyUrl());
            prepayRequest.setDescription(paymentInfo.getContent());
            prepayRequest.setOutTradeNo(orderNo);
            //.3 对接微信支付
            PrepayWithRequestPaymentResponse response = service.prepayWithRequestPayment(prepayRequest);
            if (response != null) {
                String timeStamp = response.getTimeStamp();
                String nonceStr = response.getNonceStr();
                String packageVal = response.getPackageVal();
                String signType = response.getSignType();
                String paySign = response.getPaySign();
                Map<String, String> map = new HashMap<>();
                map.put("timeStamp", timeStamp);
                map.put("nonceStr", nonceStr);
                map.put("package", packageVal);
                map.put("signType", signType);
                map.put("paySign", paySign);
                return map;
            }
        } catch (Exception e) {
            log.error("对接微信支付异常", e);
            throw new RuntimeException(e);
        }
        return Map.of();
    }

    /**
     * 根据商户侧订单编号查询微信支付结果
     *
     * @param orderNo
     * @return true:已支付 false:未支付
     */
    @Override
    public Boolean queryPayStatus(String orderNo) {
       /* //1. 创建小程序支付业务对象
        JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(rsaAutoCertificateConfig).build();
        //2. 构建查询支付请求对象
        QueryOrderByOutTradeNoRequest reqeust = new QueryOrderByOutTradeNoRequest();
        reqeust.setMchid(wxPayV3Config.getMerchantId());
        reqeust.setOutTradeNo(orderNo);
        //3. 执行查询
        Transaction transaction = service.queryOrderByOutTradeNo(reqeust);
        if (transaction != null) {
            //3.1 验证支付状态
            Transaction.TradeStateEnum tradeState = transaction.getTradeState();
            if (Transaction.TradeStateEnum.SUCCESS == tradeState) {
                //3.2 验证实付金额
                TransactionAmount amount = transaction.getAmount();
                Integer payerTotal = amount.getPayerTotal();
                //3.3 根据订单编号查询本地交易记录中得到 应付金额
                if (payerTotal.intValue() == 1) {
                    return true;
                }
            }
        }
        return false;*/
        //模拟 用户已付款成功 直接处理核心业务
        Transaction transaction = new Transaction();
        transaction.setTransactionId("wx"+ IdUtil.getSnowflakeNextId());
        transaction.setOutTradeNo(orderNo);
        paymentInfoService.updatePaymentInfo(transaction);
        return true;
    }

    /**
     * 用户微信支付成功后，微信支付异步回调，告知商户用户支付结果
     *
     * @param request 验签的信息包含在请求头；加密后数据在请求体中
     * @return
     */
    @Override
    public Map<String, String> wxPaySuccessNotify(HttpServletRequest request) {
        //1.验签 防止出现虚假通知
        //1.1 从回调报文的HTTP请求头中会获取签名信息
        String signature = request.getHeader("Wechatpay-Signature");
        String serial = request.getHeader("Wechatpay-Serial");
        String nonce = request.getHeader("Wechatpay-Nonce");
        String timestamp = request.getHeader("Wechatpay-Timestamp");
        String signaureType = request.getHeader("Wechatpay-Signature-Type");
        log.info("签名：{}，序列号：{}，随机数：{}，时间戳：{}，签名类型：{}", signature, serial, nonce, timestamp, signaureType);
        //1.2 获取请求体参数
        String requestBody = PayUtil.readData(request);
        //1.3 构造 RequestParam 请求参数对象
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(serial)
                .nonce(nonce)
                .signature(signature)
                .timestamp(timestamp)
                .body(requestBody)
                .build();
        //1.4 初始化 NotificationParser 通知解析器对象
        NotificationParser parser = new NotificationParser(rsaAutoCertificateConfig);
        //1.5 验签、解密并转换成 Transaction交易对象
        Transaction transaction = parser.parse(requestParam, Transaction.class);

        if (transaction != null) {
            //2.业务校验：校验支付状态&校验支付金额&幂等性处理
            Transaction.TradeStateEnum tradeState = transaction.getTradeState();
            if (Transaction.TradeStateEnum.SUCCESS == tradeState
                    && transaction.getAmount().getPayerTotal().intValue() == 1) {
                //2.1 幂等性处理 找出交易对象中唯一标识 微信支付侧订单的唯一标识。
                String transactionId = transaction.getTransactionId();
                //2.2 采用Redis set k v nx ex 将唯一标识存入Redis
                String key = "payment:notify:" + transactionId;
                Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, transactionId, 25, TimeUnit.HOURS);
                if (flag) {
                    try {
                        //3.TODO 核心业务处理：本地交易记录、订单状态包含权益发放、充值状态包含余额充值
                        paymentInfoService.updatePaymentInfo(transaction);
                    } catch (Exception e) {
                        redisTemplate.delete(key);
                        throw new RuntimeException(e);
                    }
                    return Map.of("code", "SUCCESS", "message", "成功");
                }
            }

        }
        return Map.of();
    }

    @Autowired
    private RedisTemplate redisTemplate;
}
