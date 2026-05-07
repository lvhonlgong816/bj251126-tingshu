package com.atguigu.tingshu.user.pattern.impl;

import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.user.UserPaidAlbum;
import com.atguigu.tingshu.user.mapper.UserPaidAlbumMapper;
import com.atguigu.tingshu.user.pattern.DeliveryStrategy;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author: atguigu
 * @create: 2026-05-07 14:44
 */
@Slf4j
@Component(SystemConstant.ORDER_ITEM_TYPE_ALBUM)  //默认Bean名称 albumDeliveryStrategy
public class AlbumDeliveryStrategy implements DeliveryStrategy {

    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;

    /**
     * 专辑虚拟物品发货具体实现
     *
     * @param userPaidRecordVo
     */
    @Override
    public void delivery(UserPaidRecordVo userPaidRecordVo) {
        log.info("专辑虚拟物品发货:{}", userPaidRecordVo);
        //1.处理购买类型是专辑
        //1.1 根据订单编号查询专辑购买记录，验证这比订单是否重复处理
        Long count = userPaidAlbumMapper.selectCount(
                new LambdaQueryWrapper<UserPaidAlbum>()
                        .eq(UserPaidAlbum::getOrderNo, userPaidRecordVo.getOrderNo())
        );
        if (count == 0) {
            //1.2 新增已购专辑记录（等同于发放权益）
            UserPaidAlbum userPaidAlbum = new UserPaidAlbum();
            userPaidAlbum.setUserId(userPaidRecordVo.getUserId());
            userPaidAlbum.setOrderNo(userPaidRecordVo.getOrderNo());
            userPaidAlbum.setAlbumId(userPaidRecordVo.getItemIdList().get(0));
            userPaidAlbumMapper.insert(userPaidAlbum);
        }
    }
}
