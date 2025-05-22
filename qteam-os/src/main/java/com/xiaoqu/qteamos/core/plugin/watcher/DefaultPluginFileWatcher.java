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

package com.xiaoqu.qteamos.core.plugin.watcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.xiaoqu.qteamos.api.core.plugin.api.PluginFileWatcher;
import com.xiaoqu.qteamos.core.plugin.event.EventBus;
import com.xiaoqu.qteamos.core.plugin.event.plugins.PluginFileCreatedEvent;
import com.xiaoqu.qteamos.core.plugin.event.plugins.PluginFileDeletedEvent;
import com.xiaoqu.qteamos.core.plugin.event.plugins.PluginFileModifiedEvent;

import jakarta.annotation.PreDestroy;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * 默认插件文件监控实现
 * 负责监控插件目录的文件变化，并发布相应的事件
 *
 * @author yangqijun
 * @date 2025-05-27
 * @since 1.0.0
 */
@Component
@SuppressWarnings("unchecked")
public class DefaultPluginFileWatcher implements PluginFileWatcher {
    private static final Logger log = LoggerFactory.getLogger(DefaultPluginFileWatcher.class);
    
    @Value("${plugin.file-wait-time:2000}")
    private long fileWaitTime;
    
    @Autowired
    private EventBus eventBus;
    
    // WatchService映射表，目录路径 -> WatchService
    private final Map<Path, WatchService> watchServices = new HashMap<>();
    
    // 监控线程映射表，目录路径 -> 监控线程
    private final Map<Path, Thread> watchThreads = new HashMap<>();
    
    // 已知文件映射表，文件路径 -> 最后修改时间
    private final Map<String, Long> knownFiles = new ConcurrentHashMap<>();
    
    // 监控目录集合
    private final Set<Path> watchDirectories = ConcurrentHashMap.newKeySet();
    
    // 线程池用于处理文件事件
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "plugin-file-watcher-worker");
        t.setDaemon(true);
        return t;
    });
    
    /**
     * 启动对指定目录的监控
     */
    @Override
    public void startWatching(Path directory) {
        if (!Files.isDirectory(directory)) {
            log.error("无法监控非目录路径: {}", directory);
            return;
        }
        
        try {
            log.info("开始监控目录: {}", directory);
            
            // 检查目录是否已被监控
            if (watchDirectories.contains(directory)) {
                log.warn("目录已在监控中: {}", directory);
                return;
            }
            
            // 创建WatchService实例
            WatchService watchService = FileSystems.getDefault().newWatchService();
            directory.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            
            // 存储WatchService
            watchServices.put(directory, watchService);
            watchDirectories.add(directory);
            
            // 创建并启动监控线程
            Thread watchThread = new Thread(() -> watchDirectory(directory, watchService));
            watchThread.setName("plugin-watcher-" + directory.getFileName());
            watchThread.setDaemon(true);
            watchThread.start();
            
            // 存储监控线程
            watchThreads.put(directory, watchThread);
            
        } catch (IOException e) {
            log.error("创建目录监控失败: {}", directory, e);
        }
    }
    
    /**
     * 停止所有目录监控
     */
    @Override
    public void stopWatching() {
        log.info("停止所有插件目录监控");
        
        // 关闭所有WatchService
        for (Map.Entry<Path, WatchService> entry : watchServices.entrySet()) {
            try {
                WatchService watchService = entry.getValue();
                if (watchService != null) {
                    watchService.close();
                }
            } catch (IOException e) {
                log.warn("关闭目录监控服务失败: {}", entry.getKey(), e);
            }
        }
        
        // 中断所有监控线程
        for (Thread thread : watchThreads.values()) {
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }
        }
        
        // 关闭线程池
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
        
        // 清空所有集合
        watchServices.clear();
        watchThreads.clear();
        watchDirectories.clear();
        knownFiles.clear();
    }
    
    /**
     * 将目录添加到监控列表
     */
    @Override
    public boolean addWatchDirectory(Path directory) {
        if (!Files.isDirectory(directory)) {
            log.error("无法添加非目录路径到监控: {}", directory);
            return false;
        }
        
        if (watchDirectories.contains(directory)) {
            log.info("目录已在监控中: {}", directory);
            return true;
        }
        
        startWatching(directory);
        return watchDirectories.contains(directory);
    }
    
    /**
     * 从监控列表移除目录
     */
    @Override
    public boolean removeWatchDirectory(Path directory) {
        if (!watchDirectories.contains(directory)) {
            return false;
        }
        
        try {
            // 关闭WatchService
            WatchService watchService = watchServices.get(directory);
            if (watchService != null) {
                watchService.close();
            }
            
            // 中断监控线程
            Thread watchThread = watchThreads.get(directory);
            if (watchThread != null && watchThread.isAlive()) {
                watchThread.interrupt();
            }
            
            // 移除记录
            watchServices.remove(directory);
            watchThreads.remove(directory);
            watchDirectories.remove(directory);
            
            log.info("已停止监控目录: {}", directory);
            return true;
        } catch (IOException e) {
            log.error("停止目录监控失败: {}", directory, e);
            return false;
        }
    }
    
    /**
     * 处理新文件事件
     */
    @Override
    public void handleFileCreated(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        
        log.info("处理新创建的文件: {}", file.getAbsolutePath());
        
        // 更新已知文件列表
        knownFiles.put(file.getAbsolutePath(), file.lastModified());
        
        // 发布文件创建事件
        eventBus.postEvent(new PluginFileCreatedEvent(file));
    }
    
    /**
     * 处理文件修改事件
     */
    @Override
    public void handleFileModified(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        
        String filePath = file.getAbsolutePath();
        long lastModified = file.lastModified();
        
        // 检查文件是否真的被修改了
        Long knownLastModified = knownFiles.get(filePath);
        if (knownLastModified != null && knownLastModified == lastModified) {
            // 文件未真正修改，忽略
            return;
        }
        
        log.info("处理已修改的文件: {}", filePath);
        
        // 更新已知文件列表
        knownFiles.put(filePath, lastModified);
        
        // 发布文件修改事件
        eventBus.postEvent(new PluginFileModifiedEvent(file));
    }
    
    /**
     * 处理文件删除事件
     */
    @Override
    public void handleFileDeleted(Path path) {
        if (path == null) {
            return;
        }
        
        String pathStr = path.toString();
        log.info("处理已删除的文件: {}", pathStr);
        
        // 从已知文件列表中移除
        knownFiles.remove(pathStr);
        
        // 发布文件删除事件
        eventBus.postEvent(new PluginFileDeletedEvent(path));
    }
    
    /**
     * 检查文件是否被监控
     */
    @Override
    public boolean isWatched(Path filePath) {
        if (filePath == null) {
            return false;
        }
        
        // 检查文件所在目录是否被监控
        Path parent = filePath.getParent();
        return watchDirectories.contains(parent);
    }
    
    /**
     * 监控目录变化的核心方法
     */
    private void watchDirectory(Path directory, WatchService watchService) {
        try {
            log.info("开始监控目录变化: {}", directory);
            WatchKey key;
            
            while (!Thread.currentThread().isInterrupted() && (key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    // 忽略OVERFLOW事件
                    if (kind == OVERFLOW) {
                        continue;
                    }
                    
                    // 获取文件名和完整路径
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path fileName = ev.context();
                    Path fullPath = directory.resolve(fileName);
                    File file = fullPath.toFile();
                    
                    // 过滤隐藏文件
                    if (fileName.toString().startsWith(".")) {
                        continue;
                    }
                    
                    // 根据事件类型处理
                    if (kind == ENTRY_CREATE) {
                        // 避免文件可能还在写入，等待一段时间
                        Thread.sleep(fileWaitTime);
                        executor.submit(() -> handleFileCreated(file));
                    } else if (kind == ENTRY_MODIFY) {
                        // 避免文件可能还在写入，等待一段时间
                        Thread.sleep(fileWaitTime);
                        executor.submit(() -> handleFileModified(file));
                    } else if (kind == ENTRY_DELETE) {
                        executor.submit(() -> handleFileDeleted(fullPath));
                    }
                }
                
                // 重置key，继续监听
                boolean valid = key.reset();
                if (!valid) {
                    log.warn("目录监控已失效，退出监控: {}", directory);
                    break;
                }
            }
        } catch (ClosedWatchServiceException e) {
            // 正常关闭，忽略异常
            log.debug("监控服务已关闭: {}", directory);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("监控线程被中断: {}", directory);
        } catch (Exception e) {
            log.error("监控目录时发生异常: {}", directory, e);
        } finally {
            log.info("停止监控目录: {}", directory);
        }
    }
    
    /**
     * 启动对指定目录的监控，并应用过滤器
     * 
     * @param directory 要监控的目录
     * @param filter 路径过滤器，用于过滤不需要处理的文件
     */
    @Override
    public void startWatchingWithFilter(Path directory, Predicate<Path> filter) {
        if (!Files.isDirectory(directory)) {
            log.error("无法监控非目录路径: {}", directory);
            return;
        }
        
        try {
            log.info("开始监控目录(带过滤器): {}", directory);
            
            // 检查目录是否已被监控
            if (watchDirectories.contains(directory)) {
                log.warn("目录已在监控中: {}", directory);
                return;
            }
            
            // 创建WatchService实例
            WatchService watchService = FileSystems.getDefault().newWatchService();
            directory.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            
            // 存储WatchService
            watchServices.put(directory, watchService);
            watchDirectories.add(directory);
            
            // 创建并启动监控线程，使用过滤器
            Thread watchThread = new Thread(() -> {
                try {
                    WatchKey key;
                    while ((key = watchService.take()) != null) {
                        Path dir = (Path) key.watchable();
                        
                        for (WatchEvent<?> event : key.pollEvents()) {
                            WatchEvent.Kind<?> kind = event.kind();
                            
                            // 处理溢出事件
                            if (kind == OVERFLOW) {
                                log.warn("监控事件溢出，可能有事件丢失");
                                continue;
                            }
                            
                            // 获取文件名
                            WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                            Path fileName = pathEvent.context();
                            Path fullPath = dir.resolve(fileName);
                            
                            // 应用过滤器
                            if (filter != null && !filter.test(fullPath)) {
                                log.debug("文件被过滤器忽略: {}", fullPath);
                                continue;
                            }
                            
                            // 处理事件
                            processWatchEvent(kind, fullPath);
                        }
                        
                        // 重置key以继续接收事件
                        boolean valid = key.reset();
                        if (!valid) {
                            log.warn("目录不再有效，停止监控: {}", dir);
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.info("监控线程中断: {}", directory);
                } catch (Exception e) {
                    log.error("监控线程异常: {}", directory, e);
                }
            });
            
            watchThread.setName("plugin-watcher-filtered-" + directory.getFileName());
            watchThread.setDaemon(true);
            watchThread.start();
            
            // 存储监控线程
            watchThreads.put(directory, watchThread);
            
        } catch (IOException e) {
            log.error("创建目录监控失败: {}", directory, e);
        }
    }
    
    /**
     * 处理监控事件
     */
    private void processWatchEvent(WatchEvent.Kind<?> kind, Path path) {
        File file = path.toFile();
        
        if (kind == ENTRY_CREATE) {
            // 等待文件写入完成
            waitForFileStability(file);
            
            // 处理文件创建事件
            executor.submit(() -> handleFileCreated(file));
        } else if (kind == ENTRY_MODIFY) {
            // 等待文件写入完成
            waitForFileStability(file);
            
            // 处理文件修改事件
            executor.submit(() -> handleFileModified(file));
        } else if (kind == ENTRY_DELETE) {
            // 处理文件删除事件
            executor.submit(() -> handleFileDeleted(path));
        }
    }
    
    /**
     * 等待文件写入完成
     */
    private void waitForFileStability(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return;
        }
        
        try {
            long lastModified = file.lastModified();
            Thread.sleep(fileWaitTime);
            
            // 如果文件在等待期间被修改，则认为还未写入完成
            if (file.exists() && file.lastModified() != lastModified) {
                waitForFileStability(file);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 关闭资源
     */
    @PreDestroy
    public void shutdown() {
        stopWatching();
    }
} 