package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.atguigu.tingshu.album.mapper.AlbumAttributeValueMapper;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.service.AlbumAttributeValueService;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.album.service.AuditService;
import com.atguigu.tingshu.common.cache.GuiGuCache;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumAttributeValueVo;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.atguigu.tingshu.common.constant.RedisConstant.ALBUM_BLOOM_FILTER;
import static com.atguigu.tingshu.common.constant.SystemConstant.*;
import static com.atguigu.tingshu.common.result.ResultCodeEnum.ALBUM_NODE_ERROR;
import static com.atguigu.tingshu.common.result.ResultCodeEnum.NODE_ERROR;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

    @Autowired
    private AlbumInfoMapper albumInfoMapper;

    @Autowired
    private AlbumAttributeValueService albumAttributeValueService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private RabbitService rabbitService;

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
        if (ALBUM_PAY_TYPE_VIPFREE.equals(payType) || ALBUM_PAY_TYPE_REQUIRE.equals(payType)) {
            albumInfo.setTracksForFree(5);
        }
        //1.3 调用持久层保存专辑,保存后得到专辑ID
        baseMapper.insert(albumInfo);
        Long albumInfoId = albumInfo.getId();
        //2.保存专辑标签关系
        //2.1 获取到前端提交标签关系列表
        List<AlbumAttributeValueVo> albumAttributeValueVoList = albumInfoVo.getAlbumAttributeValueVoList();
        if (CollUtil.isNotEmpty(albumAttributeValueVoList)) {
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

        // 4. 对新增专辑中文本：标题跟简介需要进行内容校验
        String text = albumInfo.getAlbumTitle() + albumInfo.getAlbumIntro();
        String suggest = auditService.auditText(text);
        if ("block".equals(suggest)) {
            albumInfo.setStatus(ALBUM_STATUS_NO_PASS);
        } else if ("review".equals(suggest)) {
            albumInfo.setStatus(ALBUM_STATUS_ARTIFICIAL);
        } else if ("pass".equals(suggest)) {
            albumInfo.setStatus(ALBUM_STATUS_PASS);
            //TODO 5.同步将专辑信息保存到Elasticsearch索引库
            //采用RabbitMQ可靠性消息 异步方式
            rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM, MqConst.ROUTING_ALBUM_UPPER, albumInfoId);
        }
        albumInfoMapper.updateById(albumInfo);
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
     *
     * @param pageInfo       分页对象
     * @param albumInfoQuery 查询条件
     * @return 分页对象
     */
    @Override
    public IPage<AlbumListVo> findUserAlbumPageByUserId(IPage<AlbumListVo> pageInfo, AlbumInfoQuery albumInfoQuery) {
        //方式一：先分页条件查询专辑列表，再遍历专辑列表，根据专辑ID分别查询每个专辑统计信息 弊端：执行SQL次数太多
        //方式二：调用持久层执行动态SQL分页查询获取结果
        return albumInfoMapper.findUserAlbumPageByUserId(pageInfo, albumInfoQuery);
    }

    @Autowired
    private TrackInfoMapper trackInfoMapper;

    /**
     * 删除指定专辑
     *
     * @param id 专辑ID
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAlbumInfo(Long id) {
        //1.判断该专辑下是否关联有声音，如果存在则不允许删除
        Long count = trackInfoMapper.selectCount(new LambdaQueryWrapper<TrackInfo>().eq(TrackInfo::getAlbumId, id));
        if (count > 0) {
            throw new GuiguException(ALBUM_NODE_ERROR);
        }
        //2.根据专辑主键ID 删除专辑
        albumInfoMapper.deleteById(id);

        //3.删除专辑标签关系
        albumAttributeValueService.remove(
                new LambdaQueryWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId, id)
        );

        //4.删除专辑统计信息
        albumStatMapper.delete(
                new LambdaQueryWrapper<AlbumStat>().eq(AlbumStat::getAlbumId, id)
        );

        //TODO 同时将存在在ES索引库中专辑一并删除
        rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM, MqConst.ROUTING_ALBUM_LOWER, id);
    }

    /**
     * 查询专辑信息（包含标签列表）
     *
     * @param id
     * @return
     */
    @Override
    @GuiGuCache(prefix = RedisConstant.ALBUM_INFO_PREFIX, ttl = RedisConstant.ALBUM_TIMEOUT, timeUnit = TimeUnit.SECONDS)
    public AlbumInfo getAlbumInfoFromDB(Long id) {
        //1.根据专辑ID查询专辑信息
        AlbumInfo albumInfo = this.getById(id);
        //2.根据专辑ID查询标签关系列表 将列表封装到信息对象中
        List<AlbumAttributeValue> albumAttributeValueList = albumAttributeValueService.list(
                new LambdaQueryWrapper<AlbumAttributeValue>()
                        .eq(AlbumAttributeValue::getAlbumId, id)
        );
        if (CollUtil.isNotEmpty(albumAttributeValueList)) {
            albumInfo.setAlbumAttributeValueVoList(albumAttributeValueList);
        }
        return albumInfo;
    }

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 查询专辑信息，引入缓存提升性能 采用分布式锁避免缓存击穿问题
     *
     * @param id
     * @return
     */
    @Override
    public AlbumInfo getAlbumInfo(Long id) {
        try {
            //1.优先从Redis获取业务数据 命中则直接返回结果
            //1.1 定义业务数据Key 形式=前缀+业务数据标识
            String dataKey = RedisConstant.ALBUM_INFO_PREFIX + id;
            //1.2 从Redis获取业务数据
            AlbumInfo albumInfo = (AlbumInfo) redisTemplate.opsForValue().get(dataKey);
            if (albumInfo != null) {
                return albumInfo;
            }

            //TODO 直接查数据库 并发请求同时请求
            //2.如果缓存未命中，获取分布式锁
            //2.1 定义锁的Key 形式=业务Key+锁后缀 锁粒度尽可能细
            String lockKey = dataKey + RedisConstant.CACHE_LOCK_SUFFIX;
            //2.2 创建锁对象
            RLock lock = redissonClient.getLock(lockKey);

            //2.2 尝试获取分布式锁 p1:获取锁等待时间  p2:加锁成功后锁释放时间 p3:时间单位
            boolean flag = lock.tryLock(
                    RedisConstant.ALBUM_LOCK_WAIT_PX1,
                    RedisConstant.ALBUM_LOCK_EXPIRE_PX2,
                    TimeUnit.SECONDS
            );
            //3.获取锁成功，则执行业务（查询DB，放入Redis,释放锁）
            if (flag) {
                try {
                    //3.1 去执行数据库查询
                    albumInfo = this.getAlbumInfoFromDB(id);
                    //3.2 将查询数据放入缓存 解决缓存雪崩问题，缓存有效时间设置为：基础时间+随机时间
                    long ttl = RedisConstant.ALBUM_TIMEOUT + RandomUtil.randomInt(300, 600);
                    redisTemplate.opsForValue().set(dataKey, albumInfo, ttl, TimeUnit.SECONDS);
                    return albumInfo;
                } finally {
                    //3.3 业务结束释放锁
                    lock.unlock();
                }
            } else {
                //4.获取锁失败，自旋
                TimeUnit.MILLISECONDS.sleep(50);
                return this.getAlbumInfo(id);
            }
        } catch (Exception e) {
            log.error("Redis服务不可用", e);
            //如果Redis服务有异常，兜底处理：直接查询数据库
            return this.getAlbumInfoFromDB(id);
        }
    }

    /**
     * 修改专辑
     *
     * @param id          专辑ID
     * @param albumInfoVo 修改专辑VO信息
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAlbumInfo(Long id, AlbumInfoVo albumInfoVo) {
        //1.修改专辑信息
        //1.1 将专辑VO信息转为实体PO对象
        AlbumInfo albumInfo = BeanUtil.copyProperties(albumInfoVo, AlbumInfo.class);
        //1.2 封装专辑ID
        albumInfo.setId(id);
        //审核状态：修改为未审核
        albumInfo.setStatus(ALBUM_STATUS_NO_PASS);
        //1.3 更新专辑
        this.updateById(albumInfo);

        //2.修改专辑标签关系
        //2.1 删除原有标签列表
        albumAttributeValueService.remove(
                new LambdaQueryWrapper<AlbumAttributeValue>()
                        .eq(AlbumAttributeValue::getAlbumId, id)
        );
        //2.2 将信提交标签列表封装成标签关系列表
        List<AlbumAttributeValueVo> albumAttributeValueVoList = albumInfoVo.getAlbumAttributeValueVoList();
        if (CollUtil.isNotEmpty(albumAttributeValueVoList)) {
            List<AlbumAttributeValue> albumAttributeValueList = albumAttributeValueVoList
                    .stream()
                    .map(vo -> {
                        AlbumAttributeValue albumAttributeValue = BeanUtil.copyProperties(vo, AlbumAttributeValue.class);
                        albumAttributeValue.setAlbumId(id);
                        return albumAttributeValue;
                    }).collect(Collectors.toList());
            albumAttributeValueService.saveBatch(albumAttributeValueList);
        }
        // 3. 对修改专辑中文本：标题跟简介需要进行内容校验
        String text = albumInfo.getAlbumTitle() + albumInfo.getAlbumIntro();
        String suggest = auditService.auditText(text);
        if ("block".equals(suggest)) {
            albumInfo.setStatus(ALBUM_STATUS_NO_PASS);
            rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM, MqConst.ROUTING_ALBUM_LOWER, id);
        } else if ("review".equals(suggest)) {
            albumInfo.setStatus(ALBUM_STATUS_ARTIFICIAL);
        } else if ("pass".equals(suggest)) {
            albumInfo.setStatus(ALBUM_STATUS_PASS);
            rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM, MqConst.ROUTING_ALBUM_UPPER, id);
        }
        albumInfoMapper.updateById(albumInfo);
    }

    /**
     * 查询当前用户发布专辑列表
     *
     * @param userId
     * @return
     */
    @Override
    public List<AlbumInfo> findUserAllAlbumList(Long userId) {
        LambdaQueryWrapper<AlbumInfo> queryWrapper = new LambdaQueryWrapper<AlbumInfo>()
                .select(AlbumInfo::getId, AlbumInfo::getAlbumTitle)
                .eq(AlbumInfo::getUserId, userId)
                .orderByDesc(AlbumInfo::getId)
                .last("limit 200");
        return albumInfoMapper.selectList(queryWrapper);
    }

    /**
     * 根据专辑ID查询统计信息
     *
     * @param albumId 专辑ID
     * @return 统计VO对象
     */
    @Override
    @GuiGuCache(prefix = RedisConstant.ALBUM_INFO_PREFIX + "stat:")
    public AlbumStatVo getAlbumStatVo(Long albumId) {
        return albumInfoMapper.getAlbumStatVo(albumId);
    }

    /**
     * 在项目维护期间，重建/扩容布隆过滤器
     */
    @Override
    public void rebuildBloomFilter() {
        //1.获取原有的布隆过滤器对象 得到配置信息
        RBloomFilter<Long> oldBloomFilter = redissonClient.getBloomFilter(ALBUM_BLOOM_FILTER);
        long expectedInsertions = oldBloomFilter.getExpectedInsertions();
        double falseProbability = oldBloomFilter.getFalseProbability();
        //非精确数量
        long count = oldBloomFilter.count();
        //2.判断现有数据规模是否超过期望数据规模
        //3.超过：扩容重建
        if (count > 1000) {
            //3.1 删除旧布隆过滤器
            oldBloomFilter.delete();
            //3.1 创建初始化新布隆过滤器 旧期望数据规模*2 其他配置保留
            RBloomFilter<Long> newBloomFilter = redissonClient.getBloomFilter(ALBUM_BLOOM_FILTER + ":new");
            newBloomFilter.tryInit(expectedInsertions * 2, falseProbability);

            //3.2 查询DB过审专辑ID列表，将专辑ID存入新布隆过滤器
            List<AlbumInfo> list = this.list(
                    new LambdaQueryWrapper<AlbumInfo>()
                            .eq(AlbumInfo::getStatus, SystemConstant.ALBUM_STATUS_PASS)
                            .select(AlbumInfo::getId)
            );
            if (CollUtil.isNotEmpty(list)) {
                for (AlbumInfo albumInfo : list) {
                    newBloomFilter.add(albumInfo.getId());
                }
                //3.3 重命名改为原来布隆过滤器名称
                newBloomFilter.rename(ALBUM_BLOOM_FILTER);
            }
        } else {
            //4.未超过：重建即可
            //4.1 删除旧布隆过滤器
            oldBloomFilter.delete();
            //4.1 创建初始化新布隆过滤器 旧期望数据规模*2 其他配置保留
            RBloomFilter<Long> newBloomFilter = redissonClient.getBloomFilter(ALBUM_BLOOM_FILTER + ":new");
            newBloomFilter.tryInit(expectedInsertions, falseProbability);

            //4.2 查询DB过审专辑ID列表，将专辑ID存入新布隆过滤器
            List<AlbumInfo> list = this.list(
                    new LambdaQueryWrapper<AlbumInfo>()
                            .eq(AlbumInfo::getStatus, SystemConstant.ALBUM_STATUS_PASS)
                            .select(AlbumInfo::getId)
            );
            if (CollUtil.isNotEmpty(list)) {
                for (AlbumInfo albumInfo : list) {
                    newBloomFilter.add(albumInfo.getId());
                }
                //3.3 重命名改为原来布隆过滤器名称
                newBloomFilter.rename(ALBUM_BLOOM_FILTER);
            }
        }
    }
}
