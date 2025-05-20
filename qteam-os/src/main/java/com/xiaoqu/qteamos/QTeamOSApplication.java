/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-28 11:36:25
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-05-02 15:10:50
 * @FilePath: /qteamos/src/main/java/com/xiaoqu/qteamos/QteamosApplication.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    RedisAutoConfiguration.class,
    RedisRepositoriesAutoConfiguration.class
})
@ComponentScan(basePackages = {"com.xiaoqu.qteamos.*"})
@EnableTransactionManagement
public class QTeamOSApplication implements WebMvcConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(QTeamOSApplication.class, args);
    }

    /**
     * 修改Spring MVC的路径匹配配置，确保插件Controller能被正确匹配
     */
    @Bean
    public WebMvcRegistrations webMvcRegistrations() {
        return new WebMvcRegistrations() {
            @Override
            public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
                return new CustomRequestMappingHandlerMapping();
            }
        };
    }
    
    /**
     * 自定义RequestMappingHandlerMapping
     * 设置优先级为最高，确保插件Controller先于静态资源处理
     */
    public static class CustomRequestMappingHandlerMapping extends RequestMappingHandlerMapping {
        public CustomRequestMappingHandlerMapping() {
            // 设置最高优先级，确保先于ResourceHandlerMapping处理
            this.setOrder(Ordered.HIGHEST_PRECEDENCE);
        }
    }

}
