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

package com.xiaoqu.qteamos.api.core.plugin.model;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * 插件候选者模型
 * 表示在扫描过程中发现的潜在插件
 *
 * @author yangqijun
 * @date 2025-05-25
 * @since 1.0.0
 */
public class PluginCandidate {
    
    /**
     * 候选者类型
     */
    public enum CandidateType {
        /** JAR文件 */
        JAR_FILE,
        /** 目录 */
        DIRECTORY,
        /** 临时文件 */
        TEMP_FILE,
        /** 未知类型 */
        UNKNOWN
    }
    
    /** 插件路径 */
    private final Path path;
    
    /** 插件ID，如果已解析 */
    private String pluginId;
    
    /** 候选者类型 */
    private final CandidateType type;
    
    /** 发现时间 */
    private final LocalDateTime discoveredTime;
    
    /** 文件大小 (bytes) */
    private final long fileSize;
    
    /** 校验和 (如果计算了的话) */
    private String checksum;
    
    /** 是否已验证 */
    private boolean validated;
    
    /**
     * 创建一个插件候选者
     *
     * @param path 插件文件或目录路径
     * @param type 候选者类型
     */
    public PluginCandidate(Path path, CandidateType type) {
        this.path = path;
        this.type = type;
        this.discoveredTime = LocalDateTime.now();
        
        File file = path.toFile();
        this.fileSize = file.isFile() ? file.length() : -1;
        this.validated = false;
    }
    
    /**
     * 获取插件路径
     *
     * @return 插件路径
     */
    public Path getPath() {
        return path;
    }
    
    /**
     * 获取插件ID
     *
     * @return 插件ID，如果尚未解析则返回null
     */
    public String getPluginId() {
        return pluginId;
    }
    
    /**
     * 设置插件ID
     *
     * @param pluginId 插件ID
     */
    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }
    
    /**
     * 获取候选者类型
     *
     * @return 候选者类型
     */
    public CandidateType getType() {
        return type;
    }
    
    /**
     * 获取发现时间
     *
     * @return 发现时间
     */
    public LocalDateTime getDiscoveredTime() {
        return discoveredTime;
    }
    
    /**
     * 获取文件大小
     *
     * @return 文件大小（字节数）
     */
    public long getFileSize() {
        return fileSize;
    }
    
    /**
     * 获取校验和
     *
     * @return 校验和
     */
    public String getChecksum() {
        return checksum;
    }
    
    /**
     * 设置校验和
     *
     * @param checksum 校验和
     */
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
    
    /**
     * 检查是否已验证
     *
     * @return 是否已验证
     */
    public boolean isValidated() {
        return validated;
    }
    
    /**
     * 设置验证状态
     *
     * @param validated 验证状态
     */
    public void setValidated(boolean validated) {
        this.validated = validated;
    }
    
    @Override
    public String toString() {
        return "PluginCandidate{" +
                "path=" + path +
                ", pluginId='" + pluginId + '\'' +
                ", type=" + type +
                ", discoveredTime=" + discoveredTime +
                ", fileSize=" + fileSize +
                ", validated=" + validated +
                '}';
    }
} 