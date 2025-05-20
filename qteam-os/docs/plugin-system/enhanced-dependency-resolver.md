# 增强版插件依赖解析器

## 功能概述

增强版插件依赖解析器 (`EnhancedDependencyResolver`) 是 QTeamOS 插件系统的高级组件，提供了复杂依赖关系管理和版本冲突解决功能。相比于基础的依赖解析器，增强版提供了以下关键功能：

1. **版本冲突检测与解决** - 自动识别不同插件对同一依赖的不同版本要求，并提供多种解决策略
2. **依赖图构建** - 构建完整的插件依赖关系图，计算依赖层级
3. **多种冲突解决策略** - 支持最新版本、最旧版本、最近依赖和最高优先级等多种策略
4. **依赖层级计算** - 精确计算插件之间的依赖层级关系
5. **冲突分析与报告** - 提供详细的版本冲突信息

## 冲突解决策略

增强版依赖解析器支持以下四种冲突解决策略：

| 策略 | 说明 | 适用场景 |
|------|------|----------|
| NEWEST | 选择满足所有要求的最新版本 | 默认策略，适合大多数场景，优先使用最新功能 |
| OLDEST | 选择满足所有要求的最旧版本 | 保守策略，适合对稳定性要求较高的环境 |
| NEAREST | 选择依赖层级最近的版本 | 减少依赖传递层级，优化性能 |
| HIGHEST_RANK | 选择优先级最高的插件依赖的版本 | 当某些插件的版本选择权重更高时 |

## 使用示例

### 基本用法

```java
@Autowired
private EnhancedDependencyResolver dependencyResolver;

// 设置冲突解决策略
dependencyResolver.setDefaultStrategy(ResolutionStrategy.NEWEST);

// 检查依赖并解决冲突
boolean dependenciesMet = dependencyResolver.checkDependencies(pluginDescriptor);
```

### 冲突分析

```java
// 获取特定插件的依赖冲突
List<VersionConflict> conflicts = dependencyResolver.detectDependencyConflicts(pluginId);

// 手动解决冲突
dependencyResolver.resolveVersionConflicts(conflicts);

// 查看冲突解决结果
for (VersionConflict conflict : conflicts) {
    log.info("冲突已解决: {}, 选择版本: {}", 
             conflict.getDependencyId(), 
             conflict.getResolvedVersion());
}
```

## 版本冲突示例

### 场景

插件 A 依赖 Common 插件 `>=1.0.0`
插件 B 依赖 Common 插件 `>=2.0.0`
当前系统中有 Common 插件的 1.0.0、1.5.0、2.0.0、2.1.0 版本

### 解决方案

- **NEWEST 策略**: 选择 Common 插件 2.1.0 版本
- **OLDEST 策略**: 选择 Common 插件 2.0.0 版本（最旧的能满足所有要求的版本）
- **NEAREST 策略**: 如果插件 B 是直接依赖，插件 A 是间接依赖，则选择 2.0.0 或更高版本
- **HIGHEST_RANK 策略**: 如果插件 A 优先级为 5，插件 B 优先级为 10（数值越小优先级越高），则选择满足插件 A 要求的版本

## 内部工作原理

1. **依赖图构建** - 构建插件依赖关系的有向图
2. **层级计算** - 使用广度优先搜索计算每个插件到其所有依赖的层级距离
3. **冲突检测** - 识别同一插件不同版本要求的情况
4. **版本筛选** - 根据所有版本要求筛选兼容版本
5. **策略应用** - 根据选择的策略从兼容版本中选择最终版本

## 注意事项

- 设置为 NEWEST 策略可能导致频繁更新，但能使用最新特性
- 选择 OLDEST 策略可能更稳定，但可能缺少新功能
- 如果没有满足所有要求的版本，解析器将抛出 `PluginDependencyException` 异常
- 强烈建议在开发新插件时遵循语义化版本控制规范 