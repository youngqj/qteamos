package com.xiaoqu.qteamos.core.plugin.monitoring.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoqu.qteamos.core.plugin.monitoring.entity.PluginResourceUsage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 插件资源使用历史记录Mapper接口
 *
 * @author yangqijun
 * @date 2025-05-02
 */
@Mapper
public interface PluginResourceUsageMapper extends BaseMapper<PluginResourceUsage> {
} 