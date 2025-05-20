/*
 * Copyright (c) 2023-2025 XiaoQuTeam. All rights reserved.
 * QTeamOS is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

/**
 * 简化的插件工厂
 * 桥接SDK和core层实现，为插件开发者提供简单的接口
 *
 * @author yangqijun
 * @date 2024-07-25
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.sdk.plugin;

import com.xiaoqu.qteamos.api.core.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * 简化的插件工厂
 * 为插件开发者提供更友好的接口，封装core层的复杂实现
 */
public class SimplePluginFactory {
    
    private static final Logger log = LoggerFactory.getLogger(SimplePluginFactory.class);
    
    // 插件系统的服务实例缓存
    private static Object pluginSystemInstance;
    
    /**
     * 获取插件实例
     *
     * @param pluginId 插件ID
     * @return 插件实例
     */
    public static Plugin getPlugin(String pluginId) {
        try {
            Object pluginSystem = getPluginSystem();
            if (pluginSystem != null) {
                Method getPluginInstance = pluginSystem.getClass().getMethod("getPluginInstance", String.class);
                Object result = getPluginInstance.invoke(pluginSystem, pluginId);
                
                // 处理可能返回的Optional
                if (result != null && result.getClass().getSimpleName().equals("Optional")) {
                    Method isPresent = result.getClass().getMethod("isPresent");
                    Boolean present = (Boolean) isPresent.invoke(result);
                    
                    if (present) {
                        Method get = result.getClass().getMethod("get");
                        Object pluginObj = get.invoke(result);
                        if (pluginObj instanceof Plugin) {
                            return (Plugin) pluginObj;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取插件实例失败: {}", pluginId, e);
        }
        return null;
    }
    
    /**
     * 创建插件JAR包
     *
     * @param pluginClass 插件主类
     * @param outputDir 输出目录
     * @param metadata 插件元数据
     * @return 插件JAR文件
     */
    public static File createPluginJar(Class<? extends Plugin> pluginClass, 
                                 File outputDir, 
                                 Map<String, String> metadata) {
        if (pluginClass == null) {
            throw new IllegalArgumentException("插件主类不能为空");
        }
        
        if (outputDir == null || !outputDir.exists() || !outputDir.isDirectory()) {
            throw new IllegalArgumentException("输出目录无效");
        }
        
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        
        // 确保必要的元数据
        if (!metadata.containsKey("Plugin-Id")) {
            metadata.put("Plugin-Id", pluginClass.getSimpleName().toLowerCase());
        }
        
        if (!metadata.containsKey("Plugin-Name")) {
            metadata.put("Plugin-Name", pluginClass.getSimpleName());
        }
        
        if (!metadata.containsKey("Plugin-Version")) {
            metadata.put("Plugin-Version", "1.0.0");
        }
        
        if (!metadata.containsKey("Plugin-Class")) {
            metadata.put("Plugin-Class", pluginClass.getName());
        }
        
        // 创建MANIFEST.MF
        Manifest manifest = new Manifest();
        Attributes mainAttributes = manifest.getMainAttributes();
        mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        
        // 添加插件元数据
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            mainAttributes.put(new Attributes.Name(entry.getKey()), entry.getValue());
        }
        
        // 创建JAR文件
        String jarFileName = metadata.get("Plugin-Id") + ".jar";
        File jarFile = new File(outputDir, jarFileName);
        
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile), manifest)) {
            // 获取类的路径
            String className = pluginClass.getName().replace('.', '/') + ".class";
            String classPath = pluginClass.getResource("/" + className).getPath();
            
            // 添加类文件到JAR
            byte[] classBytes = Files.readAllBytes(Paths.get(classPath));
            jos.putNextEntry(new ZipEntry(className));
            jos.write(classBytes);
            jos.closeEntry();
            
            log.info("插件JAR创建成功: {}", jarFile.getAbsolutePath());
            return jarFile;
        } catch (IOException e) {
            log.error("创建插件JAR失败", e);
            return null;
        }
    }
    
    /**
     * 安装插件
     *
     * @param jarFile 插件JAR文件
     * @return 是否安装成功
     */
    public static boolean installPlugin(File jarFile) {
        try {
            Object pluginSystem = getPluginSystem();
            if (pluginSystem != null) {
                Method installPlugin = pluginSystem.getClass().getMethod("installPlugin", File.class);
                Object result = installPlugin.invoke(pluginSystem, jarFile);
                return result instanceof Boolean && (Boolean) result;
            }
        } catch (Exception e) {
            log.error("安装插件失败: {}", jarFile, e);
        }
        return false;
    }
    
    /**
     * 卸载插件
     *
     * @param pluginId 插件ID
     * @return 是否卸载成功
     */
    public static boolean uninstallPlugin(String pluginId) {
        try {
            Object pluginSystem = getPluginSystem();
            if (pluginSystem != null) {
                Method uninstallPlugin = pluginSystem.getClass().getMethod("uninstallPlugin", String.class);
                Object result = uninstallPlugin.invoke(pluginSystem, pluginId);
                return result instanceof Boolean && (Boolean) result;
            }
        } catch (Exception e) {
            log.error("卸载插件失败: {}", pluginId, e);
        }
        return false;
    }
    
    /**
     * 启用插件
     *
     * @param pluginId 插件ID
     * @return 是否启用成功
     */
    public static boolean enablePlugin(String pluginId) {
        try {
            Object pluginSystem = getPluginSystem();
            if (pluginSystem != null) {
                Method enablePlugin = pluginSystem.getClass().getMethod("enablePlugin", String.class);
                Object result = enablePlugin.invoke(pluginSystem, pluginId);
                return result instanceof Boolean && (Boolean) result;
            }
        } catch (Exception e) {
            log.error("启用插件失败: {}", pluginId, e);
        }
        return false;
    }
    
    /**
     * 禁用插件
     *
     * @param pluginId 插件ID
     * @return 是否禁用成功
     */
    public static boolean disablePlugin(String pluginId) {
        try {
            Object pluginSystem = getPluginSystem();
            if (pluginSystem != null) {
                Method disablePlugin = pluginSystem.getClass().getMethod("disablePlugin", String.class);
                Object result = disablePlugin.invoke(pluginSystem, pluginId);
                return result instanceof Boolean && (Boolean) result;
            }
        } catch (Exception e) {
            log.error("禁用插件失败: {}", pluginId, e);
        }
        return false;
    }
    
    /**
     * 重新加载插件
     *
     * @param pluginId 插件ID
     * @return 是否重新加载成功
     */
    public static boolean reloadPlugin(String pluginId) {
        try {
            Object pluginSystem = getPluginSystem();
            if (pluginSystem != null) {
                Method reloadPlugin = pluginSystem.getClass().getMethod("reloadPlugin", String.class);
                Object result = reloadPlugin.invoke(pluginSystem, pluginId);
                return result instanceof Boolean && (Boolean) result;
            }
        } catch (Exception e) {
            log.error("重新加载插件失败: {}", pluginId, e);
        }
        return false;
    }
    
    /**
     * 获取插件系统实例
     *
     * @return 插件系统实例
     */
    private static synchronized Object getPluginSystem() {
        if (pluginSystemInstance == null) {
            try {
                // 尝试从Spring上下文获取PluginSystem实例
                Class<?> springContextClass = Class.forName("org.springframework.context.ApplicationContext");
                Class<?> springContextHolderClass = Class.forName("org.springframework.web.context.ContextLoader");
                Method getCurrentWebApplicationContext = springContextHolderClass.getMethod("getCurrentWebApplicationContext");
                Object context = getCurrentWebApplicationContext.invoke(null);
                
                if (context != null) {
                    Method getBean = springContextClass.getMethod("getBean", Class.class);
                    Class<?> pluginSystemClass = Class.forName("com.xiaoqu.qteamos.core.plugin.PluginSystem");
                    pluginSystemInstance = getBean.invoke(context, pluginSystemClass);
                }
            } catch (Exception e) {
                log.error("获取插件系统实例失败", e);
            }
        }
        return pluginSystemInstance;
    }
    
    /**
     * 获取插件数据目录
     *
     * @param pluginId 插件ID
     * @return 插件数据目录
     */
    public static File getPluginDataDirectory(String pluginId) {
        String pluginDirPath = System.getProperty("plugin.storage-path", "./plugins");
        File pluginDir = new File(pluginDirPath, pluginId);
        return pluginDir;
    }
    
    /**
     * 获取当前插件ID
     *
     * @return 当前插件ID
     */
    public static String getCurrentPluginId() {
        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stackTrace) {
                Class<?> callerClass = Class.forName(element.getClassName());
                if (Plugin.class.isAssignableFrom(callerClass) && !callerClass.equals(AbstractPlugin.class)) {
                    return callerClass.getSimpleName().toLowerCase();
                }
            }
        } catch (Exception e) {
            log.error("获取当前插件ID失败", e);
        }
        return null;
    }
} 