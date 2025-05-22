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

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginInfo;
import com.xiaoqu.qteamos.core.plugin.model.mapper.SysPluginInfoMapper;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import com.xiaoqu.qteamos.core.plugin.running.PluginDescriptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

/**
 * 插件基本信息服务
 *
 * @author yangqijun
 * @since 2025-05-22
 */
@Service
public class SysPluginInfoService extends ServiceImpl<SysPluginInfoMapper, SysPluginInfo> {
    private static final Logger log = LoggerFactory.getLogger(SysPluginInfoService.class);

    /**
     * 保存插件基本信息
     *
     * @param pluginInfo 插件信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void savePluginInfo(PluginInfo pluginInfo) {
        PluginDescriptor descriptor = pluginInfo.getDescriptor();
        
        // 使用 saveOrUpdate 方法
        SysPluginInfo dbInfo = getOne(
                Wrappers.<SysPluginInfo>lambdaQuery()
                        .eq(SysPluginInfo::getPluginId, descriptor.getPluginId()));
        
        if (dbInfo == null) {
            dbInfo = new SysPluginInfo();
        }
        
        // 设置插件基本信息
        dbInfo.setPluginId(descriptor.getPluginId())
                .setName(descriptor.getName())
                .setVersion(descriptor.getVersion())
                .setDescription(descriptor.getDescription())
                .setAuthor(descriptor.getAuthor())
                .setMainClass(descriptor.getMainClass())
                .setType(descriptor.getType())
                .setTrust(descriptor.getTrust().toString())
                .setRequiredSystemVersion(descriptor.getRequiredSystemVersion())
                .setPriority(descriptor.getPriority());
        
        saveOrUpdate(dbInfo);
        log.info("保存/更新插件基本信息: {}", descriptor.getPluginId());
    }

    /**
     * 获取所有已安装的插件信息
     *
     * @return 插件信息列表
     */
    public List<SysPluginInfo> getAllInstalledPlugins() {
        return list();
    }
} 