package com.atguigu.tingshu.account.mapper;

import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserAccountDetailMapper extends BaseMapper<UserAccountDetail> {


    /**
     * 分页条件查询账户变动日志记录
     * @param pageInfo 分页对象
     * @param  userId 用户ID
     * @param tradeType  1201-充值 1204-消费
     */
    Page<UserAccountDetail> findUserAccountDetail(
            Page<UserAccountDetail> pageInfo,
            @Param("userId") Long userId,
            @Param("tradeType") String tradeType
    );
}
