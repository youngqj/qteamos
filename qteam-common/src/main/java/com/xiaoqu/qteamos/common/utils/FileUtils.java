package com.xiaoqu.qteamos.common.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.digest.DigestUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件工具类
 */
@Slf4j
public class FileUtils {

    /**
     * 计算文件MD5值
     *
     * @param file 文件
     * @return MD5值
     */
    public static String calculateMD5(File file) {
        try {
            return DigestUtil.md5Hex(file);
        } catch (Exception e) {
            log.error("计算文件MD5值失败", e);
            return null;
        }
    }

    /**
     * 计算文件MD5值
     *
     * @param filePath 文件路径
     * @return MD5值
     */
    public static String calculateMD5(String filePath) {
        return calculateMD5(new File(filePath));
    }

    /**
     * 验证文件MD5值
     *
     * @param file 文件
     * @param md5  MD5值
     * @return 是否匹配
     */
    public static boolean verifyMD5(File file, String md5) {
        String calculatedMD5 = calculateMD5(file);
        return calculatedMD5 != null && calculatedMD5.equalsIgnoreCase(md5);
    }

    /**
     * 验证文件MD5值
     *
     * @param filePath 文件路径
     * @param md5     MD5值
     * @return 是否匹配
     */
    public static boolean verifyMD5(String filePath, String md5) {
        return verifyMD5(new File(filePath), md5);
    }

    /**
     * 创建目录
     *
     * @param dirPath 目录路径
     * @return 是否成功
     */
    public static boolean createDirectory(String dirPath) {
        try {
            Path path = Paths.get(dirPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            return true;
        } catch (IOException e) {
            log.error("创建目录失败: {}", dirPath, e);
            return false;
        }
    }

    /**
     * 删除文件或目录
     *
     * @param path 路径
     * @return 是否成功
     */
    public static boolean delete(String path) {
        try {
            return FileUtil.del(path);
        } catch (Exception e) {
            log.error("删除文件或目录失败: {}", path, e);
            return false;
        }
    }

    /**
     * 移动文件
     *
     * @param srcPath  源路径
     * @param destPath 目标路径
     * @return 是否成功
     */
    public static boolean move(String srcPath, String destPath) {
        try {
            FileUtil.move(Paths.get(srcPath), Paths.get(destPath), true);
            return true;
        } catch (Exception e) {
            log.error("移动文件失败: {} -> {}", srcPath, destPath, e);
            return false;
        }
    }

    /**
     * 复制文件
     *
     * @param srcPath  源路径
     * @param destPath 目标路径
     * @return 是否成功
     */
    public static boolean copy(String srcPath, String destPath) {
        try {
            FileUtil.copy(srcPath, destPath, true);
            return true;
        } catch (Exception e) {
            log.error("复制文件失败: {} -> {}", srcPath, destPath, e);
            return false;
        }
    }

    /**
     * 获取文件扩展名
     *
     * @param fileName 文件名
     * @return 扩展名
     */
    public static String getExtension(String fileName) {
        return FileUtil.extName(fileName);
    }

    /**
     * 获取文件大小
     *
     * @param file 文件
     * @return 文件大小（字节）
     */
    public static long getFileSize(File file) {
        return FileUtil.size(file);
    }

    /**
     * 获取格式化的文件大小
     *
     * @param file 文件
     * @return 格式化后的文件大小（如：1.5 MB）
     */
    public static String getFormattedFileSize(File file) {
        return FileUtil.readableFileSize(getFileSize(file));
    }
} 