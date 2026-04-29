package com.atguigu.tingshu.album;

import com.atguigu.tingshu.album.impl.AlbumDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * <p>
 * 专辑模块远程调用Feign接口,底层产生代理对象发起Http请求调用
 * 服务提供方专辑服务中提供接口地址：
 *  http://localhost:8501/api/album/albumInfo/getAlbumInfo/{id}
 * 服务调用方使用确定请求地址：将目标服务实例列表缓存到本地
 *  1.从Feign接口中得到：http://service-album/api/album/albumInfo/getAlbumInfo/{id}
 *  2.从Nacos得到专辑服务service-album对应两个实例信息,缓存到调用方本地
 *      实例1：http://localhost:8501
 *      实例2：http://localhost:8401
 *  3.OpenFeign底层集成负载均衡器组件：LoadBalancer 默认轮询
 *  4.发请求会将URL路径中域名改为具体IP跟端口
 *    第一次：http://localhost:8501/api/album/albumInfo/getAlbumInfo/{id}
 *    第二次：http://localhost:8401/api/album/albumInfo/getAlbumInfo/{id}
 * </p>
 *
 * @author atguigu
 */
@FeignClient(value = "service-album", path = "api/album", fallback = AlbumDegradeFeignClient.class)
public interface AlbumFeignClient {
    /**
     * 查询专辑信息（包含标签列表）
     * @param id
     * @return
     */
    @GetMapping("/albumInfo/getAlbumInfo/{id}")
    public Result<AlbumInfo> getAlbumInfo(@PathVariable Long id);


    /**
     * 根据3级分类ID查询分类视图对象
     * @param category3Id
     * @return
     */
    @GetMapping("/category/getCategoryView/{category3Id}")
    public Result<BaseCategoryView> getCategoryView(@PathVariable Long category3Id);


    /**
     * 根据1级分类ID查询置顶前七个三级分类列表
     * @param category1Id 1级分类ID
     * @return 三级分类列表
     */
    @GetMapping("/category/findTopBaseCategory3/{category1Id}")
    public Result<List<BaseCategory3>> findTop7BaseCategory3(@PathVariable Long category1Id);

    /**
     * 根据专辑ID查询统计信息
     * @param albumId 专辑ID
     * @return 统计VO对象
     */
    @GetMapping("/albumInfo/getAlbumStatVo/{albumId}")
    public Result<AlbumStatVo> getAlbumStatVo(@PathVariable Long albumId);

    @GetMapping("/category/findAllCategory1")
    public Result<List<BaseCategory1>> findAllCategory1();
}
