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
 * 插件描述符
 * 包含插件的元数据信息，用于插件管理和加载
 *
 * @author yangqijun
 * @date 2025-05-02
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.plugin.model;

import lombok.Data;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class PluginDescriptor {
    
    /**
     * 插件ID
     */
    private String id;
    
    /**
     * 插件名称
     */
    private String name;
    
    /**
     * 插件版本
     */
    private String version;
    
    /**
     * 插件描述
     */
    private String description;
    
    /**
     * 插件主类全限定名
     */
    private String mainClass;
    
    /**
     * 插件作者
     */
    private String author;
    
    /**
     * 插件依赖
     */
    private List<PluginDependency> dependencies;
    
    /**
     * 插件许可证
     */
    private String license;
    
    /**
     * 插件扩展点
     */
    private List<String> extensionPoints;
    
    /**
     * 插件配置项
     */
    private Map<String, Object> properties;
    
    /**
     * 插件最低系统版本要求
     */
    private String minSystemVersion;
    
    /**
     * 是否启用
     */
    private boolean enabled;
} 