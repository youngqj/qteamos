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

/**
 * 插件安全沙箱配置
 * 用于配置插件沙箱的行为和限制
 *
 * @author yangqijun
 * @date 2024-07-09
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.plugin.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "plugin.sandbox")
public class SandboxConfig {

    /**
     * 是否启用安全沙箱
     */
    private boolean enabled = false;

    /**
     * 是否启用类隔离
     */
    private boolean classIsolationEnabled = true;

    /**
     * 是否启用资源限制
     */
    private boolean resourceLimitEnabled = true;

    /**
     * 是否启用权限检查
     */
    private boolean permissionCheckEnabled = true;

    /**
     * 是否启用插件签名验证
     */
    private boolean signatureVerificationEnabled = false;

    /**
     * 默认内存限制 (MB)
     */
    private long defaultMemoryLimit = 256;

    /**
     * 默认CPU使用率限制 (%)
     */
    private int defaultCpuLimit = 50;

    /**
     * 默认文件存储限制 (MB)
     */
    private long defaultStorageLimit = 100;

    /**
     * 默认线程数限制
     */
    private int defaultThreadLimit = 10;

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isClassIsolationEnabled() {
        return classIsolationEnabled;
    }

    public void setClassIsolationEnabled(boolean classIsolationEnabled) {
        this.classIsolationEnabled = classIsolationEnabled;
    }

    public boolean isResourceLimitEnabled() {
        return resourceLimitEnabled;
    }

    public void setResourceLimitEnabled(boolean resourceLimitEnabled) {
        this.resourceLimitEnabled = resourceLimitEnabled;
    }

    public boolean isPermissionCheckEnabled() {
        return permissionCheckEnabled;
    }

    public void setPermissionCheckEnabled(boolean permissionCheckEnabled) {
        this.permissionCheckEnabled = permissionCheckEnabled;
    }

    public boolean isSignatureVerificationEnabled() {
        return signatureVerificationEnabled;
    }

    public void setSignatureVerificationEnabled(boolean signatureVerificationEnabled) {
        this.signatureVerificationEnabled = signatureVerificationEnabled;
    }

    public long getDefaultMemoryLimit() {
        return defaultMemoryLimit;
    }

    public void setDefaultMemoryLimit(long defaultMemoryLimit) {
        this.defaultMemoryLimit = defaultMemoryLimit;
    }

    public int getDefaultCpuLimit() {
        return defaultCpuLimit;
    }

    public void setDefaultCpuLimit(int defaultCpuLimit) {
        this.defaultCpuLimit = defaultCpuLimit;
    }

    public long getDefaultStorageLimit() {
        return defaultStorageLimit;
    }

    public void setDefaultStorageLimit(long defaultStorageLimit) {
        this.defaultStorageLimit = defaultStorageLimit;
    }

    public int getDefaultThreadLimit() {
        return defaultThreadLimit;
    }

    public void setDefaultThreadLimit(int defaultThreadLimit) {
        this.defaultThreadLimit = defaultThreadLimit;
    }
} 