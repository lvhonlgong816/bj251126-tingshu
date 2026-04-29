package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.common.util.MongoUtil;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;

import java.math.BigDecimal;
import java.util.Map;

public interface UserListenProcessService {

    /**
     * 获取用户某个声音播放进度
     * @param userId
     * @param trackId
     * @return
     */
    BigDecimal getTrackBreakSecond(Long userId, Long trackId);

    String getCollectionName(MongoUtil.MongoCollectionEnum collectionEnum, Long userId);

    /**
     * 更新声音播放进度
     * @param userId
     * @param userListenProcessVo
     */
    void updateListenProcess(Long userId, UserListenProcessVo userListenProcessVo);

    /**
     * 查询当前用户最近播放专辑/声音
     * @return {"albumId“："","trackId":""}
     */
    Map<String, Long> getLatelyTrack(Long userId);
}
