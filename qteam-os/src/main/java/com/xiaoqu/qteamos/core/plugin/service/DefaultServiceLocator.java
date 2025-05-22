/*
 * Copyright (c) 2023-2024 XiaoQuTeam. All rights reserved.
 * QTeamOS is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package com.xiaoqu.qteamos.core.plugin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认服务定位器实现
 * 提供插件与系统服务之间的桥梁，允许插件获取各种系统服务
 *
 * @author yangqijun
 * @date 2025-06-10
 * @since 1.0.0
 */
@Component
public class DefaultServiceLocator implements ServiceLocator, ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(DefaultServiceLocator.class);
    
    // Spring应用上下文
    private ApplicationContext applicationContext;
    
    // 自定义注册的服务缓存
    private final Map<Class<?>, Object> serviceCache = new ConcurrentHashMap<>();
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        log.info("服务定位器初始化完成");
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClass) {
        if (serviceClass == null) {
            return null;
        }
        
        // 先从自定义缓存中查找
        if (serviceCache.containsKey(serviceClass)) {
            return (T) serviceCache.get(serviceClass);
        }
        
        // 从Spring上下文中查找
        try {
            return applicationContext.getBean(serviceClass);
        } catch (BeansException e) {
            log.debug("从Spring上下文中未找到服务: {}", serviceClass.getName());
            return null;
        }
    }
    
    @Override
    public <T> Optional<T> findService(Class<T> serviceClass) {
        return Optional.ofNullable(getService(serviceClass));
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getServices(Class<T> serviceClass) {
        if (serviceClass == null) {
            return new ArrayList<>();
        }
        
        List<T> result = new ArrayList<>();
        
        // 从缓存中查找
        serviceCache.forEach((aClass, service) -> {
            if (serviceClass.isAssignableFrom(aClass)) {
                result.add((T) service);
            }
        });
        
        // 从Spring上下文中查找
        try {
            Map<String, T> beansOfType = applicationContext.getBeansOfType(serviceClass);
            result.addAll(beansOfType.values());
        } catch (BeansException e) {
            log.debug("从Spring上下文中查找服务时出错: {}", e.getMessage());
        }
        
        return result;
    }
    
    @Override
    public <T> void registerService(Class<T> serviceClass, T serviceImpl) {
        if (serviceClass == null || serviceImpl == null) {
            return;
        }
        
        serviceCache.put(serviceClass, serviceImpl);
        log.debug("注册服务: {}", serviceClass.getName());
    }
    
    @Override
    public <T> boolean unregisterService(Class<T> serviceClass) {
        if (serviceClass == null) {
            return false;
        }
        
        Object removed = serviceCache.remove(serviceClass);
        if (removed != null) {
            log.debug("注销服务: {}", serviceClass.getName());
            return true;
        }
        
        return false;
    }
    
    @Override
    public <T> boolean hasService(Class<T> serviceClass) {
        if (serviceClass == null) {
            return false;
        }
        
        // 检查缓存
        if (serviceCache.containsKey(serviceClass)) {
            return true;
        }
        
        // 检查Spring上下文
        try {
            return applicationContext.getBeanNamesForType(serviceClass).length > 0;
        } catch (Exception e) {
            return false;
        }
    }
} 