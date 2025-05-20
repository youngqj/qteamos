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
 * 默认安全服务实现
 * 提供基础的安全服务功能实现
 *
 * @author yangqijun
 * @date 2025-05-04
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 默认安全服务实现
 * 提供基础的认证授权和安全防护功能
 */
@Service
public class DefaultSecurityService implements SecurityService {
    
    private static final Logger log = LoggerFactory.getLogger(DefaultSecurityService.class);
    
    /**
     * 是否启用API访问控制
     */
    @Value("${qteamos.security.enabled:true}")
    private boolean enabled;
    
    /**
     * 是否启用CORS
     */
    @Value("${qteamos.security.enable-cors:true}")
    private boolean enableCors;
    
    /**
     * 是否已初始化
     */
    private boolean initialized = false;
    
    /**
     * 安全检查次数统计
     */
    private final AtomicInteger securityCheckCount = new AtomicInteger(0);
    
    /**
     * 初始化安全服务
     */
    @Override
    public void initialize() {
        if (initialized) {
            log.info("安全服务已经初始化，跳过重复初始化");
            return;
        }
        
        log.info("初始化安全服务...");
        
        if (!enabled) {
            log.warn("API访问控制已禁用，系统可能存在安全风险");
        } else {
            log.info("API访问控制已启用");
            
            if (enableCors) {
                log.info("CORS支持已启用");
            } else {
                log.info("CORS支持已禁用");
            }
        }
        
        initialized = true;
        log.info("安全服务初始化完成");
    }
    
    /**
     * 关闭安全服务
     */
    @Override
    public void shutdown() {
        log.info("关闭安全服务...");
        securityCheckCount.set(0);
        initialized = false;
        log.info("安全服务已关闭");
    }
    
    /**
     * 检查安全服务健康状态
     *
     * @return 如果安全服务运行正常则返回true
     */
    @Override
    public boolean isHealthy() {
        return initialized;
    }
    
    /**
     * 获取安全服务状态信息
     *
     * @return 安全服务状态信息
     */
    @Override
    public String getStatus() {
        if (!initialized) {
            return "未初始化";
        }
        
        if (!enabled) {
            return "有限功能模式（API访问控制已禁用）";
        }
        
        StringBuilder status = new StringBuilder("正常运行");
        status.append("，API访问控制: 已启用");
        status.append("，CORS支持: ").append(enableCors ? "已启用" : "已禁用");
        status.append("，安全检查次数: ").append(securityCheckCount.get());
        
        return status.toString();
    }
    
    /**
     * 执行安全检查
     *
     * @param resourcePath 资源路径
     * @param userId 用户ID
     * @return 是否通过安全检查
     */
    public boolean checkAccess(String resourcePath, String userId) {
        if (!initialized || !enabled) {
            return true; // 安全功能未启用时默认放行
        }
        
        // 增加安全检查计数
        securityCheckCount.incrementAndGet();
        
        // 实际项目中，这里应该实现真正的安全检查逻辑
        // 例如：检查用户权限、角色、IP限制等
        log.debug("执行安全检查: 资源={}, 用户={}", resourcePath, userId);
        
        return true; // 示例实现始终返回true
    }
} 