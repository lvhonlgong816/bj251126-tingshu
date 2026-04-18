package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "专辑管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class AlbumInfoApiController {

	@Autowired
	private AlbumInfoService albumInfoService;


	/**
	 * TODO 该接口必须登录才能访问
	 * 参数校验框架：@Validated 对请求体中VO中使用校验注解属性进行校验，底层基于AOP（前置通知）调用controller之前就会校验
	 * 	校验成功才会调用controller方法，校验失败抛出异常，抛出的异常MethodArgumentNotValidException会进入全局异常处理类，返回给前端
	 * @param albumInfoVo
	 * @return
	 */
	@Operation(summary = "保存专辑（内容创作者或运营管理人员）")
	@PostMapping("/albumInfo/saveAlbumInfo")
	public Result saveAlbumInfo(@Validated @RequestBody AlbumInfoVo albumInfoVo) {
		//1.从ThreadLocal获取当前登录用户ID
		Long userId = AuthContextHolder.getUserId();
		//2.调用service方法保存
		albumInfoService.saveAlbumInfo(albumInfoVo, userId);
		//3.返回结果
		return Result.ok();
	}


	/**
	 * TODO 该接口必须登录才能访问
	 * 分页查询当前用户专辑列表
	 * @param page 页码
	 * @param limit 页大小
	 * @return 分页对象 列表中专辑信息（包含统计信息）
	 */
	@Operation(summary = "分页查询当前用户专辑列表")
	@PostMapping("/albumInfo/findUserAlbumPage/{page}/{limit}")
	public Result<IPage<AlbumListVo>> findUserAlbumPageByUserId(@PathVariable Long page,
																@PathVariable Long limit,
																@RequestBody AlbumInfoQuery albumInfoQuery
	) {
		//1.从ThreadLocal获取当前登录用户ID
		Long userId = AuthContextHolder.getUserId();
		//2.创建Ipage分页对象，封装页码、页大小
		IPage<AlbumListVo> pageInfo = new Page<>(page, limit);

		//3.调用service方法完成条件分页查询,其他分页数据：总记录数、总页数、当前页数据在持久层查询DB后封装
		albumInfoQuery.setUserId(userId);
		pageInfo = albumInfoService.findUserAlbumPageByUserId(pageInfo, albumInfoQuery);
		//4.将分页结果对象响应
		return Result.ok(pageInfo);

	}

}

