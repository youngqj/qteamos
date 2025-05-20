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
     * 初始化数据库
     * 自动检测并执行db/init.sql文件
     */
    protected void initDatabaseIfNeeded() {
        try {
            if (context != null) {
                // 假设在插件数据目录中有一个db子目录
                File sqlDir = new File(context.getDataFolderPath(), "db");
                if (sqlDir.exists() && sqlDir.isDirectory()) {
                    File initSql = new File(sqlDir, "init.sql");
                    if (initSql.exists() && initSql.isFile()) {
                        log.info("检测到数据库初始化脚本，执行: {}", initSql.getName());
                        boolean success = executeSqlFile(initSql);
                        if (success) {
                            log.info("数据库初始化成功");
                        } else {
                            log.error("数据库初始化失败");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("初始化数据库时发生异常", e);
        }
    }
    
    /**
     * 执行SQL文件
     * 
     * @param sqlFile SQL文件
     * @return 是否成功
     */
    protected boolean executeSqlFile(File sqlFile) {
        Connection connection = null;
        
        try {
            if (context != null && sqlFile.exists()) {
                String sql = new String(Files.readAllBytes(sqlFile.toPath()));
                
                try {
                    connection = context.getDataSourceService().getConnection();
                    try (Statement statement = connection.createStatement()) {
                        // 分割SQL语句
                        String[] sqlStatements = sql.split(";");
                        for (String sqlStatement : sqlStatements) {
                            String trimmedSql = sqlStatement.trim();
                            if (!trimmedSql.isEmpty()) {
                                statement.execute(trimmedSql);
                            }
                        }
                        return true;
                    }
                } catch (SQLException e) {
                    log.error("执行SQL语句失败", e);
                }
            }
        } catch (Exception e) {
            log.error("执行SQL文件失败: " + sqlFile.getAbsolutePath(), e);
        } finally {
            // 确保连接关闭
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    log.error("关闭数据库连接失败", e);
                }
            }
        }
        return false;
    }
} 