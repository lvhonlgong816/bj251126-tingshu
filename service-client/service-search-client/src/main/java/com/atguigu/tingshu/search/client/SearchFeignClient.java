package com.atguigu.tingshu.search.client;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.search.client.impl.SearchDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * <p>
 * 搜索模块远程调用API接口
 * </p>
 *
 * @author atguigu
 */
@FeignClient(value = "service-search", path = "api/search",fallback = SearchDegradeFeignClient.class)
public interface SearchFeignClient {

    /**
     * 更新小时榜TOPN记录
     * @return
     */
    @GetMapping("/albumInfo/updateLatelyAlbumRanking/{topN}")
    public Result updateLatelyAlbumRanking(@PathVariable Integer topN);

}
