/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-28 11:36:51
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-05-01 10:29:05
 * @FilePath: /qteamos/src/main/java/com/xiaoqu/qteamos/core/plugin/running/PluginState.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.core.plugin.running;

/**
 * 插件状态枚举
 * 表示插件在生命周期中的不同状态
 * 
 * @author yangqijun
 * @version 1.0.0
 */
public enum PluginState {
    
    /**
     * 已创建：插件刚被发现，尚未加载
     */
    CREATED("已创建"),
    
    /**
     * 已加载：插件类和资源已加载，但尚未初始化
     */
    LOADED("已加载"),
    
    /**
     * 已初始化：插件已初始化，但尚未启动
     */
    INITIALIZED("已初始化"),
    
    /**
     * 已启动：插件已完全启动并运行
     */
    STARTED("已启动"),
    
    /**
     * 已停止：插件已停止运行，但仍保留在内存中
     */
    STOPPED("已停止"),
    
    /**
     * 正在卸载：插件正在从内存中卸载
     */
    UNLOADING("正在卸载"),
    
    /**
     * 已卸载：插件已从内存中卸载
     */
    UNLOADED("已卸载"),
    
    /**
     * 已禁用：插件被手动禁用
     */
    DISABLED("已禁用"),
    
    /**
     * 错误：插件加载或运行期间发生错误
     */
    ERROR("错误"),
    
    /**
     * 依赖失败：插件依赖解析失败
     */
    DEPENDENCY_FAILED("依赖失败"),
    
    /**
     * 失败：插件加载、初始化或启动失败
     */
    FAILED("失败"),
    
    /**
     * 运行中：插件正在运行
     */
    RUNNING("运行中"),
    
    /**
     * 已隔离状态（因连续错误被隔离）
     */
    ISOLATED("已隔离"),
    
    /**
     * 资源受限：因资源使用超限被暂停
     */
    RESOURCE_LIMITED("资源受限");
    
    private final String description;
    
    PluginState(String description) {
        this.description = description;
    }
    
    /**
     * 获取状态描述
     * 
     * @return 状态描述
     */
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return name() + "(" + description + ")";
    }
} 