package com.atguigu.tingshu.user.pattern;

import com.atguigu.tingshu.vo.user.UserPaidRecordVo;

/**
 * 策略类：采用接口 提供抽象虚拟物品发货方法
 */
public interface DeliveryStrategy {

    /**
     * 虚拟物品发货抽象方法
     * @param userPaidRecordVo
     */
    public void delivery(UserPaidRecordVo userPaidRecordVo);
}
