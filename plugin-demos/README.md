# QTeamOS 插件演示

本目录包含 QTeamOS SDK 的演示插件和测试框架，可用于验证 SDK 功能和开发插件参考。

## 目录结构

- `hello-world-plugin`: 一个简单的示例插件，展示基础插件开发流程
- `package.sh`: 打包脚本 
- `test-plugin-loader.sh`: 测试脚本，用于创建测试环境和运行插件




## Hello World 插件

这个示例插件演示了插件的基本开发方式，包括：

1. 继承 `AbstractPlugin` 类，实现插件生命周期方法
2. 使用配置服务读取配置
3. 使用缓存服务缓存数据
4. 发布和订阅事件
5. 清理资源

### 插件结构

```
plugin-helloworld/
├── pom.xml                           # Maven 构建文件
├── src/
│   └── main/
│       ├── java/
│       │   └── com/xiaoqu/qteamos/plugin/helloworld/
│       │       └── HelloWorldPlugin.java  # 插件主类
│       └── resources/
│           └── plugin.yml            # 插件描述文件
└── README.md
```

### 插件配置

插件描述文件 `plugin.yml` 包含插件元数据和配置信息：

```yaml
# 插件基本信息
pluginId: hello-world
name: Hello World Plugin
version: 1.0.0
description: 一个简单的QTeamOS演示插件
author: yangqijun
mainClass: com.xiaoqu.plugins.hello.HelloWorldPlugin

# 插件依赖
dependencies:
  - id: core
    version: ">=1.0.0"

# 插件设置
# ...
```



## 开发自己的插件

参考 Hello World 插件的结构和实现，可以快速开发自己的插件：

1. 创建一个 Maven 项目
2. 添加对 QTeamOS SDK 的依赖（scope 为 provided）
3. 创建一个实现 `Plugin` 接口或继承 `AbstractPlugin` 的主类
4. 创建 `plugin.yml` 描述文件
5. 构建并测试插件

## 编译插件

```bash
cd plugin-demos/hello-world-plugin
mvn clean package
```

编译好的插件 JAR 文件将位于 `target` 目录中。 