package com.atguigu.tingshu.user.pattern.impl;

import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.atguigu.tingshu.user.mapper.UserPaidTrackMapper;
import com.atguigu.tingshu.user.pattern.DeliveryStrategy;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author: atguigu
 * @create: 2026-05-07 14:44
 */
@Slf4j
@Component(SystemConstant.ORDER_ITEM_TYPE_TRACK)
@SuppressWarnings("all")
public class TrackDeliveryStrategy implements DeliveryStrategy {

    @Autowired
    private UserPaidTrackMapper userPaidTrackMapper;

    @Autowired
    private AlbumFeignClient albumFeignClient;

    /**
     * 声音虚拟物品发货具体实现
     *
     * @param userPaidRecordVo
     */
    @Override
    public void delivery(UserPaidRecordVo userPaidRecordVo) {
        log.info("声音虚拟物品发货:{}", userPaidRecordVo);
        //2.处理购买类型是声音
        //2.1 根据订单编号查询声音购买记录，验证这比订单是否重复处理
        Long count = userPaidTrackMapper.selectCount(
                new LambdaQueryWrapper<UserPaidTrack>()
                        .eq(UserPaidTrack::getOrderNo, userPaidRecordVo.getOrderNo())
        );
        if (count == 0) {
            //2.1 新增声音购买记录 可能存在多条声音购买记录
            List<Long> itemIdList = userPaidRecordVo.getItemIdList();
            //2.2 远程调用"专辑服务获取声音信息" 得到专辑ID
            TrackInfo trackInfo = albumFeignClient.getTrackInfo(itemIdList.get(0)).getData();
            Long albumId = trackInfo.getAlbumId();
            //2.2 新增声音购买记录（等同于发放权益）
            for (Long itemId : itemIdList) {
                UserPaidTrack userPaidTrack = new UserPaidTrack();
                userPaidTrack.setOrderNo(userPaidRecordVo.getOrderNo());
                userPaidTrack.setUserId(userPaidRecordVo.getUserId());
                userPaidTrack.setAlbumId(albumId);
                userPaidTrack.setTrackId(itemId);
                userPaidTrackMapper.insert(userPaidTrack);
            }
        }
    }
}
