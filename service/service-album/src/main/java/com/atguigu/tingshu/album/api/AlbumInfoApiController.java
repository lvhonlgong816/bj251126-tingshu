package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "专辑管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class AlbumInfoApiController {

	@Autowired
	private AlbumInfoService albumInfoService;


	/**
	 * 该接口必须登录才能访问
	 * 参数校验框架：@Validated 对请求体中VO中使用校验注解属性进行校验，底层基于AOP（前置通知）调用controller之前就会校验
	 * 	校验成功才会调用controller方法，校验失败抛出异常，抛出的异常MethodArgumentNotValidException会进入全局异常处理类，返回给前端
	 * @param albumInfoVo
	 * @return
	 */
	@GuiGuLogin(required = true)  //调用该接口前必须得登录状态（校验），并且将用户ID设置到ThreadLocal中
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
	 * 该接口必须登录才能访问
	 * 分页查询当前用户专辑列表
	 * @param page 页码
	 * @param limit 页大小
	 * @return 分页对象 列表中专辑信息（包含统计信息）
	 */
	@GuiGuLogin(required = true)
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


	/**
	 * 查询当前用户发布专辑列表
	 * @return
	 */
	@Operation(summary = "查询当前用户发布专辑列表")
	@GetMapping("/albumInfo/findUserAllAlbumList")
	public Result<List<AlbumInfo>> findUserAllAlbumList(){
		//1.从ThreadLocal中获取用户ID
		Long userId = AuthContextHolder.getUserId();
		//2.调用业务层获取专辑列表
		List<AlbumInfo> list = albumInfoService.findUserAllAlbumList(userId);
		//3.响应数据
		return Result.ok(list);

	}


	/**
	 * 删除指定专辑
	 * @param id 专辑ID
	 * @return
	 */
	@Operation(summary = "删除专辑")
	@DeleteMapping("/albumInfo/removeAlbumInfo/{id}")
	public Result removeAlbumInfo(@PathVariable Long id){
		albumInfoService.removeAlbumInfo(id);
		return Result.ok();
	}

	/**
	 * 查询专辑信息（包含标签列表）
	 * @param id
	 * @return
	 */
	@Operation(summary = "查询专辑信息（包含标签列表）")
	@GetMapping("/albumInfo/getAlbumInfo/{id}")
	public Result<AlbumInfo> getAlbumInfo(@PathVariable Long id){
		AlbumInfo albumInfo = albumInfoService.getAlbumInfoFromDB(id);
		return Result.ok(albumInfo);
	}


	/**
	 * 修改专辑
	 * @param id 专辑ID
	 * @param albumInfoVo 修改专辑VO信息
	 * @return
	 */
	@Operation(summary = "修改专辑")
	@PutMapping("/albumInfo/updateAlbumInfo/{id}")
	public Result updateAlbumInfo(@PathVariable Long id, @Validated @RequestBody AlbumInfoVo albumInfoVo){
		albumInfoService.updateAlbumInfo(id, albumInfoVo);
		return Result.ok();
	}


	/**
	 * 根据专辑ID查询统计信息
	 * @param albumId 专辑ID
	 * @return 统计VO对象
	 */
	@Operation(summary = "根据专辑ID查询统计信息")
	@GetMapping("/albumInfo/getAlbumStatVo/{albumId}")
	public Result<AlbumStatVo> getAlbumStatVo(@PathVariable Long albumId){
		AlbumStatVo vo = albumInfoService.getAlbumStatVo(albumId);
		return Result.ok(vo);
	}


	/**
	 * 在项目维护期间，重建/扩容布隆过滤器
	 */
	@Operation(summary = "重建布隆过滤器")
	@GetMapping("/bloomFilter/rebuild")
	public void rebuildBloomFilter(){
		albumInfoService.rebuildBloomFilter();
	}
}

