package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface AlbumInfoService extends IService<AlbumInfo> {


    /**
     * 保存专辑信息
     * @param albumInfoVo 专辑VO
     * @param userId 用户ID
     */
    void saveAlbumInfo(AlbumInfoVo albumInfoVo, Long userId);

    public void saveAlbumInfoStat(Long albumId, String statType, int statNum);

    /**
     * 分页条件查询指定用户专辑列表
     * @param pageInfo 分页对象
     * @param albumInfoQuery 查询条件
     * @return 分页对象
     */
    IPage<AlbumListVo> findUserAlbumPageByUserId(IPage<AlbumListVo> pageInfo, AlbumInfoQuery albumInfoQuery);

    /**
     * 删除指定专辑
     * @param id 专辑ID
     * @return
     */
    void removeAlbumInfo(Long id);

    /**
     * 查询专辑信息（包含标签列表）
     * @param id
     * @return
     */
    AlbumInfo getAlbumInfo(Long id);

    /**
     * 修改专辑
     * @param id 专辑ID
     * @param albumInfoVo 修改专辑VO信息
     * @return
     */
    void updateAlbumInfo(Long id, AlbumInfoVo albumInfoVo);

    /**
     * 查询当前用户发布专辑列表
     * @param userId
     * @return
     */
    List<AlbumInfo> findUserAllAlbumList(Long userId);
}
