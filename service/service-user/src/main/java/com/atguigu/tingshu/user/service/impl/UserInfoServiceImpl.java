package com.atguigu.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.cache.GuiGuCache;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.user.*;
import com.atguigu.tingshu.user.mapper.*;
import com.atguigu.tingshu.user.pattern.DeliveryStrategy;
import com.atguigu.tingshu.user.pattern.factory.DeliveryStrategyFactory;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
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

    @Autowired
    private AlbumFeignClient albumFeignClient;

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

    /**
     * 判断指定用户是否购买指定专辑
     *
     * @param albumId
     * @return 购买状态：true:已购买专辑、 false:未购买专辑
     */
    @Override
    public Boolean isPaidAlbum(Long userId, Long albumId) {
        Long count = userPaidAlbumMapper.selectCount(
                new LambdaQueryWrapper<UserPaidAlbum>()
                        .eq(UserPaidAlbum::getAlbumId, albumId)
                        .eq(UserPaidAlbum::getUserId, userId)
        );
        return count > 0;
    }

    /**
     * 根据专辑id+用户ID获取用户已购买声音id列表
     *
     * @param albumId
     * @return
     */
    @Override
    public List<Long> findUserPaidTrackIdList(Long userId, Long albumId) {
        List<UserPaidTrack> userPaidTrackList = userPaidTrackMapper.selectList(
                new LambdaQueryWrapper<UserPaidTrack>()
                        .eq(UserPaidTrack::getAlbumId, albumId)
                        .eq(UserPaidTrack::getUserId, userId)
                        .select(UserPaidTrack::getTrackId)
        );
        if (CollUtil.isNotEmpty(userPaidTrackList)) {
            List<Long> paidTrackIdList = userPaidTrackList.stream()
                    .map(UserPaidTrack::getTrackId)
                    .collect(Collectors.toList());
            return paidTrackIdList;
        }
        return List.of();
    }

    @Autowired
    private UserVipServiceMapper userVipServiceMapper;

    @Autowired
    private DeliveryStrategyFactory factory;

    /**
     * 支付成功后权益方法（虚拟物品发货）
     *
     * @param userPaidRecordVo
     * @return
     */
    @Override
    public void savePaidRecord(UserPaidRecordVo userPaidRecordVo) {
        //项目类型: 1001-专辑 1002-声音 1003-vip会员
        String itemType = userPaidRecordVo.getItemType();
        DeliveryStrategy deliveryStrategy = factory.getDeliveryStrategy(itemType);
        deliveryStrategy.delivery(userPaidRecordVo);


        //if (SystemConstant.ORDER_ITEM_TYPE_ALBUM.equals(itemType)) {
        //    //1.处理购买类型是专辑
        //    //1.1 根据订单编号查询专辑购买记录，验证这比订单是否重复处理
        //    Long count = userPaidAlbumMapper.selectCount(
        //            new LambdaQueryWrapper<UserPaidAlbum>()
        //                    .eq(UserPaidAlbum::getOrderNo, userPaidRecordVo.getOrderNo())
        //    );
        //    if (count == 0) {
        //        //1.2 新增已购专辑记录（等同于发放权益）
        //        UserPaidAlbum userPaidAlbum = new UserPaidAlbum();
        //        userPaidAlbum.setUserId(userPaidRecordVo.getUserId());
        //        userPaidAlbum.setOrderNo(userPaidRecordVo.getOrderNo());
        //        userPaidAlbum.setAlbumId(userPaidRecordVo.getItemIdList().get(0));
        //        userPaidAlbumMapper.insert(userPaidAlbum);
        //    }
        //} else if (SystemConstant.ORDER_ITEM_TYPE_TRACK.equals(itemType)) {
        //    //2.处理购买类型是声音
        //    //2.1 根据订单编号查询声音购买记录，验证这比订单是否重复处理
        //    Long count = userPaidTrackMapper.selectCount(
        //            new LambdaQueryWrapper<UserPaidTrack>()
        //                    .eq(UserPaidTrack::getOrderNo, userPaidRecordVo.getOrderNo())
        //    );
        //    if (count == 0) {
        //        //2.1 新增声音购买记录 可能存在多条声音购买记录
        //        List<Long> itemIdList = userPaidRecordVo.getItemIdList();
        //        //2.2 远程调用"专辑服务获取声音信息" 得到专辑ID
        //        TrackInfo trackInfo = albumFeignClient.getTrackInfo(itemIdList.get(0)).getData();
        //        Long albumId = trackInfo.getAlbumId();
        //        //2.2 新增声音购买记录（等同于发放权益）
        //        for (Long itemId : itemIdList) {
        //            UserPaidTrack userPaidTrack = new UserPaidTrack();
        //            userPaidTrack.setOrderNo(userPaidRecordVo.getOrderNo());
        //            userPaidTrack.setUserId(userPaidRecordVo.getUserId());
        //            userPaidTrack.setAlbumId(albumId);
        //            userPaidTrack.setTrackId(itemId);
        //            userPaidTrackMapper.insert(userPaidTrack);
        //        }
        //    }
        //} else if (SystemConstant.ORDER_ITEM_TYPE_VIP.equals(itemType)) {
        //    //3.处理购买类型是VIP会员
        //    //3.1 根据订单编号查询会员购买记录，验证这比订单是否重复处理
        //    Long count = userVipServiceMapper.selectCount(
        //            new LambdaQueryWrapper<UserVipService>()
        //                    .eq(UserVipService::getOrderNo, userPaidRecordVo.getOrderNo())
        //    );
        //    if (count == 0) {
        //        //3.2 获取当前用户身份，是否为VIP会员
        //        Boolean isVIP = false;
        //        UserInfoVo userInfoVo = this.getUserInfo(userPaidRecordVo.getUserId());
        //        if (userInfoVo.getIsVip().intValue() == 1 && userInfoVo.getVipExpireTime().after(new Date())) {
        //            isVIP = true;
        //        }
        //        //3.3 封装会员购买记录，计算本次会员生效时间，失效时间
        //        UserVipService userVipService = new UserVipService();
        //        userVipService.setOrderNo(userPaidRecordVo.getOrderNo());
        //        userVipService.setUserId(userPaidRecordVo.getUserId());
        //        //3.3.2 根据用户选择套餐ID查询套餐信息
        //        VipServiceConfig vipServiceConfig = vipServiceConfigMapper.selectById(userPaidRecordVo.getItemIdList().get(0));
        //        Integer serviceMonth = vipServiceConfig.getServiceMonth();
        //        //3.3.1 本次会员起始时间 如果用户是普通用户=当前时间 如果是VIP获取当前用户会员失效时间+1天
        //        //      本次会员过期时间 如果用户是普通用户=当前时间+服务月数 如果是VIP=现有会员过期时间+服务月数
        //        if (!isVIP) {
        //            userVipService.setStartTime(new Date());
        //            userVipService.setExpireTime(DateUtil.offsetMonth(new Date(), serviceMonth));
        //        } else {
        //            DateTime startTime = DateUtil.offsetDay(userInfoVo.getVipExpireTime(), 1);
        //            userVipService.setStartTime(startTime);
        //            userVipService.setExpireTime(DateUtil.offsetMonth(startTime, serviceMonth));
        //        }
        //        //userVipService.setIsAutoRenew();
        //        //userVipService.setNextRenewTime();
        //
        //        //3.4 新增会员购买记录
        //        userVipServiceMapper.insert(userVipService);
        //
        //
        //        //3.5 更新用户信息表会员标识以及过期时间
        //        UserInfo userInfo = new UserInfo();
        //        userInfo.setId(userPaidRecordVo.getUserId());
        //        userInfo.setIsVip(1);
        //        userInfo.setVipExpireTime(userVipService.getExpireTime());
        //        userInfoMapper.updateById(userInfo);
        //    }
        //}
    }

    /***
     * 取消过期会员 会员标识
     * @param now
     */
    @Override
    public void updateVipExpireStatus(Date now) {
        //1.找出会员已失效用户列表
        List<UserInfo> userInfoList = userInfoMapper.selectList(
                new LambdaQueryWrapper<UserInfo>()
                        .eq(UserInfo::getIsVip, 1)
                        .lt(UserInfo::getVipExpireTime, now)
                        .select(UserInfo::getId)
        );
        //2.更新会员标识
        if(CollUtil.isNotEmpty(userInfoList)){
            for (UserInfo userInfo : userInfoList) {
                userInfo.setIsVip(0);
                userInfoMapper.updateById(userInfo);
            }
        }
    }

    @Autowired
    private VipServiceConfigMapper vipServiceConfigMapper;
}
