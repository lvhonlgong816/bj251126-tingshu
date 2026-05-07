package com.atguigu.tingshu.order.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "订单管理")
@RestController
@RequestMapping("api/order")
@SuppressWarnings({"all"})
public class OrderInfoApiController {

	@Autowired
	private OrderInfoService orderInfoService;


	/**
	 * 订单结算（会员套餐、专辑、声音）
	 * @param tradeVo 交易vo信息 包含：购买项目类型、项目ID、购买声音数量
	 * @return 订单VO信息
	 */
	@GuiGuLogin
	@Operation(summary = "订单结算（会员套餐、专辑、声音）")
	@PostMapping("/orderInfo/trade")
	public Result<OrderInfoVo> trade(@RequestBody TradeVo tradeVo){
		//1.获取当前用户ID
		Long userId = AuthContextHolder.getUserId();
		//2.调用业务逻辑封装结果VO
		OrderInfoVo orderInfoVo  = orderInfoService.trade(userId, tradeVo);
		//3.响应订单VO
		return Result.ok(orderInfoVo);
	}


	/**
	 * 提交/结算订单（处理余额支付逻辑）
	 * @param orderInfoVo 订单vo信息
	 * @return {"orderNo":"本次订单保存后订单编号"} 用于后续对接微信支付或者展示订单详情
	 */
	@GuiGuLogin
	@Operation(summary = "提交/结算订单（处理余额支付逻辑）")
	@PostMapping("/orderInfo/submitOrder")
	public Result<Map<String, String>> submitOrder(@RequestBody OrderInfoVo orderInfoVo){
		//1.获取当前用户ID
		Long userId = AuthContextHolder.getUserId();
		//2.调用业务逻辑
		Map<String, String> map  = orderInfoService.submitOrder(userId, orderInfoVo);
		//3.响应订单编号
		return Result.ok(map);
	}
}

