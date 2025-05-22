/*
 * Copyright (c) 2023-2024 XiaoQuTeam. All rights reserved.
 * QTeamOS is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package com.xiaoqu.qteamos.core.plugin.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * 插件类加载器
 * 专门为插件加载优化的类加载器，扩展了DynamicClassLoader
 * 提供插件类查找、依赖分析和类冲突检测等功能
 *
 * @author yangqijun
 * @date 2025-07-12
 * @since 1.0.0
 */
@Component
public class PluginClassLoader extends DynamicClassLoader {

    private static final Logger log = LoggerFactory.getLogger(PluginClassLoader.class);
    
    /**
     * 插件依赖关系缓存
     */
    private final Map<String, Set<String>> dependenciesMap = new ConcurrentHashMap<>();
    
    /**
     * 插件导出的包缓存
     */
    private final Map<String, Set<String>> exportedPackagesMap = new ConcurrentHashMap<>();
    
    /**
     * 插件内部的服务实现缓存
     */
    private final Map<String, List<String>> serviceImplementationsMap = new ConcurrentHashMap<>();
    
    /**
     * 插件API接口缓存
     */
    private final Map<String, List<String>> apiInterfacesMap = new ConcurrentHashMap<>();
    
    /**
     * 构造函数
     *
     * @param pluginId 插件ID
     * @param urls 类路径URL数组
     * @param parent 父类加载器
     * @param configuration 类加载器配置
     */
    public PluginClassLoader(String pluginId, URL[] urls, ClassLoader parent, 
                            ClassLoaderConfiguration configuration) {
        super(pluginId, urls, parent, configuration);
        log.debug("创建插件类加载器: {}", pluginId);
    }
    
    /**
     * 添加依赖插件的类加载器
     *
     * @param dependencyId 依赖插件ID
     * @param dependencyClassLoader 依赖插件的类加载器
     */
    public void addDependencyClassLoader(String dependencyId, ClassLoader dependencyClassLoader) {
        log.debug("为插件[{}]添加依赖类加载器: {}", getPluginId(), dependencyId);
        // 这里只是记录依赖关系，实际的类加载逻辑由父类处理
        dependenciesMap.computeIfAbsent(dependencyId, k -> ConcurrentHashMap.newKeySet());
    }
    
    /**
     * 添加插件JAR文件并分析内容
     *
     * @param jarFile 插件JAR文件
     * @throws ClassLoadingException 如果添加失败
     */
    @Override
    public void addJarFile(File jarFile) throws ClassLoadingException {
        super.addJarFile(jarFile);
        
        // 分析JAR文件内容
        analyzeJarContent(jarFile);
    }
    
    /**
     * 分析JAR文件内容，提取服务实现和API接口
     *
     * @param jarFile JAR文件
     */
    private void analyzeJarContent(File jarFile) {
        log.debug("分析插件[{}]JAR文件内容: {}", getPluginId(), jarFile.getName());
        
        try (JarFile jar = new JarFile(jarFile)) {
            // 收集插件中的服务实现类
            List<String> serviceImplementations = findServiceImplementations(jar);
            serviceImplementationsMap.put(jarFile.getName(), serviceImplementations);
            
            // 收集插件中的API接口
            List<String> apiInterfaces = findApiInterfaces(jar);
            apiInterfacesMap.put(jarFile.getName(), apiInterfaces);
            
            // 收集插件导出的包
            Set<String> exportedPackages = findExportedPackages(jar);
            exportedPackagesMap.put(jarFile.getName(), exportedPackages);
            
            log.debug("插件[{}]分析结果: 服务实现={}, API接口={}, 导出包={}", 
                     getPluginId(), serviceImplementations.size(), 
                     apiInterfaces.size(), exportedPackages.size());
        } catch (IOException e) {
            log.warn("分析插件[{}]JAR文件内容失败: {}", getPluginId(), e.getMessage());
        }
    }
    
    /**
     * 在JAR文件中查找服务实现类
     *
     * @param jar JAR文件
     * @return 服务实现类列表
     */
    private List<String> findServiceImplementations(JarFile jar) {
        List<String> implementations = new ArrayList<>();
        
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                String className = entry.getName().replace("/", ".")
                        .substring(0, entry.getName().length() - 6);
                
                // 检查是否是服务实现类
                if (className.endsWith("ServiceImpl") || className.endsWith("RepositoryImpl") ||
                    className.contains(".service.impl.") || className.contains(".repository.impl.")) {
                    implementations.add(className);
                }
            }
        }
        
        return implementations;
    }
    
    /**
     * 在JAR文件中查找API接口
     *
     * @param jar JAR文件
     * @return API接口列表
     */
    private List<String> findApiInterfaces(JarFile jar) {
        List<String> interfaces = new ArrayList<>();
        
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                String className = entry.getName().replace("/", ".")
                        .substring(0, entry.getName().length() - 6);
                
                // 检查是否是API接口
                if (className.endsWith("Service") || className.endsWith("Repository") ||
                    className.contains(".api.") || className.endsWith("API")) {
                    interfaces.add(className);
                }
            }
        }
        
        return interfaces;
    }
    
    /**
     * 在JAR文件中查找导出的包
     *
     * @param jar JAR文件
     * @return 导出的包集合
     */
    private Set<String> findExportedPackages(JarFile jar) {
        Set<String> packages = ConcurrentHashMap.newKeySet();
        
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                String className = entry.getName().replace("/", ".")
                        .substring(0, entry.getName().length() - 6);
                
                // 提取包名
                int lastDot = className.lastIndexOf('.');
                if (lastDot > 0) {
                    String packageName = className.substring(0, lastDot);
                    
                    // 检查是否是导出包
                    if (packageName.contains(".api.") || 
                        packageName.contains(".pub.") ||
                        packageName.endsWith(".api")) {
                        packages.add(packageName);
                    }
                }
            }
        }
        
        return packages;
    }
    
    /**
     * 获取所有服务实现类
     *
     * @return 服务实现类列表
     */
    public List<String> getAllServiceImplementations() {
        return serviceImplementationsMap.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有API接口
     *
     * @return API接口列表
     */
    public List<String> getAllApiInterfaces() {
        return apiInterfacesMap.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有导出的包
     *
     * @return 导出的包集合
     */
    public Set<String> getAllExportedPackages() {
        return exportedPackagesMap.values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }
    
    /**
     * 获取依赖的插件ID集合
     *
     * @return 依赖的插件ID集合
     */
    public Set<String> getDependencies() {
        return Collections.unmodifiableSet(dependenciesMap.keySet());
    }
    
    /**
     * 检查是否存在指定名称的服务实现
     *
     * @param serviceName 服务名称
     * @return 是否存在服务实现
     */
    public boolean hasServiceImplementation(String serviceName) {
        for (List<String> implementations : serviceImplementationsMap.values()) {
            for (String impl : implementations) {
                if (impl.contains(serviceName)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 查找指定接口的实现类
     *
     * @param interfaceName 接口名称
     * @return 实现类列表
     */
    public List<String> findImplementationsOf(String interfaceName) {
        List<String> impls = new ArrayList<>();
        
        for (List<String> implementations : serviceImplementationsMap.values()) {
            for (String impl : implementations) {
                if (impl.contains(interfaceName.replace("Interface", ""))
                        || impl.contains(interfaceName.replace("API", ""))) {
                    impls.add(impl);
                }
            }
        }
        
        return impls;
    }
    
    /**
     * 检测并报告潜在的类冲突
     *
     * @return 冲突的类名集合
     */
    public Set<String> detectClassConflicts() {
        return getConflictedClasses();
    }
    
    /**
     * 获取插件使用的依赖库列表
     *
     * @return 依赖库列表
     */
    public List<String> getPluginDependencyLibraries() {
        List<String> libraries = new ArrayList<>();
        
        for (URL url : getURLs()) {
            String path = url.getPath();
            if (path.endsWith(".jar")) {
                String jarName = path.substring(path.lastIndexOf('/') + 1);
                libraries.add(jarName);
            }
        }
        
        return libraries;
    }
    
    /**
     * 关闭类加载器，释放所有资源
     */
    @Override
    public void close() throws IOException {
        // 清理插件特有的缓存
        dependenciesMap.clear();
        serviceImplementationsMap.clear();
        apiInterfacesMap.clear();
        exportedPackagesMap.clear();
        
        // 调用父类关闭方法
        super.close();
        
        log.debug("插件[{}]类加载器已关闭", getPluginId());
    }
} 