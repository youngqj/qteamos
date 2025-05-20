package com.xiaoqu.qteamos.core.plugin.manager;

import com.xiaoqu.qteamos.core.plugin.manager.exception.PluginLifecycleException;
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginVersion;
import com.xiaoqu.qteamos.core.plugin.running.PluginDescriptor;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import com.xiaoqu.qteamos.core.plugin.running.PluginState;
import com.xiaoqu.qteamos.core.plugin.service.PluginPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.lang.reflect.Method;

// 导入EventBus
import com.xiaoqu.qteamos.core.plugin.event.EventBus;

/**
 * 插件灰度发布和回滚管理器
 * 负责插件的灰度发布进度跟踪、批次控制、健康检查和版本回滚
 *
 * @author yangqijun
 * @date 2024-07-19
 */
@Component
public class PluginRolloutManager {
    private static final Logger log = LoggerFactory.getLogger(PluginRolloutManager.class);

    @Autowired
    private PluginRegistry pluginRegistry;

    @Autowired
    private PluginLifecycleManager lifecycleManager;

    @Autowired
    private PluginPersistenceService persistenceService;
    
    @Autowired
    @Qualifier("enhancedPluginVersionManager")
    private EnhancedPluginVersionManager versionManager;
    
 
    
    // 配置相关属性
    @Value("${plugin.rollout.auto-proceed:false}")
    private boolean autoProceedEnabled;
    
    @Value("${plugin.rollout.healthcheck.enabled:true}")
    private boolean healthCheckEnabled;
    
    @Value("${plugin.cluster.enabled:false}")
    private boolean clusterMode;
    
    @Value("${plugin.rollout.default.batch-size:20}")
    private int defaultBatchSize;
    
    @Value("${plugin.rollout.default.validate-time:30}")
    private int defaultValidateTime;

    // 记录灰度发布状态
    private final Map<String, RolloutStatus> rolloutStatuses = new ConcurrentHashMap<>();
    
    // 记录集群节点的部署状态(nodeId -> {pluginId, version})
    private final Map<String, Map<String, String>> clusterNodeDeployments = new ConcurrentHashMap<>();

    // 通过依赖注入获取EventBus
    @Autowired
    private EventBus eventBus;

    /**
     * 启动灰度发布过程
     *
     * @param pluginId 插件ID
     * @param targetVersion 目标版本
     * @param batchSize 每批百分比
     * @param validateTime 每批验证时间（分钟）
     * @return 灰度发布状态
     */
    public RolloutStatus startGradualRollout(String pluginId, String targetVersion, int batchSize, int validateTime) {
        log.info("开始灰度发布: 插件[{}]，版本[{}]，批次大小[{}%]，验证时间[{}分钟]", 
                pluginId, targetVersion, batchSize, validateTime);
        
        // 验证参数
        if (batchSize <= 0 || batchSize > 100) {
            batchSize = defaultBatchSize;
            log.warn("批次大小无效，使用默认值: {}", defaultBatchSize);
        }
        
        if (validateTime <= 0) {
            validateTime = defaultValidateTime;
            log.warn("验证时间无效，使用默认值: {}", defaultValidateTime);
        }
        
        // 验证插件和版本存在
        Optional<PluginInfo> currentPlugin = pluginRegistry.getPlugin(pluginId);
        if (currentPlugin.isEmpty()) {
            log.error("灰度发布失败: 插件[{}]不存在", pluginId);
            return RolloutStatus.failed(pluginId, "插件不存在");
        }
        
        // 检查目标版本是否存在并可用
        if (!isVersionAvailable(pluginId, targetVersion)) {
            log.error("灰度发布失败: 插件[{}]的目标版本[{}]不存在或不可用", pluginId, targetVersion);
            return RolloutStatus.failed(pluginId, "目标版本不存在或不可用");
        }
        
        // 检查是否已有进行中的灰度发布
        RolloutStatus existingStatus = rolloutStatuses.get(pluginId);
        if (existingStatus != null && 
            (existingStatus.getState() == RolloutState.IN_PROGRESS || 
             existingStatus.getState() == RolloutState.PAUSED)) {
            log.warn("插件[{}]已有进行中的灰度发布，先取消之前的发布", pluginId);
            cancelRollout(pluginId, "被新的灰度发布任务替代");
        }
        
        // 创建灰度发布状态
        RolloutStatus status = new RolloutStatus(
                pluginId, 
                currentPlugin.get().getVersion(), 
                targetVersion, 
                batchSize, 
                validateTime
        );
        
        // 记录状态并持久化
        rolloutStatuses.put(pluginId, status);
        persistRolloutStatus(status);
        
        // 发布灰度开始事件
        publishRolloutEvent(status, RolloutEventType.STARTED);
        
        // 执行第一批次发布
        return proceedToNextBatch(pluginId);
    }
    
    /**
     * 进行下一批次的灰度发布
     *
     * @param pluginId 插件ID
     * @return 更新后的灰度发布状态
     */
    public RolloutStatus proceedToNextBatch(String pluginId) {
        RolloutStatus status = rolloutStatuses.get(pluginId);
        if (status == null) {
            log.error("无法找到插件[{}]的灰度发布状态", pluginId);
            return RolloutStatus.failed(pluginId, "无灰度发布记录");
        }
        
        // 如果已经完成或失败，不再继续
        if (status.getState() == RolloutState.COMPLETED || status.getState() == RolloutState.FAILED) {
            return status;
        }
        
        try {
            // 计算本次批次的目标百分比
            int nextBatch = status.getCurrentBatch() + 1;
            int targetPercentage = Math.min(nextBatch * status.getBatchSize(), 100);
            
            log.info("执行灰度发布批次[{}]: 插件[{}]，目标百分比[{}%]", 
                    nextBatch, pluginId, targetPercentage);
            
            // 发布批次开始事件
            publishRolloutEvent(status, RolloutEventType.BATCH_STARTED);
            
            // 在集群环境中执行节点级别的灰度
            if (clusterMode) {
                boolean batchSuccess = executeClusterBatchUpdate(pluginId, status.getTargetVersion(), targetPercentage);
                if (!batchSuccess) {
                    status.setState(RolloutState.FAILED);
                    status.setMessage("集群批次更新失败");
                    persistRolloutStatus(status);
                    publishRolloutEvent(status, RolloutEventType.FAILED);
                    return status;
                }
            } 
            // 在单机环境中直接执行更新
            else {
                // 如果是第一批次或最后批次，需要实际执行更新
                if (nextBatch == 1 || targetPercentage == 100) {
                    boolean updateSuccess = executePluginUpdate(pluginId, status.getTargetVersion());
                    if (!updateSuccess) {
                        status.setState(RolloutState.FAILED);
                        status.setMessage("更新失败");
                        persistRolloutStatus(status);
                        publishRolloutEvent(status, RolloutEventType.FAILED);
                        return status;
                    }
                }
            }
            
            // 更新状态
            status.setCurrentBatch(nextBatch);
            status.setCurrentPercentage(targetPercentage);
            status.setLastBatchTime(LocalDateTime.now());
            
            // 检查是否完成
            if (targetPercentage >= 100) {
                status.setState(RolloutState.COMPLETED);
                status.setMessage("灰度发布完成");
                status.setCompletionTime(LocalDateTime.now());
                publishRolloutEvent(status, RolloutEventType.COMPLETED);
            } else {
                status.setState(RolloutState.IN_PROGRESS);
                status.setMessage("批次" + nextBatch + "发布完成，等待验证");
                publishRolloutEvent(status, RolloutEventType.BATCH_COMPLETED);
            }
            
            // 持久化状态
            persistRolloutStatus(status);
            
            return status;
        } catch (Exception e) {
            log.error("灰度发布批次执行异常: 插件[{}]", pluginId, e);
            status.setState(RolloutState.FAILED);
            status.setMessage("发布异常: " + e.getMessage());
            persistRolloutStatus(status);
            publishRolloutEvent(status, RolloutEventType.FAILED);
            return status;
        }
    }
    
    /**
     * 在集群环境中执行批次更新
     * 根据百分比选择集群中的节点进行更新
     */
    private boolean executeClusterBatchUpdate(String pluginId, String targetVersion, int targetPercentage) {
        try {
            // 获取集群节点信息
            List<String> allNodes = getClusterNodes();
            if (allNodes.isEmpty()) {
                log.warn("集群节点列表为空，无法执行批次更新");
                return false;
            }
            
            // 计算本次需要更新的节点数
            int totalNodes = allNodes.size();
            int targetNodes = (int) Math.ceil(totalNodes * (targetPercentage / 100.0));
            
            // 获取已经更新的节点
            List<String> updatedNodes = getNodesWithVersion(pluginId, targetVersion);
            
            // 如果已经达到目标节点数，无需再更新
            if (updatedNodes.size() >= targetNodes) {
                log.info("已有{}个节点更新到版本[{}]，无需更新更多节点", updatedNodes.size(), targetVersion);
                return true;
            }
            
            // 计算本次需要更新的节点
            int nodesToUpdate = targetNodes - updatedNodes.size();
            
            // 获取未更新的节点
            List<String> remainingNodes = new ArrayList<>(allNodes);
            remainingNodes.removeAll(updatedNodes);
            
            // 随机选择节点进行更新
            Collections.shuffle(remainingNodes);
            List<String> nodesToUpdateList = remainingNodes.subList(0, Math.min(nodesToUpdate, remainingNodes.size()));
            
            // 执行节点更新
            boolean allSuccess = true;
            for (String nodeId : nodesToUpdateList) {
                boolean nodeUpdateSuccess = executeNodeUpdate(nodeId, pluginId, targetVersion);
                if (!nodeUpdateSuccess) {
                    log.error("节点[{}]更新插件[{}]到版本[{}]失败", nodeId, pluginId, targetVersion);
                    allSuccess = false;
                }
            }
            
            return allSuccess;
        } catch (Exception e) {
            log.error("执行集群批次更新异常", e);
            return false;
        }
    }
    
    /**
     * 获取集群节点列表
     */
    private List<String> getClusterNodes() {
        // 实际系统中应从注册中心或配置中心获取节点列表
        // 此处简化为从clusterNodeDeployments中获取
        return new ArrayList<>(clusterNodeDeployments.keySet());
    }
    
    /**
     * 获取已部署特定版本的节点列表
     */
    private List<String> getNodesWithVersion(String pluginId, String version) {
        return clusterNodeDeployments.entrySet().stream()
                .filter(entry -> {
                    Map<String, String> nodePlugins = entry.getValue();
                    return nodePlugins.containsKey(pluginId) && version.equals(nodePlugins.get(pluginId));
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    /**
     * 执行节点更新
     */
    private boolean executeNodeUpdate(String nodeId, String pluginId, String targetVersion) {
        try {
            // 实际系统中应该通过消息队列或RPC调用节点的更新API
            log.info("正在更新节点[{}]的插件[{}]到版本[{}]", nodeId, pluginId, targetVersion);
            
            // 模拟更新成功
            Map<String, String> nodePlugins = clusterNodeDeployments.computeIfAbsent(nodeId, k -> new HashMap<>());
            nodePlugins.put(pluginId, targetVersion);
            
            return true;
        } catch (Exception e) {
            log.error("更新节点异常", e);
            return false;
        }
    }
    
    /**
     * 执行插件实际更新
     *
     * @param pluginId 插件ID
     * @param targetVersion 目标版本
     * @return 更新是否成功
     */
    private boolean executePluginUpdate(String pluginId, String targetVersion) {
        try {
            // 1. 查找当前插件
            Optional<PluginInfo> currentPlugin = pluginRegistry.getPlugin(pluginId);
            if (currentPlugin.isEmpty()) {
                log.error("更新失败: 插件[{}]不存在", pluginId);
                return false;
            }
            
            // 2. 查找目标版本
            PluginInfo targetPluginInfo = versionManager.getPluginVersion(pluginId, targetVersion);
            if (targetPluginInfo == null) {
                log.error("更新失败: 插件[{}]的目标版本[{}]不存在", pluginId, targetVersion);
                return false;
            }
            
            // 3. 执行更新
            boolean success = lifecycleManager.updatePlugin(currentPlugin.get(), targetPluginInfo);
            
            if (success) {
                log.info("插件[{}]更新成功: {} -> {}", pluginId, currentPlugin.get().getVersion(), targetVersion);
            } else {
                log.error("插件[{}]更新失败", pluginId);
            }
            
            return success;
        } catch (Exception e) {
            log.error("执行插件更新异常: " + pluginId, e);
            return false;
        }
    }
    
    /**
     * 暂停灰度发布
     *
     * @param pluginId 插件ID
     * @param reason 暂停原因
     * @return 灰度发布状态
     */
    public RolloutStatus pauseRollout(String pluginId, String reason) {
        RolloutStatus status = rolloutStatuses.get(pluginId);
        if (status == null || status.getState() != RolloutState.IN_PROGRESS) {
            log.warn("无法暂停灰度发布: 插件[{}]没有进行中的灰度发布", pluginId);
            return status;
        }
        
        status.setState(RolloutState.PAUSED);
        status.setMessage("已暂停: " + reason);
        persistRolloutStatus(status);
        publishRolloutEvent(status, RolloutEventType.PAUSED);
        
        log.info("已暂停插件[{}]的灰度发布: {}", pluginId, reason);
        return status;
    }
    
    /**
     * 恢复灰度发布
     *
     * @param pluginId 插件ID
     * @return 灰度发布状态
     */
    public RolloutStatus resumeRollout(String pluginId) {
        RolloutStatus status = rolloutStatuses.get(pluginId);
        if (status == null || status.getState() != RolloutState.PAUSED) {
            log.warn("无法恢复灰度发布: 插件[{}]没有暂停的灰度发布", pluginId);
            return status;
        }
        
        status.setState(RolloutState.IN_PROGRESS);
        status.setMessage("已恢复灰度发布");
        persistRolloutStatus(status);
        publishRolloutEvent(status, RolloutEventType.RESUMED);
        
        log.info("已恢复插件[{}]的灰度发布", pluginId);
        return status;
    }
    
    /**
     * 取消灰度发布
     *
     * @param pluginId 插件ID
     * @param reason 取消原因
     * @return 灰度发布状态
     */
    public RolloutStatus cancelRollout(String pluginId, String reason) {
        RolloutStatus status = rolloutStatuses.get(pluginId);
        if (status == null) {
            log.warn("无法取消灰度发布: 插件[{}]没有灰度发布记录", pluginId);
            return null;
        }
        
        if (status.getState() == RolloutState.COMPLETED || status.getState() == RolloutState.FAILED) {
            log.warn("灰度发布已经完成或失败，无需取消: 插件[{}]", pluginId);
            return status;
        }
        
        status.setState(RolloutState.FAILED);
        status.setMessage("已取消: " + reason);
        status.setCompletionTime(LocalDateTime.now());
        persistRolloutStatus(status);
        publishRolloutEvent(status, RolloutEventType.CANCELLED);
        
        log.info("已取消插件[{}]的灰度发布: {}", pluginId, reason);
        return status;
    }
    
    /**
     * 回滚插件到指定版本
     *
     * @param pluginId 插件ID
     * @param targetVersion 目标版本
     * @return 回滚是否成功
     */
    public boolean rollbackToVersion(String pluginId, String targetVersion) {
        log.info("开始回滚插件: 插件[{}]，目标版本[{}]", pluginId, targetVersion);
        
        try {
            // 验证插件和版本
            Optional<PluginInfo> currentPlugin = pluginRegistry.getPlugin(pluginId);
            if (currentPlugin.isEmpty()) {
                log.error("回滚失败: 插件[{}]不存在", pluginId);
                return false;
            }
            
            // 验证目标版本是否存在
            if (!isVersionAvailable(pluginId, targetVersion)) {
                log.error("回滚失败: 目标版本[{}]不存在或不可用", targetVersion);
                return false;
            }
            
            // 获取目标版本信息
            PluginInfo rollbackInfo = versionManager.getPluginVersion(pluginId, targetVersion);
            if (rollbackInfo == null) {
                log.error("回滚失败: 无法获取目标版本[{}]的详细信息", targetVersion);
                return false;
            }
            
            // 添加回滚标记
            rollbackInfo.getDescriptor().getUpdateInfo().put("isRollback", "true");
            rollbackInfo.getDescriptor().getUpdateInfo().put("rollbackFrom", currentPlugin.get().getVersion());
            rollbackInfo.getDescriptor().getUpdateInfo().put("rollbackTime", LocalDateTime.now().toString());
            
            // 如果有活跃的灰度发布，标记为失败
            RolloutStatus status = rolloutStatuses.get(pluginId);
            if (status != null && (status.getState() == RolloutState.IN_PROGRESS || status.getState() == RolloutState.PAUSED)) {
                cancelRollout(pluginId, "执行回滚到版本" + targetVersion);
            }
            
            // 执行更新（回滚）
            boolean success = lifecycleManager.updatePlugin(currentPlugin.get(), rollbackInfo);
            
            if (success) {
                log.info("插件回滚成功: 插件[{}]，从版本[{}]回滚到[{}]", 
                        pluginId, currentPlugin.get().getVersion(), targetVersion);
                
                // 记录回滚历史
                persistenceService.recordPluginRollback(
                    pluginId, 
                    currentPlugin.get().getVersion(), 
                    targetVersion, 
                    "手动回滚"
                );
                
                // 如果是集群模式，需要同步回滚到所有节点
                if (clusterMode) {
                    executeClusterRollback(pluginId, targetVersion);
                }
            } else {
                log.error("插件回滚失败: 插件[{}]，目标版本[{}]", pluginId, targetVersion);
            }
            
            return success;
        } catch (Exception e) {
            log.error("插件回滚异常: " + pluginId, e);
            return false;
        }
    }
    
    /**
     * 在集群环境中执行插件回滚
     */
    private void executeClusterRollback(String pluginId, String targetVersion) {
        try {
            List<String> allNodes = getClusterNodes();
            for (String nodeId : allNodes) {
                executeNodeUpdate(nodeId, pluginId, targetVersion);
            }
        } catch (Exception e) {
            log.error("执行集群回滚异常", e);
        }
    }
    
    /**
     * 检查版本是否可用
     */
    private boolean isVersionAvailable(String pluginId, String version) {
        try {
            // 从版本管理器中检查版本是否存在
            return versionManager.isVersionAvailable(pluginId, version);
        } catch (Exception e) {
            log.error("检查版本可用性异常", e);
            return false;
        }
    }
    
    /**
     * 持久化灰度发布状态
     */
    private void persistRolloutStatus(RolloutStatus status) {
        try {
            // 实际系统中应该将状态持久化到数据库
            persistenceService.saveRolloutStatus(status);
        } catch (Exception e) {
            log.error("持久化灰度发布状态异常", e);
        }
    }
    
    /**
     * 发布灰度发布事件
     */
    private void publishRolloutEvent(RolloutStatus status, RolloutEventType eventType) {
        try {
            // 创建对应类型的事件
            com.xiaoqu.qteamos.core.plugin.event.plugins.PluginRolloutEvent event = null;
            
            switch (eventType) {
                case STARTED:
                    event = com.xiaoqu.qteamos.core.plugin.event.plugins.PluginRolloutEvent.createStartedEvent(
                        status.getPluginId(),
                        status.getCurrentVersion(),
                        status.getTargetVersion()
                    );
                    break;
                    
                case BATCH_STARTED:
                    event = com.xiaoqu.qteamos.core.plugin.event.plugins.PluginRolloutEvent.createBatchStartedEvent(
                        status.getPluginId(),
                        status.getCurrentVersion(),
                        status.getTargetVersion(),
                        status.getCurrentPercentage()
                    );
                    break;
                    
                case BATCH_COMPLETED:
                    event = com.xiaoqu.qteamos.core.plugin.event.plugins.PluginRolloutEvent.createBatchCompletedEvent(
                        status.getPluginId(),
                        status.getCurrentVersion(),
                        status.getTargetVersion(),
                        status.getCurrentPercentage(),
                        status.getCurrentBatch()
                    );
                    break;
                    
                case PAUSED:
                    event = com.xiaoqu.qteamos.core.plugin.event.plugins.PluginRolloutEvent.createPausedEvent(
                        status.getPluginId(),
                        status.getCurrentVersion(),
                        status.getTargetVersion(),
                        status.getCurrentPercentage(),
                        status.getMessage()
                    );
                    break;
                    
                case RESUMED:
                    event = com.xiaoqu.qteamos.core.plugin.event.plugins.PluginRolloutEvent.createResumedEvent(
                        status.getPluginId(),
                        status.getCurrentVersion(),
                        status.getTargetVersion(),
                        status.getCurrentPercentage()
                    );
                    break;
                    
                case COMPLETED:
                    event = com.xiaoqu.qteamos.core.plugin.event.plugins.PluginRolloutEvent.createCompletedEvent(
                        status.getPluginId(),
                        status.getCurrentVersion(),
                        status.getTargetVersion()
                    );
                    break;
                    
                case FAILED:
                    event = com.xiaoqu.qteamos.core.plugin.event.plugins.PluginRolloutEvent.createFailedEvent(
                        status.getPluginId(),
                        status.getCurrentVersion(),
                        status.getTargetVersion(),
                        status.getCurrentPercentage(),
                        status.getMessage()
                    );
                    break;
                    
                case CANCELLED:
                    event = com.xiaoqu.qteamos.core.plugin.event.plugins.PluginRolloutEvent.createCancelledEvent(
                        status.getPluginId(),
                        status.getCurrentVersion(),
                        status.getTargetVersion(),
                        status.getCurrentPercentage(),
                        status.getMessage()
                    );
                    break;
            }
            
            if (event != null) {
                // 直接使用注入的eventBus发布事件，而不是通过applicationContext获取
                eventBus.postEvent(event);
                
                log.debug("发布灰度发布事件: {}", event);
            }
        } catch (Exception e) {
            log.warn("发布灰度发布事件异常", e);
        }
    }
    
    /**
     * 定时检查等待验证的灰度发布
     * 如果验证时间已到，自动进入下一批次
     */
    @Scheduled(fixedDelayString = "${plugin.rollout.check-interval:60000}")
    public void checkPendingRollouts() {
        if (!autoProceedEnabled) {
            return;
        }
        
        try {
            LocalDateTime now = LocalDateTime.now();
            
            for (RolloutStatus status : rolloutStatuses.values()) {
                // 只处理进行中的灰度发布
                if (status.getState() != RolloutState.IN_PROGRESS) {
                    continue;
                }
                
                // 检查是否超过验证时间
                LocalDateTime lastBatchTime = status.getLastBatchTime();
                if (lastBatchTime == null) {
                    continue;
                }
                
                long minutesSinceLastBatch = Duration.between(lastBatchTime, now).toMinutes();
                if (minutesSinceLastBatch >= status.getValidateTimeMinutes()) {
                    // 检查健康状态
                    if (healthCheckEnabled && !isPluginHealthy(status.getPluginId())) {
                        // 如果检测到不健康，自动暂停灰度发布
                        pauseRollout(status.getPluginId(), "健康检查失败");
                        continue;
                    }
                    
                    // 自动进入下一批次
                    log.info("灰度发布验证时间已到，自动进入下一批次: 插件[{}]", status.getPluginId());
                    proceedToNextBatch(status.getPluginId());
                }
            }
        } catch (Exception e) {
            log.error("检查待处理灰度发布异常", e);
        }
    }
    
    /**
     * 检查插件健康状态
     */
    private boolean isPluginHealthy(String pluginId) {
        try {
            Optional<PluginInfo> plugin = pluginRegistry.getPlugin(pluginId);
            if (plugin.isEmpty()) {
                return false;
            }
            
            // 检查插件状态是否为RUNNING
            if (plugin.get().getState() != PluginState.RUNNING) {
                return false;
            }
            
            // 如果插件提供了健康检查方法，调用它
            Object instance = plugin.get().getPluginInstance();
            if (instance != null) {
                try {
                    Method healthCheckMethod = instance.getClass().getDeclaredMethod("checkHealth");
                    if (healthCheckMethod != null) {
                        healthCheckMethod.setAccessible(true);
                        Object result = healthCheckMethod.invoke(instance);
                        if (result instanceof Boolean) {
                            return (Boolean) result;
                        }
                    }
                } catch (NoSuchMethodException e) {
                    // 插件没有提供健康检查方法，忽略
                }
            }
            
            // 默认认为健康
            return true;
        } catch (Exception e) {
            log.error("检查插件健康状态异常: " + pluginId, e);
            return false;
        }
    }
    
    /**
     * 获取插件的灰度发布状态
     *
     * @param pluginId 插件ID
     * @return 灰度发布状态
     */
    public Optional<RolloutStatus> getRolloutStatus(String pluginId) {
        return Optional.ofNullable(rolloutStatuses.get(pluginId));
    }
    
    /**
     * 获取所有灰度发布状态
     *
     * @return 所有灰度发布状态
     */
    public Collection<RolloutStatus> getAllRolloutStatuses() {
        return rolloutStatuses.values();
    }
    
    /**
     * 检查插件是否应该自动激活
     * 系统插件且受信任的插件将自动激活
     *
     * @param descriptor 插件描述符
     * @return 是否应自动激活
     */
    public boolean shouldAutoActivate(PluginDescriptor descriptor) {
        return "system".equals(descriptor.getType()) && 
               ("trusted".equals(descriptor.getTrust()) || "official".equals(descriptor.getTrust()));
    }
    
    /**
     * 灰度发布状态
     */
    public static class RolloutStatus {
        private String pluginId;
        private String currentVersion;
        private String targetVersion;
        private int batchSize;
        private int validateTimeMinutes;
        private int currentBatch;
        private int currentPercentage;
        private RolloutState state;
        private String message;
        private LocalDateTime startTime;
        private LocalDateTime lastBatchTime;
        private LocalDateTime completionTime;
        private Map<String, Object> metadata;
        
        public RolloutStatus(String pluginId, String currentVersion, String targetVersion, 
                            int batchSize, int validateTimeMinutes) {
            this.pluginId = pluginId;
            this.currentVersion = currentVersion;
            this.targetVersion = targetVersion;
            this.batchSize = batchSize;
            this.validateTimeMinutes = validateTimeMinutes;
            this.currentBatch = 0;
            this.currentPercentage = 0;
            this.state = RolloutState.INITIALIZED;
            this.message = "灰度发布初始化";
            this.startTime = LocalDateTime.now();
            this.metadata = new HashMap<>();
        }
        
        public static RolloutStatus failed(String pluginId, String message) {
            RolloutStatus status = new RolloutStatus(pluginId, "", "", 0, 0);
            status.setState(RolloutState.FAILED);
            status.setMessage(message);
            return status;
        }
        
        // getter/setter 方法
        public String getPluginId() {
            return pluginId;
        }
        
        public String getCurrentVersion() {
            return currentVersion;
        }
        
        public String getTargetVersion() {
            return targetVersion;
        }
        
        public int getBatchSize() {
            return batchSize;
        }
        
        public int getValidateTimeMinutes() {
            return validateTimeMinutes;
        }
        
        public int getCurrentBatch() {
            return currentBatch;
        }
        
        public void setCurrentBatch(int currentBatch) {
            this.currentBatch = currentBatch;
        }
        
        public int getCurrentPercentage() {
            return currentPercentage;
        }
        
        public void setCurrentPercentage(int currentPercentage) {
            this.currentPercentage = currentPercentage;
        }
        
        public RolloutState getState() {
            return state;
        }
        
        public void setState(RolloutState state) {
            this.state = state;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public LocalDateTime getStartTime() {
            return startTime;
        }
        
        public LocalDateTime getLastBatchTime() {
            return lastBatchTime;
        }
        
        public void setLastBatchTime(LocalDateTime lastBatchTime) {
            this.lastBatchTime = lastBatchTime;
        }
        
        public LocalDateTime getCompletionTime() {
            return completionTime;
        }
        
        public void setCompletionTime(LocalDateTime completionTime) {
            this.completionTime = completionTime;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
        
        @Override
        public String toString() {
            return String.format("插件[%s]灰度发布: %s -> %s, 进度: %d%%, 状态: %s, %s", 
                    pluginId, currentVersion, targetVersion, currentPercentage, state, message);
        }
    }
    
    /**
     * 灰度发布状态枚举
     */
    public enum RolloutState {
        INITIALIZED,    // 初始化
        IN_PROGRESS,    // 进行中
        PAUSED,         // 暂停
        COMPLETED,      // 完成
        FAILED          // 失败
    }
    
    /**
     * 灰度发布事件类型
     */
    public enum RolloutEventType {
        STARTED,          // 灰度发布开始
        BATCH_STARTED,    // 批次开始
        BATCH_COMPLETED,  // 批次完成
        PAUSED,           // 暂停
        RESUMED,          // 恢复
        COMPLETED,        // 完成
        FAILED,           // 失败
        CANCELLED         // 取消
    }
    
    /**
     * 灰度发布事件
     */
    public static class PluginRolloutEvent {
        private final Object source;
        private final String pluginId;
        private final String fromVersion;
        private final String toVersion;
        private final int percentage;
        private final RolloutEventType eventType;
        
        public PluginRolloutEvent(Object source, String pluginId, String fromVersion, 
                                 String toVersion, int percentage, RolloutEventType eventType) {
            this.source = source;
            this.pluginId = pluginId;
            this.fromVersion = fromVersion;
            this.toVersion = toVersion;
            this.percentage = percentage;
            this.eventType = eventType;
        }
        
        public Object getSource() { return source; }
        public String getPluginId() { return pluginId; }
        public String getFromVersion() { return fromVersion; }
        public String getToVersion() { return toVersion; }
        public int getPercentage() { return percentage; }
        public RolloutEventType getEventType() { return eventType; }
    }
} 