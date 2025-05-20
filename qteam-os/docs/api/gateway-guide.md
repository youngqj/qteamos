# QTeamOS网关层使用指南

## 1. 概述

QTeamOS网关层是系统中用于统一管理和转发插件API请求的核心组件。它提供了一个统一的入口点，将外部请求路由到相应的插件处理器，同时提供了权限控制、监控统计和动态API注册等功能。

网关层核心优势:
- **统一入口**: 所有插件API请求通过统一前缀访问
- **动态路由**: 支持插件的热加载和热卸载，API路径自动注册和注销
- **灵活配置**: 可配置API前缀、路径映射规则
- **自动发现**: 自动扫描和注册插件中的控制器类

## 2. 配置选项

### 2.1 核心配置

在`application.properties`或`application.yml`中配置:

```yaml
qteamos:
  gateway:
    # API请求前缀，默认为/api
    api-prefix: /api
    
    # 是否启用网关功能，默认为true
    enabled: true
    
    # 是否记录API访问日志，默认为true
    log-enabled: true
    
    # 日志级别，可选值: BASIC, HEADERS, FULL，默认为BASIC
    log-level: BASIC
    
    # 安全配置
    security:
      # 是否启用API访问控制，默认为true
      enabled: true
      # 是否启用API请求限流，默认为false
      rate-limit-enabled: false
      # 每分钟最大请求数，默认为100
      rate-limit-per-minute: 100
```

### 2.2 配置参数说明

| 参数名                | 说明                     | 默认值  | 可选值                |
|--------------------|------------------------|------|---------------------|
| api-prefix         | API请求统一前缀              | /api | 任意有效URL路径           |
| enabled            | 是否启用网关功能               | true | true, false         |
| log-enabled        | 是否记录API访问日志            | true | true, false         |
| log-level          | 日志记录详细程度               | BASIC | BASIC, HEADERS, FULL |
| security.enabled   | 是否启用API访问控制            | true | true, false         |
| rate-limit-enabled | 是否启用API请求限流            | false | true, false         |
| rate-limit-per-minute | 每分钟最大请求数             | 100  | 正整数                 |

## 3. 功能说明

### 3.1 API路径映射

网关层自动将请求映射到插件控制器:

1. 外部请求格式: `{api-prefix}/{pluginId}/{controller-path}`
   - 例如: `/api/user-manager/users/1`
   
2. 自动映射规则:
   - 系统自动将请求转发给对应插件ID的控制器
   - 支持标准的RESTful风格API和传统MVC风格API

### 3.2 控制器自动发现

网关层支持多种方式发现插件控制器:

1. **插件描述中声明**:
   在plugin.yml的metadata部分配置:
   ```yaml
   metadata:
     controllers:
       - com.xiaoqu.pluginInterface.example.controller.UserController
       - com.xiaoqu.pluginInterface.example.api.ProductApi
   ```

2. **基于约定扫描**:
   如果没有明确配置，系统会:
   - 从插件主类包开始扫描，如`com.xiaoqu.pluginInterface.example`
   - 自动识别任何位置的控制器类

3. **插件API声明**:
   插件主类可以实现`getControllerPackages()`或`getControllerClasses()`方法返回控制器信息

### 3.3 控制器识别规则

系统使用以下规则识别控制器类:

1. **注解识别**: 类上有`@Controller`或`@RestController`注解
2. **命名约定**: 类名包含以下关键字:
   - Controller (如UserController)
   - RestController (如UserRestController)
   - RestCtroller (兼容拼写变体)
   - Ctrl (如UserCtrl)
   - Resource (如UserResource)

### 3.4 API注册和注销事件

网关层提供了API生命周期事件机制:

1. **PluginApiRegistrationEvent**: 当API被注册时触发
2. **PluginApiUnregistrationEvent**: 当API被注销时触发

可以通过监听这些事件实现自定义逻辑，如API网关统计、文档生成等。

### 3.5 安全性功能

网关层提供以下安全性功能:

1. **访问控制**: 基于插件权限的API访问控制
2. **请求限流**: 防止API被滥用的限流机制
3. **请求校验**: 自动验证请求参数和格式
4. **异常处理**: 统一的错误响应格式

#### 3.5.1 配置插件API权限

在plugin.yml中配置API权限:

```yaml
permissions:
  - "api:read"     # 读取权限
  - "api:write"    # 写入权限
  - "api:admin"    # 管理权限
```

#### 3.5.2 在控制器中使用权限注解

```java
@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/{id}")
    @RequiresPermission("api:read")  // 需要读取权限
    public User getUser(@PathVariable Long id) {
        // 实现获取用户逻辑
        return userService.getById(id);
    }
    
    @PostMapping
    @RequiresPermission("api:write")  // 需要写入权限
    public User createUser(@RequestBody User user) {
        // 实现创建用户逻辑
        return userService.create(user);
    }
    
    @DeleteMapping("/{id}")
    @RequiresPermission("api:admin")  // 需要管理权限
    public void deleteUser(@PathVariable Long id) {
        // 实现删除用户逻辑
    }
}
```

## 4. 使用示例

### 4.1 创建插件控制器

在插件中创建RESTful风格控制器:

```java
package com.xiaoqu.pluginInterface.example.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        // 实现获取用户逻辑
        return userService.getById(id);
    }
    
    @PostMapping
    public User createUser(@RequestBody User user) {
        // 实现创建用户逻辑
        return userService.create(user);
    }
}
```

### 4.2 访问插件API

假设有一个ID为"user-manager"的插件，包含上述控制器:

- 获取用户: `GET /api/user-manager/users/1`
- 创建用户: `POST /api/user-manager/users`

### 4.3 自定义API前缀

如果想将API前缀修改为"/v1":

```yaml
qteamos:
  gateway:
    api-prefix: /v1
```

修改后的访问路径:
- 获取用户: `GET /v1/user-manager/users/1`

## 5. 最佳实践

### 5.1 API版本控制

推荐在控制器层面处理API版本:

```java
@RestController
@RequestMapping("/v1/products")
public class ProductControllerV1 {
    // V1版本API实现
}

@RestController
@RequestMapping("/v2/products")
public class ProductControllerV2 {
    // V2版本API实现
}
```

### 5.2 插件API文档集成

推荐在插件中集成Swagger/OpenAPI文档:

1. 在插件POM中添加依赖:
   ```xml
   <dependency>
       <groupId>org.springdoc</groupId>
       <artifactId>springdoc-openapi-ui</artifactId>
       <version>1.6.12</version>
   </dependency>
   ```

2. 在控制器中添加文档注解:
   ```java
   @RestController
   @RequestMapping("/users")
   @Tag(name = "用户管理", description = "用户管理相关API")
   public class UserController {
       
       @Operation(summary = "获取用户信息", description = "根据用户ID获取详细信息")
       @GetMapping("/{id}")
       public User getUser(@Parameter(description = "用户ID") @PathVariable Long id) {
           // 实现获取用户逻辑
       }
   }
   ```

### 5.3 错误处理

推荐统一的错误处理方式:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleException(Exception ex) {
        ApiError error = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex) {
        ApiError error = new ApiError(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }
}
```

### 5.4 性能优化

#### 5.4.1 控制器延迟加载

对于大型插件，可以使用延迟加载机制:

```java
@Configuration
public class PluginApiConfig {
    @Bean
    public LazyControllerLoader lazyControllerLoader() {
        return new LazyControllerLoader();
    }
}
```

#### 5.4.2 请求缓存

对于频繁访问且数据变化不大的API，启用缓存:

```java
@RestController
@RequestMapping("/products")
public class ProductController {

    @Cacheable(value = "productCache", key = "#id")
    @GetMapping("/{id}")
    public Product getProduct(@PathVariable Long id) {
        // 实现获取产品逻辑
    }
}
```

#### 5.4.3 异步处理

对于耗时操作，使用异步处理:

```java
@RestController
@RequestMapping("/reports")
public class ReportController {

    @GetMapping("/generate")
    public CompletableFuture<Report> generateReport() {
        return CompletableFuture.supplyAsync(() -> {
            // 耗时的报告生成逻辑
            return reportService.generate();
        });
    }
}
```

## 6. 常见问题

### Q: 插件API路径404错误

**可能原因**:
- 控制器类没有被正确识别
- 插件ID不正确
- 请求路径格式不正确

**解决方法**:
1. 确认插件已正确加载和启动
2. 检查控制器类是否有正确的注解
3. 确认请求格式为`/api/{pluginId}/{controller-path}`
4. 在日志中查看插件控制器注册信息

### Q: 如何监控API调用情况

可以通过以下方式监控API调用:

1. 实现`PluginApiRegistrationEvent`和`PluginApiUnregistrationEvent`事件监听器
2. 在Spring配置中添加请求拦截器
3. 使用Actuator监控端点

示例监听器:

```java
@Component
public class ApiMonitor {
    @EventListener
    public void onApiRegistered(PluginApiRegistrationEvent event) {
        log.info("API注册: {} - {}.{}",
                event.getMappingInfo(),
                event.getControllerClass().getSimpleName(),
                event.getMethod().getName());
    }
}
```

### Q: 如何定制API路径映射规则

如需自定义API路径映射规则，可以:

1. 继承`PluginRequestMappingHandlerMapping`类
2. 重写`createMappingInfo`方法
3. 在Spring配置中注册自定义处理器

### Q: 插件热更新后API不可用

**可能原因**:
- 插件更新后控制器未被重新注册
- 类加载器冲突

**解决方法**:
1. 确保在插件重新加载完成后调用`unregisterPluginControllers`和`registerPluginControllers`
2. 检查插件类加载器是否已正确更新
3. 在插件管理器的`reloadPlugin`方法中添加API刷新逻辑 