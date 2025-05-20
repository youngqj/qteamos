# QTeamOS 插件开发SDK

QTeamOS 插件SDK是一个用于开发QTeamOS插件的工具包，提供了丰富的API和工具，让插件开发变得简单易用。

## 概述

QTeamOS采用插件架构设计，通过SDK可以开发各种功能插件，扩展QTeamOS的功能。插件开发完成后，只需要将插件包放入QTeamOS的插件目录即可被系统识别和加载。

## 特性

- 简单易用的插件API
- 完整的生命周期管理
- 丰富的系统服务集成（数据库、缓存、配置等）
- 统一的插件清单格式
- 安全的权限控制
- 多插件依赖管理

## 快速入门

### 1. 创建插件项目

使用Maven创建一个Java项目，并添加QTeamOS SDK依赖：

```xml
<dependency>
    <groupId>com.xiaoqu</groupId>
    <artifactId>qteam-sdk</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

### 2. 实现插件主类

创建一个实现`TDApiPlugin`接口的类，或者继承`AbstractPlugin`类：

```java
package com.example.plugin;

import com.xiaoqu.qteamos.sdk.plugin.AbstractPlugin;
import com.xiaoqu.qteamos.sdk.plugin.context.PluginContext;

public class ExamplePlugin extends AbstractPlugin {
    
    @Override
    public void init(PluginContext context) throws Exception {
        super.init(context);
        // 自定义初始化逻辑
    }
    
    @Override
    public void start() throws Exception {
        super.start();
        // 插件启动逻辑
    }
    
    @Override
    public void stop() throws Exception {
        // 插件停止逻辑
        super.stop();
    }
    
    @Override
    public void uninstall() throws Exception {
        // 插件卸载逻辑
        super.uninstall();
    }
}
```

### 3. 创建插件清单

在`src/main/resources`目录下创建`plugin.yml`文件：

```yaml
pluginId: "example-plugin"
name: "示例插件"
version: "1.0.0"
mainClass: "com.example.plugin.ExamplePlugin"
author: "开发者名称"
description: "这是一个示例插件"
type: "normal"
trust: "trusted"
license: "MIT"
priority: 10
provider: "示例公司"
website: "https://example.com"
category: "tools"

# 所需权限
permissions:
  - "file.read"
  - "db.query"

# 系统要求
requiredSystemVersion: "1.0.0"

# 插件配置
config:
  greeting: "你好，世界！"
  enableDebug: true
```

### 4. 打包插件

使用Maven打包插件：

```bash
mvn clean package
```

### 5. 部署插件

将生成的JAR文件复制到QTeamOS的`plugins`目录中。

## 插件生命周期

QTeamOS插件有以下生命周期方法：

1. **init**: 插件初始化时调用，用于初始化资源
2. **start**: 插件启动时调用，开始提供功能
3. **stop**: 插件停止时调用，暂停功能
4. **uninstall**: 插件卸载前调用，清理资源

## 主要API

### PluginContext

`PluginContext`是插件与系统交互的核心接口，提供以下功能：

- 获取插件ID、数据目录等基本信息
- 获取系统服务（配置、数据库、缓存等）
- 注册和获取服务

### 配置服务

```java
// 获取配置服务
ConfigService configService = context.getConfigService();

// 读取配置
String greeting = configService.getString("greeting", "默认问候语");
boolean debug = configService.getBoolean("enableDebug", false);

// 修改配置
configService.set("greeting", "新问候语");
configService.save();
```

### 数据库服务

```java
// 获取数据库服务
DataSourceService dataSourceService = context.getDataSourceService();

// 获取连接
try (Connection conn = dataSourceService.getConnection()) {
    // 执行SQL操作
}

// 执行SQL脚本
dataSourceService.executeSql(getId(), "db/init.sql");
```

### 缓存服务

```java
// 获取缓存服务
CacheService cacheService = context.getCacheService();

// 设置缓存
cacheService.set("key", "value", 1, TimeUnit.HOURS);

// 获取缓存
String value = cacheService.get("key");
```

### 用户服务

```java
// 获取用户服务
UserService userService = context.getUserService();

// 获取当前用户
UserInfo currentUser = userService.getCurrentUser();

// 检查权限
boolean hasPermission = userService.hasPermission("file.read");
```

## 插件清单参考

`plugin.yml`是插件的元数据文件，定义了插件的基本信息和配置。以下是完整的字段参考：

```yaml
# 基本信息
pluginId: "plugin-id"          # 插件唯一标识符
name: "插件名称"               # 插件显示名称
version: "1.0.0"               # 插件版本
mainClass: "com.example.Main"  # 插件主类
author: "作者名称"             # 插件作者
description: "插件描述"        # 插件描述

# 类型和信任级别
type: "normal"                 # normal-普通插件, system-系统插件
trust: "trusted"               # trusted-受信任的, official-官方的

# 其他元数据
license: "MIT"                 # 插件许可证类型
priority: 10                   # 插件优先级，数值越小优先级越高
provider: "提供者"             # 插件提供者/开发者
website: "https://example.com" # 插件官网或文档地址
category: "tools"              # 插件分类

# 权限和系统要求
permissions:                   # 所需权限
  - "file.read"
  - "db.query"
requiredSystemVersion: "1.0.0" # 所需最低系统版本

# 依赖配置
dependencies:                  # 依赖的其他插件
  some-plugin:                 # 依赖的插件ID
    version: ">=1.0.0"         # 版本要求
    optional: false            # 是否可选依赖

# 生命周期方法映射
lifecycle:                     # 自定义生命周期方法
  init: "onInit"               # 初始化方法
  start: "onStart"             # 启动方法
  stop: "onStop"               # 停止方法
  unload: "onUnload"           # 卸载方法

# 扩展点定义
extensionPoints:               # 定义的扩展点
  - id: "menu.extension"       # 扩展点ID
    name: "菜单扩展点"         # 扩展点名称
    description: "允许添加菜单" # 扩展点描述
    type: "interface"          # 扩展点类型
    interfaceClass: "com.example.MenuExtension" # 接口类
    multiple: true             # 是否允许多个实现
    required: false            # 是否必须实现

# 资源定义
resources:                     # 插件资源
  - path: "static/js"          # 资源路径
    type: "script"             # 资源类型

# 插件配置
config:                        # 默认配置
  greeting: "你好，世界！"      # 配置项
  enableDebug: true           # 配置项

# 健康检查
healthCheck:                   # 健康检查配置
  url: "/health/check"         # 健康检查URL
  timeout: 3000               # 超时时间(毫秒)

# 升级配置
update:                        # 升级配置
  previousVersion: null        # 上一个版本
  targetVersion: "1.0.0"       # 目标版本
  databaseChange: false        # 是否包含数据库变更
  migrationScripts: []         # 数据库迁移脚本列表
  requiresGrayRelease: false   # 是否需要灰度发布
  breakingChanges: false       # 是否包含破坏性变更
  rollbackSupported: true      # 是否支持回滚
```

## 最佳实践

1. **使用AbstractPlugin**: 优先使用`AbstractPlugin`作为插件基类，它提供了许多通用功能
2. **资源隔离**: 使用插件上下文提供的目录存储插件的文件和数据
3. **配置管理**: 将可变配置放入配置文件，避免硬编码
4. **错误处理**: 妥善处理异常，避免影响系统稳定性
5. **资源释放**: 在`stop`和`uninstall`方法中释放占用的资源
6. **版本兼容**: 注意维护向后兼容性，特别是在更新插件时
7. **权限最小化**: 只申请必要的权限，遵循最小权限原则

## 常见问题

### 插件无法加载

- 检查`plugin.yml`格式是否正确
- 确认`mainClass`指向的类存在并实现了`TDApiPlugin`接口
- 检查依赖项是否满足

### 找不到类或方法

- 检查SDK版本是否与系统匹配
- 确认所有依赖都已正确打包或声明

### 插件之间冲突

- 检查插件ID是否唯一
- 确认依赖关系是否正确声明
- 考虑使用不同的优先级解决加载顺序问题

## 支持

如果你在使用SDK过程中遇到问题，可以通过以下渠道获取帮助：

- 官方文档：[https://docs.qteamos.com/sdk](https://docs.qteamos.com/sdk)
- 问题反馈：[https://github.com/xiaoqu/qteamos/issues](https://github.com/xiaoqu/qteamos/issues)
- 邮件支持：support@qteamos.com

## 许可证

QTeamOS SDK 根据 [Mulan PSL v2](http://license.coscl.org.cn/MulanPSL2) 许可证发布。 