package com.xiaoqu.qteamos.core.plugin.config;

import com.xiaoqu.qteamos.core.plugin.manager.EnhancedDependencyResolver;
import com.xiaoqu.qteamos.core.plugin.manager.PluginRegistry;
import com.xiaoqu.qteamos.core.plugin.manager.EnhancedPluginVersionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;

/**
 * 插件依赖配置类
 * 配置增强版依赖解析器及其相关功能
 *
 * @author yangqijun
 * @date 2024-07-18
 */
@Configuration
public class PluginDependencyConfig {
    private static final Logger log = LoggerFactory.getLogger(PluginDependencyConfig.class);
    
    /**
     * 依赖解析策略配置
     */
    @Value("${plugin.dependency.resolution-strategy:NEWEST}")
    private String resolutionStrategy;
    
    /**
     * 是否启用增强版依赖解析
     */
    @Value("${plugin.dependency.enhanced-resolver.enabled:true}")
    private boolean enhancedResolverEnabled;
    
    /**
     * 配置增强版依赖解析器
     *
     * @param pluginRegistry 插件注册表
     * @param versionManager 版本管理器
     * @return 增强版依赖解析器
     */
    @Bean
    @ConditionalOnProperty(
            name = "plugin.dependency.enhanced-resolver.enabled", 
            havingValue = "true", 
            matchIfMissing = true)
    public EnhancedDependencyResolver enhancedDependencyResolver(
            PluginRegistry pluginRegistry,
            EnhancedPluginVersionManager versionManager) {
        
        log.info("初始化增强版插件依赖解析器...");
        EnhancedDependencyResolver resolver = new EnhancedDependencyResolver();
        
        // 设置默认解析策略
        try {
            EnhancedDependencyResolver.ResolutionStrategy strategy = 
                    EnhancedDependencyResolver.ResolutionStrategy.valueOf(resolutionStrategy);
            resolver.setDefaultStrategy(strategy);
            log.info("设置依赖解析策略: {}", strategy);
        } catch (IllegalArgumentException e) {
            log.warn("无效的依赖解析策略: {}, 使用默认策略: NEWEST", resolutionStrategy);
            resolver.setDefaultStrategy(EnhancedDependencyResolver.ResolutionStrategy.NEWEST);
        }
        
        return resolver;
    }
} 