package com.xiaoqu.qteamos.core.plugin.running;

import lombok.Builder;
import lombok.Data;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 插件配置
 * 管理插件的全局配置和特定配置
 * 
 * @author yangqijun
 * @version 1.0.0
 */
@Data
@Builder
public class PluginConfig {
    
    /**
     * 插件目录
     */
    private File pluginsDir;
    
    /**
     * 插件临时目录
     */
    private File tempDir;
    
    /**
     * 插件配置目录
     */
    private File configDir;
    
    /**
     * 自动加载插件
     */
    private boolean autoLoad;
    
    /**
     * 自动启动插件
     */
    private boolean autoStart;
    
    /**
     * 开发模式
     * 在开发模式下，会加载更多的调试信息
     */
    private boolean devMode;
    
    /**
     * 插件扫描间隔（毫秒）
     * 用于热部署，如果为0则禁用热部署
     */
    private long scanInterval;
    
    /**
     * 插件检查超时（毫秒）
     */
    private long checkTimeout;
    
    /**
     * 系统版本
     */
    private String systemVersion;
    
    /**
     * 插件属性
     */
    @Builder.Default
    private Map<String, Object> properties = new HashMap<>();
    
    /**
     * 获取插件属性值
     * 
     * @param key 属性键
     * @return 属性值
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    /**
     * 获取插件属性值，如果不存在则返回默认值
     * 
     * @param key 属性键
     * @param defaultValue 默认值
     * @return 属性值或默认值
     */
    public Object getProperty(String key, Object defaultValue) {
        return properties.getOrDefault(key, defaultValue);
    }
    
    /**
     * 设置插件属性值
     * 
     * @param key 属性键
     * @param value 属性值
     * @return 当前配置实例
     */
    public PluginConfig setProperty(String key, Object value) {
        properties.put(key, value);
        return this;
    }
    
    /**
     * 是否包含指定属性
     * 
     * @param key 属性键
     * @return 是否包含
     */
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }
    
    /**
     * 移除属性
     * 
     * @param key 属性键
     * @return 被移除的属性值
     */
    public Object removeProperty(String key) {
        return properties.remove(key);
    }
    
    /**
     * 清空所有属性
     */
    public void clearProperties() {
        properties.clear();
    }
} 