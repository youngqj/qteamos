<!--
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-30 18:09:32
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-05-02 11:08:53
 * @FilePath: /qteamos/docs/changelogs/CHANGELOG.md
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
-->
# QTeamOS 变更日志

本文件记录了QTeamOS项目的所有显著变更。

## [未发布]

### 新增功能
- 初始项目结构搭建
- 基础核心框架实现
- 插件健康监控系统
- 插件资源配额管理机制
- 系统初始化协调机制(SystemInitializer)
- 核心服务默认实现(数据库、缓存、网关、安全)
- 插件数据源管理器完善与增强
- 优化热加载机制实现
- 完整健康检查服务实现
- 沙盒安全机制增强

### 优化改进
- 网关层优化与重构
- API路径规范化处理
- 插件控制器发现机制增强
- 优化系统启动流程，实现阶段性启动策略
- 重构插件系统初始化机制，改为由SystemInitializer协调调用
- 增强数据源安全性和隔离机制
- 改进插件类加载隔离机制
- 完善资源释放机制，防止内存泄漏
- 优化热更新流程，提高稳定性
- 增强插件状态监控和自动故障恢复能力
- 建立完整的插件健康指标体系
- 实现更精细的资源访问控制
- 优化插件权限管理机制
- 增强插件运行时行为监控与安全隔离

### 问题修复
- 修复网关层API前缀处理问题
- 修复日期标注问题
- 解决类型安全警告
- 修复SystemBanner中重复版权声明问题
- 修正application.yml中的日志配置，将"qelebase"更新为"qteamos"
- 修复热更新时资源未完全释放的问题
- 解决类加载冲突导致的稳定性问题
- 修复插件健康检查中的并发问题
- 解决监控数据积累导致的内存占用问题
- 修复插件安全隔离中的权限越界问题
- 解决插件资源监控的性能开销问题

### 破坏性变更
- 尚无破坏性变更

## 2025年
- [2025年5月](./2025/2025-05-08.md)
- [2025年5月](./2025/2025-05-07.md)
- [2025年5月](./2025/2025-05-06.md)
- [2025年5月](./2025/2025-05-05.md)
- [2025年5月](./2025/2025-05-04.md)
- [2025年4月](./2025/2025-04.md)
