package com.atguigu.tingshu.common.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign拦截器：只会拦截所有OpenFeign请求，拦截到请求后，如果希望将上游系统中令牌自动传递到下游系统，手动设置请求头
 */
@Component
public class FeignInterceptor implements RequestInterceptor {

    /**
     * Feign拦截器逻辑
     * @param requestTemplate
     */
    public void apply(RequestTemplate requestTemplate){
        //  获取请求对象
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        //异步编排 与 MQ消费者端 为 null
        if(null != requestAttributes) {
            ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes)requestAttributes;
            HttpServletRequest request = servletRequestAttributes.getRequest();
            String token = request.getHeader("token");
            requestTemplate.header("token", token);
        }
    }
}
