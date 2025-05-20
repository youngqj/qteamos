package com.xiaoqu.qteamos.core.plugin.event.plugins;


import com.xiaoqu.qteamos.core.plugin.event.AbstractEvent;

/**
 * 系统启动事件
 * 在系统启动完成后发布，用于通知插件系统已准备就绪
 *
 * @author yangqijun
 * @date 2024-07-03
 */
public class SystemStartupEvent extends AbstractEvent {
    
    /**
     * 系统事件主题
     */
    public static final String TOPIC = "system";
    
    /**
     * 系统启动事件类型
     */
    public static final String TYPE = "startup";
    
    /**
     * 启动时间
     */
    private final long startupTime;
    
    /**
     * 系统信息
     */
    private final SystemInfo systemInfo;
    
    /**
     * 构造函数
     */
    public SystemStartupEvent() {
        this(System.currentTimeMillis(), new SystemInfo());
    }
    
    /**
     * 构造函数
     *
     * @param startupTime 启动时间
     * @param systemInfo 系统信息
     */
    public SystemStartupEvent(long startupTime, SystemInfo systemInfo) {
        super(TOPIC, TYPE, "system");
        this.startupTime = startupTime;
        this.systemInfo = systemInfo;
    }
    
    /**
     * 获取启动时间
     *
     * @return 启动时间
     */
    public long getStartupTime() {
        return startupTime;
    }
    
    /**
     * 获取系统信息
     *
     * @return 系统信息
     */
    public SystemInfo getSystemInfo() {
        return systemInfo;
    }
    
    /**
     * 系统信息类
     * 包含系统版本、运行环境等信息
     */
    public static class SystemInfo {
        private final String version;
        private final String javaVersion;
        private final String osName;
        private final String osVersion;
        private final long maxMemory;
        private final int processorCount;
        
        /**
         * 构造函数
         */
        public SystemInfo() {
            this.version = getSystemVersion();
            this.javaVersion = System.getProperty("java.version");
            this.osName = System.getProperty("os.name");
            this.osVersion = System.getProperty("os.version");
            this.maxMemory = Runtime.getRuntime().maxMemory();
            this.processorCount = Runtime.getRuntime().availableProcessors();
        }
        
        /**
         * 获取系统版本
         *
         * @return 系统版本
         */
        private String getSystemVersion() {
            // 这里可以从配置文件或者其他地方获取系统版本
            return "1.0.0";
        }
        
        public String getVersion() {
            return version;
        }
        
        public String getJavaVersion() {
            return javaVersion;
        }
        
        public String getOsName() {
            return osName;
        }
        
        public String getOsVersion() {
            return osVersion;
        }
        
        public long getMaxMemory() {
            return maxMemory;
        }
        
        public int getProcessorCount() {
            return processorCount;
        }
        
        @Override
        public String toString() {
            return "SystemInfo{" +
                    "version='" + version + '\'' +
                    ", javaVersion='" + javaVersion + '\'' +
                    ", osName='" + osName + '\'' +
                    ", osVersion='" + osVersion + '\'' +
                    ", maxMemory=" + maxMemory +
                    ", processorCount=" + processorCount +
                    '}';
        }
    }
} 