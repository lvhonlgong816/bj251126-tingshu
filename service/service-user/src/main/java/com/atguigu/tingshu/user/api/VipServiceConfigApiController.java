package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.cache.GuiGuCache;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.user.service.VipServiceConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "VIP服务配置管理接口")
@RestController
@RequestMapping("api/user")
@SuppressWarnings({"all"})
public class VipServiceConfigApiController {

	@Autowired
	private VipServiceConfigService vipServiceConfigService;


	/**
	 * 加载VIP套餐列表
	 * @return
	 */
	@GuiGuCache(prefix = "vipconfig:all:")
	@Operation(summary = "加载VIP套餐列表")
	@GetMapping("/vipServiceConfig/findAll")
	public Result<List<VipServiceConfig>> findAll(){
		List<VipServiceConfig> list = vipServiceConfigService.list();
		return Result.ok(list);
	}


	/**
	 * 获取指定VIP套餐信息
	 * @param id
	 * @return
	 */
	@GuiGuCache(prefix = "vipconfig:info:")
	@Operation(summary = "获取指定VIP套餐信息")
	@GetMapping("/vipServiceConfig/getVipServiceConfig/{id}")
	public Result<VipServiceConfig> getVipServiceConfig(@PathVariable Long id){
		VipServiceConfig vipServiceConfig = vipServiceConfigService.getById(id);
		return Result.ok(vipServiceConfig);
	}
}

