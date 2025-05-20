/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-30 15:22:59
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-04-30 22:03:41
 * @FilePath: /qteamos/src/main/java/com/xiaoqu/qteamos/core/plugin/model/dto/PluginDTO.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.core.plugin.model.dto;

import lombok.Data;
import lombok.Builder;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

/**
 * 插件数据传输对象
 * 
 * @author yangqijun
 * @version 1.0.0
 */
@Data
@Builder
public class PluginDTO {
    
    @NotBlank(message = "插件ID不能为空")
    private String pluginId;
    
    @NotBlank(message = "插件名称不能为空")
    private String name;
    
    @NotBlank(message = "插件版本不能为空")
    private String version;
    
    private String description;
    
    private String author;
    
    @NotBlank(message = "主类不能为空")
    private String mainClass;
    
    @NotBlank(message = "插件类型不能为空")
    private String type;
    
    private String trust;
    
    private List<PluginDependencyDTO> dependencies;
    
    private String requiredSystemVersion;
    
    private boolean enabled;
    
    private int priority;
    
    private Map<String, Object> properties;
    
    private List<String> permissions;
    
    private Map<String, String> lifecycle;
    
    private List<ExtensionPointDTO> extensionPoints;
}
