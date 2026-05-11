package com.atguigu.tingshu.user.client;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.user.client.impl.UserDegradeFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 用户模块远程调用API接口
 * </p>
 *
 * @author atguigu
 */
@FeignClient(value = "service-user",path = "api/user",fallback = UserDegradeFeignClient.class)
public interface UserFeignClient {

    /**
     * 查询指定主播信息
     * @param userId
     * @return
     */
    @GetMapping("/userInfo/getUserInfoVo/{userId}")
    public Result<UserInfoVo> getUserInfoVo(@PathVariable Long userId);


    /**
     * 查询指定用户某个专辑下声音购买状态
     * @param userId 用户ID
     * @param albumId 专辑ID
     * @param needCheckPayStateTrackIdList 待检查购买状态声音ID列表
     * @return {声音ID：购买状态}
     */

    @PostMapping("/userInfo/userIsPaidTrack/{userId}/{albumId}")
    public Result<Map<Long, Integer>> userIsPaidTrack(
            @PathVariable Long userId,
            @PathVariable Long albumId,
            @RequestBody List<Long> needCheckPayStateTrackIdList
    );

    /**
     * 获取指定VIP套餐信息
     * @param id
     * @return
     */
    @GetMapping("/vipServiceConfig/getVipServiceConfig/{id}")
    public Result<VipServiceConfig> getVipServiceConfig(@PathVariable Long id);


    /**
     * 为了获取当前用户ID，确保调用方请求头比如有：token
     * 判断当前用户是否购买指定专辑
     * @param albumId
     * @return 购买状态：true:已购买专辑、 false:未购买专辑
     */
    @GetMapping("/userInfo/isPaidAlbum/{albumId}")
    public Result<Boolean> isPaidAlbum(@PathVariable Long albumId);

    /**
     * 根据专辑id+用户ID获取用户已购买声音id列表
     * @param albumId
     * @return
     */
    @GetMapping("/userInfo/findUserPaidTrackList/{albumId}")
    public Result<List<Long>> findUserPaidTrackIdList(@PathVariable Long albumId);

    /**
     * 由于后续微信支付成功后，同样需要进行权益发放，微信异步回调没有token令牌 故不要加@GuiGuLogin注解
     * 支付成功后权益方法（虚拟物品发货）
     * @param userPaidRecordVo
     * @return
     */
    @PostMapping("/userInfo/savePaidRecord")
    public Result savePaidRecord(@RequestBody UserPaidRecordVo userPaidRecordVo);

    /**
     * 更新VIP状态：处理过期会员
     * @return
     */
    @GetMapping("/updateVipExpireStatus")
    public Result updateVipExpireStatus();
}
