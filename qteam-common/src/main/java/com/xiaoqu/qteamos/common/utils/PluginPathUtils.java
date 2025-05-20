package com.xiaoqu.qteamos.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 插件路径工具类
 * 提供插件路径处理的统一方法
 *
 * @author qelebase
 * @version 1.0.0
 * @copyright 浙江小趣信息技术有限公司
 */
public class PluginPathUtils {
    private static final Logger log = LoggerFactory.getLogger(PluginPathUtils.class);
    
    /**
     * 规范化路径，去除冗余的相对路径标记
     *
     * @param path 原始路径
     * @return 规范化后的路径，如果输入为null则返回null
     */
    public static String normalizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 使用nio的Path规范化路径
            Path normalizedPath = Paths.get(path).normalize();
            return normalizedPath.toString();
        } catch (Exception e) {
            log.warn("路径规范化失败: {}, 错误: {}", path, e.getMessage());
            // 简单替换处理作为后备方案
            return path.replace("/./", "/").replace("\\.\\", "\\");
        }
    }
    
    /**
     * 构建插件JAR文件路径
     *
     * @param installPath 插件安装目录
     * @param pluginCode 插件代码
     * @return 构建的JAR文件路径
     */
    public static String buildPluginJarPath(String installPath, String pluginCode) {
        if (installPath == null || pluginCode == null) {
            return null;
        }
        
        String normalizedPath = normalizePath(installPath);
        if (normalizedPath == null) {
            return null;
        }
        
        return normalizedPath + File.separator + pluginCode + ".jar";
    }
    
    /**
     * 构建插件配置文件路径
     *
     * @param installPath 插件安装目录
     * @return 配置文件路径
     */
    public static String buildPluginYmlPath(String installPath) {
        if (installPath == null) {
            return null;
        }
        
        String normalizedPath = normalizePath(installPath);
        if (normalizedPath == null) {
            return null;
        }
        
        return normalizedPath + File.separator + "plugin.yml";
    }
    
    /**
     * 检查路径是否存在且可访问
     *
     * @param path 待检查的路径
     * @return 路径是否有效
     */
    public static boolean isPathAccessible(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        
        try {
            File file = new File(path);
            return file.exists() && file.canRead();
        } catch (Exception e) {
            log.warn("路径访问检查失败: {}, 错误: {}", path, e.getMessage());
            return false;
        }
    }
    
    /**
     * 计算文件MD5值
     *
     * @param filePath 文件路径
     * @return MD5值，失败时返回null
     */
    public static String calculateFileMd5(String filePath) {
        if (!isPathAccessible(filePath)) {
            return null;
        }
        
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
            byte[] digest = md.digest(fileBytes);
            
            // 转换为16进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("计算文件MD5失败: {}, 错误: {}", filePath, e.getMessage());
            return null;
        }
    }
    
    /**
     * 验证文件的MD5值是否匹配
     *
     * @param filePath 文件路径
     * @param expectedMd5 期望的MD5值
     * @return 是否匹配
     */
    public static boolean verifyFileMd5(String filePath, String expectedMd5) {
        if (expectedMd5 == null || expectedMd5.trim().isEmpty()) {
            return true; // 如果没有期望的MD5，则不验证
        }
        
        String actualMd5 = calculateFileMd5(filePath);
        if (actualMd5 == null) {
            return false;
        }
        
        return expectedMd5.equalsIgnoreCase(actualMd5);
    }
    
    /**
     * 检查插件目录是否有效
     * 
     * @param installPath 插件安装路径
     * @param pluginCode 插件代码
     * @return 是否有效
     */
    public static boolean isPluginDirectoryValid(String installPath, String pluginCode) {
        if (installPath == null || pluginCode == null) {
            return false;
        }
        
        String normalizedPath = normalizePath(installPath);
        if (normalizedPath == null) {
            return false;
        }
        
        // 检查目录是否存在
        File dir = new File(normalizedPath);
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("插件目录不存在: {}", normalizedPath);
            return false;
        }
        
        // 检查jar文件是否存在
        String jarPath = buildPluginJarPath(normalizedPath, pluginCode);
        if (!isPathAccessible(jarPath)) {
            log.warn("插件JAR文件不存在或无法访问: {}", jarPath);
            return false;
        }
        
        // 检查配置文件是否存在
        String ymlPath = buildPluginYmlPath(normalizedPath);
        if (!isPathAccessible(ymlPath)) {
            log.warn("插件配置文件不存在或无法访问: {}", ymlPath);
            return false;
        }
        
        return true;
    }
} 