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

package com.xiaoqu.qteamos.core.plugin.installer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.xiaoqu.qteamos.api.core.plugin.api.PluginInfo;
import com.xiaoqu.qteamos.api.core.plugin.api.PluginInstaller;
import com.xiaoqu.qteamos.api.core.plugin.model.PluginCandidate;
import com.xiaoqu.qteamos.core.plugin.adapter.PluginInfoAdapter;
import com.xiaoqu.qteamos.core.plugin.event.EventBus;
import com.xiaoqu.qteamos.core.plugin.event.PluginEvent;
import com.xiaoqu.qteamos.core.plugin.event.plugins.PluginInstalledEvent;
import com.xiaoqu.qteamos.core.plugin.event.plugins.PluginUninstalledEvent;
import com.xiaoqu.qteamos.core.plugin.manager.PluginRegistry;
import com.xiaoqu.qteamos.core.plugin.running.PluginDescriptor;
import com.xiaoqu.qteamos.core.utils.PluginFileUtils;

/**
 * 默认插件安装器实现
 * 负责插件的安装、卸载和升级
 *
 * @author yangqijun
 * @date 2025-05-27
 * @since 1.0.0
 */
@Component
public class DefaultPluginInstaller implements PluginInstaller {
    private static final Logger log = LoggerFactory.getLogger(DefaultPluginInstaller.class);
    
    @Value("${plugin.storage-path:./plugins}")
    private String pluginDir;
    
    @Value("${plugin.temp-dir:./plugins-temp}")
    private String tempDir;
    
    @Autowired
    private PluginRegistry pluginRegistry;
    
    @Autowired
    private EventBus eventBus;
    
    @Autowired
    private PluginInfoAdapter pluginInfoAdapter;
    
    /**
     * 安装插件候选者
     */
    @Override
    public PluginInfo installCandidate(PluginCandidate candidate) {
        if (candidate == null) {
            log.error("插件候选者为空");
            return null;
        }
        
        log.info("安装插件候选者: {}", candidate.getPath());
        
        Path path = candidate.getPath();
        if (path == null || !Files.exists(path)) {
            log.error("插件候选者路径不存在: {}", path);
            return null;
        }
        
        File file = path.toFile();
        switch (candidate.getType()) {
            case JAR_FILE:
                return installFromFile(file);
            case DIRECTORY:
                return installFromPath(path);
            default:
                log.error("不支持的插件候选者类型: {}", candidate.getType());
                return null;
        }
    }
    
    /**
     * 从文件安装插件
     */
    @Override
    public PluginInfo installFromFile(File pluginFile) {
        if (pluginFile == null || !pluginFile.exists() || !pluginFile.isFile()) {
            log.error("插件文件不存在或不是文件: {}", pluginFile);
            return null;
        }
        
        log.info("从文件安装插件: {}", pluginFile.getAbsolutePath());
        
        try {
            // 验证插件文件
            if (!validatePlugin(pluginFile)) {
                log.error("插件文件验证失败: {}", pluginFile.getAbsolutePath());
                return null;
            }
            
            // 解析插件描述符
            PluginDescriptor descriptor = PluginFileUtils.parsePluginDescriptor(pluginFile.toPath());
            if (descriptor == null) {
                log.error("无法解析插件描述符: {}", pluginFile.getAbsolutePath());
                return null;
            }
            
            String pluginId = descriptor.getPluginId();
            
            // 检查插件是否已存在
            if (pluginRegistry.hasPlugin(pluginId)) {
                return upgradePlugin(pluginId, pluginFile);
            }
            
            // 创建插件目录
            File pluginDirectory = new File(pluginDir, pluginId);
            if (!pluginDirectory.exists() && !pluginDirectory.mkdirs()) {
                log.error("创建插件目录失败: {}", pluginDirectory.getAbsolutePath());
                return null;
            }
            
            // 复制插件文件到插件目录
            File targetJar = new File(pluginDirectory, pluginId + ".jar");
            try {
                PluginFileUtils.copyFile(pluginFile, targetJar);
            } catch (IOException e) {
                log.error("复制插件文件失败: {}", e.getMessage(), e);
                return null;
            }
            
            // 提取plugin.yml
            File targetYml = new File(pluginDirectory, "plugin.yml");
            try {
                PluginFileUtils.extractPluginYml(pluginFile.toPath(), targetYml);
            } catch (IOException e) {
                log.error("提取plugin.yml失败: {}", e.getMessage(), e);
                return null;
            }
            
            // 创建并注册插件信息
            com.xiaoqu.qteamos.core.plugin.running.PluginInfo corePluginInfo = 
                    com.xiaoqu.qteamos.core.plugin.running.PluginInfo.builder()
                    .descriptor(descriptor)
                    .jarPath(targetJar.toPath())
                    .build();
            corePluginInfo.setPluginFile(pluginDirectory);
            
            // 注册插件
            pluginRegistry.registerPlugin(corePluginInfo);
            
            // 发布插件安装事件
            eventBus.postEvent(new PluginInstalledEvent(pluginId, descriptor.getVersion(), targetJar.toPath()));
            
            // 转换为API层PluginInfo并返回
            return pluginInfoAdapter.toApiPluginInfo(corePluginInfo);
            
        } catch (Exception e) {
            log.error("安装插件异常: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 从路径安装插件
     */
    @Override
    public PluginInfo installFromPath(Path pluginPath) {
        if (pluginPath == null || !Files.exists(pluginPath)) {
            log.error("插件路径不存在: {}", pluginPath);
            return null;
        }
        
        File pluginDirectory = pluginPath.toFile();
        if (!pluginDirectory.isDirectory()) {
            log.error("插件路径不是目录: {}", pluginPath);
            return null;
        }
        
        log.info("从目录安装插件: {}", pluginPath);
        
        try {
            // 查找plugin.yml文件
            File ymlFile = new File(pluginDirectory, "plugin.yml");
            if (!ymlFile.exists() || !ymlFile.isFile()) {
                log.error("插件目录中找不到plugin.yml文件: {}", pluginPath);
                return null;
            }
            
            // 解析插件描述符
            PluginDescriptor descriptor = PluginFileUtils.parsePluginYml(ymlFile);
            if (descriptor == null) {
                log.error("无法解析插件描述符: {}", ymlFile.getAbsolutePath());
                return null;
            }
            
            String pluginId = descriptor.getPluginId();
            
            // 查找插件JAR文件
            File jarFile = findPluginJarFile(pluginDirectory, pluginId);
            if (jarFile == null) {
                log.error("插件目录中找不到有效的JAR文件: {}", pluginPath);
                return null;
            }
            
            // 验证插件
            if (!validatePlugin(jarFile)) {
                log.error("插件验证失败: {}", pluginId);
                return null;
            }
            
            // 检查插件是否已存在
            if (pluginRegistry.hasPlugin(pluginId)) {
                return upgradePlugin(pluginId, jarFile);
            }
            
            // 创建目标插件目录
            File targetDirectory = new File(pluginDir, pluginId);
            if (!targetDirectory.exists() && !targetDirectory.mkdirs()) {
                log.error("创建插件目录失败: {}", targetDirectory.getAbsolutePath());
                return null;
            }
            
            // 复制整个目录内容
            copyDirectory(pluginDirectory, targetDirectory);
            
            // 创建并注册插件信息
            com.xiaoqu.qteamos.core.plugin.running.PluginInfo corePluginInfo = 
                    com.xiaoqu.qteamos.core.plugin.running.PluginInfo.builder()
                    .descriptor(descriptor)
                    .jarPath(new File(targetDirectory, jarFile.getName()).toPath())
                    .build();
            corePluginInfo.setPluginFile(targetDirectory);
            
            // 注册插件
            pluginRegistry.registerPlugin(corePluginInfo);
            
            // 发布插件安装事件
            eventBus.postEvent(new PluginInstalledEvent(pluginId, descriptor.getVersion(), corePluginInfo.getJarPath()));
            
            // 转换为API层PluginInfo并返回
            return pluginInfoAdapter.toApiPluginInfo(corePluginInfo);
            
        } catch (Exception e) {
            log.error("安装插件异常: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 卸载插件
     */
    @Override
    public boolean uninstallPlugin(String pluginId) {
        if (pluginId == null || pluginId.isEmpty()) {
            log.error("插件ID为空");
            return false;
        }
        
        log.info("卸载插件: {}", pluginId);
        
        try {
            // 检查插件是否存在
            if (!pluginRegistry.hasPlugin(pluginId)) {
                log.error("插件不存在: {}", pluginId);
                return false;
            }
            
            // 获取插件信息
            com.xiaoqu.qteamos.core.plugin.running.PluginInfo pluginInfo = 
                    pluginRegistry.getPlugin(pluginId).orElse(null);
            if (pluginInfo == null) {
                log.error("无法获取插件信息: {}", pluginId);
                return false;
            }
            
            // 从注册表中移除插件
            pluginRegistry.unregisterPlugin(pluginId);
            
            // 删除插件目录
            File pluginDirectory = pluginInfo.getPluginFile();
            if (pluginDirectory != null && pluginDirectory.exists()) {
                deleteDirectory(pluginDirectory);
            }
            
            // 发布插件卸载事件
            String version = pluginInfo.getDescriptor().getVersion();
            eventBus.postEvent(new PluginUninstalledEvent(pluginId, version));
            
            log.info("插件卸载成功: {}", pluginId);
            return true;
            
        } catch (Exception e) {
            log.error("卸载插件异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 升级插件
     */
    @Override
    public PluginInfo upgradePlugin(String pluginId, File newPluginFile) {
        if (pluginId == null || pluginId.isEmpty()) {
            log.error("插件ID为空");
            return null;
        }
        
        if (newPluginFile == null || !newPluginFile.exists() || !newPluginFile.isFile()) {
            log.error("新插件文件不存在或不是文件: {}", newPluginFile);
            return null;
        }
        
        log.info("升级插件: {}", pluginId);
        
        try {
            // 检查插件是否存在
            if (!pluginRegistry.hasPlugin(pluginId)) {
                log.error("插件不存在: {}", pluginId);
                return null;
            }
            
            // 解析新插件描述符
            PluginDescriptor newDescriptor = PluginFileUtils.parsePluginDescriptor(newPluginFile.toPath());
            if (newDescriptor == null) {
                log.error("无法解析新插件描述符: {}", newPluginFile.getAbsolutePath());
                return null;
            }
            
            // 验证插件ID一致
            if (!pluginId.equals(newDescriptor.getPluginId())) {
                log.error("新插件ID与要升级的插件ID不匹配: {} != {}", 
                        newDescriptor.getPluginId(), pluginId);
                return null;
            }
            
            // 验证插件
            if (!validatePlugin(newPluginFile)) {
                log.error("插件验证失败: {}", pluginId);
                return null;
            }
            
            // 获取当前插件信息
            com.xiaoqu.qteamos.core.plugin.running.PluginInfo currentPluginInfo = 
                    pluginRegistry.getPlugin(pluginId).orElse(null);
            if (currentPluginInfo == null) {
                log.error("无法获取当前插件信息: {}", pluginId);
                return null;
            }
            
            String currentVersion = currentPluginInfo.getDescriptor().getVersion();
            String newVersion = newDescriptor.getVersion();
            
            // 备份当前插件
            Path backupPath = backupPlugin(currentPluginInfo);
            if (backupPath == null) {
                log.error("备份当前插件失败: {}", pluginId);
                return null;
            }
            
            try {
                // 从注册表中移除当前插件
                pluginRegistry.unregisterPlugin(pluginId);
                
                // 创建新的插件目录
                File pluginDirectory = new File(pluginDir, pluginId);
                if (!pluginDirectory.exists() && !pluginDirectory.mkdirs()) {
                    // 如果创建失败，尝试恢复备份
                    restoreBackup(backupPath, pluginId);
                    log.error("创建插件目录失败: {}", pluginDirectory.getAbsolutePath());
                    return null;
                }
                
                // 清空插件目录
                cleanDirectory(pluginDirectory);
                
                // 复制新插件文件
                File targetJar = new File(pluginDirectory, pluginId + ".jar");
                try {
                    PluginFileUtils.copyFile(newPluginFile, targetJar);
                } catch (IOException e) {
                    // 如果复制失败，尝试恢复备份
                    restoreBackup(backupPath, pluginId);
                    log.error("复制新插件文件失败: {}", e.getMessage(), e);
                    return null;
                }
                
                // 提取plugin.yml
                File targetYml = new File(pluginDirectory, "plugin.yml");
                try {
                    PluginFileUtils.extractPluginYml(newPluginFile.toPath(), targetYml);
                } catch (IOException e) {
                    // 如果提取失败，尝试恢复备份
                    restoreBackup(backupPath, pluginId);
                    log.error("提取plugin.yml失败: {}", e.getMessage(), e);
                    return null;
                }
                
                // 创建新的插件信息
                com.xiaoqu.qteamos.core.plugin.running.PluginInfo newPluginInfo = 
                        com.xiaoqu.qteamos.core.plugin.running.PluginInfo.builder()
                        .descriptor(newDescriptor)
                        .jarPath(targetJar.toPath())
                        .build();
                newPluginInfo.setPluginFile(pluginDirectory);
                
                // 注册新插件
                pluginRegistry.registerPlugin(newPluginInfo);
                
                // 发布插件升级事件
                eventBus.postEvent(PluginEvent.createEnabledEvent(pluginId, newVersion));
                
                log.info("插件升级成功: {} ({}->{})", pluginId, currentVersion, newVersion);
                
                // 删除备份
                deleteDirectory(backupPath.toFile());
                
                // 转换为API层PluginInfo并返回
                return pluginInfoAdapter.toApiPluginInfo(newPluginInfo);
                
            } catch (Exception e) {
                // 如果出现异常，尝试恢复备份
                restoreBackup(backupPath, pluginId);
                log.error("升级插件异常: {}", e.getMessage(), e);
                return null;
            }
            
        } catch (Exception e) {
            log.error("升级插件异常: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 从临时目录处理插件
     */
    @Override
    public PluginInfo processTempFile(File tempFile) {
        if (tempFile == null || !tempFile.exists()) {
            log.error("临时文件不存在: {}", tempFile);
            return null;
        }
        
        log.info("处理临时文件: {}", tempFile.getAbsolutePath());
        
        if (tempFile.isDirectory()) {
            return processTempDirectory(tempFile);
        } else if (tempFile.isFile() && tempFile.getName().endsWith(".jar")) {
            return processTempJarFile(tempFile);
        } else {
            log.error("不支持的临时文件类型: {}", tempFile.getAbsolutePath());
            return null;
        }
    }
    
    /**
     * 验证插件
     */
    @Override
    public boolean validatePlugin(File pluginFile) {
        if (pluginFile == null || !pluginFile.exists() || !pluginFile.isFile()) {
            log.error("插件文件不存在或不是文件: {}", pluginFile);
            return false;
        }
        
        log.info("验证插件文件: {}", pluginFile.getAbsolutePath());
        
        try {
            // 解析插件描述符
            PluginDescriptor descriptor = PluginFileUtils.parsePluginDescriptor(pluginFile.toPath());
            if (descriptor == null) {
                log.error("无法解析插件描述符: {}", pluginFile.getAbsolutePath());
                return false;
            }
            
            // 检查必要字段
            if (descriptor.getPluginId() == null || descriptor.getPluginId().isEmpty()) {
                log.error("插件ID不能为空");
                return false;
            }
            
            if (descriptor.getVersion() == null || descriptor.getVersion().isEmpty()) {
                log.error("插件版本不能为空");
                return false;
            }
            
            if (descriptor.getMainClass() == null || descriptor.getMainClass().isEmpty()) {
                log.error("插件主类不能为空");
                return false;
            }
            
            // TODO: 可以添加更多验证逻辑，如签名验证、兼容性检查等
            
            return true;
            
        } catch (Exception e) {
            log.error("验证插件异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    //-------------------------------------------------------------------------
    // 私有辅助方法
    //-------------------------------------------------------------------------
    
    /**
     * 处理临时目录中的插件目录
     */
    private PluginInfo processTempDirectory(File tempDirectory) {
        log.info("处理临时插件目录: {}", tempDirectory.getName());
        
        // 查找plugin.yml文件
        File ymlFile = new File(tempDirectory, "plugin.yml");
        if (!ymlFile.exists() || !ymlFile.isFile()) {
            log.error("临时插件目录中找不到plugin.yml文件: {}", tempDirectory);
            return null;
        }
        
        try {
            // 解析插件描述符
            PluginDescriptor descriptor = PluginFileUtils.parsePluginYml(ymlFile);
            if (descriptor == null) {
                log.error("无法解析插件描述符: {}", ymlFile.getAbsolutePath());
                return null;
            }
            
            String pluginId = descriptor.getPluginId();
            
            // 查找插件JAR文件
            File jarFile = findPluginJarFile(tempDirectory, pluginId);
            if (jarFile == null) {
                log.error("临时插件目录中找不到有效的JAR文件: {}", tempDirectory);
                return null;
            }
            
            // 验证插件
            if (!validatePlugin(jarFile)) {
                log.error("插件验证失败: {}", pluginId);
                return null;
            }
            
            // 安装插件
            return installFromPath(tempDirectory.toPath());
            
        } catch (Exception e) {
            log.error("处理临时插件目录异常: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 处理临时目录中的JAR文件
     */
    private PluginInfo processTempJarFile(File jarFile) {
        log.info("处理临时插件JAR文件: {}", jarFile.getName());
        
        try {
            // 解析插件描述符
            PluginDescriptor descriptor = PluginFileUtils.parsePluginDescriptor(jarFile.toPath());
            if (descriptor == null) {
                log.error("无法解析插件描述符: {}", jarFile.getAbsolutePath());
                return null;
            }
            
            // 验证插件
            if (!validatePlugin(jarFile)) {
                log.error("插件验证失败: {}", descriptor.getPluginId());
                return null;
            }
            
            // 安装插件
            return installFromFile(jarFile);
            
        } catch (Exception e) {
            log.error("处理临时插件JAR文件异常: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 查找插件JAR文件
     */
    private File findPluginJarFile(File directory, String pluginId) {
        // 按优先级查找以下文件：
        // 1. pluginId.jar
        File exactMatch = new File(directory, pluginId + ".jar");
        if (exactMatch.exists() && exactMatch.isFile()) {
            return exactMatch;
        }
        
        // 2. 以pluginId-开头的jar (匹配如 pluginId-1.0.0.jar)
        File[] versionedJars = directory.listFiles((dir, name) -> 
            name.startsWith(pluginId + "-") && name.endsWith(".jar"));
        if (versionedJars != null && versionedJars.length > 0) {
            // 如果有多个版本，返回按字母排序最后一个（通常是最新版本）
            java.util.Arrays.sort(versionedJars);
            return versionedJars[versionedJars.length - 1];
        }
        
        // 3. 目录中的plugin.jar (保持向后兼容)
        File legacyFile = new File(directory, "plugin.jar");
        if (legacyFile.exists() && legacyFile.isFile()) {
            return legacyFile;
        }
        
        // 4. 如果目录中只有一个jar文件，直接使用它
        File[] jarFiles = directory.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles != null && jarFiles.length == 1) {
            return jarFiles[0];
        }
        
        // 没有找到合适的JAR文件
        return null;
    }
    
    /**
     * 备份插件
     */
    private Path backupPlugin(com.xiaoqu.qteamos.core.plugin.running.PluginInfo pluginInfo) {
        try {
            File pluginDirectory = pluginInfo.getPluginFile();
            if (pluginDirectory == null || !pluginDirectory.exists()) {
                log.error("插件目录不存在: {}", pluginDirectory);
                return null;
            }
            
            String pluginId = pluginInfo.getDescriptor().getPluginId();
            String backupDirName = String.format("%s_backup_%s_%s", 
                    pluginId, 
                    pluginInfo.getDescriptor().getVersion(),
                    UUID.randomUUID().toString().substring(0, 8));
            
            File backupDir = new File(tempDir, backupDirName);
            if (!backupDir.exists() && !backupDir.mkdirs()) {
                log.error("创建备份目录失败: {}", backupDir.getAbsolutePath());
                return null;
            }
            
            // 复制整个插件目录
            copyDirectory(pluginDirectory, backupDir);
            
            log.info("插件备份成功: {} -> {}", pluginId, backupDir.getAbsolutePath());
            return backupDir.toPath();
            
        } catch (Exception e) {
            log.error("备份插件异常: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 恢复插件备份
     */
    private boolean restoreBackup(Path backupPath, String pluginId) {
        try {
            File backupDir = backupPath.toFile();
            if (!backupDir.exists() || !backupDir.isDirectory()) {
                log.error("备份目录不存在: {}", backupPath);
                return false;
            }
            
            File targetDir = new File(pluginDir, pluginId);
            
            // 清空目标目录
            if (targetDir.exists()) {
                cleanDirectory(targetDir);
            } else if (!targetDir.mkdirs()) {
                log.error("创建插件目录失败: {}", targetDir.getAbsolutePath());
                return false;
            }
            
            // 复制备份内容
            copyDirectory(backupDir, targetDir);
            
            // 加载插件描述符
            File ymlFile = new File(targetDir, "plugin.yml");
            if (!ymlFile.exists()) {
                log.error("恢复的插件目录中找不到plugin.yml文件: {}", targetDir);
                return false;
            }
            
            PluginDescriptor descriptor = PluginFileUtils.parsePluginYml(ymlFile);
            if (descriptor == null) {
                log.error("无法解析恢复的插件描述符: {}", ymlFile.getAbsolutePath());
                return false;
            }
            
            // 查找插件JAR文件
            File jarFile = findPluginJarFile(targetDir, pluginId);
            if (jarFile == null) {
                log.error("恢复的插件目录中找不到有效的JAR文件: {}", targetDir);
                return false;
            }
            
            // 创建插件信息
            com.xiaoqu.qteamos.core.plugin.running.PluginInfo pluginInfo = 
                    com.xiaoqu.qteamos.core.plugin.running.PluginInfo.builder()
                    .descriptor(descriptor)
                    .jarPath(jarFile.toPath())
                    .build();
            pluginInfo.setPluginFile(targetDir);
            
            // 注册恢复的插件
            pluginRegistry.registerPlugin(pluginInfo);
            
            log.info("插件备份恢复成功: {}", pluginId);
            return true;
            
        } catch (Exception e) {
            log.error("恢复插件备份异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 复制目录内容
     */
    private void copyDirectory(File sourceDir, File targetDir) throws IOException {
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new IOException("创建目标目录失败: " + targetDir.getAbsolutePath());
        }
        
        File[] files = sourceDir.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            File targetFile = new File(targetDir, file.getName());
            
            if (file.isDirectory()) {
                copyDirectory(file, targetFile);
            } else {
                Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
    
    /**
     * 清空目录内容
     */
    private void cleanDirectory(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                deleteDirectory(file);
            } else if (!file.delete()) {
                log.warn("删除文件失败: {}", file.getAbsolutePath());
            }
        }
    }
    
    /**
     * 递归删除目录
     */
    private void deleteDirectory(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                deleteDirectory(file);
            } else if (!file.delete()) {
                log.warn("删除文件失败: {}", file.getAbsolutePath());
            }
        }
        
        if (!directory.delete()) {
            log.warn("删除目录失败: {}", directory.getAbsolutePath());
        }
    }
} 