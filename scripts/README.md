# QTeamOS 脚本目录

本目录包含用于 QTeamOS 项目的各类脚本文件，按功能分类存放。

## 目录结构

- `sbin/`: 构建相关脚本，包括核心应用构建、SDK构建、插件项目创建和Docker镜像构建
- `service/`: 服务管理脚本，包括启动、停止、重启和系统服务注册

## 快速导航

### 构建脚本

- 核心构建: `./sbin/build-kernel.sh`
- SDK构建: `./sbin/build-sdk.sh`
- 创建插件: `./sbin/create-pluginInterface-project.sh`

### 服务管理脚本

- 启动服务: `./service/start.sh`
- 停止服务: `./service/stop.sh`
- 重启服务: `./service/restart.sh`
- Systemd服务配置: `./service/qteamos-systemd.service`

## 使用须知

1. 所有脚本在使用前请确保具有执行权限：
```bash
chmod +x scripts/sbin/*.sh scripts/service/*.sh
```

2. 脚本使用说明和参数请参考各子目录下的README文件

3. 自动化构建和部署

可以使用以下命令完成整个项目的构建和打包：

```bash
# 构建核心应用
cd scripts/sbin && ./build-kernel.sh

# 构建SDK
cd scripts/sbin && ./build-sdk.sh

# 创建一个新的插件项目
cd scripts/sbin && ./create-pluginInterface-project.sh
```

## 开发环境要求

- Java 17或更高版本
- Maven 3.6.0或更高版本
- Bash shell环境

## 版权信息

Copyright © 2023 浙江小趣信息技术有限公司，保留所有权利。 