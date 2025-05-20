package com.xiaoqu.qteamos.common.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

public class AESUtil {
    private static final String ALGORITHM = "AES/ECB/PKCS5Padding";
    private static final String KEY_ALGORITHM = "AES";

    /**
     * 加密
     *
     * @param content 需要加密的内容
     * @param key     加密密钥（Base64编码）
     * @return 加密后的内容（Base64编码）
     */
    public static String encrypt(String content, String key) {
        try {
            if (content == null || content.isEmpty()) {
                return content;
            }
            // 获取真实密钥
            byte[] realKey = getRealKey(key);
            SecretKeySpec secretKey = new SecretKeySpec(realKey, KEY_ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("AES加密失败", e);
        }
    }

    /**
     * 解密
     *
     * @param encrypted 已加密的内容（Base64编码）
     * @param key       解密密钥（Base64编码）
     * @return 解密后的内容
     */
    public static String decrypt(String encrypted, String key) {
        try {
            if (encrypted == null || encrypted.isEmpty()) {
                return encrypted;
            }
            // 获取真实密钥
            byte[] realKey = getRealKey(key);
            SecretKeySpec secretKey = new SecretKeySpec(realKey, KEY_ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] original = cipher.doFinal(Base64.getDecoder().decode(encrypted));
            return new String(original, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES解密失败", e);
        }
    }

    /**
     * 获取真实的AES密钥
     * AES密钥长度必须是16、24或32字节
     *
     * @param key Base64编码的密钥
     * @return 真实的AES密钥
     */
    private static byte[] getRealKey(String key) throws Exception {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("密钥不能为空");
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(key);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            keyBytes = sha.digest(keyBytes);
            return Arrays.copyOf(keyBytes, 32); // 使用32字节（256位）密钥
        } catch (Exception e) {
            throw new IllegalArgumentException("无效的密钥格式", e);
        }
    }
} 