package com.atguigu.tingshu.payment.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.payment.service.WxPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "微信支付接口")
@RestController
@RequestMapping("api/payment")
@Slf4j
public class WxPayApiController {

    @Autowired
    private WxPayService wxPayService;

    /**
     * 对接微信支付，获取小程序拉取微信支付所需参数
     * @param paymentType 支付类型 1301：订单  1302：充值
     * @param orderNo 订单/充值订单编号
     * @return 拉起微信支付map
     */
    @Operation(summary = "对接微信支付，获取小程序拉取微信支付所需参数")
    @PostMapping("/wxPay/createJsapi/{paymentType}/{orderNo}")
    public Result<Map<String, String>> createJsapi(@PathVariable String paymentType, @PathVariable String orderNo){
        Map<String, String> map  = wxPayService.createJsapi(paymentType, orderNo);
        return Result.ok(map);
    }

    /**
     * 根据商户侧订单编号查询微信支付结果
     * @param orderNo
     * @return true:已支付 false:未支付
     */
    @Operation(summary = "根据商户侧订单编号查询微信支付结果")
    @GetMapping("/wxPay/queryPayStatus/{orderNo}")
    public Result<Boolean> queryPayStatus(@PathVariable String orderNo){
        Boolean flag = wxPayService.queryPayStatus(orderNo);
        return Result.ok(flag);
    }


    /**
     * 用户微信支付成功后，微信支付异步回调，告知商户用户支付结果
     * @param request 验签的信息包含在请求头；加密后数据在请求体中
     * @return
     */
    //@Operation(summary = "用户微信支付成功后，微信支付异步回调，告知商户用户支付结果")
    //@PostMapping("/wxPay/notify")
    public Map<String, String> wxPaySuccessNotify(HttpServletRequest request){
        Map<String, String> map = wxPayService.wxPaySuccessNotify(request);
        return map;
    }

}
