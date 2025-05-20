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
 * 插件资源提供者接口
 * 提供插件访问其资源的能力
 *
 * @author yangqijun
 * @date 2025-05-07
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.sdk.plugin;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

/**
 * 插件资源提供者接口
 * 提供插件访问其资源的能力
 */
public interface PluginResourceProvider {
    
    /**
     * 获取插件资源的URL
     *
     * @param resourcePath 资源路径
     * @return 资源URL，如果资源不存在返回null
     */
    URL getResource(String resourcePath);
    
    /**
     * 获取插件资源的输入流
     *
     * @param resourcePath 资源路径
     * @return 资源输入流，如果资源不存在返回null
     */
    InputStream getResourceAsStream(String resourcePath);
    
    /**
     * 查找指定路径下的所有资源
     *
     * @param path 路径
     * @param recursive 是否递归查找
     * @return 资源列表
     */
    List<String> findResources(String path, boolean recursive);
    
    /**
     * 获取插件JAR文件路径
     *
     * @return 插件JAR文件路径
     */
    String getPluginJarPath();
    
    /**
     * 检查资源是否存在
     *
     * @param resourcePath 资源路径
     * @return 是否存在
     */
    boolean resourceExists(String resourcePath);
} 