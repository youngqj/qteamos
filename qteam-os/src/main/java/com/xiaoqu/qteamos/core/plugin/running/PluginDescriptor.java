package com.xiaoqu.qteamos.core.plugin.running;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * 插件描述符
 * 用于描述插件的基本信息和元数据
 * 简化版本，适用于私有化部署环境
 * 
 * @author yangqijun
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PluginDescriptor {
    
    /**
     * 插件ID，全局唯一标识
     * -- GETTER --
     *  获取插件的唯一标识
     *
     * @return 插件ID
     */
    private String pluginId;
    
    /**
     * 插件名称
     */
    private String name;
    
    /**
     * 插件版本
     */
    private String version;
    
    /**
     * 插件描述
     */
    private String description;
    
    /**
     * 插件作者
     */
    private String author;
    
    /**
     * 插件主类
     * 插件的入口类，必须实现Plugin接口
     */
    private String mainClass;
    
    /**
     * 插件类型
     * normal: 普通插件
     * system: 系统插件
     */
    private String type;
    
    /**
     * 插件信任级别
     * trust: 受信任的
     * untrusted: 不受信任的
     */
    private String trust;
    
    /**
     * 插件依赖列表
     */
    private List<PluginDependency> dependencies;
    
    /**
     * 插件的最小系统版本要求
     */
    private String requiredSystemVersion;
    
    /**
     * 插件是否启用
     */
    private boolean enabled;
    
    /**
     * 插件优先级，数值越小优先级越高
     */
    private Integer priority;
    
    /**
     * 插件的配置项
     */
    private Map<String, Object> properties;
    
    /**
     * 插件申请的权限列表
     */
    private List<String> permissions;
    
    /**
     * 更新相关信息
     */
    private Map<String, String> updateInfo = new java.util.HashMap<>();
    
    /**
     * 生命周期钩子配置
     */
    private Map<String, String> lifecycle;
    
    /**
     * 插件提供的扩展点列表
     */
    private List<ExtensionPoint> extensionPoints;
    
    /**
     * 插件资源文件列表
     */
    private List<PluginResource> resources;
    
    /**
     * 插件元数据，存储任意额外信息
     */
    private Map<String, Object> metadata;

    /**
     * 获取插件的完整标识（ID和版本）
     * 
     * @return 插件的完整标识
     */
    public String getFullId() {
        return pluginId + "@" + version;
    }
    
    /**
     * 检查插件描述符的有效性
     * 
     * @return 是否有效
     */
    public boolean isValid() {
        return pluginId != null && !pluginId.isEmpty() 
                && name != null && !name.isEmpty()
                && version != null && !version.isEmpty()
                && mainClass != null && !mainClass.isEmpty();
    }
    
    /**
     * 检查插件是否为系统插件
     * 
     * @return 是否为系统插件
     */
    public boolean isSystemPlugin() {
        return "system".equalsIgnoreCase(type);
    }
    
    /**
     * 检查插件是否受信任
     * 
     * @return 是否受信任
     */
    public boolean isTrusted() {
        return trust != null && "trust".equalsIgnoreCase(trust);
    }
    
    /**
     * 检查插件是否需要特定权限
     * 
     * @param permission 权限名称
     * @return 是否需要该权限
     */
    public boolean requiresPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
    
    /**
     * 获取上一个版本号
     * 
     * @return 上一个版本号，如果未指定则返回null
     */
    public String getPreviousVersion() {
        if (updateInfo != null) {
            return updateInfo.get("previousVersion");
        }
        return null;
    }
    
    /**
     * 检查插件是否包含数据库变更
     * 
     * @return 是否包含数据库变更
     */
    public boolean hasDatabaseChanges() {
        if (updateInfo != null) {
            return "true".equalsIgnoreCase(updateInfo.get("databaseChange"));
        }
        return false;
    }
    
    /**
     * 获取迁移脚本列表
     * 
     * @return 迁移脚本列表
     */
    public List<String> getMigrationScripts() {
        if (updateInfo != null && updateInfo.containsKey("migrationScripts")) {
            String scripts = updateInfo.get("migrationScripts");
            return List.of(scripts.split(","));
        }
        return List.of();
    }
    
    /**
     * 检查插件是否支持回滚
     * 
     * @return 是否支持回滚
     */
    public boolean supportsRollback() {
        if (updateInfo != null && updateInfo.containsKey("rollback")) {
            String rollbackInfo = updateInfo.get("rollback");
            return Boolean.parseBoolean(rollbackInfo.split(",")[0]);
        }
        return false;
    }
    
    /**
     * 获取初始化方法名
     *
     * @return 初始化方法名，如果未指定则返回null
     */
    public String getInitMethodName() {
        if (lifecycle != null && lifecycle.containsKey("init")) {
            return lifecycle.get("init");
        }
        return null;
    }
    
    /**
     * 获取启动方法名
     *
     * @return 启动方法名，如果未指定则返回null
     */
    public String getStartMethodName() {
        if (lifecycle != null && lifecycle.containsKey("start")) {
            return lifecycle.get("start");
        }
        return null;
    }
    
    /**
     * 获取停止方法名
     *
     * @return 停止方法名，如果未指定则返回null
     */
    public String getStopMethodName() {
        if (lifecycle != null && lifecycle.containsKey("stop")) {
            return lifecycle.get("stop");
        }
        return null;
    }
    
    /**
     * 获取卸载方法名
     *
     * @return 卸载方法名，如果未指定则返回null
     */
    public String getUnloadMethodName() {
        if (lifecycle != null && lifecycle.containsKey("unload")) {
            return lifecycle.get("unload");
        }
        return null;
    }
    
    /**
     * 获取指定ID的扩展点
     *
     * @param extensionPointId 扩展点ID
     * @return 扩展点对象，如果不存在则返回null
     */
    public ExtensionPoint getExtensionPoint(String extensionPointId) {
        if (extensionPoints == null || extensionPoints.isEmpty()) {
            return null;
        }
        
        return extensionPoints.stream()
                .filter(ep -> extensionPointId.equals(ep.getId()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 检查插件是否提供指定ID的扩展点
     *
     * @param extensionPointId 扩展点ID
     * @return 是否提供该扩展点
     */
    public boolean providesExtensionPoint(String extensionPointId) {
        return getExtensionPoint(extensionPointId) != null;
    }
    
    /**
     * 创建一个PluginDescriptor构建器
     *
     * @return PluginDescriptorBuilder实例
     */
    public static PluginDescriptorBuilder builder() {
        return new PluginDescriptorBuilder();
    }
    
    /**
     * 插件描述符构建器
     * 用于构建PluginDescriptor实例
     */
    public static class PluginDescriptorBuilder {
        private final PluginDescriptor descriptor = new PluginDescriptor();
        
        public PluginDescriptorBuilder pluginId(String pluginId) {
            descriptor.pluginId = pluginId;
            return this;
        }
        
        public PluginDescriptorBuilder name(String name) {
            descriptor.name = name;
            return this;
        }
        
        public PluginDescriptorBuilder version(String version) {
            descriptor.version = version;
            return this;
        }
        
        public PluginDescriptorBuilder description(String description) {
            descriptor.description = description;
            return this;
        }
        
        public PluginDescriptorBuilder author(String author) {
            descriptor.author = author;
            return this;
        }
        
        public PluginDescriptorBuilder mainClass(String mainClass) {
            descriptor.mainClass = mainClass;
            return this;
        }
        
        public PluginDescriptorBuilder type(String type) {
            descriptor.type = type;
            return this;
        }
        
        public PluginDescriptorBuilder trust(String trust) {
            descriptor.trust = trust;
            return this;
        }
        
        public PluginDescriptorBuilder dependencies(List<PluginDependency> dependencies) {
            descriptor.dependencies = dependencies;
            return this;
        }
        
        public PluginDescriptorBuilder requiredSystemVersion(String requiredSystemVersion) {
            descriptor.requiredSystemVersion = requiredSystemVersion;
            return this;
        }
        
        public PluginDescriptorBuilder enabled(boolean enabled) {
            descriptor.enabled = enabled;
            return this;
        }
        
        public PluginDescriptorBuilder priority(Integer priority) {
            descriptor.priority = priority;
            return this;
        }
        
        public PluginDescriptorBuilder properties(Map<String, Object> properties) {
            descriptor.properties = properties;
            return this;
        }
        
        public PluginDescriptorBuilder permissions(List<String> permissions) {
            descriptor.permissions = permissions;
            return this;
        }
        
        public PluginDescriptorBuilder updateInfo(Map<String, String> updateInfo) {
            descriptor.updateInfo = updateInfo;
            return this;
        }
        
        public PluginDescriptorBuilder lifecycle(Map<String, String> lifecycle) {
            descriptor.lifecycle = lifecycle;
            return this;
        }
        
        public PluginDescriptorBuilder extensionPoints(List<ExtensionPoint> extensionPoints) {
            descriptor.extensionPoints = extensionPoints;
            return this;
        }
        
        public PluginDescriptorBuilder resources(List<PluginResource> resources) {
            descriptor.resources = resources;
            return this;
        }
        
        public PluginDescriptorBuilder metadata(Map<String, Object> metadata) {
            descriptor.metadata = metadata;
            return this;
        }
        
        public PluginDescriptor build() {
            return descriptor;
        }
    }
} 