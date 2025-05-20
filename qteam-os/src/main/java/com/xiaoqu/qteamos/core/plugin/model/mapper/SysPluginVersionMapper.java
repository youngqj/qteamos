package com.xiaoqu.qteamos.core.plugin.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 插件版本历史Mapper接口
 *
 * @author yangqijun
 * @since 2025-04-28
 */
@Mapper
public interface SysPluginVersionMapper extends BaseMapper<SysPluginVersion> {
    
    /**
     * 根据插件ID查询所有版本
     *
     * @param pluginId 插件ID
     * @return 版本列表
     */
    @Select("SELECT * FROM sys_plugin_version WHERE plugin_id = #{pluginId} ORDER BY record_time DESC")
    List<SysPluginVersion> selectVersionsByPluginId(@Param("pluginId") String pluginId);
    
    /**
     * 查询插件的最新版本
     *
     * @param pluginId 插件ID
     * @return 最新版本记录
     */
    @Select("SELECT * FROM sys_plugin_version WHERE plugin_id = #{pluginId} ORDER BY record_time DESC LIMIT 1")
    SysPluginVersion selectLatestVersion(@Param("pluginId") String pluginId);
    
    /**
     * 查询插件的最新已确认版本
     *
     * @param pluginId 插件ID
     * @return 最新已确认版本记录
     */
    @Select("SELECT * FROM sys_plugin_version WHERE plugin_id = #{pluginId} AND deployed = true AND change_type != 'deprecated' ORDER BY deploy_time DESC LIMIT 1")
    SysPluginVersion selectLatestConfirmedVersion(@Param("pluginId") String pluginId);
} 