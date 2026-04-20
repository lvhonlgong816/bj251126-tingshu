package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface TrackInfoService extends IService<TrackInfo> {

    /**
     * 保存声音
     * @param trackInfoVo  声音信息vo
     * @param userId 用户ID
     */
    void saveTrackInfo(TrackInfoVo trackInfoVo, Long userId);

    /**
     * 保存声音统计信息
     * @param trackId 声音ID
     * @param statType 统计类型
     * @param statNum 统计数值
     */
    void saveTrackStat(Long trackId, String statType, int statNum);

    /**
     * 条件分页查询当前用户声音列表
     * @param pageInfo 分页对象
     * @param trackInfoQuery 查询条件
     * @return 分页对象
     */
    Page<TrackListVo> findUserTrackPage(Page<TrackListVo> pageInfo, TrackInfoQuery trackInfoQuery);

    /**
     * 修改声音信息
     * @param id 声音Id
     * @param trackInfoVo 声音信息VO
     * @return
     */
    void updateTrackInfo(Long id, TrackInfoVo trackInfoVo);

    /**
     * 删除声音
     * @param id 声音ID
     */
    void removeTrackInfo(Long id);
}
