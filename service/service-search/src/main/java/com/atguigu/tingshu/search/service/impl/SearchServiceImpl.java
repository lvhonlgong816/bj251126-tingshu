package com.atguigu.tingshu.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.RandomUtil;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.search.repository.AlbumInfoIndexRepository;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.PipedReader;
import java.math.BigDecimal;
import java.util.List;
import java.util.PrimitiveIterator;
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

    /**
     * 手动上架指定专辑到索引库
     *
     * @param albumId
     */
    @Override
    public void upperAlbum(Long albumId) {
        //1.创建索引库文档对象
        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();

        //2.封装专辑信息（包括标签列表）
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


        //3.封装分类信息
        //3.1 远程调用专辑服务获取分类信息
        BaseCategoryView baseCategoryView = albumFeignClient.getCategoryView(albumInfo.getCategory3Id()).getData();
        Assert.notNull(baseCategoryView, "专辑：{}下分类{}不存在", albumId, albumInfo.getCategory3Id());
        //3.2 封装1,2分类ID属性
        albumInfoIndex.setCategory1Id(baseCategoryView.getCategory1Id());
        albumInfoIndex.setCategory2Id(baseCategoryView.getCategory2Id());

        //4.封装主播信息
        UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(albumInfo.getUserId()).getData();
        Assert.notNull(userInfoVo, "专辑：{}主播：{}信息为空", albumId, albumInfo.getUserId());
        albumInfoIndex.setAnnouncerName(userInfoVo.getNickname());

        //5.封装统计信息 TODO 暂时采用随机值，后续改为远程调用
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
}
