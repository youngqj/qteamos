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

package com.xiaoqu.qteamos.core.plugin.event.plugins;

import com.xiaoqu.qteamos.core.plugin.event.PluginEvent;
import com.xiaoqu.qteamos.api.core.plugin.model.PluginCandidate;

/**
 * 插件发现事件
 * 当扫描器发现新的插件候选者时触发
 *
 * @author yangqijun
 * @date 2025-05-25
 * @since 1.0.0
 */
public class PluginDiscoveredEvent extends PluginEvent {
    
    private final PluginCandidate candidate;
    
    /**
     * 创建插件发现事件
     *
     * @param candidate 发现的插件候选者
     */
    public PluginDiscoveredEvent(PluginCandidate candidate) {
        super("discovered", candidate.getPluginId(), null);
        this.candidate = candidate;
    }
    
    /**
     * 获取插件候选者
     *
     * @return 插件候选者
     */
    public PluginCandidate getCandidate() {
        return candidate;
    }
    
    @Override
    public String toString() {
        return "PluginDiscoveredEvent{" +
                "candidate=" + candidate +
                '}';
    }
} 