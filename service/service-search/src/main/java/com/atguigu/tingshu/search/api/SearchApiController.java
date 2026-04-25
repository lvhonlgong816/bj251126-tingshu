package com.atguigu.tingshu.search.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "搜索专辑管理")
@RestController
@RequestMapping("api/search")
@SuppressWarnings({"all"})
public class SearchApiController {

    @Autowired
    private SearchService searchService;


    /**
     * 手动上架指定专辑到索引库
     * @param albumId
     * @return
     */
    @Operation(summary = "手动上架指定专辑")
    @GetMapping("/albumInfo/upperAlbum/{albumId}")
    public Result upperAlbum(@PathVariable Long albumId){
        searchService.upperAlbum(albumId);
        return Result.ok();
    }

    /**
     * 手动从索引库下架指定专辑
     * @param albumId
     * @return
     */
    @GetMapping("/albumInfo/lowerAlbum/{albumId}")
    @Operation(summary = "手动从索引库下架指定专辑")
    public Result lowerAlbum(@PathVariable Long albumId){
        searchService.lowerAlbum(albumId);
        return Result.ok();
    }


    /**
     * 站内搜索
     * @param albumIndexQuery
     * @return
     */
    @Operation(summary = "站内搜索")
    @PostMapping("/albumInfo")
    public Result<AlbumSearchResponseVo> search(@RequestBody AlbumIndexQuery albumIndexQuery){
        AlbumSearchResponseVo vo = searchService.search(albumIndexQuery);
        return Result.ok(vo);
    }


    /**
     * 查询置顶三级分类包含热门专辑列表
     * @param category1Id
     * @return
     */
    @Operation(summary = "查询置顶三级分类包含热门专辑列表")
    @GetMapping("/albumInfo/channel/{category1Id}")
    public Result<List<Map<String, Object>>> channel(@PathVariable Long category1Id){
        List<Map<String, Object>>  list = searchService.channel(category1Id);
        return Result.ok(list);
    }
}

