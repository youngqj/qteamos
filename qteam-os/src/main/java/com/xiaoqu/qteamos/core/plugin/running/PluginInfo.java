package com.xiaoqu.qteamos.core.plugin.running;


import com.xiaoqu.qteamos.core.plugin.loader.DynamicClassLoader;
import lombok.Builder;
import lombok.Data;

import java.io.File;
import java.nio.file.Path;
import java.util.Date;
import java.util.Map;

/**
 * 插件信息
 * 表示已加载的插件的运行时信息
 * 
 * @author yangqijun
 * @version 1.0.0
 */
@Data
@Builder
public class PluginInfo {
    
    /**
     * 插件描述符
     */
    private PluginDescriptor descriptor;
    
    /**
     * 插件文件
     */
    private File pluginFile;
    
    /**
     * 插件类加载器
     */
    private DynamicClassLoader classLoader;
    
    /**
     * 插件实例
     */
    private Object pluginInstance;
    
    /**
     * 插件状态
     */
    private PluginState state;
    
    /**
     * 加载时间
     */
    private Date loadTime;
    
    /**
     * 启动时间
     */
    private Date startTime;
    
    /**
     * 停止时间
     */
    private Date stopTime;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 插件JAR文件路径
     */
    private Path jarPath;
    
    /**
     * 是否启用
     */
    @Builder.Default
    private boolean enabled = false;
    
    /**
     * 资源路径映射
     */
    private Map<String, String> resourcePaths;
    
    /**
     * 获取插件ID
     * 
     * @return 插件ID
     */
    public String getPluginId() {
        return descriptor != null ? descriptor.getPluginId() : null;
    }
    
    /**
     * 获取插件名称
     * 
     * @return 插件名称
     */
    public String getName() {
        return descriptor != null ? descriptor.getName() : null;
    }
    
    /**
     * 获取插件版本
     * 
     * @return 插件版本
     */
    public String getVersion() {
        return descriptor != null ? descriptor.getVersion() : null;
    }
    
    /**
     * 检查插件是否已启动
     * 
     * @return 是否已启动
     */
    public boolean isStarted() {
        return state == PluginState.STARTED;
    }
    
    /**
     * 检查插件是否已停止
     * 
     * @return 是否已停止
     */
    public boolean isStopped() {
        return state == PluginState.STOPPED;
    }
    
    /**
     * 检查插件是否发生错误
     * 
     * @return 是否发生错误
     */
    public boolean hasError() {
        return state == PluginState.ERROR;
    }
    
    /**
     * 获取插件描述符
     * 
     * @return 插件描述符
     */
    public PluginDescriptor getDescriptor() {
        return descriptor;
    }
    
    /**
     * 设置插件描述符
     * 
     * @param descriptor 插件描述符
     */
    public void setDescriptor(PluginDescriptor descriptor) {
        this.descriptor = descriptor;
    }
    
    /**
     * 获取插件JAR文件路径
     * 
     * @return 插件JAR文件路径
     */
    public Path getJarPath() {
        return jarPath;
    }
    
    /**
     * 设置插件JAR文件路径
     * 
     * @param jarPath 插件JAR文件路径
     */
    public void setJarPath(Path jarPath) {
        this.jarPath = jarPath;
    }
    
    /**
     * 获取插件状态
     * 
     * @return 插件状态
     */
    public PluginState getState() {
        return state;
    }
    
    /**
     * 设置插件状态
     * 
     * @param state 插件状态
     */
    public void setState(PluginState state) {
        this.state = state;
    }
    
    /**
     * 是否启用
     * 
     * @return 是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 设置是否启用
     * 
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * 获取错误信息
     * 
     * @return 错误信息
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * 设置错误信息
     * 
     * @param errorMessage 错误信息
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    @Override
    public String toString() {
        return "PluginInfo{" +
                "descriptor=" + descriptor +
                ", state=" + state +
                ", enabled=" + enabled +
                '}';
    }
} 