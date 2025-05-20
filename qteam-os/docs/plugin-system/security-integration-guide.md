# QTeamOS 插件安全集成指南

## 1. 概述

QTeamOS提供了基于Spring Security的安全框架，支持壳子应用和插件共享安全机制。本指南将介绍如何在插件中集成和使用安全功能。

安全层的核心特性:

- **统一的认证授权管理**：所有插件共享系统的认证机制，无需单独实现
- **插件可自定义权限**：插件可以定义和使用自己的权限和角色
- **使用原生Spring Security**：完全兼容Spring Security的API和注解
- **无需自定义注解**：使用标准的Spring Security注解即可
- **与网关层集成**：安全层与网关层紧密集成，提供统一API路径和权限管理

## 2. 在plugin.yml中定义权限

插件可以在plugin.yml描述文件中定义自己的权限，示例如下:

```yaml
pluginId: my-pluginInterface
name: 我的插件
version: 1.0.0
main: com.example.pluginInterface.MyPlugin
permissions:
  - "my-pluginInterface:view"     # 查看权限
  - "my-pluginInterface:edit"     # 编辑权限
  - "my-pluginInterface:admin"    # 管理权限
  - "my-pluginInterface:data:read"  # 数据读取权限
  - "my-pluginInterface:data:write" # 数据写入权限
```

定义的权限会自动被系统识别和加载。

## 3. 创建插件安全扩展

### 3.1 实现PluginSecurityExtension接口

```java
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import com.xiaoqu.qteamos.core.security.pluginInterface.PluginSecurityExtension;

public class MyPluginSecurityExtension implements PluginSecurityExtension {
    
    @Override
    public String getPluginId() {
        return "my-pluginInterface";
    }
    
    @Override
    public void configure(HttpSecurity http) throws Exception {
        // 配置插件特定的安全规则
        http.authorizeHttpRequests(authorize -> {
            // 公共资源
            authorize.requestMatchers("/api/my-pluginInterface/public/**").permitAll();
            
            // 需要特定权限的资源
            authorize.requestMatchers("/api/my-pluginInterface/admin/**")
                     .hasAuthority("my-pluginInterface:admin");
                     
            authorize.requestMatchers("/api/my-pluginInterface/data/**")
                     .hasAnyAuthority("my-pluginInterface:data:read", "my-pluginInterface:admin");
        });
    }
    
    @Override
    public String[] getPermissions() {
        // 返回插件定义的权限（可选，如果在plugin.yml中已定义）
        return new String[] {
            "my-pluginInterface:view",
            "my-pluginInterface:edit",
            "my-pluginInterface:admin",
            "my-pluginInterface:data:read",
            "my-pluginInterface:data:write"
        };
    }
    
    @Override
    public String[] getRoles() {
        // 返回插件定义的角色
        return new String[] {
            "ROLE_MY_PLUGIN_ADMIN",
            "ROLE_MY_PLUGIN_USER"
        };
    }
}
```

### 3.2 在插件主类中提供安全扩展实例

方法一: 让插件主类实现PluginSecurityExtension接口

```java
import com.xiaoqu.qteamos.core.pluginInterface.api.Plugin;
import com.xiaoqu.qteamos.core.security.pluginInterface.PluginSecurityExtension;

public class MyPlugin implements Plugin, PluginSecurityExtension {
    // 实现Plugin接口的方法
    @Override
    public void start() {
        // 插件启动逻辑
    }
    
    @Override
    public void stop() {
        // 插件停止逻辑
    }
    
    // 实现PluginSecurityExtension接口的方法
    @Override
    public String getPluginId() {
        return "my-pluginInterface";
    }
    
    @Override
    public void configure(HttpSecurity http) throws Exception {
        // 安全配置
    }
}
```

方法二: 提供getSecurityExtension方法

```java
import com.xiaoqu.qteamos.core.pluginInterface.api.Plugin;
import com.xiaoqu.qteamos.core.security.pluginInterface.PluginSecurityExtension;

public class MyPlugin implements Plugin {
    private final PluginSecurityExtension securityExtension;
    
    public MyPlugin() {
        this.securityExtension = new MyPluginSecurityExtension();
    }
    
    // 插件管理器会自动查找此方法
    public PluginSecurityExtension getSecurityExtension() {
        return securityExtension;
    }
    
    // 其他Plugin接口方法实现
}
```

## 4. 在控制器中使用权限控制

Spring Security提供多种权限控制注解，所有这些注解都可在插件控制器中使用。

### 4.1 使用@PreAuthorize注解

最灵活的方式是使用@PreAuthorize注解，支持SpEL表达式:

```java
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/my-pluginInterface")
public class MyPluginController {

    @GetMapping("/data")
    @PreAuthorize("hasAuthority('my-pluginInterface:view')")
    public ResponseEntity<Object> getData() {
        // 实现方法
    }
    
    @PostMapping("/data")
    @PreAuthorize("hasAuthority('my-pluginInterface:edit')")
    public ResponseEntity<Object> createData(@RequestBody Map<String, Object> data) {
        // 实现方法
    }
    
    @DeleteMapping("/data/{id}")
    @PreAuthorize("hasAuthority('my-pluginInterface:admin')")
    public ResponseEntity<Object> deleteData(@PathVariable String id) {
        // 实现方法
    }
}
```

### 4.2 使用@Secured注解

针对角色控制，可以使用@Secured注解:

```java
import org.springframework.security.access.annotation.Secured;

@RestController
@RequestMapping("/api/my-pluginInterface/admin")
public class AdminController {

    @GetMapping("/users")
    @Secured("ROLE_MY_PLUGIN_ADMIN")
    public ResponseEntity<List<User>> getUsers() {
        // 实现方法
    }
}
```

### 4.3 使用JSR-250 注解

也可以使用标准的JSR-250注解:

```java
import jakarta.annotation.security.RolesAllowed;

@RestController
@RequestMapping("/api/my-pluginInterface/reports")
public class ReportController {

    @GetMapping("/{id}")
    @RolesAllowed({"ROLE_MY_PLUGIN_ADMIN", "ROLE_ADMIN"})
    public ResponseEntity<Report> getReport(@PathVariable String id) {
        // 实现方法
    }
}
```

## 5. 在服务层使用权限检查

在服务层代码中，可以通过注入PermissionService使用编程方式验证权限:

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xiaoqu.qteamos.core.security.service.PermissionService;

@Service
public class MyPluginService {

    @Autowired
    private PermissionService permissionService;
    
    public void performAction(String resourceId) {
        // 编程方式检查权限
        if (!permissionService.hasPermission("my-pluginInterface:edit")) {
            throw new AccessDeniedException("没有编辑权限");
        }
        
        // 检查多个权限中的任意一个
        if (!permissionService.hasAnyPermission("my-pluginInterface:admin", "my-pluginInterface:data:write")) {
            throw new AccessDeniedException("权限不足");
        }
        
        // 执行受保护的操作
    }
}
```

## 6. 处理认证和用户信息

### 6.1 获取当前用户

在控制器中获取当前用户信息:

```java
@GetMapping("/profile")
public ResponseEntity<Map<String, Object>> getProfile(Principal principal) {
    // principal包含当前登录用户信息
    String username = principal.getName();
    
    // 更多用户信息可通过SecurityContextHolder获取
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
    
    // 构建响应
    Map<String, Object> profile = new HashMap<>();
    profile.put("username", username);
    profile.put("authorities", authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()));
    
    return ResponseEntity.ok(profile);
}
```

### 6.2 提供插件用户源

插件可以提供自己的用户认证源，通过监听UserLoadingEvent事件:

```java
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import com.xiaoqu.qteamos.core.security.service.QTeamUserDetailsService.UserLoadingEvent;

@Component
public class MyPluginUserProvider {

    @EventListener
    public void onUserLoading(UserLoadingEvent event) {
        String username = event.getUsername();
        
        // 检查是否是插件管理的用户
        if (username.startsWith("pluginInterface-")) {
            // 从插件的用户源加载用户
            MyPluginUser user = loadUserFromPluginSource(username);
            
            if (user != null) {
                // 转换为Spring Security的UserDetails对象
                UserDetails userDetails = convertToUserDetails(user);
                event.setUserDetails(userDetails);
            }
        }
    }
    
    // 插件用户加载逻辑
    private MyPluginUser loadUserFromPluginSource(String username) {
        // 实现用户加载逻辑
    }
    
    // 转换为UserDetails
    private UserDetails convertToUserDetails(MyPluginUser user) {
        // 实现转换逻辑
    }
}
```

## 7. 网关层集成

QTeamOS的安全层与网关层进行了深度集成，确保插件API请求经过统一的权限检查。网关与安全层的集成主要体现在以下几个方面：

### 7.1 统一的API路径格式

网关使用`/api/{pluginId}/...`格式的路径，安全层在配置时也必须使用相同的路径格式：

```java
// 正确的路径格式
authorize.requestMatchers("/api/my-pluginInterface/users/**").hasAuthority("my-pluginInterface:users:view");

// 错误的路径格式（不符合网关规范）
authorize.requestMatchers("/my-pluginInterface/users/**").hasAuthority("my-pluginInterface:users:view");
```

所有API路径必须遵循网关的路径规范，才能确保权限规则正确生效。

### 7.2 API注册与权限关联

当插件API在网关注册时，系统会自动提取API的安全元数据：

1. 从控制器方法上获取权限注解（如@PreAuthorize、@Secured等）
2. 创建API路径与安全规则的映射
3. 在API请求时应用相应的权限检查

这种关联是自动完成的，无需插件开发者额外干预。

### 7.3 插件安全扩展与网关整合

插件提供的`PluginSecurityExtension`配置会影响网关层的所有请求：

```java
@Override
public void configure(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(authorize -> {
        // 这些配置会影响网关层对应路径的所有请求
        authorize.requestMatchers("/api/" + pluginId + "/public/**").permitAll();
        authorize.requestMatchers("/api/" + pluginId + "/admin/**").hasAuthority(pluginId + ":admin");
    });
}
```

### 7.4 统一的安全过滤器

网关层集成了专门的安全过滤器（GatewaySecurityFilter），负责：

1. 拦截所有API请求
2. 查找对应的安全元数据
3. 执行权限检查
4. 拒绝未授权的访问

此过滤器确保了即使控制器没有显式添加安全注解，仍然可以通过插件安全扩展配置应用权限规则。

### 7.5 API访问统计与安全审计

网关层会记录API访问信息，包括：

- 请求路径
- 用户身份
- 访问时间
- 授权结果

这些信息可用于安全审计和访问分析。

## 8. 使用安全API

系统提供了一系列REST API用于权限管理:

### 8.1 获取当前用户信息

```
GET /api/security/current-user
```

响应:
```json
{
  "authenticated": true,
  "username": "admin",
  "roles": ["ROLE_ADMIN", "ROLE_USER"]
}
```

### 8.2 检查权限

```
GET /api/security/has-permission?permission=my-pluginInterface:view
```

响应:
```json
{
  "permission": "my-pluginInterface:view",
  "hasPermission": true
}
```

### 8.3 获取所有权限

```
GET /api/security/permissions
```

响应:
```json
[
  "core:system:view",
  "core:user:manage",
  "my-pluginInterface:admin",
  "my-pluginInterface:view",
  "my-pluginInterface:edit"
]
```

### 8.4 获取所有角色

```
GET /api/security/roles
```

响应:
```json
[
  "ROLE_ADMIN",
  "ROLE_USER",
  "ROLE_MY_PLUGIN_ADMIN"
]
```

## 9. 最佳实践

1. **使用明确的权限命名规则**：建议使用`插件ID:资源:操作`的格式，例如`my-pluginInterface:user:create`

2. **最小权限原则**：为操作分配最小必要的权限，避免过度授权

3. **使用方法级权限**：优先使用方法级别的权限注解，而不是在控制器类级别定义

4. **区分角色和权限**：角色是权限的集合，权限是具体的操作许可

5. **权限文档化**：在插件文档中明确列出所有定义的权限和对应功能

6. **错误处理**：提供友好的权限不足提示信息，避免暴露敏感信息

7. **测试安全配置**：编写安全配置的测试用例，确保权限规则正确生效

8. **遵循网关路径规范**：确保所有API路径和安全配置遵循`/api/{pluginId}/...`格式 