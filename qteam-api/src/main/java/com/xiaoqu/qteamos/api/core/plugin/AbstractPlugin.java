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
 * 抽象插件类
 * 提供插件接口的基本实现，方便插件开发者继承
 *
 * @author yangqijun
 * @date 2025-05-02
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.api.core.plugin;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 抽象插件类
 * 提供插件基础功能的通用实现
 */
@Slf4j
public abstract class AbstractPlugin implements Plugin {
    
    /**
     * 插件上下文
     */
    @Getter
    protected PluginContext context;
    
    /**
     * 插件信息
     */
    private final String id;
    private final String name;
    private final String version;
    private final String description;
    private final String author;
    
    /**
     * 构造方法
     * 
     * @param id 插件ID
     * @param name 插件名称
     * @param version 插件版本
     * @param description 插件描述
     * @param author 插件作者
     */
    protected AbstractPlugin(String id, String name, String version, String description, String author) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.description = description;
        this.author = author;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getVersion() {
        return version;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public String getAuthor() {
        return author;
    }
    
    @Override
    public void init(PluginContext context) throws Exception {
        this.context = context;
        log.info("Plugin {} initialized", id);
    }
    
    @Override
    public void start() throws Exception {
        log.info("Plugin {} started", id);
    }
    
    @Override
    public void stop() throws Exception {
        log.info("Plugin {} stopped", id);
    }
    
    @Override
    public void destroy() throws Exception {
        log.info("Plugin {} destroyed", id);
    }
    
    @Override
    public void uninstall() throws Exception {
        log.info("Plugin {} uninstalled", id);
    }
} 