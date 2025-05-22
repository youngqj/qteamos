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
    
    @Value("${plugin.dev-dir:./plugins-dev}")
    private String pluginDevDir;
    
    // 插件目录是否使用绝对路径
    @Value("${plugin.use-absolute-path:false}")
    private boolean useAbsolutePath;
    
    // 是否包含开发目录
    @Value("${plugin.include-dev-dir:true}")
    private boolean includeDevDir;
    
    // 自定义额外插件目录
    @Value("${plugin.additional-dirs:}")
    private String additionalDirs;
    
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
        // 自动识别单位：如果值小于1000，假定单位是秒而非毫秒
        if (scanInterval > 0 && scanInterval < 1000) {
            log.warn("配置的扫描间隔 {} 可能是以秒为单位，自动转换为毫秒: {}", 
                    scanInterval, scanInterval * 1000);
            scanInterval = scanInterval * 1000;
        }
        
        // 确保扫描间隔在合理范围内（至少5秒，避免过于频繁扫描）
        if (scanInterval < 5000) {
            log.warn("配置的扫描间隔 {}ms 过小，自动调整为5000ms", scanInterval);
            scanInterval = 5000;
        }
        
        // 初始化时输出路径信息，便于调试
        log.info("插件扫描器初始化，扫描路径: {}，间隔: {}ms", pluginDir, scanInterval);
        
        // 不在初始化时自动启动扫描，让PluginSystemNew控制扫描时机
        // if (autoDiscoverEnabled) {
        //     startScheduledScanning();
        // }
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
        String dirName = file.getName();
        File candidateJar = null;
        
        if (file.isDirectory()) {
            // 检查目录中是否包含plugin.yml文件
            File pluginYmlFile = new File(file, "plugin.yml");
            boolean hasPluginYml = pluginYmlFile.exists() && pluginYmlFile.isFile();
            
            // 检查目录中是否包含JAR文件
            File[] subFiles = file.listFiles();
            boolean hasJarFile = false;
            
            if (subFiles != null) {
                for (File subFile : subFiles) {
                    String fileName = subFile.getName();
                    if (fileName.endsWith(".jar")) {
                        hasJarFile = true;
                        
                        // 寻找与目录同名的JAR作为候选
                        if (fileName.equals(dirName + ".jar") || 
                            fileName.equals("plugin.jar") || 
                            fileName.startsWith(dirName + "-")) {
                            candidateJar = subFile;
                            break;
                        } else if (candidateJar == null) {
                            // 记录第一个找到的JAR作为备选
                            candidateJar = subFile;
                        }
                    }
                }
            }
            
            // 必须至少有plugin.yml文件或JAR文件之一
            if (!hasPluginYml && !hasJarFile) {
                log.debug("目录不是有效的插件: {}, hasPluginYml={}, hasJarFile={}", 
                          file.getAbsolutePath(), hasPluginYml, hasJarFile);
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
                // 直接从JAR文件加载描述符
                PluginDescriptor descriptor = descriptorLoader.loadFromJar(file);
                if (descriptor != null) {
                    candidate.setPluginId(descriptor.getPluginId());
                    candidate.setValidated(true);
                    log.debug("从JAR文件加载到插件描述符: {}, 插件ID: {}", 
                             file.getName(), descriptor.getPluginId());
                }
            } else {
                // 目录类型插件处理
                
                // 优先尝试从plugin.yml文件加载
                File pluginYmlFile = new File(file, "plugin.yml");
                if (pluginYmlFile.exists() && pluginYmlFile.isFile()) {
                    PluginDescriptor descriptor = descriptorLoader.loadFromFile(pluginYmlFile);
                    if (descriptor != null) {
                        candidate.setPluginId(descriptor.getPluginId());
                        candidate.setValidated(true);
                        log.debug("从plugin.yml文件加载到插件描述符: {}, 插件ID: {}", 
                                 pluginYmlFile.getName(), descriptor.getPluginId());
                        return candidate;
                    }
                }
                
                // 如果从plugin.yml加载失败，尝试从JAR文件加载
                if (candidateJar != null) {
                    PluginDescriptor descriptor = descriptorLoader.loadFromJar(candidateJar);
                    if (descriptor != null) {
                        candidate.setPluginId(descriptor.getPluginId());
                        candidate.setValidated(true);
                        log.debug("从目录中的JAR文件加载到插件描述符: {}, 插件ID: {}", 
                                 candidateJar.getName(), descriptor.getPluginId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("加载插件描述符失败: {}", file.getAbsolutePath(), e);
        }
        
        return candidate;
    }
    
    @Override
    public void startScheduledScanning() {
        if (scheduledExecutor != null && !scheduledExecutor.isShutdown()) {
            // 已经启动，忽略
            return;
        }
        
        if (scanInterval < 5000) {
            log.warn("扫描间隔 {}ms 过小，这可能会导致系统性能问题", scanInterval);
        }
        
        log.info("启动定期插件扫描，间隔{}毫秒（{}秒）", scanInterval, scanInterval / 1000);
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
        int totalCandidates = 0;
        
        try {
            // 扫描主插件目录
            File mainPluginDir = resolvePath(pluginDir);
            if (mainPluginDir.exists()) {
                log.debug("扫描插件目录: {}", mainPluginDir.getAbsolutePath());
                List<PluginCandidate> candidates = scanPlugins(mainPluginDir.toPath());
                totalCandidates += candidates.size();
            } else {
                log.warn("插件目录不存在: {}", mainPluginDir.getAbsolutePath());
            }
            
            // 扫描开发插件目录（如果配置了包含开发目录）
            if (includeDevDir && pluginDevDir != null && !pluginDevDir.isEmpty()) {
                File devPluginDir = resolvePath(pluginDevDir);
                if (devPluginDir.exists()) {
                    log.debug("扫描开发插件目录: {}", devPluginDir.getAbsolutePath());
                    List<PluginCandidate> candidates = scanPlugins(devPluginDir.toPath());
                    totalCandidates += candidates.size();
                } else {
                    log.warn("开发插件目录不存在: {}", devPluginDir.getAbsolutePath());
                }
            }
            
            // 扫描额外的插件目录（如果有配置）
            if (additionalDirs != null && !additionalDirs.isEmpty()) {
                String[] dirs = additionalDirs.split(",");
                for (String dir : dirs) {
                    dir = dir.trim();
                    if (!dir.isEmpty()) {
                        File additionalDir = resolvePath(dir);
                        if (additionalDir.exists()) {
                            log.debug("扫描额外插件目录: {}", additionalDir.getAbsolutePath());
                            List<PluginCandidate> candidates = scanPlugins(additionalDir.toPath());
                            totalCandidates += candidates.size();
                        } else {
                            log.warn("额外插件目录不存在: {}", additionalDir.getAbsolutePath());
                        }
                    }
                }
            }
            
            return totalCandidates;
        } catch (Exception e) {
            log.error("插件扫描出错", e);
            return 0;
        }
    }
    
    /**
     * 解析路径为文件对象，处理相对路径和绝对路径
     *
     * @param path 路径字符串
     * @return 解析后的文件对象
     */
    private File resolvePath(String path) {
        if (path == null || path.isEmpty()) {
            return new File(".");
        }
        
        File file;
        if (path.startsWith("/") || path.contains(":")) {
            // 已经是绝对路径
            file = new File(path);
        } else if (useAbsolutePath) {
            // 转换为绝对路径（相对于用户目录）
            String userDir = System.getProperty("user.dir");
            file = new File(userDir, path);
        } else {
            // 使用相对路径
            file = new File(path);
        }
        
        return file;
    }
} 