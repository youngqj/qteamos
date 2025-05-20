/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-30 20:59:02
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-04-30 20:59:54
 * @FilePath: /qteamos/src/main/java/com/xiaoqu/qteamos/core/plugin/model/mapper/SysPluginUpdateHistoryMapper.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.core.plugin.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginUpdateHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 插件更新历史表Mapper接口
 *
 * @author yangqijun
 * @since 2025-05-05
 */
@Mapper
public interface SysPluginUpdateHistoryMapper extends BaseMapper<SysPluginUpdateHistory> {
    
    /**
     * 查询插件的更新历史
     *
     * @param pluginId 插件ID
     * @return 更新历史列表
     */
    @Select("SELECT * FROM sys_plugin_update_history WHERE plugin_id = #{pluginId} AND deleted = 0 ORDER BY update_time DESC")
    List<SysPluginUpdateHistory> findUpdateHistory(@Param("pluginId") String pluginId);
    
    /**
     * 查询最近的更新历史
     *
     * @param limit 限制数量
     * @return 更新历史列表
     */
    @Select("SELECT * FROM sys_plugin_update_history WHERE deleted = 0 ORDER BY update_time DESC LIMIT #{limit}")
    List<SysPluginUpdateHistory> findRecentUpdates(@Param("limit") int limit);
} 