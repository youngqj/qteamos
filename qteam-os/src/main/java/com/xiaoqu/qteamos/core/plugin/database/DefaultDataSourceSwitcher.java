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

package com.xiaoqu.qteamos.core.plugin.database;

import com.xiaoqu.qteamos.core.databases.core.DatabaseService;
import com.xiaoqu.qteamos.core.databases.core.DataSourceContextHolder;
import com.xiaoqu.qteamos.sdk.database.DataSourceSwitcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Supplier;

/**
 * 数据源切换工具默认实现
 * 
 * @author yangqijun
 * @date 2025-01-20
 * @since 1.0.0
 */
@Slf4j
@Service
public class DefaultDataSourceSwitcher implements DataSourceSwitcher {
    
    @Autowired
    private DatabaseService databaseService;
    
    @Override
    public List<String> getAvailableDataSources() {
        return databaseService.getAvailableDataSourceNames();
    }
    
    @Override
    public <T> T executeWith(String dataSourceName, Supplier<T> action) {
        String currentDataSource = DataSourceContextHolder.getDataSource();
        try {
            log.debug("切换到数据源: {}", dataSourceName);
            DataSourceContextHolder.setDataSource(dataSourceName);
            return action.get();
        } finally {
            log.debug("恢复数据源: {}", currentDataSource);
            DataSourceContextHolder.setDataSource(currentDataSource);
        }
    }
    
    @Override
    public void executeWith(String dataSourceName, Runnable action) {
        executeWith(dataSourceName, () -> {
            action.run();
            return null;
        });
    }
} 