/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-30 21:57:44
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-04-30 21:57:44
 * @FilePath: /qteamos/src/main/java/com/xiaoqu/qteamos/core/plugin/model/dto/ExtensionPointDTO.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.core.plugin.model.dto;

import lombok.Data;
import lombok.Builder;
import java.util.Map;

/**
 * 插件扩展点数据传输对象
 * 
 * @author yangqijun
 * @version 1.0.0
 */
@Data
@Builder
public class ExtensionPointDTO {
    
    /**
     * 扩展点ID
     */
    private String id;
    
    /**
     * 扩展点名称
     */
    private String name;
    
    /**
     * 扩展点描述
     */
    private String description;
    
    /**
     * 扩展点类型（可选）
     * 例如：interface, abstract, annotation等
     */
    private String type;
    
    /**
     * 扩展点接口或抽象类全限定名
     */
    private String interfaceClass;
    
    /**
     * 是否允许多个实现
     */
    private boolean multiple = true;
    
    /**
     * 是否必须实现
     */
    private boolean required = false;
    
    /**
     * 扩展点元数据
     */
    private Map<String, Object> metadata;
} 