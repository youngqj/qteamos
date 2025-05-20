package com.xiaoqu.qteamos.core.databases.example;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoqu.qteamos.core.databases.mybatis.MybatisPlusService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MyBatis Plus示例服务
 * 展示插件中使用MybatisPlusService的方法
 * 
 * 支持两种使用方式：
 * 1. 原生方式：直接注入Mapper接口，自动使用默认数据源
 *    - 优点：使用简单，代码简洁
 *    - 缺点：只能访问默认数据源
 *    - 示例：@Autowired private UserMapper userMapper;
 *    
 * 2. 服务方式：通过MybatisPlusService获取Mapper
 *    - 优点：可以动态切换数据源
 *    - 缺点：代码略微复杂
 *    - 示例：UserMapper mapper = mybatisPlusService.getMapper(UserMapper.class, "businessDB");
 *
 * @author yangqijun
 * @date 2025-05-03
 */
@Slf4j
@Component
public class MybatisPlusExampleService {

    @Autowired
    private MybatisPlusService mybatisPlusService;

    // 假设直接注入的Mapper（原生方式）
    // @Autowired
    // private UserMapper userMapper;

    /**
     * 运行MyBatis Plus示例
     */
    public void runMybatisPlusExamples() {
        log.info("=== 运行MyBatis Plus示例 ===");

        // 展示两种使用方式
        nativeMybatisPlusExample();
        serviceMybatisPlusExample();
        multiDataSourceExample();
        
        log.info("=== MyBatis Plus示例完成 ===");
    }

    /**
     * 原生MyBatis Plus方式示例
     * 直接注入Mapper接口使用
     */
    private void nativeMybatisPlusExample() {
        log.info("--- 原生MyBatis Plus方式示例 ---");
        
        try {
            // 注意：这里假设有一个直接注入的UserMapper
            // 在实际测试中，需要确保有对应的Mapper接口和实体类
            
            // 基本查询
            // List<User> users = userMapper.selectList(null);
            // log.info("查询到用户数: {}", users.size());
            
            // 条件查询
            // QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            // queryWrapper.eq("status", "active");
            // List<User> activeUsers = userMapper.selectList(queryWrapper);
            // log.info("查询到活跃用户数: {}", activeUsers.size());
            
            // 分页查询
            // Page<User> page = new Page<>(1, 10);
            // IPage<User> userPage = userMapper.selectPage(page, null);
            // log.info("分页查询结果: 总数={}, 当前页数据数={}", 
            //         userPage.getTotal(), userPage.getRecords().size());
            
            log.info("原生MyBatis Plus方式示例 - 注释了实际代码，需要根据实际环境配置");
            
        } catch (Exception e) {
            log.error("原生MyBatis Plus方式示例执行失败", e);
        }
    }

    /**
     * 服务方式MyBatis Plus示例
     * 通过MybatisPlusService获取Mapper
     */
    private void serviceMybatisPlusExample() {
        log.info("--- 服务方式MyBatis Plus示例 ---");
        
        try {
            // 通过服务获取默认数据源的Mapper
            // UserMapper mapper = mybatisPlusService.getMapper(UserMapper.class);
            
            // 使用获取的Mapper执行操作
            // List<User> users = mapper.selectList(null);
            // log.info("通过服务查询到用户数: {}", users.size());
            
            // 条件查询
            // QueryWrapper<User> queryWrapper = mybatisPlusService.createQueryWrapper();
            // queryWrapper.eq("status", "active");
            // List<User> activeUsers = mapper.selectList(queryWrapper);
            // log.info("通过服务查询到活跃用户数: {}", activeUsers.size());
            
            // 还可以直接使用服务类提供的方法进行操作
            // 例如，使用泛型方法进行查询，不需要直接处理Mapper
            // List<User> users = mybatisPlusService.selectList("primaryDataSource", User.class, queryWrapper);
            // log.info("使用服务方法查询到用户数: {}", users.size());
            
            log.info("服务方式MyBatis Plus示例 - 注释了实际代码，需要根据实际环境配置");
            
        } catch (Exception e) {
            log.error("服务方式MyBatis Plus示例执行失败", e);
        }
    }

    /**
     * 多数据源示例
     * 演示如何在不同数据源间切换
     */
    private void multiDataSourceExample() {
        log.info("--- 多数据源示例 ---");
        
        try {
            // 从默认数据源查询
            // UserMapper defaultMapper = mybatisPlusService.getMapper(UserMapper.class);
            // List<User> defaultUsers = defaultMapper.selectList(null);
            // log.info("默认数据源用户数: {}", defaultUsers.size());
            
            // 从业务数据源查询
            // 假设有一个名为"businessDB"的数据源
            try {
                // UserMapper businessMapper = mybatisPlusService.getMapper(UserMapper.class, "businessDB");
                // List<User> businessUsers = businessMapper.selectList(null);
                // log.info("业务数据源用户数: {}", businessUsers.size());
                
                // 注意：第一次调用getMapper方法时会创建SqlSessionFactory并缓存
                // 后续对同一数据源的调用会复用缓存的SqlSessionFactory
            } catch (Exception e) {
                log.info("业务数据源未配置，跳过测试: {}", e.getMessage());
            }
            
            // 使用事务处理
            try {
                // mybatisPlusService.executeTransaction("businessDB", sqlSession -> {
                //     UserMapper transMapper = sqlSession.getMapper(UserMapper.class);
                //     
                //     // 在事务中执行多个操作
                //     User newUser = new User();
                //     newUser.setUsername("transactionUser");
                //     newUser.setStatus("active");
                //     transMapper.insert(newUser);
                //     
                //     // 更新其他记录
                //     User updateUser = new User();
                //     updateUser.setId(1L);
                //     updateUser.setStatus("inactive");
                //     transMapper.updateById(updateUser);
                //     
                //     return true;
                // });
                // log.info("事务执行成功");
            } catch (Exception e) {
                log.info("事务执行失败: {}", e.getMessage());
            }
            
            log.info("多数据源示例 - 注释了实际代码，需要根据实际环境配置");
            
        } catch (Exception e) {
            log.error("多数据源示例执行失败", e);
        }
    }
    
    /**
     * 用户实体类（示例）
     */
    public static class User {
        private Long id;
        private String username;
        private String status;
        
        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
    
    /**
     * 用户Mapper接口（示例）
     */
    public interface UserMapper {
        List<User> selectList(QueryWrapper<User> queryWrapper);
        IPage<User> selectPage(Page<User> page, QueryWrapper<User> queryWrapper);
        int insert(User user);
        int updateById(User user);
    }
} 