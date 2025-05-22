/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-27 19:29:11
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-05-01 18:41:43
 * @FilePath: /qteamos/src/main/java/com/xiaoqu/qteamos/core/plugin/running/PluginDescriptorLoader.java
 * @Description: 插件描述文件加载器
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.core.plugin.running;

import com.xiaoqu.qteamos.common.utils.VersionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 插件描述文件加载器
 * 用于从插件JAR文件中加载和解析plugin.yml文件
 * 适配私有化部署环境的插件结构
 * 
 * @author yangqijun
 * @version 1.0.0
 */
@Slf4j
@Component
public class PluginDescriptorLoader {
    
    /**
     * 插件描述文件名
     */
    private static final String PLUGIN_DESCRIPTOR_FILENAME = "plugin.yml";
    
    /**
     * 从插件JAR文件加载插件描述符
     * 
     * @param pluginFile 插件JAR文件
     * @return 插件描述符
     * @throws PluginException 如果加载失败
     */
    public PluginDescriptor loadFromJar(File pluginFile) throws PluginException {
        if (pluginFile == null || !pluginFile.exists() || !pluginFile.isFile()) {
            throw new PluginException(
                    PluginException.PluginExceptionType.DESCRIPTOR_ERROR,
                    null,
                    "插件文件不存在或不是有效文件: " + (pluginFile != null ? pluginFile.getAbsolutePath() : "null")
            );
        }
        
        try (JarFile jarFile = new JarFile(pluginFile)) {
            // 查找插件描述文件
            JarEntry entry = jarFile.getJarEntry(PLUGIN_DESCRIPTOR_FILENAME);
            if (entry == null) {
                throw new PluginException(
                        PluginException.PluginExceptionType.DESCRIPTOR_ERROR,
                        null,
                        "找不到插件描述文件: " + PLUGIN_DESCRIPTOR_FILENAME
                );
            }
            
            // 读取插件描述文件
            try (InputStream inputStream = jarFile.getInputStream(entry)) {
                return loadFromStream(inputStream);
            }
        } catch (IOException e) {
            throw new PluginException(
                    PluginException.PluginExceptionType.DESCRIPTOR_ERROR,
                    null,
                    "读取插件描述文件时发生错误: " + e.getMessage(),
                    e
            );
        }
    }
    
    /**
     * 从文件加载插件描述符
     * 
     * @param descriptorFile 描述符文件
     * @return 插件描述符
     * @throws PluginException 如果加载失败
     */
    public PluginDescriptor loadFromFile(File descriptorFile) throws PluginException {
        if (descriptorFile == null || !descriptorFile.exists() || !descriptorFile.isFile()) {
            throw new PluginException(
                    PluginException.PluginExceptionType.DESCRIPTOR_ERROR,
                    null,
                    "描述符文件不存在或不是有效文件: " + 
                    (descriptorFile != null ? descriptorFile.getAbsolutePath() : "null")
            );
        }
        
        try (InputStream inputStream = Files.newInputStream(descriptorFile.toPath())) {
            return loadFromStream(inputStream);
        } catch (IOException e) {
            throw new PluginException(
                    PluginException.PluginExceptionType.DESCRIPTOR_ERROR,
                    null,
                    "读取描述符文件时发生错误: " + e.getMessage(),
                    e
            );
        }
    }
    
    /**
     * 从输入流加载插件描述符
     * 
     * @param inputStream 输入流
     * @return 插件描述符
     * @throws PluginException 如果加载失败
     */
    public PluginDescriptor loadFromStream(InputStream inputStream) throws PluginException {
        if (inputStream == null) {
            throw new PluginException(
                    PluginException.PluginExceptionType.DESCRIPTOR_ERROR,
                    null,
                    "输入流为空"
            );
        }
        
        try {
            // 解析YAML
            Yaml yaml = new Yaml();
            Map<String, Object> map = yaml.load(inputStream);
            
            // 转换为PluginDescriptor对象
            return convertToDescriptor(map);
        } catch (Exception e) {
            throw new PluginException(
                    PluginException.PluginExceptionType.DESCRIPTOR_ERROR,
                    null,
                    "解析插件描述文件时发生错误: " + e.getMessage(),
                    e
            );
        }
    }
    
    /**
     * 将YAML解析结果转换为PluginDescriptor对象
     * 
     * @param map YAML解析结果
     * @return 插件描述符
     * @throws PluginException 如果转换失败
     */
    @SuppressWarnings("unchecked")
    private PluginDescriptor convertToDescriptor(Map<String, Object> map) throws PluginException {
        // 创建描述符构建器
        PluginDescriptor.PluginDescriptorBuilder builder = PluginDescriptor.builder();
        
        // 解析新格式
        builder.pluginId((String) map.get("pluginId"))
                .name((String) map.get("name"))
                .version((String) map.get("version"))
                .mainClass((String) map.get("mainClass"))
                .description((String) map.getOrDefault("description", ""))
                .author((String) map.getOrDefault("author", ""))
                .type((String) map.getOrDefault("type", "normal"))
                .trust((String) map.getOrDefault("trust", "trust"))
                .enabled((Boolean) map.getOrDefault("enabled", true))
                .requiredSystemVersion((String) map.getOrDefault("requiredSystemVersion", "1.0.0"));

        // 处理权限
        if (map.get("permissions") instanceof List<?> permissionsList) {
            builder.permissions(permissionsList.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList());
        }

        // 处理生命周期钩子
        if (map.get("lifecycle") instanceof Map<?, ?> lifecycleMap) {
            builder.lifecycle((Map<String, String>) lifecycleMap);
        }

        // 处理扩展点
        if (map.get("extensionPoints") instanceof List<?> extensionPointList) {
            List<ExtensionPoint> extensionPoints = ((List<Map<String, Object>>) extensionPointList).stream()
                    .map(this::convertToExtensionPoint)
                    .filter(ExtensionPoint::isValid)
                    .toList();
            builder.extensionPoints(extensionPoints);
        }

        // 处理资源文件
        if (map.get("resources") instanceof List<?> resourceList) {
            List<PluginResource> resources = ((List<Map<String, Object>>) resourceList).stream()
                    .map(this::convertToResource)
                    .filter(PluginResource::isValid)
                    .toList();
            builder.resources(resources);
        }

        // 处理元数据
        if (map.get("metadata") instanceof Map<?, ?> metadataMap) {
            builder.metadata((Map<String, Object>) metadataMap);
        }
        
        // 处理更新信息
        if (map.get("update") instanceof Map<?, ?> updateInfo) {
            Map<String, String> updateInfoMap = new HashMap<>();
            // 转换更新信息为Map<String, String>
            for (Map.Entry<?, ?> entry : updateInfo.entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() != null) {
                    updateInfoMap.put((String) entry.getKey(), entry.getValue().toString());
                }
            }
            builder.updateInfo(updateInfoMap);
        }
        
        // 验证必填字段
        validateRequiredFields(builder);
        
        // 处理依赖
        List<PluginDependency> dependencies = new ArrayList<>();
        Object depsObj = map.get("dependencies");
        if (depsObj instanceof List<?> dependencyList) {
            for (Object depObj : dependencyList) {
                if (depObj instanceof Map<?, ?> dependencyMap) {
                    dependencies.add(convertToDependency((Map<String, Object>) dependencyMap));
                }
            }
        } else if (depsObj instanceof Map<?, ?> depsMap) {
            // 解析成key-value格式的依赖
            for (Map.Entry<?, ?> entry : depsMap.entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() instanceof Map<?, ?> depInfo) {
                    String pluginId = (String) entry.getKey();
                    String versionReq = depInfo.containsKey("version") ? 
                        (String) depInfo.get("version") : "*";
                    boolean optional = depInfo.containsKey("optional") && 
                        depInfo.get("optional") instanceof Boolean ? 
                        (Boolean) depInfo.get("optional") : false;
                    
                    PluginDependency dependency = PluginDependency.builder()
                            .pluginId(pluginId)
                            .versionRequirement(versionReq)
                            .optional(optional)
                            .build();
                    dependencies.add(dependency);
                }
            }
        }
        builder.dependencies(dependencies);
        
        // 处理配置
        if (map.get("config") instanceof Map<?, ?> configMap) {
            builder.properties((Map<String, Object>) configMap);
        }
        
        // 创建并验证描述符
        PluginDescriptor descriptor = builder.build();
        if (!descriptor.isValid()) {
            throw new PluginException(
                    PluginException.PluginExceptionType.DESCRIPTOR_ERROR,
                    descriptor.getPluginId(),
                    "插件描述符验证失败: 必填字段不完整或格式不正确"
            );
        }
        
        // 验证版本格式
        validateVersionFormat(descriptor);
        
        return descriptor;
    }
    
    /**
     * 验证必填字段
     * 
     * @param builder 描述符构建器
     * @throws PluginException 如果缺少必填字段
     */
    private void validateRequiredFields(PluginDescriptor.PluginDescriptorBuilder builder) throws PluginException {
        PluginDescriptor descriptor = builder.build();
        List<String> missingFields = new ArrayList<>();
        
        if (StringUtils.isBlank(descriptor.getPluginId())) {
            missingFields.add("pluginId");
        }
        
        if (StringUtils.isBlank(descriptor.getName())) {
            missingFields.add("name");
        }
        
        if (StringUtils.isBlank(descriptor.getVersion())) {
            missingFields.add("version");
        }
        
        if (StringUtils.isBlank(descriptor.getMainClass())) {
            missingFields.add("mainClass");
        }
        
        if (!missingFields.isEmpty()) {
            throw new PluginException(
                    PluginException.PluginExceptionType.DESCRIPTOR_ERROR,
                    null,
                    "缺少必填字段: " + String.join(", ", missingFields)
            );
        }
    }
    
    /**
     * 验证版本格式
     * 
     * @param descriptor 插件描述符
     * @throws PluginException 如果版本格式不正确
     */
    private void validateVersionFormat(PluginDescriptor descriptor) throws PluginException {
        try {
            if (!VersionUtils.isValidVersion(descriptor.getVersion())) {
                throw new PluginException(
                        PluginException.PluginExceptionType.DESCRIPTOR_ERROR,
                        descriptor.getPluginId(),
                        "插件版本格式不正确: " + descriptor.getVersion() + "，应遵循语义化版本规范 (主版本号.次版本号.修订号)"
                );
            }
            
            if (descriptor.getRequiredSystemVersion() != null && 
                !VersionUtils.isValidVersion(descriptor.getRequiredSystemVersion())) {
                throw new PluginException(
                        PluginException.PluginExceptionType.DESCRIPTOR_ERROR,
                        descriptor.getPluginId(),
                        "系统版本要求格式不正确: " + descriptor.getRequiredSystemVersion() + 
                        "，应遵循语义化版本规范 (主版本号.次版本号.修订号)"
                );
            }
            
            // 验证前一个版本号的格式
            String prevVersion = descriptor.getPreviousVersion();
            if (prevVersion != null && !prevVersion.isEmpty() && !VersionUtils.isValidVersion(prevVersion)) {
                throw new PluginException(
                        PluginException.PluginExceptionType.DESCRIPTOR_ERROR,
                        descriptor.getPluginId(),
                        "前一个版本号格式不正确: " + prevVersion + 
                        "，应遵循语义化版本规范 (主版本号.次版本号.修订号)"
                );
            }
        } catch (Exception e) {
            throw new PluginException(
                    PluginException.PluginExceptionType.DESCRIPTOR_ERROR,
                    descriptor.getPluginId(),
                    "版本格式验证失败: " + e.getMessage(),
                    e
            );
        }
    }
    
    /**
     * 转换依赖信息
     * 
     * @param map 依赖信息映射
     * @return 插件依赖对象
     */
    private PluginDependency convertToDependency(Map<String, Object> map) {
        PluginDependency.PluginDependencyBuilder builder = PluginDependency.builder();
        
        if (map.containsKey("pluginId")) {
            builder.pluginId((String) map.get("pluginId"));
        }
        
        if (map.containsKey("versionRequirement")) {
            builder.versionRequirement((String) map.get("versionRequirement"));
        }
        
        if (map.containsKey("optional") && map.get("optional") instanceof Boolean) {
            builder.optional((Boolean) map.get("optional"));
        } else {
            builder.optional(false);  // 默认为非可选依赖
        }
        
        return builder.build();
    }
    
    /**
     * 转换扩展点信息
     * 
     * @param map 扩展点信息映射
     * @return 扩展点对象
     */
    private ExtensionPoint convertToExtensionPoint(Map<String, Object> map) {
        ExtensionPoint.ExtensionPointBuilder builder = ExtensionPoint.builder();
        
        if (map.containsKey("id")) {
            builder.id((String) map.get("id"));
        }
        
        if (map.containsKey("name")) {
            builder.name((String) map.get("name"));
        }
        
        if (map.containsKey("description")) {
            builder.description((String) map.get("description"));
        }
        
        if (map.containsKey("type")) {
            builder.type((String) map.get("type"));
        }
        
        if (map.containsKey("interfaceClass")) {
            builder.interfaceClass((String) map.get("interfaceClass"));
        }
        
        if (map.containsKey("multiple") && map.get("multiple") instanceof Boolean) {
            builder.multiple((Boolean) map.get("multiple"));
        }
        
        if (map.containsKey("required") && map.get("required") instanceof Boolean) {
            builder.required((Boolean) map.get("required"));
        }
        
        return builder.build();
    }
    
    /**
     * 转换资源文件信息
     * 
     * @param map 资源文件信息映射
     * @return 资源文件对象
     */
    private PluginResource convertToResource(Map<String, Object> map) {
        PluginResource.PluginResourceBuilder builder = PluginResource.builder();
        
        if (map.containsKey("path")) {
            builder.path((String) map.get("path"));
        }
        
        if (map.containsKey("type")) {
            builder.type((String) map.get("type"));
        }
        
        if (map.containsKey("description")) {
            builder.description((String) map.get("description"));
        }
        
        if (map.containsKey("required") && map.get("required") instanceof Boolean) {
            builder.required((Boolean) map.get("required"));
        }
        
        return builder.build();
    }
    
    /**
     * 将对象转换为整数，如果转换失败则返回默认值
     * 
     * @param obj 要转换的对象
     * @param defaultValue 默认值
     * @return 转换后的整数或默认值
     */
    private Integer parseIntOrDefault(Object obj, Integer defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        
        if (obj instanceof Integer) {
            return (Integer) obj;
        }
        
        if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        
        return defaultValue;
    }
} 