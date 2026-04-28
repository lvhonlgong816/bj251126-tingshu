package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户管理接口")
@RestController
@RequestMapping("api/user")
@SuppressWarnings({"all"})
public class UserInfoApiController {

    @Autowired
    private UserInfoService userInfoService;


    /**
     * 查询指定主播信息
     *
     * @param userId
     * @return
     */
    @Operation(summary = "查询指定主播信息")
    @GetMapping("/userInfo/getUserInfoVo/{userId}")
    public Result<UserInfoVo> getUserInfoVo(@PathVariable Long userId) {
        UserInfoVo userInfo = userInfoService.getUserInfo(userId);
        return Result.ok(userInfo);
    }

    /**
     * 查询指定用户某个专辑下声音购买状态
     * @param userId 用户ID
     * @param albumId 专辑ID
     * @param needCheckPayStateTrackIdList 待检查购买状态声音ID列表
     * @return {声音ID：购买状态}
     */

    @Operation(summary = "查询指定用户某个专辑下声音购买状态")
    @PostMapping("/userInfo/userIsPaidTrack/{userId}/{albumId}")
    public Result<Map<Long, Integer>> userIsPaidTrack(
            @PathVariable Long userId,
            @PathVariable Long albumId,
            @RequestBody List<Long> needCheckPayStateTrackIdList
    ) {
        Map<Long, Integer> map = userInfoService.userIsPaidTrack(userId, albumId, needCheckPayStateTrackIdList);
        return Result.ok(map);
    }

}

