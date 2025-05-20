package com.xiaoqu.qteamos.core.plugin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoqu.qteamos.core.plugin.error.PluginErrorHandler;
import com.xiaoqu.qteamos.api.core.plugin.api.ConfigServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置服务API实现类
 *
 * @author yangqijun
 * @date 2025-05-01
 */
@Component
public class ConfigServiceApiImpl implements ConfigServiceApi {

    private static final Logger log = LoggerFactory.getLogger(ConfigServiceApiImpl.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 插件配置缓存
     * key：插件ID，value：配置Map
     */
    private final Map<String, Map<String, Object>> configCache = new ConcurrentHashMap<>();

    @Autowired
    private PluginErrorHandler errorHandler;

    @Autowired
    private PluginServiceApiImpl pluginServiceApi;

    @Autowired
    private StorageServiceApiImpl storageService;

    @Override
    public Optional<String> getString(String key) {
        try {
            Object value = getConfigValue(key);
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(value.toString());
        } catch (Exception e) {
            handleError(e, "获取字符串配置异常: " + key);
            return Optional.empty();
        }
    }

    @Override
    public String getString(String key, String defaultValue) {
        return getString(key).orElse(defaultValue);
    }

    @Override
    public Optional<Integer> getInt(String key) {
        try {
            Object value = getConfigValue(key);
            if (value == null) {
                return Optional.empty();
            }
            if (value instanceof Number) {
                return Optional.of(((Number) value).intValue());
            }
            return Optional.of(Integer.parseInt(value.toString()));
        } catch (Exception e) {
            handleError(e, "获取整型配置异常: " + key);
            return Optional.empty();
        }
    }

    @Override
    public int getInt(String key, int defaultValue) {
        return getInt(key).orElse(defaultValue);
    }

    @Override
    public Optional<Boolean> getBoolean(String key) {
        try {
            Object value = getConfigValue(key);
            if (value == null) {
                return Optional.empty();
            }
            if (value instanceof Boolean) {
                return Optional.of((Boolean) value);
            }
            return Optional.of(Boolean.parseBoolean(value.toString()));
        } catch (Exception e) {
            handleError(e, "获取布尔配置异常: " + key);
            return Optional.empty();
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return getBoolean(key).orElse(defaultValue);
    }

    @Override
    public <T> Optional<List<T>> getList(String key, Class<T> elementType) {
        try {
            Object value = getConfigValue(key);
            if (value == null) {
                return Optional.empty();
            }
            
            if (value instanceof List) {
                List<?> list = (List<?>) value;
                List<T> result = new ArrayList<>();
                
                for (Object item : list) {
                    if (elementType.isInstance(item)) {
                        result.add(elementType.cast(item));
                    } else {
                        // 尝试转换
                        T converted = objectMapper.convertValue(item, elementType);
                        result.add(converted);
                    }
                }
                
                return Optional.of(result);
            }
            
            return Optional.empty();
        } catch (Exception e) {
            handleError(e, "获取列表配置异常: " + key);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Map<String, Object>> getMap(String key) {
        try {
            Object value = getConfigValue(key);
            if (value == null) {
                return Optional.empty();
            }
            
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) value;
                return Optional.of(map);
            }
            
            // 尝试将值转换为Map
            Map<String, Object> map = objectMapper.convertValue(value, 
                    new TypeReference<Map<String, Object>>() {});
            return Optional.of(map);
        } catch (Exception e) {
            handleError(e, "获取Map配置异常: " + key);
            return Optional.empty();
        }
    }

    @Override
    public boolean set(String key, Object value) {
        try {
            String pluginId = getCurrentPluginId();
            Map<String, Object> config = getPluginConfig(pluginId);
            config.put(key, value);
            return true;
        } catch (Exception e) {
            handleError(e, "设置配置异常: " + key);
            return false;
        }
    }

    @Override
    public boolean remove(String key) {
        try {
            String pluginId = getCurrentPluginId();
            Map<String, Object> config = getPluginConfig(pluginId);
            config.remove(key);
            return true;
        } catch (Exception e) {
            handleError(e, "删除配置异常: " + key);
            return false;
        }
    }

    @Override
    public Map<String, Object> getAll() {
        try {
            String pluginId = getCurrentPluginId();
            return new HashMap<>(getPluginConfig(pluginId));
        } catch (Exception e) {
            handleError(e, "获取所有配置异常");
            return new HashMap<>();
        }
    }

    @Override
    public boolean save() {
        try {
            String pluginId = getCurrentPluginId();
            Map<String, Object> config = getPluginConfig(pluginId);
            
            // 获取配置文件
            File configFile = getConfigFile(pluginId);
            
            // 保存配置
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(configFile, config);
            
            log.debug("插件[{}]配置保存成功", pluginId);
            return true;
        } catch (Exception e) {
            handleError(e, "保存配置异常");
            return false;
        }
    }

    @Override
    public boolean reload() {
        try {
            String pluginId = getCurrentPluginId();
            configCache.remove(pluginId);
            getPluginConfig(pluginId); // 重新加载配置
            
            log.debug("插件[{}]配置重新加载成功", pluginId);
            return true;
        } catch (Exception e) {
            handleError(e, "重新加载配置异常");
            return false;
        }
    }

    /**
     * 获取配置值
     */
    private Object getConfigValue(String key) {
        String pluginId = getCurrentPluginId();
        Map<String, Object> config = getPluginConfig(pluginId);
        return config.get(key);
    }

    /**
     * 获取插件配置
     */
    private Map<String, Object> getPluginConfig(String pluginId) {
        Map<String, Object> config = configCache.get(pluginId);
        if (config == null) {
            config = loadPluginConfig(pluginId);
            configCache.put(pluginId, config);
        }
        return config;
    }

    /**
     * 加载插件配置
     */
    private Map<String, Object> loadPluginConfig(String pluginId) {
        File configFile = getConfigFile(pluginId);
        if (configFile.exists()) {
            try {
                return objectMapper.readValue(configFile, 
                        new TypeReference<Map<String, Object>>() {});
            } catch (IOException e) {
                log.error("加载插件配置异常: {}", pluginId, e);
            }
        }
        return new ConcurrentHashMap<>();
    }

    /**
     * 获取配置文件
     */
    private File getConfigFile(String pluginId) {
        File configDir = storageService.getPluginConfigDirectory(pluginId);
        return new File(configDir, "plugin-config.json");
    }

    /**
     * 获取当前插件ID
     */
    private String getCurrentPluginId() {
        return pluginServiceApi.getPluginId();
    }

    /**
     * 处理错误
     */
    private void handleError(Exception e, String message) {
        log.error(message, e);
        errorHandler.handlePluginError(getCurrentPluginId(), e, PluginErrorHandler.OperationType.RUNTIME);
    }
} 