/*
 * Copyright (c) 2023-2024 XiaoQuTeam. All rights reserved.
 * QTeamOS is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

# 网关服务（Gateway Service）

## 模块概述
网关服务是QTeamOS系统的核心组件之一，作为连接客户端与插件API的桥梁，提供统一的访问入口、安全控制和资源保护。网关服务确保插件API的安全可控，并支持插件版本管理与可观测性。

## 核心职责

### 1. 统一API管理
- 提供统一的API访问入口：通过`/api/{plugin-id}/`前缀路径
- 集成已有的插件路由机制，确保API请求正确路由至目标插件
- 支持插件API的自动注册和注销

### 2. 安全控制集成
- 与Spring Security集成，实现基于路径的访问控制：
  - `/api/{plugin-id}/pub/**`：公共API，无需认证
  - `/api/{plugin-id}/admin/**`：管理API，需要管理员权限
  - `/api/{plugin-id}/**`：普通API，需要基本认证
- 提供统一的认证失败处理

### 3. 资源保护
- 实现API限流机制，防止单个插件消耗过多系统资源
- 支持插件级别的自定义限流规则配置
- 提供优雅的限流响应处理

### 4. 插件版本管理支持
- 监听`PluginRolloutEvent`事件，响应插件版本变更
- 确保灰度发布过程中的路由稳定性
- 支持插件版本更新后的路由刷新

### 5. 可观测性
- 收集和提供API调用监控指标
- 支持可配置的请求日志记录
- 提供网关健康状态检查

## 核心流程图

### 插件API注册流程

```
┌───────────┐      ┌───────────────────────────────┐      ┌───────────────┐
│           │      │                               │      │               │
│  插件启动  ├─────►│ PluginRequestMappingHandler  ├─────►│  注册API路由   │
│           │      │ 读取配置的API前缀(api-prefix)  │      │  /api/{id}/... │
└───────────┘      └───────────────────────────────┘      └───────┬───────┘
                                                                  │
                                                                  ▼
┌────────────────┐      ┌────────────────────────┐      ┌─────────────────┐
│                │      │                        │      │                 │
│ 插件API可用    │◄─────┤ 发布API注册完成事件    │◄─────┤  注册到Spring MVC │
│                │      │                        │      │  请求处理器映射   │
└────────────────┘      └────────────────────────┘      └─────────────────┘
```

### 请求处理流程

```
┌───────────┐      ┌───────────────────┐      ┌─────────────────────┐
│           │      │                   │      │                     │
│ 客户端请求 ├─────►│ Spring Security   ├─────►│ 认证和授权检查       │
│           │      │ 过滤器链          │      │ (基于路径规则)       │
└───────────┘      └───────────────────┘      └──────────┬──────────┘
                                                        │
                                                        ▼
┌───────────────┐      ┌───────────────────┐      ┌─────────────────┐
│               │      │                   │      │                 │
│ 插件处理请求   │◄─────┤ Spring MVC路由    │◄─────┤ API限流检查     │
│ 并返回响应     │      │ 到目标控制器      │      │ (令牌桶算法)    │
└───────────────┘      └───────────────────┘      └─────────────────┘
```

### 插件版本变更流程

```
┌───────────────┐      ┌───────────────────┐      ┌─────────────────────┐
│               │      │                   │      │                     │
│ 插件版本更新   ├─────►│ PluginRolloutEvent├─────►│ GatewayService接收  │
│ (灰度发布)     │      │ 事件触发          │      │ 并处理事件          │
└───────────────┘      └───────────────────┘      └──────────┬──────────┘
                                                            │
                                                            ▼
┌────────────────┐      ┌────────────────────────┐      ┌─────────────────┐
│                │      │                        │      │                 │
│ API路由使用    │◄─────┤ 路由注册完成          │◄─────┤ 刷新插件API路由  │
│ 新版本插件      │      │                        │      │                 │
└────────────────┘      └────────────────────────┘      └─────────────────┘
```

## 技术实现

### 1. 与插件系统集成
- 直接使用已有的`PluginRequestMappingHandlerMapping`注册路由
- 通过配置文件统一设置API前缀，保持一致性
- 通过事件机制与插件系统松耦合，确保扩展性

### 2. 安全控制
- 集成Spring Security进行身份认证和访问控制
- 基于路径的权限规则配置
- 统一的安全异常处理

### 3. 资源保护
- 使用令牌桶算法实现API限流
- 支持插件级别的自定义限流规则
- 优雅的限流响应策略

### 4. 监控与日志
- API调用量、响应时间等核心指标收集
- 可配置的请求日志记录级别
- 结构化的监控数据输出

## 配置选项

网关服务支持以下配置项：

```properties
# API前缀路径
qteamos.gateway.api-prefix=/api

# 公共API前缀
qteamos.gateway.public-path-prefix=/pub

# 管理API前缀  
qteamos.gateway.admin-path-prefix=/admin

# 默认限流速率(每分钟请求数)
qteamos.gateway.default-rate-limit=100

# 是否启用请求日志
qteamos.gateway.enable-request-logging=true

# 是否启用限流
qteamos.gateway.enable-rate-limit=true
```

## 使用示例

### 插件开发接口示例

```java
@RestController
@RequestMapping("/pub/auth")
public class AuthController {
    
    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginDTO loginDTO) {
        // 登录实现
        return Result.success("登录成功");
    }
    
    @GetMapping("/codeimg")
    public void getVerifyCode(HttpServletResponse response) {
        // 获取验证码
    }
}
```

以上代码将自动映射为:
- `/api/{plugin-id}/pub/auth/login` - 无需认证
- `/api/{plugin-id}/pub/auth/codeimg` - 无需认证

### 需要认证的接口示例

```java
@RestController
@RequestMapping("/user")
public class UserController {
    
    @GetMapping("/info")
    public Result<UserInfo> getUserInfo() {
        // 需要token认证
        return Result.success(userInfo);
    }
}
```

以上代码将自动映射为:
- `/api/{plugin-id}/user/info` - 需要认证

### 管理接口示例

```java
@RestController
@RequestMapping("/admin/system")
public class AdminController {
    
    @PostMapping("/config")
    public Result<Boolean> updateConfig(@RequestBody ConfigDTO config) {
        // 需要管理员权限
        return Result.success(true);
    }
}
```

以上代码将自动映射为:
- `/api/{plugin-id}/admin/system/config` - 需要管理员权限

## 模块目录结构

保持简洁的目录结构，避免不必要的复杂性：

```
core/gateway/
├── config/         - 网关配置
├── filter/         - 过滤器（限流等）
├── service/        - 网关服务接口和实现
├── model/          - 数据模型（如需）
└── util/           - 工具类（如需）
``` 