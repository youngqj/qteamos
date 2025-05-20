/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-07 18:13:31
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-04-08 10:43:15
 * @FilePath: /qelebase/Users/yangqijun/dev/QEleBase/qelebase-common/src/main/java/com/xiaoqu/qelebase/common/utils/CryptoUtils.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.common.utils;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 加密解密工具类
 */
@Component
public class CryptoUtils {

    @Value("${spring.security.encryption.key}")
    private String secretKey;

    private AES aes;

    @PostConstruct
    public void init() {
        // 生成密钥
        byte[] key = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue(), secretKey.getBytes(StandardCharsets.UTF_8)).getEncoded();
        aes = SecureUtil.aes(key);
    }

    /**
     * AES加密
     *
     * @param content 待加密内容
     * @return 加密后的内容（Base64编码）
     */
    public String encrypt(String content) {
        return aes.encryptBase64(content);
    }

    /**
     * AES解密
     *
     * @param encrypted 已加密内容（Base64编码）
     * @return 解密后的内容
     */
    public String decrypt(String encrypted) {
        return aes.decryptStr(encrypted);
    }

    /**
     * MD5加密
     *
     * @param content 待加密内容
     * @return MD5加密后的内容
     */
    public static String md5(String content) {
        return SecureUtil.md5(content);
    }

    /**
     * SHA256加密
     *
     * @param content 待加密内容
     * @return SHA256加密后的内容
     */
    public static String sha256(String content) {
        return SecureUtil.sha256(content);
    }

    /**
     * 生成文件的MD5值
     *
     * @param filePath 文件路径
     * @return 文件的MD5值
     */
    public static String fileMd5(String filePath) {
        return SecureUtil.md5(filePath);
    }
} 