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
 * 插件依赖定义
 * 用于描述插件之间的依赖关系
 *
 * @author yangqijun
 * @date 2025-05-13
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.api.core.plugin.descriptor;

/**
 * 插件依赖
 * 描述插件与其他插件之间的依赖关系
 */
public class PluginDependency {
    
    /**
     * 插件ID
     */
    private String pluginId;
    
    /**
     * 最低版本要求
     */
    private String minVersion;
    
    /**
     * 最高版本要求
     */
    private String maxVersion;
    
    /**
     * 是否必需
     * 如果为true，则系统必须满足此依赖才能加载插件
     * 如果为false，则此依赖是可选的
     */
    private boolean required = true;

    /**
     * 获取插件ID
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * 设置插件ID
     */
    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    /**
     * 获取最低版本要求
     */
    public String getMinVersion() {
        return minVersion;
    }

    /**
     * 设置最低版本要求
     */
    public void setMinVersion(String minVersion) {
        this.minVersion = minVersion;
    }

    /**
     * 获取最高版本要求
     */
    public String getMaxVersion() {
        return maxVersion;
    }

    /**
     * 设置最高版本要求
     */
    public void setMaxVersion(String maxVersion) {
        this.maxVersion = maxVersion;
    }

    /**
     * 是否为必需依赖
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * 设置是否为必需依赖
     */
    public void setRequired(boolean required) {
        this.required = required;
    }
    
    /**
     * 创建一个新的依赖对象
     */
    public static PluginDependency create(String pluginId, String minVersion) {
        PluginDependency dependency = new PluginDependency();
        dependency.pluginId = pluginId;
        dependency.minVersion = minVersion;
        return dependency;
    }
    
    /**
     * 创建一个新的依赖对象
     */
    public static PluginDependency create(String pluginId, String minVersion, String maxVersion) {
        PluginDependency dependency = create(pluginId, minVersion);
        dependency.maxVersion = maxVersion;
        return dependency;
    }
    
    /**
     * 创建一个新的依赖对象
     */
    public static PluginDependency create(String pluginId, String minVersion, String maxVersion, boolean required) {
        PluginDependency dependency = create(pluginId, minVersion, maxVersion);
        dependency.required = required;
        return dependency;
    }
} 