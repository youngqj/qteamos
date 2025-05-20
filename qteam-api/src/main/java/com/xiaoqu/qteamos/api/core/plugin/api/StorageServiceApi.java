package com.xiaoqu.qteamos.api.core.plugin.api;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * 存储服务API接口
 * 提供安全的文件存储访问能力
 *
 * @author yangqijun
 * @date 2025-05-01
 */
public interface StorageServiceApi {

    /**
     * 获取插件数据目录
     *
     * @return 插件数据目录
     */
    File getPluginDataDirectory();

    /**
     * 获取插件配置目录
     *
     * @return 插件配置目录
     */
    File getPluginConfigDirectory();

    /**
     * 获取插件临时目录
     *
     * @return 插件临时目录
     */
    File getPluginTempDirectory();

    /**
     * 获取插件静态资源目录
     *
     * @return 插件静态资源目录
     */
    File getPluginStaticDirectory();

    /**
     * 获取插件日志目录
     *
     * @return 插件日志目录
     */
    File getPluginLogDirectory();

    /**
     * 创建文件
     *
     * @param relativePath 相对路径
     * @return 文件对象
     */
    File createFile(String relativePath);

    /**
     * 读取文件内容
     *
     * @param relativePath 相对路径
     * @return 文件内容
     */
    Optional<String> readFileContent(String relativePath);

    /**
     * 写入文件内容
     *
     * @param relativePath 相对路径
     * @param content 文件内容
     * @return 是否成功
     */
    boolean writeFileContent(String relativePath, String content);

    /**
     * 获取文件输入流
     *
     * @param relativePath 相对路径
     * @return 输入流
     */
    Optional<InputStream> getInputStream(String relativePath);

    /**
     * 获取文件输出流
     *
     * @param relativePath 相对路径
     * @return 输出流
     */
    Optional<OutputStream> getOutputStream(String relativePath);

    /**
     * 文件是否存在
     *
     * @param relativePath 相对路径
     * @return 是否存在
     */
    boolean exists(String relativePath);

    /**
     * 删除文件
     *
     * @param relativePath 相对路径
     * @return 是否成功
     */
    boolean delete(String relativePath);

    /**
     * 创建目录
     *
     * @param relativePath 相对路径
     * @return 是否成功
     */
    boolean createDirectory(String relativePath);

    /**
     * 列出目录内容
     *
     * @param relativePath 相对路径
     * @return 目录内容
     */
    List<Path> listDirectory(String relativePath);

    /**
     * 获取文件大小
     *
     * @param relativePath 相对路径
     * @return 文件大小
     */
    long getFileSize(String relativePath);

    /**
     * 复制文件
     *
     * @param sourcePath 源路径
     * @param targetPath 目标路径
     * @return 是否成功
     */
    boolean copyFile(String sourcePath, String targetPath);

    /**
     * 移动文件
     *
     * @param sourcePath 源路径
     * @param targetPath 目标路径
     * @return 是否成功
     */
    boolean moveFile(String sourcePath, String targetPath);
} 