/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-27 18:48:11
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-04-27 21:24:52
 * @FilePath: /QEleBase/qelebase-core/src/main/java/com/xiaoqu/qelebase/core/pluginSource/model/PluginDependency.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.core.plugin.running;

import com.xiaoqu.qteamos.common.utils.VersionUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

/**
 * 插件依赖
 * 描述插件之间的依赖关系
 * 
 * @author yangqijun
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginDependency {
    
    /**
     * 依赖的插件ID
     */
    private String pluginId;
    
    /**
     * 依赖的插件版本要求
     * 支持以下格式：
     * - 精确版本：1.0.0
     * - 版本范围：>=1.0.0, <2.0.0
     * - 通配符：1.*
     * - 波浪号：~1.2.3（表示 >=1.2.3 <1.3.0）
     * - 脱字符：^1.2.3（表示 >=1.2.3 <2.0.0）
     */
    private String versionRequirement;
    
    /**
     * 是否为可选依赖
     * 如果为true，即使依赖不存在，插件也可以加载
     */
    private boolean optional;
    
    /**
     * 获取依赖的插件ID
     * 
     * @return 插件ID
     */
    public String getPluginId() {
        return pluginId;
    }
    
    /**
     * 设置依赖的插件ID
     * 
     * @param pluginId 插件ID
     */
    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }
    
    /**
     * 获取版本要求
     * 
     * @return 版本要求
     */
    public String getVersionRequirement() {
        return versionRequirement;
    }
    
    /**
     * 设置版本要求
     * 
     * @param versionRequirement 版本要求
     */
    public void setVersionRequirement(String versionRequirement) {
        this.versionRequirement = versionRequirement;
    }
    
    /**
     * 是否为可选依赖
     * 
     * @return 是否可选
     */
    public boolean isOptional() {
        return optional;
    }
    
    /**
     * 设置是否为可选依赖
     * 
     * @param optional 是否可选
     */
    public void setOptional(boolean optional) {
        this.optional = optional;
    }
    
    /**
     * 检查依赖是否有效
     * 
     * @return 是否有效
     */
    public boolean isValid() {
        return pluginId != null && !pluginId.isEmpty();
    }
    
    /**
     * 检查给定版本是否满足依赖要求
     * 
     * @param version 版本号
     * @return 是否满足要求
     */
    public boolean isSatisfiedBy(String version) {
        if (versionRequirement == null || versionRequirement.isEmpty()) {
            return true; // 没有版本要求，任何版本都满足
        }
        
        // 使用VersionUtils检查版本是否满足要求
        try {
            return VersionUtils.satisfiesRequirement(version, versionRequirement);
        } catch (Exception e) {
            // 版本格式错误或比较失败
            return false;
        }
    }
    
    /**
     * 获取人类可读的依赖描述
     * 
     * @return 依赖描述
     */
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(pluginId);
        if (versionRequirement != null && !versionRequirement.isEmpty()) {
            sb.append('@').append(versionRequirement);
        }
        if (optional) {
            sb.append(" (可选)");
        }
        return sb.toString();
    }
    
    /**
     * 获取版本要求（兼容性方法）
     * 
     * @return 版本要求
     */
    public String getVersion() {
        return versionRequirement;
    }
    
    /**
     * 创建一个PluginDependency构建器
     *
     * @return PluginDependencyBuilder实例
     */
    public static PluginDependencyBuilder builder() {
        return new PluginDependencyBuilder();
    }
    
    /**
     * 插件依赖构建器
     * 用于构建PluginDependency实例
     */
    public static class PluginDependencyBuilder {
        private final PluginDependency dependency = new PluginDependency();
        
        public PluginDependencyBuilder pluginId(String pluginId) {
            dependency.pluginId = pluginId;
            return this;
        }
        
        public PluginDependencyBuilder versionRequirement(String versionRequirement) {
            dependency.versionRequirement = versionRequirement;
            return this;
        }
        
        public PluginDependencyBuilder optional(boolean optional) {
            dependency.optional = optional;
            return this;
        }
        
        public PluginDependency build() {
            return dependency;
        }
    }
} 