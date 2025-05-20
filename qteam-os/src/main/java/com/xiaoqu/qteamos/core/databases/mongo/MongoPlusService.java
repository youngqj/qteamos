package com.xiaoqu.qteamos.core.databases.mongo;

import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MongoDB Plus服务
 * 提供类似MyBatis Plus的API来操作MongoDB
 *
 * @author yangqijun
 * @date 2025-05-03
 */
@Slf4j
@Service
public class MongoPlusService {

    private final Map<String, MongoTemplate> mongoTemplates = new ConcurrentHashMap<>();

    @Autowired
    @Qualifier("primaryMongoTemplate")
    private MongoTemplate primaryMongoTemplate;

    @Autowired(required = false)
    @Qualifier("secondaryMongoTemplate")
    private MongoTemplate secondaryMongoTemplate;

    /**
     * 初始化MongoDB模板映射
     */
    @PostConstruct
    public void init() {
        mongoTemplates.put("primary", primaryMongoTemplate);
        if (secondaryMongoTemplate != null) {
            mongoTemplates.put("secondary", secondaryMongoTemplate);
        }
        log.info("MongoPlusService初始化完成，可用数据源：{}", mongoTemplates.keySet());
    }

    /**
     * 获取默认MongoDB模板
     */
    public MongoTemplate getMongoTemplate() {
        return primaryMongoTemplate;
    }

    /**
     * 获取指定名称的MongoDB模板
     */
    public MongoTemplate getMongoTemplate(String dataSourceName) {
        MongoTemplate template = mongoTemplates.get(dataSourceName);
        if (template == null) {
            throw new IllegalArgumentException("找不到MongoDB数据源: " + dataSourceName);
        }
        return template;
    }

    /**
     * 创建查询对象
     */
    public Query createQuery() {
        return new Query();
    }

    /**
     * 创建更新对象
     */
    public Update createUpdate() {
        return new Update();
    }

    /**
     * 创建条件对象
     */
    public Criteria where(String key) {
        return Criteria.where(key);
    }

    /**
     * 查询列表（默认数据源）
     */
    public <T> List<T> find(Query query, Class<T> entityClass) {
        return primaryMongoTemplate.find(query, entityClass);
    }

    /**
     * 查询列表（指定数据源）
     */
    public <T> List<T> find(Query query, Class<T> entityClass, String dataSourceName) {
        return getMongoTemplate(dataSourceName).find(query, entityClass);
    }

    /**
     * 查询单个对象（默认数据源）
     */
    public <T> T findOne(Query query, Class<T> entityClass) {
        return primaryMongoTemplate.findOne(query, entityClass);
    }

    /**
     * 查询单个对象（指定数据源）
     */
    public <T> T findOne(Query query, Class<T> entityClass, String dataSourceName) {
        return getMongoTemplate(dataSourceName).findOne(query, entityClass);
    }

    /**
     * 分页查询（默认数据源）
     */
    public <T> Page<T> findPage(Query query, Class<T> entityClass, int page, int size) {
        long total = primaryMongoTemplate.count(query, entityClass);
        
        Query pageQuery = Query.of(query)
                .with(PageRequest.of(page, size));
        
        List<T> content = primaryMongoTemplate.find(pageQuery, entityClass);
        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }

    /**
     * 分页查询（指定数据源）
     */
    public <T> Page<T> findPage(Query query, Class<T> entityClass, int page, int size, String dataSourceName) {
        MongoTemplate template = getMongoTemplate(dataSourceName);
        long total = template.count(query, entityClass);
        
        Query pageQuery = Query.of(query)
                .with(PageRequest.of(page, size));
        
        List<T> content = template.find(pageQuery, entityClass);
        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }
    
    /**
     * 插入文档（默认数据源）
     */
    public <T> T insert(T entity) {
        return primaryMongoTemplate.insert(entity);
    }
    
    /**
     * 插入文档（指定数据源）
     */
    public <T> T insert(T entity, String dataSourceName) {
        return getMongoTemplate(dataSourceName).insert(entity);
    }
    
    /**
     * 批量插入文档（默认数据源）
     */
    public <T> List<T> insertAll(List<T> entities, Class<T> entityClass) {
        return new ArrayList<>(primaryMongoTemplate.insertAll(entities));
    }
    
    /**
     * 批量插入文档（指定数据源）
     */
    public <T> List<T> insertAll(List<T> entities, Class<T> entityClass, String dataSourceName) {
        return new ArrayList<>(getMongoTemplate(dataSourceName).insertAll(entities));
    }
    
    /**
     * 更新文档（默认数据源）
     */
    public <T> long updateFirst(Query query, Update update, Class<T> entityClass) {
        return Optional.ofNullable(primaryMongoTemplate.updateFirst(query, update, entityClass))
                .map(result -> result.getModifiedCount())
                .orElse(0L);
    }
    
    /**
     * 更新文档（指定数据源）
     */
    public <T> long updateFirst(Query query, Update update, Class<T> entityClass, String dataSourceName) {
        return Optional.ofNullable(getMongoTemplate(dataSourceName).updateFirst(query, update, entityClass))
                .map(result -> result.getModifiedCount())
                .orElse(0L);
    }
    
    /**
     * 更新多个文档（默认数据源）
     */
    public <T> long updateMulti(Query query, Update update, Class<T> entityClass) {
        return Optional.ofNullable(primaryMongoTemplate.updateMulti(query, update, entityClass))
                .map(result -> result.getModifiedCount())
                .orElse(0L);
    }
    
    /**
     * 更新多个文档（指定数据源）
     */
    public <T> long updateMulti(Query query, Update update, Class<T> entityClass, String dataSourceName) {
        return Optional.ofNullable(getMongoTemplate(dataSourceName).updateMulti(query, update, entityClass))
                .map(result -> result.getModifiedCount())
                .orElse(0L);
    }
    
    /**
     * 删除文档（默认数据源）
     */
    public <T> long remove(Query query, Class<T> entityClass) {
        return Optional.ofNullable(primaryMongoTemplate.remove(query, entityClass))
                .map(result -> result.getDeletedCount())
                .orElse(0L);
    }
    
    /**
     * 删除文档（指定数据源）
     */
    public <T> long remove(Query query, Class<T> entityClass, String dataSourceName) {
        return Optional.ofNullable(getMongoTemplate(dataSourceName).remove(query, entityClass))
                .map(result -> result.getDeletedCount())
                .orElse(0L);
    }
    
    /**
     * 获取Repository接口实现
     * @param repositoryClass Repository接口类
     * @return Repository接口实现
     */
    @SuppressWarnings("unchecked")
    public <T> T getRepository(Class<T> repositoryClass) {
        return getRepository(repositoryClass, "primary");
    }
    
    /**
     * 获取指定数据源的Repository接口实现
     * @param repositoryClass Repository接口类
     * @param dataSourceName 数据源名称
     * @return Repository接口实现
     */
    @SuppressWarnings("unchecked")
    public <T> T getRepository(Class<T> repositoryClass, String dataSourceName) {
        MongoTemplate template = getMongoTemplate(dataSourceName);
        
        // 使用JDK动态代理创建Repository实现
        return (T) Proxy.newProxyInstance(
            repositoryClass.getClassLoader(),
            new Class<?>[] { repositoryClass },
            new MongoRepositoryInvocationHandler(template)
        );
    }
    
    /**
     * MongoDB Repository调用处理器
     */
    private static class MongoRepositoryInvocationHandler implements InvocationHandler {
        
        private final MongoTemplate mongoTemplate;
        
        public MongoRepositoryInvocationHandler(MongoTemplate mongoTemplate) {
            this.mongoTemplate = mongoTemplate;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("toString")) {
                return "MongoRepositoryProxy";
            }
            
            if (method.getName().equals("equals")) {
                return proxy == args[0];
            }
            
            if (method.getName().equals("hashCode")) {
                return System.identityHashCode(proxy);
            }
            
            // 处理其他方法调用
            // 这里可以根据方法名和参数解析具体的MongoDB操作
            // 示例：假设方法名格式如 findByNameAndAge(String name, int age)
            String methodName = method.getName();
            
            // 简单处理，实际情况下需要更复杂的解析逻辑
            // 这里只是一个示例框架
            log.debug("调用MongoDB Repository方法: {}", methodName);
            
            if (methodName.startsWith("findBy")) {
                return handleFindBy(method, args);
            } else if (methodName.startsWith("countBy")) {
                return handleCountBy(method, args);
            } else if (methodName.startsWith("deleteBy")) {
                return handleDeleteBy(method, args);
            }
            
            throw new UnsupportedOperationException("不支持的Repository方法: " + methodName);
        }
        
        private Object handleFindBy(Method method, Object[] args) {
            // 示例实现，实际应用中需要更复杂的解析
            // 解析方法名为查询条件
            Class<?> returnType = method.getReturnType();
            Class<?> entityClass = extractEntityClass(method);
            
            Query query = new Query();
            // 这里应该解析方法名为查询条件
            
            if (List.class.isAssignableFrom(returnType)) {
                return mongoTemplate.find(query, entityClass);
            } else {
                return mongoTemplate.findOne(query, entityClass);
            }
        }
        
        private Object handleCountBy(Method method, Object[] args) {
            // 示例实现
            Class<?> entityClass = extractEntityClass(method);
            Query query = new Query();
            // 解析方法名为查询条件
            
            return mongoTemplate.count(query, entityClass);
        }
        
        private Object handleDeleteBy(Method method, Object[] args) {
            // 示例实现
            Class<?> entityClass = extractEntityClass(method);
            Query query = new Query();
            // 解析方法名为查询条件
            
            return mongoTemplate.remove(query, entityClass).getDeletedCount();
        }
        
        private Class<?> extractEntityClass(Method method) {
            // 实际应用中需要从接口泛型或注解中提取
            // 这里简单返回Document类型
            return Document.class;
        }
    }
} 