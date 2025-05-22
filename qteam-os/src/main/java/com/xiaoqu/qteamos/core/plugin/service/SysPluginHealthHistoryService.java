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
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginHealthHistory;

import java.util.List;
import java.util.Optional;

/**
 * 插件健康检查历史服务接口
 *
 * @author yangqijun
 * @date 2024-08-15
 * @since 1.0.0
 */
public interface SysPluginHealthHistoryService extends IService<SysPluginHealthHistory> {

    /**
     * 添加健康检查记录
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param state 插件状态
     * @param healthy 是否健康
     * @param message 健康状态消息
     * @param failCount 失败计数
     * @param checkType 检查类型
     * @return 记录ID
     */
    Long addHealthCheck(String pluginId, String version, String state, 
            boolean healthy, String message, int failCount, String checkType);

    /**
     * 添加健康检查记录（包含资源使用情况）
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param state 插件状态
     * @param healthy 是否健康
     * @param message 健康状态消息
     * @param failCount 失败计数
     * @param memoryUsageMb 内存使用量(MB)
     * @param threadCount 线程数量
     * @param checkType 检查类型
     * @return 记录ID
     */
    Long addHealthCheck(String pluginId, String version, String state, 
            boolean healthy, String message, int failCount, 
            int memoryUsageMb, int threadCount, String checkType);

    /**
     * 获取健康检查历史记录
     *
     * @param pluginId 插件ID
     * @param limit 最大记录数
     * @return 历史记录列表
     */
    List<SysPluginHealthHistory> getHealthHistory(String pluginId, int limit);

    /**
     * 获取最后一次健康检查记录
     *
     * @param pluginId 插件ID
     * @return 健康检查记录
     */
    Optional<SysPluginHealthHistory> getLastHealthCheck(String pluginId);

    /**
     * 获取健康的插件ID列表
     *
     * @return 插件ID列表
     */
    List<String> getHealthyPlugins();

    /**
     * 获取不健康的插件ID列表
     *
     * @return 插件ID列表
     */
    List<String> getUnhealthyPlugins();

    /**
     * 删除插件健康检查历史记录
     *
     * @param pluginId 插件ID
     */
    void deletePluginHealthHistory(String pluginId);

    /**
     * 清除所有健康检查记录
     */
    void clearAllHealthRecords();
} 