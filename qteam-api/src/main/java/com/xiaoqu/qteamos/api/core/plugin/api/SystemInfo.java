package com.xiaoqu.qteamos.api.core.plugin.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统信息
 *
 * @author yangqijun
 * @date 2025-05-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemInfo {

    /**
     * 系统版本
     */
    private String version;

    /**
     * 系统名称
     */
    private String name;

    /**
     * 系统构建时间
     */
    private String buildTime;

    /**
     * 系统运行环境
     */
    private String environment;

    /**
     * 系统部署模式
     */
    private String deployMode;

    /**
     * 系统已运行时间（毫秒）
     */
    private long uptime;

    /**
     * JVM内存信息
     */
    private MemoryInfo memoryInfo;

    /**
     * 获取格式化的运行时间
     *
     * @return 格式化的运行时间
     */
    public String getFormattedUptime() {
        long seconds = uptime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        return String.format("%d天%d小时%d分钟%d秒", 
                days, hours % 24, minutes % 60, seconds % 60);
    }

    /**
     * 内存信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoryInfo {
        /**
         * 已分配内存（MB）
         */
        private long allocatedMemory;
        
        /**
         * 已使用内存（MB）
         */
        private long usedMemory;
        
        /**
         * 最大可用内存（MB）
         */
        private long maxMemory;
    }
} 