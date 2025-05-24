# QTeam插件系统基础设施简化增强方案

## 🎯 项目背景重新定位

**QTeam是面向企业内部的插件化开发框架**，服务场景：
- **开发者都是企业内部人员**，无需防范恶意插件
- **插件最终私有化部署**，重点是开发效率和部署便利性  
- **插件分类管理**：system类型（自动加载）+ normal类型（手工控制）
- **资源共享模式**：插件从壳子获取资源，专注业务逻辑实现

## 🎯 设计目标调整

### ✅ **核心目标**：
1. **简化插件开发**：让写插件像写普通Spring Boot应用一样简单
2. **便捷资源访问**：通过SDK API在运行时选择数据库、缓存等资源
3. **统一开发体验**：提供一致的API和配置方式
4. **基于现有架构**：利用Spring Security、现有数据源管理等

### ❌ **避免的错误设计**：
- ~~在plugin.yml中配置数据库、缓存选择~~
- ~~自定义权限注解（已有Spring Security）~~
- ~~重复的Controller注册机制（已完成）~~
- ~~复杂的配置解析逻辑~~

## 📋 **正确的简化方案**

### 1. **实现PluginDatabaseService**
```java
/**
 * 插件数据库服务实现
 * 基于现有DataSourceManager，提供运行时数据源选择
 */
@Service
public class DefaultPluginDatabaseService implements PluginDatabaseService {
    
    @Autowired
    private DataSourceManager dataSourceManager;
    
    @Autowired
    private SqlSessionFactory sqlSessionFactory;
    
    // 默认使用主数据源
    @Override
    public <T> boolean save(T entity) {
        // 使用默认主数据源的MyBatis Plus操作
        return mybatisPlusService.save(entity);
    }
    
    // 支持选择数据源
    @Override
    public <R> R executeWithDataSource(String dataSourceName, DatabaseAction<R> action) {
        DataSource targetDataSource = dataSourceManager.getDataSource(dataSourceName);
        // 切换数据源执行操作
        return action.execute();
    }
    
    // 其他已设计好的API...
}
```

### 2. **plugin.yml保持简单**
```yaml
# 只包含基本插件信息，不涉及资源配置
pluginId: "user-management"
name: "用户管理插件"
version: "1.0.0"
mainClass: "com.example.UserPlugin"
author: "开发者"
description: "用户管理功能"

# 不需要：
# infrastructure: ...
# permissions: ...
```

### 3. **SDK API增强（重点）**
```java
// 插件基类增强 - 添加便捷方法
public abstract class AbstractPlugin implements Plugin {
    
    // 已有的context...
    
    /**
     * 获取数据库服务（默认主库）
     */
    protected PluginDatabaseService getDatabase() {
        return context.getDatabaseService();
    }
    
    /**
     * 获取指定类型的缓存服务
     */
    protected CacheService getCache(String type) {
        return context.getCacheService(type); // "redis", "caffeine", "file"
    }
    
    /**
     * 获取默认缓存服务
     */
    protected CacheService getCache() {
        return context.getCacheService(); // 使用壳子默认配置
    }
}
```

### 4. **利用现有权限架构**
```java
// 直接使用Spring Security，不需要自定义注解
@RestController
public class UserController {
    
    @GetMapping("/api/users")
    @PreAuthorize("hasRole('USER')") // 标准Spring Security注解
    public List<User> getUsers() {
        return getDatabase().list(User.class);
    }
    
    // 插件Controller已自动注册到主路由，无需额外处理
}
```

## 🚀 **修正后的实施计划**

### 第一阶段：核心实现（1.5-2天）

**目标**：让插件可以实际使用PluginDatabaseService

1. **实现DefaultPluginDatabaseService**（1-1.5天）
   ```java
   // 在qteam-os中实现
   @Service 
   public class DefaultPluginDatabaseService implements PluginDatabaseService {
       @Autowired
       private DataSourceManager dataSourceManager;
       
       // 实现save、list、page等MyBatis Plus风格API
   }
   ```

2. **桥接核心层PluginContext**（0.5天）
   ```java
   // 修改核心层PluginContext实现，添加getDatabaseService()
   @Override
   public PluginDatabaseService getDatabaseService() {
       return applicationContext.getBean(DefaultPluginDatabaseService.class);
   }
   ```

### 第二阶段：完善和测试（0.5天）

1. **更新database-example插件**（0.5天）
   - 展示完整的CRUD操作
   - 展示多数据源使用

## 📝 **具体实现任务**

### ✅ **已完成的工作**：
1. **PluginDatabaseService接口设计** - 完整的MyBatis Plus风格API
2. **多类型缓存支持** - FILE/REDIS/CAFFEINE都已实现
3. **plugin.yml保持简单** - 不需要修改配置解析
4. **SDK层PluginContext** - 已有getDatabaseService()方法

### ❌ **需要实现的工作**：
1. **DefaultPluginDatabaseService核心实现**
2. **核心层PluginContext的getDatabaseService()桥接**
3. **示例插件更新**

## 🎯 **总结**

实际上**大部分架构工作已经完成**，主要缺少的是：
1. **DefaultPluginDatabaseService的具体实现**（最重要）
2. **核心层和SDK层的桥接**

工作量比预期**大幅减少**，从原计划的5-6天缩减到**2-2.5天**即可完成。 