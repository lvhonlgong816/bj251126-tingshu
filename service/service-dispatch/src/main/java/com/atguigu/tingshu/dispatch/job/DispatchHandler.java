package com.atguigu.tingshu.dispatch.job;

import com.atguigu.tingshu.search.client.SearchFeignClient;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import groovy.transform.AutoClone;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DispatchHandler {


    @XxlJob("hellojob")
    public void hellojob() {
        String jobParam = XxlJobHelper.getJobParam();
        if ("error".equals(jobParam)) {
            int i = 1 / 0;
        }
        log.info("任务执行");
    }


    @XxlJob("childjob")
    public void childjob(){
        log.info("子任务执行..");
    }

    /***
     * 所有分片（执行器）一次调度广播所有执行器同时执行任务
     * 1.避免任务（给顾客发送短信）重复执行
     */
    @XxlJob("shardjob")
    public void shardjob(){
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.info("分片任务执行,总分片数（执行器数量）：{}, 当前分片（执行器）索引：{}", shardTotal, shardIndex);
    }

    @Autowired
    private SearchFeignClient searchFeignClient;

    @Autowired
    private UserFeignClient userFeignClient;


    /**
     * 定时更新小时榜TOPN记录
     */
    @XxlJob("updateLatelyAlbumRanking")
    public void updateLatelyAlbumRanking() {
        //1.获取任务参数
        String jobParam = XxlJobHelper.getJobParam();
        int topN = Integer.parseInt(jobParam);
        //2.远程调用搜索服务更新
        searchFeignClient.updateLatelyAlbumRanking(topN);
    }

    /**
     * 定时更新过期会员标识
     */
    @XxlJob("updateVipExpireStatus")
    public void updateVipExpireStatus() {
        userFeignClient.updateVipExpireStatus();
    }
}
