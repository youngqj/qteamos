# QEleBase 构建脚本

本目录包含用于构建 QEleBase 应用和插件的脚本。

## 文件说明

- `build-kernel.sh`: 构建 QEleBase 核心应用的脚本
- `build-sdk.sh`: 构建 QEleBase SDK 的脚本
- `create-pluginInterface-project.sh`: 创建新插件项目的脚本
- `build-docker.sh`: 构建 QEleBase Docker 镜像的脚本
- `docker-compose.yml`: Docker Compose 配置文件，用于快速部署完整的 QEleBase 环境

## 使用方法

### 构建核心应用

```bash
# 构建应用
./build-kernel.sh
```

### 构建 SDK

```bash
# 构建 SDK
./build-sdk.sh
```

### 创建新插件项目

```bash
# 创建插件项目
./create-pluginInterface-project.sh
```

### 构建 Docker 镜像

```bash
# 构建 Docker 镜像
./build-docker.sh

# 构建并推送到镜像仓库
./build-docker.sh --push

# 指定版本和镜像名称
./build-docker.sh --version 1.0.0 --name myorg/qelebase

# 查看更多选项
./build-docker.sh --help
```

### 使用 Docker Compose 部署

```bash
# 在构建 Docker 镜像后，使用 Docker Compose 部署完整环境
docker-compose -f docker-compose.yml up -d

# 停止环境
docker-compose -f docker-compose.yml down
```

### 参数说明

各脚本的具体参数和选项请参考脚本内的注释说明，或者使用 `-h` 或 `--help` 参数查看帮助信息：

```bash
./build-kernel.sh --help
./build-sdk.sh --help
./create-pluginInterface-project.sh --help
./build-docker.sh --help
```

## 开发环境要求

- JDK 17 或以上
- Maven 3.6 或以上
- Bash Shell 环境
- Docker (用于构建和运行容器)
- Docker Compose (用于部署完整环境)

## 注意事项

1. 确保脚本具有执行权限：
```bash
chmod +x build-kernel.sh build-sdk.sh create-pluginInterface-project.sh build-docker.sh
```

2. 构建脚本依赖于项目的 Maven 配置，修改 POM 文件可能会影响构建结果

3. 在运行脚本前，请确保已设置好 JAVA_HOME 和 MAVEN_HOME 环境变量

4. 使用 Docker 相关功能前，请确保已安装 Docker 和 Docker Compose 