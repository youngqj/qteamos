/*
 * Copyright (c) 2023-2025 XiaoQuTeam. All rights reserved.
 * QTeamOS is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package com.xiaoqu.qteamos.common.utils;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Base64;
import java.util.Arrays;

/**
 * 加密工具类
 * 提供AES加密/解密功能
 *
 * @author yangqijun
 * @date 2025-05-19
 * @since 1.0.0
 */
@Component
public class EncryptionUtils {
    private static final Logger log = LoggerFactory.getLogger(EncryptionUtils.class);
    
    /**
     * 默认密钥 - 仅用于开发环境
     * 生产环境应通过配置文件或环境变量覆盖
     */
    private static final String DEFAULT_KEY = "OURNS0ngofJzQJqIbfwUJPHLa6MVvEuvUI7hbs+UUJpGno+8BzbT42mMPdI0Bs9C";
    
    /**
     * 从配置文件中读取加密密钥
     * 如未配置则使用默认密钥（不推荐用于生产环境）
     */
    @Value("${qteamos.security.encryption.key:#{null}}")
    private String configuredKey;
    
    /**
     * AES密钥长度 (256/192/128)
     */
    @Value("${qteamos.security.encryption.key-length:256}")
    private int keyLength;
    
    // 添加静态引用持有实例
    private static EncryptionUtils instance;
    
    // AES实例使用volatile确保线程可见性
    private static volatile AES aesInstance;
    
    // 静态初始化锁
    private static final Object initLock = new Object();
    
    /**
     * 初始化加密组件
     */
    @PostConstruct
    public void init() {
        // 保存实例引用
        instance = this;
        
        // 实例初始化时尝试初始化AES
        initializeAes();
    }
    
    /**
     * 初始化AES加密实例
     */
    private void initializeAes() {
        if (aesInstance != null) {
            return; // 已初始化
        }
        
        synchronized (initLock) {
            if (aesInstance != null) {
                return; // 双重检查锁定
            }
            
            String keyToUse = configuredKey != null ? configuredKey : DEFAULT_KEY;
            
            try {
                // 验证密钥长度
                if (keyLength != 128 && keyLength != 192 && keyLength != 256) {
                    log.warn("无效的AES密钥长度配置: {}，将使用默认值256", keyLength);
                    keyLength = 256;
                }
                
                // 生成指定长度的密钥
                byte[] keyBytes = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue(), 
                                                      keyToUse.getBytes())
                                          .getEncoded();
                
                // 截取到指定长度(字节)
                keyBytes = Arrays.copyOf(keyBytes, keyLength / 8);
                
                // 创建AES实例
                aesInstance = new AES(keyBytes);
                
                if (configuredKey == null) {
                    log.warn("使用默认加密密钥 - 不建议在生产环境中使用");
                } else {
                    log.info("已初始化AES加密工具，密钥长度: {}位", keyLength);
                }
            } catch (Exception e) {
                log.error("初始化加密工具失败: {}", e.getMessage(), e);
                throw new RuntimeException("加密工具初始化失败", e);
            }
        }
    }
    
    /**
     * 确保AES实例初始化
     */
    private static void ensureInitialized() {
        if (aesInstance == null) {
            if (instance != null) {
                // 如果Spring已创建实例，使用它初始化
                instance.initializeAes();
            } else {
                // 如果Spring尚未初始化，使用默认配置
                synchronized (initLock) {
                    if (aesInstance == null) {
                        log.warn("在Spring容器外部进行加密组件初始化");
                        
                        try {
                            byte[] keyBytes = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue(), 
                                                                  DEFAULT_KEY.getBytes())
                                                      .getEncoded();
                            keyBytes = Arrays.copyOf(keyBytes, 256 / 8); // 默认使用256位
                            aesInstance = new AES(keyBytes);
                            log.info("使用默认配置初始化AES加密工具");
                        } catch (Exception e) {
                            log.error("默认初始化加密工具失败: {}", e.getMessage(), e);
                            throw new RuntimeException("加密工具初始化失败", e);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 加密字符串
     *
     * @param content 要加密的内容
     * @return 加密后的Base64字符串
     */
    public static String encrypt(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        try {
            ensureInitialized();
            return aesInstance.encryptBase64(content);
        } catch (Exception e) {
            log.error("加密失败: {}", e.getMessage(), e);
            return content;  // 失败时返回原内容
        }
    }
    
    /**
     * 解密字符串
     *
     * @param encryptedContent 已加密的Base64字符串
     * @return 解密后的原始内容
     */
    public static String decrypt(String encryptedContent) {
        if (encryptedContent == null || encryptedContent.isEmpty()) {
            return encryptedContent;
        }
        
        try {
            ensureInitialized();
            return aesInstance.decryptStr(encryptedContent);
        } catch (Exception e) {
            log.error("解密失败: {}", e.getMessage(), e);
            return encryptedContent;  // 失败时返回原内容
        }
    }
} 