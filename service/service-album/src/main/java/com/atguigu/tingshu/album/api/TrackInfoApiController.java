package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.cache.GuiGuCache;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackStatVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "声音管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class TrackInfoApiController {

    @Autowired
    private TrackInfoService trackInfoService;

    @Autowired
    private VodService vodService;


    /**
     * 将音视频文件上传到腾讯云点播平台
     *
     * @param file
     * @return
     */
    @Operation(summary = "将音视频文件上传到腾讯云点播平台")
    @PostMapping("/trackInfo/uploadTrack")
    public Result<Map<String, String>> uploadTrack(@RequestParam("file") MultipartFile file) {
        Map<String, String> map = vodService.uploadTrack(file);
        return Result.ok(map);
    }


    /**
     * 该接口必须登录才能访问
     * 保存声音
     *
     * @param trackInfoVo 声音信息vo
     * @return
     */
    @GuiGuLogin
    @Operation(summary = "保存声音")
    @PostMapping("/trackInfo/saveTrackInfo")
    public Result saveTrackInfo(@Validated @RequestBody TrackInfoVo trackInfoVo) {
        //1.获取当前用户ID
        Long userId = AuthContextHolder.getUserId();
        //2.调用业务逻辑
        trackInfoService.saveTrackInfo(trackInfoVo, userId);
        return Result.ok();
    }


    /**
     * 当前接口必须才能访问
     * 条件分页查询当前用户声音列表
     *
     * @param page           页码
     * @param limit          页大小
     * @param trackInfoQuery 查询条件
     * @return MP分页对象
     */
    @GuiGuLogin
    @Operation(summary = "条件分页查询当前用户声音列表")
    @PostMapping("/trackInfo/findUserTrackPage/{page}/{limit}")
    public Result<Page<TrackListVo>> findUserTrackPage(
            @PathVariable Long page,
            @PathVariable Long limit,
            @RequestBody TrackInfoQuery trackInfoQuery
    ) {
        //1.获取用户ID
        Long userId = AuthContextHolder.getUserId();
        trackInfoQuery.setUserId(userId);
        //2.封装分页对象、查询对象
        Page<TrackListVo> pageInfo = new Page<>(page, limit);
        //3.调用业务层，分页获取数据
        pageInfo = trackInfoService.findUserTrackPage(pageInfo, trackInfoQuery);
        //4.响应结果
        return Result.ok(pageInfo);
    }


    /**
     * 根据声音ID查询声音信息
     * @param id
     * @return
     */
    @Operation(summary = "根据声音ID查询声音信息")
    @GetMapping("/trackInfo/getTrackInfo/{id}")
    @GuiGuCache(prefix = "track:info:")
    public Result<TrackInfo> getTrackInfo(@PathVariable Long id){
        TrackInfo trackInfo = trackInfoService.getById(id);
        return Result.ok(trackInfo);
    }


    /**
     * 修改声音信息
     * @param id 声音Id
     * @param trackInfoVo 声音信息VO
     * @return
     */
    @Operation(summary = "修改声音信息")
    @PutMapping("/trackInfo/updateTrackInfo/{id}")
    public Result updateTrackInfo(@PathVariable Long id,@Validated @RequestBody TrackInfoVo trackInfoVo){
        trackInfoService.updateTrackInfo(id, trackInfoVo);
        return Result.ok();
    }

    @Operation(summary = "删除声音")
    @DeleteMapping("/trackInfo/removeTrackInfo/{id}")
    public Result removeTrackInfo(@PathVariable Long id){
        trackInfoService.removeTrackInfo(id);
        return Result.ok();
    }


    /**
     * 该接口未登录，返回声音列表 如果用户已登录，根据当前用户身份、购买情况、专辑付费类型综合判断付费标识
     * 分页获取专辑声音列表（动态判断付费标识）
     * @param albumId
     * @param page
     * @param limit
     * @return
     */
    @GuiGuLogin(required = false)
    @Operation(summary = "分页获取专辑声音列表（动态判断付费标识）")
    @GetMapping("/trackInfo/findAlbumTrackPage/{albumId}/{page}/{limit}")
    public Result<Page<AlbumTrackListVo>> findAlbumTrackPage(
            @PathVariable Long albumId,
            @PathVariable Long page,
            @PathVariable Long limit
    ){
        //1.获取当前用户ID
        Long userId = AuthContextHolder.getUserId();
        //2.封装MP分页对象 页码、页大小
        Page<AlbumTrackListVo> pageInfo = new Page<>(page, limit);
        //3.调用业务层查询分页相关数据
        pageInfo = trackInfoService.findAlbumTrackPage(pageInfo, albumId, userId);
        //4.返回结果
        return Result.ok(pageInfo);
    }


    /**
     * 获取声音统计数值
     * @param trackId
     * @return
     */
    @Operation(summary = "获取声音统计数值")
    @GetMapping("/trackInfo/getTrackStatVo/{trackId}")
    public Result<TrackStatVo> getTrackStatVo(@PathVariable Long trackId){
        TrackStatVo vo = trackInfoService.getTrackStatVo(trackId);
        return Result.ok(vo);
    }


    /**
     * 以选择购买声音作为起始，基于未购买声音数量，返回分集购买列表
     * @param trackId 选择购买声音ID
     * @return [{name:"本集",price:0.1,trackCount:1},{name:"后10集",price:1,trackCount:10}..]
     */
    @GuiGuLogin
    @Operation(summary = "以选择购买声音作为起始，基于未购买声音数量，返回分集购买列表")
    @GetMapping("/trackInfo/findUserTrackPaidList/{trackId}")
    public Result<List<Map<String, Object>>> findFenJiPaidList(@PathVariable Long trackId){
        //1.获取当前用户ID
        Long userId = AuthContextHolder.getUserId();
        //2.调用业务层
        List<Map<String, Object>> list = trackInfoService.findFenJiPaidList(userId, trackId);
        //3.返回结果
        return Result.ok(list);
    }


    /**
     * 以用户选择声音作为起始，查询当前用户未购买声音列表，展示订单确认页
     * @param trackId
     * @param trackCount
     * @return
     */
    @GuiGuLogin
    @Operation(summary = "以用户选择声音作为起始，查询当前用户未购买声音列表，展示订单确认页")
    @GetMapping("/trackInfo/findPaidTrackInfoList/{trackId}/{trackCount}")
    public Result<List<TrackInfo>> findPaidTrackInfoList(@PathVariable Long trackId, @PathVariable Integer trackCount){
        //1.获取当前用户ID
        Long userId = AuthContextHolder.getUserId();
        //2.调用业务层
        List<TrackInfo> list = trackInfoService.findPaidTrackInfoList(userId, trackId, trackCount);
        //3.返回结果
        return Result.ok(list);
    }

}

