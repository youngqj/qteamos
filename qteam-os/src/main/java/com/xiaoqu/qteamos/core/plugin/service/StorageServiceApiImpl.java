package com.xiaoqu.qteamos.core.plugin.service;

import com.xiaoqu.qteamos.core.plugin.error.PluginErrorHandler;
import com.xiaoqu.qteamos.api.core.plugin.api.StorageServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 存储服务API实现类
 *
 * @author yangqijun
 * @date 2025-05-01
 */
@Component
public class StorageServiceApiImpl implements StorageServiceApi, InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(StorageServiceApiImpl.class);

    @Value("${plugin.storage-path}")
    private String pluginsRootPath;

    @Autowired
    private PluginErrorHandler errorHandler;

    @Autowired
    private PluginServiceApiImpl pluginServiceApi;

    /**
     * 初始化方法，输出插件存储路径
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("插件存储路径配置为: {}", pluginsRootPath);
        // 确保插件根目录存在
        try {
            File pluginsRoot = new File(pluginsRootPath);
            if (!pluginsRoot.exists()) {
                if (pluginsRoot.mkdirs()) {
                    log.info("已创建插件根目录: {}", pluginsRoot.getAbsolutePath());
                } else {
                    log.error("无法创建插件根目录: {}", pluginsRoot.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            log.error("初始化插件存储目录失败", e);
        }
    }

    @Override
    public File getPluginDataDirectory() {
        String pluginId = getCurrentPluginId();
        return getOrCreateDirectory(pluginId, "data");
    }

    @Override
    public File getPluginConfigDirectory() {
        String pluginId = getCurrentPluginId();
        return getOrCreateDirectory(pluginId, "config");
    }

    @Override
    public File getPluginTempDirectory() {
        String pluginId = getCurrentPluginId();
        return getOrCreateDirectory(pluginId, "temp");
    }

    @Override
    public File getPluginStaticDirectory() {
        String pluginId = getCurrentPluginId();
        return getOrCreateDirectory(pluginId, "static");
    }

    @Override
    public File getPluginLogDirectory() {
        String pluginId = getCurrentPluginId();
        return getOrCreateDirectory(pluginId, "logs");
    }
    
    /**
     * 获取指定插件的配置目录
     * 
     * @param pluginId 插件ID
     * @return 配置目录
     */
    public File getPluginConfigDirectory(String pluginId) {
        return getOrCreateDirectory(pluginId, "config");
    }

    @Override
    public File createFile(String relativePath) {
        try {
            String pluginId = getCurrentPluginId();
            File file = resolveFile(pluginId, relativePath);
            
            // 确保父目录存在
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new IOException("无法创建目录: " + parentDir.getAbsolutePath());
                }
            }
            
            // 创建文件
            if (!file.exists() && !file.createNewFile()) {
                throw new IOException("无法创建文件: " + file.getAbsolutePath());
            }
            
            return file;
        } catch (Exception e) {
            handleError(e, "创建文件异常: " + relativePath);
            return null;
        }
    }

    @Override
    public Optional<String> readFileContent(String relativePath) {
        try {
            String pluginId = getCurrentPluginId();
            File file = resolveFile(pluginId, relativePath);
            
            if (!file.exists() || !file.isFile()) {
                return Optional.empty();
            }
            
            byte[] bytes = Files.readAllBytes(file.toPath());
            return Optional.of(new String(bytes, StandardCharsets.UTF_8));
        } catch (Exception e) {
            handleError(e, "读取文件内容异常: " + relativePath);
            return Optional.empty();
        }
    }

    @Override
    public boolean writeFileContent(String relativePath, String content) {
        try {
            String pluginId = getCurrentPluginId();
            File file = resolveFile(pluginId, relativePath);
            
            // 确保父目录存在
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new IOException("无法创建目录: " + parentDir.getAbsolutePath());
                }
            }
            
            // 写入内容
            Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (Exception e) {
            handleError(e, "写入文件内容异常: " + relativePath);
            return false;
        }
    }

    @Override
    public Optional<InputStream> getInputStream(String relativePath) {
        try {
            String pluginId = getCurrentPluginId();
            File file = resolveFile(pluginId, relativePath);
            
            if (!file.exists() || !file.isFile()) {
                return Optional.empty();
            }
            
            return Optional.of(Files.newInputStream(file.toPath()));
        } catch (Exception e) {
            handleError(e, "获取文件输入流异常: " + relativePath);
            return Optional.empty();
        }
    }

    @Override
    public Optional<OutputStream> getOutputStream(String relativePath) {
        try {
            String pluginId = getCurrentPluginId();
            File file = resolveFile(pluginId, relativePath);
            
            // 确保父目录存在
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new IOException("无法创建目录: " + parentDir.getAbsolutePath());
                }
            }
            
            return Optional.of(Files.newOutputStream(file.toPath()));
        } catch (Exception e) {
            handleError(e, "获取文件输出流异常: " + relativePath);
            return Optional.empty();
        }
    }

    @Override
    public boolean exists(String relativePath) {
        try {
            String pluginId = getCurrentPluginId();
            File file = resolveFile(pluginId, relativePath);
            return file.exists();
        } catch (Exception e) {
            handleError(e, "检查文件存在异常: " + relativePath);
            return false;
        }
    }

    @Override
    public boolean delete(String relativePath) {
        try {
            String pluginId = getCurrentPluginId();
            File file = resolveFile(pluginId, relativePath);
            
            if (!file.exists()) {
                return true;
            }
            
            return Files.deleteIfExists(file.toPath());
        } catch (Exception e) {
            handleError(e, "删除文件异常: " + relativePath);
            return false;
        }
    }

    @Override
    public boolean createDirectory(String relativePath) {
        try {
            String pluginId = getCurrentPluginId();
            File dir = resolveFile(pluginId, relativePath);
            
            if (dir.exists()) {
                return dir.isDirectory();
            }
            
            return dir.mkdirs();
        } catch (Exception e) {
            handleError(e, "创建目录异常: " + relativePath);
            return false;
        }
    }

    @Override
    public List<Path> listDirectory(String relativePath) {
        try {
            String pluginId = getCurrentPluginId();
            File dir = resolveFile(pluginId, relativePath);
            
            if (!dir.exists() || !dir.isDirectory()) {
                return List.of();
            }
            
            try (Stream<Path> paths = Files.list(dir.toPath())) {
                return paths.collect(Collectors.toList());
            }
        } catch (Exception e) {
            handleError(e, "列出目录内容异常: " + relativePath);
            return List.of();
        }
    }

    @Override
    public long getFileSize(String relativePath) {
        try {
            String pluginId = getCurrentPluginId();
            File file = resolveFile(pluginId, relativePath);
            
            if (!file.exists() || !file.isFile()) {
                return -1;
            }
            
            return file.length();
        } catch (Exception e) {
            handleError(e, "获取文件大小异常: " + relativePath);
            return -1;
        }
    }

    @Override
    public boolean copyFile(String sourcePath, String targetPath) {
        try {
            String pluginId = getCurrentPluginId();
            File sourceFile = resolveFile(pluginId, sourcePath);
            File targetFile = resolveFile(pluginId, targetPath);
            
            if (!sourceFile.exists() || !sourceFile.isFile()) {
                return false;
            }
            
            // 确保目标父目录存在
            File parentDir = targetFile.getParentFile();
            if (!parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new IOException("无法创建目录: " + parentDir.getAbsolutePath());
                }
            }
            
            Files.copy(sourceFile.toPath(), targetFile.toPath());
            return true;
        } catch (Exception e) {
            handleError(e, "复制文件异常: " + sourcePath + " -> " + targetPath);
            return false;
        }
    }

    @Override
    public boolean moveFile(String sourcePath, String targetPath) {
        try {
            String pluginId = getCurrentPluginId();
            File sourceFile = resolveFile(pluginId, sourcePath);
            File targetFile = resolveFile(pluginId, targetPath);
            
            if (!sourceFile.exists() || !sourceFile.isFile()) {
                return false;
            }
            
            // 确保目标父目录存在
            File parentDir = targetFile.getParentFile();
            if (!parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    throw new IOException("无法创建目录: " + parentDir.getAbsolutePath());
                }
            }
            
            Files.move(sourceFile.toPath(), targetFile.toPath());
            return true;
        } catch (Exception e) {
            handleError(e, "移动文件异常: " + sourcePath + " -> " + targetPath);
            return false;
        }
    }

    /**
     * 获取或创建目录
     */
    private File getOrCreateDirectory(String pluginId, String dirName) {
        Path pluginRoot = Paths.get(pluginsRootPath, pluginId);
        Path dirPath = pluginRoot.resolve(dirName);
        File dir = dirPath.toFile();
        
        if (!dir.exists() && !dir.mkdirs()) {
            log.error("无法创建目录: {}", dirPath);
        }
        
        return dir;
    }

    /**
     * 解析文件路径
     */
    private File resolveFile(String pluginId, String relativePath) {
        Path pluginRoot = Paths.get(pluginsRootPath, pluginId, "data");
        return pluginRoot.resolve(relativePath).toFile();
    }

    /**
     * 获取当前插件ID
     */
    private String getCurrentPluginId() {
        return pluginServiceApi.getPluginId();
    }

    /**
     * 处理错误
     */
    private void handleError(Exception e, String message) {
        log.error(message, e);
        errorHandler.handlePluginError(getCurrentPluginId(), e, PluginErrorHandler.OperationType.RUNTIME);
    }
} 