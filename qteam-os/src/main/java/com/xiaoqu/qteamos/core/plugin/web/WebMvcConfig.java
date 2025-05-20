/*
 * Copyright (c) 2023-2025 XiaoQuTeam. All rights reserved.
 * QTeamOS is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

/**
 * Web MVC配置
 * 配置插件系统的Web MVC相关功能
 *
 * @author yangqijun
 * @date 2025-05-16
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.plugin.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Web MVC配置类
 * 配置插件系统的Web MVC相关功能，特别是消息转换器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    private static final Logger log = LoggerFactory.getLogger(WebMvcConfig.class);
    
    @Autowired(required = false)
    private ObjectMapper objectMapper;
    
    /**
     * 提供MappingJackson2HttpMessageConverter
     * 确保所有控制器返回的JSON都经过统一的转换器处理
     */
    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        
        // 如果存在自定义的ObjectMapper，使用它
        if (objectMapper != null) {
            log.info("使用自定义的ObjectMapper配置JSON转换器");
            converter.setObjectMapper(objectMapper);
        }
        
        // 设置JSON转换器支持的媒体类型
        converter.setSupportedMediaTypes(List.of(
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_JSON_UTF8,
            MediaType.valueOf("application/*+json")
        ));
        
        log.info("配置MappingJackson2HttpMessageConverter完成");
        return converter;
    }
    
    /**
     * 配置内容协商策略
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        log.info("配置内容协商策略");
        
        configurer
            .favorParameter(false)
            .ignoreAcceptHeader(false)
            .defaultContentType(MediaType.APPLICATION_JSON)
            .mediaType("json", MediaType.APPLICATION_JSON)
            .mediaType("xml", MediaType.APPLICATION_XML);
    }
    
    /**
     * 配置消息转换器
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("配置HTTP消息转换器");
        
        // 清空现有转换器列表，确保我们的转换器优先级最高
        converters.clear();
        
        // 添加JSON转换器到首位，确保优先使用
        MappingJackson2HttpMessageConverter jsonConverter = mappingJackson2HttpMessageConverter();
        converters.add(jsonConverter);
        
        // 添加UTF-8字符串转换器
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringConverter.setWriteAcceptCharset(false);
        converters.add(stringConverter);
        
        // 添加其他常用转换器
        converters.add(new org.springframework.http.converter.ByteArrayHttpMessageConverter());
        converters.add(new org.springframework.http.converter.ResourceHttpMessageConverter());
        
        log.info("HTTP消息转换器配置完成，转换器数量: {}", converters.size());
        log.info("JSON转换器支持的媒体类型: {}", jsonConverter.getSupportedMediaTypes());
    }
} 