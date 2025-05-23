# QTeam插件生态架构说明

## 🌟 生态定位

QTeam是一个面向**软件定制开发**的插件化框架，服务于企业级私有化部署场景。

## 🏗️ 整体架构

### 三层架构体系

```
┌─────────────────────────────────────────────────────────────┐
│                    📱 开发者平台                              │
│  ┌─────────────────┬─────────────────┬─────────────────────┐  │
│  │   🏢 官方插件    │   🔧 第三方插件   │   📚 开发资源        │  │
│  │  • system类型   │  • normal类型    │  • SDK文档         │  │
│  │  • trusted级别   │  • 各种信任级别   │  • 开发工具         │  │
│  │  • 自动加载运行   │  • 手工控制运行   │  • 示例代码         │  │
│  └─────────────────┴─────────────────┴─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            ⬇️ 按需选择 + 组合打包
┌─────────────────────────────────────────────────────────────┐
│                🏠 壳子 + 插件安装包 (当前项目)                 │
│  ┌─────────────────┬─────────────────┬─────────────────────┐  │
│  │   🔧 QTeam壳子   │   📦 插件SDK     │   🎯 选定插件包      │  │
│  │  • qteam-os     │  • qteam-sdk    │  • 必选system插件   │  │
│  │  • qteam-api    │  • 开发接口      │  • 可选normal插件   │  │
│  │  • 插件运行时     │  • 便捷工具      │  • 客户定制插件     │  │
│  └─────────────────┴─────────────────┴─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            ⬇️ 私有化部署
┌─────────────────────────────────────────────────────────────┐
│                🖥️ 客户环境 (离线运行)                         │
│  ┌─────────────────┬─────────────────┬─────────────────────┐  │
│  │   ⚡ 自动运行     │   🎮 手工控制     │   🔄 云更新         │  │
│  │  • system插件   │  • normal插件    │  • 仅此功能联网     │  │
│  │  • 核心功能      │  • 业务功能      │  • 连接开发者平台    │  │
│  │  • 基础服务      │  • 可选模块      │  • 版本管理         │  │
│  └─────────────────┴─────────────────┴─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## 🎯 业务流程

### 1. 需求分析阶段
```
客户需求 → 功能分析 → 插件选择
                  ↓
        现有插件够用？ → 是 → 组合打包
                  ↓
                 否 → SDK开发新插件
```

### 2. 开发交付阶段
```
插件开发 → 测试验证 → 上传平台 → 选择组合 → 打包部署
```

### 3. 运维阶段
```
私有化运行 ← → 仅插件更新时联网 ← → 开发者平台
```

## 📦 插件分类体系

### System插件 (核心基础)
- **作者**: QTeamOS官方
- **类型**: system  
- **信任级别**: trusted
- **加载方式**: 自动加载运行
- **用途**: 系统核心功能、基础服务
- **示例**: 用户管理、权限管理、系统监控

### Normal插件 (业务功能)
- **作者**: 第三方开发者、合作伙伴
- **类型**: normal
- **信任级别**: verified/community
- **加载方式**: 手工控制运行
- **用途**: 业务功能模块、行业解决方案
- **示例**: CRM、ERP、财务管理

## 🔧 技术特点

### 面向企业内部/合作伙伴开发
- **不是公开平台**：主要服务软件定制开发商
- **安全可控**：私有化部署，数据不出企业
- **便捷开发**：SDK简化插件开发流程
- **灵活组合**：按需选择插件组合

### 资源共享而非隔离
- **数据库访问**：共享连接池，资源池选择
- **缓存服务**：命名空间避免冲突
- **权限体系**：统一集成而非严格隔离
- **配置管理**：灵活配置，便于不同环境部署

## 🎪 当前项目范围

### 我们正在开发的部分
```
🔧 壳子层 (qteam-os)
├─ 插件生命周期管理
├─ 资源服务提供
├─ 事件系统
└─ 插件更新机制

📦 SDK层 (qteam-sdk)  
├─ 开发接口定义
├─ 便捷工具类
├─ 资源访问API
└─ 插件基类

🧪 示例插件 (plugin-demos)
├─ HelloWorld插件
├─ 开发模板
└─ 最佳实践示例
```

### 暂未开发的部分
```
📱 开发者平台
├─ 插件仓库管理
├─ 版本控制
├─ 下载分发
└─ 开发者社区
```

## ⚡ 设计原则

### 1. 开发便利优于安全防范
```java
// 简单直接的API设计
DataSource db = getDatabase("business");
CacheService cache = getCache();
UserInfo user = getCurrentUser();
```

### 2. 资源共享优于严格隔离
```yaml
# 灵活的资源配置
datasources:
  primary: "主业务库"
  analytics: "分析库"
  archive: "归档库"
```

### 3. 配置灵活优于一成不变
```yaml
# 支持不同环境的配置
environments:
  dev: { database: "dev_db" }
  test: { database: "test_db" }  
  prod: { database: "prod_db" }
```

## 🚀 未来规划

### 短期目标 (3个月内)
- ✅ 完善插件框架核心功能
- ✅ 丰富SDK开发接口
- ✅ 创建完整的示例插件
- ⏳ 编写详细的开发文档

### 中期目标 (6个月内)
- ⏳ 开发者平台基础版本
- ⏳ 插件市场和分发机制
- ⏳ 企业级部署工具
- ⏳ 监控和运维支持

### 长期目标 (1年内)
- ⏳ 完整的插件生态
- ⏳ 行业解决方案模板
- ⏳ 智能插件推荐
- ⏳ 开发者社区建设

## 📝 重要说明

> **注意**：当前项目专注于壳子+插件层的开发，开发者平台部分暂未实施。我们假定开发者平台已经构建完成，专注于插件运行时的核心功能实现。

> **定位**：这是一个面向软件定制开发的插件化框架，不是面向最终用户的软件产品。主要用户是软件开发商和企业IT部门。 