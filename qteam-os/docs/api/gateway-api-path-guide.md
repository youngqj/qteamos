<!--
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-05-03 10:40:15
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-05-03 10:40:15
 * @FilePath: /qteamos/docs/api/gateway-api-path-guide.md
 * @Description: QTeamOS网关层API路径规范指南
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
-->

# QTeamOS网关层API路径规范指南

## 1. 概述

本文档详细说明QTeamOS网关层的API路径规范，旨在帮助插件开发者正确构建和使用API路径，确保插件API能够无缝集成到QTeamOS平台。随着QTeamOS网关层的优化与重构，API路径规范进行了统一和标准化处理，本指南将帮助您理解和遵循这些规范。

### 1.1 网关层功能简介

QTeamOS网关层是连接外部请求与插件API的桥梁，提供以下核心功能：

- **统一访问入口**：所有插件API通过统一前缀访问
- **自动路由**：将请求自动路由到对应插件的控制器
- **权限控制**：基于角色和权限的访问控制
- **请求监控**：API调用统计和性能监控
- **安全防护**：请求验证、限流等安全机制

### 1.2 最近变更（2025-05-03）

网关层最近完成的重要优化：

- API前缀由`/plugins/`统一调整为`/api/`
- 简化了路径映射处理逻辑
- 增强了插件控制器发现机制
- 统一了API路径规范和命名约定

## 2. API路径结构

### 2.1 标准路径格式

QTeamOS中的API路径遵循以下标准格式：

```
/{api-prefix}/{pluginInterface-id}/{controller-path}
```

其中：
- **{api-prefix}**: 全局API前缀，默认为`/api`
- **{pluginInterface-id}**: 插件唯一标识符
- **{controller-path}**: 控制器内定义的路径

例如：对于ID为`user-manager`的插件中定义了`/users/{id}`路径的控制器，完整访问路径为：
```
/api/user-manager/users/123
```

### 2.2 特殊路径前缀

为了区分不同类型的API，QTeamOS定义了以下特殊路径前缀：

#### 2.2.1 公共API路径

公共API是指无需认证即可访问的接口，使用`pub`前缀：

```
/api/{pluginInterface-id}/pub/{path}
```

示例：
```
/api/user-manager/pub/statistics
```

#### 2.2.2 管理API路径

管理API是指需要管理员权限的接口，使用`admin`前缀：

```
/api/{pluginInterface-id}/admin/{path}
```

示例：
```
/api/user-manager/admin/settings
```

#### 2.2.3 API版本控制

推荐在控制器路径中包含版本号：

```
/api/{pluginInterface-id}/v1/{path}
/api/{pluginInterface-id}/v2/{path}
```

示例：
```
/api/user-manager/v1/users
/api/user-manager/v2/users
```

## 3. 在插件中定义API路径

### 3.1 使用Spring注解定义路径

在控制器类中使用`@RequestMapping`注解及其变体定义路径：

```java
@RestController
@RequestMapping("/users")  // 只需指定相对路径，不包含前缀和插件ID
public class UserController {

    @GetMapping("/{id}")   // GET /api/{pluginInterface-id}/users/{id}
    public User getUser(@PathVariable Long id) {
        // 实现
    }
    
    @PostMapping           // POST /api/{pluginInterface-id}/users
    public User createUser(@RequestBody User user) {
        // 实现
    }
}
```

### 3.2 公共API示例

定义公共API控制器：

```java
@RestController
@RequestMapping("/pub/statistics")  // 使用pub前缀
public class PublicStatisticsController {

    @GetMapping           // GET /api/{pluginInterface-id}/pub/statistics
    public Statistics getPublicStats() {
        // 实现
    }
}
```

### 3.3 管理API示例

定义管理API控制器：

```java
@RestController
@RequestMapping("/admin/settings")  // 使用admin前缀
public class AdminSettingsController {

    @GetMapping           // GET /api/{pluginInterface-id}/admin/settings
    @RequiresPermission("admin")  // 要求管理员权限
    public Settings getSettings() {
        // 实现
    }
    
    @PutMapping           // PUT /api/{pluginInterface-id}/admin/settings
    @RequiresPermission("admin")  // 要求管理员权限
    public Settings updateSettings(@RequestBody Settings settings) {
        // 实现
    }
}
```

## 4. 控制器命名与发现规则

QTeamOS网关层通过多种机制发现和注册插件中的控制器：

### 4.1 控制器命名约定

系统自动识别符合以下命名约定的类作为控制器：

- **Controller**: 如`UserController`
- **RestController**: 如`UserRestController`
- **RestCtroller**: 如`UserRestCtroller`（兼容拼写变体）
- **Ctrl**: 如`UserCtrl`
- **Resource**: 如`UserResource`

### 4.2 控制器包路径约定

系统会优先扫描以下包路径中的控制器类：

- `.controller`：如`com.example.pluginInterface.controller`
- `.web`：如`com.example.pluginInterface.web`
- `.rest`：如`com.example.pluginInterface.rest`
- `.api`：如`com.example.pluginInterface.api`
- `.auth`：如`com.example.pluginInterface.auth`

### 4.3 在插件描述文件中声明控制器

在`pluginInterface.yml`文件中显式声明控制器类，更加清晰且可靠：

```yaml
pluginInterface:
  id: user-manager
  version: 1.0.0
  # 其他配置...
  
  controllers:
    - com.example.pluginInterface.controller.UserController
    - com.example.pluginInterface.api.ProductResource
    - com.example.pluginInterface.web.OrderCtrl
```

### 4.4 实现PluginControllerProvider接口

插件主类可以实现`PluginControllerProvider`接口并提供控制器类列表：

```java
public class MyPlugin implements Plugin, PluginControllerProvider {
    
    @Override
    public List<String> getControllerClassNames() {
        return Arrays.asList(
            "com.example.pluginInterface.controller.UserController",
            "com.example.pluginInterface.api.ProductResource"
        );
    }
}
```

## 5. 路径映射规则

### 5.1 基本映射规则

- 控制器中定义的所有路径都将自动添加`/api/{pluginInterface-id}`前缀
- 路径中的多个斜杠会被合并为一个
- 如果`api-prefix`配置了尾部斜杠，系统会自动处理以避免路径错误

### 5.2 路径优先级

当多个控制器路径可能匹配同一请求时，系统按以下优先级处理：

1. 精确路径匹配优先于通配符匹配
2. 更具体的通配符模式优先于宽泛的模式
3. 插件路径映射优先于系统默认路径

### 5.3 路径冲突处理

当不同插件定义了相同路径时：

1. 系统会记录警告日志
2. 按照插件加载顺序，先加载的插件路径优先
3. 推荐通过插件ID确保路径唯一性

## 6. 最佳实践

### 6.1 路径设计建议

1. **使用有意义的资源名称**：路径应反映操作的资源
   ```
   /users
   /orders
   /products
   ```

2. **遵循RESTful设计原则**：
   - 使用名词复数表示资源集合
   - 使用HTTP方法表示操作类型
   - 使用URL参数标识资源实例

3. **避免深层嵌套**：
   ```
   // 推荐
   /orders/{orderId}
   /products/{productId}
   
   // 避免
   /users/{userId}/orders/{orderId}/items/{itemId}
   ```

4. **使用查询参数进行过滤、排序和分页**：
   ```
   /users?role=admin
   /products?category=electronics&sort=price&direction=asc
   /orders?page=1&size=20
   ```

### 6.2 版本控制策略

1. **URL路径版本**（推荐）：
   ```
   /api/{pluginInterface-id}/v1/users
   /api/{pluginInterface-id}/v2/users
   ```

2. **请求头版本**：
   ```
   X-API-Version: 1
   ```

3. **内容协商版本**：
   ```
   Accept: application/vnd.api.v1+json
   ```

### 6.3 安全性考虑

1. **正确使用HTTP方法**：
   - GET：读取操作
   - POST：创建操作
   - PUT/PATCH：更新操作
   - DELETE：删除操作

2. **应用适当的权限控制**：
   ```java
   @GetMapping("/{id}")
   @RequiresPermission("users:read")
   public User getUser(@PathVariable Long id) {
       // 实现
   }
   ```

3. **保护敏感操作和数据**：
   - 使用`admin`前缀隔离管理功能
   - 实施请求限流防止滥用

## 7. 故障排查

### 7.1 常见问题与解决方案

1. **路径无法访问**：
   - 检查插件ID是否正确
   - 确认控制器是否被系统识别
   - 验证权限配置是否限制了访问

2. **404错误**：
   - 检查路径拼写和格式
   - 确认插件是否成功加载
   - 查看日志中是否有路径注册错误

3. **权限问题**：
   - 检查用户权限是否匹配API要求
   - 确认API是否使用了正确的权限注解

### 7.2 调试技巧

1. **启用详细日志**：
   ```yaml
   logging:
     level:
       com.xiaoqu.qteamos.core.gateway: DEBUG
   ```

2. **查看注册的API路径**：
   - 访问`/api/system/routes`查看所有注册路径
   - 检查特定插件的路径映射：`/api/system/routes/{pluginInterface-id}`

## 8. 配置参考

### 8.1 核心配置项

```yaml
qteamos:
  gateway:
    # API请求前缀
    api-prefix: /api
    
    # 公共API前缀
    public-path-prefix: /pub
    
    # 管理API前缀
    admin-path-prefix: /admin
    
    # 是否启用网关功能
    enabled: true
    
    # 是否记录API访问日志
    enable-request-logging: true
    
    # 日志级别: BASIC, HEADERS, FULL
    log-level: BASIC
    
    # 是否启用API请求限流
    enable-rate-limit: false
    
    # 每分钟最大请求数
    default-rate-limit: 100
```

### 8.2 开发环境推荐配置

```yaml
qteamos:
  gateway:
    # 开发环境详细日志
    log-level: FULL
    # 宽松的限流设置
    enable-rate-limit: true
    default-rate-limit: 500
``` 