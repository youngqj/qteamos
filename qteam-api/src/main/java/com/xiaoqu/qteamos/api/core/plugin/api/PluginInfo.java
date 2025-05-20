package com.xiaoqu.qteamos.api.core.plugin.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 插件信息
 * 用于API接口返回的插件信息
 *
 * @author yangqijun
 * @date 2025-05-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginInfo {

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
     * 插件类型
     */
    private String type;
    
    /**
     * 插件信任级别
     */
    private String trust;
    
    /**
     * 插件状态
     */
    private String state;
    
    /**
     * 插件是否启用
     */
    private boolean enabled;
    
    /**
     * 插件加载时间
     */
    private Date loadTime;
    
    /**
     * 插件启动时间
     */
    private Date startTime;
    
    /**
     * 插件依赖列表
     */
    private List<DependencyInfo> dependencies;
    
    /**
     * 插件扩展点
     */
    private List<ExtensionPointInfo> extensionPoints;
    
    /**
     * 插件元数据
     */
    private Map<String, Object> metadata;
    
    /**
     * 依赖信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DependencyInfo {
        /**
         * 依赖插件ID
         */
        private String pluginId;
        
        /**
         * 版本要求
         */
        private String version;
        
        /**
         * 是否可选
         */
        private boolean optional;
    }
    
    /**
     * 扩展点信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtensionPointInfo {
        /**
         * 扩展点ID
         */
        private String id;
        
        /**
         * 名称
         */
        private String name;
        
        /**
         * 描述
         */
        private String description;
        
        /**
         * 类型
         */
        private String type;
    }
} 