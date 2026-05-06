package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface UserInfoService extends IService<UserInfo> {

    /**
     * 微信小程序一键登录
     *
     * @param code 小程序集成微信获取访问微信账户基本信息临时凭据，用于获取微信账号唯一标识
     * @return {"token":"用户登录成功令牌"}
     */
    Map<String, String> wxLogin(String code);

    /**
     * 获取当前登录用户信息
     * @param userId
     * @return
     */
    UserInfoVo getUserInfo(Long userId);

    /**
     * 用户信息修改
     * @param userId 用户ID
     * @param userInfoVo 用户信息VO
     */
    void updateUser(Long userId, UserInfoVo userInfoVo);

    /**
     * 查询指定用户某个专辑下声音购买状态
     * @param userId 用户ID
     * @param albumId 专辑ID
     * @param needCheckPayStateTrackIdList 待检查购买状态声音ID列表
     * @return {声音ID：购买状态}
     */
    Map<Long, Integer> userIsPaidTrack(Long userId, Long albumId, List<Long> needCheckPayStateTrackIdList);

    /**
     * 判断指定用户是否购买指定专辑
     * @param albumId
     * @return 购买状态：true:已购买专辑、 false:未购买专辑
     */
    Boolean isPaidAlbum(Long userId, Long albumId);

    /**
     * 根据专辑id+用户ID获取用户已购买声音id列表
     * @param albumId
     * @return
     */
    List<Long> findUserPaidTrackIdList(Long userId, Long albumId);
}
