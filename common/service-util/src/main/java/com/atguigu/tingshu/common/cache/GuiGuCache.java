package com.atguigu.tingshu.common.cache;

import com.atguigu.tingshu.common.constant.RedisConstant;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface GuiGuCache {

    /**
     * 加入到Redis中Key前缀
     * @return
     */
    String prefix() default "none";

    /**
     * 有效时间
     * @return
     */
    long ttl() default RedisConstant.ALBUM_TIMEOUT;

    /**
     * 时间单位
     * @return
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

}
