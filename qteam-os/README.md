# QTeamOS

QTeamOS是一个基于Java Spring Boot的高性能、可扩展的插件化系统架构框架。它支持动态加载插件、细粒度权限控制、分布式部署，适用于快速开发企业级应用系统。

<p align="center">
  <img src="docs/images/logo.png" alt="QTeamOS Logo" width="200"/>
</p>

[![License](https://img.shields.io/badge/License-MulanPSL2-blue.svg)](LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-green.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/JDK-17-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)

## 项目特性

- **插件化架构**：支持动态加载、卸载和热部署插件
- **模块化设计**：核心引擎与业务逻辑完全分离
- **安全防护体系**：多层次安全保障，沙箱隔离机制
- **分布式支持**：内置分布式ID生成器，支持集群部署
- **用户权限系统**：完善的RBAC权限模型，细粒度权限控制
- **开发者友好**：简化开发流程，降低学习成本
- **界面美观**：内置现代化UI组件
- **高性能**：插件级别的性能监控与优化

## 核心服务模块

QTeamOS提供了以下核心服务模块，方便插件开发者快速集成功能：

- **[数据库服务](docs/database/database-service-guide.md)**：支持多数据源、ORM框架和动态切换
- **[缓存服务](docs/cache/cache-service-guide.md)**：提供统一接口操作多种缓存实现

## 系统架构

QTeamOS采用分层架构设计，主要分为以下几个核心模块：

- **核心层(Core)**：提供插件加载、生命周期管理、安全沙箱等基础设施
- **SDK层**：为插件开发者提供接口和工具
- **应用层(App)**：包含用户、角色、权限等基础功能
- **网关层(Gateway)**：处理API路由和访问控制
- **插件层(Plugins)**：由第三方开发的功能扩展模块

<p align="center">
  <img src="docs/images/architecture.png" alt="QTeamOS Architecture" width="600"/>
</p>

## 快速开始

### 环境要求

- JDK 17+
- MySQL 8.0+
- Redis 6.0+ (集群模式需要)
- Maven 3.6+

### 安装步骤

1. **克隆仓库**

```bash
git clone https://github.com/xiaoqu/qteamos.git
cd qteamos
```

2. **准备数据库**

创建MySQL数据库并执行初始化脚本（按顺序）：

```bash
mysql -u username -p
> CREATE DATABASE qteamos DEFAULT CHARACTER SET utf8mb4;
> exit;

mysql -u username -p qteamos < src/main/resources/db/id-generator.sql
mysql -u username -p qteamos < src/main/resources/db/init-system-tables.sql
mysql -u username -p qteamos < src/main/resources/db/init-app-tables.sql
```

3. **配置应用**

复制示例配置文件并修改：

```bash
cp src/main/resources/application.yml.example src/main/resources/application.yml
```

根据环境修改`application.yml`中的数据库连接信息和部署模式。

4. **编译运行**

```bash
mvn clean package
java -jar target/qteamos.jar
```

5. **访问系统**

浏览器访问：http://localhost:8080/

默认管理员账号：
- 用户名：admin
- 密码：admin

## 插件开发

### 创建插件项目

1. 引入SDK依赖
2. 实现插件接口
3. 打包并部署到插件目录

详细指南请参考[插件开发文档](docs/pluginInterface/development-guide.md)。

### 插件示例

```java
@Plugin(id = "example-pluginInterface", name = "示例插件", version = "1.0.0")
public class ExamplePlugin implements QTeamPlugin {
    
    @Override
    public void onLoad() {
        // 插件加载时执行
    }
    
    @Override
    public void onEnable() {
        // 插件启用时执行
    }
    
    @Override
    public void onDisable() {
        // 插件禁用时执行
    }
}
```

## 部署方式

QTeamOS支持两种部署模式：

### 单机模式

适用于开发环境和小型应用场景：

```yaml
qteamos:
  deployment:
    mode: standalone
```

### 集群模式

适用于生产环境和大规模应用场景：

```yaml
qteamos:
  deployment:
    mode: cluster
    node:
      datacenter-id: 1  # 不同数据中心使用不同ID
      worker-id: 2      # 同一数据中心内不同节点使用不同ID
```

更多部署细节请参考[部署指南](docs/deployment-guide.md)。

## 项目规划

QTeamOS整体规划分为两个主要部分：

1. **qteamos**：插件化系统核心（本工程）
2. **qteamcloud**：插件管理云平台，包含应用市场、授权管理等功能

## 技术栈与依赖

QTeamOS基于以下关键技术和依赖库构建：

### 框架与核心依赖
- Spring Boot 3.4.5
- Spring Security (Spring Boot Starter)
- Spring Data Redis (Spring Boot Starter)
- MyBatis-Plus 3.5.8

### 数据库与缓存
- MySQL 8.2.0
- Druid 1.2.20 (数据库连接池)
- Redisson 3.23.5 (Redis客户端)

### 安全与认证
- JWT (jjwt) 0.11.5
- Jasypt 3.0.5 (配置加密)

### 任务调度
- Quartz (Spring Boot Starter)
- XXL-Job 2.4.2 (分布式任务调度)

### 工具库
- Hutool 5.8.26 (Java工具类库)
- Guava 32.1.3-jre (Google核心库)
- Lombok 1.18.30 (代码简化工具)
- Apache Commons
  - commons-lang3 3.14.0
  - commons-io 2.15.1
  - commons-pool2 2.11.1
- SnakeYAML 2.0 (YAML解析)
- Gson 2.10.1 (JSON处理)

## 许可证

QTeamOS使用[木兰宽松许可证，第2版（Mulan PSL v2）](http://license.coscl.org.cn/MulanPSL2)

## 贡献指南

欢迎贡献代码、报告问题或提出新功能建议。详情请参阅[贡献指南](CONTRIBUTING.md)。

## 联系我们

- 官方网站：https://qteamos.xiaoqu.team
- 邮箱：contact@xiaoqu.com

    