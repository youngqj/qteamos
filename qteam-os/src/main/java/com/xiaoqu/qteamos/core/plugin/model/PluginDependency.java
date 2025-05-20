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
 * 插件依赖
 * 描述插件之间的依赖关系
 *
 * @author yangqijun
 * @date 2025-05-02
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.plugin.model;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class PluginDependency {
    
    /**
     * 依赖的插件ID
     */
    private String pluginId;
    
    /**
     * 依赖的最低版本要求
     */
    private String minVersion;
    
    /**
     * 依赖的最高版本限制
     */
    private String maxVersion;
    
    /**
     * 是否为必须依赖
     */
    private boolean required;
} 