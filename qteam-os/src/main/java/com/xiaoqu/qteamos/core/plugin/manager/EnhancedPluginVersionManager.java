package com.xiaoqu.qteamos.core.plugin.manager;

import com.xiaoqu.qteamos.common.utils.VersionUtils;
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginVersion;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import com.xiaoqu.qteamos.core.plugin.service.PluginPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 增强版插件版本管理器
 * 负责插件版本的储存、检索和管理
 *
 * @author yangqijun
 * @date 2024-07-20
 */
@Component("enhancedPluginVersionManager")
public class EnhancedPluginVersionManager {
    private static final Logger log = LoggerFactory.getLogger(EnhancedPluginVersionManager.class);
    
    @Autowired
    private PluginPersistenceService persistenceService;
    
    @Autowired
    private PluginLifecycleManager lifecycleManager;
    
    @Value("${plugin.version.repository.path:./plugins/versions}")
    private String versionRepositoryPath;
    
    // 版本缓存，用于提高检索性能
    private final Map<String, Map<String, PluginInfo>> versionCache = new ConcurrentHashMap<>();
    
    /**
     * 初始化版本管理器
     */
    public void initialize() {
        try {
            // 确保版本仓库目录存在
            Path repositoryPath = Paths.get(versionRepositoryPath);
            if (!Files.exists(repositoryPath)) {
                Files.createDirectories(repositoryPath);
                log.info("创建插件版本仓库目录: {}", repositoryPath);
            }
            
            // 加载已有版本到缓存
            loadVersionsToCache();
            
            log.info("插件版本管理器初始化完成");
        } catch (Exception e) {
            log.error("初始化插件版本管理器失败", e);
        }
    }
    
    /**
     * 加载已有版本到缓存
     */
    private void loadVersionsToCache() {
        try {
            // 从持久化服务获取所有插件版本信息
            List<SysPluginVersion> allVersions = persistenceService.getPluginVersionHistory();
            
            for (SysPluginVersion versionInfo : allVersions) {
                String pluginId = versionInfo.getPluginId();
                String version = versionInfo.getVersion();
                
                // 检查对应版本文件是否存在
                File versionFile = getVersionFile(pluginId, version);
                if (!versionFile.exists()) {
                    log.warn("插件[{}]版本[{}]文件不存在: {}", pluginId, version, versionFile.getPath());
                    continue;
                }
                
                try {
                    // 加载版本但不初始化
                    PluginInfo pluginInfo = lifecycleManager.loadPlugin(versionFile.getPath(), false);
                    
                    // 添加到缓存
                    //TODO: 需要优化根据系统设定的缓存来存放 杨其军
                    versionCache
                        .computeIfAbsent(pluginId, k -> new ConcurrentHashMap<>())
                        .put(version, pluginInfo);
                    
                    log.debug("已加载插件[{}]版本[{}]到缓存", pluginId, version);
                } catch (Exception e) {
                    log.error("加载插件[{}]版本[{}]失败", pluginId, version, e);
                }
            }
            
            log.info("已加载{}个插件的版本信息到缓存", versionCache.size());
        } catch (Exception e) {
            log.error("加载版本到缓存失败", e);
        }
    }
    
    /**
     * 获取版本文件路径
     */
    private File getVersionFile(String pluginId, String version) {
        String fileName = pluginId + "-" + version + ".jar";
        return Paths.get(versionRepositoryPath, pluginId, fileName).toFile();
    }
    
    /**
     * 保存插件版本
     *
     * @param pluginInfo 插件信息
     * @param pluginFilePath 插件文件路径
     * @return 是否保存成功
     */
    public boolean savePluginVersion(PluginInfo pluginInfo, String pluginFilePath) {
        if (pluginInfo == null) {
            return false;
        }
        
        String pluginId = pluginInfo.getPluginId();
        String version = pluginInfo.getVersion();
        
        try {
            // 创建插件版本目录
            Path pluginVersionDir = Paths.get(versionRepositoryPath, pluginId);
            if (!Files.exists(pluginVersionDir)) {
                Files.createDirectories(pluginVersionDir);
            }
            
            // 拷贝插件文件到版本库
            File sourceFile = new File(pluginFilePath);
            File targetFile = getVersionFile(pluginId, version);
            
            if (!sourceFile.exists()) {
                log.error("源插件文件不存在: {}", pluginFilePath);
                return false;
            }
            
            // 如果目标文件已存在，先删除
            if (targetFile.exists()) {
                boolean deleted = targetFile.delete();
                if (!deleted) {
                    log.error("无法删除已存在的版本文件: {}", targetFile.getPath());
                    return false;
                }
            }
            
            // 拷贝文件
            Files.copy(sourceFile.toPath(), targetFile.toPath());
            
            // 更新或创建版本记录
            SysPluginVersion versionRecord = new SysPluginVersion();
            versionRecord.setPluginId(pluginId);
            versionRecord.setVersion(version);
            versionRecord.setReleaseNotes(pluginInfo.getDescriptor().getDescription());
            versionRecord.setRecordTime(LocalDateTime.now());
            versionRecord.setDeployTime(LocalDateTime.now());
            versionRecord.setDeployed(true);
            
            persistenceService.savePluginVersion(versionRecord);
            
            // 更新缓存
            versionCache
                .computeIfAbsent(pluginId, k -> new ConcurrentHashMap<>())
                .put(version, pluginInfo);
            
            log.info("保存插件[{}]版本[{}]成功", pluginId, version);
            return true;
        } catch (IOException e) {
            log.error("保存插件版本失败: " + pluginId, e);
            return false;
        }
    }
    
    /**
     * 获取插件版本
     *
     * @param pluginId 插件ID
     * @param version 版本号
     * @return 插件信息
     */
    public PluginInfo getPluginVersion(String pluginId, String version) {
        // 先从缓存中查找
        Map<String, PluginInfo> pluginVersions = versionCache.get(pluginId);
        if (pluginVersions != null && pluginVersions.containsKey(version)) {
            return pluginVersions.get(version);
        }
        
        // 缓存中不存在，尝试从文件系统加载
        try {
            File versionFile = getVersionFile(pluginId, version);
            if (!versionFile.exists()) {
                log.warn("插件[{}]版本[{}]文件不存在", pluginId, version);
                return null;
            }
            
            // 加载插件但不初始化
            PluginInfo pluginInfo = lifecycleManager.loadPlugin(versionFile.getPath(), false);
            
            // 添加到缓存
            versionCache
                .computeIfAbsent(pluginId, k -> new ConcurrentHashMap<>())
                .put(version, pluginInfo);
            
            return pluginInfo;
        } catch (Exception e) {
            log.error("获取插件[{}]版本[{}]失败", pluginId, version, e);
            return null;
        }
    }
    
    /**
     * 检查版本是否可用
     *
     * @param pluginId 插件ID
     * @param version 版本号
     * @return 是否可用
     */
    public boolean isVersionAvailable(String pluginId, String version) {
        // 检查缓存
        Map<String, PluginInfo> versions = versionCache.get(pluginId);
        if (versions != null && versions.containsKey(version)) {
            return true;
        }
        
        // 检查文件系统
        File versionFile = getVersionFile(pluginId, version);
        if (versionFile.exists()) {
            return true;
        }
        
        // 检查数据库记录
        try {
            Optional<SysPluginVersion> versionRecord = persistenceService.getPluginVersion(pluginId, version);
            return versionRecord.isPresent();
        } catch (Exception e) {
            log.error("检查版本可用性异常", e);
            return false;
        }
    }
    
    /**
     * 删除插件版本
     *
     * @param pluginId 插件ID
     * @param version 版本号
     * @return 是否删除成功
     */
    public boolean deletePluginVersion(String pluginId, String version) {
        try {
            // 删除版本文件
            File versionFile = getVersionFile(pluginId, version);
            if (versionFile.exists()) {
                boolean deleted = versionFile.delete();
                if (!deleted) {
                    log.error("无法删除版本文件: {}", versionFile.getPath());
                    return false;
                }
            }
            
            // 删除版本记录
            persistenceService.deletePluginVersion(pluginId, version);
            
            // 从缓存中移除
            Map<String, PluginInfo> versions = versionCache.get(pluginId);
            if (versions != null) {
                versions.remove(version);
                if (versions.isEmpty()) {
                    versionCache.remove(pluginId);
                }
            }
            
            log.info("删除插件[{}]版本[{}]成功", pluginId, version);
            return true;
        } catch (Exception e) {
            log.error("删除插件版本失败: " + pluginId, e);
            return false;
        }
    }
    
    /**
     * 获取插件所有版本
     *
     * @param pluginId 插件ID
     * @return 版本列表
     */
    public List<SysPluginVersion> getPluginVersions(String pluginId) {
        try {
            return persistenceService.getPluginVersionHistory(pluginId);
        } catch (Exception e) {
            log.error("获取插件版本历史失败: " + pluginId, e);
            return List.of(); // 返回空列表
        }
    }
    
    /**
     * 获取插件最新版本
     *
     * @param pluginId 插件ID
     * @return 最新版本
     */
    public Optional<SysPluginVersion> getLatestVersion(String pluginId) {
        try {
            List<SysPluginVersion> versions = persistenceService.getPluginVersionHistory(pluginId);
            if (versions.isEmpty()) {
                return Optional.empty();
            }
            
            // 按创建时间降序排序，取第一个
            versions.sort((v1, v2) -> v2.getRecordTime().compareTo(v1.getRecordTime()));
            return Optional.of(versions.get(0));
        } catch (Exception e) {
            log.error("获取插件最新版本失败: " + pluginId, e);
            return Optional.empty();
        }
    }
    
    /**
     * 获取插件版本升级路径
     *
     * @param pluginId 插件ID
     * @param fromVersion 起始版本
     * @param toVersion 目标版本
     * @return 升级路径(版本列表)
     */
    public List<String> getUpgradePath(String pluginId, String fromVersion, String toVersion) {
        // 获取所有版本记录
        List<SysPluginVersion> allVersions = getPluginVersions(pluginId);
        
        // 构建版本图
        Map<String, Set<String>> versionGraph = new HashMap<>();
        for (SysPluginVersion version : allVersions) {
            String v = version.getVersion();
            String prev = version.getPreviousVersion();
            
            if (prev != null) {
                // 添加从前一版本到当前版本的边
                versionGraph.computeIfAbsent(prev, k -> new HashSet<>()).add(v);
            }
            
            // 确保所有版本都在图中有节点
            versionGraph.computeIfAbsent(v, k -> new HashSet<>());
        }
        
        // 使用BFS查找最短路径
        return findShortestPath(versionGraph, fromVersion, toVersion);
    }
    
    /**
     * 查找从起始版本到目标版本的最短路径
     */
    private List<String> findShortestPath(Map<String, Set<String>> graph, String start, String end) {
        if (start.equals(end)) {
            return List.of(start);
        }
        
        Queue<String> queue = new LinkedList<>();
        Map<String, String> predecessors = new HashMap<>();
        Set<String> visited = new HashSet<>();
        
        queue.add(start);
        visited.add(start);
        
        while (!queue.isEmpty()) {
            String current = queue.poll();
            
            for (String neighbor : graph.getOrDefault(current, Set.of())) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    predecessors.put(neighbor, current);
                    queue.add(neighbor);
                    
                    if (neighbor.equals(end)) {
                        // 找到路径，构建结果
                        List<String> path = new ArrayList<>();
                        String node = end;
                        while (node != null) {
                            path.add(0, node);
                            node = predecessors.get(node);
                        }
                        return path;
                    }
                }
            }
        }
        
        // 未找到路径，返回直接升级路径
        return List.of(start, end);
    }
    
    /**
     * 清理缓存
     */
    public void clearCache() {
        versionCache.clear();
        log.info("插件版本缓存已清理");
    }
    
    /**
     * 获取系统中所有可用的版本号
     * 
     * @return 所有版本号的列表
     */
    public List<String> getAllAvailableVersions() {
        Set<String> allVersions = new HashSet<>();
        
        try {
            // 从数据库获取所有插件版本
            List<SysPluginVersion> dbVersions = persistenceService.getPluginVersionHistory();
            for (SysPluginVersion version : dbVersions) {
                allVersions.add(version.getVersion());
            }
            
            // 返回去重后的版本号列表
            return new ArrayList<>(allVersions);
        } catch (Exception e) {
            log.error("获取所有可用版本失败", e);
            return List.of(); // 返回空列表
        }
    }
    
    /**
     * 检查插件是否有更新版本可用
     *
     * @param pluginId 插件ID
     * @param currentVersion 当前版本
     * @return 是否有更新可用
     */
    public boolean hasUpdate(String pluginId, String currentVersion) {
        try {
            Optional<SysPluginVersion> latestVersionOpt = getLatestVersion(pluginId);
            if (latestVersionOpt.isEmpty()) {
                return false;  // 没有版本信息，视为无更新
            }
            
            String latestVersion = latestVersionOpt.get().getVersion();
            // 使用VersionUtils比较版本号
            return VersionUtils.compare(latestVersion, currentVersion) > 0;
        } catch (Exception e) {
            log.error("检查插件更新异常: {} {}", pluginId, currentVersion, e);
            return false;  // 出现异常，视为无更新
        }
    }
    
    /**
     * 比较两个版本号
     *
     * @param version1 版本1
     * @param version2 版本2
     * @return 如果version1 > version2返回正数，如果version1 < version2返回负数，相等返回0
     */
    public int compareVersions(String version1, String version2) {
        return VersionUtils.compare(version1, version2);
    }
    
    /**
     * 获取插件的最新版本号字符串
     *
     * @param pluginId 插件ID
     * @return 最新版本号字符串
     */
    public Optional<String> getLatestVersionString(String pluginId) {
        try {
            Optional<SysPluginVersion> latestVersionOpt = getLatestVersion(pluginId);
            if (latestVersionOpt.isEmpty()) {
                return Optional.empty();
            }
            
            return Optional.of(latestVersionOpt.get().getVersion());
        } catch (Exception e) {
            log.error("获取插件最新版本号异常: {}", pluginId, e);
            return Optional.empty();
        }
    }
    
    /**
     * 检查版本兼容性
     *
     * @param pluginId 插件ID
     * @param currentVersion 当前版本
     * @param targetVersion 目标版本
     * @return 是否兼容
     */
    public boolean checkVersionCompatibility(String pluginId, String currentVersion, String targetVersion) {
        try {
            // 获取版本信息
            List<SysPluginVersion> versions = getPluginVersions(pluginId);
            
            // 先检查是否存在目标版本
            boolean targetExists = versions.stream()
                    .anyMatch(v -> v.getVersion().equals(targetVersion));
            
            if (!targetExists) {
                log.warn("目标版本不存在: {} {}", pluginId, targetVersion);
                return false;
            }
            
            // 简单实现：假设主版本号相同则兼容
            // 在实际项目中，应该根据项目的版本兼容性规则实现更复杂的逻辑
            try {
                VersionUtils.Version v1 = VersionUtils.parseVersion(currentVersion);
                VersionUtils.Version v2 = VersionUtils.parseVersion(targetVersion);
                
                return v1.getMajor() == v2.getMajor();
            } catch (Exception e) {
                log.error("解析版本号异常: {} {} {}", pluginId, currentVersion, targetVersion, e);
                return false;
            }
        } catch (Exception e) {
            log.error("检查版本兼容性异常: {} {} {}", pluginId, currentVersion, targetVersion, e);
            return false;
        }
    }
    
    /**
     * 标记版本已部署
     *
     * @param pluginId 插件ID
     * @param version 版本号
     * @return 是否成功
     */
    public boolean markVersionDeployed(String pluginId, String version) {
        try {
            Optional<SysPluginVersion> versionOpt = persistenceService.getPluginVersion(pluginId, version);
            if (versionOpt.isEmpty()) {
                log.warn("版本不存在，无法标记为已部署: {} {}", pluginId, version);
                return false;
            }
            
            SysPluginVersion versionRecord = versionOpt.get();
            versionRecord.setDeployed(true);
            versionRecord.setDeployTime(LocalDateTime.now());
            
            persistenceService.savePluginVersion(versionRecord);
            log.info("成功标记版本为已部署: {} {}", pluginId, version);
            return true;
        } catch (Exception e) {
            log.error("标记版本已部署异常: {} {}", pluginId, version, e);
            return false;
        }
    }
} 