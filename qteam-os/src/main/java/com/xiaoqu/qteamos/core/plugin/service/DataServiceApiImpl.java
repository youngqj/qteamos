package com.xiaoqu.qteamos.core.plugin.service;

import com.xiaoqu.qteamos.core.plugin.error.PluginErrorHandler;
import com.xiaoqu.qteamos.api.core.plugin.api.DataServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 数据服务API实现类
 *
 * @author yangqijun
 * @date 2025-05-01
 */
@Component
public class DataServiceApiImpl implements DataServiceApi {

    private static final Logger log = LoggerFactory.getLogger(DataServiceApiImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    private PluginErrorHandler errorHandler;

    @Autowired
    private PluginServiceApiImpl pluginServiceApi;

    @Autowired
    public DataServiceApiImpl(DataSource dataSource, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public List<Map<String, Object>> query(String sql, Object... params) {
        try {
            log.debug("插件[{}]执行查询: {}", getCurrentPluginId(), sql);
            return jdbcTemplate.queryForList(sql, params);
        } catch (Exception e) {
            log.error("查询数据异常: {}", e.getMessage(), e);
            errorHandler.handlePluginError(getCurrentPluginId(), e, PluginErrorHandler.OperationType.RUNTIME);
            throw e;
        }
    }

    @Override
    public int update(String sql, Object... params) {
        try {
            log.debug("插件[{}]执行更新: {}", getCurrentPluginId(), sql);
            return jdbcTemplate.update(sql, params);
        } catch (Exception e) {
            log.error("更新数据异常: {}", e.getMessage(), e);
            errorHandler.handlePluginError(getCurrentPluginId(), e, PluginErrorHandler.OperationType.RUNTIME);
            throw e;
        }
    }

    @Override
    public Optional<Object> insertAndGetKey(String sql, Object... params) {
        try {
            log.debug("插件[{}]执行插入并返回主键: {}", getCurrentPluginId(), sql);
            
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(createPreparedStatementCreator(sql, params), keyHolder);
            
            Number key = keyHolder.getKey();
            return Optional.ofNullable(key);
        } catch (Exception e) {
            log.error("插入数据并获取主键异常: {}", e.getMessage(), e);
            errorHandler.handlePluginError(getCurrentPluginId(), e, PluginErrorHandler.OperationType.RUNTIME);
            return Optional.empty();
        }
    }

    @Override
    public int[] batchUpdate(String sql, List<Object[]> batchArgs) {
        try {
            log.debug("插件[{}]执行批量更新: {}", getCurrentPluginId(), sql);
            return jdbcTemplate.batchUpdate(sql, batchArgs);
        } catch (Exception e) {
            log.error("批量更新数据异常: {}", e.getMessage(), e);
            errorHandler.handlePluginError(getCurrentPluginId(), e, PluginErrorHandler.OperationType.RUNTIME);
            throw e;
        }
    }

    @Override
    public <T> T executeInTransaction(TransactionAction<T> action) {
        try {
            log.debug("插件[{}]开始执行事务操作", getCurrentPluginId());
            
            return transactionTemplate.execute(new TransactionCallback<T>() {
                @Override
                public T doInTransaction(TransactionStatus status) {
                    try {
                        return action.execute(DataServiceApiImpl.this);
                    } catch (Exception e) {
                        log.error("事务操作异常: {}", e.getMessage(), e);
                        errorHandler.handlePluginError(getCurrentPluginId(), e, PluginErrorHandler.OperationType.RUNTIME);
                        status.setRollbackOnly();
                        throw new RuntimeException("事务操作异常", e);
                    }
                }
            });
        } catch (Exception e) {
            log.error("执行事务异常: {}", e.getMessage(), e);
            errorHandler.handlePluginError(getCurrentPluginId(), e, PluginErrorHandler.OperationType.RUNTIME);
            throw e;
        }
    }

    /**
     * 创建PreparedStatementCreator
     */
    private PreparedStatementCreator createPreparedStatementCreator(String sql, Object... params) {
        return new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                if (params != null) {
                    for (int i = 0; i < params.length; i++) {
                        ps.setObject(i + 1, params[i]);
                    }
                }
                return ps;
            }
        };
    }

    /**
     * 获取当前插件ID
     */
    private String getCurrentPluginId() {
        return pluginServiceApi.getPluginId();
    }
} 