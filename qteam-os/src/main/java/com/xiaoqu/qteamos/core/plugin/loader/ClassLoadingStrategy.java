package com.xiaoqu.qteamos.core.plugin.loader;

/**
 * 类加载策略枚举
 * 定义插件类加载器的加载策略
 * 
 * @author yangqijun
 * @version 1.0.0
 */
public enum ClassLoadingStrategy {
    /**
     * 父加载器优先策略
     * 当加载类时，首先尝试由父加载器加载，如果父加载器无法加载才由插件类加载器加载
     * 这是Java类加载的默认策略，符合双亲委派模型
     */
    PARENT_FIRST,
    
    /**
     * 子加载器优先策略
     * 当加载类时，首先尝试由插件类加载器加载，如果无法加载才由父加载器加载
     * 这种策略允许插件覆盖系统提供的类，但可能导致类型冲突
     */
    CHILD_FIRST
} 