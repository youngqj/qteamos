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
 * 插件HTTP消息转换器配置
 * 确保插件控制器返回的数据能被正确序列化为JSON
 *
 * @author yangqijun
 * @date 2025-05-16
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.plugin.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
 * 插件HTTP消息转换器配置类
 * 确保插件控制器返回的JSON数据能被正确处理
 */
@Configuration
public class PluginHttpMessageConverterConfigurer implements WebMvcConfigurer {
    
    private static final Logger log = LoggerFactory.getLogger(PluginHttpMessageConverterConfigurer.class);
    
    @Autowired
    private MappingJackson2HttpMessageConverter jacksonConverter;
    
    /**
     * 配置内容协商
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON);
    }
    
    /**
     * 配置HTTP消息转换器
     * 提高JSON转换器的优先级，确保插件返回的数据能够被正确处理
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("配置插件HTTP消息转换器");
        
        // 设置UTF-8编码的String转换器
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringConverter.setWriteAcceptCharset(false);
        converters.add(0, stringConverter);
        
        // 提高JSON转换器的优先级
        if (jacksonConverter != null) {
            log.info("添加Jackson JSON转换器到首位");
            converters.add(0, jacksonConverter);
        } else {
            log.info("创建新的Jackson JSON转换器");
            MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
            converters.add(0, converter);
        }
        
        log.info("插件HTTP消息转换器配置完成，转换器数量: {}", converters.size());
    }
} 