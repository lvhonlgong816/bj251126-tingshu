package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.Map;

public interface UserAccountService extends IService<UserAccount> {


    /**
     * 初始化账户记录
     * @param msgData
     */
    void initUserAccount(Map<String, Object> msgData);

    /**
     * 保存账户变动日志
     * @param userId 用户ID
     * @param title 内容
     * @param trade_type 交易类型 交易类型：1201-充值 1204-消费
     * @param amount 金额
     * @param orderNo 订单编号
     */
    void saveUserAccountDetail(Long userId, String title, String trade_type, BigDecimal amount, String orderNo);

    /**
     * 查询指定用户可用账户余额
     * @param userId
     * @return
     */
    BigDecimal getAvailableAmount(Long userId);

    /**
     * 支付方式选择余额支付，执行扣减逻辑
     *
     * @param accountDeductVo 扣减信息vo
     * @return
     */
    void checkAndDeduct(AccountDeductVo accountDeductVo);

    /**
     *
     * @param pageInfo 分页对象
     * @param userId 用户ID
     * @param tradeType 交易类型 1201充值 1204消费
     */
    void findUserAccountDetailPage(Page pageInfo, Long userId, String tradeType);
}
