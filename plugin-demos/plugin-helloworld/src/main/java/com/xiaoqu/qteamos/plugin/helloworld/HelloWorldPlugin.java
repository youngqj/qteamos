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
 * HelloWorld插件主类
 * 基于QTeamOS SDK实现，展示插件基本功能和生命周期管理
 *
 * @author yangqijun
 * @date 2025-05-06
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.plugin.helloworld;

import com.xiaoqu.qteamos.sdk.plugin.AbstractPlugin;
import com.xiaoqu.qteamos.sdk.plugin.Plugin;
import com.xiaoqu.qteamos.sdk.plugin.PluginContext;
import com.xiaoqu.qteamos.plugin.helloworld.controller.HelloWorldController;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * HelloWorld插件主类
 * 简化版实现，只展示基本生命周期
 *
 * @author yangqijun
 * @version 1.0.0
 */
@Slf4j
public class HelloWorldPlugin extends AbstractPlugin implements Plugin {
    
    // 配置信息
    private String greeting = "Hello, QTeamOS!";
    private boolean debugMode = false;
    
    /**
     * 默认构造函数
     * 系统通过反射调用此构造函数创建插件实例
     */
    public HelloWorldPlugin() {
        System.out.println("创建HelloWorld插件实例");
        
        // 设置默认值
        this.id = "helloworld-plugin";
        this.name = "HelloWorld插件";
        this.version = "1.0.0";
        this.description = "HelloWorld插件示例，用于展示QTeamOS插件开发的基本流程";
        this.author = "yangqijun";
    }
    
    /**
     * 设置插件上下文
     * 用于兼容适配器方式的初始化
     *
     * @param context 插件上下文
     */
    public void setContext(PluginContext context) {
        System.out.println("设置插件上下文 - 通过setContext方法");
        super.context = context;
    }
    
    /**
     * 初始化插件
     * 用于兼容适配器方式的初始化
     *
     * @throws Exception 初始化异常
     */
    public void initPlugin() throws Exception {
        System.out.println("调用initPlugin方法初始化插件");
        
        if (this.context == null) {
            throw new IllegalStateException("插件上下文未设置，请先调用setContext方法");
        }
        
        // 直接调用标准init方法
        init(this.context);
    }
    
    /**
     * 插件初始化方法
     * 在系统加载插件时调用，用于初始化资源
     *
     * @param context 插件上下文，提供系统服务和资源
     * @throws Exception 初始化异常
     */
    @Override
    public void init(PluginContext context) throws Exception {
        super.init(context);
        
        System.out.println("初始化HelloWorld插件");
        
        // 读取配置信息
        this.greeting = context.getPluginConfig("greeting", "Hello, QTeamOS!");
        this.debugMode = Boolean.parseBoolean(context.getPluginConfig("enableDebug", "false"));
        
        // 打印成功信息
        System.out.println("HelloWorld插件初始化成功，准备运行");
    }
    
    /**
     * 启动插件
     *
     * @throws Exception 启动异常
     */
    @Override
    public void start() throws Exception {
        System.out.println("启动HelloWorld插件");
        System.out.println(greeting);
        super.start();
    }
    
    /**
     * 停止插件
     *
     * @throws Exception 停止异常
     */
    @Override
    public void stop() throws Exception {
        System.out.println("停止HelloWorld插件");
        super.stop();
    }
    
    /**
     * 插件卸载时调用
     *
     * @throws Exception 卸载异常
     */
    @Override
    public void uninstall() throws Exception {
        System.out.println("插件 [" + getId() + "] 卸载");
        super.uninstall();
    }
    
    /**
     * 销毁插件
     *
     * @throws Exception 销毁异常
     */
    @Override
    public void destroy() throws Exception {
        System.out.println("销毁HelloWorld插件");
        super.destroy();
    }

    /**
     * 设置属性数据
     * 接收Map形式的上下文数据，用于绕过类型不匹配问题
     *
     * @param properties 属性数据
     */
    @SuppressWarnings("unchecked")
    public void setProperties(Map<String, Object> properties) {
        System.out.println("设置属性数据 - 通过setProperties方法");
        
        // 设置基本属性
        if (properties.containsKey("pluginId")) {
            this.id = (String) properties.get("pluginId");
        }
        
        if (properties.containsKey("version")) {
            this.version = (String) properties.get("version");
        }
        
        // 设置配置
        if (properties.containsKey("configs")) {
            Map<String, String> configs = (Map<String, String>) properties.get("configs");
            if (configs != null) {
                this.greeting = configs.getOrDefault("greeting", this.greeting);
                this.debugMode = Boolean.parseBoolean(configs.getOrDefault("enableDebug", String.valueOf(this.debugMode)));
            }
        }
        
        // 记录数据路径
        if (properties.containsKey("dataFolderPath")) {
            String dataFolderPath = (String) properties.get("dataFolderPath");
            System.out.println("数据文件夹路径: " + dataFolderPath);
        }
    }
    
    /**
     * 获取控制器类名列表
     * 系统通过此方法获取插件提供的控制器
     *
     * @return 控制器类名列表
     */
    public List<String> getControllerClassNames() {
        return Arrays.asList(
            "com.xiaoqu.qteamos.plugin.helloworld.controller.HelloWorldController"
        );
    }

    /**
     * 获取控制器类列表
     * 用于框架自动注册控制器
     *
     * @return 控制器类列表
     */
    public List<Class<?>> getControllerClasses() {
        log.info("HelloWorld插件提供控制器类列表");
        return Arrays.asList(HelloWorldController.class);
    }
} 