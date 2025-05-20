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
 * 插件内容协商视图解析器
 * 根据请求的Accept头选择合适的视图解析器处理插件控制器返回值
 *
 * @author yangqijun
 * @date 2025-05-16
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.plugin.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import java.util.*;

/**
 * 插件内容协商视图解析器
 * 负责解析插件控制器返回的视图，根据请求的Accept头选择合适的格式
 */
@Component
public class PluginContentNegotiatingViewResolver extends ContentNegotiatingViewResolver {
    private static final Logger log = LoggerFactory.getLogger(PluginContentNegotiatingViewResolver.class);
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired(required = false)
    private List<HttpMessageConverter<?>> messageConverters;
    
    /**
     * 初始化方法
     */
    @Autowired
    public void init(ContentNegotiationManager contentNegotiationManager) {
        log.info("初始化插件内容协商视图解析器");
        
        // 设置内容协商管理器
        setContentNegotiationManager(contentNegotiationManager);
        
        // 设置默认视图
        setDefaultViews(Arrays.asList(createDefaultJackson2JsonView()));
        
        // 设置视图解析器
        List<ViewResolver> resolvers = new ArrayList<>();
        
        // 添加自定义插件视图解析器
        resolvers.add(new PluginJsonViewResolver());
        
        // 设置视图解析器
        setViewResolvers(resolvers);
        
        // 设置高优先级，确保插件视图解析器先被调用
        setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        
        log.info("插件内容协商视图解析器初始化完成");
    }
    
    /**
     * 创建默认的JSON视图
     */
    private MappingJackson2JsonView createDefaultJackson2JsonView() {
        MappingJackson2JsonView jsonView = new MappingJackson2JsonView();
        jsonView.setPrettyPrint(true);
        jsonView.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        // 如果有自定义的消息转换器，使用其中的ObjectMapper
        if (messageConverters != null) {
            for (HttpMessageConverter<?> converter : messageConverters) {
                if (converter instanceof MappingJackson2HttpMessageConverter) {
                    MappingJackson2HttpMessageConverter jacksonConverter = 
                            (MappingJackson2HttpMessageConverter) converter;
                    jsonView.setObjectMapper(jacksonConverter.getObjectMapper());
                    break;
                }
            }
        }
        
        log.debug("创建默认JSON视图: {}", jsonView);
        return jsonView;
    }
    
    /**
     * 插件JSON视图解析器
     * 将非视图对象转换为JSON视图
     */
    private class PluginJsonViewResolver implements ViewResolver {
        
        @Override
        public View resolveViewName(String viewName, Locale locale) throws Exception {
            log.debug("解析视图名称: {}, 区域: {}", viewName, locale);
            
            // 如果视图名称为空或以"redirect:"或"forward:"开头，返回null，由其他解析器处理
            if (viewName == null || viewName.startsWith("redirect:") || viewName.startsWith("forward:")) {
                return null;
            }
            
            // 创建一个JSON视图
            MappingJackson2JsonView jsonView = createDefaultJackson2JsonView();
            
            // 设置视图名称属性
            jsonView.setAttributesMap(Collections.singletonMap("viewName", viewName));
            
            log.debug("创建JSON视图: {}, 视图名称: {}", jsonView, viewName);
            return jsonView;
        }
    }
} 