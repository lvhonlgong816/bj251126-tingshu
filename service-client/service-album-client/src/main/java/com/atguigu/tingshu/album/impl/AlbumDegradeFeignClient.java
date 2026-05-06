package com.atguigu.tingshu.album.impl;


import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.*;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class AlbumDegradeFeignClient implements AlbumFeignClient {


    @Override
    public Result<AlbumInfo> getAlbumInfo(Long id) {
        log.error("[专辑服务]提供远程调用接口getAlbumInfo执行了服务降级");
        return null;
    }

    @Override
    public Result<BaseCategoryView> getCategoryView(Long category3Id) {
        log.error("[专辑服务]提供远程调用接口getCategoryView执行了服务降级");
        return null;
    }

    @Override
    public Result<List<BaseCategory3>> findTop7BaseCategory3(Long category1Id) {
        log.error("[专辑服务]提供远程调用接口findTop7BaseCategory3执行了服务降级");
        return null;
    }

    @Override
    public Result<AlbumStatVo> getAlbumStatVo(Long albumId) {
        log.error("[专辑服务]提供远程调用接口getAlbumStatVo执行了服务降级");
        return null;
    }

    @Override
    public Result<List<BaseCategory1>> findAllCategory1() {
        log.error("[专辑服务]提供远程调用接口findAllCategory1执行了服务降级");
        return null;
    }

    @Override
    public Result<List<TrackInfo>> findPaidTrackInfoList(Long trackId, Integer trackCount) {
        log.error("[专辑服务]提供远程调用接口findPaidTrackInfoList执行了服务降级");
        return null;
    }
}
