package com.xiaoqu.qteamos.core.plugin.running;

import lombok.Builder;
import lombok.Data;

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
     * trusted: 受信任的
     * official: 官方的
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
    private int priority;
    
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
    private Map<String, Object> updateInfo;
    
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
        return "trusted".equalsIgnoreCase(trust) || "official".equalsIgnoreCase(trust);
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
        if (updateInfo != null && updateInfo.containsKey("previousVersion")) {
            return (String) updateInfo.get("previousVersion");
        }
        return null;
    }
    
    /**
     * 检查插件是否包含数据库变更
     * 
     * @return 是否包含数据库变更
     */
    public boolean hasDatabaseChanges() {
        if (updateInfo != null && updateInfo.containsKey("databaseChange")) {
            return (boolean) updateInfo.getOrDefault("databaseChange", false);
        }
        return false;
    }
    
    /**
     * 获取数据库迁移脚本列表
     * 
     * @return 迁移脚本列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getMigrationScripts() {
        if (updateInfo != null && updateInfo.containsKey("migrationScripts")) {
            return (List<String>) updateInfo.get("migrationScripts");
        }
        return List.of();
    }
    
    /**
     * 检查插件是否支持回滚
     * 
     * @return 是否支持回滚
     */
    @SuppressWarnings("unchecked")
    public boolean supportsRollback() {
        if (updateInfo != null && updateInfo.containsKey("rollback")) {
            Map<String, Object> rollbackInfo = (Map<String, Object>) updateInfo.get("rollback");
            return (boolean) rollbackInfo.getOrDefault("supported", false);
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
        if (extensionPoints == null || extensionPointId == null) {
            return null;
        }
        
        return extensionPoints.stream()
                .filter(ep -> extensionPointId.equals(ep.getId()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     *.检查插件是否提供指定的扩展点
     *
     * @param extensionPointId 扩展点ID
     * @return 是否提供该扩展点
     */
    public boolean providesExtensionPoint(String extensionPointId) {
        return getExtensionPoint(extensionPointId) != null;
    }
} 