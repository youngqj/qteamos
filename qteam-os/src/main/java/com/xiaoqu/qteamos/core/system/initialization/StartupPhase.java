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
 * 系统启动阶段枚举
 * 定义系统从启动到完全初始化的各个阶段
 *
 * @author yangqijun
 * @date 2025-05-04
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.system.initialization;

/**
 * 系统启动阶段
 * 定义系统启动过程中的不同阶段，用于启动流程控制和状态报告
 */
public enum StartupPhase {
    
    /**
     * 准备阶段：系统配置加载，基础环境检查
     */
    PREPARING("准备阶段", 0),
    
    /**
     * 核心服务初始化阶段：数据库、缓存等基础服务启动
     */
    CORE_SERVICES("核心服务初始化", 1),
    
    /**
     * 插件系统初始化阶段：插件管理器启动，插件环境准备
     */
    PLUGIN_SYSTEM("插件系统初始化", 2),
    
    /**
     * 插件加载阶段：扫描并加载已安装的插件
     */
    PLUGIN_LOADING("插件加载", 3),
    
    /**
     * 应用服务启动阶段：API网关、安全服务等应用层服务启动
     */
    APPLICATION_SERVICES("应用服务启动", 4),
    
    /**
     * 系统就绪阶段：所有服务启动完成，系统可以接受外部请求
     */
    READY("系统就绪", 5);
    
    /**
     * 阶段名称
     */
    private final String name;
    
    /**
     * 阶段顺序
     */
    private final int order;
    
    /**
     * 构造函数
     *
     * @param name 阶段名称
     * @param order 阶段顺序
     */
    StartupPhase(String name, int order) {
        this.name = name;
        this.order = order;
    }
    
    /**
     * 获取阶段名称
     *
     * @return 阶段名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 获取阶段顺序
     *
     * @return 阶段顺序
     */
    public int getOrder() {
        return order;
    }
    
    /**
     * 获取下一个阶段
     *
     * @return 下一个阶段，如果是最后一个阶段则返回自身
     */
    public StartupPhase next() {
        StartupPhase[] phases = StartupPhase.values();
        int currentIndex = this.ordinal();
        
        if (currentIndex < phases.length - 1) {
            return phases[currentIndex + 1];
        }
        
        return this;
    }
    
    /**
     * 检查当前阶段是否在指定阶段之前
     *
     * @param phase 待比较阶段
     * @return 如果当前阶段在指定阶段之前则返回true
     */
    public boolean isBefore(StartupPhase phase) {
        return this.order < phase.order;
    }
    
    /**
     * 检查当前阶段是否在指定阶段之后
     *
     * @param phase 待比较阶段
     * @return 如果当前阶段在指定阶段之后则返回true
     */
    public boolean isAfter(StartupPhase phase) {
        return this.order > phase.order;
    }
    
    @Override
    public String toString() {
        return name + " (" + order + ")";
    }
} 