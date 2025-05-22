# QTeamOS插件生命周期与事件系统

## 插件生命周期

### 状态流转

插件在QTeamOS系统中按照以下状态流转：

1. **CREATED（已创建）**：插件被发现并初始注册，但尚未加载
2. **LOADED（已加载）**：插件JAR文件已加载，类和资源可用
3. **INITIALIZED（已初始化）**：插件已完成初始化
4. **STARTED/RUNNING（已启动/运行中）**：插件完全启动并正常运行
5. **STOPPED（已停止）**：插件已暂停运行但仍在内存中
6. **UNLOADED（已卸载）**：插件已从内存中完全卸载

此外，还有几个特殊状态：
- **DISABLED（已禁用）**：插件被管理员手动禁用
- **ERROR（错误）**：运行期间发生错误
- **DEPENDENCY_FAILED（依赖失败）**：依赖检查不通过
- **FAILED（失败）**：加载、初始化或启动失败

### 完整流程及对应模块

1. **扫描发现**
   - 模块：`PluginSystemCoordinator`
   - 方法：`scanPluginDirectory()`, `scanForNewPlugins()`
   - 描述：扫描插件目录，发现新的插件文件或目录

2. **解析**
   - 模块：`DefaultPluginLoader`, `PluginDescriptorLoader`
   - 方法：`parsePluginDescriptor()`, `loadFromJar()`
   - 描述：解析插件JAR文件中的描述信息，提取元数据

3. **加载**
   - 模块：`PluginLifecycleCoordinator`
   - 方法：`loadPlugin()`
   - 描述：创建插件类加载器，加载插件类和资源
   - 状态变化：CREATED → LOADED

4. **注册**
   - 模块：`PluginRegistry`
   - 方法：`registerPlugin()`
   - 描述：将插件信息注册到插件注册表中

5. **初始化**
   - 模块：`DefaultPluginInitializer`
   - 方法：`initialize()`
   - 描述：调用插件的initialize()方法，完成初始化
   - 状态变化：LOADED → INITIALIZED

6. **启动**
   - 模块：`DefaultPluginStarter`
   - 方法：`start()`
   - 描述：调用插件的start()方法，启动插件功能
   - 状态变化：INITIALIZED → STARTED/RUNNING

7. **控制器注册**
   - 模块：`PluginRequestMappingHandlerMapping`
   - 方法：`registerPluginControllers()`
   - 描述：扫描并注册插件中的Controller，发布API注册事件

8. **灰度发布**
   - 模块：`PluginRolloutManager`
   - 方法：`startGradualRollout()`, `proceedToNextBatch()`
   - 描述：按批次、百分比逐步发布新版本，监控健康状态

9. **正式上线**
   - 模块：`PluginRolloutManager`
   - 描述：完成最后一批次灰度发布，状态变为COMPLETED
   - 状态变化：IN_PROGRESS → COMPLETED

10. **卸载/停止**
    - 模块：`PluginLifecycleCoordinator`
    - 方法：`stopPlugin()`, `unloadPlugin()`
    - 描述：停止并卸载插件，释放资源

## 事件系统

QTeamOS插件系统采用事件驱动架构，实现了松耦合的组件通信机制。

### 主要事件类型

- **插件生命周期事件**：加载、初始化、启动、停止、卸载等事件
- **灰度发布事件**：开始、批次完成、暂停、恢复、完成、失败等事件
- **API注册事件**：插件控制器注册和卸载事件

### 事件处理流程

1. 事件发布者通过`EventBus`发布事件
2. `EventBus`根据事件类型和主题分发事件
3. 注册了相应主题和类型的`EventListener`接收并处理事件

### 事件监听器示例

- `RolloutEventListener`：监听灰度发布相关事件
- `RolloutEventSampleImpl`：演示如何将灰度事件集成到网关模块

## 时序图

### 插件加载与启动时序图

```
PluginSystemCoordinator      PluginLifecycleCoordinator     PluginRegistry           EventBus                插件实例                  控制器注册
    |                               |                             |                       |                       |                         |
    |--扫描插件目录---------------->|                             |                       |                       |                         |
    |                               |                             |                       |                       |                         |
    |--解析插件描述符-------------->|                             |                       |                       |                         |
    |                               |                             |                       |                       |                         |
    |--创建插件信息--------------->|                              |                       |                       |                         |
    |                               |                             |                       |                       |                         |
    |                               |---注册插件------------------>|                       |                       |                         |
    |                               |                             |                       |                       |                         |
    |                               |---加载插件------------------->|                       |                       |                         |
    |                               |                             |                       |                       |                         |
    |                               |---发布加载事件--------------->|---事件通知------------>|                       |                         |
    |                               |                             |                       |                       |                         |
    |                               |---初始化插件---------------->|                       |---initialize()-------->|                         |
    |                               |                             |                       |                       |                         |
    |                               |---发布初始化事件------------->|---事件通知------------>|                       |                         |
    |                               |                             |                       |                       |                         |
    |                               |---启动插件------------------->|                       |---start()------------>|                         |
    |                               |                             |                       |                       |                         |
    |                               |---发布启动事件--------------->|---事件通知------------>|                       |                         |
    |                               |                             |                       |                       |                         |
    |                               |                             |                       |                       |                         |---注册控制器--->|
    |                               |                             |                       |                       |                         |---发布API注册事件->|
```

### 灰度发布时序图

```
用户/管理员             PluginRolloutManager           PluginLifecycleCoordinator  EventBus               RolloutEventListener       网关模块
    |                         |                               |                         |                         |                         |
    |--启动灰度发布---------->|                               |                         |                         |                         |
    |                         |                               |                         |                         |                         |
    |                         |---发布灰度开始事件------------>|------------------------->|---处理灰度开始事件------>|                         |
    |                         |                               |                         |                         |                         |
    |                         |---计算批次百分比-------------->|                         |                         |                         |
    |                         |                               |                         |                         |                         |
    |                         |---批次开始事件--------------->|------------------------->|---处理批次开始事件------>|---准备网关配置---------->|
    |                         |                               |                         |                         |                         |
    |                         |---执行插件更新--------------->|---更新插件--------------->|                         |                         |
    |                         |                               |                         |                         |                         |
    |                         |---批次完成事件--------------->|------------------------->|---处理批次完成事件------>|---调整流量分配---------->|
    |                         |                               |                         |                         |                         |
    |                         |---健康检查------------------->|                         |                         |                         |
    |                         |                               |                         |                         |                         |
    |                         |---执行下一批次---------------->|                         |                         |                         |
    |                         |        ...                    |                         |                         |                         |
    |                         |---发布完成事件--------------->|------------------------->|---处理完成事件---------->|---更新全量路由---------->|
```

## 控制器注册流程

1. 插件启动后，Spring容器刷新时触发`PluginRequestMappingHandlerMapping`的初始化
2. 扫描所有已启动的插件，获取插件中的控制器类
3. 控制器类的发现通过以下方式：
   - 从插件元数据中获取显式声明的控制器
   - 调用插件提供的`getControllerClasses()`方法获取控制器列表
   - 扫描插件主包中带有`@RestController`注解的类
4. 为每个控制器方法创建`RequestMappingInfo`并注册到Spring MVC
5. 发布API注册事件，通知网关和其他相关模块

## 版本管理相关流程

- **版本存储**：`EnhancedPluginVersionManager` 负责将插件版本保存到版本库
- **版本回滚**：`PluginRolloutManager.rollbackToVersion()` 在灰度失败时回退到稳定版本
- **版本升级路径**：`EnhancedPluginVersionManager.getUpgradePath()` 计算版本间升级路径

## 事件驱动优势

1. **松耦合**：组件之间通过事件通信，减少直接依赖
2. **可扩展性**：新组件可以通过监听事件集成到系统中
3. **异步处理**：支持同步和异步事件处理
4. **优先级控制**：通过优先级控制事件处理顺序
5. **可测试性**：事件机制使组件更易于单元测试 