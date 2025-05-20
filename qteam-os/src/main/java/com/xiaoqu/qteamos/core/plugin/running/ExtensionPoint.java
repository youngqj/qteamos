package com.xiaoqu.qteamos.core.plugin.running;

import lombok.Builder;
import lombok.Data;

/**
 * 插件扩展点
 * 描述插件提供的可被其他插件扩展的接口点
 * 
 * @author yangqijun
 * @version 1.0.0
 */
@Data
@Builder
public class ExtensionPoint {
    
    /**
     * 扩展点ID，全局唯一标识
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
     * 默认为true
     */
    @Builder.Default
    private boolean multiple = true;
    
    /**
     * 是否必须实现
     * 默认为false
     */
    @Builder.Default
    private boolean required = false;
    
    /**
     * 获取扩展点ID
     * 
     * @return 扩展点ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * 获取扩展点完整ID（包含插件ID）
     * 
     * @param pluginId 插件ID
     * @return 扩展点完整ID
     */
    public String getFullId(String pluginId) {
        return pluginId + "." + id;
    }
    
    /**
     * 检查扩展点有效性
     * 
     * @return 是否有效
     */
    public boolean isValid() {
        return id != null && !id.isEmpty() && 
               name != null && !name.isEmpty();
    }
} 