package com.xiaoqu.qteamos.core.plugin.dto;

import com.xiaoqu.qteamos.core.plugin.running.PluginState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 插件数据传输对象
 * 用于在系统各层之间传递插件信息
 *
 * @author yangqijun
 * @date 2024-07-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginDTO {
    
    /**
     * 插件ID
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
     * 插件主页
     */
    private String homepage;
    
    /**
     * 插件许可证
     */
    private String license;
    
    /**
     * 插件类型（system/app/module等）
     */
    private String type;
    
    /**
     * 插件信任级别（trust/untrusted）
     */
    private String trust;
    
    /**
     * 插件图标URL
     */
    private String icon;
    
    /**
     * 插件状态
     */
    private PluginState state;
    
    /**
     * 插件是否启用
     */
    private boolean enabled;
    
    /**
     * 插件加载时间
     */
    private Date loadTime;
    
    /**
     * 插件主类
     */
    private String mainClass;
    
    /**
     * 插件依赖列表
     */
    private List<PluginDependencyDTO> dependencies;
    
    /**
     * 插件JAR文件路径
     */
    private String jarPath;
    
    /**
     * 插件资源路径映射
     */
    private Map<String, String> resourcePaths;
    
    /**
     * 插件错误信息
     */
    private String errorMessage;
} 