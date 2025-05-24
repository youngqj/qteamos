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
 * 抽象插件基类
 * 为插件开发者提供便捷的API，包装core层的复杂实现
 * 
 * @author yangqijun
 * @date 2024-07-25
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.sdk.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 抽象插件基类
 * 为插件开发者提供便捷的API，简化插件开发流程
 */
public abstract class AbstractPlugin implements Plugin {
    
    /**
     * 日志对象
     */
    protected final Logger log;
    
    /**
     * 插件ID
     */
    protected String id;
    
    /**
     * 插件名称
     */
    protected String name;
    
    /**
     * 插件版本
     */
    protected String version;
    
    /**
     * 插件描述
     */
    protected String description;
    
    /**
     * 插件作者
     */
    protected String author;
    
    /**
     * 插件上下文
     */
    protected PluginContext context;
    
    /**
     * 默认构造函数
     */
    public AbstractPlugin() {
        this.log = LoggerFactory.getLogger(getClass());
    }
    
    /**
     * 带参数的构造函数
     * 
     * @param id 插件ID
     * @param name 插件名称
     * @param version 插件版本
     * @param description 插件描述
     * @param author 插件作者
     */
    public AbstractPlugin(String id, String name, String version, String description, String author) {
        this.log = LoggerFactory.getLogger(getClass());
        this.id = id;
        this.name = name;
        this.version = version;
        this.description = description;
        this.author = author;
    }
    
    @Override
    public String getId() {
        return id != null ? id : (context != null ? context.getPluginId() : null);
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
        return description != null ? description : "插件描述";
    }
    
    @Override
    public String getAuthor() {
        return author != null ? author : "未知";
    }
    
    @Override
    public void init(com.xiaoqu.qteamos.sdk.plugin.PluginContext context) throws Exception {
        this.context = context;
        
        if (this.id == null) {
        this.id = context.getPluginId();
        }
        
        log.info("插件 [{}] 初始化", id);
        
        // 确保插件所需的目录存在
        ensureDirectoriesExist();
        
        // 初始化数据库(如果有)
        initDatabaseIfNeeded();
    }
    
    @Override
    public void start() throws Exception {
        log.info("插件 [{}] 启动", id);
    }
    
    @Override
    public void stop() throws Exception {
        log.info("插件 [{}] 停止", id);
    }
    
    @Override
    public void destroy() throws Exception {
        log.info("插件 [{}] 销毁", id);
    }
    
    @Override
    public void uninstall() throws Exception {
        log.info("插件 [{}] 卸载", id);
        destroy();
    }
    
    /**
     * 确保插件所需的目录存在
     */
    protected void ensureDirectoriesExist() {
        if (context != null) {
            ensureDirectoryExists(new File(context.getDataFolderPath()));
        }
    }
    
    /**
     * 确保目录存在，如果不存在则创建
     * 
     * @param directory 目录
     */
    protected void ensureDirectoryExists(File directory) {
        if (!directory.exists()) {
            try {
                Files.createDirectories(directory.toPath());
                log.debug("创建目录: {}", directory.getAbsolutePath());
            } catch (IOException e) {
                log.error("创建目录失败: " + directory.getAbsolutePath(), e);
            }
        }
    }
    
    /**
     * 获取插件数据目录
     * 
     * @return 数据目录路径
     */
    protected String getDataFolderPath() {
        if (context != null) {
            // 使用插件ID作为数据目录名
            return "data/" + context.getPluginId();
        }
        return "data/unknown";
    }
    
    /**
     * 初始化数据库（如果需要）
     * 已废弃：请直接使用@Autowired注入Mapper
     * 
     * @deprecated 请使用标准Spring Boot方式：@Autowired注入Mapper
     */
    @Deprecated
    protected void initDatabaseIfNeeded() {
        log.warn("initDatabaseIfNeeded方法已废弃，请使用标准Spring Boot方式：@Autowired注入Mapper");
    }
    
    /**
     * 执行SQL脚本（已废弃）
     * 请使用标准Spring Boot方式管理数据库
     * 
     * @param sqlFile SQL文件
     * @return 是否成功
     * @deprecated 请使用标准Spring Boot方式管理数据库
     */
    @Deprecated
    protected boolean executeSqlFile(File sqlFile) {
        log.warn("executeSqlFile方法已废弃，请使用标准Spring Boot方式管理数据库");
        return false;
    }
} 