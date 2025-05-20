/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-28 14:11:32
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-05-05 01:26:52
 * @FilePath: /QTeam/qteam-os/src/main/java/com/xiaoqu/qteamos/core/plugin/model/mapper/SysPluginStatusMapper.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.core.plugin.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 插件状态表Mapper接口
 *
 * @author yangqijun
 * @since 2025-05-05
 */
@Mapper
public interface SysPluginStatusMapper extends BaseMapper<SysPluginStatus> {
    
    /**
     * 根据插件ID查询最新状态
     *
     * @param pluginId 插件ID
     * @return 插件状态
     */
    @Select("SELECT * FROM sys_plugin_status WHERE plugin_id = #{pluginId} AND deleted = 0 ORDER BY version DESC LIMIT 1")
    SysPluginStatus findLatestStatus(@Param("pluginId") String pluginId);
    
    /**
     * 查询所有启用的插件状态
     *
     * @return 启用的插件状态列表
     */
    @Select("SELECT * FROM sys_plugin_status WHERE enabled = 1 AND deleted = 0")
    List<SysPluginStatus> findAllEnabled();
} 