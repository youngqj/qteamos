# QTeamOS 数据库服务模块使用手册

## 1. 模块概述

数据库服务模块是QTeamOS的核心组件，提供了插件化的数据库访问系统，支持多种数据库类型和动态数据源切换。通过统一的接口和简洁的注解，插件开发者可以轻松实现数据库操作和多数据源管理。

## 2. 核心特性

- **动态数据源**：基于Spring的AbstractRoutingDataSource实现数据源动态切换
- **多种数据库支持**：兼容MySQL、MongoDB等多种数据库
- **注解式数据源切换**：通过@DataSource注解轻松实现数据源切换
- **MyBatis Plus集成**：提供完整的MybatisPlusService服务，简化数据库操作
- **事务支持**：支持跨数据源的事务操作

## 3. 核心组件

| 组件名称 | 功能描述 |
| -------- | -------- |
| DataSourceProperties | 数据源配置属性类，用于加载配置文件中的数据源配置 |
| DynamicDataSource | 继承AbstractRoutingDataSource，实现数据源动态路由 |
| DataSourceContextHolder | 基于ThreadLocal实现线程隔离的数据源上下文 |
| DataSource注解 | 标记方法或类使用的数据源 |
| DataSourceAspect | 处理@DataSource注解，自动切换数据源 |
| MybatisPlusService | 提供获取不同数据源Mapper的能力，支持复杂操作 |

## 4. 配置说明

### 4.1 MySQL数据源配置

在`application.yml`或`application-dev.yml`中配置MySQL数据源：

```yaml
spring:
  datasource:
    primary-name: systemDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/qteamosDB?useUnicode=true&characterEncoding=utf8
    username: root
    password: 123456
    # Druid连接池配置
    druid:
      initial-size: 5
      min-idle: 5
      max-active: 20
      
  # 多数据源配置
  datasource-multi:
    # 启用多数据源
    enabled: false
    # 业务数据库
    business:
      url: jdbc:mysql://localhost:3306/qelebase_business
      username: xqadmin
      password: Xiaoqu@123
      driver-class-name: com.mysql.cj.jdbc.Driver
    # 日志数据库
    log:
      url: jdbc:mysql://localhost:3306/qelebase_log
      username: xqadmin
      password: Xiaoqu@123
      driver-class-name: com.mysql.cj.jdbc.Driver
```

### 4.2 MongoDB数据源配置

在`application.yml`或`application-dev.yml`中配置MongoDB数据源：

```yaml
spring:
  data:
    mongodb:
      # 主MongoDB数据源配置
      primary:
        uri: mongodb://localhost:27017/primary
        # 或使用详细配置
        # host: localhost
        # port: 27017
        # database: primary
        # username: admin
        # password: password
        # authentication-database: admin
      
      # 次要MongoDB数据源配置
      secondary:
        uri: mongodb://localhost:27017/secondary
      
      # 其他MongoDB配置选项
      # client:
      #   connection-pool:
      #     max-size: 100
      #     min-size: 5
      #     max-wait-time: 15000
```

## 5. 使用示例

### 5.1 使用注解切换数据源

```java
// 在方法上使用
@DataSource("businessDB")
public List<User> getBusinessUsers() {
    return userMapper.selectList(null);
}

// 在类上使用
@Service
@DataSource("logDB")
public class LogService {
    // 该类中的所有方法默认使用logDB数据源
}
```

### 5.2 使用MyBatis Plus服务

```java
@Autowired
private MybatisPlusService mybatisPlusService;

// 获取默认数据源的Mapper
UserMapper mapper = mybatisPlusService.getMapper(UserMapper.class);

// 获取指定数据源的Mapper
UserMapper businessMapper = mybatisPlusService.getMapper(UserMapper.class, "businessDB");

// 创建查询条件
QueryWrapper<User> queryWrapper = mybatisPlusService.createQueryWrapper();
queryWrapper.eq("status", 1);

// 执行查询
List<User> users = mybatisPlusService.selectList("businessDB", User.class, queryWrapper);

// 执行事务
mybatisPlusService.executeTransaction("businessDB", sqlSession -> {
    // 在事务中执行操作
    UserMapper mapper = sqlSession.getMapper(UserMapper.class);
    return mapper.insert(user);
});
```

### 5.3 原生数据库访问方式

除了MyBatis Plus之外，QTeamOS还支持多种原生数据库访问方式，您可以根据需要选择合适的方式：

#### 5.3.1 使用JDBC原生API

```java
@Autowired
private DataSourceManager dataSourceManager;

@DataSource("businessDB")  // 自动切换数据源
public void executeWithJdbc() {
    // 获取数据源
    DataSource dataSource = dataSourceManager.getDataSource("businessDB");
    
    try (Connection conn = dataSource.getConnection();
         PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE status = ?")) {
        ps.setInt(1, 1);
        
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                // 处理结果集
                String name = rs.getString("name");
                int age = rs.getInt("age");
                // ...
            }
        }
    } catch (SQLException e) {
        log.error("JDBC执行异常", e);
        throw new DatabaseException("JDBC执行异常: " + e.getMessage(), e);
    }
}
```

#### 5.3.2 使用Spring JdbcTemplate

```java
@Autowired
private JdbcTemplate jdbcTemplate;

@Autowired
private DataSourceManager dataSourceManager;

// 使用默认数据源的JdbcTemplate
public List<User> findActiveUsers() {
    return jdbcTemplate.query(
        "SELECT * FROM users WHERE status = ?",
        new Object[]{1},
        (rs, rowNum) -> {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setName(rs.getString("name"));
            user.setAge(rs.getInt("age"));
            return user;
        }
    );
}

// 使用指定数据源的JdbcTemplate
public List<User> findUsersFromBusinessDB() {
    // 获取业务数据源
    DataSource businessDataSource = dataSourceManager.getDataSource("businessDB");
    JdbcTemplate businessJdbcTemplate = new JdbcTemplate(businessDataSource);
    
    return businessJdbcTemplate.query(
        "SELECT * FROM business_users WHERE department = ?",
        new Object[]{"IT"},
        (rs, rowNum) -> {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setName(rs.getString("name"));
            user.setDepartment(rs.getString("department"));
            return user;
        }
    );
}
```

#### 5.3.3 使用JPA/Hibernate

```java
// 注入EntityManager
@PersistenceContext
private EntityManager entityManager;

// 使用JPA查询
@DataSource("primaryDataSource")  // 切换数据源
public List<User> findUsersWithJpa(String name) {
    return entityManager.createQuery(
        "SELECT u FROM User u WHERE u.name LIKE :name", User.class)
        .setParameter("name", "%" + name + "%")
        .getResultList();
}

// 使用JPA保存实体
@DataSource("businessDB")
@Transactional
public User saveUser(User user) {
    if (user.getId() == null) {
        entityManager.persist(user);
    } else {
        user = entityManager.merge(user);
    }
    return user;
}
```

### 5.4 使用MongoDB

```java
// 注入MongoDB模板
@Autowired
@Qualifier("primaryMongoTemplate")
private MongoTemplate primaryMongoTemplate;

@Autowired
@Qualifier("secondaryMongoTemplate")
private MongoTemplate secondaryMongoTemplate;

// 基本查询操作
public User findUserById(String id) {
    return primaryMongoTemplate.findById(id, User.class);
}

// 条件查询
public List<User> findActiveUsers() {
    Query query = new Query(Criteria.where("status").is("active"));
    return primaryMongoTemplate.find(query, User.class);
}

// 插入文档
public User saveUser(User user) {
    return primaryMongoTemplate.save(user);
}

// 更新文档
public void updateUserAge(String id, int newAge) {
    Query query = new Query(Criteria.where("id").is(id));
    Update update = new Update().set("age", newAge);
    primaryMongoTemplate.updateFirst(query, update, User.class);
}

// 删除文档
public void deleteUser(String id) {
    Query query = new Query(Criteria.where("id").is(id));
    primaryMongoTemplate.remove(query, User.class);
}

// 分页查询
public Page<User> findUsersByPage(int page, int size) {
    Query query = new Query().with(PageRequest.of(page, size));
    long total = primaryMongoTemplate.count(query, User.class);
    List<User> users = primaryMongoTemplate.find(query, User.class);
    return new PageImpl<>(users, PageRequest.of(page, size), total);
}
```

## 6. 最佳实践

- 合理规划数据源，避免过多数据源导致资源浪费
- 使用@DataSource注解而非手动设置数据源上下文
- 对于频繁使用的Mapper，优先注入而非每次通过MybatisPlusService获取
- 在事务方法中避免切换数据源
- 定期检查连接池状态，合理设置连接池参数
- 避免在缓存中存储敏感信息，必要时进行加密处理
- 对用户输入的查询条件进行严格验证，防止SQL注入和NoSQL注入
- 使用参数化查询而非字符串拼接SQL
- 定期更新数据库和缓存服务的密码
- 生产环境启用安全相关配置，如Druid的wall防火墙 