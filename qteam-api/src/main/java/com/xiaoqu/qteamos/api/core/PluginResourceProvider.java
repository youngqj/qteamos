package com.xiaoqu.qteamos.api.core;

import javax.sql.DataSource;

/**
 * 插件资源提供者接口
 * 定义插件获取主应用资源的标准方法
 *
 * @author yangqijun
 * @date 2024-07-02
 */
public interface PluginResourceProvider {

    /**
     * 获取数据源
     *
     * @return 数据源
     */
    DataSource getDataSource();

    /**
     * 获取指定名称的资源
     *
     * @param name 资源名称
     * @return 资源对象
     */
    Object getResource(String name);

    /**
     * 获取指定名称和类型的资源
     *
     * @param name 资源名称
     * @param type 资源类型
     * @param <T> 资源类型
     * @return 资源对象
     */
    <T> T getResource(String name, Class<T> type);

    /**
     * 获取指定类型的资源
     *
     * @param type 资源类型
     * @param <T> 资源类型
     * @return 资源对象
     */
    <T> T getResource(Class<T> type);
} 