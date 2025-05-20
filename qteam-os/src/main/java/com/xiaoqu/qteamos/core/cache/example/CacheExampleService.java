package com.xiaoqu.qteamos.core.cache.example;

import com.xiaoqu.qteamos.core.cache.api.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 缓存示例服务
 * 演示缓存服务的使用方法
 *
 * @author yangqijun
 * @date 2025-05-04
 */
@Slf4j
@Component
public class CacheExampleService {

    @Autowired
    private CacheService cacheService;

    /**
     * 运行缓存示例
     */
    public void runCacheExamples() {
        log.info("=== 运行缓存示例 ===");
        log.info("当前使用的缓存类型: {}", cacheService.getType());

        // 基础缓存操作示例
        basicCacheOperations();

        // 过期时间操作示例
        expirationOperations();

        // 计数器操作示例
        counterOperations();

        // Hash结构操作示例
        hashOperations();

        // Set结构操作示例
        setOperations();

        // List结构操作示例
        listOperations();

        log.info("=== 缓存示例完成 ===");
    }

    /**
     * 基础缓存操作示例
     */
    private void basicCacheOperations() {
        log.info("--- 基础缓存操作示例 ---");

        // 设置字符串
        cacheService.set("string:key", "Hello Cache!");
        log.info("设置字符串缓存");

        // 获取字符串
        String value = cacheService.get("string:key", String.class);
        log.info("获取字符串缓存: {}", value);

        // 设置数值
        cacheService.set("number:key", 42);
        log.info("设置数值缓存");

        // 获取数值
        Integer number = cacheService.get("number:key", Integer.class);
        log.info("获取数值缓存: {}", number);

        // 设置布尔值
        cacheService.set("bool:key", true);
        log.info("设置布尔缓存");

        // 获取布尔值
        Boolean bool = cacheService.get("bool:key", Boolean.class);
        log.info("获取布尔缓存: {}", bool);

        // 设置复杂对象
        User user = new User(1L, "张三", 25);
        cacheService.set("user:1", user);
        log.info("设置对象缓存");

        // 获取复杂对象
        User cachedUser = cacheService.get("user:1", User.class);
        log.info("获取对象缓存: {}", cachedUser);

        // 检查键是否存在
        boolean exists = cacheService.exists("user:1");
        log.info("检查键存在: {}", exists);

        // 删除键
        boolean deleted = cacheService.delete("bool:key");
        log.info("删除缓存: {}", deleted);

        // 批量删除
        long count = cacheService.deleteAll(Arrays.asList("string:key", "number:key"));
        log.info("批量删除缓存: {}", count);
    }

    /**
     * 过期时间操作示例
     */
    private void expirationOperations() {
        log.info("--- 过期时间操作示例 ---");

        // 设置带过期时间的缓存
        cacheService.set("expire:key", "将在5秒后过期", 5, TimeUnit.SECONDS);
        log.info("设置带过期时间的缓存");

        // 获取过期时间
        long expireTime = cacheService.getExpire("expire:key", TimeUnit.SECONDS);
        log.info("获取过期时间(秒): {}", expireTime);

        // 修改过期时间
        boolean setExpire = cacheService.expire("user:1", 1, TimeUnit.MINUTES);
        log.info("修改过期时间: {}", setExpire);

        // 将键设置为永不过期
        boolean setPersist = cacheService.expire("user:1", -1, TimeUnit.SECONDS);
        log.info("设置键永不过期: {}", setPersist);

        log.info("等待过期时间演示...");
        try {
            // 等待过期
            Thread.sleep(6000);

            // 检查过期键
            String expiredValue = cacheService.get("expire:key", String.class);
            log.info("获取已过期的缓存: {}", expiredValue); // 应该为null
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 计数器操作示例
     */
    private void counterOperations() {
        log.info("--- 计数器操作示例 ---");

        // 递增操作
        long incr1 = cacheService.increment("counter:key", 1);
        log.info("递增操作(初始): {}", incr1); // 应该为1

        long incr2 = cacheService.increment("counter:key", 5);
        log.info("递增操作(+5): {}", incr2); // 应该为6

        // 递减操作
        long decr = cacheService.decrement("counter:key", 2);
        log.info("递减操作(-2): {}", decr); // 应该为4
    }

    /**
     * Hash结构操作示例
     */
    private void hashOperations() {
        log.info("--- Hash结构操作示例 ---");

        // 设置Hash值
        cacheService.setHashValue("user:hash", "name", "李四");
        cacheService.setHashValue("user:hash", "age", 30);
        cacheService.setHashValue("user:hash", "email", "lisi@example.com");
        log.info("设置Hash值");

        // 获取Hash值
        String name = cacheService.getHashValue("user:hash", "name", String.class);
        Integer age = cacheService.getHashValue("user:hash", "age", Integer.class);
        log.info("获取Hash值: name={}, age={}", name, age);

        // 获取整个Hash
        Map<String, Object> userHash = cacheService.getEntireHash("user:hash");
        log.info("获取整个Hash: {}", userHash);

        // 检查Hash键是否存在
        boolean hasEmail = cacheService.existsHashKey("user:hash", "email");
        log.info("检查Hash键是否存在: {}", hasEmail);

        // 删除Hash值
        long hashDeleted = cacheService.deleteHashValue("user:hash", "email");
        log.info("删除Hash值: {}", hashDeleted);
    }

    /**
     * Set结构操作示例
     */
    private void setOperations() {
        log.info("--- Set结构操作示例 ---");

        // 添加到Set
        long added1 = cacheService.addToSet("tags:set", "Java", "Spring");
        log.info("添加到Set: {}", added1);

        long added2 = cacheService.addToSet("tags:set", "Redis", "MongoDB", "Java");
        log.info("添加到Set(含重复): {}", added2);

        // 获取Set
        Set<String> tags = cacheService.getSet("tags:set", String.class);
        log.info("获取Set: {}", tags);
    }

    /**
     * List结构操作示例
     */
    private void listOperations() {
        log.info("--- List结构操作示例 ---");

        // 左侧添加到List
        long leftSize = cacheService.leftPush("log:list", "日志1");
        cacheService.leftPush("log:list", "日志2");
        log.info("左侧添加到List: {}", leftSize);

        // 右侧添加到List
        long rightSize = cacheService.rightPush("log:list", "日志3");
        cacheService.rightPush("log:list", "日志4");
        log.info("右侧添加到List: {}", rightSize);

        // 获取List大小
        long listSize = cacheService.getListSize("log:list");
        log.info("获取List大小: {}", listSize);

        // 获取List范围
        List<String> logs = cacheService.getList("log:list", 0, -1, String.class);
        log.info("获取整个List: {}", logs);

        List<String> partLogs = cacheService.getList("log:list", 1, 2, String.class);
        log.info("获取部分List(1-2): {}", partLogs);
    }

    /**
     * 用户实体示例
     */
    public static class User {
        private Long id;
        private String name;
        private Integer age;

        public User() {
        }

        public User(Long id, String name, Integer age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        @Override
        public String toString() {
            return "User{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", age=" + age +
                    '}';
        }
    }
} 