# 插件系统重构文档

## DynamicClassLoader（动态类加载器）篇章

### 1. 模块概述

DynamicClassLoader模块是插件系统的核心组件之一，负责处理插件类的加载、隔离和卸载。该模块通过自定义类加载机制，实现了插件与主程序之间的类隔离，有效防止类冲突和内存泄漏问题。

### 2. 核心功能

- **类加载机制**：支持父优先和子优先两种策略
- **隔离包管理**：通过配置隔离、共享和禁止包列表，确保类的隔离性
- **内存泄漏防护**：提供资源注册和释放机制，防止内存泄漏
- **类卸载机制**：支持插件类加载器的完全卸载，释放相关资源

### 3. 模块结构

DynamicClassLoader模块包含以下核心类：

- **ClassLoadingStrategy**：类加载策略枚举，定义了PARENT_FIRST和CHILD_FIRST两种加载策略
- **ClassLoaderConfiguration**：类加载器配置类，管理隔离包、共享包和禁止包的配置
- **ClassLoadingException**：类加载异常类，处理加载过程中的各种异常情况
- **DynamicClassLoader**：核心类加载器实现，继承自URLClassLoader，提供类加载和资源管理功能
- **DynamicClassLoaderFactory**：类加载器工厂，负责创建和管理DynamicClassLoader实例

### 4. 类加载策略

模块支持两种类加载策略：

1. **PARENT_FIRST（父优先）**：
   - 先尝试由父加载器加载类，如果失败才由插件类加载器加载
   - 遵循Java标准的双亲委派模型
   - 适合大多数场景，防止类冲突

2. **CHILD_FIRST（子优先）**：
   - 先尝试由插件类加载器加载类，如果失败才由父加载器加载
   - 允许插件覆盖系统类，提供更大的灵活性
   - 需谨慎使用，可能导致类型冲突

### 5. 包隔离机制

通过ClassLoaderConfiguration可以配置三种类型的包：

1. **隔离包（isolatedPackages）**：
   - 这些包优先从插件类加载器加载
   - 适用于插件私有的实现类

2. **共享包（sharedPackages）**：
   - 这些包优先从父类加载器加载
   - 适用于插件API和公共库

3. **禁止包（blockedPackages）**：
   - 这些包不允许从插件中加载
   - 保护系统核心类，防止安全风险

### 6. 内存泄漏防护

DynamicClassLoader提供了以下机制防止内存泄漏：

- **资源注册**：通过registerCloseable方法注册需要释放的资源
- **自动关闭**：在类加载器关闭时，自动关闭所有注册的资源
- **引用管理**：使用WeakReference管理资源引用，避免强引用导致的内存泄漏
- **触发GC**：在类加载器关闭后可选择性触发垃圾回收

### 7. 使用示例

#### 7.1 创建类加载器

```java
// 创建类加载器工厂
DynamicClassLoaderFactory factory = new DynamicClassLoaderFactory();

// 使用默认配置创建类加载器
File pluginFile = new File("/path/to/pluginInterface.jar");
DynamicClassLoader classLoader = factory.createClassLoader("pluginInterface-id", pluginFile);

// 使用自定义配置创建类加载器
ClassLoaderConfiguration config = ClassLoaderConfiguration.builder()
    .strategy(ClassLoadingStrategy.CHILD_FIRST)
    .build();
    
DynamicClassLoader customLoader = factory.createClassLoader("pluginInterface-id", pluginFile, config);
```

#### 7.2 加载插件类

```java
try {
    // 加载插件主类
    Class<?> pluginClass = classLoader.loadClass("com.example.pluginInterface.PluginMain");
    
    // 实例化插件
    Object pluginInstance = pluginClass.getDeclaredConstructor().newInstance();
    
    // 调用插件方法
    Method method = pluginClass.getMethod("initialize");
    method.invoke(pluginInstance);
} catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | 
         IllegalAccessException | InvocationTargetException e) {
    // 处理异常
}
```

#### 7.3 释放资源

```java
// 释放单个插件的类加载器
factory.releaseClassLoader("pluginInterface-id");

// 释放所有类加载器
factory.releaseAllClassLoaders();
```

### 8. 最佳实践

1. **选择合适的类加载策略**：
   - 大多数情况下使用PARENT_FIRST策略
   - 只有在插件需要覆盖系统功能时才使用CHILD_FIRST策略

2. **合理配置包隔离**：
   - 将系统核心包（如java.*, javax.*）添加到禁止包列表
   - 将公共API和框架添加到共享包列表
   - 将插件私有实现添加到隔离包列表

3. **资源管理**：
   - 及时释放不再使用的类加载器
   - 使用registerCloseable方法注册需要释放的资源
   - 避免跨类加载器引用，防止内存泄漏

4. **异常处理**：
   - 捕获并处理ClassLoadingException，根据异常类型做出相应处理
   - 记录详细的异常信息，便于调试和排错

### 9. 注意事项

1. **类型转换**：
   - 不同类加载器加载的相同类在JVM中被视为不同类型
   - 避免直接在插件类和系统类之间进行类型转换，应使用反射或接口

2. **静态变量**：
   - 插件间的静态变量不共享，每个插件类加载器有独立的静态变量空间
   - 如需共享状态，考虑使用共享服务或事件机制

3. **类加载性能**：
   - 类加载操作相对耗时，避免频繁加载/卸载
   - 合理使用类缓存机制，提高性能

4. **插件隔离与通信**：
   - 强隔离提高安全性但增加通信复杂度
   - 通过明确定义的API和接口进行插件间通信

### 10. 未来扩展

1. **热部署支持**：
   - 增强类加载器，支持在不停机的情况下更新插件
   - 实现类版本管理，处理类更新后的状态迁移

2. **安全沙箱**：
   - 增加权限控制机制，限制插件的系统访问权限
   - 实现资源配额管理，防止插件消耗过多系统资源

3. **监控与诊断**：
   - 提供类加载器的监控接口，收集类加载统计信息
   - 增加类加载诊断工具，帮助排查类加载相关问题

4. **多租户支持**：
   - 扩展类加载器以支持多租户环境
   - 实现租户间的资源隔离和安全边界

### 11. 常见问题解答

1. **Q: 为什么需要自定义类加载器？**  
   A: 标准类加载器无法满足插件系统对类隔离和卸载的需求。自定义类加载器可以实现更灵活的类加载策略和资源管理。

2. **Q: 如何解决NoClassDefFoundError和ClassNotFoundException问题？**  
   A: 这些问题通常由类加载路径配置错误或包隔离策略不当引起。检查类路径、包配置，确保相关依赖可访问。

3. **Q: 如何处理插件间的依赖关系？**  
   A: 插件依赖管理应通过插件系统统一处理，可以使用共享库机制或服务注册发现模式解决依赖问题。

4. **Q: 类加载器使用后为什么没有被GC回收？**  
   A: 可能存在类加载器泄漏问题，常见原因包括：静态引用、线程局部变量、JNI引用等。使用堆分析工具排查具体原因。

5. **Q: 子优先加载策略有什么风险？**  
   A: 子优先策略可能导致类型不一致问题，尤其是当插件加载的类与系统类同名但实现不同时。谨慎使用，并严格测试。





# 插件系统 - 内部数据模型篇

本文档描述了QEleBase插件系统的内部数据模型设计，包括插件描述符、插件依赖、扩展点等核心组件，以及相关的数据库表结构。

## 核心数据模型

### 1. 插件描述符 (PluginDescriptor)

插件描述符是插件的核心元数据，描述了插件的基本信息、依赖关系、扩展点等内容。主要字段包括：

- **pluginId**: 插件唯一标识符
- **name**: 插件名称
- **version**: 插件版本
- **description**: 插件描述
- **author**: 插件作者
- **mainClass**: 插件主类
- **type**: 插件类型 (normal/system)
- **trust**: 信任级别 (trusted/official)
- **dependencies**: 插件依赖列表
- **requiredSystemVersion**: 所需最低系统版本
- **enabled**: 是否启用
- **priority**: 插件优先级
- **properties**: 插件配置项
- **permissions**: 插件申请的权限列表
- **updateInfo**: 更新相关信息
- **lifecycle**: 生命周期钩子配置
- **extensionPoints**: 插件提供的扩展点列表
- **resources**: 插件资源文件列表
- **metadata**: 插件元数据

### 2. 插件依赖 (PluginDependency)

描述插件之间的依赖关系，主要字段包括：

- **pluginId**: 依赖的插件ID
- **versionRequirement**: 版本要求（如：>=1.0.0 <2.0.0）
- **optional**: 是否可选依赖

### 3. 扩展点 (ExtensionPoint)

描述插件提供的可被其他插件扩展的接口点，主要字段包括：

- **id**: 扩展点ID
- **name**: 扩展点名称
- **description**: 扩展点描述
- **type**: 扩展点类型
- **interfaceClass**: 扩展点接口或抽象类全限定名
- **multiple**: 是否允许多个实现
- **required**: 是否必须实现

### 4. 资源文件 (PluginResource)

描述插件包含的资源文件，主要字段包括：

- **path**: 资源路径
- **type**: 资源类型 (file/directory)
- **description**: 资源描述
- **required**: 是否必须存在

## 数据库表结构

为了持久化存储插件信息，插件系统定义了以下表结构，所有表名均以`sys_plugin_`为前缀，以避免与插件业务表冲突。

### 1. 插件基本信息表 (sys_plugin_info)

存储插件的基本信息，如ID、名称、版本等。

```sql
CREATE TABLE `sys_plugin_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plugin_id` varchar(100) NOT NULL COMMENT '插件唯一标识符',
  `name` varchar(100) NOT NULL COMMENT '插件名称',
  `version` varchar(20) NOT NULL COMMENT '插件版本',
  ...
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plugin_id_version` (`plugin_id`, `version`)
)
```

### 2. 插件状态表 (sys_plugin_status)

记录插件的运行状态，如是否启用、当前状态等。

```sql
CREATE TABLE `sys_plugin_status` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plugin_id` varchar(100) NOT NULL COMMENT '插件唯一标识符',
  `version` varchar(20) NOT NULL COMMENT '插件版本',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `status` varchar(20) DEFAULT 'INSTALLED' COMMENT '插件状态',
  ...
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plugin_id_version` (`plugin_id`, `version`)
)
```

### 3. 插件依赖关系表 (sys_plugin_dependency)

存储插件之间的依赖关系。

```sql
CREATE TABLE `sys_plugin_dependency` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plugin_id` varchar(100) NOT NULL COMMENT '插件唯一标识符',
  `plugin_version` varchar(20) NOT NULL COMMENT '插件版本',
  `dependency_plugin_id` varchar(100) NOT NULL COMMENT '依赖的插件ID',
  `version_requirement` varchar(50) DEFAULT '*' COMMENT '版本要求',
  `optional` tinyint(1) DEFAULT '0' COMMENT '是否可选依赖',
  ...
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plugin_dependency` (`plugin_id`, `plugin_version`, `dependency_plugin_id`)
)
```

### 4. 插件配置表 (sys_plugin_config)

管理插件的配置项。

```sql
CREATE TABLE `sys_plugin_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plugin_id` varchar(100) NOT NULL COMMENT '插件唯一标识符',
  `plugin_version` varchar(20) NOT NULL COMMENT '插件版本',
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_value` text COMMENT '配置值',
  ...
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plugin_config` (`plugin_id`, `plugin_version`, `config_key`)
)
```

### 5. 其他相关表

- **sys_plugin_update_history**: 记录插件的更新历史
- **sys_plugin_migration**: 跟踪数据库迁移脚本的执行情况
- **sys_plugin_permission**: 管理插件所需的权限
- **sys_plugin_author**: 存储插件作者信息
- **sys_plugin_resource**: 记录插件包含的资源文件
- **sys_plugin_extension_point**: 管理插件提供的扩展点
- **sys_plugin_extension_impl**: 跟踪插件对扩展点的实现

## 配置文件格式 (pluginInterface.yml)

插件描述文件采用YAML格式，示例如下：

```yaml
# 插件基本信息
pluginId: "example-pluginInterface"        # 插件唯一标识符
name: "示例插件"                  # 插件名称
version: "1.0.0"                 # 插件版本
description: "这是一个示例插件，用于测试和验证插件系统"
main: "com.xiaoqu.example.ExamplePlugin"  # 插件主类

# 插件特性
author: "qelebase@xiaoquio.com"  # 插件作者
type: "system"                   # 插件类型: normal-普通插件, system-系统插件
trust: "trusted"                 # 信任级别: trusted-受信任的, official-官方的

# 系统兼容性
requiredSystemVersion: "2.0.0"   # 所需最低系统版本
enabled: true                    # 是否默认启用
priority: 10                     # 插件优先级，数值越小优先级越高

# 依赖信息
dependencies:                    # 插件依赖列表
  - pluginId: "core-pluginInterface"      # 依赖的插件ID
    versionRequirement: ">=1.0.0 <2.0.0"  # 版本要求
    optional: false              # 是否可选依赖

# 扩展点
extensionPoints:                 # 插件提供的扩展点
  - id: "example.menu"           # 扩展点ID
    name: "菜单扩展"              # 扩展点名称
    description: "允许其他插件添加菜单项"  # 扩展点描述
    interfaceClass: "com.xiaoqu.example.extension.MenuExtension"  # 扩展接口全限定名

# 生命周期钩子
lifecycle:                       # 生命周期钩子配置
  init: "onInit"                 # 初始化方法名
  start: "onStart"               # 启动方法名
  stop: "onStop"                 # 停止方法名

# 更新管理
update:
  previousVersion: "0.9.0"       # 上一个版本号
  databaseChange: true           # 是否包含数据库变更
  migrationScripts:              # 数据库迁移脚本列表
    - "db/migration/V1.0.0__initial_schema.sql"
```

## 加载与解析流程

1. **PluginDescriptorLoader**类负责从JAR文件中加载`pluginInterface.yml`并解析为`PluginDescriptor`对象
2. 解析过程支持新旧两种格式，保证兼容性
3. 加载时会验证必填字段、版本格式等
4. 解析成功后，将相关信息保存到数据库中

## 数据模型关系图

```
PluginDescriptor
  ├── PluginDependency (0..*)
  ├── ExtensionPoint (0..*)
  └── PluginResource (0..*)
```

## 常见问题

1. **如何处理版本兼容性？**
   - 通过`requiredSystemVersion`字段检查兼容性
   - 使用语义化版本规范验证版本格式

2. **插件依赖冲突如何解决？**
   - 基于版本要求检查依赖冲突
   - 支持可选依赖（optional=true）

3. **插件升级时如何处理数据迁移？**
   - 通过`update`部分定义迁移脚本
   - 系统会自动执行并记录迁移状态

4. **如何管理插件权限？**
   - 通过`permissions`声明所需权限
   - 由系统决定是否授予权限
   - 权限信息持久化到`sys_plugin_permission`表 


   

# 插件系统 - 插件管理实现篇

本文档描述了QEleBase插件系统的插件管理实现，包括插件注册表、生命周期管理、依赖解析、资源共享以及状态管理等核心组件。

## 核心组件

### 1. PluginRegistry（插件注册表）

插件注册表负责管理所有已安装的插件信息，提供插件查询、注册和移除功能。

**主要功能：**
- 注册新插件
- 更新插件信息
- 根据插件ID查询插件
- 获取所有已注册插件列表
- 按状态筛选插件
- 从注册表中移除插件

**核心方法：**
```java
// 注册插件
boolean registerPlugin(PluginInfo pluginInfo);
// 更新插件信息
void updatePlugin(PluginInfo pluginInfo);
// 移除插件
Optional<PluginInfo> unregisterPlugin(String pluginId);
// 获取插件信息
Optional<PluginInfo> getPlugin(String pluginId);
// 获取所有插件
Collection<PluginInfo> getAllPlugins();
// 按状态筛选插件
Collection<PluginInfo> getPluginsByState(boolean enabled);
```

### 2. PluginLifecycleManager（生命周期管理器）

生命周期管理器负责管理插件的完整生命周期，包括加载、初始化、启动、停止、卸载等阶段。

**主要功能：**
- 加载插件：从JAR文件加载插件类
- 创建插件实例：实例化插件主类
- 初始化插件：调用插件初始化方法
- 启动/停止插件：激活或停止插件功能
- 卸载插件：释放插件资源
- 重新加载插件：支持热更新

**核心方法：**
```java
// 加载插件
boolean loadPlugin(PluginInfo pluginInfo) throws PluginLifecycleException;
// 初始化插件
boolean initializePlugin(String pluginId) throws PluginLifecycleException;
// 启动插件
boolean startPlugin(String pluginId) throws PluginLifecycleException;
// 停止插件
boolean stopPlugin(String pluginId) throws PluginLifecycleException;
// 卸载插件
boolean unloadPlugin(String pluginId) throws PluginLifecycleException;
// 重新加载插件
boolean reloadPlugin(String pluginId) throws PluginLifecycleException;
```

### 3. DependencyResolver（依赖解析器）

依赖解析器负责检查和解析插件之间的依赖关系，确保插件按正确顺序加载和卸载。

**主要功能：**
- 检查插件依赖是否满足
- 获取插件的所有依赖（包括传递依赖）
- 检测循环依赖
- 计算插件加载的拓扑排序
- 获取依赖特定插件的插件列表

**核心方法：**
```java
// 检查依赖是否满足
boolean checkDependencies(PluginDescriptor descriptor);
// 获取所有依赖
List<String> getAllDependencies(String pluginId) throws PluginDependencyException;
// 获取拓扑排序结果
List<String> getTopologicalOrder() throws PluginDependencyException;
// 获取依赖特定插件的所有插件
List<String> getDependentPlugins(String pluginId);
```

### 4. PluginResourceBridge（资源桥接器）

资源桥接器负责管理插件和主应用之间的资源共享，提供资源代理和访问控制。

**主要功能：**
- 创建插件上下文
- 提供资源访问能力
- 共享数据源等核心资源
- 资源生命周期管理

**核心方法：**
```java
// 创建插件上下文
PluginContext createPluginContext(PluginInfo pluginInfo);
// 获取共享资源
<T> T getSharedResource(String name, Class<T> type);
```

### 5. PluginStateManager（状态管理器）

状态管理器负责监控和管理插件状态变化，触发相应的事件通知。

**主要功能：**
- 记录插件状态变化
- 发布状态变化事件
- 获取插件当前状态
- 获取处于特定状态的插件列表
- 获取失败的插件列表

**核心方法：**
```java
// 记录状态变化
void recordStateChange(String pluginId, PluginState newState);
// 获取插件状态
Optional<PluginState> getPluginState(String pluginId);
// 检查插件是否处于特定状态
boolean isInState(String pluginId, PluginState state);
// 获取处于特定状态的插件列表
List<String> getPluginsInState(PluginState state);
```

### 6. PluginSystem（插件系统）

插件系统是整个插件框架的入口，整合所有组件，提供统一的对外接口。

**主要功能：**
- 初始化插件系统
- 扫描和加载插件
- 安装新插件
- 卸载和删除插件
- 启用/禁用插件
- 获取插件列表和状态信息

**核心方法：**
```java
// 扫描并加载插件
void scanAndLoadPlugins();
// 加载单个插件
String loadPlugin(Path jarPath) throws Exception;
// 卸载插件
boolean unloadPlugin(String pluginId);
// 安装新插件
boolean installPlugin(File jarFile);
// 卸载并删除插件
boolean uninstallPlugin(String pluginId);
// 启用插件
boolean enablePlugin(String pluginId);
// 禁用插件
boolean disablePlugin(String pluginId);
```

## 插件API接口

### 1. Plugin（插件接口）

所有插件必须实现的基础接口，定义了插件的生命周期方法。

```java
public interface Plugin {
    // 初始化插件
    void init(PluginContext context) throws Exception;
    // 启动插件
    void start() throws Exception;
    // 停止插件
    void stop() throws Exception;
    // 销毁插件
    void destroy() throws Exception;
}
```

### 2. PluginContext（插件上下文）

插件上下文接口，提供插件与主应用交互的环境。

```java
public interface PluginContext {
    // 获取插件ID
    String getPluginId();
    // 获取插件类加载器
    ClassLoader getClassLoader();
    // 获取资源提供者
    PluginResourceProvider getResourceProvider();
}
```

### 3. PluginResourceProvider（资源提供者）

资源提供者接口，定义了插件获取主应用资源的标准方法。

```java
public interface PluginResourceProvider {
    // 获取数据源
    DataSource getDataSource();
    // 获取指定名称的资源
    Object getResource(String name);
    // 获取指定名称和类型的资源
    <T> T getResource(String name, Class<T> type);
    // 获取指定类型的资源
    <T> T getResource(Class<T> type);
}
```

## 异常处理

插件管理模块定义了以下异常类处理不同类型的异常情况：

- **PluginLifecycleException**：生命周期异常，在插件加载、初始化、启动、停止和卸载过程中可能抛出
- **PluginDependencyException**：依赖异常，在处理插件依赖关系时可能抛出，如循环依赖、缺失依赖等
- **PluginInitializeException**：初始化异常，在插件初始化过程中可能抛出

## 插件状态流转

插件在生命周期中会经历以下状态：

1. **CREATED**：已创建，尚未加载
2. **LOADED**：已加载，尚未初始化
3. **INITIALIZED**：已初始化，尚未启动
4. **STARTED**：正在运行
5. **STOPPED**：已停止
6. **UNLOADED**：已卸载
7. **DISABLED**：已禁用
8. **ERROR**：错误状态

状态流转图：
```
CREATED → LOADED → INITIALIZED → STARTED ←→ STOPPED → UNLOADED
                                  ↑          ↓
                                  ↑          ↓
                               DISABLED     ERROR
```

## 最佳实践

1. **插件隔离与通信**：
   - 使用明确定义的API进行插件与主应用通信
   - 避免插件直接访问主应用内部结构
   - 通过事件机制实现插件间松耦合通信

2. **依赖管理**：
   - 明确声明插件依赖
   - 避免循环依赖
   - 使用可选依赖处理非必要依赖

3. **资源管理**：
   - 插件使用完资源后及时释放
   - 避免插件持有主应用资源的强引用
   - 使用资源代理模式访问共享资源

4. **异常处理**：
   - 插件中的异常不应影响主应用和其他插件
   - 使用合适的异常类型传递错误信息
   - 记录详细的异常日志，便于排查问题

## 解决的关键问题

1. **资源共享问题**：
   - 通过PluginResourceBridge提供统一的资源访问机制
   - 实现数据源等核心资源的安全共享
   - 解决了SMS插件无法获取数据源的问题

2. **插件依赖管理**：
   - 通过DependencyResolver实现依赖检查和解析
   - 支持版本兼容性检查
   - 使用拓扑排序确保按正确顺序加载和卸载插件

3. **生命周期管理**：
   - 提供完整的插件生命周期管理
   - 支持热更新和动态加载/卸载
   - 优雅处理插件启动和停止

4. **状态监控与事件通知**：
   - 通过PluginStateManager监控插件状态变化
   - 实现事件驱动的插件系统
   - 支持插件状态变化的监听和响应 

# 插件系统 - 事件系统篇

本文档描述了QEleBase插件系统的事件系统设计和实现，包括事件机制、事件总线、事件监听器等核心组件。

## 核心组件

### 1. Event（事件接口）

事件接口定义了事件的基本属性和方法，所有具体事件必须实现此接口。

**核心方法：**
```java
// 获取事件主题
String getTopic();
// 获取事件类型
String getType();
// 获取事件来源
String getSource();
// 获取事件时间戳
long getTimestamp();
// 事件是否可取消
boolean isCancellable();
// 事件是否已取消
boolean isCancelled();
// 取消事件
boolean cancel();
```

### 2. AbstractEvent（抽象事件）

抽象事件类提供了Event接口的基本实现，作为所有具体事件的基类。

**核心功能：**
- 存储事件的基本属性：主题、类型、来源、时间戳
- 提供事件取消机制
- 实现toString方法，方便调试

### 3. EventBus（事件总线）

事件总线是事件系统的核心，负责事件的注册、分发和管理。

**核心功能：**
- 注册事件处理器：`registerHandler(EventHandler handler)`
- 注销事件处理器：`unregisterHandler(EventHandler handler)`
- 发布事件：`postEvent(Event event)`
- 同步和异步事件处理支持
- 事件匹配和过滤机制
- 事件处理优先级支持

**内部实现：**
- 使用线程池处理异步事件
- 按主题和类型组织事件处理器
- 支持事件传播和取消
- 提供优雅关闭机制

### 4. EventHandler（事件处理器）

事件处理器接口定义了处理事件的标准方法。

**核心方法：**
```java
// 处理事件
boolean handle(Event event);
// 获取处理器优先级
int getPriority();
// 获取关注的主题
String[] getTopics();
// 获取关注的类型
String[] getTypes();
// 是否同步处理
boolean isSynchronous();
// 异常时是否继续传播
boolean isContinueOnError();
```

### 5. EventListener（事件监听器注解）

事件监听器注解用于标记方法为事件处理方法，可以自动注册到事件总线。

**使用示例：**
```java
@Component
public class MyEventListener {
    
    @EventListener(
        topics = {"pluginInterface"},
        types = {"loaded", "started"},
        priority = 10,
        synchronous = true
    )
    public boolean onPluginEvent(PluginEvent event) {
        // 处理插件事件
        System.out.println("Plugin event: " + event.getPluginId());
        return true;
    }
}
```

### 6. EventListenerProcessor（事件监听器处理器）

事件监听器处理器用于自动扫描事件监听器注解并注册到事件总线。

**核心功能：**
- 扫描带有@EventListener注解的方法
- 创建MethodEventHandler包装被注解的方法
- 注册处理器到事件总线
- 支持Spring代理对象处理

## 事件类型

### 1. 系统事件

系统事件用于通知系统级别的状态变化。

- **SystemStartupEvent**：系统启动事件，通知插件系统已启动完成
- **SystemShutdownEvent**：系统关闭事件，通知插件系统即将关闭

### 2. 插件事件

插件事件用于通知插件状态变化。

- **PluginEvent**：插件基础事件，包含以下类型：
  - **loaded**：插件已加载
  - **initialized**：插件已初始化
  - **started**：插件已启动
  - **stopped**：插件已停止
  - **unloaded**：插件已卸载
  - **enabled**：插件已启用
  - **disabled**：插件已禁用
  - **error**：插件发生错误
  - **dependency_failed**：插件依赖检查失败

## 使用示例

### 1. 发布事件

```java
// 创建事件总线实例（通常由Spring自动注入）
EventBus eventBus = new EventBus();

// 创建并发布系统启动事件
SystemStartupEvent startupEvent = new SystemStartupEvent();
eventBus.postEvent(startupEvent);

// 创建并发布插件事件
PluginEvent pluginEvent = PluginEvent.createLoadedEvent("my-pluginInterface", "1.0.0");
eventBus.postEvent(pluginEvent);
```

### 2. 手动注册事件处理器

```java
// 创建事件处理器
EventHandler handler = new EventHandler() {
    @Override
    public boolean handle(Event event) {
        if (event instanceof PluginEvent) {
            PluginEvent pluginEvent = (PluginEvent) event;
            System.out.println("Plugin event: " + pluginEvent.getPluginId());
        }
        return true;
    }
    
    @Override
    public String[] getTopics() {
        return new String[]{"pluginInterface"};
    }
    
    @Override
    public String[] getTypes() {
        return new String[]{"loaded", "started"};
    }
};

// 注册处理器
eventBus.registerHandler(handler);
```

### 3. 使用注解注册事件处理器

```java
@Component
public class PluginEventListener {
    
    @EventListener(topics = "pluginInterface", types = {"loaded", "started"})
    public boolean onPluginLoaded(PluginEvent event) {
        System.out.println("Plugin loaded: " + event.getPluginId());
        return true;
    }
    
    @EventListener(topics = "system", types = "shutdown")
    public boolean onSystemShutdown(SystemShutdownEvent event) {
        System.out.println("System shutting down: " + event.getReason());
        // 执行清理操作
        return true;
    }
}
```

## 最佳实践

1. **主题和类型命名**：
   - 主题使用名词，表示事件发生的实体/模块
   - 类型使用动词或状态，表示事件的行为或状态变化
   - 保持命名一致性，便于事件过滤和订阅

2. **事件粒度**：
   - 事件粒度不宜过粗或过细
   - 粒度过粗导致处理逻辑复杂，依赖事件内容判断
   - 粒度过细导致事件类过多，订阅复杂

3. **同步与异步**：
   - 对于需要即时响应的事件，使用同步处理
   - 对于耗时操作，使用异步处理避免阻塞
   - 注意异步处理中的线程安全问题

4. **异常处理**：
   - 事件处理器应捕获和处理自身异常，避免影响事件总线
   - 对于关键事件，建议设置`continueOnError=true`确保传播

5. **事件数据设计**：
   - 事件数据应包含足够信息，但避免过多冗余
   - 事件对象应是不可变的，防止处理过程中被修改
   - 避免在事件中包含敏感信息，如密码等 

## 插件安装流程

### 自动安装流程

1. 将插件放入临时目录（默认为 `./plugins-temp`）
2. 系统自动检测到新插件，进行以下处理：
   - 验证插件的基本信息和完整性
   - 将插件迁移到正式安装目录（默认为 `./plugins`）
   - 自动加载并启动插件（如果是系统插件和受信任插件）

### 手动安装流程

1. 通过管理界面上传插件
2. 系统将插件保存到临时目录
3. 触发与自动安装相同的后续流程

### 配置参数

可以通过以下配置参数自定义插件管理行为：

```properties
# 插件存储路径，默认为./plugins
pluginInterface.storage-path=./plugins

# 插件临时目录，用于存放待验证的插件，默认为./plugins-temp
pluginInterface.temp-dir=./plugins-temp

# 是否开启自动发现插件功能，默认开启
pluginInterface.auto-discover=true

# 是否监控目录变化，默认开启
pluginInterface.watch-dir=true

# 定时扫描间隔（毫秒），默认5分钟
pluginInterface.scan-interval=300000
``` 