package com.atguigu.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.common.cache.GuiGuCache;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.model.user.UserPaidAlbum;
import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.mapper.UserPaidAlbumMapper;
import com.atguigu.tingshu.user.mapper.UserPaidTrackMapper;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Autowired
    private WxMaService wxMaService;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;

    /**
     * 微信小程序一键登录
     *
     * @param code 小程序集成微信获取访问微信账户基本信息临时凭据，用于获取微信账号唯一标识
     * @return {"token":"用户登录成功令牌"}
     */
    @Override
    public Map<String, String> wxLogin(String code) {
        try {
            //1.调用微信接口获取微信账户唯一标识 appid + secret + code 调用微信服务端
            WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
            String wxOpenId = sessionInfo.getOpenid();

            //2.根据微信账户唯一标识 判断是否存在
            UserInfo userInfo = userInfoMapper.selectOne(
                    new LambdaQueryWrapper<UserInfo>()
                            .eq(UserInfo::getWxOpenId, wxOpenId)
            );

            //3.如果首次登录，将微信唯一标识关联到自定义用户记录且新增，同时为用户新增账户记录
            if (userInfo == null) {
                //3.1 新增用户记录关联微信账户唯一标识
                userInfo = new UserInfo();
                userInfo.setWxOpenId(wxOpenId);
                userInfo.setAvatarUrl("http://192.168.200.6:9000/tingshu/2026-04-20/3dcb2f53a4d14b3db5638f689f573162.png");
                userInfo.setNickname("听友" + IdUtil.nanoId());
                userInfoMapper.insert(userInfo);
                //3.2 TODO 采用RabbitMQ异步新增对应账户记录
                //3.2.1 准备需要发送业务数据 Map封装或者Vo对象（必须实现序列化接口）
                HashMap<String, Object> msgData = new HashMap<>();
                msgData.put("userId", userInfo.getId());
                msgData.put("title", "新用户专项体验金");
                msgData.put("amount", new BigDecimal("100"));
                msgData.put("orderNo", "zs" + IdUtil.getSnowflakeNextId());
                //3.2.2 调用生产者发送消息工具方法发送消息
                rabbitService.sendMessage(MqConst.EXCHANGE_USER, MqConst.ROUTING_USER_REGISTER, msgData);
            }

            //4.基于用户基本信息生成令牌，将用户存入Redis
            //4.1 为用户生成令牌 UUID方式
            String token = IdUtil.randomUUID();
            //4.2 构建用户登录key 形式：前缀:token
            String loginKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;
            //4.3 存入Redis key:登录key value:用户基本信息：UserInfoVo 存7天有效期
            UserInfoVo userInfoVo = BeanUtil.copyProperties(userInfo, UserInfoVo.class);
            redisTemplate.opsForValue().set(loginKey, userInfoVo, RedisConstant.USER_LOGIN_KEY_TIMEOUT, TimeUnit.SECONDS);
            //5.返回令牌
            return Map.of("token", token);
        } catch (WxErrorException e) {
            log.error("微信登录异常", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取当前登录用户信息
     *
     * @param userId
     * @return
     */
    @Override
    @GuiGuCache(prefix = "user:info:")
    public UserInfoVo getUserInfo(Long userId) {
        UserInfo userInfo = this.getById(userId);
        if (userInfo != null) {
            return BeanUtil.copyProperties(userInfo, UserInfoVo.class);
        }
        return null;
    }

    /**
     * 用户信息修改
     *
     * @param userId     用户ID
     * @param userInfoVo 用户信息VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long userId, UserInfoVo userInfoVo) {
        //1.只允许修改昵称、头像
        UserInfo userInfo = new UserInfo();
        userInfo.setId(userId);
        userInfo.setNickname(userInfoVo.getNickname());
        userInfo.setAvatarUrl(userInfoVo.getAvatarUrl());
        //2.修改
        userInfoMapper.updateById(userInfo);
    }

    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;

    @Autowired
    private UserPaidTrackMapper userPaidTrackMapper;

    /**
     * 查询指定用户某个专辑下声音购买状态
     *
     * @param userId                       用户ID
     * @param albumId                      专辑ID
     * @param needCheckPayStateTrackIdList 待检查购买状态声音ID列表
     * @return {声音ID：购买状态}
     */
    @Override
    public Map<Long, Integer> userIsPaidTrack(Long userId, Long albumId, List<Long> needCheckPayStateTrackIdList) {
        HashMap<Long, Integer> map = new HashMap<>();
        //1.根据用户ID+专辑ID查询专辑购买记录，如果存在购买记录 则购买状态直接返回1
        Long count = userPaidAlbumMapper.selectCount(
                new LambdaQueryWrapper<UserPaidAlbum>()
                        .eq(UserPaidAlbum::getUserId, userId)
                        .eq(UserPaidAlbum::getAlbumId, albumId)
        );
        if (count > 0) {
            //遍历待检查声音ID列表 将每个声音购买状态设置为1 返回即可
            for (Long trackId : needCheckPayStateTrackIdList) {
                map.put(trackId, 1);
            }
            return map;
        }

        //2.根据用户ID+专辑ID查询声音购买记录
        List<UserPaidTrack> userPaidTrackList = userPaidTrackMapper.selectList(
                new LambdaQueryWrapper<UserPaidTrack>()
                        .eq(UserPaidTrack::getUserId, userId)
                        .eq(UserPaidTrack::getAlbumId, albumId)
                        .select(UserPaidTrack::getTrackId)
        );

        //2.1 如果不存在声音购买记录 则购买状态直接返回0
        if (CollUtil.isEmpty(userPaidTrackList)) {
            //遍历待检查声音ID列表 将每个声音购买状态设置为0 返回即可
            for (Long trackId : needCheckPayStateTrackIdList) {
                map.put(trackId, 0);
            }
            return map;
        }
        //2.2 如果存在声音购买记录，已购买声音设置1 未购买设置为0
        List<Long> userPaidTrackIdList =
                userPaidTrackList.stream().map(UserPaidTrack::getTrackId).collect(Collectors.toList());
        //遍历待检查声音ID列表 如果待检查声音ID出现在已购声音列表中 将购买状态设置1 反之 设置0
        for (Long trackId : needCheckPayStateTrackIdList) {
            if (userPaidTrackIdList.contains(trackId)) {
                map.put(trackId, 1);
            } else {
                map.put(trackId, 0);
            }
        }
        return map;
    }
}
