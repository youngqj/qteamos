# HelloWorld 插件示例

这是一个基于QTeamOS新版SDK的HelloWorld插件示例，展示了基本的插件开发流程和接口使用方法。

## 插件功能

该插件实现了以下功能：

1. 插件的生命周期管理（初始化、启动、停止、卸载）
2. 配置读取和使用
3. 数据库初始化和访问

## 使用方法

### 1. 构建插件

```bash
mvn clean package
```

### 2. 部署插件

将生成的JAR文件复制到QTeamOS的`plugins`目录中。

### 3. 验证插件

登录QTeamOS系统后台，在插件管理中可以看到HelloWorld插件，可以进行启动、停止等操作。

## 技术说明

### 插件结构

- `HelloWorldPlugin.java`: 插件主类，继承自`AbstractPlugin`
- `plugin.yml`: 插件配置文件，定义了插件的基本信息和配置项
- `V1__init_helloworld_tables.sql`: 数据库初始化脚本

### 关键API使用

1. 配置服务：
```java
String greeting = getContext().getConfigService().getString("greeting", "默认值");
```

2. 数据库服务：
```java
getContext().getDataSourceService().executeSql(getId(), "db/migration/V1__init_helloworld_tables.sql");
```

## 开发注意事项

1. 插件ID必须唯一
2. 配置文件必须命名为`plugin.yml`并放在resources目录下
3. 所有数据库访问都应通过SDK提供的服务接口进行
4. 在stop和uninstall方法中需要释放资源

## 调试方法

1. 启用调试模式：在`plugin.yml`中设置`enableDebug: true`
2. 查看日志：插件的日志会输出到QTeamOS的日志目录

## 更多信息

参考 [QTeamOS SDK文档](https://docs.qteamos.com/sdk) 