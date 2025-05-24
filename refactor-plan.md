# QTeam插件框架重构计划

## 🎯 重构目标
将现有78分的框架提升到90+分，解决接口不统一、依赖复杂、调试困难等问题。

## 📋 重构策略：新模块并行开发

### Phase 1: 新接口设计 (1-2周)

#### 1.1 创建新API模块
```
qteam-api-v2/
├── src/main/java/com/xiaoqu/qteamos/v2/api/
│   ├── core/
│   │   ├── Plugin.java              // 统一的插件接口
│   │   ├── PluginContext.java       // 简化的上下文接口
│   │   ├── PluginInfo.java          // 插件信息
│   │   └── PluginException.java     // 统一异常
│   ├── service/
│   │   ├── ConfigService.java       // 配置服务
│   │   ├── CacheService.java        // 缓存服务
│   │   └── DataSourceService.java   // 数据源服务
│   └── event/
│       ├── EventBus.java            // 事件总线
│       └── PluginEvent.java         // 插件事件
```

#### 1.2 新接口设计原则
```java
// ✅ 简洁统一的插件接口
public interface Plugin {
    PluginInfo getInfo();
    void initialize(PluginContext context) throws PluginException;
    void start() throws PluginException;
    void stop() throws PluginException;
    void destroy() throws PluginException;
}

// ✅ 简化的上下文接口
public interface PluginContext {
    String getPluginId();
    Config getConfig();
    ServiceRegistry getServices();
    EventBus getEventBus();
    Logger getLogger();
}
```

### Phase 2: SDK重构 (2-3周)

#### 2.1 创建新SDK模块
```
qteam-sdk-v2/
├── pom.xml                          // 内置常用依赖
├── src/main/java/com/xiaoqu/qteamos/v2/sdk/
│   ├── plugin/
│   │   ├── BasePlugin.java          // 统一基类
│   │   └── SimplePlugin.java        // 简化实现
│   ├── util/
│   │   ├── PluginUtils.java         // 工具类
│   │   └── ConfigUtils.java         // 配置工具
│   └── annotation/
│       ├── PluginComponent.java     // 组件注解
│       └── PluginService.java       // 服务注解
```

#### 2.2 依赖内化策略
```xml
<!-- qteam-sdk-v2/pom.xml -->
<dependencies>
    <!-- 内置常用依赖，插件无需声明 -->
    <dependency>
        <groupId>com.xiaoqu</groupId>
        <artifactId>qteam-api-v2</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    <!-- 其他常用依赖... -->
</dependencies>
```

### Phase 3: 核心系统适配 (3-4周)

#### 3.1 兼容性适配器
```java
// 在qteam-os中创建适配器
@Component
public class PluginV2Adapter {
    
    // 新接口插件的加载器
    public void loadV2Plugin(Plugin v2Plugin) {
        // 适配新接口到现有系统
    }
    
    // 保持对V1插件的支持
    public void loadV1Plugin(com.xiaoqu.qteamos.api.core.Plugin v1Plugin) {
        // 现有逻辑不变
    }
}
```

#### 3.2 渐进式迁移
```java
// 现有功能逐步迁移到新接口
@Service
public class PluginManagerV2 implements PluginManager {
    
    @Autowired
    private PluginManagerV1 legacyManager; // 旧实现
    
    @Override
    public void installPlugin(String pluginPath) {
        if (isV2Plugin(pluginPath)) {
            installV2Plugin(pluginPath);  // 新逻辑
        } else {
            legacyManager.installPlugin(pluginPath); // 旧逻辑
        }
    }
}
```

### Phase 4: 插件迁移示例 (1-2周)

#### 4.1 新版HelloWorld插件
```java
// plugin-demos/plugin-helloworld-v2/
@PluginComponent
public class HelloWorldPluginV2 extends BasePlugin {
    
    @Override
    public PluginInfo getInfo() {
        return PluginInfo.builder()
            .id("helloworld-v2")
            .name("HelloWorld V2")
            .version("2.0.0")
            .author("yangqijun")
            .build();
    }
    
    @Override
    protected void onInitialize() {
        getLogger().info("HelloWorld V2 插件初始化");
    }
    
    @Override
    protected void onStart() {
        getLogger().info("HelloWorld V2 插件启动");
    }
}
```

#### 4.2 迁移对比
```java
// ❌ 旧版本：复杂的依赖和初始化
<dependencies>
    <dependency>qteamos-sdk</dependency>
    <dependency>qteamos-api</dependency>  
    <dependency>qteamos-common</dependency>
    <!-- 20+个provided依赖 -->
</dependencies>

public class HelloWorldPlugin extends AbstractPlugin {
    public void init(PluginContext context) throws Exception { }
    public void initPlugin() throws Exception { }
    public void setContext(PluginContext context) { }
    public void setProperties(Map<String, Object> properties) { }
}

// ✅ 新版本：简洁的依赖和初始化  
<dependencies>
    <dependency>
        <groupId>com.xiaoqu</groupId>
        <artifactId>qteam-sdk-v2</artifactId>
    </dependency>
</dependencies>

@PluginComponent
public class HelloWorldPluginV2 extends BasePlugin {
    @Override
    protected void onInitialize() { }
}
```

## 🏷️ 新旧文件标记策略

### 1. 模块级别标记
```
qteam-api      // 标记为 @Deprecated(since="2.0")
qteam-api-v2   // 新版本模块

qteam-sdk      // 标记为 @Deprecated(since="2.0") 
qteam-sdk-v2   // 新版本模块
```

### 2. 代码级别标记
```java
// 旧代码标记
@Deprecated(since = "2.0", forRemoval = true)
@DeprecatedSince(version = "2.0", reason = "使用 qteam-api-v2 中的新接口")
public interface Plugin {
    // 旧接口
}

// 新代码标记
@Since("2.0")
@ApiVersion("2.0")
public interface Plugin {
    // 新接口
}
```

### 3. 文件命名约定
```
src/main/java/com/xiaoqu/qteamos/
├── api/                    // 旧版本 (标记deprecated)
│   └── core/
│       ├── Plugin.java     // @Deprecated
│       └── PluginContext.java
└── v2/                     // 新版本
    └── api/
        └── core/
            ├── Plugin.java     // @Since("2.0")
            └── PluginContext.java
```

### 4. 清理时间表
```
版本 2.0.0: 发布新接口，旧接口标记deprecated
版本 2.1.0: 完成所有核心功能迁移
版本 2.2.0: 删除旧接口，完成重构
```

## 📊 重构收益预估

### 开发体验提升
```
插件依赖数量: 20+ → 1
初始化方式: 4种 → 1种  
接口学习成本: 高 → 低
调试便利性: 差 → 好
```

### 框架评分提升
```
当前评分: 78分
重构后评分: 90+分
提升幅度: 15.4%
```

## ⏱️ 时间安排

| 阶段 | 工期 | 里程碑 |
|------|------|--------|
| Phase 1 | 1-2周 | 新接口设计完成 |
| Phase 2 | 2-3周 | 新SDK发布 |  
| Phase 3 | 3-4周 | 核心系统适配完成 |
| Phase 4 | 1-2周 | 示例插件迁移完成 |
| **总计** | **7-11周** | **重构完成** |

相比从0重写(15-20周)，节省40-45%时间。

## 🚦 风险控制

### 1. 兼容性风险
- 保持V1接口运行，直到V2稳定
- 提供自动迁移工具
- 详细的迁移文档

### 2. 进度风险
- 分阶段交付，每阶段可独立验证
- 并行开发，不影响现有功能
- 预留缓冲时间

### 3. 质量风险
- 每个新接口都要有单元测试
- 新旧版本对比测试
- 代码审查和架构评审

## ✅ 成功标准

重构完成后应该达到：

1. **简洁性**: 插件只需要1个依赖，1种初始化方式
2. **一致性**: 无重复抽象，接口职责清晰
3. **易用性**: 5分钟创建hello world插件
4. **可维护性**: 新功能添加不影响现有插件
5. **高质量**: 代码覆盖率>80%，架构评分>90分 