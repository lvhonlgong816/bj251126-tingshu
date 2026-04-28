package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TrackInfoMapper extends BaseMapper<TrackInfo> {


    /**
     * 条件分页查询当前用户声音列表
     *
     * @param pageInfo       分页对象
     * @param trackInfoQuery 查询条件
     * @return 分页对象
     */
    Page<TrackListVo> findUserTrackPage(Page<TrackListVo> pageInfo, @Param("vo") TrackInfoQuery trackInfoQuery);

    /**
     * 分页获取专辑声音列表（动态判断付费标识）
     * @param pageInfo 分页对象
     * @param albumId 专辑ID
     * @return
     */
    Page<AlbumTrackListVo> findAlbumTrackPage(Page<AlbumTrackListVo> pageInfo, @Param("albumId") Long albumId);
}
