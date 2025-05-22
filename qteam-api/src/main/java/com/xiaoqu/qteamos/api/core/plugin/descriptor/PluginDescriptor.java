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
 * API层定义，用于消除模块间循环依赖
 *
 * @author yangqijun
 * @date 2025-05-13
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.api.core.plugin.descriptor;

import java.util.List;
import java.util.Map;

/**
 * 插件描述符
 * 用于描述插件的基本信息和元数据
 */
public class PluginDescriptor {
    
    /**
     * 插件ID，全局唯一标识
     */
    private String pluginId;
    
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
     * 插件作者
     */
    private String author;
    
    /**
     * 插件主类
     * 插件的入口类，必须实现Plugin接口
     */
    private String mainClass;
    
    /**
     * 插件类型
     * normal: 普通插件
     * system: 系统插件
     */
    private String type;
    
    /**
     * 插件信任级别
     * trust: 受信任的
     * untrusted: 不受信任的
     */
    private String trust;
    
    /**
     * 插件依赖列表
     */
    private List<PluginDependency> dependencies;
    
    /**
     * 插件的最小系统版本要求
     */
    private String requiredSystemVersion;
    
    /**
     * 插件是否启用
     */
    private boolean enabled;
    
    /**
     * 插件优先级，数值越小优先级越高
     */
    private int priority;
    
    /**
     * 插件的配置项
     */
    private Map<String, Object> properties;
    
    /**
     * 插件申请的权限列表
     */
    private List<String> permissions;
    
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
     * 获取插件名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 设置插件名称
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * 获取插件版本
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * 设置插件版本
     */
    public void setVersion(String version) {
        this.version = version;
    }
    
    /**
     * 获取插件描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 设置插件描述
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * 获取插件作者
     */
    public String getAuthor() {
        return author;
    }
    
    /**
     * 设置插件作者
     */
    public void setAuthor(String author) {
        this.author = author;
    }
    
    /**
     * 获取插件主类
     */
    public String getMainClass() {
        return mainClass;
    }
    
    /**
     * 设置插件主类
     */
    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }
    
    /**
     * 获取插件类型
     */
    public String getType() {
        return type;
    }
    
    /**
     * 设置插件类型
     */
    public void setType(String type) {
        this.type = type;
    }
    
    /**
     * 获取插件信任级别
     */
    public String getTrust() {
        return trust;
    }
    
    /**
     * 设置插件信任级别
     */
    public void setTrust(String trust) {
        this.trust = trust;
    }
    
    /**
     * 获取插件依赖列表
     */
    public List<PluginDependency> getDependencies() {
        return dependencies;
    }
    
    /**
     * 设置插件依赖列表
     */
    public void setDependencies(List<PluginDependency> dependencies) {
        this.dependencies = dependencies;
    }
    
    /**
     * 获取插件最小系统版本要求
     */
    public String getRequiredSystemVersion() {
        return requiredSystemVersion;
    }
    
    /**
     * 设置插件最小系统版本要求
     */
    public void setRequiredSystemVersion(String requiredSystemVersion) {
        this.requiredSystemVersion = requiredSystemVersion;
    }
    
    /**
     * 获取插件是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 设置插件是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * 获取插件优先级
     */
    public int getPriority() {
        return priority;
    }
    
    /**
     * 设置插件优先级
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    /**
     * 获取插件配置项
     */
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    /**
     * 设置插件配置项
     */
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
    
    /**
     * 获取插件申请的权限列表
     */
    public List<String> getPermissions() {
        return permissions;
    }
    
    /**
     * 设置插件申请的权限列表
     */
    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
    
    /**
     * 构建器类，用于创建PluginDescriptor实例
     */
    public static class Builder {
        private final PluginDescriptor descriptor = new PluginDescriptor();
        
        public Builder pluginId(String pluginId) {
            descriptor.setPluginId(pluginId);
            return this;
        }
        
        public Builder name(String name) {
            descriptor.setName(name);
            return this;
        }
        
        public Builder version(String version) {
            descriptor.setVersion(version);
            return this;
        }
        
        public Builder description(String description) {
            descriptor.setDescription(description);
            return this;
        }
        
        public Builder author(String author) {
            descriptor.setAuthor(author);
            return this;
        }
        
        public Builder mainClass(String mainClass) {
            descriptor.setMainClass(mainClass);
            return this;
        }
        
        public Builder type(String type) {
            descriptor.setType(type);
            return this;
        }
        
        public Builder trust(String trust) {
            descriptor.setTrust(trust);
            return this;
        }
        
        public Builder dependencies(List<PluginDependency> dependencies) {
            descriptor.setDependencies(dependencies);
            return this;
        }
        
        public Builder requiredSystemVersion(String requiredSystemVersion) {
            descriptor.setRequiredSystemVersion(requiredSystemVersion);
            return this;
        }
        
        public Builder enabled(boolean enabled) {
            descriptor.setEnabled(enabled);
            return this;
        }
        
        public Builder priority(int priority) {
            descriptor.setPriority(priority);
            return this;
        }
        
        public Builder properties(Map<String, Object> properties) {
            descriptor.setProperties(properties);
            return this;
        }
        
        public Builder permissions(List<String> permissions) {
            descriptor.setPermissions(permissions);
            return this;
        }
        
        public PluginDescriptor build() {
            return descriptor;
        }
    }
    
    /**
     * 创建Builder实例
     */
    public static Builder builder() {
        return new Builder();
    }
} 