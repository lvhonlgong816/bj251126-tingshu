package com.atguigu.tingshu.user.pattern.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.model.user.UserVipService;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.mapper.UserVipServiceMapper;
import com.atguigu.tingshu.user.mapper.VipServiceConfigMapper;
import com.atguigu.tingshu.user.pattern.DeliveryStrategy;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.swing.plaf.ProgressBarUI;
import java.util.Date;

/**
 * @author: atguigu
 * @create: 2026-05-07 14:44
 */
@Slf4j
@Component(SystemConstant.ORDER_ITEM_TYPE_VIP)
public class VIPDeliveryStrategy implements DeliveryStrategy {

    @Autowired
    private UserVipServiceMapper userVipServiceMapper;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private VipServiceConfigMapper vipServiceConfigMapper;
    /**
     * VIP会员虚拟物品发货具体实现
     *
     * @param userPaidRecordVo
     */
    @Override
    public void delivery(UserPaidRecordVo userPaidRecordVo) {
        log.info("VIP会员虚拟物品发货:{}", userPaidRecordVo);
        //3.处理购买类型是VIP会员
        //3.1 根据订单编号查询会员购买记录，验证这比订单是否重复处理
        Long count = userVipServiceMapper.selectCount(
                new LambdaQueryWrapper<UserVipService>()
                        .eq(UserVipService::getOrderNo, userPaidRecordVo.getOrderNo())
        );
        if (count == 0) {
            //3.2 获取当前用户身份，是否为VIP会员
            Boolean isVIP = false;
            UserInfo userInfo = userInfoMapper.selectById(userPaidRecordVo.getUserId());
            if (userInfo.getIsVip().intValue() == 1 && userInfo.getVipExpireTime().after(new Date())) {
                isVIP = true;
            }
            //3.3 封装会员购买记录，计算本次会员生效时间，失效时间
            UserVipService userVipService = new UserVipService();
            userVipService.setOrderNo(userPaidRecordVo.getOrderNo());
            userVipService.setUserId(userPaidRecordVo.getUserId());
            //3.3.2 根据用户选择套餐ID查询套餐信息
            VipServiceConfig vipServiceConfig = vipServiceConfigMapper.selectById(userPaidRecordVo.getItemIdList().get(0));
            Integer serviceMonth = vipServiceConfig.getServiceMonth();
            //3.3.1 本次会员起始时间 如果用户是普通用户=当前时间 如果是VIP获取当前用户会员失效时间+1天
            //      本次会员过期时间 如果用户是普通用户=当前时间+服务月数 如果是VIP=现有会员过期时间+服务月数
            if (!isVIP) {
                userVipService.setStartTime(new Date());
                userVipService.setExpireTime(DateUtil.offsetMonth(new Date(), serviceMonth));
            } else {
                DateTime startTime = DateUtil.offsetDay(userInfo.getVipExpireTime(), 1);
                userVipService.setStartTime(startTime);
                userVipService.setExpireTime(DateUtil.offsetMonth(startTime, serviceMonth));
            }
            //userVipService.setIsAutoRenew();
            //userVipService.setNextRenewTime();

            //3.4 新增会员购买记录
            userVipServiceMapper.insert(userVipService);


            //3.5 更新用户信息表会员标识以及过期时间
            userInfo.setIsVip(1);
            userInfo.setVipExpireTime(userVipService.getExpireTime());
            userInfoMapper.updateById(userInfo);
        }
    }
}
