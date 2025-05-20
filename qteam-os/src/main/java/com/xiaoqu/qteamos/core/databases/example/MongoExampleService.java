package com.xiaoqu.qteamos.core.databases.example;

import com.xiaoqu.qteamos.core.databases.mongo.MongoPlusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * MongoDB示例服务
 * 演示如何在插件中使用MongoPlusService
 *
 * @author yangqijun
 * @date 2025-05-03
 */
@Slf4j
@Component
public class MongoExampleService {

    @Autowired
    private MongoPlusService mongoPlusService;
    
    /**
     * 运行MongoDB示例
     */
    public void runMongoExamples() {
        log.info("=== 运行MongoDB示例 ===");
        
        // 基本查询示例
        basicQueryExample();
        
        // 高级查询示例
        advancedQueryExample();
        
        // 增删改示例
        crudExample();
        
        // 多数据源示例
        multiDataSourceExample();
        
        // Repository示例
        repositoryExample();
        
        log.info("=== MongoDB示例完成 ===");
    }
    
    /**
     * 基本查询示例
     */
    private void basicQueryExample() {
        log.info("--- 基本查询示例 ---");
        
        try {
            // 创建查询条件
            Query query = mongoPlusService.createQuery();
            query.addCriteria(Criteria.where("status").is("active"));
            
            // 执行查询
            List<Map> users = mongoPlusService.find(query, Map.class);
            log.info("查询到用户数: {}", users.size());
            
            // 查询单个文档
            Map user = mongoPlusService.findOne(
                    mongoPlusService.createQuery().addCriteria(
                            Criteria.where("username").is("admin")), 
                    Map.class);
            log.info("查询到用户: {}", user);
            
        } catch (Exception e) {
            log.error("基本查询示例执行失败", e);
        }
    }
    
    /**
     * 高级查询示例
     */
    private void advancedQueryExample() {
        log.info("--- 高级查询示例 ---");
        
        try {
            // 创建复杂查询条件
            Query query = mongoPlusService.createQuery();
            query.addCriteria(
                    new Criteria().andOperator(
                            Criteria.where("age").gt(18),
                            Criteria.where("status").is("active"),
                            Criteria.where("createTime").gt(new Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000))
                    )
            );
            
            // 添加排序
            query.with(org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Direction.DESC, "createTime"));
            
            // 分页查询
            Page<Map> userPage = mongoPlusService.findPage(query, Map.class, 0, 10);
            log.info("分页查询结果: 总数={}, 当前页数据数={}", 
                    userPage.getTotalElements(), userPage.getContent().size());
            
        } catch (Exception e) {
            log.error("高级查询示例执行失败", e);
        }
    }
    
    /**
     * 增删改示例
     */
    private void crudExample() {
        log.info("--- 增删改示例 ---");
        
        try {
            // 插入文档
            UserInfo user = new UserInfo();
            user.setUsername("testuser");
            user.setEmail("test@example.com");
            user.setAge(25);
            user.setStatus("active");
            user.setCreateTime(new Date());
            
            UserInfo savedUser = mongoPlusService.insert(user);
            log.info("插入用户成功: id={}", savedUser.getId());
            
            // 更新文档
            Query updateQuery = mongoPlusService.createQuery()
                    .addCriteria(Criteria.where("username").is("testuser"));
            
            Update update = mongoPlusService.createUpdate()
                    .set("age", 26)
                    .set("lastLoginTime", new Date());
            
            long updateCount = mongoPlusService.updateFirst(updateQuery, update, UserInfo.class);
            log.info("更新用户数: {}", updateCount);
            
            // 批量插入
            List<UserInfo> users = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                UserInfo u = new UserInfo();
                u.setUsername("batchuser" + i);
                u.setEmail("batch" + i + "@example.com");
                u.setAge(20 + i);
                u.setStatus("active");
                u.setCreateTime(new Date());
                users.add(u);
            }
            
            List<UserInfo> savedUsers = mongoPlusService.insertAll(users, UserInfo.class);
            log.info("批量插入用户数: {}", savedUsers.size());
            
            // 删除文档
            Query removeQuery = mongoPlusService.createQuery()
                    .addCriteria(Criteria.where("username").regex("^batchuser"));
            
            long removeCount = mongoPlusService.remove(removeQuery, UserInfo.class);
            log.info("删除用户数: {}", removeCount);
            
        } catch (Exception e) {
            log.error("增删改示例执行失败", e);
        }
    }
    
    /**
     * 多数据源示例
     */
    private void multiDataSourceExample() {
        log.info("--- 多数据源示例 ---");
        
        try {
            // 使用主数据源
            List<Map> primaryUsers = mongoPlusService.find(
                    mongoPlusService.createQuery().addCriteria(
                            Criteria.where("status").is("active")), 
                    Map.class);
            log.info("主数据源用户数: {}", primaryUsers.size());
            
            // 使用次要数据源
            try {
                List<Map> secondaryLogs = mongoPlusService.find(
                        mongoPlusService.createQuery().addCriteria(
                                Criteria.where("level").is("ERROR")), 
                        Map.class,
                        "secondary");
                log.info("次要数据源日志数: {}", secondaryLogs.size());
            } catch (IllegalArgumentException e) {
                log.info("次要数据源未配置，跳过测试");
            }
            
        } catch (Exception e) {
            log.error("多数据源示例执行失败", e);
        }
    }
    
    /**
     * Repository示例
     */
    private void repositoryExample() {
        log.info("--- Repository示例 ---");
        
        try {
            // 获取Repository
            UserRepository userRepo = mongoPlusService.getRepository(UserRepository.class);
            
            // 调用Repository方法
            // 注意：这里只是演示，实际实现需要更完善的代理处理逻辑
            try {
                List<UserInfo> users = userRepo.findByStatus("active");
                log.info("通过Repository查询到用户数: {}", users.size());
                
                UserInfo user = userRepo.findByUsername("admin");
                log.info("通过Repository查询到用户: {}", user);
                
                long count = userRepo.countByStatus("active");
                log.info("通过Repository查询到活跃用户数: {}", count);
            } catch (UnsupportedOperationException e) {
                log.info("Repository方法未完全实现: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("Repository示例执行失败", e);
        }
    }
    
    /**
     * 用户信息实体类
     */
    public static class UserInfo {
        private String id;
        private String username;
        private String email;
        private Integer age;
        private String status;
        private Date createTime;
        private Date lastLoginTime;
        
        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Date getCreateTime() { return createTime; }
        public void setCreateTime(Date createTime) { this.createTime = createTime; }
        
        public Date getLastLoginTime() { return lastLoginTime; }
        public void setLastLoginTime(Date lastLoginTime) { this.lastLoginTime = lastLoginTime; }
        
        @Override
        public String toString() {
            return "UserInfo{" +
                    "id='" + id + '\'' +
                    ", username='" + username + '\'' +
                    ", email='" + email + '\'' +
                    ", age=" + age +
                    ", status='" + status + '\'' +
                    ", createTime=" + createTime +
                    ", lastLoginTime=" + lastLoginTime +
                    '}';
        }
    }
    
    /**
     * 用户Repository接口
     * 演示如何通过方法名约定定义MongoDB查询操作
     */
    public interface UserRepository {
        
        /**
         * 根据状态查询用户列表
         */
        List<UserInfo> findByStatus(String status);
        
        /**
         * 根据用户名查询单个用户
         */
        UserInfo findByUsername(String username);
        
        /**
         * 根据邮箱和状态查询用户列表
         */
        List<UserInfo> findByEmailAndStatus(String email, String status);
        
        /**
         * 根据年龄范围查询用户列表
         */
        List<UserInfo> findByAgeBetween(int minAge, int maxAge);
        
        /**
         * 统计特定状态的用户数量
         */
        long countByStatus(String status);
        
        /**
         * 删除指定用户名的用户
         */
        long deleteByUsername(String username);
    }
} 