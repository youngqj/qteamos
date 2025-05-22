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
 * 系统启动配置属性
 * 用于加载和存储系统启动相关的配置信息
 *
 * @author yangqijun
 * @date 2025-05-04
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.system.initialization;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

/**
 * 系统启动配置属性
 * 从配置文件中加载系统启动相关的配置参数
 */
@Getter
@Component
@ConfigurationProperties(prefix = "qteamos.system.startup")
public class SystemStartupProperties {
    
    /**
     * 启动超时时间（毫秒）
     * -- GETTER --
     *  获取启动超时时间
     *
     *
     * -- SETTER --
     *  设置启动超时时间
     *
     @return 启动超时时间（毫秒）
      * @param timeoutMillis 启动超时时间（毫秒）

     */
    @Setter
    private long timeoutMillis = 120000; // 默认2分钟
    
    /**
     * 是否启用异步启动
     * -- GETTER --
     *  是否启用异步启动
     *
     *
     * -- SETTER --
     *  设置是否启用异步启动
     *
     @return 如果启用异步启动则返回true
      * @param asyncStartup 是否启用异步启动

     */
    @Setter
    private boolean asyncStartup = true;
    
    /**
     * 是否在启动时自动加载插件
     * -- SETTER --
     *  设置是否在启动时自动加载插件
     *
     *
     * -- GETTER --
     *  是否在启动时自动加载插件
     *
     @param autoLoadPlugins 是否在启动时自动加载插件
      * @return 如果启动时自动加载插件则返回true

     */
    @Setter
    private boolean autoLoadPlugins = true;
    
    /**
     * 是否启用健康检查
     * -- SETTER --
     *  设置是否启用健康检查
     *
     *
     * -- GETTER --
     *  是否启用健康检查
     *
     @param healthCheckEnabled 是否启用健康检查
      * @return 如果启用健康检查则返回true

     */
    @Setter
    private boolean healthCheckEnabled = true;
    
    /**
     * 是否输出详细的启动信息
     * -- SETTER --
     *  设置是否输出详细的启动信息

     * -- GETTER --
     *  是否输出详细的启动信息
     *
     */
    @Setter
    private boolean verboseLogging = false;
    
    /**
     * 是否在发生启动错误时继续
     * -- GETTER --
     *  是否在发生启动错误时继续
     *
     *
     * -- SETTER --
     *  设置是否在发生启动错误时继续
     *
     @return 如果在发生启动错误时继续则返回true
      * @param continueOnError 是否在发生启动错误时继续

     */
    @Setter
    private boolean continueOnError = false;
    
    /**
     * 核心服务组件的启动顺序配置
     * -- GETTER --
     *  获取核心服务组件的启动顺序配置
     *
     *
     * -- SETTER --
     *  设置核心服务组件的启动顺序配置
     *
     @return 核心服务组件的启动顺序配置
      * @param coreServices 核心服务组件的启动顺序配置

     */
    @Setter
    private CoreServicesConfig coreServices = new CoreServicesConfig();
    
    /**
     * 插件存储路径，默认为./plugins
     * -- GETTER --
     *  获取插件存储路径
     *
     * @return 插件存储路径

     */
    @Value("${plugin.storage-path:./plugins}")
    private String pluginStoragePath;
    
    /**
     * 插件临时目录，用于存放待验证的插件，默认为./plugins-temp
     * -- GETTER --
     *  获取插件临时目录
     *
     * @return 插件临时目录

     */
    @Value("${plugin.temp-dir:./plugins-temp}")
    private String pluginTempDir;
    
    /**
     * 是否开启自动发现插件功能，默认开启
     * -- GETTER --
     *  是否开启自动发现插件功能
     *
     * @return 如果开启自动发现插件功能则返回true

     */
    @Value("${plugin.auto-discover:true}")
    private boolean pluginAutoDiscover;

    /**
     * 核心服务组件的启动顺序配置
     */
    @Setter
    @Getter
    public static class CoreServicesConfig {
        /**
         * 数据库服务启动顺序
         * -- GETTER --
         *  获取数据库服务启动顺序
         *
         *
         * -- SETTER --
         *  设置数据库服务启动顺序
         *
         @return 数据库服务启动顺序
          * @param databaseOrder 数据库服务启动顺序

         */
        private int databaseOrder = 1;
        
        /**
         * 缓存服务启动顺序
         * -- GETTER --
         *  获取缓存服务启动顺序
         *
         *
         * -- SETTER --
         *  设置缓存服务启动顺序
         *
         @return 缓存服务启动顺序
          * @param cacheOrder 缓存服务启动顺序

         */
        private int cacheOrder = 2;
        
        /**
         * 安全服务启动顺序
         * -- GETTER --
         *  获取安全服务启动顺序
         *
         *
         * -- SETTER --
         *  设置安全服务启动顺序
         *
         @return 安全服务启动顺序
          * @param securityOrder 安全服务启动顺序

         */
        private int securityOrder = 3;
        
        /**
         * 网关服务启动顺序
         * -- GETTER --
         *  获取网关服务启动顺序
         *
         *
         * -- SETTER --
         *  设置网关服务启动顺序
         *
         @return 网关服务启动顺序
          * @param gatewayOrder 网关服务启动顺序

         */
        private int gatewayOrder = 4;
        
        /**
         * 插件系统启动顺序
         * -- GETTER --
         *  获取插件系统启动顺序
         *
         *
         * -- SETTER --
         *  设置插件系统启动顺序
         *
         @return 插件系统启动顺序
          * @param pluginSystemOrder 插件系统启动顺序

         */
        private int pluginSystemOrder = 5;

    }
} 