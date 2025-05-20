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

/**
 * 插件部署历史服务实现类
 * 负责记录插件部署、发布和回滚的历史记录
 *
 * @author yangqijun
 * @date 2025-07-01
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.plugin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoqu.qteamos.core.plugin.manager.PluginReleaseManager.ReleaseStatus;
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginUpdateHistory;
import com.xiaoqu.qteamos.core.plugin.model.mapper.SysPluginUpdateHistoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PluginDeploymentHistoryServiceImpl implements PluginDeploymentHistoryService {
    
    private static final Logger log = LoggerFactory.getLogger(PluginDeploymentHistoryServiceImpl.class);
    
    @Autowired
    private SysPluginUpdateHistoryMapper updateHistoryMapper;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    public void recordDeployment(String pluginId, String version, boolean success, String message) {
        log.info("记录插件部署: 插件[{}], 版本[{}], 成功[{}], 消息[{}]", pluginId, version, success, message);
        
        SysPluginUpdateHistory history = new SysPluginUpdateHistory();
        history.setPluginId(pluginId);
        history.setTargetVersion(version);
        history.setUpdateLog("DEPLOY");
        history.setUpdateModifyTime(LocalDateTime.now());
        history.setStatus(success ? "SUCCESS" : "FAILED");
        history.setErrorMessage(success ? null : message);
        
        try {
            updateHistoryMapper.insert(history);
        } catch (Exception e) {
            log.error("记录插件部署历史失败", e);
        }
    }
    
    @Override
    public void recordStatusChange(String pluginId, String version, ReleaseStatus status) {
        log.info("记录状态变更: 插件[{}], 版本[{}], 状态[{}]", pluginId, version, status);
        
        SysPluginUpdateHistory history = new SysPluginUpdateHistory();
        history.setPluginId(pluginId);
        history.setTargetVersion(version);
        history.setUpdateLog("STATUS_CHANGE");
        history.setUpdateModifyTime(LocalDateTime.now());
        history.setStatus("SUCCESS");
        
        // 将状态信息保存到元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("status", status.name());
        
        try {
            String metadataJson = objectMapper.writeValueAsString(metadata);
            history.setUpdateLog(history.getUpdateLog() + ": " + metadataJson);
            updateHistoryMapper.insert(history);
        } catch (Exception e) {
            log.error("记录状态变更历史失败", e);
        }
    }
    
    @Override
    public void recordRolloutProgress(String pluginId, String version, int batch, int percentage, boolean success) {
        log.info("记录灰度发布进度: 插件[{}], 版本[{}], 批次[{}], 百分比[{}], 成功[{}]", 
                pluginId, version, batch, percentage, success);
        
        SysPluginUpdateHistory history = new SysPluginUpdateHistory();
        history.setPluginId(pluginId);
        history.setTargetVersion(version);
        history.setUpdateLog("ROLLOUT_PROGRESS");
        history.setUpdateModifyTime(LocalDateTime.now());
        history.setStatus(success ? "SUCCESS" : "FAILED");
        
        // 将灰度进度信息保存到元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("batch", batch);
        metadata.put("percentage", percentage);
        
        try {
            String metadataJson = objectMapper.writeValueAsString(metadata);
            history.setUpdateLog(history.getUpdateLog() + ": " + metadataJson);
            updateHistoryMapper.insert(history);
        } catch (Exception e) {
            log.error("记录灰度进度历史失败", e);
        }
    }
    
    @Override
    public void recordConfirmation(String pluginId, String version) {
        log.info("记录版本确认: 插件[{}], 版本[{}]", pluginId, version);
        
        SysPluginUpdateHistory history = new SysPluginUpdateHistory();
        history.setPluginId(pluginId);
        history.setTargetVersion(version);
        history.setUpdateLog("CONFIRM");
        history.setUpdateModifyTime(LocalDateTime.now());
        history.setStatus("SUCCESS");
        
        try {
            updateHistoryMapper.insert(history);
        } catch (Exception e) {
            log.error("记录版本确认历史失败", e);
        }
    }
    
    @Override
    public void recordRejection(String pluginId, String version, String rollbackVersion) {
        log.info("记录版本拒绝: 插件[{}], 版本[{}], 回滚到[{}]", pluginId, version, rollbackVersion);
        
        SysPluginUpdateHistory history = new SysPluginUpdateHistory();
        history.setPluginId(pluginId);
        history.setPreviousVersion(version);
        history.setTargetVersion(rollbackVersion);
        history.setUpdateLog("REJECT_ROLLBACK");
        history.setUpdateModifyTime(LocalDateTime.now());
        history.setStatus("SUCCESS");
        
        // 将回滚信息保存到元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("rejectedVersion", version);
        
        try {
            String metadataJson = objectMapper.writeValueAsString(metadata);
            history.setUpdateLog(history.getUpdateLog() + ": " + metadataJson);
            updateHistoryMapper.insert(history);
        } catch (Exception e) {
            log.error("记录版本拒绝历史失败", e);
        }
    }
    
    @Override
    public void recordDeprecation(String pluginId, String version) {
        log.info("记录版本弃用: 插件[{}], 版本[{}]", pluginId, version);
        
        SysPluginUpdateHistory history = new SysPluginUpdateHistory();
        history.setPluginId(pluginId);
        history.setTargetVersion(version);
        history.setUpdateLog("DEPRECATE");
        history.setUpdateModifyTime(LocalDateTime.now());
        history.setStatus("SUCCESS");
        
        try {
            updateHistoryMapper.insert(history);
        } catch (Exception e) {
            log.error("记录版本弃用历史失败", e);
        }
    }
    
    @Override
    public List<Map<String, Object>> getDeploymentHistory(String pluginId, int limit) {
        log.debug("获取插件部署历史: 插件[{}], 限制[{}]", pluginId, limit);
        
        LambdaQueryWrapper<SysPluginUpdateHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysPluginUpdateHistory::getPluginId, pluginId)
                   .likeRight(SysPluginUpdateHistory::getUpdateLog, "INSTALL")
                   .or()
                   .likeRight(SysPluginUpdateHistory::getUpdateLog, "UPGRADE")
                   .or()
                   .likeRight(SysPluginUpdateHistory::getUpdateLog, "DOWNGRADE")
                   .or()
                   .likeRight(SysPluginUpdateHistory::getUpdateLog, "DEPLOY")
                   .orderByDesc(SysPluginUpdateHistory::getUpdateModifyTime)
                   .last(limit > 0, "LIMIT " + limit);
        
        List<SysPluginUpdateHistory> records = updateHistoryMapper.selectList(queryWrapper);
        
        return convertToMapList(records);
    }
    
    @Override
    public List<Map<String, Object>> getStatusChangeHistory(String pluginId, String version) {
        log.debug("获取插件状态变更历史: 插件[{}], 版本[{}]", pluginId, version);
        
        LambdaQueryWrapper<SysPluginUpdateHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysPluginUpdateHistory::getPluginId, pluginId)
                   .eq(SysPluginUpdateHistory::getTargetVersion, version)
                   .and(w -> w.likeRight(SysPluginUpdateHistory::getUpdateLog, "STATUS_CHANGE")
                           .or()
                           .likeRight(SysPluginUpdateHistory::getUpdateLog, "CONFIRM")
                           .or()
                           .likeRight(SysPluginUpdateHistory::getUpdateLog, "REJECT_ROLLBACK")
                           .or()
                           .likeRight(SysPluginUpdateHistory::getUpdateLog, "DEPRECATE"))
                   .orderByDesc(SysPluginUpdateHistory::getUpdateModifyTime);
        
        List<SysPluginUpdateHistory> records = updateHistoryMapper.selectList(queryWrapper);
        
        return convertToMapList(records);
    }
    
    /**
     * 将实体列表转换为Map列表
     */
    private List<Map<String, Object>> convertToMapList(List<SysPluginUpdateHistory> records) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (SysPluginUpdateHistory record : records) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", record.getId());
            map.put("pluginId", record.getPluginId());
            map.put("fromVersion", record.getPreviousVersion());
            map.put("toVersion", record.getTargetVersion());
            map.put("updateType", record.getUpdateLog());
            map.put("updateTime", record.getUpdateModifyTime());
            map.put("success", "SUCCESS".equals(record.getStatus()));
            map.put("errorMessage", record.getErrorMessage());
            
            // 尝试解析updateLog中的JSON部分
            String updateLog = record.getUpdateLog();
            if (updateLog != null && updateLog.contains(": {")) {
                try {
                    String jsonPart = updateLog.substring(updateLog.indexOf(": {") + 1);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> metadata = objectMapper.readValue(jsonPart, Map.class);
                    map.put("metadata", metadata);
                } catch (Exception e) {
                    log.warn("解析更新日志失败: {}", updateLog);
                }
            }
            
            result.add(map);
        }
        
        return result;
    }
} 