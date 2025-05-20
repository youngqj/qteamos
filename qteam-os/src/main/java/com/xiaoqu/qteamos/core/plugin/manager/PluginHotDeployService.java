package com.xiaoqu.qteamos.core.plugin.manager;

import com.xiaoqu.qteamos.core.plugin.event.EventBus;
import com.xiaoqu.qteamos.core.plugin.running.PluginDescriptor;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import com.xiaoqu.qteamos.core.plugin.running.PluginState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 插件信息工厂类
 * 负责创建和复制PluginInfo对象
 */
class PluginInfoFactory {
    
    /**
     * 创建新版本的PluginInfo
     * 
     * @param oldPlugin 旧插件信息
     * @param newPluginPath 新插件路径
     * @param newVersion 新版本号
     * @return 新的PluginInfo对象
     */
    public static PluginInfo createNewVersionInfo(PluginInfo oldPlugin, Path newPluginPath, String newVersion) {
        // 获取旧版本号
        String oldVersion = oldPlugin.getDescriptor().getVersion();
        
        // 创建新的描述符
        PluginDescriptor newDescriptor = PluginDescriptor.builder()
                .pluginId(oldPlugin.getDescriptor().getPluginId())
                .version(newVersion)
                .name(oldPlugin.getDescriptor().getName())
                .build();
        
        // 复制其他属性
        newDescriptor.getUpdateInfo().put("previousVersion", oldVersion);
        
        // 创建新的PluginInfo
        return PluginInfo.builder()
                .descriptor(newDescriptor)
                .pluginFile(newPluginPath.toFile())
                .jarPath(newPluginPath)
                .state(PluginState.CREATED)
                .build();
    }
}

/**
 * 插件热部署服务
 * 支持在系统运行时动态加载/更新插件
 * 
 * @author yangqijun
 * @date 2024-07-15
 */
@Service
public class PluginHotDeployService {
    private static final Logger log = LoggerFactory.getLogger(PluginHotDeployService.class);
    
    @Autowired
    private PluginLifecycleManager lifecycleManager;
    
    @Autowired
    private PluginRegistry pluginRegistry;
    
    @Autowired
    private EventBus eventBus;
    
    // 添加部署历史服务依赖
    @Autowired(required = false)
    private PluginDeploymentHistoryService deploymentHistoryService;
    
    // 添加发布管理器依赖
    @Autowired(required = false)
    private PluginReleaseManager releaseManager;
    
    // 插件目录监控服务
    private WatchService watchService;
    
    // 监控的插件目录
    private Path pluginsDirectory;
    
    // 文件修改时间缓存，用于防止重复处理同一文件变更
    private final Map<Path, Long> fileModificationTimes = new ConcurrentHashMap<>();
    
    // 热部署任务执行器
    private ScheduledExecutorService executorService;
    
    // 正在处理的插件，防止重复处理
    private final Set<String> processingPlugins = ConcurrentHashMap.newKeySet();
    
    // 热部署操作历史
    private final List<DeploymentRecord> deploymentHistory = Collections.synchronizedList(new ArrayList<>());
    
    // 添加插件备份存储
    private final Map<String, PluginBackup> pluginBackups = new ConcurrentHashMap<>();
    
    // 添加资源追踪器
    private final ResourceTracker resourceTracker = new ResourceTracker();
    
    /**
     * 初始化插件热部署服务
     * 
     * @param pluginsDir 插件目录
     * @throws IOException IO异常
     */
    public void initialize(String pluginsDir) throws IOException {
        this.pluginsDirectory = Paths.get(pluginsDir);
        
        // 确保目录存在
        if (!Files.exists(pluginsDirectory)) {
            Files.createDirectories(pluginsDirectory);
        }
        
        // 初始化文件监控
        watchService = FileSystems.getDefault().newWatchService();
        pluginsDirectory.register(watchService, 
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);
        
        // 缓存现有插件文件的修改时间
        Files.list(pluginsDirectory)
                .filter(path -> path.toString().endsWith(".jar"))
                .forEach(path -> fileModificationTimes.put(path, path.toFile().lastModified()));
        
        // 初始化执行器
        executorService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "plugin-hot-deploy");
            t.setDaemon(true);
            return t;
        });
        
        // 启动文件监控任务
        executorService.scheduleWithFixedDelay(
                this::monitorPluginDirectory,
                0,
                5,
                TimeUnit.SECONDS
        );
        
        log.info("插件热部署服务已初始化，监控目录: {}", pluginsDirectory);
    }
    
    /**
     * 关闭服务
     */
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                log.error("关闭文件监控服务异常", e);
            }
        }
        
        log.info("插件热部署服务已关闭");
    }
    
    /**
     * 监控插件目录变化
     */
    private void monitorPluginDirectory() {
        try {
            WatchKey key = watchService.poll();
            if (key == null) {
                return;
            }
            
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                
                // 忽略OVERFLOW事件
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }
                
                // 获取文件名
                @SuppressWarnings("unchecked")
                WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                Path filename = pathEvent.context();
                Path fullPath = pluginsDirectory.resolve(filename);
                
                // 只处理JAR文件
                if (!fullPath.toString().endsWith(".jar")) {
                    continue;
                }
                
                log.debug("检测到插件文件变更: {} - {}", kind, fullPath);
                
                // 处理文件变更
                if (kind == StandardWatchEventKinds.ENTRY_CREATE || 
                        kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                    handleFileCreatedOrModified(fullPath);
                } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                    handleFileDeleted(fullPath);
                }
            }
            
            // 重置key，以便继续接收事件
            key.reset();
            
        } catch (Exception e) {
            log.error("监控插件目录异常", e);
        }
    }
    
    /**
     * 处理插件文件创建或修改事件
     * 
     * @param filePath 文件路径
     */
    private void handleFileCreatedOrModified(Path filePath) {
        try {
            // 获取文件修改时间
            long lastModified = filePath.toFile().lastModified();
            
            // 检查是否是真实的文件变更
            if (fileModificationTimes.containsKey(filePath) && 
                    fileModificationTimes.get(filePath) >= lastModified) {
                return;
            }
            
            // 更新修改时间
            fileModificationTimes.put(filePath, lastModified);
            
            // 等待文件拷贝完成
            Thread.sleep(1000);
            
            // 解析插件ID和版本
            String fileName = filePath.getFileName().toString();
            String pluginIdWithVersion = fileName.substring(0, fileName.lastIndexOf(".jar"));
            
            // 示例格式: plugin-id-1.0.0.jar
            // 解析插件ID和版本
            int lastDashIndex = pluginIdWithVersion.lastIndexOf("-");
            if (lastDashIndex <= 0) {
                log.warn("插件文件名格式不正确: {}", fileName);
                return;
            }
            
            String pluginId = pluginIdWithVersion.substring(0, lastDashIndex);
            String version = pluginIdWithVersion.substring(lastDashIndex + 1);
            
            // 如果插件正在处理中，跳过
            if (!processingPlugins.add(pluginId)) {
                log.info("插件正在处理中，跳过: {}", pluginId);
                return;
            }
            
            try {
                Optional<PluginInfo> existingPlugin = pluginRegistry.getPlugin(pluginId);
                
                // 记录部署操作
                DeploymentRecord record = new DeploymentRecord(
                        pluginId, version, 
                        existingPlugin.map(p -> p.getDescriptor().getVersion()).orElse(null), 
                        DeploymentType.HOT_DEPLOYMENT);
                
                // 如果插件已存在，执行热更新
                if (existingPlugin.isPresent()) {
                    log.info("检测到插件更新: {} -> {}", pluginId, version);
                    boolean success = hotUpdatePlugin(existingPlugin.get(), filePath, version);
                    record.setSuccess(success);
                    if (success) {
                        log.info("插件热更新成功: {}", pluginId);
                    } else {
                        log.error("插件热更新失败: {}", pluginId);
                    }
                } else {
                    // 如果插件不存在，执行热加载
                    log.info("检测到新插件: {} {}", pluginId, version);
                    boolean success = hotDeployPlugin(filePath, pluginId, version);
                    record.setSuccess(success);
                    if (success) {
                        log.info("插件热部署成功: {}", pluginId);
                    } else {
                        log.error("插件热部署失败: {}", pluginId);
                    }
                }
                
                // 记录操作历史
                record.setEndTime(System.currentTimeMillis());
                deploymentHistory.add(record);
                
            } finally {
                // 无论成功失败，移除处理标记
                processingPlugins.remove(pluginId);
            }
            
        } catch (Exception e) {
            log.error("处理插件文件变更异常: {}", filePath, e);
        }
    }
    
    /**
     * 处理插件文件删除事件
     * 
     * @param filePath 文件路径
     */
    private void handleFileDeleted(Path filePath) {
        try {
            // 解析插件ID
            String fileName = filePath.getFileName().toString();
            String pluginIdWithVersion = fileName.substring(0, fileName.lastIndexOf(".jar"));
            
            int lastDashIndex = pluginIdWithVersion.lastIndexOf("-");
            if (lastDashIndex <= 0) {
                return;
            }
            
            String pluginId = pluginIdWithVersion.substring(0, lastDashIndex);
            
            // 移除文件修改时间缓存
            fileModificationTimes.remove(filePath);
            
            // 如果插件正在使用中，不执行任何操作
            if (pluginRegistry.hasPlugin(pluginId)) {
                log.info("插件文件被删除，但插件仍在使用中: {}", pluginId);
            }
            
        } catch (Exception e) {
            log.error("处理插件文件删除异常: {}", filePath, e);
        }
    }
    
    /**
     * 热更新插件
     * 
     * @param existingPlugin 现有插件
     * @param newPluginPath 新插件路径
     * @param newVersion 新版本号
     * @return 更新是否成功
     */
    private boolean hotUpdatePlugin(PluginInfo existingPlugin, Path newPluginPath, String newVersion) {
        String pluginId = existingPlugin.getDescriptor().getPluginId();
        
        try {
            // 记录原始状态，用于恢复
            boolean wasRunning = existingPlugin.getState() == PluginState.RUNNING;
            
            // 创建备份
            PluginBackup backup = backupPlugin(existingPlugin);
            log.info("已创建插件备份: {}", pluginId);
            
            // 停止现有插件
            if (wasRunning) {
                lifecycleManager.stopPlugin(pluginId);
            }
            
            // 卸载现有插件
            lifecycleManager.unloadPlugin(pluginId);
            
            // 使用工厂方法创建新的PluginInfo对象
            PluginInfo newPluginInfo = PluginInfoFactory.createNewVersionInfo(
                    existingPlugin, newPluginPath, newVersion);
            
            // 加载插件
            if (!lifecycleManager.loadPlugin(newPluginInfo)) {
                log.error("插件热更新失败，无法加载新版本: {}", pluginId);
                return rollbackPlugin(pluginId, backup);
            }
            
            // 初始化插件
            if (!lifecycleManager.initializePlugin(pluginId)) {
                log.error("插件热更新失败，无法初始化新版本: {}", pluginId);
                return rollbackPlugin(pluginId, backup);
            }
            
            // 如果之前是运行状态，启动插件
            if (wasRunning && !lifecycleManager.startPlugin(pluginId)) {
                log.error("插件热更新失败，无法启动新版本: {}", pluginId);
                return rollbackPlugin(pluginId, backup);
            }
            
            // 发布热更新事件
            publishHotDeploymentEvent(pluginId, HotDeploymentAction.UPDATED, existingPlugin.getDescriptor().getVersion(), newVersion);
            
            // 移除备份
            pluginBackups.remove(pluginId);
            
            return true;
        } catch (Exception e) {
            log.error("插件热更新异常: {}", pluginId, e);
            
            // 尝试回滚
            PluginBackup backup = pluginBackups.get(pluginId);
            if (backup != null) {
                log.info("尝试回滚插件: {}", pluginId);
                return backup.restore(lifecycleManager, pluginRegistry);
            }
            
            return false;
        }
    }
    
    /**
     * 回滚插件到上一版本
     * 
     * @param pluginId 插件ID
     * @param backup 备份
     * @return 回滚是否成功
     */
    private boolean rollbackPlugin(String pluginId, PluginBackup backup) {
        log.info("触发插件回滚机制: {}", pluginId);
        
        try {
            if (backup.restore(lifecycleManager, pluginRegistry)) {
                log.info("插件回滚成功: {}", pluginId);
                return false; // 返回false表示更新失败
            } else {
                log.error("插件回滚失败: {}", pluginId);
                return false;
            }
        } catch (Exception e) {
            log.error("插件回滚过程中发生异常: {}", pluginId, e);
            return false;
        } finally {
            // 清理备份
            pluginBackups.remove(pluginId);
        }
    }
    
    /**
     * 热部署新插件
     * 
     * @param pluginPath 插件路径
     * @param pluginId 插件ID
     * @param version 版本号
     * @return 部署是否成功
     */
    private boolean hotDeployPlugin(Path pluginPath, String pluginId, String version) {
        try {
            // 使用工厂方法创建PluginInfo对象
            PluginDescriptor descriptor = PluginDescriptor.builder()
                    .pluginId(pluginId)
                    .version(version)
                    .name(pluginId)
                    .build();
            
            // 创建插件信息
            PluginInfo pluginInfo = PluginInfo.builder()
                    .descriptor(descriptor)
                    .pluginFile(pluginPath.toFile())
                    .jarPath(pluginPath)
                    .state(PluginState.CREATED)
                    .build();
            
            // 注册插件
            if (!pluginRegistry.registerPlugin(pluginInfo)) {
                log.error("插件热部署失败，无法注册: {}", pluginId);
                return false;
            }
            
            // 加载插件
            if (!lifecycleManager.loadPlugin(pluginInfo)) {
                log.error("插件热部署失败，无法加载: {}", pluginId);
                pluginRegistry.unregisterPlugin(pluginId);
                return false;
            }
            
            // 初始化插件
            if (!lifecycleManager.initializePlugin(pluginId)) {
                log.error("插件热部署失败，无法初始化: {}", pluginId);
                lifecycleManager.unloadPlugin(pluginId);
                pluginRegistry.unregisterPlugin(pluginId);
                return false;
            }
            
            // 启动插件
            if (!lifecycleManager.startPlugin(pluginId)) {
                log.error("插件热部署失败，无法启动: {}", pluginId);
                lifecycleManager.unloadPlugin(pluginId);
                pluginRegistry.unregisterPlugin(pluginId);
                return false;
            }
            
            // 发布热部署事件
            publishHotDeploymentEvent(pluginId, HotDeploymentAction.DEPLOYED, null, version);
            
            return true;
        } catch (Exception e) {
            log.error("插件热部署异常: {}", pluginId, e);
            return false;
        }
    }
    
    /**
     * 发布热部署事件
     * 
     * @param pluginId 插件ID
     * @param action 部署动作
     * @param oldVersion 旧版本
     * @param newVersion 新版本
     */
    private void publishHotDeploymentEvent(String pluginId, HotDeploymentAction action, 
                                          String oldVersion, String newVersion) {
        HotDeploymentEvent event = new HotDeploymentEvent(
                pluginId, action, oldVersion, newVersion);
        eventBus.postEvent(event);
    }
    
    /**
     * 获取热部署历史记录
     * 
     * @return 部署历史记录
     */
    public List<DeploymentRecord> getDeploymentHistory() {
        return new ArrayList<>(deploymentHistory);
    }
    
    /**
     * 获取插件的部署历史记录
     * 
     * @param pluginId 插件ID
     * @return 部署历史记录
     */
    public List<DeploymentRecord> getPluginDeploymentHistory(String pluginId) {
        return deploymentHistory.stream()
                .filter(record -> record.getPluginId().equals(pluginId))
                .collect(Collectors.toList());
    }
    
    /**
     * 手动触发插件热更新
     * 
     * @param pluginId 插件ID
     * @param pluginFile 插件文件
     * @return 更新是否成功
     */
    public boolean triggerHotUpdate(String pluginId, File pluginFile) {
        // 检查插件是否存在
        Optional<PluginInfo> existingPlugin = pluginRegistry.getPlugin(pluginId);
        if (existingPlugin.isEmpty()) {
            log.error("无法触发热更新，插件不存在: {}", pluginId);
            return false;
        }
        
        // 如果插件正在处理中，跳过
        if (!processingPlugins.add(pluginId)) {
            log.info("插件正在处理中，跳过: {}", pluginId);
            return false;
        }
        
        try {
            Path pluginPath = pluginFile.toPath();
            String fileName = pluginPath.getFileName().toString();
            String version = fileName.substring(fileName.lastIndexOf("-") + 1, fileName.lastIndexOf(".jar"));
            
            // 记录部署操作
            DeploymentRecord record = new DeploymentRecord(
                    pluginId, version, 
                    existingPlugin.get().getDescriptor().getVersion(), 
                    DeploymentType.MANUAL_UPDATE);
            
            // 执行热更新
            boolean success = hotUpdatePlugin(existingPlugin.get(), pluginPath, version);
            record.setSuccess(success);
            record.setEndTime(System.currentTimeMillis());
            deploymentHistory.add(record);
            
            return success;
        } catch (Exception e) {
            log.error("手动触发热更新异常: {}", pluginId, e);
            return false;
        } finally {
            // 无论成功失败，移除处理标记
            processingPlugins.remove(pluginId);
        }
    }
    
    /**
     * 热部署事件
     */
    public static class HotDeploymentEvent implements com.xiaoqu.qteamos.core.plugin.event.Event {
        private final String pluginId;
        private final HotDeploymentAction action;
        private final String oldVersion;
        private final String newVersion;
        private final long timestamp;
        
        public HotDeploymentEvent(String pluginId, HotDeploymentAction action, 
                                 String oldVersion, String newVersion) {
            this.pluginId = pluginId;
            this.action = action;
            this.oldVersion = oldVersion;
            this.newVersion = newVersion;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getPluginId() {
            return pluginId;
        }
        
        public HotDeploymentAction getAction() {
            return action;
        }
        
        public String getOldVersion() {
            return oldVersion;
        }
        
        public String getNewVersion() {
            return newVersion;
        }
        
        @Override
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public String getTopic() {
            return "plugin";
        }
        
        @Override
        public String getType() {
            return "hot_deployment";
        }
        
        @Override
        public String getSource() {
            return pluginId;
        }
    }
    
    /**
     * 热部署动作枚举
     */
    public enum HotDeploymentAction {
        DEPLOYED, // 新部署
        UPDATED,  // 更新
        REMOVED   // 移除
    }
    
    /**
     * 部署类型枚举
     */
    public enum DeploymentType {
        HOT_DEPLOYMENT,   // 自动热部署
        MANUAL_UPDATE,    // 手动触发更新
        CONFIRM_RELEASE,  // 确认灰度发布为正式版本
        REJECT_RELEASE,   // 拒绝灰度发布并回滚
        ROLLBACK          // 手动回滚操作
    }
    
    /**
     * 部署记录
     */
    public static class DeploymentRecord {
        private final String pluginId;
        private final String version;
        private final String previousVersion;
        private final DeploymentType type;
        private final long startTime;
        private long endTime;
        private boolean success;
        private String errorMessage;                       // 错误信息
        private String operatorId;                         // 操作者ID
        
        public DeploymentRecord(String pluginId, String version, 
                               String previousVersion, DeploymentType type) {
            this.pluginId = pluginId;
            this.version = version;
            this.previousVersion = previousVersion;
            this.type = type;
            this.startTime = System.currentTimeMillis();
        }
        
        public String getPluginId() {
            return pluginId;
        }
        
        public String getVersion() {
            return version;
        }
        
        public String getPreviousVersion() {
            return previousVersion;
        }
        
        public DeploymentType getType() {
            return type;
        }
        
        public long getStartTime() {
            return startTime;
        }
        
        public long getEndTime() {
            return endTime;
        }
        
        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public long getDuration() {
            return endTime - startTime;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
        
        public String getOperatorId() {
            return operatorId;
        }
        
        public void setOperatorId(String operatorId) {
            this.operatorId = operatorId;
        }
    }
    
    /**
     * 插件备份
     * 存储插件状态，支持在更新失败时回滚
     */
    public class PluginBackup {
        private final String pluginId;
        private final String version;
        private final PluginState state;
        private final Path jarPath;
        private final File pluginFile;
        private final Map<String, Object> pluginData;
        
        public PluginBackup(PluginInfo plugin) {
            this.pluginId = plugin.getDescriptor().getPluginId();
            this.version = plugin.getDescriptor().getVersion();
            this.state = plugin.getState();
            this.jarPath = plugin.getJarPath();
            this.pluginFile = plugin.getPluginFile();
            this.pluginData = new HashMap<>();
            
            // 备份插件额外数据
            try {
                // 保存描述符和关键数据
                pluginData.put("descriptor", plugin.getDescriptor());
            } catch (Exception e) {
                log.warn("备份插件数据时出现异常: {}", pluginId, e);
            }
            
            log.info("已创建插件备份: {} {}", pluginId, version);
        }
        
        /**
         * 恢复插件到备份状态
         * 
         * @param lifecycleManager 生命周期管理器
         * @param registry 插件注册表
         * @return 恢复是否成功
         */
        public boolean restore(PluginLifecycleManager lifecycleManager, PluginRegistry registry) {
            log.info("开始从备份恢复插件: {} {}", pluginId, version);
            
            try {
                // 检查插件是否存在
                Optional<PluginInfo> existingPlugin = registry.getPlugin(pluginId);
                if (existingPlugin.isPresent()) {
                    // 停止并卸载当前实例
                    lifecycleManager.stopPlugin(pluginId);
                    lifecycleManager.unloadPlugin(pluginId);
                    
                    // 恢复到备份状态
                    PluginInfo plugin = existingPlugin.get();
                    plugin.setPluginFile(this.pluginFile);
                    plugin.setJarPath(this.jarPath);
                    plugin.setState(PluginState.CREATED);
                    
                    // 恢复描述符
                    if (pluginData.containsKey("descriptor")) {
                        plugin.setDescriptor((PluginDescriptor) pluginData.get("descriptor"));
                    }
                    
                    // 重新加载和初始化
                    if (lifecycleManager.loadPlugin(plugin) && 
                            lifecycleManager.initializePlugin(pluginId)) {
                        
                        // 如果之前是运行状态，则启动
                        if (this.state == PluginState.RUNNING) {
                            lifecycleManager.startPlugin(pluginId);
                        }
                        
                        log.info("插件恢复成功: {}", pluginId);
                        return true;
                    }
                } else {
                    log.error("无法恢复插件，插件不在注册表中: {}", pluginId);
                }
                
                return false;
            } catch (Exception e) {
                log.error("恢复插件时发生异常: {}", pluginId, e);
                return false;
            }
        }
    }
    
    // 备份插件状态
    private PluginBackup backupPlugin(PluginInfo plugin) {
        PluginBackup backup = new PluginBackup(plugin);
        pluginBackups.put(plugin.getDescriptor().getPluginId(), backup);
        return backup;
    }
    
    /**
     * 资源追踪器
     * 负责管理插件使用的资源，确保在插件卸载时释放
     */
    public class ResourceTracker {
        private final Map<String, Set<AutoCloseable>> managedResources = new ConcurrentHashMap<>();
        
        // 注册资源
        public void trackResource(String pluginId, AutoCloseable resource) {
            managedResources.computeIfAbsent(pluginId, k -> ConcurrentHashMap.newKeySet())
                .add(resource);
        }
        
        // 释放资源
        public void releaseResources(String pluginId) {
            Set<AutoCloseable> resources = managedResources.remove(pluginId);
            if (resources != null) {
                for (AutoCloseable resource : resources) {
                    try {
                        resource.close();
                    } catch (Exception e) {
                        log.error("释放插件资源失败", e);
                    }
                }
            }
        }
    }
}