package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface RechargeInfoService extends IService<RechargeInfo> {

    /**
     * 根据充值订单编号，查询充值记录
     * @param orderNo
     * @return
     */
    RechargeInfo getRechargeInfo(String orderNo);

    /**
     * 保存充值记录，返回充值订单编号用于对接微信支付
     * @param rechargeInfoVo
     * @return {orderNo:"充值订单编号"}
     */
    Map<String, String> submitRecharge(Long userId, RechargeInfoVo rechargeInfoVo);

    /**
     * 支付成功后，修改充值状态以及完成充值
     * @param orderNo
     * @return
     */
    void rechargePaySuccess(String orderNo);
}
