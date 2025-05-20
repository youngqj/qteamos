package com.xiaoqu.qteamos.core.plugin.event.plugins;

import com.xiaoqu.qteamos.core.plugin.event.AbstractEvent;

/**
 * 系统关闭事件
 * 在系统即将关闭前发布，允许插件进行资源释放和状态保存
 *
 * @author yangqijun
 * @date 2024-07-03
 */
public class SystemShutdownEvent extends AbstractEvent {
    
    /**
     * 系统事件主题
     */
    public static final String TOPIC = "system";
    
    /**
     * 系统关闭事件类型
     */
    public static final String TYPE = "shutdown";
    
    /**
     * 关闭原因
     */
    private final ShutdownReason reason;
    
    /**
     * 剩余时间（毫秒）
     * 指示插件在系统强制关闭前还有多少时间进行清理工作
     */
    private final long remainingTime;
    
    /**
     * 构造函数
     *
     * @param reason 关闭原因
     */
    public SystemShutdownEvent(ShutdownReason reason) {
        this(reason, 5000);
    }
    
    /**
     * 构造函数
     *
     * @param reason 关闭原因
     * @param remainingTime 剩余时间（毫秒）
     */
    public SystemShutdownEvent(ShutdownReason reason, long remainingTime) {
        super(TOPIC, TYPE, "system");
        this.reason = reason;
        this.remainingTime = remainingTime;
    }
    
    /**
     * 获取关闭原因
     *
     * @return 关闭原因
     */
    public ShutdownReason getReason() {
        return reason;
    }
    
    /**
     * 获取剩余时间
     *
     * @return 剩余时间（毫秒）
     */
    public long getRemainingTime() {
        return remainingTime;
    }
    
    /**
     * 关闭原因枚举
     */
    public enum ShutdownReason {
        /**
         * 正常关闭
         */
        NORMAL("正常关闭"),
        
        /**
         * 应用升级
         */
        UPGRADE("应用升级"),
        
        /**
         * 系统错误
         */
        ERROR("系统错误"),
        
        /**
         * 外部信号
         */
        SIGNAL("外部信号"),
        
        /**
         * 超时关闭
         */
        TIMEOUT("超时关闭"),
        
        /**
         * 用户请求
         */
        USER_REQUEST("用户请求"),
        
        /**
         * 未知原因
         */
        UNKNOWN("未知原因");
        
        private final String description;
        
        ShutdownReason(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
} 