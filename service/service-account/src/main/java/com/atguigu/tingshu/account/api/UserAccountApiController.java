package com.atguigu.tingshu.account.api;

import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "用户账户管理")
@RestController
@RequestMapping("api/account")
@SuppressWarnings({"all"})
public class UserAccountApiController {

    @Autowired
    private UserAccountService userAccountService;


    /**
     * 查询当前用户可用账户余额
     *
     * @return
     */
    @GuiGuLogin
    @Operation(summary = "查询当前用户可用账户余额")
    @GetMapping("/userAccount/getAvailableAmount")
    public Result<BigDecimal> getAvailableAmount() {
        //1.从ThreadLocal获取当前用户ID
        Long userId = AuthContextHolder.getUserId();
        //2.调用业务逻辑
        BigDecimal amount = userAccountService.getAvailableAmount(userId);
        //3.返回可用金额
        return Result.ok(amount);
    }


    /**
     * 支付方式选择余额支付，执行扣减逻辑
     *
     * @param accountDeductVo 扣减信息vo
     * @return
     */
    @Operation(summary = "余额扣减（检查并且扣减余额）")
    @PostMapping("/userAccount/checkAndDeduct")
    public Result checkAndDeduct(@RequestBody AccountDeductVo accountDeductVo) {
        userAccountService.checkAndDeduct(accountDeductVo);
        return Result.ok();
    }

    /**
     * 分页查询当前用户消费记录
     * @param page
     * @param limit
     * @return
     */
    @GuiGuLogin
    @Operation(summary = "分页查询当前用户消费记录")
    @GetMapping("/userAccount/findUserConsumePage/{page}/{limit}")
    public Result<Page<UserAccountDetail>> findUserConsumePage(
            @PathVariable Long page,
            @PathVariable Long limit
    ) {
        //1.获取当前用户ID
        Long userId = AuthContextHolder.getUserId();
        //2.构建分页对象
        Page pageInfo = new Page<UserAccountDetail>(page, limit);
        //3.执行业务逻辑
        userAccountService.findUserAccountDetailPage(pageInfo, userId, SystemConstant.ACCOUNT_TRADE_TYPE_MINUS);
        //4.响应分页对象
        return Result.ok(pageInfo);
    }

    /**
     * 分页查询当前用户充值记录
     * @param page
     * @param limit
     * @return
     */
    @GuiGuLogin
    @Operation(summary = "分页查询当前用户充值记录")
    @GetMapping("/userAccount/findUserRechargePage/{page}/{limit}")
    public Result<Page<UserAccountDetail>> findUserRechargePage(
            @PathVariable Long page,
            @PathVariable Long limit

    ) {
        //1.获取当前用户ID
        Long userId = AuthContextHolder.getUserId();
        //2.构建分页对象
        Page pageInfo = new Page<UserAccountDetail>(page, limit);
        //3.执行业务逻辑
        userAccountService.findUserAccountDetailPage(pageInfo, userId, SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT);
        //4.响应分页对象
        return Result.ok(pageInfo);
    }

}

