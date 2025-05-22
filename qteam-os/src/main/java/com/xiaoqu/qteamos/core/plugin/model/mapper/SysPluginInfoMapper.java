package com.xiaoqu.qteamos.core.plugin.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 插件基本信息表Mapper接口
 *
 * @author yangqijun
 * @since 2025-04-28
 */
@Mapper
public interface SysPluginInfoMapper extends BaseMapper<SysPluginInfo> {
    
    /**
     * 根据插件ID查询最新版本插件信息
     *
     * @param pluginId 插件ID
     * @return 插件信息
     */
    @Select("SELECT * FROM sys_plugin_info WHERE plugin_id = #{pluginId} AND deleted = 0 ORDER BY version DESC LIMIT 1")
    SysPluginInfo findLatestVersion(@Param("pluginId") String pluginId);
    
    /**
     * 根据类型查询插件列表
     *
     * @param type 插件类型
     * @return 插件列表
     */
    @Select("SELECT * FROM sys_plugin_info WHERE type = #{type} AND deleted = 0")
    List<SysPluginInfo> findByType(@Param("type") String type);
    
    /**
     * 分页查询所有启用的插件
     *
     * @param page 分页参数
     * @return 分页结果
     */
    @Select("SELECT i.* FROM sys_plugin_info i " +
            "LEFT JOIN sys_plugin_status s ON i.plugin_id = s.plugin_id AND i.version = s.version " +
            "WHERE i.deleted = 0 AND s.enabled = 1 " +
            "ORDER BY i.priority ASC, i.create_time DESC")
    IPage<SysPluginInfo> pageEnabledPlugins(IPage<SysPluginInfo> page);
    
    /**
     * 查询插件详细信息，包括状态
     *
     * @param pluginId 插件ID
     * @param version 版本
     * @return 插件详细信息
     */
    Map<String, Object> selectPluginWithStatus(@Param("pluginId") String pluginId, @Param("version") String version);
    
    /**
     * 获取依赖于指定插件的所有插件
     *
     * @param pluginId 插件ID
     * @return 依赖该插件的插件列表
     */
    List<SysPluginInfo> findDependentPlugins(@Param("pluginId") String pluginId);
    
    /**
     * 根据插件类型和标签分页查询插件
     *
     * @param page 分页参数
     * @param type 插件类型
     * @param category 插件分类
     * @return 分页结果
     */
    IPage<SysPluginInfo> pagePluginsByTypeAndCategory(IPage<SysPluginInfo> page, @Param("type") String type, @Param("category") String category);
    
    /**
     * 搜索插件
     *
     * @param keyword 关键字
     * @return 插件列表
     */
    List<SysPluginInfo> searchPlugins(@Param("keyword") String keyword);
    
    /**
     * 统计各类型插件数量
     *
     * @return 类型-数量映射
     */
    List<Map<String, Object>> countPluginsByType();
    
    /**
     * 联合查询插件信息和状态
     *
     * @return 插件信息和状态的联合结果
     */
    @Select("SELECT i.*, s.enabled, s.status, s.error_message, s.installed_time, " +
            "s.last_start_time, s.last_stop_time, i.jar_file AS jar_file, i.have_dependency AS have_dependency " +
            "FROM sys_plugin_info i " +
            "LEFT JOIN sys_plugin_status s ON i.plugin_id = s.plugin_id AND i.version = s.version " +
            "WHERE i.deleted = 0")
    List<Map<String, Object>> selectPluginsWithStatus();
} 