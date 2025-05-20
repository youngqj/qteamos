/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-30 21:57:44
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-04-30 21:57:44
 * @FilePath: /qteamos/src/main/java/com/xiaoqu/qteamos/core/plugin/model/dto/PluginDependencyDTO.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.core.plugin.model.dto;

import lombok.Data;
import lombok.Builder;

/**
 * 插件依赖数据传输对象
 * 
 * @author yangqijun
 * @version 1.0.0
 */
@Data
@Builder
public class PluginDependencyDTO {
    
    /**
     * 依赖插件ID
     */
    private String pluginId;
    
    /**
     * 版本要求
     */
    private String version;
    
    /**
     * 是否可选依赖
     */
    private boolean optional;
}
