package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.RechargeInfo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface RechargeInfoService extends IService<RechargeInfo> {

    /**
     * 根据充值订单编号，查询充值记录
     * @param orderNo
     * @return
     */
    RechargeInfo getRechargeInfo(String orderNo);
}
