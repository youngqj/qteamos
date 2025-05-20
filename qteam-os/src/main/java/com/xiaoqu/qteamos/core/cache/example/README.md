# 缓存服务模块

缓存服务模块提供统一的缓存操作接口，支持多种缓存实现，适用于插件开发中的各种缓存需求。

## 特性

- **统一API**：提供统一的接口操作不同类型的缓存
- **多种实现**：支持文件缓存、Redis缓存和Caffeine本地缓存
- **丰富的数据结构**：支持字符串、Hash、Set、List等多种数据结构
- **灵活配置**：通过配置文件轻松切换缓存类型和参数
- **插件友好**：插件开发者可以直接使用缓存服务，无需关心底层实现

## 缓存类型

1. **文件缓存 (FILE)**
   - 基于文件系统存储的持久化缓存
   - 适用于单机环境和需要持久化的场景
   - 支持所有缓存操作

2. **Redis缓存 (REDIS)**
   - 基于Redis的分布式缓存
   - 适用于分布式环境和高性能场景
   - 支持所有缓存操作和原生Redis功能

3. **Caffeine缓存 (CAFFEINE)**
   - 基于Caffeine的高性能本地内存缓存
   - 适用于对性能要求极高的场景
   - 支持所有缓存操作，内存占用低

## 使用方法

### 配置

在`application.yml`中配置缓存服务：

```yaml
cache:
  # 缓存类型：FILE, REDIS, CAFFEINE
  type: FILE
  # 是否启用
  enabled: true
  # 全局过期时间（秒），默认24小时，-1表示永不过期
  default-expiration: 86400
  # 缓存键前缀
  key-prefix: "qteamos:"
  
  # 文件缓存配置
  file:
    directory: "${java.io.tmpdir}/qteamos-cache"
    serialized: true
    clean-interval: 3600
  
  # Redis缓存配置
  redis:
    host: localhost
    port: 6379
    password: 
    database: 0
    timeout: 2000
    ssl: false
  
  # Caffeine缓存配置
  caffeine:
    initial-capacity: 100
    maximum-size: 10000
    record-stats: false
```

### 在代码中使用

直接注入`CacheService`使用：

```java
@Autowired
private CacheService cacheService;

// 设置缓存
cacheService.set("key", "value");

// 设置带过期时间的缓存
cacheService.set("key", "value", 1, TimeUnit.HOURS);

// 获取缓存
String value = cacheService.get("key", String.class);

// 删除缓存
cacheService.delete("key");
```

### 使用Hash结构

```java
// 设置Hash值
cacheService.setHashValue("user:1", "name", "张三");
cacheService.setHashValue("user:1", "age", 30);

// 获取Hash值
String name = cacheService.getHashValue("user:1", "name", String.class);

// 获取整个Hash
Map<String, Object> userMap = cacheService.getEntireHash("user:1");
```

### 使用Set结构

```java
// 添加到Set
cacheService.addToSet("tags", "Java", "Spring", "Redis");

// 获取Set成员
Set<String> tags = cacheService.getSet("tags", String.class);
```

### 使用List结构

```java
// 添加到List
cacheService.leftPush("logs", "日志1");
cacheService.rightPush("logs", "日志2");

// 获取List
List<String> logs = cacheService.getList("logs", 0, -1, String.class);
```

## 示例

参考`CacheExampleService`类获取更多使用示例。

## 依赖

根据选择的缓存类型，确保添加相应的依赖：

- 文件缓存：无需额外依赖
- Redis缓存：`spring-boot-starter-data-redis`
- Caffeine缓存：`com.github.ben-manes.caffeine:caffeine` 

## 详细文档

更多详细说明和使用方法，请参考[缓存服务模块使用手册](../../../../../docs/cache/cache-service-guide.md)。 