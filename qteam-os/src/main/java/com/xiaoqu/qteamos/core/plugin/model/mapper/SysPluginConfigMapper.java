/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-28 14:11:55
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-05-02 16:13:55
 * @FilePath: /qteamos/src/main/java/com/xiaoqu/qteamos/core/plugin/model/mapper/SysPluginConfigMapper.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
/*
 * Copyright (c) 2023-2025 XiaoQuTeam. All rights reserved.
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

/**
 * 插件配置表Mapper接口
 * 负责处理插件配置的数据库操作
 *
 * @author yangqijun
 * @date 2025-04-28
 * @since 1.0.0
 */

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginConfig;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface SysPluginConfigMapper extends BaseMapper<SysPluginConfig> {
    
    /**
     * 根据插件ID查询配置
     * 
     * @param pluginId 插件ID
     * @return 插件配置列表
     */
    SysPluginConfig getByPluginId(String pluginId);
} 