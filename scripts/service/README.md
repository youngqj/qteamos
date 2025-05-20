# QEleBase 服务脚本

本目录包含用于管理 QEleBase 应用的系统服务脚本。

## 文件说明

- `start.sh`: 启动 QEleBase 应用的脚本
- `stop.sh`: 停止 QEleBase 应用的脚本
- `restart.sh`: 重启 QEleBase 应用的脚本
- `qelebase-systemd.service`: systemd 服务配置文件，用于将 QEleBase 注册为系统服务

## 使用方法

### 手动启动/停止/重启

```bash
# 启动应用
./start.sh

# 停止应用
./stop.sh

# 重启应用
./restart.sh
```

### 安装为系统服务 (CentOS 7+/Ubuntu/Debian)

1. 将服务文件复制到 systemd 配置目录：

```bash
sudo cp qelebase-systemd.service /etc/systemd/system/
```

2. 重新加载 systemd 配置：

```bash
sudo systemctl daemon-reload
```

3. 启用 QEleBase 服务（设置开机启动）：

```bash
sudo systemctl enable qelebase
```

4. 启动服务：

```bash
sudo systemctl start qelebase
```

### 系统服务管理命令

```bash
# 启动服务
sudo systemctl start qelebase

# 停止服务
sudo systemctl stop qelebase

# 重启服务
sudo systemctl restart qelebase

# 查看服务状态
sudo systemctl status qelebase

# 查看服务日志
sudo journalctl -u qelebase -f
```

## 配置说明

### 系统服务配置

服务配置文件 `qelebase-systemd.service` 中可以根据需要修改以下参数：

- `User` 和 `Group`: 运行服务的用户和组
- `WorkingDirectory`: 应用的工作目录
- `JAVA_HOME`: Java 安装路径
- `SPRING_PROFILES_ACTIVE`: Spring 应用的运行环境配置

### 应用启动配置

在 `start.sh` 中可以根据需要修改以下参数：

- `APP_HOME`: 应用安装目录
- `JAR_NAME`: 应用 JAR 包名称
- `JAVA_OPTS`: Java 虚拟机参数，如内存配置等

## 注意事项

1. 确保脚本具有执行权限：
```bash
chmod +x start.sh stop.sh restart.sh
```

2. 首次部署时需要创建系统用户：
```bash
sudo useradd -r -s /bin/false qelebase
```

3. 确保 APP_HOME 目录存在并且 qelebase 用户有权限：
```bash
sudo mkdir -p /opt/qelebase
sudo chown -R qelebase:qelebase /opt/qelebase
``` 