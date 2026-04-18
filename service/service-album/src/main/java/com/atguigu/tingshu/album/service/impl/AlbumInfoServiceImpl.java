package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.atguigu.tingshu.album.mapper.AlbumAttributeValueMapper;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.service.AlbumAttributeValueService;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumAttributeValueVo;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.atguigu.tingshu.common.constant.SystemConstant.*;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

    @Autowired
    private AlbumInfoMapper albumInfoMapper;

    @Autowired
    private AlbumAttributeValueService albumAttributeValueService;

    /**
     * 保存专辑信息
     *
     * @param albumInfoVo 专辑VO
     * @param userId      用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)   //默认清空发生RuntimeException跟error才会进行回滚
    public void saveAlbumInfo(AlbumInfoVo albumInfoVo, Long userId) {
        //1.保存专辑信息
        //1.1 将专辑信息VO转为实体PO对象
        AlbumInfo albumInfo = BeanUtil.copyProperties(albumInfoVo, AlbumInfo.class);
        //1.2 封装一些属性 用户ID，免费试听集数，审核状态
        albumInfo.setUserId(userId);
        albumInfo.setStatus(ALBUM_STATUS_NO_PASS);
        //0101-免费、0102-vip免费、0103-付费
        String payType = albumInfo.getPayType();
        if(ALBUM_PAY_TYPE_VIPFREE.equals(payType) || ALBUM_PAY_TYPE_REQUIRE.equals(payType)){
            albumInfo.setTracksForFree(5);
        }
        //1.3 调用持久层保存专辑,保存后得到专辑ID
        baseMapper.insert(albumInfo);
        Long albumInfoId = albumInfo.getId();
        //2.保存专辑标签关系
        //2.1 获取到前端提交标签关系列表
        List<AlbumAttributeValueVo> albumAttributeValueVoList = albumInfoVo.getAlbumAttributeValueVoList();
        if(CollUtil.isNotEmpty(albumAttributeValueVoList)){
            //2.2 将标签关系列表VO转为实体PO对象 关联专辑ID
            List<AlbumAttributeValue> albumAttributeValueList = albumAttributeValueVoList
                    .stream()
                    .map(vo -> {
                        AlbumAttributeValue albumAttributeValue = BeanUtil.copyProperties(vo, AlbumAttributeValue.class);
                        albumAttributeValue.setAlbumId(albumInfoId);
                        return albumAttributeValue;
                    }).collect(Collectors.toList());
            //2.3 调用标签关系业务层对象批量保存
            albumAttributeValueService.saveBatch(albumAttributeValueList);
        }
        //3.为专辑新增4条统计信息
        this.saveAlbumInfoStat(albumInfoId, ALBUM_STAT_PLAY, 0);
        this.saveAlbumInfoStat(albumInfoId, ALBUM_STAT_SUBSCRIBE, 0);
        this.saveAlbumInfoStat(albumInfoId, ALBUM_STAT_BUY, 0);
        this.saveAlbumInfoStat(albumInfoId, ALBUM_STAT_COMMENT, 0);
    }

    @Autowired
    private AlbumStatMapper albumStatMapper;
    /**
     * 保存专辑统计信息
     *
     * @param albumId  专辑ID
     * @param statType 统计类型
     * @param statNum  统计数值 0401-播放量 0402-订阅量 0403-购买量 0403-评论数'
     */
    @Override
    public void saveAlbumInfoStat(Long albumId, String statType, int statNum) {
        AlbumStat albumStat = new AlbumStat();
        albumStat.setAlbumId(albumId);
        albumStat.setStatType(statType);
        albumStat.setStatNum(statNum);
        albumStatMapper.insert(albumStat);
    }

    /**
     * 分页条件查询指定用户专辑列表
     * @param pageInfo 分页对象
     * @param albumInfoQuery 查询条件
     * @return 分页对象
     */
    @Override
    public IPage<AlbumListVo> findUserAlbumPageByUserId(IPage<AlbumListVo> pageInfo, AlbumInfoQuery albumInfoQuery) {
        //方式一：先分页条件查询专辑列表，再遍历专辑列表，根据专辑ID分别查询每个专辑统计信息 弊端：执行SQL次数太多
        //方式二：调用持久层执行动态SQL分页查询获取结果
        return albumInfoMapper.findUserAlbumPageByUserId(pageInfo, albumInfoQuery);
    }
}
