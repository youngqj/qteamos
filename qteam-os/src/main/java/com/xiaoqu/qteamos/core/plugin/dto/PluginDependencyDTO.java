package com.xiaoqu.qteamos.core.plugin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 插件依赖数据传输对象
 * 用于传递插件依赖信息
 *
 * @author yangqijun
 * @date 2024-07-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginDependencyDTO {
    
    /**
     * 依赖的插件ID
     */
    private String pluginId;
    
    /**
     * 依赖的插件版本要求
     */
    private String versionRequirement;
    
    /**
     * 是否为可选依赖
     */
    private boolean optional;
    
    /**
     * 依赖是否已满足
     */
    private boolean satisfied;
    
    /**
     * 实际安装的版本（如果已满足）
     */
    private String installedVersion;
} 