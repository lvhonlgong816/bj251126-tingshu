package com.atguigu.tingshu.account.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.account.mapper.RechargeInfoMapper;
import com.atguigu.tingshu.account.mapper.UserAccountMapper;
import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@SuppressWarnings({"all"})
public class RechargeInfoServiceImpl extends ServiceImpl<RechargeInfoMapper, RechargeInfo> implements RechargeInfoService {

    @Autowired
    private RechargeInfoMapper rechargeInfoMapper;

    @Override
    public RechargeInfo getRechargeInfo(String orderNo) {
        return rechargeInfoMapper.selectOne(
                new LambdaQueryWrapper<RechargeInfo>()
                        .eq(RechargeInfo::getOrderNo, orderNo)
        );
    }

    /**
     * 保存充值记录，返回充值订单编号用于对接微信支付
     *
     * @param rechargeInfoVo
     * @return {orderNo:"充值订单编号"}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> submitRecharge(Long userId, RechargeInfoVo rechargeInfoVo) {
        //1.将充值VO转为PO对象
        RechargeInfo rechargeInfo = new RechargeInfo();
        rechargeInfo.setRechargeAmount(rechargeInfoVo.getAmount());
        rechargeInfo.setPayWay(rechargeInfoVo.getPayWay());

        //2.封装属性：用户ID、充值订单编号、充值状态=订单状态 0901-正常 0902-已支付 0903-已取消
        rechargeInfo.setUserId(userId);
        rechargeInfo.setRechargeStatus(SystemConstant.ORDER_STATUS_UNPAID);
        //订单编号：CZ+当天日期+雪花算法
        String orderNo = "CZ" + DateUtil.today().replaceAll("-", "") + IdUtil.getSnowflakeNextId();
        rechargeInfo.setOrderNo(orderNo);

        //3.保存充值记录
        rechargeInfoMapper.insert(rechargeInfo);

        //4.TODO 基于Rabbit延迟消息自动关闭超时充值记录

        //5.返回充值订单编号
        return Map.of("orderNo", orderNo);
    }

    @Autowired
    private UserAccountService userAccountService;

    /**
     * 支付成功后，修改充值状态以及完成充值
     *
     * @param orderNo
     * @return
     */
    @Override
    public void rechargePaySuccess(String orderNo) {
        //1.修改充值状态 改为：已支付
        int update = rechargeInfoMapper.update(
                null,
                new LambdaUpdateWrapper<RechargeInfo>()
                        .eq(RechargeInfo::getOrderNo, orderNo)
                        .eq(RechargeInfo::getRechargeStatus, SystemConstant.ORDER_STATUS_UNPAID)
                        .set(RechargeInfo::getRechargeStatus, SystemConstant.ORDER_STATUS_PAID)
        );
        //2.余额充值
        if (update > 0) {
            //2.1 根据充值订单编号查询充值记录
            RechargeInfo rechargeInfo = rechargeInfoMapper.selectOne(
                    new LambdaQueryWrapper<RechargeInfo>()
                            .eq(RechargeInfo::getOrderNo, orderNo)
            );
            //2.2 更新余额
            boolean flag = userAccountService.update(
                    null,
                    new LambdaUpdateWrapper<UserAccount>()
                            .eq(UserAccount::getUserId, rechargeInfo.getUserId())
                            .setSql("total_amount = total_amount+" + rechargeInfo.getRechargeAmount())
                            .setSql("available_amount = available_amount+" + rechargeInfo.getRechargeAmount())
                            .setSql("total_income_amount = total_income_amount+" + rechargeInfo.getRechargeAmount())
            );
            if (flag) {
                //3.新增账户变动日志
                userAccountService.saveUserAccountDetail(
                        rechargeInfo.getUserId(),
                        "账户充值",
                        SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT,
                        rechargeInfo.getRechargeAmount(),
                        rechargeInfo.getOrderNo()
                );
            }

        }

    }
}
