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
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginStateHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 插件状态变更历史Mapper接口
 *
 * @author yangqijun
 * @date 2024-08-10
 */
@Mapper
public interface SysPluginStateHistoryMapper extends BaseMapper<SysPluginStateHistory> {

    /**
     * 获取插件状态变更历史
     *
     * @param pluginId 插件ID
     * @param limit 限制数量
     * @return 状态变更历史列表
     */
    @Select("SELECT * FROM sys_plugin_state_history WHERE plugin_id = #{pluginId} ORDER BY change_time DESC LIMIT #{limit}")
    List<SysPluginStateHistory> getStateHistory(@Param("pluginId") String pluginId, @Param("limit") int limit);

    /**
     * 获取插件最后一次状态变更记录
     *
     * @param pluginId 插件ID
     * @return 最后一次状态变更记录
     */
    @Select("SELECT * FROM sys_plugin_state_history WHERE plugin_id = #{pluginId} ORDER BY change_time DESC LIMIT 1")
    SysPluginStateHistory getLastStateChange(@Param("pluginId") String pluginId);

    /**
     * 获取处于指定状态的插件列表
     *
     * @param state 状态
     * @return 插件ID列表
     */
    @Select("SELECT DISTINCT plugin_id FROM sys_plugin_state_history WHERE new_state = #{state} AND plugin_id NOT IN (SELECT plugin_id FROM sys_plugin_state_history WHERE new_state != #{state} AND change_time > (SELECT MAX(change_time) FROM sys_plugin_state_history WHERE new_state = #{state} AND plugin_id = sys_plugin_state_history.plugin_id))")
    List<String> getPluginsInState(@Param("state") String state);

    /**
     * 删除插件状态历史记录
     *
     * @param pluginId 插件ID
     */
    void deleteByPluginId(@Param("pluginId") String pluginId);
} 