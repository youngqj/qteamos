package com.xiaoqu.qteamos.core.plugin.loader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态类加载器工厂
 * 用于创建和管理插件的类加载器
 * 
 * @author yangqijun
 * @version 1.0.0
 */
@Component
@Slf4j
public class DynamicClassLoaderFactory {
    
    /**
     * 类加载器缓存，避免为同一插件创建多个类加载器
     */
    private final Map<String, DynamicClassLoader> classLoaderCache = new ConcurrentHashMap<>();
    
    /**
     * 插件类加载器缓存，专门用于插件
     */
    private final Map<String, PluginClassLoader> pluginClassLoaderCache = new ConcurrentHashMap<>();
    
    /**
     * 默认的类加载器配置
     */
    private final ClassLoaderConfiguration defaultConfiguration;
    
    /**
     * 构造函数
     */
    public DynamicClassLoaderFactory() {
        this.defaultConfiguration = createDefaultConfiguration();
    }
    
    /**
     * 创建默认的类加载器配置
     * 
     * @return 默认配置
     */
    private ClassLoaderConfiguration createDefaultConfiguration() {
        ClassLoaderConfiguration config = new ClassLoaderConfiguration();
        
        // 使用父加载器优先策略，确保SDK和API类优先从父加载器加载
        config.setStrategy(ClassLoadingStrategy.PARENT_FIRST); 
        config.setResourceSharingEnabled(true);
        config.setMemoryLeakProtectionEnabled(true);
        
        // 只禁止JDK内部实现包，不禁止标准API包
        // 移除对java和javax包的阻止，这些应该由系统类加载器加载
        config.addBlockedPackage("sun.");
        config.addBlockedPackage("com.sun.");
        config.addBlockedPackage("org.xml.");
        
        // 添加默认的共享包，这些包应该由父加载器加载
        config.addSharedPackage("org.springframework.");
        config.addSharedPackage("org.slf4j.");
        config.addSharedPackage("ch.qos.logback.");
        
        // 明确添加SDK和API包作为共享包
        config.addSharedPackage("com.xiaoqu.qteamos.sdk.");
        config.addSharedPackage("com.xiaoqu.qteamos.api.");
        
        // 具体添加SDK和API的核心包，提高优先级
        config.addSharedPackage("com.xiaoqu.qteamos.sdk.plugin.");
        config.addSharedPackage("com.xiaoqu.qteamos.api.core.plugin.");
        
        // 支持Lombok等常用工具
        config.addSharedPackage("lombok.");
        
        log.info("创建默认类加载器配置: 共享包={}, 类加载策略={}", 
                 config.getSharedPackages(), config.getStrategy());
                
        return config;
    }
    
    /**
     * 为插件创建类加载器
     * 
     * @param pluginId 插件ID
     * @param pluginFile 插件文件
     * @return 动态类加载器
     * @throws IOException 如果创建失败
     */
    public DynamicClassLoader createClassLoader(String pluginId, File pluginFile) throws IOException {
        return createClassLoader(pluginId, pluginFile, null);
    }
    
    /**
     * 为插件创建类加载器，使用自定义配置
     * 
     * @param pluginId 插件ID
     * @param pluginFile 插件文件或目录
     * @param configuration 类加载器配置
     * @return 动态类加载器
     * @throws IOException 如果创建失败
     */
    public DynamicClassLoader createClassLoader(String pluginId, File pluginFile, 
                                               ClassLoaderConfiguration configuration) throws IOException {
        // 检查插件ID和文件是否有效
        if (pluginId == null || pluginId.isEmpty()) {
            throw new IllegalArgumentException("插件ID不能为空");
        }
        
        if (pluginFile == null || !pluginFile.exists()) {
            throw new IllegalArgumentException("插件文件不存在: " + 
                    (pluginFile != null ? pluginFile.getAbsolutePath() : "null"));
        }
        
        // 使用默认配置或自定义配置
        ClassLoaderConfiguration config = configuration != null ? 
                configuration : defaultConfiguration;
        
        // 创建类加载器
        DynamicClassLoader classLoader = new DynamicClassLoader(
                pluginId,
                new URL[0],
                getClass().getClassLoader(),
                config
        );
        
        try {
            // 根据文件类型添加资源
            if (pluginFile.isFile()) {
                // 添加JAR文件
                classLoader.addJarFile(pluginFile);
                log.info("为插件添加JAR文件: {}", pluginFile.getName());
            } else if (pluginFile.isDirectory()) {
                // 添加目录
                classLoader.addDirectory(pluginFile);
                
                // 查找并添加目录中的JAR文件
                File[] jarFiles = pluginFile.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
                if (jarFiles != null) {
                    for (File jarFile : jarFiles) {
                        classLoader.addJarFile(jarFile);
                        log.info("为插件添加目录中的JAR文件: {}", jarFile.getName());
                    }
                }
                log.info("为插件添加目录: {}", pluginFile.getAbsolutePath());
            } else {
                throw new IllegalArgumentException("无效的插件文件类型: " + pluginFile.getAbsolutePath());
            }
            
            // 缓存类加载器
            classLoaderCache.put(pluginId, classLoader);
            
            log.info("为插件创建类加载器: pluginId={}, file={}, strategy={}", 
                    pluginId, pluginFile.getName(), config.getStrategy());
            
            return classLoader;
        } catch (ClassLoadingException e) {
            // 关闭类加载器并转换为IOException
            try {
                classLoader.close();
            } catch (IOException closeEx) {
                log.warn("关闭失败的类加载器时出错: {}", closeEx.getMessage());
            }
            
            throw new IOException("添加插件资源失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 为插件创建专用插件类加载器
     * 
     * @param pluginId 插件ID
     * @param pluginFile 插件文件
     * @return 插件类加载器
     * @throws IOException 如果创建失败
     */
    public PluginClassLoader createPluginClassLoader(String pluginId, File pluginFile) throws IOException {
        return createPluginClassLoader(pluginId, pluginFile, null);
    }
    
    /**
     * 为插件创建专用插件类加载器，使用自定义配置
     * 
     * @param pluginId 插件ID
     * @param pluginFile 插件文件
     * @param configuration 类加载器配置
     * @return 插件类加载器
     * @throws IOException 如果创建失败
     */
    public PluginClassLoader createPluginClassLoader(String pluginId, File pluginFile, 
                                                   ClassLoaderConfiguration configuration) throws IOException {
        // 检查插件ID和文件是否有效
        if (pluginId == null || pluginId.isEmpty()) {
            throw new IllegalArgumentException("插件ID不能为空");
        }
        
        if (pluginFile == null || !pluginFile.exists() || !pluginFile.isFile()) {
            throw new IllegalArgumentException("插件文件不存在或不是有效文件: " + 
                    (pluginFile != null ? pluginFile.getAbsolutePath() : "null"));
        }
        
        // 使用默认配置或自定义配置
        ClassLoaderConfiguration config = configuration != null ? 
                configuration : defaultConfiguration;
        
        // 创建插件类加载器
        PluginClassLoader classLoader = new PluginClassLoader(
                pluginId,
                new URL[0],
                getClass().getClassLoader(),
                config
        );
        
        try {
            // 添加插件JAR文件，会自动分析JAR内容
            classLoader.addJarFile(pluginFile);
            
            // 缓存类加载器
            pluginClassLoaderCache.put(pluginId, classLoader);
            
            log.info("为插件创建专用类加载器: pluginId={}, file={}, strategy={}", 
                    pluginId, pluginFile.getName(), config.getStrategy());
            
            return classLoader;
        } catch (ClassLoadingException e) {
            // 关闭类加载器并转换为IOException
            try {
                classLoader.close();
            } catch (IOException closeEx) {
                log.warn("关闭失败的类加载器时出错: {}", closeEx.getMessage());
            }
            
            throw new IOException("添加插件JAR文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取插件的类加载器
     * 优先返回专用插件类加载器，如果不存在则返回标准动态类加载器
     * 
     * @param pluginId 插件ID
     * @return 类加载器，如果不存在则返回null
     */
    public ClassLoader getClassLoader(String pluginId) {
        // 优先返回插件类加载器
        PluginClassLoader pluginLoader = pluginClassLoaderCache.get(pluginId);
        if (pluginLoader != null) {
            return pluginLoader;
        }
        
        // 兼容旧版，返回动态类加载器
        return classLoaderCache.get(pluginId);
    }
    
    /**
     * 获取插件的专用类加载器
     * 
     * @param pluginId 插件ID
     * @return 插件类加载器，如果不存在则返回null
     */
    public PluginClassLoader getPluginClassLoader(String pluginId) {
        return pluginClassLoaderCache.get(pluginId);
    }
    
    /**
     * 为两个插件建立依赖关系
     * 
     * @param pluginId 插件ID
     * @param dependencyId 依赖插件ID
     * @return 是否成功
     */
    public boolean addPluginDependency(String pluginId, String dependencyId) {
        PluginClassLoader pluginLoader = pluginClassLoaderCache.get(pluginId);
        ClassLoader dependencyLoader = getClassLoader(dependencyId);
        
        if (pluginLoader != null && dependencyLoader != null) {
            pluginLoader.addDependencyClassLoader(dependencyId, dependencyLoader);
            log.debug("为插件[{}]添加依赖[{}]", pluginId, dependencyId);
            return true;
        }
        
        return false;
    }
    
    /**
     * 释放插件的类加载器
     * 
     * @param pluginId 插件ID
     */
    public void releaseClassLoader(String pluginId) {
        // 先尝试释放插件类加载器
        PluginClassLoader pluginLoader = pluginClassLoaderCache.remove(pluginId);
        if (pluginLoader != null && !pluginLoader.isClosed()) {
            try {
                pluginLoader.close();
                log.info("关闭插件专用类加载器: pluginId={}", pluginId);
            } catch (IOException e) {
                log.error("关闭插件专用类加载器时出错: pluginId={}, error={}", pluginId, e.getMessage(), e);
            }
        }
        
        // 再尝试释放动态类加载器（兼容旧版）
        DynamicClassLoader classLoader = classLoaderCache.remove(pluginId);
        if (classLoader != null && !classLoader.isClosed()) {
            try {
                classLoader.close();
                log.info("关闭插件动态类加载器: pluginId={}", pluginId);
            } catch (IOException e) {
                log.error("关闭插件动态类加载器时出错: pluginId={}, error={}", pluginId, e.getMessage(), e);
            }
        }
    }
    
    /**
     * 释放所有类加载器
     */
    public void releaseAllClassLoaders() {
        // 释放所有插件类加载器
        for (Map.Entry<String, PluginClassLoader> entry : pluginClassLoaderCache.entrySet()) {
            String pluginId = entry.getKey();
            PluginClassLoader classLoader = entry.getValue();
            
            if (classLoader != null && !classLoader.isClosed()) {
                try {
                    classLoader.close();
                    log.info("关闭插件专用类加载器: pluginId={}", pluginId);
                } catch (IOException e) {
                    log.error("关闭插件专用类加载器时出错: pluginId={}, error={}", 
                            pluginId, e.getMessage(), e);
                }
            }
        }
        
        // 释放所有动态类加载器（兼容旧版）
        for (Map.Entry<String, DynamicClassLoader> entry : classLoaderCache.entrySet()) {
            String pluginId = entry.getKey();
            DynamicClassLoader classLoader = entry.getValue();
            
            if (classLoader != null && !classLoader.isClosed()) {
                try {
                    classLoader.close();
                    log.info("关闭插件动态类加载器: pluginId={}", pluginId);
                } catch (IOException e) {
                    log.error("关闭插件动态类加载器时出错: pluginId={}, error={}", 
                            pluginId, e.getMessage(), e);
                }
            }
        }
        
        pluginClassLoaderCache.clear();
        classLoaderCache.clear();
    }
    
    /**
     * 获取类加载器总数
     * 
     * @return 类加载器总数
     */
    public int getClassLoaderCount() {
        return classLoaderCache.size() + pluginClassLoaderCache.size();
    }
    
    /**
     * 获取插件类加载器数量
     * 
     * @return 插件类加载器数量
     */
    public int getPluginClassLoaderCount() {
        return pluginClassLoaderCache.size();
    }
    
    /**
     * 检查插件类加载器是否存在
     * 
     * @param pluginId 插件ID
     * @return 是否存在
     */
    public boolean hasClassLoader(String pluginId) {
        return pluginClassLoaderCache.containsKey(pluginId) || classLoaderCache.containsKey(pluginId);
    }
    
    /**
     * 获取默认的类加载器配置
     * 
     * @return 默认配置
     */
    public ClassLoaderConfiguration getDefaultConfiguration() {
        return defaultConfiguration;
    }
} 