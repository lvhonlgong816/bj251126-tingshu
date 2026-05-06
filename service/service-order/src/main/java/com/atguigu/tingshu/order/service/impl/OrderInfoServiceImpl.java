package com.atguigu.tingshu.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.order.helper.SignHelper;
import com.atguigu.tingshu.order.mapper.OrderInfoMapper;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.atguigu.tingshu.common.constant.SystemConstant.*;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private AlbumFeignClient albumFeignClient;


    /**
     * 订单结算（会员套餐、专辑、声音）
     *
     * @param tradeVo 交易vo信息 包含：购买项目类型、项目ID、购买声音数量
     * @return 订单VO信息
     */
    @Override
    public OrderInfoVo trade(Long userId, TradeVo tradeVo) {
        //1.初始化VO对象 以及价格相关属性、商品相关集合属性
        OrderInfoVo orderInfoVo = new OrderInfoVo();
        //1.1 声明三个价格 "0.00"
        BigDecimal originalAmount = new BigDecimal("0.00");
        BigDecimal orderAmount = new BigDecimal("0.00");
        BigDecimal derateAmount = new BigDecimal("0.00");
        //1.2 声明2个集合：商品明细、优惠列表
        List<OrderDetailVo> orderDetailVoList = new ArrayList<>();
        List<OrderDerateVo> orderDerateVoList = new ArrayList<>();

        //付款项目类型: 1001-专辑 1002-声音 1003-vip会员
        String itemType = tradeVo.getItemType();
        //2.处理项目类型是：VIP套餐
        if (ORDER_ITEM_TYPE_VIP.equals(itemType)) {
            //2.1 远程调用"用户"服务获取套餐详情得到商品及价格信息
            VipServiceConfig vipServiceConfig = userFeignClient.getVipServiceConfig(tradeVo.getItemId()).getData();
            Assert.notNull(vipServiceConfig, "套餐：{}不存在", tradeVo.getItemId());
            //2.2 给“价格相关”属性赋值
            originalAmount = vipServiceConfig.getPrice();
            orderAmount = vipServiceConfig.getDiscountPrice();
            if (originalAmount.compareTo(orderAmount) == 1) {
                derateAmount = originalAmount.subtract(orderAmount);
            }
            //2.3 给商品明细、优惠列表属性赋值
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemId(tradeVo.getItemId());
            orderDetailVo.setItemName("套餐：" + vipServiceConfig.getName());
            orderDetailVo.setItemUrl(vipServiceConfig.getImageUrl());
            orderDetailVo.setItemPrice(vipServiceConfig.getPrice());
            orderDetailVoList.add(orderDetailVo);

            if (originalAmount.compareTo(orderAmount) == 1) {
                OrderDerateVo orderDerateVo = new OrderDerateVo();
                //订单减免类型 1405-专辑折扣 1406-VIP服务折
                orderDerateVo.setDerateType(ORDER_DERATE_VIP_SERVICE_DISCOUNT);
                orderDerateVo.setDerateAmount(derateAmount);
                orderDerateVo.setRemarks("限时套餐优惠");
                orderDerateVoList.add(orderDerateVo);
            }

        } else if (ORDER_ITEM_TYPE_ALBUM.equals(itemType)) {
            //3. 处理项目类型是：专辑
            //3.1 远程调用"用户服务"判断是否重复购买专辑
            Long albumId = tradeVo.getItemId();
            Boolean flag = userFeignClient.isPaidAlbum(albumId).getData();
            if (flag) {
                throw new GuiguException(500, "您已购买本专辑，请勿重复购买");
            }
            //3.2 远程调用"专辑"服务获取专辑信息,得到价格、以及折扣（普通用户，VIP折扣）
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(albumId).getData();
            Assert.notNull(albumInfo, "专辑信息{}不存在", albumId);
            BigDecimal price = albumInfo.getPrice();
            BigDecimal discount = albumInfo.getDiscount();
            BigDecimal vipDiscount = albumInfo.getVipDiscount();

            //3.3 远程调用"用户服务"获取用户身份用于算价
            UserInfoVo userInfoVo = userFeignClient.getUserInfoVo(userId).getData();
            Assert.notNull(userInfoVo, "用户：{}不存在", userId);
            Boolean isVIP = false;
            if (userInfoVo.getIsVip().intValue() == 1
                    && userInfoVo.getVipExpireTime().after(new Date())) {
                isVIP = true;
            }
            //3.4 封装"商品"相关价格
            originalAmount = price;
            orderAmount = originalAmount;
            //3.4.1 如果存在普通用户折扣且当前用户为普通用户
            if (!isVIP && discount.doubleValue() != -1) {
                orderAmount = originalAmount.multiply(discount)
                        .divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
            }
            //3.4.2 如果存在会员用户折扣且当前用户为VIP用户
            if (isVIP && vipDiscount.doubleValue() != -1) {
                orderAmount = originalAmount.multiply(vipDiscount)
                        .divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
            }
            if (originalAmount.compareTo(orderAmount) == 1) {
                derateAmount = originalAmount.subtract(orderAmount);
            }
            //3.5 封装"商品"列表及商品优惠列表
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemId(albumId);
            orderDetailVo.setItemName("专辑：" + albumInfo.getAlbumTitle());
            orderDetailVo.setItemUrl(albumInfo.getCoverUrl());
            orderDetailVo.setItemPrice(originalAmount);
            orderDetailVoList.add(orderDetailVo);

            if (originalAmount.compareTo(orderAmount) == 1) {
                OrderDerateVo orderDerateVo = new OrderDerateVo();
                orderDerateVo.setDerateType(ORDER_DERATE_ALBUM_DISCOUNT);
                orderDerateVo.setDerateAmount(derateAmount);
                orderDerateVo.setRemarks("专辑限时优惠");
                orderDerateVoList.add(orderDerateVo);
            }
        } else if (ORDER_ITEM_TYPE_TRACK.equals(itemType)) {
            //4. 处理项目类型是：声音
            //4.1 远程调用"专辑服务"获取待购买声音列表，将声音作为商品展示结算页
            Long trackId = tradeVo.getItemId();
            List<TrackInfo> waitBuyTrackInfoList = albumFeignClient.findPaidTrackInfoList(trackId, tradeVo.getTrackCount()).getData();
            Assert.notNull(waitBuyTrackInfoList, "暂无结算声音");
            //4.2 远程调用"专辑服务"获取声音单价
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfo(waitBuyTrackInfoList.get(0).getAlbumId()).getData();
            Assert.notNull(albumInfo, "专辑：{}不存在");
            BigDecimal price = albumInfo.getPrice();
            //4.3 计算订单声音相关价格 注意：声音不支持折扣
            originalAmount = price.multiply(BigDecimal.valueOf(waitBuyTrackInfoList.size()));
            orderAmount = originalAmount;

            //4.4 封装"商品列表"
            orderDetailVoList = waitBuyTrackInfoList
                    .stream()
                    .map(trackInfo -> {
                        OrderDetailVo orderDetailVo = new OrderDetailVo();
                        orderDetailVo.setItemId(trackInfo.getId());
                        orderDetailVo.setItemName("声音："+trackInfo.getTrackTitle());
                        orderDetailVo.setItemUrl(trackInfo.getCoverUrl());
                        orderDetailVo.setItemPrice(price);
                        return orderDetailVo;
                    }).collect(Collectors.toList());

        }

        //5.封装订单VO对象属性
        //5.1 封装价格有关3个属性
        orderInfoVo.setOriginalAmount(originalAmount);
        orderInfoVo.setOrderAmount(orderAmount);
        orderInfoVo.setDerateAmount(derateAmount);
        //5.2 封装商品有关2个集合属性
        orderInfoVo.setOrderDetailVoList(orderDetailVoList);
        orderInfoVo.setOrderDerateVoList(orderDerateVoList);
        //5.3  其他信息封装：购买类型、流水号、时间戳、签名
        //5.3.1 设置购买类型
        orderInfoVo.setItemType(tradeVo.getItemType());
        //5.3.2 流水号机制：生成本次订单流水号 解决：订单重复提交
        String tradeKey = RedisConstant.ORDER_TRADE_NO_PREFIX+userId;
        String tradeNo = IdUtil.fastUUID();
        redisTemplate.opsForValue().set(tradeKey, tradeNo, 5, TimeUnit.MINUTES);
        orderInfoVo.setTradeNo(tradeNo);
        //5.3.3 生成本次 时间戳，订单签名 解决：数据被抓包篡改
        orderInfoVo.setTimestamp(System.currentTimeMillis());
        //5.3.4 由于目前无法确定支付方式 生成签名 将订单vo付款方式去掉
        //将订单VO转为Map 付款方式payway去除掉
        Map<String, Object> map = BeanUtil.beanToMap(orderInfoVo, false, true);
        String sign = SignHelper.getSign(map);
        orderInfoVo.setSign(sign);

        //6.返回订单vo对象
        return orderInfoVo;
    }

    @Autowired
    private RedisTemplate redisTemplate;


}
