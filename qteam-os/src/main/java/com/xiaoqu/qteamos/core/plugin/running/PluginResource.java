package com.xiaoqu.qteamos.core.plugin.running;

import lombok.Builder;
import lombok.Data;

/**
 * 插件资源
 * 描述插件包含的资源文件
 * 
 * @author yangqijun
 * @version 1.0.0
 */
@Data
@Builder
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
    @Builder.Default
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
} 