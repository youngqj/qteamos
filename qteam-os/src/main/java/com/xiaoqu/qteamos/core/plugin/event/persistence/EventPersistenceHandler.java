package com.xiaoqu.qteamos.core.plugin.event.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoqu.qteamos.core.plugin.event.Event;
import com.xiaoqu.qteamos.core.plugin.event.EventHandler;
import com.xiaoqu.qteamos.core.plugin.event.EventListener;
import com.xiaoqu.qteamos.core.plugin.event.PluginEvent;
import com.xiaoqu.qteamos.core.plugin.event.plugins.SystemShutdownEvent;
import com.xiaoqu.qteamos.core.plugin.event.plugins.SystemStartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 事件持久化处理器
 * 用于将关键事件记录到数据库中
 *
 * @author yangqijun
 * @date 2024-07-03
 */
@Component
public class EventPersistenceHandler implements EventHandler {
    private static final Logger log = LoggerFactory.getLogger(EventPersistenceHandler.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private static final String INSERT_EVENT_SQL = 
            "INSERT INTO sys_plugin_event_log (event_id, topic, type, source, target, data, timestamp) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
    
    /**
     * 持久化系统启动事件
     */
    @EventListener(topics = "system", types = "startup")
    public boolean persistSystemStartupEvent(SystemStartupEvent event) {
        try {
            persistEvent(event, null, objectMapper.writeValueAsString(event.getSystemInfo()));
            log.info("系统启动事件已持久化");
            return true;
        } catch (Exception e) {
            log.error("持久化系统启动事件失败", e);
            return true; // 持久化失败不影响事件传播
        }
    }
    
    /**
     * 持久化系统关闭事件
     */
    @EventListener(topics = "system", types = "shutdown")
    public boolean persistSystemShutdownEvent(SystemShutdownEvent event) {
        try {
            persistEvent(event, null, objectMapper.writeValueAsString(
                    new ShutdownInfo(event.getReason().name(), event.getReason().getDescription(), event.getRemainingTime())
            ));
            log.info("系统关闭事件已持久化");
            return true;
        } catch (Exception e) {
            log.error("持久化系统关闭事件失败", e);
            return true;
        }
    }
    
    /**
     * 持久化插件事件
     */
    @EventListener(topics = "plugin")
    public boolean persistPluginEvent(PluginEvent event) {
        try {
            // 只持久化关键事件
            switch (event.getType()) {
                case PluginEvent.TYPE_LOADED:
                case PluginEvent.TYPE_STARTED:
                case PluginEvent.TYPE_STOPPED:
                case PluginEvent.TYPE_UNLOADED:
                case PluginEvent.TYPE_ERROR:
                case PluginEvent.TYPE_DEPENDENCY_FAILED:
                    persistEvent(event, event.getPluginId(), 
                            objectMapper.writeValueAsString(
                                    new PluginEventInfo(event.getPluginId(), event.getVersion(), event.getData())
                            ));
                    log.debug("插件事件已持久化: {}", event);
                    break;
                default:
                    // 不持久化其他类型的事件
                    break;
            }
            return true;
        } catch (Exception e) {
            log.error("持久化插件事件失败: " + event, e);
            return true;
        }
    }
    
    /**
     * 持久化事件到数据库
     */
    private void persistEvent(Event event, String target, String data) {
        String eventId = UUID.randomUUID().toString();
        jdbcTemplate.update(INSERT_EVENT_SQL,
                eventId,
                event.getTopic(),
                event.getType(),
                event.getSource(),
                target,
                data,
                event.getTimestamp()
        );
    }
    
    @Override
    public boolean handle(Event event) {
        // 这个方法不会被直接调用，因为我们使用了@EventListener注解
        // 但是需要实现接口方法
        return true;
    }
    
    @Override
    public String[] getTopics() {
        return new String[]{"system", "plugin"};
    }
    
    @Override
    public String[] getTypes() {
        return new String[]{"*"};
    }
    
    /**
     * 系统关闭信息
     */
    private static class ShutdownInfo {
        private final String reason;
        private final String description;
        private final long remainingTime;
        
        public ShutdownInfo(String reason, String description, long remainingTime) {
            this.reason = reason;
            this.description = description;
            this.remainingTime = remainingTime;
        }
        
        public String getReason() {
            return reason;
        }
        
        public String getDescription() {
            return description;
        }
        
        public long getRemainingTime() {
            return remainingTime;
        }
    }
    
    /**
     * 插件事件信息
     */
    private static class PluginEventInfo {
        private final String pluginId;
        private final String version;
        private final Object data;
        
        public PluginEventInfo(String pluginId, String version, Object data) {
            this.pluginId = pluginId;
            this.version = version;
            this.data = data;
        }
        
        public String getPluginId() {
            return pluginId;
        }
        
        public String getVersion() {
            return version;
        }
        
        public Object getData() {
            return data;
        }
    }
} 