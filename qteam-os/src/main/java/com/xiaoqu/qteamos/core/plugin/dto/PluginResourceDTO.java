package com.xiaoqu.qteamos.core.plugin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 插件资源数据传输对象
 * 用于传递插件资源相关信息
 *
 * @author yangqijun
 * @date 2024-07-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginResourceDTO {
    
    /**
     * 资源路径
     */
    private String path;
    
    /**
     * 资源类型
     */
    private String type;
    
    /**
     * 资源大小（字节）
     */
    private long size;
    
    /**
     * 是否为目录
     */
    private boolean directory;
    
    /**
     * 最后修改时间
     */
    private long lastModified;
} 