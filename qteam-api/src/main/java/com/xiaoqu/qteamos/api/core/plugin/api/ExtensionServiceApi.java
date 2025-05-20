package com.xiaoqu.qteamos.api.core.plugin.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 扩展点服务API接口
 * 提供插件扩展点注册和调用能力
 *
 * @author yangqijun
 * @date 2025-05-01
 */
public interface ExtensionServiceApi {

    /**
     * 注册扩展点实现
     *
     * @param extensionPoint 扩展点ID
     * @param implementation 扩展点实现
     * @return 是否注册成功
     */
    boolean registerExtension(String extensionPoint, Object implementation);

    /**
     * 注册扩展点实现（带优先级）
     *
     * @param extensionPoint 扩展点ID
     * @param implementation 扩展点实现
     * @param priority 优先级（值越大优先级越高）
     * @return 是否注册成功
     */
    boolean registerExtension(String extensionPoint, Object implementation, int priority);

    /**
     * 注销扩展点实现
     *
     * @param extensionPoint 扩展点ID
     * @param implementation 扩展点实现
     * @return 是否注销成功
     */
    boolean unregisterExtension(String extensionPoint, Object implementation);

    /**
     * 获取扩展点的所有实现
     *
     * @param extensionPoint 扩展点ID
     * @return 扩展点实现列表
     */
    <T> List<T> getExtensions(String extensionPoint);

    /**
     * 获取扩展点的最高优先级实现
     *
     * @param extensionPoint 扩展点ID
     * @return 扩展点实现
     */
    <T> Optional<T> getHighestPriorityExtension(String extensionPoint);

    /**
     * 判断扩展点是否存在
     *
     * @param extensionPoint 扩展点ID
     * @return 是否存在
     */
    boolean hasExtension(String extensionPoint);

    /**
     * 获取扩展点元数据
     *
     * @param extensionPoint 扩展点ID
     * @return 元数据
     */
    Map<String, Object> getExtensionMetadata(String extensionPoint);

    /**
     * 获取所有可用的扩展点
     *
     * @return 扩展点ID列表
     */
    List<String> getAllExtensionPoints();
} 