/*
 * Copyright (c) 2023-2024 XiaoQuTeam. All rights reserved.
 * QTeamOS is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package com.xiaoqu.qteamos.core.plugin.scanner;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.xiaoqu.qteamos.api.core.plugin.api.PluginScanner;
import com.xiaoqu.qteamos.core.plugin.event.EventBus;
import com.xiaoqu.qteamos.core.plugin.event.plugins.PluginDiscoveredEvent;
import com.xiaoqu.qteamos.api.core.plugin.model.PluginCandidate;
import com.xiaoqu.qteamos.api.core.plugin.model.PluginCandidate.CandidateType;
import com.xiaoqu.qteamos.core.plugin.running.PluginDescriptor;
import com.xiaoqu.qteamos.core.plugin.running.PluginDescriptorLoader;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * 默认插件扫描器实现
 * 负责扫描插件目录，发现新的插件
 *
 * @author yangqijun
 * @date 2025-05-25
 * @since 1.0.0
 */
@Component
public class DefaultPluginScanner implements PluginScanner {
    private static final Logger log = LoggerFactory.getLogger(DefaultPluginScanner.class);
    
    @Value("${plugin.storage-path:./plugins}")
    private String pluginDir;
    
    @Value("${plugin.scan-interval:300000}")
    private long scanInterval;
    
    @Value("${plugin.auto-discover:true}")
    private boolean autoDiscoverEnabled;
    
    @Autowired
    private EventBus eventBus;
    
    @Autowired
    private PluginDescriptorLoader descriptorLoader;
    
    // 已知插件文件映射（文件路径 -> 最后修改时间）
    private final Map<String, Long> knownPluginFiles = new ConcurrentHashMap<>();
    
    // 定时扫描任务执行器
    private ScheduledExecutorService scheduledExecutor;
    
    /**
     * 初始化扫描器
     */
    @PostConstruct
    public void init() {
        if (autoDiscoverEnabled) {
            startScheduledScanning();
        }
    }
    
    /**
     * 关闭扫描器资源
     */
    @PreDestroy
    public void shutdown() {
        stopScheduledScanning();
    }
    
    @Override
    public List<PluginCandidate> scanPlugins(Path directory) {
        List<PluginCandidate> candidates = new ArrayList<>();
        File dir = directory.toFile();
        
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("插件目录不存在或不是目录: {}", directory);
            return candidates;
        }
        
        log.debug("扫描插件目录: {}", directory);
        File[] files = dir.listFiles();
        if (files == null) {
            return candidates;
        }
        
        for (File file : files) {
            // 忽略隐藏文件和非目录、非JAR文件
            if (file.isHidden() || (!file.isDirectory() && !file.getName().endsWith(".jar"))) {
                continue;
            }
            
            // 检查是否是新文件或已修改的文件
            String filePath = file.getAbsolutePath();
            long lastModified = file.lastModified();
            
            if (knownPluginFiles.containsKey(filePath)) {
                Long knownLastModified = knownPluginFiles.get(filePath);
                if (knownLastModified != null && knownLastModified == lastModified) {
                    // 文件未变化，跳过
                    continue;
                }
            }
            
            // 处理插件文件或目录
            try {
                PluginCandidate candidate = createCandidate(file);
                if (candidate != null) {
                    candidates.add(candidate);
                    
                    // 发布插件发现事件
                    eventBus.postEvent(new PluginDiscoveredEvent(candidate));
                    
                    // 更新已知插件文件列表
                    knownPluginFiles.put(filePath, lastModified);
                }
            } catch (Exception e) {
                log.error("处理可能的插件文件时出错: {}", file.getAbsolutePath(), e);
            }
        }
        
        log.info("扫描目录[{}]完成，发现{}个插件候选", directory, candidates.size());
        return candidates;
    }
    
    /**
     * 创建插件候选者对象
     */
    private PluginCandidate createCandidate(File file) {
        CandidateType type;
        
        if (file.isDirectory()) {
            // 检查目录中是否包含plugin.jar或有效的插件结构
            File[] subFiles = file.listFiles();
            if (subFiles == null || subFiles.length == 0) {
                return null;
            }
            
            boolean hasPluginJar = false;
            for (File subFile : subFiles) {
                if (subFile.getName().endsWith(".jar")) {
                    hasPluginJar = true;
                    break;
                }
            }
            
            if (!hasPluginJar) {
                return null;
            }
            
            type = CandidateType.DIRECTORY;
        } else if (file.getName().endsWith(".jar")) {
            // JAR文件
            type = CandidateType.JAR_FILE;
        } else {
            // 其他类型，不支持
            return null;
        }
        
        // 创建候选者对象
        PluginCandidate candidate = new PluginCandidate(file.toPath(), type);
        
        // 尝试解析插件ID
        try {
            if (type == CandidateType.JAR_FILE) {
                PluginDescriptor descriptor = descriptorLoader.loadFromJar(file);
                if (descriptor != null) {
                    candidate.setPluginId(descriptor.getPluginId());
                    candidate.setValidated(true);
                }
            } else {
                // 对于目录，尝试查找主JAR文件并解析
                File[] subFiles = file.listFiles();
                if (subFiles != null) {
                    for (File subFile : subFiles) {
                        if (subFile.getName().endsWith(".jar")) {
                            PluginDescriptor descriptor = descriptorLoader.loadFromJar(subFile);
                            if (descriptor != null) {
                                candidate.setPluginId(descriptor.getPluginId());
                                candidate.setValidated(true);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析插件描述符失败: {}", file.getAbsolutePath(), e);
        }
        
        return candidate;
    }
    
    @Override
    public void startScheduledScanning() {
        if (scheduledExecutor != null && !scheduledExecutor.isShutdown()) {
            // 已经启动，忽略
            return;
        }
        
        log.info("启动定期插件扫描，间隔{}毫秒", scanInterval);
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "plugin-scanner");
            t.setDaemon(true);
            return t;
        });
        
        scheduledExecutor.scheduleWithFixedDelay(
            this::scanOnce,
            0,  // 立即开始第一次扫描
            scanInterval,
            TimeUnit.MILLISECONDS
        );
    }
    
    @Override
    public void stopScheduledScanning() {
        if (scheduledExecutor != null) {
            log.info("停止定期插件扫描");
            scheduledExecutor.shutdown();
            try {
                if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduledExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                scheduledExecutor.shutdownNow();
            }
            scheduledExecutor = null;
        }
    }
    
    @Override
    public int scanOnce() {
        log.debug("执行一次插件扫描");
        try {
            Path dirPath = new File(pluginDir).toPath();
            List<PluginCandidate> candidates = scanPlugins(dirPath);
            return candidates.size();
        } catch (Exception e) {
            log.error("插件扫描出错", e);
            return 0;
        }
    }
} 