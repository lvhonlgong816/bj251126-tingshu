package com.atguigu.tingshu.search.service;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;

import java.util.List;
import java.util.Map;

public interface SearchService {

    /**
     * 手动上架指定专辑到索引库
     * @param albumId
     * @return
     */
    void upperAlbum(Long albumId);


    /**
     * 手动从索引库下架指定专辑
     * @param albumId
     * @return
     */
    void lowerAlbum(Long albumId);

    /**
     * #检索条件：1.关键字、2.分类ID（1,2,3级）、3.标签（标签ID跟标签值）
     * #排序方式：1.热度 2.播放量  3.发布时间
     * #检索结果：1.分页  2.关键字高亮 3.返回满足渲染页面字段列表
     * @param albumIndexQuery
     * @return
     */
    AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery);

    /**
     * 构建站内检索请求对象
     * @param albumIndexQuery 查询条件
     * @return 检索请求对象
     */
    SearchRequest buildDSL(AlbumIndexQuery albumIndexQuery);

    /**
     * 解析ES检索响应结果，封装结果VO
     *
     * @param searchResponse  ES响应对象
     * @param albumIndexQuery 查询条件 需要获取分页参数
     * @return 结果VO
     */
    AlbumSearchResponseVo parseResult(SearchResponse<AlbumInfoIndex> searchResponse, AlbumIndexQuery albumIndexQuery);

    /**
     * 查询置顶三级分类包含热门专辑列表
     * @param category1Id
     * @return
     */
    List<Map<String, Object>> channel(Long category1Id);
}
