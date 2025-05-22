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

package com.xiaoqu.qteamos.core.plugin.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginHealthHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 插件健康检查历史记录Mapper接口
 *
 * @author yangqijun
 * @date 2024-08-15
 * @since 1.0.0
 */
@Mapper
public interface SysPluginHealthHistoryMapper extends BaseMapper<SysPluginHealthHistory> {

    /**
     * 获取插件健康检查历史
     *
     * @param pluginId 插件ID
     * @param limit 限制数量
     * @return 健康检查历史列表
     */
    @Select("SELECT * FROM sys_plugin_health_history WHERE plugin_id = #{pluginId} ORDER BY collect_time DESC LIMIT #{limit}")
    List<SysPluginHealthHistory> getHealthHistory(@Param("pluginId") String pluginId, @Param("limit") int limit);

    /**
     * 获取插件最后一次健康检查记录
     *
     * @param pluginId 插件ID
     * @return 最后一次健康检查记录
     */
    @Select("SELECT * FROM sys_plugin_health_history WHERE plugin_id = #{pluginId} ORDER BY collect_time DESC LIMIT 1")
    SysPluginHealthHistory getLastHealthCheck(@Param("pluginId") String pluginId);

    /**
     * 获取指定健康状态的插件列表
     *
     * @param healthy 是否健康
     * @return 插件ID列表
     */
    @Select("SELECT DISTINCT plugin_id FROM sys_plugin_health_history WHERE healthy = #{healthy} AND plugin_id NOT IN (SELECT plugin_id FROM sys_plugin_health_history WHERE healthy != #{healthy} AND collect_time > (SELECT MAX(collect_time) FROM sys_plugin_health_history WHERE healthy = #{healthy} AND plugin_id = sys_plugin_health_history.plugin_id))")
    List<String> getPluginsByHealthStatus(@Param("healthy") boolean healthy);

    /**
     * 删除插件健康检查历史记录
     *
     * @param pluginId 插件ID
     */
    void deleteByPluginId(@Param("pluginId") String pluginId);
} 