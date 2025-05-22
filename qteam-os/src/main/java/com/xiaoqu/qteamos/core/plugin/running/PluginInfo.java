package com.xiaoqu.qteamos.core.plugin.running;


import com.xiaoqu.qteamos.core.plugin.loader.DynamicClassLoader;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

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
@NoArgsConstructor
@AllArgsConstructor
public class PluginInfo {
    
    /**
     * 插件描述符
     */
    private PluginDescriptor descriptor;
    
    /**
     * 插件JAR文件路径
     */
    private Path jarPath;
    
    /**
     * 插件文件
     */
    private File file;
    
    /**
     * 插件类加载器
     */
    private DynamicClassLoader classLoader;
    
    /**
     * 插件状态
     */
    private PluginState state = PluginState.CREATED;
    
    /**
     * 插件实例
     */
    private Object instance;
    
    /**
     * 插件上下文
     */
    private Object context;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 插件是否启用
     */
    private boolean enabled = true;
    
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
     * 插件元数据
     */
    private Map<String, Object> metadata;
    
    /**
     * 资源路径映射
     * @deprecated 使用metadata替代
     */
    @Deprecated
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
    
    /**
     * 获取插件类加载器
     *
     * @return 类加载器
     */
    public DynamicClassLoader getClassLoader() {
        return classLoader;
    }
    
    /**
     * 设置插件类加载器
     *
     * @param classLoader 类加载器
     */
    public void setClassLoader(DynamicClassLoader classLoader) {
        this.classLoader = classLoader;
    }
    
    /**
     * 获取插件文件
     * 为了兼容旧代码
     * 
     * @return 插件文件
     */
    public File getPluginFile() {
        return file;
    }
    
    /**
     * 设置插件文件
     * 为了兼容旧代码
     * 
     * @param pluginFile 插件文件
     */
    public void setPluginFile(File pluginFile) {
        this.file = pluginFile;
    }
    
    /**
     * 获取插件实例
     * 为了兼容旧代码
     * 
     * @return 插件实例
     */
    public Object getPluginInstance() {
        return instance;
    }
    
    /**
     * 设置插件实例
     * 为了兼容旧代码
     * 
     * @param pluginInstance 插件实例
     */
    public void setPluginInstance(Object pluginInstance) {
        this.instance = pluginInstance;
    }
    
    /**
     * 获取资源路径
     * 为了兼容旧代码
     * 
     * @return 资源路径
     */
    public Map<String, String> getResourcePaths() {
        return resourcePaths;
    }
    
    /**
     * 设置资源路径
     * 为了兼容旧代码
     * 
     * @param resourcePaths 资源路径
     */
    public void setResourcePaths(Map<String, String> resourcePaths) {
        this.resourcePaths = resourcePaths;
    }
    
    @Override
    public String toString() {
        return "PluginInfo{" +
                "descriptor=" + descriptor +
                ", state=" + state +
                ", enabled=" + enabled +
                '}';
    }
    
    /**
     * 创建一个PluginInfo构建器
     *
     * @return PluginInfoBuilder实例
     */
    public static PluginInfoBuilder builder() {
        return new PluginInfoBuilder();
    }
    
    /**
     * 插件信息构建器
     * 用于构建PluginInfo实例
     */
    public static class PluginInfoBuilder {
        private final PluginInfo pluginInfo = new PluginInfo();
        
        public PluginInfoBuilder descriptor(PluginDescriptor descriptor) {
            pluginInfo.descriptor = descriptor;
            return this;
        }
        
        public PluginInfoBuilder jarPath(Path jarPath) {
            pluginInfo.jarPath = jarPath;
            return this;
        }
        
        public PluginInfoBuilder file(File file) {
            pluginInfo.file = file;
            return this;
        }
        
        public PluginInfoBuilder classLoader(DynamicClassLoader classLoader) {
            pluginInfo.classLoader = classLoader;
            return this;
        }
        
        public PluginInfoBuilder state(PluginState state) {
            pluginInfo.state = state;
            return this;
        }
        
        public PluginInfoBuilder instance(Object instance) {
            pluginInfo.instance = instance;
            return this;
        }
        
        public PluginInfoBuilder context(Object context) {
            pluginInfo.context = context;
            return this;
        }
        
        public PluginInfoBuilder errorMessage(String errorMessage) {
            pluginInfo.errorMessage = errorMessage;
            return this;
        }
        
        public PluginInfoBuilder enabled(boolean enabled) {
            pluginInfo.enabled = enabled;
            return this;
        }
        
        public PluginInfoBuilder loadTime(Date loadTime) {
            pluginInfo.loadTime = loadTime;
            return this;
        }
        
        public PluginInfoBuilder startTime(Date startTime) {
            pluginInfo.startTime = startTime;
            return this;
        }
        
        public PluginInfoBuilder stopTime(Date stopTime) {
            pluginInfo.stopTime = stopTime;
            return this;
        }
        
        public PluginInfoBuilder metadata(Map<String, Object> metadata) {
            pluginInfo.metadata = metadata;
            return this;
        }
        
        public PluginInfoBuilder resourcePaths(Map<String, String> resourcePaths) {
            pluginInfo.resourcePaths = resourcePaths;
            return this;
        }
        
        public PluginInfo build() {
            return pluginInfo;
        }
    }
} 