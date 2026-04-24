package com.atguigu.tingshu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.ThreadPoolExecutor;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)//取消数据源自动配置
@EnableDiscoveryClient
@EnableFeignClients  //扫描feign接口产生代理对象
@EnableAsync
public class ServiceSearchApplication {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(ServiceSearchApplication.class, args);
        ThreadPoolExecutor poolExecutor = context.getBean(ThreadPoolExecutor.class);
        System.err.println(poolExecutor);
    }

}
