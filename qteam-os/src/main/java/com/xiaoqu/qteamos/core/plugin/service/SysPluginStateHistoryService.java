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

package com.xiaoqu.qteamos.core.plugin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginStateHistory;

import java.util.List;
import java.util.Optional;

/**
 * 插件状态变更历史服务接口
 *
 * @author yangqijun
 * @date 2024-08-10
 */
public interface SysPluginStateHistoryService extends IService<SysPluginStateHistory> {

    /**
     * 添加状态变更记录
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param oldState 旧状态
     * @param newState 新状态
     * @param message 附加信息
     * @return 记录ID
     */
    Long addStateHistory(String pluginId, String version, String oldState, String newState, String message);

    /**
     * 获取状态变更历史记录
     *
     * @param pluginId 插件ID
     * @param limit 最大记录数
     * @return 历史记录列表
     */
    List<SysPluginStateHistory> getStateHistory(String pluginId, int limit);

    /**
     * 获取最后一次状态变更记录
     *
     * @param pluginId 插件ID
     * @return 状态变更记录
     */
    Optional<SysPluginStateHistory> getLastStateChange(String pluginId);

    /**
     * 获取处于指定状态的插件ID列表
     *
     * @param state 状态
     * @return 插件ID列表
     */
    List<String> getPluginsInState(String state);

    /**
     * 获取失败状态的插件ID列表
     *
     * @return 插件ID列表
     */
    List<String> getFailedPlugins();

    /**
     * 删除插件状态历史记录
     *
     * @param pluginId 插件ID
     */
    void deletePluginStateHistory(String pluginId);

    /**
     * 清除所有状态记录
     */
    void clearAllStateRecords();
} 