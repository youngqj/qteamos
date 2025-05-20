package com.xiaoqu.qteamos.api.core.plugin.api;

/**
 * 插件API类
 * 为插件提供统一的API入口
 *
 * @author yangqijun
 * @date 2025-05-01
 */
public final class PluginApi {
    
    /**
     * 唯一实例
     */
    private static PluginServiceApi INSTANCE;
    
    /**
     * 私有构造方法，防止实例化
     */
    private PluginApi() {
        throw new UnsupportedOperationException("PluginApi类不能实例化");
    }
    
    /**
     * 初始化API实例
     *
     * @param instance API实例
     */
    public static void init(PluginServiceApi instance) {
        if (INSTANCE != null) {
            throw new IllegalStateException("PluginApi已经初始化");
        }
        INSTANCE = instance;
    }
    
    /**
     * 获取API实例
     *
     * @return API实例
     */
    public static PluginServiceApi get() {
        if (INSTANCE == null) {
            throw new IllegalStateException("PluginApi尚未初始化");
        }
        return INSTANCE;
    }
    
    /**
     * 清理API实例
     */
    public static void cleanup() {
        INSTANCE = null;
    }
} 