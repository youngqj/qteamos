package com.xiaoqu.qteamos.core.plugin.manager;


import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 插件注册表
 * 负责管理所有已安装插件的信息，提供插件查询、注册和移除功能
 *
 * @author yangqijun
 * @date 2024-07-02
 */
@Component
public class PluginRegistry {
    private static final Logger log = LoggerFactory.getLogger(PluginRegistry.class);
    
    // 存储所有已注册的插件，key为插件ID
    private final Map<String, PluginInfo> pluginsMap = new ConcurrentHashMap<>();
    
    /**
     * 注册插件
     *
     * @param pluginInfo 插件信息
     * @return 注册是否成功
     */
    public boolean registerPlugin(PluginInfo pluginInfo) {
        if (pluginInfo == null || pluginInfo.getDescriptor() == null) {
            log.error("无法注册插件: 插件信息不完整");
            return false;
        }
        
        String pluginId = pluginInfo.getDescriptor().getPluginId();
        if (pluginsMap.containsKey(pluginId)) {
            log.warn("插件已存在: {}", pluginId);
            return false;
        }
        
        pluginsMap.put(pluginId, pluginInfo);
        log.info("插件注册成功: {}, 版本: {}", pluginId, pluginInfo.getDescriptor().getVersion());
        return true;
    }
    
    /**
     * 更新插件信息
     *
     * @param pluginInfo 更新后的插件信息
     */
    public void updatePlugin(PluginInfo pluginInfo) {
        if (pluginInfo == null || pluginInfo.getDescriptor() == null) {
            return;
        }
        
        String pluginId = pluginInfo.getDescriptor().getPluginId();
        pluginsMap.put(pluginId, pluginInfo);
        log.info("插件信息已更新: {}", pluginId);
    }
    
    /**
     * 移除插件
     *
     * @param pluginId 插件ID
     * @return 移除的插件信息
     */
    public Optional<PluginInfo> unregisterPlugin(String pluginId) {
        PluginInfo removed = pluginsMap.remove(pluginId);
        if (removed != null) {
            log.info("插件已从注册表移除: {}", pluginId);
            return Optional.of(removed);
        }
        return Optional.empty();
    }
    
    /**
     * 获取插件信息
     *
     * @param pluginId 插件ID
     * @return 插件信息
     */
    public Optional<PluginInfo> getPlugin(String pluginId) {
        return Optional.ofNullable(pluginsMap.get(pluginId));
    }
    
    /**
     * 获取所有已注册的插件
     *
     * @return 所有插件信息的集合
     */
    public Collection<PluginInfo> getAllPlugins() {
        return pluginsMap.values();
    }
    
    /**
     * 根据条件筛选插件
     * 
     * @param enabled 是否启用
     * @return 符合条件的插件列表
     */
    public Collection<PluginInfo> getPluginsByState(boolean enabled) {
        return pluginsMap.values().stream()
                .filter(plugin -> plugin.isEnabled() == enabled)
                .collect(Collectors.toList());
    }
    
    /**
     * 检查插件是否存在
     *
     * @param pluginId 插件ID
     * @return 插件是否存在
     */
    public boolean hasPlugin(String pluginId) {
        return pluginsMap.containsKey(pluginId);
    }
    
    /**
     * 获取注册的插件数量
     *
     * @return 插件数量
     */
    public int getPluginCount() {
        return pluginsMap.size();
    }
    
    /**
     * 清空注册表
     */
    public void clear() {
        pluginsMap.clear();
        log.info("插件注册表pluginsMap已清空");
    }
} 