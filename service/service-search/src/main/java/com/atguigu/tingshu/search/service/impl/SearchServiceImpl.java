package com.atguigu.tingshu.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONObject;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsAggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.repository.AlbumInfoIndexRepository;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Slf4j
@Service
@SuppressWarnings({"all"})
public class SearchServiceImpl implements SearchService {

    @Autowired
    private AlbumInfoIndexRepository albumInfoIndexRepository;

    @Autowired
    private AlbumFeignClient albumFeignClient;

    @Autowired
    private UserFeignClient userFeignClient;

    //Autowired 先按类型注入，再按bean名称注入
    //@Resource 先按Bean名称注入，再按类型注入
    @Autowired//(required = false)
    private Executor threadPoolExecutor;

    /**
     * 手动上架指定专辑到索引库
     *
     * @param albumId
     */
    @Override
    public void upperAlbum(Long albumId) {
        //1.创建索引库文档对象
        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();

        //2.封装专辑信息（包括标签列表） 创建需要返回结果异步任务对象
        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            //2.1 远程调用专辑服务获取专辑信息
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
            Assert.notNull(albumInfo, "专辑{}不存在", albumId);
            //2.2 封装专辑基本信息
            BeanUtil.copyProperties(albumInfo, albumInfoIndex);

            //2.3 封装专辑包含标签列表
            List<AlbumAttributeValue> albumAttributeValueVoList = albumInfo.getAlbumAttributeValueVoList();
            if (CollUtil.isNotEmpty(albumAttributeValueVoList)) {
                List<AttributeValueIndex> attributeValueIndexList = albumAttributeValueVoList.
                        stream()
                        .map(albumAttributeValueVo -> BeanUtil.copyProperties(albumAttributeValueVo, AttributeValueIndex.class)
                        ).collect(Collectors.toList());
                albumInfoIndex.setAttributeValueIndexList(attributeValueIndexList);
            }
            return albumInfo;
        }, threadPoolExecutor);


        //3.封装分类信息 基于专辑信息异步任务 创建主播异步任务对象 无结果
        CompletableFuture<Void> categoryViewCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            //3.1 远程调用专辑服务获取分类信息
            BaseCategoryView baseCategoryView = albumFeignClient.getCategoryView(albumInfo.getCategory3Id()).getData();
            Assert.notNull(baseCategoryView, "专辑：{}下分类{}不存在", albumId, albumInfo.getCategory3Id());
            //3.2 封装1,2分类ID属性
            albumInfoIndex.setCategory1Id(baseCategoryView.getCategory1Id());
            albumInfoIndex.setCategory2Id(baseCategoryView.getCategory2Id());
        }, threadPoolExecutor);

        //4.封装主播信息 基于专辑信息异步任务 创建主播异步任务对象 无结果
        CompletableFuture<Void> userCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(albumInfo.getUserId()).getData();
            Assert.notNull(userInfoVo, "专辑：{}主播：{}信息为空", albumId, albumInfo.getUserId());
            albumInfoIndex.setAnnouncerName(userInfoVo.getNickname());
        }, threadPoolExecutor);

        //5.封装统计信息 TODO 暂时采用随机值，后续改为远程调用 创建异步任务 不依赖其他任务，无返回值
        CompletableFuture<Void> statCompletableFuture = CompletableFuture.runAsync(() -> {

            //5.1 产生随机数值作为四项统计数值
            int num1 = RandomUtil.randomInt(1000, 2000);
            int num2 = RandomUtil.randomInt(500, 1000);
            int num3 = RandomUtil.randomInt(100, 500);
            int num4 = RandomUtil.randomInt(10, 100);
            albumInfoIndex.setPlayStatNum(num1);
            albumInfoIndex.setSubscribeStatNum(num2);
            albumInfoIndex.setBuyStatNum(num3);
            albumInfoIndex.setCommentStatNum(num4);
            //5.2 基于统计数值计算热度 公式=不同维度统计数值*系数 累计后数值作为热度
            BigDecimal hotScore = BigDecimal.valueOf(0.1).multiply(BigDecimal.valueOf(num1))
                    .add(BigDecimal.valueOf(0.2).multiply(BigDecimal.valueOf(num2)))
                    .add(BigDecimal.valueOf(0.3).multiply(BigDecimal.valueOf(num3)))
                    .add(BigDecimal.valueOf(0.4).multiply(BigDecimal.valueOf(num4)));
            albumInfoIndex.setHotScore(hotScore.doubleValue());
        }, threadPoolExecutor);

        //6.组合所有异步任务，所有任务执行结束主线程继续
        CompletableFuture.allOf(
                        albumInfoCompletableFuture,
                        statCompletableFuture,
                        categoryViewCompletableFuture,
                        userCompletableFuture
                )
                .orTimeout(1, TimeUnit.SECONDS)
                .join();
        //6.调用ES持久层保存专辑索引库文档对象到ES
        albumInfoIndexRepository.save(albumInfoIndex);
    }


    /**
     * 手动从索引库下架指定专辑
     *
     * @param albumId
     * @return
     */
    @Override
    public void lowerAlbum(Long albumId) {
        albumInfoIndexRepository.deleteById(albumId.toString());
    }

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    /**
     * 高级检索采用ES原生方式完成 检索请求体每项参数内部采用Lambda方式  整体采用传统写法
     * <p>
     * #检索条件：1.关键字、2.分类ID（1,2,3级）、3.标签（标签ID跟标签值）
     * #排序方式：1.热度 2.播放量  3.发布时间
     * #检索结果：1.分页  2.关键字高亮 3.返回满足渲染页面字段列表
     *
     * @param albumIndexQuery
     * @return
     */
    @Override
    public AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery) {
        try {
            //一、构建检索请求对象
            SearchRequest searchRequest = this.buildDSL(albumIndexQuery);
            //System.err.println("本次检索DSL：");
            //System.err.println(searchRequest);
            //二、调用ES检索接口
            SearchResponse<AlbumInfoIndex> searchResponse = elasticsearchClient.search(searchRequest, AlbumInfoIndex.class);
            //三、解析ES检索结果
            return this.parseResult(searchResponse, albumIndexQuery);
        } catch (IOException e) {
            log.error("站内检索异常：", e);
            throw new RuntimeException(e);
        }
    }

    //专辑索引库名称
    private static final String INDEX_NAME = "albuminfo";

    /**
     * 构建站内检索请求对象
     *
     * @param albumIndexQuery 查询条件
     * @return 检索请求对象
     */
    @Override
    public SearchRequest buildDSL(AlbumIndexQuery albumIndexQuery) {
        //1.创建检索请求构建器对象 封装检索URL中索引库名称以及请求体参数
        SearchRequest.Builder builder = new SearchRequest.Builder();
        builder.index(INDEX_NAME);
        //2.逐一设置每一项检索请求体参数 设置过滤条件 请求体参数中"query"
        //2.1 创建用于封装三大过滤条件bool查询对象
        BoolQuery.Builder allConditionBoolQueryBuilder = new BoolQuery.Builder();
        //2.2 设置关键字查询条件 由用户录入,按照相关性返回 采用must
        String keyword = albumIndexQuery.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            allConditionBoolQueryBuilder.must(m -> m.match(m1 -> m1.field("albumTitle").query(keyword)));
        }
        //2.3 TODO 设置分类过滤条件 适合放入缓存中 采用filter合适
        // 设置1,2,3级分类过滤条件
        Long category1Id = albumIndexQuery.getCategory1Id();
        if (category1Id != null) {
            allConditionBoolQueryBuilder.filter(f -> f.term(t -> t.field("category1Id").value(category1Id)));
        }
        Long category2Id = albumIndexQuery.getCategory2Id();
        if (category2Id != null) {
            allConditionBoolQueryBuilder.filter(f -> f.term(t -> t.field("category2Id").value(category2Id)));
        }
        Long category3Id = albumIndexQuery.getCategory3Id();
        if (category3Id != null) {
            allConditionBoolQueryBuilder.filter(f -> f.term(t -> t.field("category3Id").value(category3Id)));
        }
        //2.4 设置标签过滤条件 适合放入缓存中 采用filter合适
        List<String> attributeList = albumIndexQuery.getAttributeList();
        if (CollUtil.isNotEmpty(attributeList)) {
            //2.4.1 遍历标签条件，每遍历一次创建nested查询
            for (String s : attributeList) {
                //2.4.2 对标签条件":"进行分割 得到数组 0=标签ID 1=标签值ID
                String[] split = s.split(":");
                if (split != null && split.length == 2) {
                    allConditionBoolQueryBuilder.filter(
                            f -> f.nested(n -> n.path("attributeValueIndexList").query(
                                    q -> q.bool(b -> b.must(
                                            m -> m.term(t -> t.field("attributeValueIndexList.attributeId").value(split[0]))
                                    ).must(m -> m.term(t -> t.field("attributeValueIndexList.valueId").value(split[1]))))
                            ))
                    );
                }
            }
        }
        //2.5 将bool查询对象封装到query中
        builder.query(allConditionBoolQueryBuilder.build()._toQuery());
        //2.2 设置分页 请求体参数中"from"=起始文档索引 "size"="页大小"
        Integer pageNo = albumIndexQuery.getPageNo();
        Integer pageSize = albumIndexQuery.getPageSize();
        int from = (pageNo - 1) * pageSize;
        builder.from(from).size(pageSize);

        //2.3 设置排序
        //2.3.1 获取排序参数 非空校验
        String order = albumIndexQuery.getOrder();
        if (StringUtils.isNotBlank(order)) {
            String[] split = order.split(":");
            if (split != null && split.length == 2) {
                String orderField = "";
                switch (split[0]) {
                    case "1":
                        orderField = "hotScore";
                        break;
                    case "2":
                        orderField = "playStatNum";
                        break;
                    case "3":
                        orderField = "createTime";
                        break;
                }
                //2.3.2 获取排序字段名称 以及 排序 方式
                String finalOrderField = orderField;
                builder.sort(s -> s.field(f -> f.field(finalOrderField).order("asc".equals(split[1]) ? SortOrder.Asc : SortOrder.Desc)));
            }
        }
        //2.4 设置高亮 必须全文查询才有高亮，请求体参数中“highlight” 三个要素：1.高亮字段 2.高亮前置标签 3.高亮后置标签
        if (StringUtils.isNotBlank(keyword)) {
            builder.highlight(h -> h.fields("albumTitle", hf -> hf.preTags("<font style='color:red'>").postTags("</font>")));
        }

        //2.5 设置响应业务字段列表
        builder.source(s -> s.filter(f -> f.excludes(Arrays.asList(new String[]{"subscribeStatNum", "buyStatNum", "commentStatNum", "attributeValueIndexList", "announcerName", "category1Id", "category2Id", "category3Id"}))));

        //3.基于构建器对象返回检索请求对象
        return builder.build();
    }

    /**
     * 解析ES检索响应结果，封装结果VO
     *
     * @param searchResponse  ES响应对象
     * @param albumIndexQuery
     * @return 结果VO
     */
    @Override
    public AlbumSearchResponseVo parseResult(SearchResponse<AlbumInfoIndex> searchResponse, AlbumIndexQuery albumIndexQuery) {
        //1.创建响应VO对象
        AlbumSearchResponseVo vo = new AlbumSearchResponseVo();
        //2.解析ES结果封装专辑VO列表
        List<AlbumInfoIndexVo> albumInfoIndexVoList = searchResponse.hits().hits()
                .stream()
                .map(hit -> {
                    //2.1 将文档中source转为专辑vo对象
                    AlbumInfoIndexVo albumInfoIndexVo = BeanUtil.copyProperties(hit.source(), AlbumInfoIndexVo.class);
                    //2.2 处理可能有高亮片段
                    Map<String, List<String>> highlightMap = hit.highlight();
                    if (CollUtil.isNotEmpty(highlightMap)) {
                        String highlightAlbumTitle = highlightMap.get("albumTitle").get(0);
                        albumInfoIndexVo.setAlbumTitle(highlightAlbumTitle);
                    }
                    return albumInfoIndexVo;
                }).collect(Collectors.toList());
        vo.setList(albumInfoIndexVoList);
        //3.封装分页相关属性
        Integer pageNo = albumIndexQuery.getPageNo();
        Integer pageSize = albumIndexQuery.getPageSize();
        long total = searchResponse.hits().total().value();
        long totalPages = total % pageSize == 0 ? total / pageSize : total / pageSize + 1;
        vo.setPageNo(pageNo);
        vo.setPageSize(pageSize);
        vo.setTotal(total);
        vo.setTotalPages(totalPages);

        //4.响应VO
        return vo;
    }

    /**
     * 查询置顶三级分类包含热门专辑列表
     *
     * @param category1Id
     * @return
     */
    @Override
    public List<Map<String, Object>> channel(Long category1Id) {
        try {
            //1.远程调用专辑服务，获取置顶三级分类ID列表
            List<BaseCategory3> baseCategory3List = albumFeignClient.findTop7BaseCategory3(category1Id).getData();
            Assert.notNull(baseCategory3List, "暂无置顶三级分类");
            //1.1 获取所有三级分类ID
            List<FieldValue> fieldValueList = baseCategory3List.stream()
                    .map(c3 -> FieldValue.of(c3.getId()))
                    .collect(Collectors.toList());

            //1.2 方便后续封装三级分类对象，将三级分类列表转为Map<三级分类ID， 三级分类对象>
            Map<Long, BaseCategory3> category3Map = baseCategory3List
                    .stream()
                    .collect(Collectors.toMap(BaseCategory3::getId, c3 -> c3));


            //2.执行聚合检索
            SearchResponse<AlbumInfoIndex> searchResponse = elasticsearchClient.search(
                    s -> s.query(q -> q.terms(t -> t.field("category3Id").terms(t1 -> t1.value(fieldValueList))))
                            .size(0)
                            .aggregations("category3_agg", a -> a.terms(t -> t.field("category3Id").size(10))
                                    //top_hits子聚合
                                    .aggregations("top6_agg", a1 -> a1.topHits(
                                            t -> t.sort(s1 -> s1.field(f -> f.field("hotScore").order(SortOrder.Desc)))
                                                    .size(6)
                                                    //TODO 点击查看全部 从第一个热门专辑获取三级分类ID
                                                    .source(s1 -> s1.filter(f -> f.excludes("subscribeStatNum", "buyStatNum", "commentStatNum", "attributeValueIndexList", "announcerName")))
                                    ))
                            ),
                    AlbumInfoIndex.class
            );
            //3.解析ES聚合结果
            //3.1 获取三级分类聚合对象
            LongTermsAggregate category3Agg = searchResponse.aggregations().get("category3_agg").lterms();
            //3.2 遍历桶数组，创建置顶三级分类Map对象
            List<Map<String, Object>> mapList = category3Agg.buckets().array()
                    .stream()
                    .map(bucket -> {
                        //3.2.0 创建置顶分类热门专辑Map
                        Map<String, Object> map = new HashMap<>();
                        //3.2.1 获取到三级分类ID 封装分类对象
                        long category3Id = bucket.key();
                        map.put("baseCategory3", category3Map.get(category3Id));
                        //3.2.2 下钻获取tophits子聚合结果 获取top6专辑
                        List<AlbumInfoIndex> top6List = bucket.aggregations().get("top6_agg")
                                .topHits().hits().hits()
                                .stream()
                                .map(hit -> JSON.parseObject(hit.source().toString(), AlbumInfoIndex.class)
                                ).collect(Collectors.toList());
                        //3.2.3 封装当前分类下 热度最高top6专辑列表
                        map.put("list", top6List);
                        return map;
                    }).collect(Collectors.toList());
            return mapList;
        } catch (IOException e) {
            log.error("首页置顶分类热门专辑异常", e);
            throw new RuntimeException(e);
        }
    }
}
