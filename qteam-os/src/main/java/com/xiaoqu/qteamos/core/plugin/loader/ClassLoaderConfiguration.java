package com.xiaoqu.qteamos.core.plugin.loader;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 类加载器配置类
 * 用于配置插件类加载器的行为
 * 
 * @author yangqijun
 * @version 1.1.0
 */
@Getter
@Setter
@NoArgsConstructor
public class ClassLoaderConfiguration {
    
    /**
     * 类加载策略
     */
    private ClassLoadingStrategy strategy = ClassLoadingStrategy.PARENT_FIRST;
    
    /**
     * 隔离的包前缀列表
     * 这些包会优先从插件类加载器中加载，不遵循双亲委派模型
     */
    private Set<String> isolatedPackages = new HashSet<>();
    
    /**
     * 隔离的包正则表达式列表
     * 支持更灵活的包隔离配置
     */
    private Set<Pattern> isolatedPatterns = new HashSet<>();
    
    /**
     * 共享的包前缀列表
     * 这些包会优先从父类加载器中加载，即使策略是CHILD_FIRST
     */
    private Set<String> sharedPackages = new HashSet<>();
    
    /**
     * 共享的包正则表达式列表
     * 支持更灵活的包共享配置
     */
    private Set<Pattern> sharedPatterns = new HashSet<>();
    
    /**
     * 禁止的包前缀列表
     * 这些包不允许从插件中加载，防止安全风险
     */
    private Set<String> blockedPackages = new HashSet<>();
    
    /**
     * 禁止的包正则表达式列表
     * 支持更灵活的包禁止配置
     */
    private Set<Pattern> blockedPatterns = new HashSet<>();
    
    /**
     * 包优先级配置
     * 当出现包冲突时，用于决定加载优先级
     * 优先级越高，越优先加载
     */
    private Map<String, Integer> packagePriorities = new HashMap<>();
    
    /**
     * 资源共享模式
     * 如果为true，资源将优先从父类加载器中加载
     */
    private boolean resourceSharingEnabled = true;
    
    /**
     * 是否启用内存泄漏保护
     */
    private boolean memoryLeakProtectionEnabled = true;
    
    /**
     * 类加载缓存配置
     */
    private boolean classCachingEnabled = true;
    
    /**
     * 是否启用类冲突检测
     */
    private boolean classConflictDetectionEnabled = true;
    
    /**
     * 类加载决策缓存，提高性能
     */
    private final Map<String, Boolean> isolationDecisionCache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> sharedDecisionCache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> blockDecisionCache = new ConcurrentHashMap<>();
    
    /**
     * 构建器模式创建配置
     */
    @Builder
    public ClassLoaderConfiguration(
            ClassLoadingStrategy strategy,
            Set<String> isolatedPackages,
            Set<Pattern> isolatedPatterns,
            Set<String> sharedPackages,
            Set<Pattern> sharedPatterns,
            Set<String> blockedPackages,
            Set<Pattern> blockedPatterns,
            Map<String, Integer> packagePriorities,
            boolean resourceSharingEnabled,
            boolean memoryLeakProtectionEnabled,
            boolean classCachingEnabled,
            boolean classConflictDetectionEnabled) {
        
        this.strategy = strategy != null ? strategy : ClassLoadingStrategy.PARENT_FIRST;
        
        if (isolatedPackages != null) {
            this.isolatedPackages = isolatedPackages;
        }
        
        if (isolatedPatterns != null) {
            this.isolatedPatterns = isolatedPatterns;
        }
        
        if (sharedPackages != null) {
            this.sharedPackages = sharedPackages;
        }
        
        if (sharedPatterns != null) {
            this.sharedPatterns = sharedPatterns;
        }
        
        if (blockedPackages != null) {
            this.blockedPackages = blockedPackages;
        }
        
        if (blockedPatterns != null) {
            this.blockedPatterns = blockedPatterns;
        }
        
        if (packagePriorities != null) {
            this.packagePriorities = packagePriorities;
        }
        
        this.resourceSharingEnabled = resourceSharingEnabled;
        this.memoryLeakProtectionEnabled = memoryLeakProtectionEnabled;
        this.classCachingEnabled = classCachingEnabled;
        this.classConflictDetectionEnabled = classConflictDetectionEnabled;
    }
    
    /**
     * 添加隔离的包
     * 
     * @param packageName 包名
     * @return 当前配置实例
     */
    public ClassLoaderConfiguration addIsolatedPackage(String packageName) {
        this.isolatedPackages.add(packageName);
        // 清除相关缓存
        isolationDecisionCache.clear();
        return this;
    }
    
    /**
     * 添加隔离的包正则表达式
     * 
     * @param pattern 正则表达式模式
     * @return 当前配置实例
     */
    public ClassLoaderConfiguration addIsolatedPattern(String pattern) {
        this.isolatedPatterns.add(Pattern.compile(pattern));
        // 清除相关缓存
        isolationDecisionCache.clear();
        return this;
    }
    
    /**
     * 添加共享的包
     * 
     * @param packageName 包名
     * @return 当前配置实例
     */
    public ClassLoaderConfiguration addSharedPackage(String packageName) {
        this.sharedPackages.add(packageName);
        // 清除相关缓存
        sharedDecisionCache.clear();
        return this;
    }
    
    /**
     * 添加共享的包正则表达式
     * 
     * @param pattern 正则表达式模式
     * @return 当前配置实例
     */
    public ClassLoaderConfiguration addSharedPattern(String pattern) {
        this.sharedPatterns.add(Pattern.compile(pattern));
        // 清除相关缓存
        sharedDecisionCache.clear();
        return this;
    }
    
    /**
     * 添加禁止的包
     * 
     * @param packageName 包名
     * @return 当前配置实例
     */
    public ClassLoaderConfiguration addBlockedPackage(String packageName) {
        this.blockedPackages.add(packageName);
        // 清除相关缓存
        blockDecisionCache.clear();
        return this;
    }
    
    /**
     * 添加禁止的包正则表达式
     * 
     * @param pattern 正则表达式模式
     * @return 当前配置实例
     */
    public ClassLoaderConfiguration addBlockedPattern(String pattern) {
        this.blockedPatterns.add(Pattern.compile(pattern));
        // 清除相关缓存
        blockDecisionCache.clear();
        return this;
    }
    
    /**
     * 设置包的优先级
     * 
     * @param packageName 包名
     * @param priority 优先级(数值越大优先级越高)
     * @return 当前配置实例
     */
    public ClassLoaderConfiguration setPackagePriority(String packageName, int priority) {
        this.packagePriorities.put(packageName, priority);
        return this;
    }
    
    /**
     * 判断是否允许加载指定包的类
     * 
     * @param className 类名
     * @return 是否允许加载
     */
    public boolean isAllowedToLoad(String className) {
        // 检查缓存
        Boolean cached = blockDecisionCache.get(className);
        if (cached != null) {
            return !cached;
        }
        
        // 检查是否在禁止列表中
        for (String blockedPackage : blockedPackages) {
            if (className.startsWith(blockedPackage)) {
                blockDecisionCache.put(className, true);
                return false;
            }
        }
        
        // 检查正则表达式匹配
        for (Pattern pattern : blockedPatterns) {
            if (pattern.matcher(className).matches()) {
                blockDecisionCache.put(className, true);
                return false;
            }
        }
        
        blockDecisionCache.put(className, false);
        return true;
    }
    
    /**
     * 判断指定包是否应该隔离
     * 
     * @param className 类名
     * @return 是否应该隔离
     */
    public boolean isIsolated(String className) {
        // 检查缓存
        Boolean cached = isolationDecisionCache.get(className);
        if (cached != null) {
            return cached;
        }
        
        // 检查前缀匹配
        for (String isolatedPackage : isolatedPackages) {
            if (className.startsWith(isolatedPackage)) {
                isolationDecisionCache.put(className, true);
                return true;
            }
        }
        
        // 检查正则表达式匹配
        for (Pattern pattern : isolatedPatterns) {
            if (pattern.matcher(className).matches()) {
                isolationDecisionCache.put(className, true);
                return true;
            }
        }
        
        isolationDecisionCache.put(className, false);
        return false;
    }
    
    /**
     * 判断指定包是否应该共享
     * 
     * @param className 类名
     * @return 是否应该共享
     */
    public boolean isShared(String className) {
        // 检查缓存
        Boolean cached = sharedDecisionCache.get(className);
        if (cached != null) {
            return cached;
        }
        
        // 检查前缀匹配
        for (String sharedPackage : sharedPackages) {
            if (className.startsWith(sharedPackage)) {
                sharedDecisionCache.put(className, true);
                return true;
            }
        }
        
        // 检查正则表达式匹配
        for (Pattern pattern : sharedPatterns) {
            if (pattern.matcher(className).matches()) {
                sharedDecisionCache.put(className, true);
                return true;
            }
        }
        
        sharedDecisionCache.put(className, false);
        return false;
    }
    
    /**
     * 获取包的优先级
     * 
     * @param packageName 包名
     * @return 优先级，默认为0
     */
    public int getPackagePriority(String packageName) {
        for (Map.Entry<String, Integer> entry : packagePriorities.entrySet()) {
            if (packageName.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return 0;
    }
    
    /**
     * 清除缓存
     */
    public void clearCache() {
        isolationDecisionCache.clear();
        sharedDecisionCache.clear();
        blockDecisionCache.clear();
    }
} 