package com.xiaoqu.qteamos.core.plugin.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginDependency;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 插件依赖关系表Mapper接口
 *
 * @author yangqijun
 * @since 2025-05-05
 */
@Mapper
public interface SysPluginDependencyMapper extends BaseMapper<SysPluginDependency> {
    
    /**
     * 查询依赖于指定插件的插件列表
     *
     * @param pluginId 插件ID
     * @return 依赖列表
     */
    @Select("SELECT * FROM sys_plugin_dependency WHERE dependency_plugin_id = #{pluginId} AND deleted = 0")
    List<SysPluginDependency> findDependentPlugins(@Param("pluginId") String pluginId);
    
    /**
     * 查询插件的所有依赖项
     *
     * @param pluginId 插件ID
     * @param version 版本
     * @return 依赖项列表
     */
    @Select("SELECT * FROM sys_plugin_dependency WHERE plugin_id = #{pluginId} AND plugin_version = #{version} AND deleted = 0")
    List<SysPluginDependency> findDependencies(@Param("pluginId") String pluginId, @Param("version") String version);
} 