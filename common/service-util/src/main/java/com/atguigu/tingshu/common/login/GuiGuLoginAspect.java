package com.atguigu.tingshu.common.login;

import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @author: atguigu
 * @create: 2026-04-21 10:07
 */
@Slf4j
@Aspect
@Component
public class GuiGuLoginAspect {

    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * 通过环绕通知对业务模块api包下，且使用自定义认证注解@GuiGuLogin目标方法进行增强
     *
     * @param pjp        目标切入点对象
     * @param guiGuLogin 注解对象
     * @return
     * @throws Throwable
     */
    @Around("execution(* com.atguigu.tingshu.*.api.*.*(..)) && @annotation(guiGuLogin)")
    public Object doBasicProfiling(ProceedingJoinPoint pjp, GuiGuLogin guiGuLogin) throws Throwable {
        log.info("AOP 前置逻辑，认证状态校验，将用户ID存入TL");
        //1.获取请求头中令牌
        //1.1 通过请求上下文对象获取请求对象 RequestAttributes接口 底层：基于ThreadLocal实现
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        //1.2 强转为实现类对象ServletRequestAttributes:实现类
        ServletRequestAttributes sra = (ServletRequestAttributes) requestAttributes;
        HttpServletRequest request = sra.getRequest();
        //1.3 按照前后端约定 从token请求头获取令牌
        String token = request.getHeader("token");
        //2.尝试查询Redis获取当前用户基本信息
        //2.1 构建查询Redis登录用户信息Key
        String loginKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;

        //2.2 调用Redis模板对象获取用户基本信息:UserInfoVo
        UserInfoVo userInfoVo = (UserInfoVo) redisTemplate.opsForValue().get(loginKey);

        //3.如果用户信息为空且目标方法要求必须登录 抛出异常：业务状态码设置208 前端引导用户跳转登录页
        if(userInfoVo==null && guiGuLogin.required()){
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        }

        //4.如果用户信息有值，将用户ID存入ThreadLocal，方便在javaEE三层controller，service，Mapper获取用户ID
        if(userInfoVo!=null){
            AuthContextHolder.setUserId(userInfoVo.getId());
        }
        //5.执行目标方法
        Object retVal = pjp.proceed();
        log.info("controller目标方法执行");

        //6.避免Threalocal出现内存泄漏，使用完毕清理ThreadLocal
        log.info("AOP 后置逻辑,清理ThreadLocal");
        AuthContextHolder.removeUserId();
        //7.响应结果
        return retVal;
    }

}
