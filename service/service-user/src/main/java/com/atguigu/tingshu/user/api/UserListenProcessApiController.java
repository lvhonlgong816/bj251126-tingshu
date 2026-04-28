package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "用户声音播放进度管理接口")
@RestController
@RequestMapping("api/user")
@SuppressWarnings({"all"})
public class UserListenProcessApiController {

    @Autowired
    private UserListenProcessService userListenProcessService;

    /**
     * 获取当前登录用户某个声音播放进度
     *
     * @param trackId
     * @return
     */
    @GuiGuLogin(required = false)
    @Operation(summary = "获取当前登录用户某个声音播放进度")
    @GetMapping("/userListenProcess/getTrackBreakSecond/{trackId}")
    public Result<BigDecimal> getTrackBreakSecond(@PathVariable Long trackId) {
        //1.获取当前用户ID
        Long userId = AuthContextHolder.getUserId();
        if (userId != null) {
            //2.调用业务层获取播放进度
            BigDecimal bigDecimal = userListenProcessService.getTrackBreakSecond(userId, trackId);
            return Result.ok(bigDecimal);
        }
        //3.响应
        return Result.ok(BigDecimal.ZERO);
    }

    /**
     * 更新声音播放进度
     * @param userListenProcessVo
     * @return
     */
    @GuiGuLogin(required = false)
    @Operation(summary = "更新声音播放进度")
    @PostMapping("/userListenProcess/updateListenProcess")
    public Result updateListenProcess(@RequestBody UserListenProcessVo userListenProcessVo){
        //1.获取当前用户ID
        Long userId = AuthContextHolder.getUserId();
        if (userId != null) {
            userListenProcessService.updateListenProcess(userId, userListenProcessVo);
        }
        return Result.ok();
    }
}

