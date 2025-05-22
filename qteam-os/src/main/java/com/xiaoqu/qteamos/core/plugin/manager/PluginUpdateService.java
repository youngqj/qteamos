package com.xiaoqu.qteamos.core.plugin.manager;

import com.xiaoqu.qteamos.api.core.plugin.exception.PluginLifecycleException;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import com.xiaoqu.qteamos.core.plugin.running.PluginState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件更新服务
 * 负责检查和执行插件更新
 *
 * @author yangqijun
 * @date 2024-07-15
 */
@Service
public class PluginUpdateService {
    private static final Logger log = LoggerFactory.getLogger(PluginUpdateService.class);
    
    @Autowired
    private PluginRegistry pluginRegistry;
    
    @Autowired
    @Qualifier("enhancedPluginVersionManager")
    private EnhancedPluginVersionManager versionManager;
    
    @Autowired
    private PluginLifecycleManager lifecycleManager;
    
    // 存储等待更新的插件
    private final Map<String, String> pendingUpdates = new ConcurrentHashMap<>();
    
    // 存储更新结果
    private final Map<String, UpdateResult> updateResults = new ConcurrentHashMap<>();
    
    /**
     * 检查插件更新
     * 定期执行，检查所有插件是否有可用更新
     */
    @Scheduled(fixedDelay = 3600000) // 每小时执行一次
    public void checkForUpdates() {
        log.info("开始检查插件更新...");
        
        List<PluginInfo> availableUpdates = new ArrayList<>();
        
        for (PluginInfo pluginInfo : pluginRegistry.getAllPlugins()) {
            String pluginId = pluginInfo.getDescriptor().getPluginId();
            String currentVersion = pluginInfo.getDescriptor().getVersion();
            
            if (versionManager.hasUpdate(pluginId, currentVersion)) {
                Optional<String> latestVersion = versionManager.getLatestVersionString(pluginId);
                if (latestVersion.isPresent()) {
                    log.info("插件有更新可用: {} 当前版本:{} 最新版本:{}", 
                            pluginId, currentVersion, latestVersion.get());
                    
                    // 只有当前版本与最新版本兼容时才添加到可用更新列表
                    if (versionManager.checkVersionCompatibility(pluginId, currentVersion, latestVersion.get())) {
                        availableUpdates.add(pluginInfo);
                        
                        // 添加到待更新列表
                        pendingUpdates.put(pluginId, latestVersion.get());
                    } else {
                        log.warn("插件最新版本与当前版本不兼容: {} 当前版本:{} 最新版本:{}", 
                                pluginId, currentVersion, latestVersion.get());
                    }
                }
            }
        }
        
        log.info("插件更新检查完成，{}个插件有可用更新", availableUpdates.size());
    }
    
    /**
     * 获取可用更新列表
     *
     * @return 可用更新列表
     */
    public Map<String, String> getAvailableUpdates() {
        return pendingUpdates;
    }
    
    /**
     * 执行自动更新
     * 更新所有待更新状态的插件
     *
     * @return 更新结果
     */
    public List<UpdateResult> performAutomaticUpdates() {
        log.info("开始执行自动更新，共{}个插件", pendingUpdates.size());
        
        List<UpdateResult> results = new ArrayList<>();
        
        for (Map.Entry<String, String> entry : pendingUpdates.entrySet()) {
            String pluginId = entry.getKey();
            String targetVersion = entry.getValue();
            
            UpdateResult result = updatePlugin(pluginId, targetVersion);
            results.add(result);
            
            if (result.isSuccess()) {
                // 从待更新列表中移除
                pendingUpdates.remove(pluginId);
            }
        }
        
        log.info("自动更新完成，成功:{}，失败:{}", 
                results.stream().filter(UpdateResult::isSuccess).count(),
                results.stream().filter(r -> !r.isSuccess()).count());
        
        return results;
    }
    
    /**
     * 更新单个插件
     *
     * @param pluginId 插件ID
     * @param targetVersion 目标版本
     * @return 更新结果
     */
    public UpdateResult updatePlugin(String pluginId, String targetVersion) {
        log.info("开始更新插件: {} 目标版本:{}", pluginId, targetVersion);
        
        UpdateResult result = new UpdateResult(pluginId);
        result.setTargetVersion(targetVersion);
        
        try {
            // 获取当前插件信息
            Optional<PluginInfo> optCurrentPlugin = pluginRegistry.getPlugin(pluginId);
            if (optCurrentPlugin.isEmpty()) {
                throw new PluginLifecycleException("插件不存在: " + pluginId);
            }
            
            PluginInfo currentPlugin = optCurrentPlugin.get();
            String currentVersion = currentPlugin.getDescriptor().getVersion();
            result.setCurrentVersion(currentVersion);
            
            // 检查是否需要更新
            if (versionManager.compareVersions(currentVersion, targetVersion) >= 0) {
                log.info("插件已经是最新版本，无需更新: {} {}", pluginId, currentVersion);
                result.setSuccess(true);
                result.setMessage("插件已经是最新版本");
                updateResults.put(pluginId, result);
                return result;
            }
            
            // 检查版本兼容性
            if (!versionManager.checkVersionCompatibility(pluginId, currentVersion, targetVersion)) {
                throw new PluginLifecycleException("版本不兼容，无法直接更新: " + 
                        currentVersion + " -> " + targetVersion);
            }
            
            // 备份当前插件状态以便回滚
            boolean wasRunning = currentPlugin.getState() == PluginState.RUNNING;
            
            // TODO: 这里应该加载新版本插件文件并创建新的PluginInfo对象
            // 此处简化处理，假设已经有了新版本的PluginInfo对象
            PluginInfo newPlugin = createOrLoadNewPluginVersion(pluginId, targetVersion);
            
            if (newPlugin == null) {
                throw new PluginLifecycleException("无法加载新版本插件: " + targetVersion);
            }
            
            // 执行更新
            result.setStartTime(System.currentTimeMillis());
            
            boolean updateSuccess = lifecycleManager.updatePlugin(currentPlugin, newPlugin);
            
            result.setEndTime(System.currentTimeMillis());
            result.setSuccess(updateSuccess);
            
            if (updateSuccess) {
                result.setMessage("更新成功");
                versionManager.markVersionDeployed(pluginId, targetVersion);
                log.info("插件更新成功: {} {} -> {}", pluginId, currentVersion, targetVersion);
            } else {
                result.setMessage("更新失败");
                log.error("插件更新失败: {} {} -> {}", pluginId, currentVersion, targetVersion);
            }
        } catch (Exception e) {
            log.error("插件更新异常: " + pluginId, e);
            result.setSuccess(false);
            result.setMessage("更新异常: " + e.getMessage());
            result.setEndTime(System.currentTimeMillis());
        }
        
        // 保存更新结果
        updateResults.put(pluginId, result);
        
        return result;
    }
    
    /**
     * 获取更新结果
     *
     * @param pluginId 插件ID
     * @return 更新结果
     */
    public Optional<UpdateResult> getUpdateResult(String pluginId) {
        return Optional.ofNullable(updateResults.get(pluginId));
    }
    
    /**
     * 获取所有更新结果
     *
     * @return 所有更新结果
     */
    public List<UpdateResult> getAllUpdateResults() {
        return new ArrayList<>(updateResults.values());
    }
    
    /**
     * 创建或加载新版本插件
     * 此处为示例方法，实际项目中应从存储库或文件系统加载新版本插件
     *
     * @param pluginId 插件ID
     * @param targetVersion 目标版本
     * @return 新版本插件信息
     */
    private PluginInfo createOrLoadNewPluginVersion(String pluginId, String targetVersion) {
        // TODO: 从存储库或文件系统加载新版本插件
        // 此处简化处理，使用当前版本插件信息，修改版本号
        Optional<PluginInfo> optCurrentPlugin = pluginRegistry.getPlugin(pluginId);
        if (optCurrentPlugin.isEmpty()) {
            return null;
        }
        
        // 注意：这里只是为了演示，实际应该创建新的PluginInfo对象
        PluginInfo currentPlugin = optCurrentPlugin.get();
        PluginInfo newPlugin = PluginInfo.builder()
                .descriptor(currentPlugin.getDescriptor())
                .file(currentPlugin.getPluginFile())
                .jarPath(currentPlugin.getJarPath())
                .state(PluginState.CREATED)
                .build();
        
        // 设置新版本信息
        newPlugin.getDescriptor().getUpdateInfo().put("previousVersion", 
                currentPlugin.getDescriptor().getVersion());
        
        // 返回新版本插件信息
        return newPlugin;
    }
    
    /**
     * 更新结果类
     */
    public static class UpdateResult {
        private String pluginId;
        private String currentVersion;
        private String targetVersion;
        private boolean success;
        private String message;
        private long startTime;
        private long endTime;
        
        public UpdateResult(String pluginId) {
            this.pluginId = pluginId;
            this.startTime = System.currentTimeMillis();
        }
        
        public String getPluginId() {
            return pluginId;
        }
        
        public String getCurrentVersion() {
            return currentVersion;
        }
        
        public void setCurrentVersion(String currentVersion) {
            this.currentVersion = currentVersion;
        }
        
        public String getTargetVersion() {
            return targetVersion;
        }
        
        public void setTargetVersion(String targetVersion) {
            this.targetVersion = targetVersion;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public long getStartTime() {
            return startTime;
        }
        
        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }
        
        public long getEndTime() {
            return endTime;
        }
        
        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }
        
        public long getDuration() {
            return endTime - startTime;
        }
    }
} 