package com.atguigu.tingshu.album.task;

import cn.hutool.core.collection.CollUtil;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.service.AuditService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.atguigu.tingshu.common.constant.SystemConstant.*;

/**
 * @author: atguigu
 * @create: 2026-04-20 16:09
 */
@Component
public class ReviewResultTask {


    @Autowired
    private TrackInfoMapper trackInfoMapper;

    @Autowired
    private AuditService auditService;

    /**
     * Cron表达式：秒 分 时 日 月 周 [年]
     */
    @Scheduled(cron = "0/5 * * * * ?")
    public void getReviewResultTask(){
        //1.查询处于审核中声音列表
        List<TrackInfo> trackInfoList = trackInfoMapper.selectList(
                new LambdaQueryWrapper<TrackInfo>()
                        .eq(TrackInfo::getStatus, SystemConstant.TRACK_STATUS_REVIEWING)
                        .select(TrackInfo::getId, TrackInfo::getReviewTaskId)
                        .last("limit 100")
        );

        //2.遍历审核中声音列表 根据声音中任务ID查询审核结果
        if(CollUtil.isNotEmpty(trackInfoList)){
            for (TrackInfo trackInfo : trackInfoList) {
                String reviewTaskId = trackInfo.getReviewTaskId();
                String suggest = auditService.getRevivewTaskResult(reviewTaskId);
                if("block".equals(suggest)){
                    trackInfo.setStatus(TRACK_STATUS_NO_PASS);
                }else if("review".equals(suggest)){
                    trackInfo.setStatus(TRACK_STATUS_ARTIFICIAL);
                }else if("pass".equals(suggest)){
                    trackInfo.setStatus(TRACK_STATUS_PASS);
                }
                trackInfoMapper.updateById(trackInfo);
            }
        }
    }


}
