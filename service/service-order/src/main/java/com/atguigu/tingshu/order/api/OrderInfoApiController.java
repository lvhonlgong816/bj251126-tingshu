package com.atguigu.tingshu.order.api;

import cn.hutool.db.sql.Order;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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


	/**
	 * 根据订单编号查询订单详情（包含订单明细列表，减免列表）
	 * @param orderNo
	 * @return
	 */
	@Operation(summary = "根据订单编号查询订单详情（包含订单明细列表，减免列表）")
	@GetMapping("/orderInfo/getOrderInfo/{orderNo}")
	public Result<OrderInfo> getOrderInfo(@PathVariable String orderNo){
		OrderInfo orderInfo = orderInfoService.getOrderInfo(orderNo);
		return Result.ok(orderInfo);
	}


	/**
	 * 分页查询订单(包含订单明细、减免列表)
	 * @param page
	 * @param limit
	 * @return
	 */
	@GuiGuLogin
	@Operation(summary = "分页查询订单")
	@GetMapping("/orderInfo/findUserPage/{page}/{limit}")
	public Result<Page<OrderInfo>> findUserPage(@PathVariable Long page, @PathVariable Long limit){
		//1.获取当前用户ID
		Long userId = AuthContextHolder.getUserId();
		//2.创建分页对象
		Page<OrderInfo> pageInfo = new Page<>(page, limit);

		//3.业务逻辑执行
		pageInfo = orderInfoService.findUserPage(pageInfo, userId);

		//4.返回分页对象
		return Result.ok(pageInfo);
	}

	/**
	 * 用户支付成功后，修改订单状态，虚拟物品发货
	 * @param orderNo
	 * @return
	 */
	@Operation(summary = "用户支付成功后，修改订单状态，虚拟物品发货")
	@GetMapping("/orderInfo/orderPaySuccess/{orderNo}")
	public Result orderPaySuccess(@PathVariable String orderNo){
		orderInfoService.orderPaySuccess(orderNo);
		return Result.ok();
	}
}

