package com.xiaoqu.qteamos.core.plugin.running;

import lombok.Data;

/**
 * 插件资源
 * 描述插件包含的资源文件
 * 
 * @author yangqijun
 * @version 1.0.0
 */
@Data
public class PluginResource {
    
    /**
     * 资源路径
     * 相对于插件根目录的路径
     */
    private String path;
    
    /**
     * 资源类型
     * file: 文件
     * directory: 目录
     */
    private String type;
    
    /**
     * 资源描述（可选）
     */
    private String description;
    
    /**
     * 资源是否必须存在
     * 默认为true
     */
    private boolean required = true;
    
    /**
     * 检查资源是否为目录
     * 
     * @return 是否为目录
     */
    public boolean isDirectory() {
        return "directory".equalsIgnoreCase(type);
    }
    
    /**
     * 检查资源是否为文件
     * 
     * @return 是否为文件
     */
    public boolean isFile() {
        return "file".equalsIgnoreCase(type);
    }
    
    /**
     * 检查资源有效性
     * 
     * @return 是否有效
     */
    public boolean isValid() {
        return path != null && !path.isEmpty() && 
               (isFile() || isDirectory());
    }
    
    /**
     * 创建一个PluginResource构建器
     *
     * @return PluginResourceBuilder实例
     */
    public static PluginResourceBuilder builder() {
        return new PluginResourceBuilder();
    }
    
    /**
     * 插件资源构建器
     * 用于构建PluginResource实例
     */
    public static class PluginResourceBuilder {
        private final PluginResource resource = new PluginResource();
        
        public PluginResourceBuilder path(String path) {
            resource.path = path;
            return this;
        }
        
        public PluginResourceBuilder type(String type) {
            resource.type = type;
            return this;
        }
        
        public PluginResourceBuilder description(String description) {
            resource.description = description;
            return this;
        }
        
        public PluginResourceBuilder required(boolean required) {
            resource.required = required;
            return this;
        }
        
        public PluginResource build() {
            return resource;
        }
    }
} 