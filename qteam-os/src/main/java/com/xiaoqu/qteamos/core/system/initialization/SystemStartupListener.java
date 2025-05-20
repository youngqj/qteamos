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
 * 系统启动事件监听器
 * 监听系统启动过程中的各种事件，并执行相应的处理
 *
 * @author yangqijun
 * @date 2025-05-04
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.system.initialization;

import com.xiaoqu.qteamos.core.plugin.event.EventListener;
import com.xiaoqu.qteamos.core.plugin.event.plugins.SystemStartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * 系统启动事件监听器
 * 监听系统启动过程中的各种事件，执行相应的处理逻辑
 */
@Component
public class SystemStartupListener {
    private static final Logger log = LoggerFactory.getLogger(SystemStartupListener.class);
    
    /**
     * 系统Banner
     */
    @Autowired
    private SystemBanner systemBanner;
    
    /**
     * 系统启动属性
     */
    @Autowired
    private SystemStartupProperties properties;
    
    /**
     * 记录系统启动时间
     */
    private long startupStartTime;
    
    /**
     * 处理Spring上下文刷新事件
     * 
     * @param event Spring上下文刷新事件
     */
    @org.springframework.context.event.EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() == null) {
            // 记录启动开始时间
            startupStartTime = System.currentTimeMillis();
            
            // 记录上下文刷新完成
            log.info("Spring上下文已刷新，核心组件已初始化");
            
            if (properties.isVerboseLogging()) {
                log.info("已加载的Bean列表:");
                String[] beanNames = event.getApplicationContext().getBeanDefinitionNames();
                for (String beanName : beanNames) {
                    log.info(" - {}", beanName);
                }
            }
        }
    }
    
    /**
     * 处理系统启动事件
     * 
     * @param event 系统启动事件
     */
    @EventListener(topics = "system", types = "startup")
    public void onSystemStartup(SystemStartupEvent event) {
        // 系统启动事件已发布
        log.info("接收到系统启动事件，系统正在初始化");
        
        // 记录启动完成时间
        long startupTime = System.currentTimeMillis() - startupStartTime;
        log.info("系统启动完成，耗时: {} 毫秒", startupTime);
        
        // 输出系统启动完成信息
        systemBanner.showStartupCompleted();
        
        // 记录系统环境信息
        logSystemInfo(event);
    }
    
    /**
     * 记录系统环境信息
     * 
     * @param event 系统启动事件
     */
    private void logSystemInfo(SystemStartupEvent event) {
        if (properties.isVerboseLogging()) {
            SystemStartupEvent.SystemInfo sysInfo = event.getSystemInfo();
            log.info("系统环境信息:");
            log.info(" - 系统版本: {}", sysInfo.getVersion());
            log.info(" - Java版本: {}", sysInfo.getJavaVersion());
            log.info(" - 操作系统: {} {}", sysInfo.getOsName(), sysInfo.getOsVersion());
            log.info(" - 最大内存: {} MB", sysInfo.getMaxMemory() / (1024 * 1024));
            log.info(" - 处理器核心数: {}", sysInfo.getProcessorCount());
        }
    }
} 