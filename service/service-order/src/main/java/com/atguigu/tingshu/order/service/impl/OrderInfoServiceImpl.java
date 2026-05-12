package com.atguigu.tingshu.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.account.AccountFeignClient;
import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.handler.GlobalExceptionHandler;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.order.OrderDerate;
import com.atguigu.tingshu.model.order.OrderDetail;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.order.helper.SignHelper;
import com.atguigu.tingshu.order.mapper.OrderInfoMapper;
import com.atguigu.tingshu.order.pattern.TradeStrategy;
import com.atguigu.tingshu.order.pattern.factory.TradeStrategyFactory;
import com.atguigu.tingshu.order.service.OrderDerateService;
import com.atguigu.tingshu.order.service.OrderDetailService;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.atguigu.tingshu.vo.order.OrderDerateVo;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.atguigu.tingshu.common.constant.SystemConstant.*;
import static com.atguigu.tingshu.common.rabbit.constant.MqConst.*;

@Slf4j
@Service
@RefreshScope
@SuppressWarnings({"all"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private AlbumFeignClient albumFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private OrderDerateService orderDerateService;
    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    /**
     * @Value读取配置文件内容 配合@RefreshScope注解实现热更新
     */
    @Value("${order.cancel}")
    private Integer cancelOrderTTL;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private TradeStrategyFactory tradeStrategyFactory;

    /**
     * 订单结算（会员套餐、专辑、声音）
     *
     * @param tradeVo 交易vo信息 包含：购买项目类型、项目ID、购买声音数量
     * @return 订单VO信息
     */
    @Override
    public OrderInfoVo trade(Long userId, TradeVo tradeVo) {
        //1.根据付款项目类型获取对应的策略实现类对象
        TradeStrategy tradeStrategy = tradeStrategyFactory.getTradeStrategy(tradeVo.getItemType());
        //2.调用不同策略实现类对象进行订单结算
        return tradeStrategy.trade(tradeVo, userId);

       /* //1.初始化VO对象 以及价格相关属性、商品相关集合属性
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
        //TODO 目前采用传统if elseif判断 每增加一种新商品类型，都需要更改现有方法逻辑，维护不方便 违背编码开闭原则
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
                        orderDetailVo.setItemName("声音：" + trackInfo.getTrackTitle());
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
        String tradeKey = RedisConstant.ORDER_TRADE_NO_PREFIX + userId;
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
        return orderInfoVo;*/
    }

    @Autowired
    private AccountFeignClient accountFeignClient;


    /**
     * 提交/结算订单（处理余额支付逻辑）
     *
     * @param userId      用户ID
     * @param orderInfoVo 订单vo信息
     * @return {"orderNo":"本次订单保存后订单编号"} 用于后续对接微信支付或者展示订单详情
     */
    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public Map<String, String> submitOrder(Long userId, OrderInfoVo orderInfoVo) {
        //1.业务校验，验证流水号防止订单重提交 采用lua脚本保证判断跟删除原子性
        //1.1 定义key
        String tradeKey = RedisConstant.ORDER_TRADE_NO_PREFIX + userId;
        //1.2 创建脚本对象
        String scriptText = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                "then\n" +
                "    return redis.call(\"del\",KEYS[1])\n" +
                "else\n" +
                "    return 0\n" +
                "end";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(scriptText, Long.class);
        //1.3 执行lua脚本 传入需要KEYS 跟 AEGV 值
        Long result = (Long) redisTemplate.execute(redisScript, Arrays.asList(tradeKey), orderInfoVo.getTradeNo());
        if (result.intValue() == 0) {
            throw new GuiguException(500, "流水号校验失败");
        }

        //2.业务校验，验证签名防止数据被篡改 在结算订单接口中payWay吧并未参与签名，此时前端选择付款方式，故验签将"payWay"去掉
        //2.1 将订单VO转为Map 手动移除掉"payWay"
        Map<String, Object> map = BeanUtil.beanToMap(orderInfoVo);
        map.remove("payWay");
        //2.2 调用工具类验签
        SignHelper.checkSign(map);

        // 核心订单业务处理逻辑
        //3.保存订单相关数据（包括：订单、订单明细、优惠列表） 此时保存订单状态：未支付
        OrderInfo orderInfo = this.saveOrderInfo(userId, orderInfoVo);

        //4.如果支付方式为余额支付 立即扣减账户余额、余额扣减修改订单状态：已支付 并且发放权益
        //支付方式：1101-微信 1102-支付宝 1103-账户余额
        String payWay = orderInfoVo.getPayWay();
        if (ORDER_PAY_ACCOUNT.equals(payWay)) {
            //4.1 远程调用"账户服务"扣减账户余额
            //4.1.1 准备扣减余额vo参数
            AccountDeductVo vo = new AccountDeductVo();
            vo.setOrderNo(orderInfo.getOrderNo());
            vo.setUserId(orderInfo.getUserId());
            vo.setAmount(orderInfo.getOrderAmount());
            vo.setContent(orderInfo.getOrderTitle());
            //4.1.2 执行远程调用调用
            Result checkAndDeductResult = accountFeignClient.checkAndDeduct(vo);
            //4.1.3 判断业务状态码是否为200
            if (checkAndDeductResult.getCode().intValue() != 200) {
                throw new GuiguException(checkAndDeductResult.getCode(), checkAndDeductResult.getMessage());
            }

            //4.2 余额扣减成功，将订单状态改为：已支付
            orderInfo.setOrderStatus(ORDER_STATUS_PAID);
            orderInfoMapper.updateById(orderInfo);

            //4.3 远程调用"用户服务"进行相关权益发放（虚拟物品发货）
            //4.3.1 创建用于虚拟物品发货vo对象
            UserPaidRecordVo userPaidRecordVo = new UserPaidRecordVo();
            userPaidRecordVo.setOrderNo(orderInfo.getOrderNo());
            userPaidRecordVo.setUserId(orderInfo.getUserId());
            userPaidRecordVo.setItemType(orderInfo.getItemType());
            List<OrderDetailVo> orderDetailVoList = orderInfoVo.getOrderDetailVoList();
            if (CollUtil.isNotEmpty(orderDetailVoList)) {
                //获取订单明细中商品ID列表
                List<Long> itemIdList = orderDetailVoList.stream().map(OrderDetailVo::getItemId).collect(Collectors.toList());
                userPaidRecordVo.setItemIdList(itemIdList);
                //4.3.2 执行远程调用
                Result savePaidRecordResult = userFeignClient.savePaidRecord(userPaidRecordVo);
                //4.3.3 判断业务状态码是否为200
                //int i = 1/-0;
                if (savePaidRecordResult.getCode().intValue() != 200) {
                    throw new GuiguException(savePaidRecordResult.getCode(), savePaidRecordResult.getMessage());
                }
            }
        }

        //5.TODO 无论是哪种付款方式，采用延迟消息自动将超时未支付订单取消掉  自动关单时间阈值：15分钟
        //方案一：采用RabbitMQ延迟消息  方案二:采用定时任务  方案三：不做处理 当进行查询判断订单是否过期
        rabbitService.sendDelayMessage(EXCHANGE_CANCEL_ORDER, ROUTING_CANCEL_ORDER, orderInfo.getId(), cancelOrderTTL);

        //6.返回本次订单订单编号，用于后续支付成功后查询订单、或者基于订单编号对接微信支付
        return Map.of("orderNo", orderInfo.getOrderNo());
    }


    /**
     * 保存订单信息
     *
     * @param userId      用户ID
     * @param orderInfoVo 订单VO信息
     * @return 订单对象
     */
    @Override
    public OrderInfo saveOrderInfo(Long userId, OrderInfoVo orderInfoVo) {
        //1.保存订单信息
        //1.1 将订单VO转为订单PO对象
        OrderInfo orderInfo = BeanUtil.copyProperties(orderInfoVo, OrderInfo.class);
        //1.2 设置用户ID
        orderInfo.setUserId(userId);
        //1.3 设置订单标题
        List<OrderDetailVo> orderDetailVoList = orderInfoVo.getOrderDetailVoList();
        if (CollUtil.isNotEmpty(orderDetailVoList)) {
            String itemName = orderDetailVoList.get(0).getItemName();
            orderInfo.setOrderTitle(itemName);
        }
        //1.4 设置订单编号 要求：全局唯一趋势递增 形式=日期+雪花算法
        String orderNo = DateUtil.today().replaceAll("-", "") + IdUtil.getSnowflakeNextId();
        orderInfo.setOrderNo(orderNo);
        //1.2 设置订单状态：订单状态：0901-未支付 0902-已支付 0903-已取消
        orderInfo.setOrderStatus(ORDER_STATUS_UNPAID);
        //1.3 保存订单信息 得到订单ID
        orderInfoMapper.insert(orderInfo);
        Long orderId = orderInfo.getId();

        //2.保存订单明细信息
        if (CollUtil.isNotEmpty(orderDetailVoList)) {
            List<OrderDetail> orderDetailList = orderDetailVoList.stream().map(vo -> {
                OrderDetail orderDetail = BeanUtil.copyProperties(vo, OrderDetail.class);
                orderDetail.setOrderId(orderId);
                return orderDetail;
            }).collect(Collectors.toList());
            orderDetailService.saveBatch(orderDetailList);
        }

        //3.保存订单减免信息
        List<OrderDerateVo> orderDerateVoList = orderInfoVo.getOrderDerateVoList();
        if (CollUtil.isNotEmpty(orderDetailVoList)) {
            List<OrderDerate> orderDerateList = orderDerateVoList
                    .stream()
                    .map(vo -> {
                        OrderDerate orderDerate = BeanUtil.copyProperties(vo, OrderDerate.class);
                        orderDerate.setOrderId(orderId);
                        return orderDerate;
                    }).collect(Collectors.toList());
            orderDerateService.saveBatch(orderDerateList);
        }
        return orderInfo;
    }

    /**
     * 根据订单编号查询订单详情（包含订单明细列表，减免列表）
     *
     * @param orderNo
     * @return
     */
    @Override
    public OrderInfo getOrderInfo(String orderNo) {
        //1.根据订单编号查询订单信息
        OrderInfo orderInfo = orderInfoMapper.selectOne(
                new LambdaQueryWrapper<OrderInfo>()
                        .eq(OrderInfo::getOrderNo, orderNo)
        );
        Long orderId = orderInfo.getId();
        //2.根据订单ID查询订单明细
        List<OrderDetail> orderDetailList = orderDetailService.list(
                new LambdaQueryWrapper<OrderDetail>()
                        .eq(OrderDetail::getOrderId, orderId)
        );
        orderInfo.setOrderDetailList(orderDetailList);
        //3.根据订单ID查询订单减免
        List<OrderDerate> orderDerateList = orderDerateService.list(
                new LambdaQueryWrapper<OrderDerate>()
                        .eq(OrderDerate::getOrderId, orderId)
        );
        orderInfo.setOrderDerateList(orderDerateList);
        return orderInfo;
    }

    /**
     * 分页查询订单(包含订单明细、减免列表)
     *
     * @param pageInfo
     * @param userId
     * @return
     */
    @Override
    public Page<OrderInfo> findUserPage(Page<OrderInfo> pageInfo, Long userId) {
        //TODO 作业：获取本页订单，获取订单ID列表， 根据订单ID列表查询订单明细集合 转为 Map<订单ID, List<订单明细>> 组装订单中明细属性
        return orderInfoMapper.findUserPage(pageInfo, userId);
    }

    /**
     * 取消订单延迟消息：判断订单支付状态，如未支付，将订单修改为已关闭
     *
     * @param orderId
     */
    @Override
    public void cancelOrder(Long orderId) {
        //1.先查询订单
        //OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        //2.判断订单状态  存在问题：判断成立后，用户此刻完成付款 造成数据不一致
        //if(SystemConstant.ORDER_STATUS_UNPAID.equals(orderInfo.getOrderStatus())){
        //    orderInfo.setOrderStatus(ORDER_STATUS_CANCEL);
        //    orderInfoMapper.updateById(orderInfo);
        //}
        //改良下 修改条件 where order_id = ? and order_status = 0901; 如果订单最后一刻变成0902 更新失败
        int update = orderInfoMapper.update(
                null,
                new LambdaUpdateWrapper<OrderInfo>()
                        .eq(OrderInfo::getId, orderId)
                        .eq(OrderInfo::getOrderStatus, ORDER_STATUS_UNPAID)
                        .set(OrderInfo::getOrderStatus, ORDER_STATUS_CANCEL)
        );
        if (update > 0) {
            log.info("取消订单成功，{}", orderId);
        }
    }

    /**
     * 用户支付成功后，修改订单状态，虚拟物品发货
     *
     * @param orderNo
     * @return
     */
    @Override
    public void orderPaySuccess(String orderNo) {
        //1.更新订单状态
        int update = orderInfoMapper.update(
                null,
                new LambdaUpdateWrapper<OrderInfo>()
                        .eq(OrderInfo::getOrderNo, orderNo)
                        .eq(OrderInfo::getOrderStatus, ORDER_STATUS_UNPAID)
                        .set(OrderInfo::getOrderStatus, ORDER_STATUS_PAID)
        );
        if (update > 0) {
            //2.虚拟物品发货
            //2.1 构建虚拟物品发货VO对象
            OrderInfo orderInfo =
                    orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
            UserPaidRecordVo vo = new UserPaidRecordVo();
            vo.setOrderNo(orderNo);
            vo.setUserId(orderInfo.getUserId());
            vo.setItemType(orderInfo.getItemType());
            List<OrderDetail> orderDetailList = orderDetailService.list(
                    new LambdaQueryWrapper<OrderDetail>()
                            .eq(OrderDetail::getOrderId, orderInfo.getId())
            );
            List<Long> itemIdList = orderDetailList.stream().map(OrderDetail::getItemId).collect(Collectors.toList());
            vo.setItemIdList(itemIdList);

            //2.2 远程调用"用户服务"虚拟物品发货
            Result result = userFeignClient.savePaidRecord(vo);
            //2.3 判断响应业务状态码
            if (result.getCode().intValue() != 200) {
                throw new GuiguException(result.getCode(), result.getMessage());
            }
        }
    }
}
