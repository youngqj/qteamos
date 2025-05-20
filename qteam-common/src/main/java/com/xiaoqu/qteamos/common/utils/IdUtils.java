/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-07 18:12:53
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-04-07 18:16:12
 * @FilePath: /qelebase/qelebase-common/src/main/java/com/xiaoqu/qelebase/common/utils/IdUtils.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.common.utils;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ID生成工具类
 */
@Component
public class IdUtils {
    
    @Value("${snowflake.worker-id:1}")
    private long workerId;
    
    @Value("${snowflake.datacenter-id:1}")
    private long datacenterId;
    
    private Snowflake snowflake;
    
    @PostConstruct
    public void init() {
        snowflake = IdUtil.getSnowflake(workerId, datacenterId);
    }
    
    /**
     * 生成雪花算法ID
     */
    public long nextId() {
        return snowflake.nextId();
    }
    
    /**
     * 生成雪花算法ID（字符串形式）
     */
    public String nextIdStr() {
        return snowflake.nextIdStr();
    }
    
    /**
     * 生成UUID（不含横线）
     */
    public static String fastUUID() {
        return IdUtil.fastSimpleUUID();
    }
    
    /**
     * 生成UUID（含横线）
     */
    public static String uuid() {
        return IdUtil.randomUUID();
    }
} 

/**
 * 使用示例：
 * // ID生成
@Autowired
private IdUtils idUtils;

long id = idUtils.nextId();  // 生成雪花算法ID
String uuid = IdUtils.fastUUID();  // 生成UUID

// 加密解密
@Autowired
private CryptoUtils cryptoUtils;

String encrypted = cryptoUtils.encrypt("敏感数据");  // AES加密
String decrypted = cryptoUtils.decrypt(encrypted);   // AES解密
String md5 = CryptoUtils.md5("文本");               // MD5加密

// 数据脱敏
String maskedPhone = SensitiveUtils.maskPhone("13812345678");        // 138****5678
String maskedEmail = SensitiveUtils.maskEmail("test@example.com");   // t***@example.com
String maskedName = SensitiveUtils.maskChineseName("张三");          // 张*

// 文件操作
String md5 = FileUtils.calculateMD5("plugin.jar");                   // 计算文件MD5值
boolean isValid = FileUtils.verifyMD5("plugin.jar", "expected-md5"); // 验证文件MD5值
String size = FileUtils.getFormattedFileSize(new File("plugin.jar")); // 获取格式化的文件大小
*/