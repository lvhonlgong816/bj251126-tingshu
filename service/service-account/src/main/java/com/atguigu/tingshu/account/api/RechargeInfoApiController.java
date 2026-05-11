package com.atguigu.tingshu.account.api;

import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "充值管理")
@RestController
@RequestMapping("api/account")
@SuppressWarnings({"all"})
public class RechargeInfoApiController {

	@Autowired
	private RechargeInfoService rechargeInfoService;


	/**
	 * 根据充值订单编号，查询充值记录
	 * @param orderNo
	 * @return
	 */
	@Operation(summary = "根据充值订单编号，查询充值记录")
	@GetMapping("/rechargeInfo/getRechargeInfo/{orderNo}")
	public Result<RechargeInfo> getRechargeInfo(@PathVariable String orderNo){
		RechargeInfo rechargeInfo = rechargeInfoService.getRechargeInfo(orderNo);
		return Result.ok(rechargeInfo);
	}


	/**
	 * 保存充值记录，返回充值订单编号用于对接微信支付
	 * @param rechargeInfoVo
	 * @return {orderNo:"充值订单编号"}
	 */
	@GuiGuLogin
	@Operation(summary = "保存充值记录，返回充值订单编号用于对接微信支付")
	@PostMapping("/rechargeInfo/submitRecharge")
	public Result<Map<String, String>> submitRecharge(@RequestBody RechargeInfoVo rechargeInfoVo){
		//1.获取当前用户ID
		Long userId = AuthContextHolder.getUserId();
		//2.调用业务逻辑
		Map<String, String> map = rechargeInfoService.submitRecharge(userId, rechargeInfoVo);
		//3.响应结果
		return Result.ok(map);
	}


	/**
	 * 支付成功后，修改充值状态以及完成充值
	 * @param orderNo
	 * @return
	 */
	@Operation(summary = "支付成功后，修改充值状态以及完成充值")
	@GetMapping("/rechargeInfo/rechargePaySuccess/{orderNo}")
	public Result rechargePaySuccess(@PathVariable String orderNo){
		rechargeInfoService.rechargePaySuccess(orderNo);
		return Result.ok();
	}

}

