package com.atguigu.tingshu.album.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.BaseAttribute;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.aspectj.weaver.ast.Var;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;


@Tag(name = "分类管理")
@RestController
@RequestMapping(value="/api/album")
@SuppressWarnings({"all"})
public class BaseCategoryApiController {

	@Autowired
	private BaseCategoryService baseCategoryService;


	/**
	 * 查询所有1,2,3级分类列表
	 * @return
	 */
	@Operation(summary = "查询所有1,2,3级分类列表")
	@GetMapping("/category/getBaseCategoryList")
	public Result<List<JSONObject>> getBaseCategoryList(){
		//1.调用业务层返回所有1,2,3级分类
		List<JSONObject> list = baseCategoryService.getBaseCategoryList();
		//2.响应结果给客户端
		return Result.ok(list);
	}


	/**
	 * 根据1级分类ID查询标签列表（包含标签值列表）
	 * @param category1Id 1级分类ID
	 * @return 标签列表
	 */
	@Operation(summary = "根据1级分类ID查询标签列表（包含标签值列表）")
	@GetMapping("/category/findAttribute/{category1Id}")
	public Result<List<BaseAttribute>> findAttribute(@PathVariable Long category1Id){
		List<BaseAttribute> list = baseCategoryService.findAttribute(category1Id);
		return Result.ok(list);
	}

}

