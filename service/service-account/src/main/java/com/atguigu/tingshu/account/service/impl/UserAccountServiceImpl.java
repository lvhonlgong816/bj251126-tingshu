package com.atguigu.tingshu.account.service.impl;

import com.atguigu.tingshu.account.mapper.UserAccountDetailMapper;
import com.atguigu.tingshu.account.mapper.UserAccountMapper;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
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
	 * @param userId 用户ID
	 * @param title 内容
	 * @param tradeType 交易类型 交易类型：1201-充值 1204-消费
	 * @param amount 金额
	 * @param orderNo 订单编号
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
}
