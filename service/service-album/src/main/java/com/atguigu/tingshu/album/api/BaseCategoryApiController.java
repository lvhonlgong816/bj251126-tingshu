package com.atguigu.tingshu.album.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.aspectj.weaver.ast.Var;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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


	public static void main(String[] args) {
		//fastjson JSON工具包
		//List<JSONObject> list2 = new ArrayList<>();
		//list2.add(new JSONObject(Map.of("categoryId",10, "categoryName", "男频")));
		//JSONObject level1_1 = new JSONObject(Map.of("categoryId",1, "categoryName", "有声书", "categoryChild",list2));
		//
		//JSONObject level1_2 = new JSONObject(Map.of("categoryId",2, "categoryName", "娱乐"));
		//
		//List<JSONObject> list = new ArrayList<>();
		//list.add(level1_1);
		//list.add(level1_2);
		//
		//System.out.println(list);

		//JSONObject level1_1 = new JSONObject();
		//level1_1.put("a", "b");
		//System.out.println(level1_1);
	}
}

