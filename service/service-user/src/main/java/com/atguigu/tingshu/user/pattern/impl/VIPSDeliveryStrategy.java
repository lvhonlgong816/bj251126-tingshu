package com.atguigu.tingshu.user.pattern.impl;

import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.user.pattern.DeliveryStrategy;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author: atguigu
 * @create: 2026-05-07 14:44
 */
@Slf4j
@Component("1004")
public class VIPSDeliveryStrategy implements DeliveryStrategy {

    /**
     * 超级VIP会员虚拟物品发货具体实现
     *
     * @param userPaidRecordVo
     */
    @Override
    public void delivery(UserPaidRecordVo userPaidRecordVo) {
        log.info("超级VIP会员虚拟物品发货:{}", userPaidRecordVo);
    }
}
