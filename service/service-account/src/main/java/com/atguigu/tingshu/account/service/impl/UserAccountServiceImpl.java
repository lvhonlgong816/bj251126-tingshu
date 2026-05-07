package com.atguigu.tingshu.account.service.impl;

import cn.hutool.core.lang.Assert;
import com.atguigu.tingshu.account.mapper.UserAccountDetailMapper;
import com.atguigu.tingshu.account.mapper.UserAccountMapper;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

    @Autowired
    private UserAccountMapper userAccountMapper;

    @Autowired
    private UserAccountDetailMapper userAccountDetailMapper;

    /**
     * 初始化账户记录
     *
     * @param msgData
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initUserAccount(Map<String, Object> msgData) {
        //0.从map获取业务数据
        Long userId = (Long) msgData.get("userId");
        BigDecimal amount = (BigDecimal) msgData.get("amount");
        //1.新增账户记录
        UserAccount userAccount = new UserAccount();
        userAccount.setUserId(userId);
        userAccount.setTotalAmount(amount);
        userAccount.setAvailableAmount(amount);
        userAccount.setTotalIncomeAmount(amount);
        this.save(userAccount);
        Long userAccountId = userAccount.getId();

        //2.新增账户变动日志
        String title = (String) msgData.get("title");
        String orderNo = (String) msgData.get("orderNo");
        this.saveUserAccountDetail(userId, title, SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT, amount, orderNo);
    }

    /**
     * 保存账户变动日志
     *
     * @param userId    用户ID
     * @param title     内容
     * @param tradeType 交易类型 交易类型：1201-充值 1204-消费
     * @param amount    金额
     * @param orderNo   订单编号
     */
    @Override
    public void saveUserAccountDetail(Long userId, String title, String tradeType, BigDecimal amount, String orderNo) {
        UserAccountDetail userAccountDetail = new UserAccountDetail();
        userAccountDetail.setUserId(userId);
        userAccountDetail.setTitle(title);
        userAccountDetail.setTradeType(tradeType);
        userAccountDetail.setAmount(amount);
        userAccountDetail.setOrderNo(orderNo);
        userAccountDetailMapper.insert(userAccountDetail);
    }

    /**
     * 查询指定用户可用账户余额
     *
     * @param userId
     * @return
     */
    @Override
    public BigDecimal getAvailableAmount(Long userId) {
        UserAccount userAccount = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>()
                        .eq(UserAccount::getUserId, userId)
                        .select(UserAccount::getAvailableAmount)
        );
        Assert.notNull(userAccount, "账户记录不存在请联系管理员");
        return userAccount.getAvailableAmount();
    }

    /**
     * 支付方式选择余额支付，执行扣减逻辑
     *
     * @param accountDeductVo 扣减信息vo
     * @return
     */
    @Override
    public void checkAndDeduct(AccountDeductVo accountDeductVo) {
        //1.扣减账户余额
        // 1.1 为了避免并发事务对同一条账户记录造成“超扣”问题，采用MySQL悲观锁 select SQL+for update; 其他并发事务无法操作记录
        UserAccount userAccount = userAccountMapper.checkAndDeduct(accountDeductVo.getUserId(), accountDeductVo.getAmount());
        if (userAccount == null) {
            throw new GuiguException(500, "账户余额不足");
        }
        if (userAccount != null) {
            // 1.2 扣减账户余额
            int update = userAccountMapper.update(
                    null,
                    new LambdaUpdateWrapper<UserAccount>()
                            .eq(UserAccount::getUserId, accountDeductVo.getUserId())
                            .ge(UserAccount::getAvailableAmount, accountDeductVo.getAmount())
                            .setSql("total_amount = total_amount - " + accountDeductVo.getAmount() + ",available_amount = available_amount-" + accountDeductVo.getAmount() + ", total_pay_amount = total_pay_amount + " + accountDeductVo.getAmount() + "")
            );
            if (update > 0) {
                //2.新值账户变动日志
                this.saveUserAccountDetail(
                        accountDeductVo.getUserId(),
                        accountDeductVo.getContent(),
                        SystemConstant.ACCOUNT_TRADE_TYPE_MINUS,
                        accountDeductVo.getAmount(),
                        accountDeductVo.getOrderNo()
                );
            }
        }

    }
}
