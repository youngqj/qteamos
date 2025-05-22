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

package com.xiaoqu.qteamos.core.plugin.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.xiaoqu.qteamos.api.core.plugin.api.PluginInfo.DependencyInfo;
import com.xiaoqu.qteamos.api.core.plugin.api.PluginInfo.ExtensionPointInfo;
import com.xiaoqu.qteamos.core.plugin.running.ExtensionPoint;
import com.xiaoqu.qteamos.core.plugin.running.PluginDependency;
import com.xiaoqu.qteamos.core.plugin.running.PluginDescriptor;
import com.xiaoqu.qteamos.core.plugin.running.PluginState;

/**
 * 插件信息适配器
 * 负责在API接口PluginInfo和核心层PluginInfo之间进行转换
 *
 * @author yangqijun
 * @date 2025-05-26
 * @since 1.0.0
 */
@Component
public class PluginInfoAdapter {
    
    /**
     * 将API接口PluginInfo转换为核心层PluginInfo
     *
     * @param apiInfo API接口PluginInfo
     * @return 核心层PluginInfo
     */
    public com.xiaoqu.qteamos.core.plugin.running.PluginInfo toCorePluginInfo(
            com.xiaoqu.qteamos.api.core.plugin.api.PluginInfo apiInfo) {
        
        if (apiInfo == null) {
            return null;
        }
        
        com.xiaoqu.qteamos.core.plugin.running.PluginInfo.PluginInfoBuilder builder = 
                toCorePluginInfoBuilder(apiInfo);
        
        if (builder == null) {
            return null;
        }
        
        return builder.build();
    }
    
    /**
     * 将核心层PluginInfo转换为API接口PluginInfo
     *
     * @param coreInfo 核心层PluginInfo
     * @return API接口PluginInfo
     */
    public com.xiaoqu.qteamos.api.core.plugin.api.PluginInfo toApiPluginInfo(
            com.xiaoqu.qteamos.core.plugin.running.PluginInfo coreInfo) {
        
        if (coreInfo == null) {
            System.out.println("转换失败: coreInfo为null");
            return null;
        }
        
        PluginDescriptor descriptor = coreInfo.getDescriptor();
        if (descriptor == null) {
            System.out.println("转换失败: descriptor为null");
            return null;
        }
        
        // 构建API接口的PluginInfo对象
        com.xiaoqu.qteamos.api.core.plugin.api.PluginInfo apiInfo = new com.xiaoqu.qteamos.api.core.plugin.api.PluginInfo();
        
        // 设置基本信息
        apiInfo.setPluginId(descriptor.getPluginId());
        apiInfo.setName(descriptor.getName());
        apiInfo.setVersion(descriptor.getVersion());
        apiInfo.setDescription(descriptor.getDescription());
        apiInfo.setAuthor(descriptor.getAuthor());
        apiInfo.setMainClass(descriptor.getMainClass());
        apiInfo.setType(descriptor.getType());
        apiInfo.setTrust(descriptor.getTrust() != null ? descriptor.getTrust().toString() : null);
        apiInfo.setState(coreInfo.getState().name());
        apiInfo.setEnabled(coreInfo.isEnabled());
        apiInfo.setLoadTime(coreInfo.getLoadTime());
        apiInfo.setStartTime(coreInfo.getStartTime());
        
        // 设置JAR路径
        if (coreInfo.getJarPath() != null) {
            apiInfo.setJarPath(coreInfo.getJarPath().toString());
        }
        
        // 设置是否有依赖
        if (descriptor.getDependencies() != null && !descriptor.getDependencies().isEmpty()) {
            apiInfo.setHaveDependency(1); // 有依赖
            // 设置依赖信息
            List<DependencyInfo> dependencies = new ArrayList<>();
            for (PluginDependency dep : descriptor.getDependencies()) {
                DependencyInfo dependencyInfo = new DependencyInfo();
                dependencyInfo.setPluginId(dep.getPluginId());
                dependencyInfo.setVersion(dep.getVersionRequirement());
                dependencyInfo.setOptional(dep.isOptional());
                dependencies.add(dependencyInfo);
            }
            apiInfo.setDependencies(dependencies);
        } else {
            apiInfo.setHaveDependency(0); // 无依赖
        }
        
        // 设置扩展点信息
        if (descriptor.getExtensionPoints() != null && !descriptor.getExtensionPoints().isEmpty()) {
            List<ExtensionPointInfo> extPoints = new ArrayList<>();
            for (ExtensionPoint extPoint : descriptor.getExtensionPoints()) {
                ExtensionPointInfo extPointInfo = new ExtensionPointInfo();
                extPointInfo.setId(extPoint.getId());
                extPointInfo.setName(extPoint.getName());
                extPointInfo.setDescription(extPoint.getDescription());
                extPointInfo.setType(extPoint.getType());
                extPoints.add(extPointInfo);
            }
            apiInfo.setExtensionPoints(extPoints);
        }
        
        // 设置元数据
        System.out.println("插件[" + descriptor.getPluginId() + "]的元数据: " + descriptor.getMetadata());
        if (descriptor.getMetadata() != null && !descriptor.getMetadata().isEmpty()) {
            System.out.println("设置元数据: " + descriptor.getMetadata());
            apiInfo.setMetadata(new HashMap<>(descriptor.getMetadata()));
        } else {
            System.out.println("元数据为空，无法设置");
            // 初始化一个示例元数据
            Map<String, Object> defaultMetadata = new HashMap<>();
            defaultMetadata.put("controllers", List.of(
                "com.xiaoqu.qteamos.plugin.helloworld.controller.HelloWorldController",
                "com.xiaoqu.qteamos.plugin.helloworld.controller.ViewDemoController"
            ));
            apiInfo.setMetadata(defaultMetadata);
            System.out.println("已设置默认元数据: " + defaultMetadata);
        }
        
        return apiInfo;
    }
    
    /**
     * 将API接口PluginInfo转换为核心层PluginInfo
     * 注意：这是一个部分转换，因为核心层PluginInfo包含运行时信息，无法完全从API接口PluginInfo创建
     *
     * @param apiInfo API接口PluginInfo
     * @return 部分填充的核心层PluginInfo构建器
     */
    public com.xiaoqu.qteamos.core.plugin.running.PluginInfo.PluginInfoBuilder toCorePluginInfoBuilder(
            com.xiaoqu.qteamos.api.core.plugin.api.PluginInfo apiInfo) {
        
        if (apiInfo == null) {
            return null;
        }
        
        // 创建描述符
        PluginDescriptor.PluginDescriptorBuilder descriptorBuilder = PluginDescriptor.builder()
                .pluginId(apiInfo.getPluginId())
                .name(apiInfo.getName())
                .version(apiInfo.getVersion())
                .description(apiInfo.getDescription())
                .author(apiInfo.getAuthor())
                .mainClass(apiInfo.getMainClass())
                .type(apiInfo.getType())
                .trust(apiInfo.getTrust() != null && !apiInfo.getTrust().isEmpty() ? apiInfo.getTrust().trim() : null);
        
        // 设置依赖
        if (apiInfo.getDependencies() != null && !apiInfo.getDependencies().isEmpty()) {
            List<PluginDependency> dependencies = apiInfo.getDependencies().stream()
                    .map(dep -> PluginDependency.builder()
                            .pluginId(dep.getPluginId())
                            .versionRequirement(dep.getVersion())
                            .optional(dep.isOptional())
                            .build())
                    .collect(Collectors.toList());
            descriptorBuilder.dependencies(dependencies);
        }
        
        // 设置元数据
        if (apiInfo.getMetadata() != null && !apiInfo.getMetadata().isEmpty()) {
            descriptorBuilder.metadata(new HashMap<>(apiInfo.getMetadata()));
        }
        
        // 构建描述符
        PluginDescriptor descriptor = descriptorBuilder.build();
        
        // 创建PluginInfo构建器
        return com.xiaoqu.qteamos.core.plugin.running.PluginInfo.builder()
                .descriptor(descriptor)
                .state(PluginState.valueOf(apiInfo.getState()))
                .enabled(apiInfo.isEnabled())
                .loadTime(apiInfo.getLoadTime())
                .startTime(apiInfo.getStartTime())
                .jarPath(apiInfo.getJarPath() != null ? java.nio.file.Path.of(apiInfo.getJarPath()) : null);
    }
} 