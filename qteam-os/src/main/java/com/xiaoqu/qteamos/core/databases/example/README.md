# 数据库服务示例

本目录包含展示如何使用QTeamOS数据库服务模块的示例代码。

## 包含示例

- **MybatisPlusExampleService**: 展示如何使用MyBatis Plus进行数据库操作
  - 原生方式：直接注入Mapper接口
  - 服务方式：通过MybatisPlusService获取Mapper
  - 多数据源示例：演示在不同数据源间切换的方法

- **MongoExampleService**: 展示如何使用MongoDB进行数据库操作
  - 基本查询：使用Criteria构建查询条件
  - 高级查询：复杂条件查询和分页
  - 增删改操作：插入、更新和删除文档
  - 多数据源：使用不同MongoDB数据源
  - Repository模式：通过动态代理获取Repository接口实现

## 使用方式

1. **引入依赖**
   确保已经添加了MyBatis Plus和MongoDB相关依赖到pom.xml中。

2. **配置数据源**
   在application.yml中配置数据源信息：

   ```yaml
   spring:
     datasource:
       primary-name: systemDataSource
       dynamic:
         primary: systemDataSource
         datasource:
           systemDataSource:
             driver-class-name: com.mysql.cj.jdbc.Driver
             url: jdbc:mysql://localhost:3306/qteamos?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8
             username: root
             password: password
           businessDB:
             driver-class-name: com.mysql.cj.jdbc.Driver
             url: jdbc:mysql://localhost:3306/business?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=GMT%2B8
             username: root
             password: password
     data:
       mongodb:
         primary:
           uri: mongodb://localhost:27017/primary
         secondary:
           uri: mongodb://localhost:27017/secondary
   ```

3. **运行示例**
   可以注入示例服务并调用runXxxExamples方法运行示例：

   ```java
   @Autowired
   private MybatisPlusExampleService mybatisPlusExampleService;
   
   @Autowired
   private MongoExampleService mongoExampleService;
   
   public void runExamples() {
      mybatisPlusExampleService.runMybatisPlusExamples();
      mongoExampleService.runMongoExamples();
   }
   ```

## 注意事项

- 示例代码中的实体类和Mapper接口仅供参考，实际使用时需要替换为真实的业务实体和Mapper。
- 多数据源功能需要确保配置了相应的数据源信息。
- MongoDB示例中的Repository方法实现是动态代理的简化实现，实际使用时可能需要更复杂的处理逻辑。 

## 详细文档

更多详细说明和使用方法，请参考[数据库服务模块使用手册](../../../../../docs/database/database-service-guide.md)。 