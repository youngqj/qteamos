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

import java.util.List;
import java.util.Optional;

/**
 * 服务定位器接口
 * 提供插件与系统服务之间的桥梁，允许插件获取各种系统服务
 *
 * @author yangqijun
 * @date 2025-06-10
 * @since 1.0.0
 */
public interface ServiceLocator {
    
    /**
     * 获取指定类型的服务
     *
     * @param serviceClass 服务类型
     * @param <T> 服务泛型
     * @return 服务实例，如果不存在则返回null
     */
    <T> T getService(Class<T> serviceClass);
    
    /**
     * 获取指定类型的服务（可选方式）
     *
     * @param serviceClass 服务类型
     * @param <T> 服务泛型
     * @return 服务实例Optional包装
     */
    <T> Optional<T> findService(Class<T> serviceClass);
    
    /**
     * 获取所有指定类型的服务实例
     *
     * @param serviceClass 服务类型
     * @param <T> 服务泛型
     * @return 服务实例列表
     */
    <T> List<T> getServices(Class<T> serviceClass);
    
    /**
     * 注册服务
     *
     * @param serviceClass 服务类型
     * @param serviceImpl 服务实现
     * @param <T> 服务泛型
     */
    <T> void registerService(Class<T> serviceClass, T serviceImpl);
    
    /**
     * 注销服务
     *
     * @param serviceClass 服务类型
     * @param <T> 服务泛型
     * @return 是否成功注销
     */
    <T> boolean unregisterService(Class<T> serviceClass);
    
    /**
     * 检查服务是否可用
     *
     * @param serviceClass 服务类型
     * @param <T> 服务泛型
     * @return 是否可用
     */
    <T> boolean hasService(Class<T> serviceClass);
} 