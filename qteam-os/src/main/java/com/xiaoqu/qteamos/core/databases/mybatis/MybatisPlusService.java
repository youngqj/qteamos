package com.xiaoqu.qteamos.core.databases.mybatis;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoqu.qteamos.core.databases.annotation.DataSource;
import com.xiaoqu.qteamos.core.databases.core.DataSourceContextHolder;
import com.xiaoqu.qteamos.core.databases.core.DataSourceManager;
import com.xiaoqu.qteamos.core.databases.exception.DatabaseException;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * MyBatis Plus服务类
 * 提供获取指定数据源的Mapper功能
 *
 * @author yangqijun
 * @date 2025-05-03
 */
@Slf4j
@Service
public class MybatisPlusService {

    @Value("${spring.datasource.primary-name:systemDataSource}")
    private String primaryDataSourceName;

    @Autowired
    private DataSourceManager dataSourceManager;

    // 缓存SqlSessionFactory，避免重复创建
    private final Map<String, SqlSessionFactory> sessionFactoryCache = new ConcurrentHashMap<>();

    /**
     * 获取默认数据源的SqlSessionFactory
     *
     * @return SqlSessionFactory
     */
    public SqlSessionFactory getSqlSessionFactory() {
        return getSqlSessionFactory(primaryDataSourceName);
    }

    /**
     * 获取指定数据源的SqlSessionFactory
     *
     * @param dataSourceName 数据源名称
     * @return SqlSessionFactory
     */
    public SqlSessionFactory getSqlSessionFactory(String dataSourceName) {
        return sessionFactoryCache.computeIfAbsent(dataSourceName, name -> {
            try {
                // 从Spring上下文中获取SqlSessionFactory
                // 这里假设使用的是Spring的SqlSessionFactoryBean配置的SqlSessionFactory
                
                // 获取数据源并设置线程上下文
                DataSourceContextHolder.setDataSource(name);
                
                // 获取对应数据源
                javax.sql.DataSource dataSource = dataSourceManager.getDataSource(name);
                if (dataSource == null) {
                    throw new DatabaseException("无法获取数据源: " + name);
                }
                
                // 创建新的SqlSessionFactory
                try {
                    log.info("为数据源{}创建SqlSessionFactory", name);
                    
                    // 使用MyBatis-Spring提供的SqlSessionFactoryBean创建SqlSessionFactory
                    org.mybatis.spring.SqlSessionFactoryBean factoryBean = new org.mybatis.spring.SqlSessionFactoryBean();
                    
                    // 设置数据源
                    factoryBean.setDataSource(dataSource);
                    
                    // 设置MyBatis配置
                    org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
                    configuration.setCacheEnabled(true);
                    configuration.setLazyLoadingEnabled(false);
                    configuration.setMapUnderscoreToCamelCase(true);
                    factoryBean.setConfiguration(configuration);
                    
                    // 设置类型别名包
                    factoryBean.setTypeAliasesPackage("com.xiaoqu.qteamos.**.entity");
                    
                    // 设置映射器位置
                    org.springframework.core.io.Resource[] resources = new org.springframework.core.io.support.PathMatchingResourcePatternResolver()
                            .getResources("classpath*:mapper/**/*.xml");
                    factoryBean.setMapperLocations(resources);
                    
                    // 创建并返回SqlSessionFactory
                    return factoryBean.getObject();
                } catch (Exception e) {
                    log.error("创建SqlSessionFactory失败: {}", name, e);
                    throw new DatabaseException("创建SqlSessionFactory失败: " + e.getMessage(), e);
                }
            } catch (Exception e) {
                log.error("获取SqlSessionFactory失败: {}", name, e);
                throw new DatabaseException("获取SqlSessionFactory失败: " + e.getMessage(), e);
            } finally {
                DataSourceContextHolder.clearDataSource();
            }
        });
    }

    /**
     * 获取默认数据源的Mapper
     *
     * @param mapperClass Mapper接口类
     * @param <T> Mapper类型
     * @return Mapper实例
     */
    public <T> T getMapper(Class<T> mapperClass) {
        return getMapper(mapperClass, primaryDataSourceName);
    }

    /**
     * 获取指定数据源的Mapper
     *
     * @param mapperClass Mapper接口类
     * @param dataSourceName 数据源名称
     * @param <T> Mapper类型
     * @return Mapper实例
     */
    public <T> T getMapper(Class<T> mapperClass, String dataSourceName) {
        try {
            // 设置当前线程数据源
            DataSourceContextHolder.setDataSource(dataSourceName);
            
            // 获取SqlSessionFactory
            SqlSessionFactory sqlSessionFactory = getSqlSessionFactory(dataSourceName);
            if (sqlSessionFactory == null) {
                throw new DatabaseException("无法获取数据源的SqlSessionFactory: " + dataSourceName);
            }
            
            // 创建SqlSession并获取Mapper
            try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
                return sqlSession.getMapper(mapperClass);
            }
        } finally {
            DataSourceContextHolder.clearDataSource();
        }
    }

    /**
     * 创建QueryWrapper
     *
     * @param <T> 实体类型
     * @return QueryWrapper
     */
    public <T> QueryWrapper<T> createQueryWrapper() {
        return new QueryWrapper<>();
    }

    /**
     * 使用指定数据源执行查询
     *
     * @param dataSourceName 数据源名称
     * @param entityClass 实体类
     * @param queryWrapper 查询条件
     * @param <T> 实体类型
     * @return 查询结果
     */
    @DataSource
    public <T> List<T> selectList(String dataSourceName, Class<T> entityClass, Wrapper<T> queryWrapper) {
        try {
            DataSourceContextHolder.setDataSource(dataSourceName);
            
            // 获取对应的SqlSession和Mapper
            SqlSessionFactory sqlSessionFactory = getSqlSessionFactory(dataSourceName);
            if (sqlSessionFactory == null) {
                throw new DatabaseException("无法获取数据源的SqlSessionFactory: " + dataSourceName);
            }
            
            try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
                // 尝试获取BaseMapper
                // 约定Mapper接口命名为 [EntityName]Mapper
                String mapperClassName = entityClass.getPackage().getName() + "." + entityClass.getSimpleName() + "Mapper";
                try {
                    Class<?> mapperClass = Class.forName(mapperClassName);
                    @SuppressWarnings("unchecked")
                    BaseMapper<T> mapper = (BaseMapper<T>) sqlSession.getMapper(mapperClass);
                    return mapper.selectList(queryWrapper);
                } catch (ClassNotFoundException e) {
                    log.error("找不到Mapper类: {}", mapperClassName, e);
                    throw new DatabaseException("找不到Mapper类: " + mapperClassName);
                }
            }
        } finally {
            DataSourceContextHolder.clearDataSource();
        }
    }

    /**
     * 使用指定数据源执行分页查询
     *
     * @param dataSourceName 数据源名称
     * @param page 分页参数
     * @param entityClass 实体类
     * @param queryWrapper 查询条件
     * @param <T> 实体类型
     * @return 分页结果
     */
    @DataSource
    public <T> IPage<T> selectPage(String dataSourceName, Page<T> page, Class<T> entityClass, Wrapper<T> queryWrapper) {
        try {
            DataSourceContextHolder.setDataSource(dataSourceName);
            
            // 获取对应的SqlSession和Mapper
            SqlSessionFactory sqlSessionFactory = getSqlSessionFactory(dataSourceName);
            if (sqlSessionFactory == null) {
                throw new DatabaseException("无法获取数据源的SqlSessionFactory: " + dataSourceName);
            }
            
            try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
                // 尝试获取BaseMapper
                String mapperClassName = entityClass.getPackage().getName() + "." + entityClass.getSimpleName() + "Mapper";
                try {
                    Class<?> mapperClass = Class.forName(mapperClassName);
                    @SuppressWarnings("unchecked")
                    BaseMapper<T> mapper = (BaseMapper<T>) sqlSession.getMapper(mapperClass);
                    return mapper.selectPage(page, queryWrapper);
                } catch (ClassNotFoundException e) {
                    log.error("找不到Mapper类: {}", mapperClassName, e);
                    throw new DatabaseException("找不到Mapper类: " + mapperClassName);
                }
            }
        } finally {
            DataSourceContextHolder.clearDataSource();
        }
    }

    /**
     * 执行带有指定数据源的事务操作
     *
     * @param dataSourceName 数据源名称
     * @param action 事务操作
     * @param <T> 返回类型
     * @return 操作结果
     */
    public <T> T executeTransaction(String dataSourceName, Function<SqlSession, T> action) {
        SqlSession sqlSession = null;
        try {
            // 设置当前线程数据源
            DataSourceContextHolder.setDataSource(dataSourceName);
            
            // 获取SqlSessionFactory
            SqlSessionFactory sqlSessionFactory = getSqlSessionFactory(dataSourceName);
            if (sqlSessionFactory == null) {
                throw new DatabaseException("无法获取数据源的SqlSessionFactory: " + dataSourceName);
            }
            
            // 创建SqlSession并开始事务
            sqlSession = sqlSessionFactory.openSession(false);
            
            try {
                // 执行事务操作
                T result = action.apply(sqlSession);
                
                // 提交事务
                sqlSession.commit();
                
                return result;
            } catch (Exception e) {
                // 回滚事务
                if (sqlSession != null) {
                    sqlSession.rollback();
                }
                throw e;
            }
        } finally {
            // 关闭SqlSession
            if (sqlSession != null) {
                sqlSession.close();
            }
            
            // 清除数据源上下文
            DataSourceContextHolder.clearDataSource();
        }
    }

    /**
     * 使用指定数据源执行插入操作
     *
     * @param dataSourceName 数据源名称
     * @param entity 实体对象
     * @param <T> 实体类型
     * @return 是否成功
     */
    @DataSource
    public <T> boolean insert(String dataSourceName, T entity) {
        try {
            DataSourceContextHolder.setDataSource(dataSourceName);
            
            // 获取对应的SqlSession和Mapper
            SqlSessionFactory sqlSessionFactory = getSqlSessionFactory(dataSourceName);
            if (sqlSessionFactory == null) {
                throw new DatabaseException("无法获取数据源的SqlSessionFactory: " + dataSourceName);
            }
            
            try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
                // 尝试获取BaseMapper
                String mapperClassName = entity.getClass().getPackage().getName() + "." + entity.getClass().getSimpleName() + "Mapper";
                try {
                    Class<?> mapperClass = Class.forName(mapperClassName);
                    @SuppressWarnings("unchecked")
                    BaseMapper<T> mapper = (BaseMapper<T>) sqlSession.getMapper(mapperClass);
                    return mapper.insert(entity) > 0;
                } catch (ClassNotFoundException e) {
                    log.error("找不到Mapper类: {}", mapperClassName, e);
                    throw new DatabaseException("找不到Mapper类: " + mapperClassName);
                }
            }
        } finally {
            DataSourceContextHolder.clearDataSource();
        }
    }

    /**
     * 使用指定数据源执行更新操作
     *
     * @param dataSourceName 数据源名称
     * @param entity 实体对象
     * @param updateWrapper 更新条件
     * @param <T> 实体类型
     * @return 是否成功
     */
    @DataSource
    public <T> boolean update(String dataSourceName, T entity, Wrapper<T> updateWrapper) {
        try {
            DataSourceContextHolder.setDataSource(dataSourceName);
            
            // 获取对应的SqlSession和Mapper
            SqlSessionFactory sqlSessionFactory = getSqlSessionFactory(dataSourceName);
            if (sqlSessionFactory == null) {
                throw new DatabaseException("无法获取数据源的SqlSessionFactory: " + dataSourceName);
            }
            
            try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
                // 尝试获取BaseMapper
                String mapperClassName = entity.getClass().getPackage().getName() + "." + entity.getClass().getSimpleName() + "Mapper";
                try {
                    Class<?> mapperClass = Class.forName(mapperClassName);
                    @SuppressWarnings("unchecked")
                    BaseMapper<T> mapper = (BaseMapper<T>) sqlSession.getMapper(mapperClass);
                    return mapper.update(entity, updateWrapper) > 0;
                } catch (ClassNotFoundException e) {
                    log.error("找不到Mapper类: {}", mapperClassName, e);
                    throw new DatabaseException("找不到Mapper类: " + mapperClassName);
                }
            }
        } finally {
            DataSourceContextHolder.clearDataSource();
        }
    }

    /**
     * 使用指定数据源执行删除操作
     *
     * @param dataSourceName 数据源名称
     * @param entityClass 实体类
     * @param queryWrapper 查询条件
     * @param <T> 实体类型
     * @return 是否成功
     */
    @DataSource
    public <T> boolean delete(String dataSourceName, Class<T> entityClass, Wrapper<T> queryWrapper) {
        try {
            DataSourceContextHolder.setDataSource(dataSourceName);
            
            // 获取对应的SqlSession和Mapper
            SqlSessionFactory sqlSessionFactory = getSqlSessionFactory(dataSourceName);
            if (sqlSessionFactory == null) {
                throw new DatabaseException("无法获取数据源的SqlSessionFactory: " + dataSourceName);
            }
            
            try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
                // 尝试获取BaseMapper
                String mapperClassName = entityClass.getPackage().getName() + "." + entityClass.getSimpleName() + "Mapper";
                try {
                    Class<?> mapperClass = Class.forName(mapperClassName);
                    @SuppressWarnings("unchecked")
                    BaseMapper<T> mapper = (BaseMapper<T>) sqlSession.getMapper(mapperClass);
                    return mapper.delete(queryWrapper) > 0;
                } catch (ClassNotFoundException e) {
                    log.error("找不到Mapper类: {}", mapperClassName, e);
                    throw new DatabaseException("找不到Mapper类: " + mapperClassName);
                }
            }
        } finally {
            DataSourceContextHolder.clearDataSource();
        }
    }
} 