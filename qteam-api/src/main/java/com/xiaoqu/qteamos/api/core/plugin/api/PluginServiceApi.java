package com.xiaoqu.qteamos.api.core.plugin.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 插件服务API接口
 * 提供标准化的接口供插件使用
 *
 * @author yangqijun
 * @date 2025-05-01
 */
public interface PluginServiceApi {

    /**
     * 获取当前插件ID
     *
     * @return 插件ID
     */
    String getPluginId();

    /**
     * 获取当前插件版本
     *
     * @return 插件版本
     */
    String getPluginVersion();

    /**
     * 获取数据服务API
     *
     * @return 数据服务API
     */
    DataServiceApi getDataService();

    /**
     * 获取配置服务API
     *
     * @return 配置服务API
     */
    ConfigServiceApi getConfigService();

    /**
     * 获取存储服务API
     *
     * @return 存储服务API
     */
    StorageServiceApi getStorageService();

    /**
     * 获取日志服务API
     *
     * @return 日志服务API
     */
    LogServiceApi getLogService();

    /**
     * 获取事件服务API
     *
     * @return 事件服务API
     */
    EventServiceApi getEventService();

    /**
     * 获取UI服务API
     *
     * @return UI服务API
     */
    UiServiceApi getUiService();

    /**
     * 获取扩展点服务API
     *
     * @return 扩展点服务API
     */
    ExtensionServiceApi getExtensionService();

    /**
     * 获取系统信息
     *
     * @return 系统信息
     */
    SystemInfo getSystemInfo();

    /**
     * 获取可用的插件列表
     *
     * @return 插件列表
     */
    List<PluginInfo> getAvailablePlugins();

    /**
     * 调用其他插件提供的服务
     *
     * @param pluginId 插件ID
     * @param serviceName 服务名称
     * @param params 调用参数
     * @return 调用结果
     */
    <T> Optional<T> invokePluginService(String pluginId, String serviceName, Map<String, Object> params);
} 