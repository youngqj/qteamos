/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-28 16:22:53
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-04-28 17:00:44
 * @FilePath: /qteamos/src/main/java/com/xiaoqu/qteamos/core/plugin/convert/PluginConverter.java
 * @Description: service
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.core.plugin.convert;

import com.xiaoqu.qteamos.core.plugin.dto.PluginDTO;
import com.xiaoqu.qteamos.core.plugin.dto.PluginDependencyDTO;
import com.xiaoqu.qteamos.core.plugin.dto.PluginResourceDTO;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import com.xiaoqu.qteamos.core.plugin.running.PluginDescriptor;
import com.xiaoqu.qteamos.core.plugin.running.PluginDependency;
import com.xiaoqu.qteamos.core.plugin.running.PluginResource;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * 插件数据转换器
 * 用于在运行时模型和DTO之间进行转换
 *
 * @author yangqijun
 * @date 2024-07-04
 */
@Component
public class PluginConverter {

    /**
     * 将运行时插件信息转换为DTO
     *
     * @param pluginInfo 运行时插件信息
     * @return 插件DTO
     */
    public PluginDTO toDTO(PluginInfo pluginInfo) {
        if (pluginInfo == null) {
            return null;
        }

        PluginDescriptor descriptor = pluginInfo.getDescriptor();
        if (descriptor == null) {
            return null;
        }

        return PluginDTO.builder()
                .pluginId(descriptor.getPluginId())
                .name(descriptor.getName())
                .version(descriptor.getVersion())
                .description(descriptor.getDescription())
                .author(descriptor.getAuthor())
                .type(descriptor.getType())
                .trust(descriptor.getTrust().toString())
                .state(pluginInfo.getState())
                .enabled(pluginInfo.isEnabled())
                .loadTime(pluginInfo.getLoadTime())
                .mainClass(descriptor.getMainClass())
                .dependencies(descriptor.getDependencies().stream()
                        .map(this::toDependencyDTO)
                        .collect(Collectors.toList()))
                .jarPath(pluginInfo.getJarPath() != null ? pluginInfo.getJarPath().toString() : null)
                .resourcePaths(pluginInfo.getResourcePaths())
                .errorMessage(pluginInfo.getErrorMessage())
                .build();
    }

    /**
     * 将插件依赖转换为DTO
     *
     * @param dependency 插件依赖
     * @return 插件依赖DTO
     */
    private PluginDependencyDTO toDependencyDTO(PluginDependency dependency) {
        if (dependency == null) {
            return null;
        }

        return PluginDependencyDTO.builder()
                .pluginId(dependency.getPluginId())
                .versionRequirement(dependency.getVersionRequirement())
                .optional(dependency.isOptional())
                .satisfied(dependency.isValid())
                .build();
    }

    /**
     * 将插件资源转换为DTO
     *
     * @param resource 插件资源
     * @return 插件资源DTO
     */
    private PluginResourceDTO toResourceDTO(PluginResource resource) {
        if (resource == null) {
            return null;
        }

        return PluginResourceDTO.builder()
                .path(resource.getPath())
                .type(resource.getType())
                .directory(resource.isDirectory())
                .build();
    }
}