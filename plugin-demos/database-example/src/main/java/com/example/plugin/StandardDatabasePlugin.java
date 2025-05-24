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

package com.example.plugin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoqu.qteamos.sdk.database.DataSourceSwitcher;
import com.xiaoqu.qteamos.sdk.plugin.AbstractPlugin;
import com.xiaoqu.qteamos.sdk.plugin.PluginContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 标准数据库插件示例
 * 展示插件开发就是标准的Spring Boot开发 - 直接注入Bean即可
 *
 * @author yangqijun
 * @date 2025-01-20
 * @since 1.0.0
 */
public class StandardDatabasePlugin extends AbstractPlugin {
    
    // 直接注入Mapper - 就像普通Spring Boot开发一样！
    @Autowired
    private UserMapper userMapper;
    
    // 只有需要多数据源时才注入这个工具
    @Autowired
    private DataSourceSwitcher dataSourceSwitcher;
    
    @Override
    public void init(PluginContext context) throws Exception {
        super.init(context);
        
        // 执行插件初始化逻辑
        initPluginData();
        
        log.info("标准数据库插件初始化完成");
    }
    
    /**
     * 初始化插件数据
     */
    private void initPluginData() {
        // 检查是否有初始数据
        long count = userMapper.selectCount(null);
        if (count == 0) {
            log.info("初始化插件用户数据");
            // 插入一些初始数据...
        }
    }
    
    /**
     * 用户管理API - 标准Spring Boot Controller
     */
    @RestController
    @RequestMapping("/api/standard-db/users")
    public class UserController {
        
        /**
         * 获取用户列表 - 使用默认主数据库
         */
        @GetMapping
        public Map<String, Object> getUsers(
                @RequestParam(required = false) String name,
                @RequestParam(required = false) String department) {
            
            try {
                QueryWrapper<User> query = new QueryWrapper<>();
                
                if (name != null && !name.trim().isEmpty()) {
                    query.like("name", name);
                }
                if (department != null && !department.trim().isEmpty()) {
                    query.eq("department", department);
                }
                
                // 直接使用注入的Mapper - 默认使用主数据库
                List<User> users = userMapper.selectList(query);
                
                return Map.of(
                    "success", true,
                    "data", users,
                    "total", users.size(),
                    "dataSource", "default"
                );
                
            } catch (Exception e) {
                log.error("查询用户失败", e);
                return Map.of("success", false, "message", "查询失败: " + e.getMessage());
            }
        }
        
        /**
         * 分页查询用户
         */
        @GetMapping("/page")
        public Map<String, Object> pageUsers(
                @RequestParam(defaultValue = "1") long current,
                @RequestParam(defaultValue = "10") long size) {
            
            try {
                Page<User> page = new Page<>(current, size);
                Page<User> result = userMapper.selectPage(page, null);
                
                return Map.of(
                    "success", true,
                    "data", result.getRecords(),
                    "total", result.getTotal(),
                    "current", result.getCurrent(),
                    "size", result.getSize(),
                    "pages", result.getPages()
                );
                
            } catch (Exception e) {
                log.error("分页查询失败", e);
                return Map.of("success", false, "message", "查询失败: " + e.getMessage());
            }
        }
        
        /**
         * 创建用户
         */
        @PostMapping
        public Map<String, Object> createUser(@RequestBody User user) {
            try {
                user.setCreateTime(LocalDateTime.now());
                user.setUpdateTime(LocalDateTime.now());
                
                int result = userMapper.insert(user);
                
                return Map.of(
                    "success", result > 0,
                    "message", result > 0 ? "创建成功" : "创建失败",
                    "data", user
                );
                
            } catch (Exception e) {
                log.error("创建用户失败", e);
                return Map.of("success", false, "message", "创建失败: " + e.getMessage());
            }
        }
        
        /**
         * 多数据源查询示例 - 只有需要时才使用DataSourceSwitcher
         */
        @GetMapping("/multi-datasource")
        public Map<String, Object> multiDataSourceExample() {
            try {
                // 默认数据源查询
                List<User> defaultUsers = userMapper.selectList(null);
                
                // 切换到业务数据库查询
                List<User> businessUsers = dataSourceSwitcher.executeWith("businessDB", () -> {
                    return userMapper.selectList(null);
                });
                
                // 切换到日志数据库查询
                List<User> logUsers = dataSourceSwitcher.executeWith("logDB", () -> {
                    return userMapper.selectList(null);
                });
                
                return Map.of(
                    "success", true,
                    "dataSources", dataSourceSwitcher.getAvailableDataSources(),
                    "defaultCount", defaultUsers.size(),
                    "businessCount", businessUsers.size(), 
                    "logCount", logUsers.size()
                );
                
            } catch (Exception e) {
                log.error("多数据源查询失败", e);
                return Map.of("success", false, "message", "查询失败: " + e.getMessage());
            }
        }
        
        /**
         * 复杂业务逻辑示例 - 跨数据库事务
         */
        @PostMapping("/complex-business")
        public Map<String, Object> complexBusiness(@RequestBody Map<String, Object> request) {
            try {
                String operation = (String) request.get("operation");
                
                switch (operation) {
                    case "sync_users":
                        // 从主库同步用户到业务库
                        return syncUsersToBusinessDB();
                    case "backup_users":
                        // 备份用户到日志库
                        return backupUsersToLogDB();
                    default:
                        return Map.of("success", false, "message", "未知操作");
                }
                
            } catch (Exception e) {
                log.error("复杂业务操作失败", e);
                return Map.of("success", false, "message", "操作失败: " + e.getMessage());
            }
        }
        
        private Map<String, Object> syncUsersToBusinessDB() {
            // 从主库查询活跃用户
            QueryWrapper<User> query = new QueryWrapper<>();
            query.eq("deleted", false);
            List<User> activeUsers = userMapper.selectList(query);
            
            // 同步到业务库
            dataSourceSwitcher.executeWith("businessDB", () -> {
                for (User user : activeUsers) {
                    // 这里可以做一些业务转换...
                    userMapper.insert(user);
                }
                return null;
            });
            
            return Map.of(
                "success", true,
                "message", "同步完成",
                "syncedCount", activeUsers.size()
            );
        }
        
        private Map<String, Object> backupUsersToLogDB() {
            // 查询所有用户
            List<User> allUsers = userMapper.selectList(null);
            
            // 备份到日志库
            dataSourceSwitcher.executeWith("logDB", () -> {
                // 清空旧数据
                userMapper.delete(null);
                // 插入新数据
                for (User user : allUsers) {
                    userMapper.insert(user);
                }
                return null;
            });
            
            return Map.of(
                "success", true,
                "message", "备份完成",
                "backupCount", allUsers.size()
            );
        }
    }
    
    /**
     * 用户实体类
     */
    public static class User {
        private Long id;
        private String name;
        private String email;
        private String department;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
        private Boolean deleted;
        
        // 构造函数
        public User() {}
        
        public User(String name, String email, String department) {
            this.name = name;
            this.email = email;
            this.department = department;
            this.deleted = false;
        }
        
        // Getter and Setter
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        
        public LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
        
        public LocalDateTime getUpdateTime() { return updateTime; }
        public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
        
        public Boolean getDeleted() { return deleted; }
        public void setDeleted(Boolean deleted) { this.deleted = deleted; }
    }
    
    /**
     * 用户Mapper接口 - 标准MyBatis Plus
     */
    public interface UserMapper extends BaseMapper<User> {
        // 如需自定义SQL，在这里添加方法即可
    }
} 