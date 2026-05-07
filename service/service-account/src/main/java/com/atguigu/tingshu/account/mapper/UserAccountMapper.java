package com.atguigu.tingshu.account.mapper;

import com.atguigu.tingshu.model.account.UserAccount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccount> {

    /**
     * 悲观锁避免并发带来超扣问题
     * @param userId
     * @param amount
     * @return
     */
    UserAccount checkAndDeduct(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

}
