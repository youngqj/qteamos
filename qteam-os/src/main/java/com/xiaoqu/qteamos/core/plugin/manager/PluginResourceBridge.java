package com.xiaoqu.qteamos.core.plugin.manager;


import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import com.xiaoqu.qteamos.api.core.plugin.PluginContext;
import com.xiaoqu.qteamos.api.core.PluginResourceProvider;
import com.xiaoqu.qteamos.api.core.plugin.PluginEventListener;
import com.xiaoqu.qteamos.api.core.cache.CacheService;
import com.xiaoqu.qteamos.api.core.config.ConfigService;
import com.xiaoqu.qteamos.api.core.datasource.DataSourceService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.File;

import com.xiaoqu.qteamos.core.plugin.service.PluginServiceApiImpl;

/**
 * 插件资源桥接器
 * 负责管理插件和主应用之间的资源共享，提供资源代理和访问控制
 *
 * @author yangqijun
 * @date 2024-07-02
 */
@Component
public class PluginResourceBridge implements ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(PluginResourceBridge.class);
    
    private ApplicationContext applicationContext;
    
    @Autowired
    private PluginServiceApiImpl pluginServiceApi;
    
    // 缓存共享资源实例
    private final Map<String, Object> sharedResources = new HashMap<>();
    
    /**
     * 创建插件上下文
     * 
     * @param pluginInfo 插件信息
     * @return 插件上下文
     */
    public PluginContext createPluginContext(PluginInfo pluginInfo) {
        DefaultPluginContext context = new DefaultPluginContext(pluginInfo);
        context.setResourceProvider(createResourceProvider());
        
        // 设置当前插件ID
        String pluginId = pluginInfo.getPluginId();
        pluginServiceApi.setCurrentPluginId(pluginId);
        
        return context;
    }
    
    /**
     * 创建资源提供者
     * 
     * @return 资源提供者
     */
    private PluginResourceProvider createResourceProvider() {
        return new DefaultPluginResourceProvider();
    }
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    /**
     * 默认插件上下文实现
     */
    private static class DefaultPluginContext implements PluginContext {
        private static final Logger log = LoggerFactory.getLogger(DefaultPluginContext.class);
        
        private final PluginInfo pluginInfo;
        private PluginResourceProvider resourceProvider;
        private final Map<String, String> configs = new ConcurrentHashMap<>();
        private File dataFolder;
        
        public DefaultPluginContext(PluginInfo pluginInfo) {
            this.pluginInfo = pluginInfo;
            
            // 创建数据文件夹，改用jarPath防止空指针
            if (pluginInfo.getJarPath() != null) {
                File jarFile = pluginInfo.getJarPath().toFile();
                File pluginDir = jarFile.getParentFile();
                this.dataFolder = new File(pluginDir, "data");
            } else if (pluginInfo.getPluginFile() != null && pluginInfo.getPluginFile().getParentFile() != null) {
                // 兼容旧方式
                this.dataFolder = new File(pluginInfo.getPluginFile().getParentFile(), "data");
            } else {
                // 无法确定路径时，使用默认目录
                String pluginId = pluginInfo.getDescriptor().getPluginId();
                this.dataFolder = new File("./plugins/" + pluginId + "/data");
                log.warn("插件[{}]未指定JAR路径或插件文件，使用默认数据目录: {}", 
                        pluginId, this.dataFolder.getAbsolutePath());
            }
            
            // 确保数据目录存在
            if (!this.dataFolder.exists()) {
                this.dataFolder.mkdirs();
            }
        }
        
        public void setResourceProvider(PluginResourceProvider resourceProvider) {
            this.resourceProvider = resourceProvider;
        }
        
        @Override
        public String getPluginId() {
            return pluginInfo.getDescriptor().getPluginId();
        }
        
        @Override
        public String getPluginVersion() {
            return pluginInfo.getVersion();
        }
        
        @Override
        public String getConfig(String key) {
            return configs.get(key);
        }

        @Override
        public String getConfig(String key, String defaultValue) {
            return configs.getOrDefault(key, defaultValue);
        }

        @Override
        public Map<String, String> getAllConfigs() {
            return new HashMap<>(configs);
        }

        @Override
        public void setConfig(String key, String value) {
            configs.put(key, value);
        }

        @Override
        public String getDataFolderPath() {
            return dataFolder.getAbsolutePath();
        }

        @Override
        public CacheService getCacheService() {
            return getService(CacheService.class);
        }

        @Override
        public DataSourceService getDataSourceService() {
            return getService(DataSourceService.class);
        }

        @Override
        public ConfigService getConfigService() {
            return getService(ConfigService.class);
        }

        @Override
        public <T> T getService(Class<T> serviceClass) {
            return resourceProvider.getResource(serviceClass);
        }

        @Override
        public void publishEvent(Object event) {
            // 这里应该实现事件发布逻辑
            // 为简洁起见，留空实现
        }

        @Override
        public <T> void addEventListener(Class<T> eventType, PluginEventListener<T> listener) {
            // 这里应该实现事件监听逻辑
            // 为简洁起见，留空实现
        }

        @Override
        public <T> void removeEventListener(Class<T> eventType, PluginEventListener<T> listener) {
            // 这里应该实现事件监听移除逻辑
            // 为简洁起见，留空实现
        }
    }
    
    /**
     * 默认资源提供者实现
     */
    private class DefaultPluginResourceProvider implements PluginResourceProvider {
        @Override
        public DataSource getDataSource() {
            try {
                // 从Spring上下文获取数据源
                return getSharedResource("dataSource", DataSource.class);
            } catch (Exception e) {
                log.error("无法获取数据源", e);
                return null;
            }
        }
        
        @Override
        public Object getResource(String name) {
            try {
                // 从Spring上下文获取资源
                return applicationContext.getBean(name);
            } catch (Exception e) {
                log.warn("无法获取资源: {}", name);
                return null;
            }
        }
        
        @Override
        public <T> T getResource(String name, Class<T> type) {
            try {
                // 从Spring上下文获取资源
                return applicationContext.getBean(name, type);
            } catch (Exception e) {
                log.warn("无法获取资源: {}, 类型: {}", name, type.getName());
                return null;
            }
        }
        
        @Override
        public <T> T getResource(Class<T> type) {
            try {
                // 从Spring上下文获取资源
                return applicationContext.getBean(type);
            } catch (Exception e) {
                log.warn("无法获取资源类型: {}", type.getName());
                return null;
            }
        }
    }
    
    /**
     * 获取共享资源，如果不存在则从应用上下文中获取并缓存
     * 
     * @param name 资源名称
     * @param type 资源类型
     * @param <T> 资源类型
     * @return 共享资源
     */
    @SuppressWarnings("unchecked")
    private <T> T getSharedResource(String name, Class<T> type) {
        if (sharedResources.containsKey(name)) {
            return (T) sharedResources.get(name);
        }
        
        try {
            T resource = applicationContext.getBean(name, type);
            if (resource != null) {
                // 缓存资源
                sharedResources.put(name, resource);
                log.info("成功缓存共享资源: {}", name);
            }
            return resource;
        } catch (Exception e) {
            log.error("获取共享资源失败: " + name, e);
            return null;
        }
    }
    
    /**
     * 清理插件上下文
     *
     * @param pluginId 插件ID
     */
    public void cleanupPluginContext(String pluginId) {
        // 清除当前插件ID
        pluginServiceApi.clearCurrentPluginId();
    }
} 