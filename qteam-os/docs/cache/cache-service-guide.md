# QTeamOS 缓存服务模块使用手册

## 1. 模块概述

缓存服务模块提供统一的缓存操作接口，支持多种缓存实现和数据结构，为插件开发者提供高效、灵活的缓存能力。

## 2. 核心特性

- **统一接口**：CacheService接口定义了所有缓存操作方法
- **多种实现**：支持文件缓存(FILE)、Redis缓存(REDIS)和Caffeine本地缓存(CAFFEINE)
- **丰富数据结构**：支持字符串、Hash、Set和List等多种数据结构
- **过期时间管理**：灵活设置缓存过期时间，支持永久缓存
- **自动序列化**：自动处理对象的序列化和反序列化

## 3. 缓存类型

| 缓存类型 | 特点 | 适用场景 |
| -------- | ---- | -------- |
| FILE | 基于文件系统的持久化缓存 | 单机环境、需要持久化的场景 |
| REDIS | 基于Redis的分布式缓存 | 分布式环境、高性能场景 |
| CAFFEINE | 基于Caffeine的高性能本地内存缓存 | 对性能要求极高的场景 |

## 4. 配置说明

在`application.yml`或`application-dev.yml`中配置缓存服务：

```yaml
# 缓存配置
cache:
  # 缓存类型：FILE, REDIS, CAFFEINE
  type: FILE
  # 是否启用
  enabled: true
  # 全局过期时间（秒），默认24小时，-1表示永不过期
  default-expiration: 86400
  # 缓存键前缀
  key-prefix: "qteamos:dev:"
  
  # 文件缓存配置
  file:
    # 缓存目录
    directory: "${java.io.tmpdir}/qteamos-cache/dev"
    # 是否使用序列化存储
    serialized: true
    # 缓存清理周期（秒）
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
    # 初始容量
    initial-capacity: 100
    # 最大容量
    maximum-size: 5000
    # 是否记录统计信息
    record-stats: true
```

## 5. 使用示例

### 5.1 基本操作

```java
@Autowired
private CacheService cacheService;

// 设置缓存
cacheService.set("user:id", user);

// 设置带过期时间的缓存
cacheService.set("token:123", token, 30, TimeUnit.MINUTES);

// 获取缓存
User user = cacheService.get("user:id", User.class);

// 删除缓存
cacheService.delete("user:id");

// 判断缓存是否存在
boolean exists = cacheService.exists("user:id");

// 设置过期时间
cacheService.expire("user:id", 1, TimeUnit.HOURS);
```

### 5.2 Hash结构操作

```java
// 设置Hash值
cacheService.setHashValue("user:1", "name", "张三");
cacheService.setHashValue("user:1", "age", 30);

// 获取Hash值
String name = cacheService.getHashValue("user:1", "name", String.class);

// 获取整个Hash
Map<String, Object> userMap = cacheService.getEntireHash("user:1");

// 检查Hash键是否存在
boolean hasEmail = cacheService.existsHashKey("user:1", "email");

// 删除Hash字段
cacheService.deleteHashValue("user:1", "email");
```

### 5.3 Set结构操作

```java
// 添加到Set
cacheService.addToSet("tags", "Java", "Spring", "Redis");

// 获取Set成员
Set<String> tags = cacheService.getSet("tags", String.class);
```

### 5.4 List结构操作

```java
// 添加到List
cacheService.leftPush("logs", "日志1");
cacheService.rightPush("logs", "日志2");

// 获取List
List<String> logs = cacheService.getList("logs", 0, -1, String.class);

// 获取List长度
long size = cacheService.getListSize("logs");
```

### 5.5 计数器操作

```java
// 递增
long count1 = cacheService.increment("counter", 1);
long count2 = cacheService.increment("counter", 5);

// 递减
long count3 = cacheService.decrement("counter", 2);
```

## 6. 最佳实践

- 合理设计缓存键，推荐使用冒号分隔的命名方式，如"module:entity:id"
- 根据数据特性选择合适的缓存类型
- 为重要缓存设置合理的过期时间，避免内存泄漏
- 对大对象或集合使用压缩算法降低内存占用
- 在高并发场景下，考虑使用分布式锁防止缓存击穿 