# QTeamOS插件控制器路径映射规则

> 最后更新日期: 2025-05-19

## 1. 控制器类型与路径前缀

QTeamOS框架针对不同类型的控制器使用不同的URL路径前缀：

| 控制器类型 | 注解 | 默认路径前缀 | 配置项 |
|---------|-----|------------|-------|
| REST API控制器 | `@RestController` | `/api` | `qteamos.gateway.api-prefix` |
| 视图控制器 | `@Controller` | `/html` | `qteamos.gateway.html-path-prefix` |

## 2. 路径结构

插件控制器的完整URL路径结构：

```
/{prefix}/p-{pluginId}/{controllerPath}/{methodPath}
```

- `{prefix}`: 根据控制器类型选择的前缀（/api或/html）
- `{pluginId}`: 插件标识符（可能被加密）
- `{controllerPath}`: 控制器类上@RequestMapping定义的路径
- `{methodPath}`: 方法上@RequestMapping或其变体定义的路径

## 3. 配置项

在application.yml中配置：

```yaml
qteamos:
  gateway:
    # REST API请求前缀，默认为/api
    api-prefix: /api
    # HTML视图路径前缀，默认为/html
    html-path-prefix: /html
    # 是否启用插件ID加密，默认为true
    encrypt-plugin-id: true
```

## 4. 控制器示例

### REST API控制器

```java
@RestController
@RequestMapping("/users")
public class UserApiController {
    @GetMapping("/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        // 实现...
        return Result.success(user);
    }
}
```

访问路径：`/api/p-myplugin/users/123`

### 视图控制器

```java
@Controller
@RequestMapping("/view")
public class UserViewController {
    @GetMapping("/profile")
    public String showProfile(Model model) {
        // 实现...
        return "profile";
    }
    
    @GetMapping(value = "/hello", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String helloHtml() {
        // 直接返回HTML字符串
        return "<html><body><h1>Hello World</h1></body></html>";
    }
}
```

访问路径：
- `/html/p-myplugin/view/profile`
- `/html/p-myplugin/view/hello`

## 5. 安全配置

确保在安全配置中允许HTML路径访问：

```yaml
plugin:
  security:
    paths:
      permit-all:
        - "/assets/**"
        - "${qteamos.gateway.api-prefix}/p-*/pub/**"
        - "${qteamos.gateway.html-path-prefix}/p-*/**"  # 允许HTML路径
```

## 6. 实现原理

QTeamOS使用Spring的路径映射机制，在`PluginRequestMappingHandlerMapping`中根据控制器类型选择不同的路径前缀：

```java
// 判断是RestController还是普通Controller
boolean isRestController = AnnotationUtils.findAnnotation(controllerClass, RestController.class) != null;

// 根据控制器类型选择前缀
String prefix = isRestController ? apiPrefix : htmlPrefix;
```

插件请求过滤器`PluginRequestFilter`通过正则表达式匹配这两种前缀模式，并将请求委托给`PluginControllerDelegator`处理。

## 7. 路径处理注意事项

1. **带斜杠和不带斜杠的路径定义都支持**：
   ```java
   @RequestMapping("/users")  // 带斜杠
   @RequestMapping("users")   // 不带斜杠
   ```

2. **支持多级路径**：
   ```java
   @GetMapping("/admin/reports/monthly")  // 正确解析为多级路径
   ```

3. **路径组合规则**：
   - 插件基础路径：`/api/p-[pluginId]` 或 `/html/p-[pluginId]`
   - 类路径：来自控制器上的`@RequestMapping`值
   - 方法路径：来自方法上的`@GetMapping`等注解值
   - 最终URL = 基础路径 + 类路径 + 方法路径

4. **支持所有Spring MVC HTTP方法注解**：
   - `@GetMapping`
   - `@PostMapping`
   - `@PutMapping`
   - `@DeleteMapping`
   - `@PatchMapping`
   - `@RequestMapping`

## 8. 插件ID加密与URL安全性

为提高安全性，插件ID在URL中默认进行加密处理：

```java
// 配置选项控制是否启用加密
@Value("${qteamos.gateway.encrypt-plugin-id:true}")
private boolean isEncryptPluginId;
```

- 启用加密后，URL中的插件ID将被加密字符串替代
- 例如：`/api/p-example-plugin/users` → `/api/p-nyYUn3zVbOLkARzs3Dm734KdvYsldha_-o3SrW_AXDU/users`
- 可通过配置禁用加密：`qteamos.gateway.encrypt-plugin-id=false`

## 9. 最佳实践

1. **明确区分控制器类型**
   - 使用`@RestController`处理API请求，返回JSON数据
   - 使用`@Controller`处理视图请求，返回HTML内容

2. **合理组织控制器**
   - API控制器放在`.api`、`.controller`、`.rest`包下
   - 视图控制器放在`.view`、`.web`、`.mvc`包下

3. **设置正确的Content-Type**
   - 视图控制器方法设置`produces = MediaType.TEXT_HTML_VALUE`
   - API控制器方法设置`produces = MediaType.APPLICATION_JSON_VALUE`

4. **使用适当的返回类型**
   - 视图控制器返回`String`（视图名）或使用`@ResponseBody`返回HTML
   - API控制器返回领域对象或`Result<T>`包装类

5. **路径命名规范**
   - REST API使用资源名词复数形式，如`/users`、`/orders`
   - 视图路径使用描述性名称，如`/view`、`/page`、`/dashboard`

6. **插件描述文件中声明控制器**
   ```yaml
   metadata:
     controllers:
       - com.xiaoqu.qteamos.plugin.example.controller.ExampleApiController
       - com.xiaoqu.qteamos.plugin.example.controller.ExampleViewController
   ``` 