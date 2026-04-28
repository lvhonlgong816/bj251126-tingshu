package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Assert;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackStatMapper;
import com.atguigu.tingshu.album.service.AuditService;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.album.*;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.atguigu.tingshu.common.constant.SystemConstant.*;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService {

    @Autowired
    private TrackInfoMapper trackInfoMapper;

    @Autowired
    private AlbumInfoMapper albumInfoMapper;


    @Autowired
    private VodService vodService;

    @Autowired
    private AuditService auditService;

    /**
     * 保存声音
     *
     * @param trackInfoVo 声音信息vo
     * @param userId      用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTrackInfo(TrackInfoVo trackInfoVo, Long userId) {
        //1.根据专辑ID查询专辑信息 用于更新声音数量
        AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfoVo.getAlbumId());
        //2.保存声音记录、更新专辑内包含声音数量
        //2.1 将声音VO转为PO对象
        TrackInfo trackInfo = BeanUtil.copyProperties(trackInfoVo, TrackInfo.class);
        //2.2 封装声音属性信息
        //2.2.1 基础：用户ID、状态、来源、封面图片
        trackInfo.setUserId(userId);
        trackInfo.setStatus(SystemConstant.TRACK_STATUS_NO_PASS);
        if (StringUtils.isBlank(trackInfoVo.getCoverUrl())) {
            trackInfo.setCoverUrl(albumInfo.getCoverUrl());
        }
        trackInfo.setSource(SystemConstant.TRACK_SOURCE_USER);
        //2.2.2 设置序号=专辑包含声音数量+1
        trackInfo.setOrderNum(albumInfo.getIncludeTrackCount() + 1);
        //2.2.2 从点播平台获取，声音时长、大小、类型
        TrackMediaInfoVo mediaInfoVo = vodService.getMediaInfo(trackInfo.getMediaFileId());
        if (mediaInfoVo != null) {
            trackInfo.setMediaDuration(BigDecimal.valueOf(mediaInfoVo.getDuration()));
            trackInfo.setMediaSize(mediaInfoVo.getSize());
            trackInfo.setMediaType(mediaInfoVo.getType());
        }
        //2.3 保存声音得到声音ID
        trackInfoMapper.insert(trackInfo);
        Long trackInfoId = trackInfo.getId();

        //2.4 更新专辑包含声音数量
        albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() + 1);
        albumInfoMapper.updateById(albumInfo);

        //3.新增统计信息
        this.saveTrackStat(trackInfoId, SystemConstant.TRACK_STAT_PLAY, 0);
        this.saveTrackStat(trackInfoId, SystemConstant.TRACK_STAT_COLLECT, 0);
        this.saveTrackStat(trackInfoId, SystemConstant.TRACK_STAT_PRAISE, 0);
        this.saveTrackStat(trackInfoId, SystemConstant.TRACK_STAT_COMMENT, 0);

        //4.对声音中文本进行内容审核
        String text = trackInfo.getTrackTitle() + trackInfo.getTrackIntro();
        String suggest = auditService.auditText(text);
        if ("block".equals(suggest)) {
            trackInfo.setStatus(TRACK_STATUS_NO_PASS);
        } else if ("review".equals(suggest)) {
            trackInfo.setStatus(TRACK_STATUS_ARTIFICIAL);
        } else if ("pass".equals(suggest)) {
            trackInfo.setStatus(TRACK_STATUS_PASS);
            //5.对上传的声音文件发起审核任务ID，关联审核任务ID
            String taskId = auditService.startReviewTask(trackInfo.getMediaFileId());
            trackInfo.setReviewTaskId(taskId);
            trackInfo.setStatus(TRACK_STATUS_REVIEWING);
        }
        trackInfoMapper.updateById(trackInfo);
    }


    @Autowired
    private TrackStatMapper trackStatMapper;

    @Override
    public void saveTrackStat(Long trackId, String statType, int statNum) {
        TrackStat trackStat = new TrackStat();
        trackStat.setTrackId(trackId);
        trackStat.setStatType(statType);
        trackStat.setStatNum(statNum);
        trackStatMapper.insert(trackStat);
    }

    /**
     * 条件分页查询当前用户声音列表
     *
     * @param pageInfo       分页对象
     * @param trackInfoQuery 查询条件
     * @return 分页对象
     */
    @Override
    public Page<TrackListVo> findUserTrackPage(Page<TrackListVo> pageInfo, TrackInfoQuery trackInfoQuery) {
        return trackInfoMapper.findUserTrackPage(pageInfo, trackInfoQuery);
    }

    /**
     * 修改声音信息
     *
     * @param id          声音Id
     * @param trackInfoVo 声音信息VO
     * @return
     */
    @Override
    public void updateTrackInfo(Long id, TrackInfoVo trackInfoVo) {
        Boolean isNeedReview = false;
        //1.判断音频文件是否更新，如果更新，再次获取新音频文件详情，TODO 对新音频再次进行审核
        //1.1 根据声音ID查询声音信息，获取原来的音频ID
        TrackInfo trackInfo = this.getById(id);
        String oldMediaFileId = trackInfo.getMediaFileId();
        //1.2 封装更新后：标题、简介、封面图片、音频URL、唯一标识
        BeanUtil.copyProperties(trackInfoVo, trackInfo);
        //1.3 对比声音vo中音频ID判断是否变更
        if (!oldMediaFileId.equals(trackInfoVo.getMediaFileId())) {
            //1.4 如果变更，再次调用平台接口获取音频详情，更新音频相关信息：时长、大小、类型、播放地址、
            TrackMediaInfoVo mediaInfo = vodService.getMediaInfo(trackInfoVo.getMediaFileId());
            trackInfo.setMediaDuration(BigDecimal.valueOf(mediaInfo.getDuration()));
            trackInfo.setMediaType(mediaInfo.getType());
            trackInfo.setMediaSize(mediaInfo.getSize());
            //4.5 将旧音频文件从点播平台删除
            vodService.deleteMedia(oldMediaFileId);
            isNeedReview = true;
        }

        //2.更新声音信息，修改后文本同样需要进行内容审核
        trackInfo.setStatus(SystemConstant.TRACK_STATUS_NO_PASS);

        //4.对声音中文本进行内容审核
        String text = trackInfo.getTrackTitle() + trackInfo.getTrackIntro();
        String suggest = auditService.auditText(text);
        if ("block".equals(suggest)) {
            trackInfo.setStatus(TRACK_STATUS_NO_PASS);
        } else if ("review".equals(suggest)) {
            trackInfo.setStatus(TRACK_STATUS_ARTIFICIAL);
        } else if ("pass".equals(suggest)) {
            trackInfo.setStatus(TRACK_STATUS_PASS);
            if (isNeedReview) {
                String taskId = auditService.startReviewTask(trackInfo.getMediaFileId());
                trackInfo.setReviewTaskId(taskId);
                trackInfo.setStatus(TRACK_STATUS_REVIEWING);
            }
        }
        trackInfoMapper.updateById(trackInfo);
    }

    /**
     * 删除声音
     *
     * @param id 声音ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTrackInfo(Long id) {
        //1.根据声音ID查询声音信息，得到专辑ID、声音序号
        TrackInfo trackInfo = this.getById(id);
        Long albumId = trackInfo.getAlbumId();
        Integer orderNum = trackInfo.getOrderNum();

        //2.根据专辑ID查询专辑信息，用于 更新专辑声音数量
        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() - 1);
        albumInfoMapper.updateById(albumInfo);

        //3.先更新专辑下大于被删声音序号 确保声音需要连续
        trackInfoMapper.update(
                null,
                new LambdaUpdateWrapper<TrackInfo>()
                        .eq(TrackInfo::getAlbumId, albumId)
                        .gt(TrackInfo::getOrderNum, orderNum)
                        .setSql("order_num = order_num - 1 ")
        );

        //4.删除声音记录
        trackInfoMapper.deleteById(id);

        // 5.删除统计信息
        trackStatMapper.delete(
                new LambdaQueryWrapper<TrackStat>().eq(TrackStat::getTrackId, id)
        );
        //6.从点播平台删除音频文件
        vodService.deleteMedia(trackInfo.getMediaFileId());
    }

    @Autowired
    private UserFeignClient userFeignClient;

    /**
     * 该接口未登录，返回声音列表 如果用户已登录，根据当前用户身份、购买情况、专辑付费类型综合判断付费标识
     * 分页获取专辑声音列表（动态判断付费标识）
     *
     * @param albumId  专辑ID
     * @param pageInfo 分页对象
     * @param userId   用户ID
     * @return
     */
    @Override
    public Page<AlbumTrackListVo> findAlbumTrackPage(Page<AlbumTrackListVo> pageInfo, Long albumId, Long userId) {
        //1.分页查询专辑包含声音列表 暂时不考虑付费标识 默认isShowPaidMark=false
        pageInfo = trackInfoMapper.findAlbumTrackPage(pageInfo, albumId);

        //2.根据专辑ID查询专辑信息 得到专辑付费类型、试听集数
        AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
        //付费类型: 0101-免费、0102-vip免费、0103-付费
        String payType = albumInfo.getPayType();
        //专辑下试听集数
        Integer tracksForFree = albumInfo.getTracksForFree();

        //TODO 处理付费标识 关键找出需要将付费标识改为True情形
        //3处理用户未登录情况 情形一：专辑付费类型是 VIP免费或付费 除去试听以外声音展示付费标识
        if (userId == null) {
            if (ALBUM_PAY_TYPE_VIPFREE.equals(payType) || ALBUM_PAY_TYPE_REQUIRE.equals(payType)) {
                pageInfo
                        .getRecords()
                        //过滤掉试听声音
                        .stream().filter(track -> track.getOrderNum() > tracksForFree)
                        .forEach(track -> track.setIsShowPaidMark(true));
            }
        }else {
            //4 TODO 处理已登录情况 结合专辑付费类型、用户身份、用户购买情况
            //4.1 远程调用 用户服务 获取当前用户身份 确定是普通用户还是VIP用户
            Boolean isVIP = false;
            UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(userId).getData();
            Assert.notNull(userInfoVo, "当前用户:{}不存在", userId);
            //判断会员：会员标识为1且过期时间大于当前时间
            if (userInfoVo.getIsVip().intValue() == 1
                    && userInfoVo.getVipExpireTime().after(new Date())) {
                isVIP = true;
            }
            Boolean isNeedCheckPayState = false;
            //4.2 情形2：普通用户 查看 付费类型是VIP免费专辑 默认无权益 但需要进一步查看购买情况
            if (!isVIP && ALBUM_PAY_TYPE_VIPFREE.equals(payType)) {
                isNeedCheckPayState = true;
            }

            //4.3 情形3：所有用户（普通+VIP会员用户） 查看 付费类型是 付费   默认无权益  同样需要进一步查看购买情况
            if(ALBUM_PAY_TYPE_REQUIRE.equals(payType)){
                isNeedCheckPayState = true;
            }
            //4.4 如果满足需要查看购买情况条件，远程调用 用户服务 查询当前页 除试听以外 每个声音购买状态
            if(isNeedCheckPayState){
                //4.4.1 获取当前页中需要验证购买状态声音ID列表（除去试听声音ID）
                List<Long> needCheckPayStateTrackIdList = pageInfo
                        .getRecords()
                        .stream()
                        .filter(track -> track.getOrderNum() > tracksForFree)
                        .map(AlbumTrackListVo::getTrackId)
                        .collect(Collectors.toList());
                //4.4.2 远程调用 用户服务 获取本页中 待检查声音购买状态 Map中Value 1：已购买 0：未购买
                Map<Long, Integer> payStateMap = userFeignClient.userIsPaidTrack(userId, albumId, needCheckPayStateTrackIdList).getData();
                //4.5 除试听以外 声音 凡是未购买声音 将付费标识改为：true
                pageInfo.getRecords()
                        .stream()
                        .filter(track -> track.getOrderNum() > tracksForFree)
                        // 从购买状态Map获取结果如果为0 说明 未购买  将付费标识改为true
                        .forEach(track -> track.setIsShowPaidMark(payStateMap.get(track.getTrackId())==0));
            }
        }
        return pageInfo;
    }

    @Autowired
    private AlbumStatMapper albumStatMapper;

    /**
     * 更新声音以及所属专辑统计设置
     * @param mqVo
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStat(TrackStatMqVo mqVo) {
        //1.更新声音统计表
        trackStatMapper.update(
                null,
                new LambdaUpdateWrapper<TrackStat>()
                        .eq(TrackStat::getTrackId, mqVo.getTrackId())
                        .eq(TrackStat::getStatType, mqVo.getStatType())
                        .setSql("stat_num = stat_num +"+mqVo.getCount())
        );
        //2.如果是播放或评论统计类型 同时 更新所属专辑统计数值
        if(TRACK_STAT_PLAY.equals(mqVo.getStatType())){
            albumStatMapper.update(
                    null,
                    new LambdaUpdateWrapper<AlbumStat>()
                            .eq(AlbumStat::getAlbumId, mqVo.getAlbumId())
                            .eq(AlbumStat::getStatType, ALBUM_STAT_PLAY)
                            .setSql("stat_num = stat_num +"+mqVo.getCount())
            );
        }
        if(TRACK_STAT_COMMENT.equals(mqVo.getStatType())){
            albumStatMapper.update(
                    null,
                    new LambdaUpdateWrapper<AlbumStat>()
                            .eq(AlbumStat::getAlbumId, mqVo.getAlbumId())
                            .eq(AlbumStat::getStatType, ALBUM_STAT_COMMENT)
                            .setSql("stat_num = stat_num +"+mqVo.getCount())
            );
        }
    }


}
