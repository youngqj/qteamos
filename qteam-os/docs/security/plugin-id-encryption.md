# 插件ID加密功能

## 1. 功能概述

插件ID加密功能通过对URL中的插件ID部分进行加密，增强了系统的安全性。该功能可以防止未授权用户通过分析URL推测出插件的命名和结构，从而保护系统免受可能的探测和攻击。

## 2. 实现原理

系统使用AES加密算法对URL中的插件ID部分进行加密处理。整个流程如下：

1. **生成URL时**：系统将明文的插件ID通过AES算法加密，并进行URL安全处理
2. **处理请求时**：系统从URL中提取加密的插件ID，解密后恢复原始插件ID，然后继续正常处理

使用的核心工具类：`com.xiaoqu.qteamos.common.utils.EncryptionUtils`

## 3. 配置选项

在`application.yml`中可以配置插件ID加密的相关选项：

```yaml
qteamos:
  gateway:
    # 是否启用插件ID加密，默认为true
    encrypt-plugin-id: true
```

可以在不同环境配置文件中设置不同的值，例如在开发环境中禁用加密以便于调试：

```yaml
# application-dev.yml
qteamos:
  gateway:
    encrypt-plugin-id: false
```

## 4. 加密处理流程

插件ID的加密处理流程如下：

1. 使用`EncryptionUtils.encrypt()`方法加密插件ID
2. 对加密结果进行URL安全处理（替换特殊字符如`=`、`/`、`+`等）
3. 加密结果缓存在内存中，提高性能

插件ID的解密处理流程如下：

1. 从URL中提取加密的插件ID
2. 还原URL安全字符处理
3. 使用`EncryptionUtils.decrypt()`方法解密
4. 解密结果也会缓存，提高性能

## 5. 涉及的类和方法

核心实现类：

- **PluginRequestMappingHandlerMapping**
  - `encryptPluginId(String pluginId)` - 加密插件ID
  - `decryptPluginId(String encryptedId)` - 解密插件ID
  - `createMappingInfo()` - 构建URL时使用加密的插件ID

- **PluginControllerDelegator**
  - `extractPluginId(String uri)` - 从URL中提取并解密插件ID

- **PluginRequestFilter**
  - 更新了URI匹配逻辑，支持处理加密的URL

## 6. 开发调试建议

在开发环境中，建议暂时关闭插件ID加密功能，便于调试和问题定位：

1. 在`application-dev.yml`中设置`qteamos.gateway.encrypt-plugin-id: false`
2. 观察日志中的`插件[xx]基础路径`信息，可以看到加密前后的对比
3. 如果需要测试加密功能，可以临时将其设置为`true`

## 7. 安全性考虑

此功能提供了基本的URL混淆和保护，但不应作为唯一的安全措施。建议结合以下措施提供完整的安全保障：

1. 正确配置认证和授权机制
2. 实施适当的API访问控制
3. 使用HTTPS协议传输数据
4. 定期审查日志和异常访问模式

## 8. 未来改进方向

1. 增加加密密钥轮换机制
2. 加强对解密失败情况的处理
3. 添加更多统计和监控指标
4. 实现自定义加密算法的可插拔支持

## 9. 负责人

- 设计实现：yangqijun
- 文档日期：2025-05-19 