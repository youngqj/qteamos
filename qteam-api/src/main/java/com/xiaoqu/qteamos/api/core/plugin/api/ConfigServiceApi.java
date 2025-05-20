package com.xiaoqu.qteamos.api.core.plugin.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 配置服务API接口
 * 提供插件配置管理能力
 *
 * @author yangqijun
 * @date 2025-05-01
 */
public interface ConfigServiceApi {

    /**
     * 获取字符串配置
     *
     * @param key 配置键
     * @return 配置值
     */
    Optional<String> getString(String key);

    /**
     * 获取字符串配置，带默认值
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    String getString(String key, String defaultValue);

    /**
     * 获取整型配置
     *
     * @param key 配置键
     * @return 配置值
     */
    Optional<Integer> getInt(String key);

    /**
     * 获取整型配置，带默认值
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    int getInt(String key, int defaultValue);

    /**
     * 获取布尔配置
     *
     * @param key 配置键
     * @return 配置值
     */
    Optional<Boolean> getBoolean(String key);

    /**
     * 获取布尔配置，带默认值
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    boolean getBoolean(String key, boolean defaultValue);

    /**
     * 获取列表配置
     *
     * @param key 配置键
     * @param elementType 元素类型
     * @param <T> 元素类型
     * @return 配置值
     */
    <T> Optional<List<T>> getList(String key, Class<T> elementType);

    /**
     * 获取Map配置
     *
     * @param key 配置键
     * @return 配置值
     */
    Optional<Map<String, Object>> getMap(String key);

    /**
     * 设置配置值
     *
     * @param key 配置键
     * @param value 配置值
     * @return 是否成功
     */
    boolean set(String key, Object value);

    /**
     * 删除配置
     *
     * @param key 配置键
     * @return 是否成功
     */
    boolean remove(String key);

    /**
     * 获取所有配置
     *
     * @return 所有配置
     */
    Map<String, Object> getAll();

    /**
     * 保存配置到存储
     *
     * @return 是否成功
     */
    boolean save();

    /**
     * 重新加载配置
     *
     * @return 是否成功
     */
    boolean reload();
} 