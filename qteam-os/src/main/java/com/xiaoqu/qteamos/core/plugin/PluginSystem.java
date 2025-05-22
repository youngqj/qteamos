package com.xiaoqu.qteamos.core.plugin;


import com.xiaoqu.qteamos.core.utils.PluginFileUtils;
import com.xiaoqu.qteamos.core.plugin.event.EventBus;
import com.xiaoqu.qteamos.core.plugin.event.PluginEvent;
import com.xiaoqu.qteamos.core.plugin.event.plugins.SystemShutdownEvent;
import com.xiaoqu.qteamos.core.plugin.loader.DynamicClassLoader;
import com.xiaoqu.qteamos.core.plugin.loader.DynamicClassLoaderFactory;
import com.xiaoqu.qteamos.core.plugin.manager.DependencyResolver;
import com.xiaoqu.qteamos.core.plugin.manager.PluginLifecycleManager;
import com.xiaoqu.qteamos.core.plugin.manager.PluginRegistry;
import com.xiaoqu.qteamos.core.plugin.manager.PluginStateManager;
import com.xiaoqu.qteamos.api.core.plugin.exception.PluginLifecycleException;
import com.xiaoqu.qteamos.core.plugin.manager.persistence.PluginStatePersistenceManager;
import com.xiaoqu.qteamos.core.plugin.running.PluginDescriptor;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import com.xiaoqu.qteamos.core.plugin.running.PluginState;
import com.xiaoqu.qteamos.api.core.plugin.Plugin;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * 插件系统入口
 * 整合所有插件管理组件，提供统一的对外接口
 *
 * @author yangqijun
 * @date 2024-07-02
 */
@Component
public class PluginSystem {
    private static final Logger log = LoggerFactory.getLogger(PluginSystem.class);
    
    @Value("${plugin.storage-path:./plugins}")
    private String pluginDir;
    
    @Value("${plugin.temp-dir:./plugins-temp}")
    private String pluginTempDir;
    
    @Value("${plugin.auto-discover:true}")
    private boolean autoDiscoverEnabled;
    
    @Value("${plugin.watch-dir:true}")
    private boolean watchDirEnabled;
    
    private final PluginRegistry pluginRegistry;
    private final PluginLifecycleManager lifecycleManager;
    private final PluginStateManager stateManager;
    private final DependencyResolver dependencyResolver;
    private final EventBus eventBus;
    private final DynamicClassLoaderFactory classLoaderFactory;
    
    // 插件状态持久化管理器
    private final PluginStatePersistenceManager pluginStatePersistenceManager;
    
    // 线程池
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    // 已知插件文件映射
    private final Map<String, Long> knownPluginFiles = new ConcurrentHashMap<>();
    
    // 文件监控服务
    private WatchService pluginWatchService;
    private WatchService tempWatchService;
    
    @Autowired
    public PluginSystem(PluginRegistry pluginRegistry, 
                       PluginLifecycleManager lifecycleManager,
                       PluginStateManager stateManager,
                       DependencyResolver dependencyResolver,
                       EventBus eventBus,
                       DynamicClassLoaderFactory classLoaderFactory,
                       PluginStatePersistenceManager pluginStatePersistenceManager) {
        this.pluginRegistry = pluginRegistry;
        this.lifecycleManager = lifecycleManager;
        this.stateManager = stateManager;
        this.dependencyResolver = dependencyResolver;
        this.eventBus = eventBus;
        this.classLoaderFactory = classLoaderFactory;
        this.pluginStatePersistenceManager = pluginStatePersistenceManager;
    }
    
    /**
     * 初始化插件系统
     * 注意：此方法已不再使用@PostConstruct自动执行，而是由SystemInitializer协调调用
     */
    public void init() {
        log.info("初始化插件系统...");
        
        // 设置类加载系统属性
        System.setProperty("java.system.class.loader", "com.xiaoqu.qteamos.core.plugin.loader.DynamicClassLoader");
        // 允许从父类加载器重复加载类，解决可能的类加载冲突
        System.setProperty("spring.classloader.overrideStandard", "true");
        
        // 设置类加载器共享包系统属性
        System.setProperty("plugin.shared.packages", 
                "com.xiaoqu.qteamos.sdk,com.xiaoqu.qteamos.api,com.xiaoqu.qteamos.sdk.plugin,com.xiaoqu.qteamos.api.core.plugin");
        
        // 允许插件使用线程上下文类加载器
        System.setProperty("plugin.thread.contextClassLoader", "system");
        
        // 创建插件目录
        createPluginDirectory();
        // 创建临时插件目录
        createTempPluginDirectory();
        // 自动扫描和加载插件
        executor.submit(() -> {
            // 扫描标准插件目录
            scanAndLoadPlugins();
            
            // 扫描临时插件目录中的插件
            log.info("扫描临时插件目录中的插件...");
            File tempDir = new File(pluginTempDir);
            if (tempDir.exists() && tempDir.isDirectory()) {
                File[] files = tempDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().equals("README.txt") || file.getName().startsWith(".")) {
                            continue;
                        }
                        log.info("处理临时目录中的文件: {}", file.getName());
                        processTempFile(file);
                    }
                }
            }
        });
        
        // 启动文件监控
        if (watchDirEnabled) {
            startFileWatcher();
        }
        
        log.info("插件系统初始化完成");
    }
    
    /**
     * 创建临时插件目录
     */
    private void createTempPluginDirectory() {
        File tempDir = new File(pluginTempDir);
        if (!tempDir.exists()) {
            if (tempDir.mkdirs()) {
                log.info("创建临时插件目录: {}", tempDir.getAbsolutePath());
                
                // 创建README文件
                File readmeFile = new File(tempDir, "README.txt");
                if (!readmeFile.exists()) {
                    try {
                        if (readmeFile.createNewFile()) {
                            Files.writeString(readmeFile.toPath(), 
                                "临时插件目录\n" +
                                "将需要安装的插件放入此目录，系统将自动验证并安装\n" +
                                "支持的格式：\n" +
                                "1. 插件JAR包\n" +
                                "2. 插件目录（包含plugin.jar和plugin.yml）"
                            );
                            log.debug("创建临时插件目录说明文件");
                        }
                    } catch (IOException e) {
                        log.warn("创建临时插件目录说明文件失败", e);
                    }
                }
            } else {
                log.error("创建临时插件目录失败: {}", tempDir.getAbsolutePath());
            }
        }
    }
    
    /**
     * 启动文件监控服务
     */
    private void startFileWatcher() {
        try {
            // 监控插件目录
            pluginWatchService = FileSystems.getDefault().newWatchService();
            Path dirPath = Paths.get(pluginDir);
            dirPath.register(pluginWatchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            
            // 启动插件目录监控线程
            executor.submit(this::watchPluginDirectory);
            log.info("插件目录监控已启动: {}", dirPath);
            
            // 监控临时插件目录
            tempWatchService = FileSystems.getDefault().newWatchService();
            Path tempDirPath = Paths.get(pluginTempDir);
            tempDirPath.register(tempWatchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            
            // 启动临时目录监控线程
            executor.submit(this::watchTempDirectory);
            log.info("临时插件目录监控已启动: {}", tempDirPath);
        } catch (IOException e) {
            log.error("启动文件监控服务失败", e);
        }
    }
    
    /**
     * 监控插件目录的文件变化
     */
    private void watchPluginDirectory() {
        try {
            log.info("开始监控插件目录变化...");
            WatchKey key;
            while ((key = pluginWatchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    // 忽略OVERFLOW事件
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    
                    // 获取文件名
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path fileName = ev.context();
                    Path fullPath = Paths.get(pluginDir, fileName.toString());
                    File file = fullPath.toFile();
                    
                    // 只处理JAR文件
                    if (!fileName.toString().endsWith(".jar")) {
                        continue;
                    }
                    
                    if (kind == ENTRY_CREATE) {
                        log.info("检测到新插件文件: {}", fileName);
                        // 避免文件可能还在写入中，等待一段时间
                        Thread.sleep(2000);
                        processNewPluginFile(file);
                    } else if (kind == ENTRY_MODIFY) {
                        log.info("检测到插件文件变更: {}", fileName);
                        // 避免文件可能还在写入中，等待一段时间
                        Thread.sleep(2000);
                        processModifiedPluginFile(file);
                    } else if (kind == ENTRY_DELETE) {
                        log.info("检测到插件文件删除: {}", fileName);
                        processDeletedPluginFile(fileName.toString());
                    }
                }
                
                // 重置key，继续监听
                boolean valid = key.reset();
                if (!valid) {
                    log.warn("插件目录监控已失效，退出监控");
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("插件目录监控线程被中断");
        } catch (Exception e) {
            log.error("监控插件目录时发生异常", e);
        }
    }
    
    /**
     * 监控临时插件目录的文件变化
     */
    private void watchTempDirectory() {
        try {
            log.info("开始监控临时插件目录变化...");
            WatchKey key;
            while (!Thread.currentThread().isInterrupted() && (key = tempWatchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    // 忽略OVERFLOW事件
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    
                    // 获取文件名
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path fileName = ev.context();
                    Path fullPath = Paths.get(pluginTempDir, fileName.toString());
                    File file = fullPath.toFile();
                    
                    // 忽略README.txt文件
                    if (fileName.toString().equals("README.txt")) {
                        continue;
                    }
                    
                    // 忽略隐藏文件
                    if (fileName.toString().startsWith(".")) {
                        log.debug("忽略隐藏文件: {}", fileName);
                        continue;
                    }
                    
                    if (kind == ENTRY_CREATE) {
                        log.info("检测到临时目录新文件: {}", fileName);
                        // 避免文件可能还在写入中，等待一段时间
                        Thread.sleep(2000);
                        processTempFile(file);
                    }
                }
                
                // 重置key，继续监听
                boolean valid = key.reset();
                if (!valid) {
                    log.warn("临时插件目录监控已失效，退出监控");
                    break;
                }
            }
        } catch (ClosedWatchServiceException e) {
            // 优雅处理关闭异常
            log.info("临时目录监控服务已关闭，监控线程退出");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("临时插件目录监控线程被中断");
        } catch (Exception e) {
            log.error("监控临时插件目录时发生异常", e);
        }
    }
    
    /**
     * 处理新插件文件
     */
    private void processNewPluginFile(File file) {
        try {
            log.info("处理新增插件文件: {}", file.getName());
            installPlugin(file);
            knownPluginFiles.put(file.getName(), file.lastModified());
        } catch (Exception e) {
            log.error("处理新增插件文件失败: " + file.getName(), e);
        }
    }
    
    /**
     * 处理修改的插件文件
     */
    private void processModifiedPluginFile(File file) {
        try {
            String fileName = file.getName();
            long lastModified = file.lastModified();
            Long knownLastModified = knownPluginFiles.get(fileName);
            
            // 如果文件修改时间相同或未知，则忽略
            if (knownLastModified != null && knownLastModified == lastModified) {
                return;
            }
            
            log.info("处理修改的插件文件: {}", fileName);
            
            // 尝试解析插件描述符
            PluginDescriptor descriptor = PluginFileUtils.parsePluginDescriptor(file.toPath());
            if (descriptor == null) {
                log.error("无法解析插件描述符: {}", file.getAbsolutePath());
                return;
            }
            
            // 检查插件是否已存在
            String pluginId = descriptor.getPluginId();
            if (pluginRegistry.hasPlugin(pluginId)) {
                // 重载插件
                reloadPlugin(pluginId);
            } else {
                // 安装新插件
                installPlugin(file);
            }
            
            // 更新已知文件修改时间
            knownPluginFiles.put(fileName, lastModified);
        } catch (Exception e) {
            log.error("处理修改的插件文件失败: " + file.getName(), e);
        }
    }
    
    /**
     * 处理删除的插件文件
     */
    private void processDeletedPluginFile(String fileName) {
        try {
            // 找到对应的插件ID
            Optional<PluginInfo> pluginInfo = pluginRegistry.getAllPlugins().stream()
                    .filter(info -> info.getJarPath() != null && 
                            info.getJarPath().getFileName().toString().equals(fileName))
                    .findFirst();
            
            if (pluginInfo.isPresent()) {
                String pluginId = pluginInfo.get().getDescriptor().getPluginId();
                log.info("处理删除的插件文件: {}, 对应插件ID: {}", fileName, pluginId);
                unloadPlugin(pluginId);
            }
            
            // 从已知文件列表中移除
            knownPluginFiles.remove(fileName);
        } catch (Exception e) {
            log.error("处理删除的插件文件失败: " + fileName, e);
        }
    }
    
    /**
     * 处理临时目录中的文件
     */
    private void processTempFile(File file) {
        try {
            // 忽略以点开头的隐藏文件
            if (file.getName().startsWith(".")) {
                log.debug("忽略隐藏文件: {}", file.getName());
                return;
            }
            
            if (file.isDirectory()) {
                processTempPluginDirectory(file);
            } else if (file.getName().endsWith(".jar")) {
                processTempPluginJar(file);
            } else {
                log.warn("不支持的文件类型: {}", file.getName());
            }
        } catch (Exception e) {
            log.error("处理临时文件失败: " + file.getName(), e);
        }
    }
    
    /**
     * 处理临时目录中的插件目录
     */
    private void processTempPluginDirectory(File tempPluginDir) {
        log.info("处理临时插件目录: {}", tempPluginDir.getName());
        
        // 检查必要文件
        File ymlFile = new File(tempPluginDir, "plugin.yml");
        if (!ymlFile.exists()) {
            log.warn("临时插件目录缺少plugin.yml文件: {}", tempPluginDir);
            return;
        }
        
        // 解析plugin.yml文件
        PluginDescriptor descriptor = PluginFileUtils.parsePluginYml(ymlFile);
        if (descriptor == null) {
            log.error("解析插件配置文件失败: {}", ymlFile);
            return;
        }
        
        String pluginId = descriptor.getPluginId();
        
        // 查找插件JAR文件
        File jarFile = findPluginJarFile(tempPluginDir, pluginId);
        if (jarFile == null) {
            log.warn("临时插件目录中找不到有效的JAR文件: {}", tempPluginDir);
            return;
        }
        
        // 验证插件
        if (!validatePlugin(jarFile, descriptor)) {
            log.error("插件验证失败: {}", pluginId);
            return;
        }
        
        // 迁移插件到安装目录
        migratePlugin(tempPluginDir, pluginId);
    }
    
    /**
     * 处理临时目录中的插件JAR文件
     */
    private void processTempPluginJar(File jarFile) {
        log.info("处理临时插件JAR文件: {}", jarFile.getName());
        
        // 解析JAR文件中的plugin.yml
        PluginDescriptor descriptor = PluginFileUtils.parsePluginDescriptor(jarFile.toPath());
        if (descriptor == null) {
            log.error("无法从JAR文件解析插件描述符: {}", jarFile.getAbsolutePath());
            return;
        }
        
        String pluginId = descriptor.getPluginId();
        
        // 验证插件
        if (!validatePlugin(jarFile, descriptor)) {
            log.error("插件验证失败: {}", pluginId);
            return;
        }
        
        // 创建插件目录
        File targetPluginDir = new File(pluginDir, pluginId);
        if (!targetPluginDir.exists()) {
            if (!targetPluginDir.mkdirs()) {
                log.error("创建插件目录失败: {}", targetPluginDir);
                return;
            }
        }
        
        // 复制JAR文件到插件目录
        try {
            File targetJarFile = new File(targetPluginDir, "plugin.jar");
            Files.copy(jarFile.toPath(), targetJarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            // 提取plugin.yml
            File targetYmlFile = new File(targetPluginDir, "plugin.yml");
            PluginFileUtils.extractPluginYml(jarFile.toPath(), targetYmlFile);
            
            log.info("插件 {} 安装完成", pluginId);
            
            // 删除临时JAR文件
            if (!jarFile.delete()) {
                log.warn("删除临时JAR文件失败: {}", jarFile);
            }
            
            // 处理新安装的插件
            processPluginDirectory(targetPluginDir);
            
        } catch (IOException e) {
            log.error("复制插件文件失败", e);
        }
    }
    
    /**
     * 迁移插件从临时目录到安装目录
     */
    private void migratePlugin(File tempPluginDir, String pluginId) {
        // 创建目标插件目录
        File targetPluginDir = new File(pluginDir, pluginId);
        if (!targetPluginDir.exists()) {
            if (!targetPluginDir.mkdirs()) {
                log.error("创建插件目录失败: {}", targetPluginDir);
                return;
            }
        }
        
        try {
            // 复制所有文件
            File[] files = tempPluginDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        Files.copy(file.toPath(), new File(targetPluginDir, file.getName()).toPath(), 
                                StandardCopyOption.REPLACE_EXISTING);
                    } else if (file.isDirectory()) {
                        // 创建子目录
                        File targetSubDir = new File(targetPluginDir, file.getName());
                        if (!targetSubDir.exists() && !targetSubDir.mkdirs()) {
                            log.warn("创建插件子目录失败: {}", targetSubDir);
                        }
                        
                        // 这里可以递归复制子目录内容，简化起见暂不实现
                    }
                }
            }
            
            log.info("插件 {} 安装完成", pluginId);
            
            // 删除临时目录
            deleteDirectory(tempPluginDir);
            
            // 处理新安装的插件
            processPluginDirectory(targetPluginDir);
            
        } catch (IOException e) {
            log.error("迁移插件文件失败", e);
        }
    }
    
    /**
     * 验证插件
     */
    private boolean validatePlugin(File jarFile, PluginDescriptor descriptor) {
        // 插件基本信息验证
        if (descriptor.getPluginId() == null || descriptor.getPluginId().isEmpty()) {
            log.error("插件ID不能为空");
            return false;
        }
        
        if (descriptor.getVersion() == null || descriptor.getVersion().isEmpty()) {
            log.error("插件版本不能为空");
            return false;
        }
        
        if (descriptor.getMainClass() == null || descriptor.getMainClass().isEmpty()) {
            log.error("插件主类不能为空");
            return false;
        }
        
        // TODO: 可以添加更多验证逻辑，如签名验证、兼容性检查等
        
        return true;
    }
    
    /**
     * 递归删除目录
     */
    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        if (!file.delete()) {
                            log.warn("删除文件失败: {}", file);
                        }
                    }
                }
            }
            if (!directory.delete()) {
                log.warn("删除目录失败: {}", directory);
            }
        }
    }
    
    /**
     * 查找插件JAR文件
     */
    private File findPluginJarFile(File pluginDir, String pluginId) {
        // 按优先级查找以下文件：
        // 1. pluginId.jar
        File exactMatch = new File(pluginDir, pluginId + ".jar");
        if (exactMatch.exists() && exactMatch.isFile()) {
            return exactMatch;
        }
        
        // 2. 以pluginId-开头的jar (匹配如 pluginId-1.0.0.jar)
        File[] versionedJars = pluginDir.listFiles((dir, name) -> 
            name.startsWith(pluginId + "-") && name.endsWith(".jar"));
        if (versionedJars != null && versionedJars.length > 0) {
            // 如果有多个版本，返回按字母排序最后一个（通常是最新版本）
            Arrays.sort(versionedJars);
            return versionedJars[versionedJars.length - 1];
        }
        
        // 3. 目录中的plugin.jar (保持向后兼容)
        File legacyFile = new File(pluginDir, "plugin.jar");
        if (legacyFile.exists() && legacyFile.isFile()) {
            return legacyFile;
        }
        
        // 4. 如果目录中只有一个jar文件，直接使用它
        File[] jarFiles = pluginDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles != null && jarFiles.length == 1) {
            return jarFiles[0];
        }
        
        // 没有找到合适的JAR文件
        return null;
    }
    
    /**
     * 定期扫描插件目录，发现新插件
     * 默认每5分钟执行一次
     */
    @Scheduled(fixedDelayString = "${plugin.scan-interval:300000}")
    public void scheduledScan() {
        if (!autoDiscoverEnabled) {
            return;
        }
        
        log.info("执行定期插件扫描...");
        scanForNewPlugins();
    }
    
    /**
     * 扫描插件目录，查找新的插件
     */
    public void scanForNewPlugins() {
        try {
            File dir = new File(pluginDir);
            if (!dir.exists() || !dir.isDirectory()) {
                log.error("插件目录不存在或不是目录: {}", dir.getAbsolutePath());
                return;
            }
            
            // 获取所有插件JAR文件
            File[] jarFiles = dir.listFiles((d, name) -> name.endsWith(".jar"));
            if (jarFiles == null || jarFiles.length == 0) {
                return;
            }
            
            // 过滤出新插件文件
            Set<String> loadedPaths = pluginRegistry.getAllPlugins().stream()
                    .filter(info -> info.getJarPath() != null)
                    .map(info -> info.getJarPath().toString())
                    .collect(Collectors.toSet());
            
            List<File> newPluginFiles = Arrays.stream(jarFiles)
                    .filter(file -> !loadedPaths.contains(file.toPath().toString()))
                    .collect(Collectors.toList());
            
            // 加载新插件
            for (File file : newPluginFiles) {
                try {
                    log.info("发现新插件: {}", file.getName());
                    installPlugin(file);
                    knownPluginFiles.put(file.getName(), file.lastModified());
                } catch (Exception e) {
                    log.error("加载新插件失败: " + file.getName(), e);
                }
            }
            
            if (!newPluginFiles.isEmpty()) {
                log.info("扫描发现并加载了{}个新插件", newPluginFiles.size());
            }
        } catch (Exception e) {
            log.error("扫描新插件过程中发生异常", e);
        }
    }
    
    /**
     * 创建插件目录
     */
    private void createPluginDirectory() {
        File dir = new File(pluginDir);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                log.info("创建插件目录成功: {}", dir.getAbsolutePath());
                
                // 创建示例插件目录结构
                createSamplePluginStructure(dir);
            } else {
                log.error("创建插件目录失败: {}", dir.getAbsolutePath());
            }
        }
    }
    
    /**
     * 创建示例插件目录结构
     */
    private void createSamplePluginStructure(File parentDir) {
        try {
            // 创建示例插件目录
            File exampleDir = new File(parentDir, "example-plugin");
            if (exampleDir.mkdir()) {
                // 创建示例插件子目录
                new File(exampleDir, "config").mkdir();
                new File(exampleDir, "data").mkdir();
                new File(exampleDir, "static").mkdir();
                new File(exampleDir, "templates").mkdir();
                
                // 创建README文件，说明这是一个示例
                File readmeFile = new File(exampleDir, "README.txt");
                Files.writeString(readmeFile.toPath(), 
                    "这是一个示例插件目录结构，用于演示插件目录组织方式。\n" +
                    "实际插件需要包含plugin.jar和plugin.yml文件。\n" +
                    "详细信息请参考开发文档。");
                
                log.info("创建示例插件目录: {}", exampleDir.getAbsolutePath());
            }
            
            // 创建系统插件目录
            File systemDir = new File(parentDir, "system-plugins");
            if (systemDir.mkdir()) {
                // 创建README文件
                File readmeFile = new File(systemDir, "README.txt");
                Files.writeString(readmeFile.toPath(), 
                    "此目录用于存放系统级插件，通常由系统自动管理。\n" +
                    "请勿手动修改此目录中的文件。");
                
                log.info("创建系统插件目录: {}", systemDir.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("创建示例插件目录结构失败", e);
        }
    }
    
    /**
     * 扫描并加载插件
     */
    public void scanAndLoadPlugins() {
        log.info("扫描并加载插件...");
        try {
            File dir = new File(pluginDir);
            log.info("***** 插件目录路径: {}, 是否存在: {}, 是否是目录: {}", dir.getAbsolutePath(), dir.exists(), dir.isDirectory());
            
            if (!dir.exists() || !dir.isDirectory()) {
                log.error("插件目录不存在或不是目录: {}", dir.getAbsolutePath());
                return;
            }
            
            // 获取所有插件目录（以插件ID命名的目录）
            File[] pluginDirs = dir.listFiles(File::isDirectory);
            if (pluginDirs == null || pluginDirs.length == 0) {
                log.info("未找到插件目录");
                return;
            }
            
            log.info("***** 发现{}个插件目录", pluginDirs.length);
            for (File pluginDir : pluginDirs) {
                log.info("***** 发现插件目录: {}", pluginDir.getName());
            }
            
            // 处理每个插件目录
            for (File pluginDir : pluginDirs) {
                try {
                    log.info("***** 开始处理插件目录: {}", pluginDir.getName());
                    processPluginDirectory(pluginDir);
                    log.info("***** 插件目录处理完成: {}", pluginDir.getName());
                } catch (Exception e) {
                    log.error("处理插件目录失败: " + pluginDir.getName(), e);
                }
            }
            
            // 尝试加载之前加载失败的插件（可能是由于依赖关系未满足）
            reloadFailedPlugins();
            
            log.info("插件扫描和加载完成，共加载{}个插件", pluginRegistry.getPluginCount());
        } catch (Exception e) {
            log.error("扫描和加载插件过程中发生异常", e);
        }
    }
    
    /**
     * 处理插件目录
     */
    private void processPluginDirectory(File pluginDir) {
        log.info("====> yqj 开始处理插件目录: {}", pluginDir.getAbsolutePath());
        String pluginId = pluginDir.getName();
        log.info("====> 解析的插件ID: {}", pluginId);
        
        // 检查是否是示例目录（包含README.txt文件）
        File readmeFile = new File(pluginDir, "README.txt");
        if (readmeFile.exists()) {
            // 这是一个示例目录，跳过处理
            log.info("====> 跳过示例目录: {}", pluginDir);
            return;
        }
        
       // File jarFile = new File(pluginDir, "plugin.jar");
        File ymlFile = new File(pluginDir, "plugin.yml");
        
        log.info("====> 检查插件yml文件: {}, 是否存在: {}", ymlFile.getAbsolutePath(), ymlFile.exists());

        // 首先检查plugin.yml文件是否存在
        if (!ymlFile.exists()) {
            log.warn("插件目录缺少必要文件plugin.yml: {}", pluginDir);
            return;
        }       
        
        // 解析yml文件
        log.info("====> 开始解析yml文件");
        PluginDescriptor descriptor = PluginFileUtils.parsePluginYml(ymlFile);
        if (descriptor == null) {
            log.error("解析插件配置文件失败: {}", ymlFile);
            return;
        }
        log.debug("====> yml文件解析结果: pluginId={}, 版本={}, 类型={}, 信任级别={}",
                descriptor.getPluginId(), descriptor.getVersion(), descriptor.getType(), descriptor.getTrust());

        File jarFile = findPluginJarFile(pluginDir, pluginId);
        if (jarFile == null) {
            log.warn("插件目录中找不到有效的JAR文件: {}", pluginDir);
            return;
        }
        log.info("====> 找到jar文件: {}", jarFile.getAbsolutePath());
        
        // 记录已知插件文件
        knownPluginFiles.put(jarFile.getAbsolutePath(), jarFile.lastModified());
        
        // 验证插件ID
        if (!pluginId.equals(descriptor.getPluginId())) {
            log.warn("插件目录名与插件ID不匹配: {} vs {}", pluginId, descriptor.getPluginId());
        }
        
        String pluginIdString = descriptor.getPluginId();
        if (!pluginIdString.equals(pluginId)) {
            log.info("插件ID: {} 存放的路径目录为：{}", pluginIdString, pluginId);
        }
        
        // 检查插件是否已存在
        log.info("====> 检查插件是否已存在: {}", pluginId);
        boolean pluginExists = pluginRegistry.hasPlugin(pluginIdString);
        log.info("====> 插件{}存在: {}", pluginIdString, pluginExists);
        
        if (pluginExists) {
            log.info("插件已存在，尝试更新: {}", pluginId);
            updateExistingPlugin(pluginId, jarFile, descriptor, pluginDir);
            return;
        }
        
        // 根据类型和信任级别决定处理方式
        boolean isSystemPlugin = "system".equals(descriptor.getType());
        boolean isTrusted = "trust".equals(descriptor.getTrust());
        
        log.info("====> 插件类型: {}, 信任级别: {}, 是否系统插件: {}, 是否可信: {}", 
            descriptor.getType(), descriptor.getTrust(), isSystemPlugin, isTrusted);
        
        if (isSystemPlugin && isTrusted) {
            // 系统级可信插件，自动加载并启动
            log.info("====> 检测到系统级可信插件，自动加载并启动: {}", pluginId);
            processPluginLoading(jarFile.toPath(), descriptor, pluginDir, true);
        } else {
            // 普通插件，只注册到数据库
            log.info("====> 检测到普通插件，仅注册到数据库: {}", pluginId);
            registerPluginInfo(descriptor, jarFile.toPath(), pluginDir);
            log.info("普通插件已注册: {}，需要通过管理界面启用", pluginId);
        }
        
        log.info("====> 插件目录处理完成: {}", pluginDir.getAbsolutePath());
    }
    
    /**
     * 处理插件加载、初始化和启动的完整流程
     * 
     * @param jarPath 插件JAR路径
     * @param descriptor 插件描述符
     * @param pluginDir 插件目录
     * @param isSystemPlugin 是否是系统插件
     */
    private void processPluginLoading(Path jarPath, PluginDescriptor descriptor, File pluginDir, boolean isSystemPlugin) {
        String pluginId = descriptor.getPluginId();
        try {
            String pluginTypeDesc = isSystemPlugin ? "系统" : "普通";
            log.info("开始处理{}插件: {}", pluginTypeDesc, pluginId);
            
            // 创建类加载器
            DynamicClassLoader classLoader;
            try {
                log.debug("为插件{}创建类加载器", pluginId);
                classLoader = classLoaderFactory.createClassLoader(pluginId, jarPath.toFile());
            } catch (IOException e) {
                throw new IllegalStateException("创建插件类加载器失败: " + pluginId, e);
            }
            
            // 创建插件信息
            log.debug("创建插件{}的信息对象", pluginId);
            PluginInfo pluginInfo = createPluginInfo(descriptor, classLoader, jarPath, pluginDir);
            pluginInfo.setEnabled(true); // 默认启用系统插件
            
            // 注册插件
            log.debug("注册插件{}", pluginId);
            if (!pluginRegistry.registerPlugin(pluginInfo)) {
                throw new IllegalStateException("注册插件失败: " + pluginId);
            }
            
            // 保存插件信息到数据库
            try {
                log.debug("保存插件{}信息到数据库", pluginId);
                pluginStatePersistenceManager.savePluginInfo(pluginInfo);
                log.info("插件信息已保存到数据库: {}", pluginId);
            } catch (Exception e) {
                log.error("保存插件信息到数据库失败: {}", pluginId, e);
                // 由于已经注册成功，这里不回滚，继续执行
            }
            
            // 记录状态变化并发布事件
            log.debug("插件{}状态变更为CREATED", pluginId);
            stateManager.recordStateChange(pluginId, PluginState.CREATED);
            
            // 加载插件
            log.debug("加载插件{}", pluginId);
            if (!lifecycleManager.loadPlugin(pluginInfo)) {
                throw new IllegalStateException("加载插件失败: " + pluginId);
            }
            
            // 发布插件加载事件
            log.debug("发布插件{}加载事件", pluginId);
            eventBus.postEvent(PluginEvent.createLoadedEvent(pluginId, descriptor.getVersion()));
            
            // 初始化插件
            log.debug("初始化插件{}", pluginId);
            if (!lifecycleManager.initializePlugin(pluginId)) {
                throw new IllegalStateException("初始化插件失败: " + pluginId);
            }
            
            // 发布插件初始化事件
            log.debug("发布插件{}初始化事件", pluginId);
            eventBus.postEvent(PluginEvent.createInitializedEvent(pluginId, descriptor.getVersion()));
            
            // 启动插件
            log.debug("启动插件{}", pluginId);
            if (!lifecycleManager.startPlugin(pluginId)) {
                throw new IllegalStateException("启动插件失败: " + pluginId);
            }
            
            // 发布插件启动事件
            log.debug("发布插件{}启动事件", pluginId);
            eventBus.postEvent(PluginEvent.createStartedEvent(pluginId, descriptor.getVersion()));
            
            log.info("{}插件{}处理完成", pluginTypeDesc, pluginId);
        } catch (Exception e) {
            log.error("{}插件{}处理异常: {}", isSystemPlugin ? "系统" : "普通", pluginId, e.getMessage(), e);
            // 记录失败状态
            stateManager.recordFailure(pluginId, e.getMessage());
        }
    }
    
    /**
     * 创建插件信息对象
     */
    private PluginInfo createPluginInfo(PluginDescriptor descriptor, ClassLoader classLoader, 
                                      Path jarPath, File pluginDir) {
        // 设置附加资源路径
        Map<String, String> resources = new HashMap<>();
        resources.put("configDir", new File(pluginDir, "config").getAbsolutePath());
        resources.put("dataDir", new File(pluginDir, "data").getAbsolutePath());
        resources.put("staticDir", new File(pluginDir, "static").getAbsolutePath());
        resources.put("templatesDir", new File(pluginDir, "templates").getAbsolutePath());
        
        // 使用Builder模式创建PluginInfo
        PluginInfo pluginInfo = PluginInfo.builder()
            .descriptor(descriptor)
            .classLoader((DynamicClassLoader)classLoader)
            .jarPath(jarPath)
            .state(PluginState.CREATED)
            .loadTime(new Date())
            .resourcePaths(resources)
            .build();
        
        return pluginInfo;
    }
    
    /**
     * 只注册插件信息，不加载
     */
    private void registerPluginInfo(PluginDescriptor descriptor, Path jarPath, File pluginDir) {
        String pluginId = descriptor.getPluginId();
        log.info("注册插件信息: {}", pluginId);
        
        // 创建插件信息对象
        PluginInfo pluginInfo = createPluginInfo(descriptor, null, jarPath, pluginDir);
        
        // 保存到数据库
        try {
            pluginStatePersistenceManager.savePluginInfo(pluginInfo);
            log.info("插件信息已保存到数据库: {}", pluginId);
        } catch (Exception e) {
            log.error("保存插件信息到数据库失败: {}", pluginId, e);
        }
    }
    
    /**
     * 更新现有插件
     */
    private void updateExistingPlugin(String pluginId, File jarFile, 
                                    PluginDescriptor descriptor, File pluginDir) {
        // 检查版本是否变化
        Optional<PluginInfo> existingPlugin = pluginRegistry.getPlugin(pluginId);
        if (existingPlugin.isEmpty()) {
            log.warn("插件信息不一致，无法找到: {}", pluginId);
            return;
        }
        
        String currentVersion = existingPlugin.get().getDescriptor().getVersion();
        String newVersion = descriptor.getVersion();
        
        if (!currentVersion.equals(newVersion)) {
            log.info("插件版本变更: {} -> {}", currentVersion, newVersion);
            // 处理版本升级
        }
        
        // 检查是否为系统插件
        boolean isSystemPlugin = "system".equals(descriptor.getType());
        boolean isTrusted = "trust".equals(descriptor.getTrust());
        
        if (isSystemPlugin && isTrusted) {
            // 系统插件需要重新加载
            reloadPlugin(pluginId);
        } else {
            // 普通插件只更新信息
            // updatePluginMetadata(pluginId, descriptor);
        }
    }
    
    /**
     * 尝试重新加载失败的插件
     */
    private void reloadFailedPlugins() {
        List<String> failedPlugins = stateManager.getFailedPlugins();
        if (failedPlugins.isEmpty()) {
            return;
        }
        
        log.info("尝试重新加载失败的插件: {}", failedPlugins);
        for (String pluginId : failedPlugins) {
            try {
                Optional<PluginInfo> optPluginInfo = pluginRegistry.getPlugin(pluginId);
                if (optPluginInfo.isPresent()) {
                    reloadPlugin(pluginId);
                }
            } catch (Exception e) {
                log.error("重新加载失败的插件异常: " + pluginId, e);
            }
        }
    }
    
    /**
     * 加载插件
     *
     * @param jarPath 插件JAR路径
     * @return 插件ID
     * @throws Exception 加载异常
     */
    public String loadPlugin(Path jarPath) throws Exception {
        log.info("开始加载插件: {}", jarPath);
        
        // 解析插件描述符
        PluginDescriptor descriptor = PluginFileUtils.parsePluginDescriptor(jarPath);
        if (descriptor == null) {
            throw new IllegalArgumentException("无法解析插件描述符: " + jarPath);
        }
        
        String pluginId = descriptor.getPluginId();
        
        // 检查插件是否已存在
        if (pluginRegistry.hasPlugin(pluginId)) {
            log.info("插件已存在，尝试卸载: {}", pluginId);
            unloadPlugin(pluginId);
        }
        
        // 创建类加载器
        DynamicClassLoader classLoader;
        try {
            classLoader = classLoaderFactory.createClassLoader(pluginId, jarPath.toFile());
        } catch (IOException e) {
            throw new IllegalStateException("创建插件类加载器失败: " + pluginId, e);
        }
        
        // 创建插件信息
        PluginInfo pluginInfo = PluginInfo.builder()
            .descriptor(descriptor)
            .classLoader(classLoader)
            .jarPath(jarPath)
            .state(PluginState.CREATED)
            .enabled(false)
            .loadTime(new Date())
            .build();
        
        // 注册插件
        if (!pluginRegistry.registerPlugin(pluginInfo)) {
            throw new IllegalStateException("注册插件失败: " + pluginId);
        }
        
        // 保存插件信息到数据库
        try {
            pluginStatePersistenceManager.savePluginInfo(pluginInfo);
            log.info("插件信息已保存到数据库: {}", pluginId);
        } catch (Exception e) {
            log.error("保存插件信息到数据库失败: {}", pluginId, e);
            // 由于已经注册成功，这里不回滚，继续执行
        }
        
        // 记录状态变化并发布事件
        stateManager.recordStateChange(pluginId, PluginState.CREATED);
        
        // 加载插件
        if (!lifecycleManager.loadPlugin(pluginInfo)) {
            throw new IllegalStateException("加载插件失败: " + pluginId);
        }
        
        // 发布插件加载事件
        eventBus.postEvent(PluginEvent.createLoadedEvent(pluginId, descriptor.getVersion()));
        
        // 初始化插件
        if (!lifecycleManager.initializePlugin(pluginId)) {
            throw new IllegalStateException("初始化插件失败: " + pluginId);
        }
        
        // 发布插件初始化事件
        eventBus.postEvent(PluginEvent.createInitializedEvent(pluginId, descriptor.getVersion()));
        
        // 启动插件
        if (!lifecycleManager.startPlugin(pluginId)) {
            throw new IllegalStateException("启动插件失败: " + pluginId);
        }
        
        // 发布插件启动事件
        eventBus.postEvent(PluginEvent.createStartedEvent(pluginId, descriptor.getVersion()));
        
        log.info("插件加载完成: {}", pluginId);
        return pluginId;
    }
    
    /**
     * 卸载插件
     *
     * @param pluginId 插件ID
     * @return 卸载是否成功
     */
    public boolean unloadPlugin(String pluginId) {
        try {
            log.info("开始卸载插件: {}", pluginId);
            
            if (!pluginRegistry.hasPlugin(pluginId)) {
                log.error("插件不存在: {}", pluginId);
                return false;
            }
            
            // 获取插件版本
            String version = pluginRegistry.getPlugin(pluginId)
                .map(info -> info.getDescriptor().getVersion())
                .orElse("unknown");
            
            // 检查依赖此插件的其他插件
            List<String> dependentPlugins = dependencyResolver.getDependentPlugins(pluginId);
            if (!dependentPlugins.isEmpty()) {
                log.error("无法卸载插件，有其他插件依赖它: {}, 依赖插件: {}", pluginId, dependentPlugins);
                return false;
            }
            
            // 卸载插件
            if (!lifecycleManager.unloadPlugin(pluginId)) {
                log.error("卸载插件失败: {}", pluginId);
                return false;
            }
            
            // 发布插件卸载事件
            eventBus.postEvent(PluginEvent.createUnloadedEvent(pluginId, version));
            
            // 从注册表中移除
            pluginRegistry.unregisterPlugin(pluginId);
            
            // 清除状态记录
            stateManager.clearStateRecord(pluginId);
            
            // 卸载插件后释放类加载器
            classLoaderFactory.releaseClassLoader(pluginId);
            
            log.info("插件卸载完成: {}", pluginId);
            return true;
        } catch (PluginLifecycleException e) {
            log.error("卸载插件异常: " + pluginId, e);
            return false;
        }
    }
    
    /**
     * 重新加载插件
     *
     * @param pluginId 插件ID
     * @return 重载是否成功
     */
    public boolean reloadPlugin(String pluginId) {
        try {
            log.info("开始重载插件: {}", pluginId);
            
            if (!pluginRegistry.hasPlugin(pluginId)) {
                log.error("插件不存在: {}", pluginId);
                return false;
            }
            
            // 获取插件JAR路径
            Optional<PluginInfo> optPluginInfo = pluginRegistry.getPlugin(pluginId);
            if (optPluginInfo.isEmpty()) {
                log.error("无法获取插件信息: {}", pluginId);
                return false;
            }
            
            Path jarPath = optPluginInfo.get().getJarPath();
            
            // 卸载插件
            if (!unloadPlugin(pluginId)) {
                log.error("卸载插件失败，无法重载: {}", pluginId);
                return false;
            }
            
            // 重新加载插件
            try {
                loadPlugin(jarPath);
                log.info("插件重载成功: {}", pluginId);
                return true;
            } catch (Exception e) {
                log.error("重载插件失败: " + pluginId, e);
                return false;
            }
        } catch (Exception e) {
            log.error("重载插件异常: " + pluginId, e);
            return false;
        }
    }
    
    /**
     * 安装插件
     *
     * @param jarFile 插件JAR文件
     * @return 安装是否成功
     */
    public boolean installPlugin(File jarFile) {
        try {
            log.info("开始安装插件: {}", jarFile.getName());
            
            // 验证文件
            if (!jarFile.exists() || !jarFile.isFile() || !jarFile.getName().endsWith(".jar")) {
                log.error("无效的插件文件: {}", jarFile.getAbsolutePath());
                return false;
            }
            
            // 解析插件描述符
            PluginDescriptor descriptor = PluginFileUtils.parsePluginDescriptor(jarFile.toPath());
            if (descriptor == null) {
                log.error("无法解析插件描述符: {}", jarFile.getAbsolutePath());
                return false;
            }
            
            // 复制插件到插件目录
            File targetFile = new File(pluginDir, jarFile.getName());
            try {
                PluginFileUtils.copyFile(jarFile, targetFile);
            } catch (IOException e) {
                log.error("复制插件文件失败: " + jarFile.getName(), e);
                return false;
            }
            
            // 加载插件
            try {
                loadPlugin(targetFile.toPath());
                log.info("插件安装成功: {}", descriptor.getPluginId());
                return true;
            } catch (Exception e) {
                log.error("加载插件失败: " + jarFile.getName(), e);
                
                // 清理文件
                if (targetFile.exists() && !targetFile.delete()) {
                    log.warn("无法删除插件文件: {}", targetFile.getAbsolutePath());
                }
                
                return false;
            }
        } catch (Exception e) {
            log.error("安装插件异常: " + jarFile.getName(), e);
            return false;
        }
    }
    
    /**
     * 卸载并删除插件
     *
     * @param pluginId 插件ID
     * @return 卸载是否成功
     */
    public boolean uninstallPlugin(String pluginId) {
        try {
            log.info("开始卸载并删除插件: {}", pluginId);
            
            if (!pluginRegistry.hasPlugin(pluginId)) {
                log.error("插件不存在: {}", pluginId);
                return false;
            }
            
            // 获取插件JAR路径
            Optional<PluginInfo> optPluginInfo = pluginRegistry.getPlugin(pluginId);
            if (optPluginInfo.isEmpty()) {
                log.error("无法获取插件信息: {}", pluginId);
                return false;
            }
            
            File jarFile = optPluginInfo.get().getJarPath().toFile();
            
            // 卸载插件
            if (!unloadPlugin(pluginId)) {
                log.error("卸载插件失败: {}", pluginId);
                return false;
            }
            
            // 删除插件文件
            if (jarFile.exists() && !jarFile.delete()) {
                log.warn("无法删除插件文件: {}", jarFile.getAbsolutePath());
            }
            
            log.info("插件卸载并删除成功: {}", pluginId);
            return true;
        } catch (Exception e) {
            log.error("卸载并删除插件异常: " + pluginId, e);
            return false;
        }
    }
    
    /**
     * 启用插件
     *
     * @param pluginId 插件ID
     * @return 启用是否成功
     */
    public boolean enablePlugin(String pluginId) {
        try {
            log.info("开始启用插件: {}", pluginId);
            
            if (!pluginRegistry.hasPlugin(pluginId)) {
                log.error("插件不存在: {}", pluginId);
                return false;
            }
            
            Optional<PluginInfo> optPluginInfo = pluginRegistry.getPlugin(pluginId);
            if (optPluginInfo.isEmpty()) {
                log.error("无法获取插件信息: {}", pluginId);
                return false;
            }
            
            PluginInfo pluginInfo = optPluginInfo.get();
            String version = pluginInfo.getDescriptor().getVersion();
            
            // 如果已启用，直接返回成功
            if (pluginInfo.isEnabled()) {
                log.info("插件已经是启用状态: {}", pluginId);
                return true;
            }
            
            // 插件状态检查
            PluginState state = pluginInfo.getState();
            log.info("插件[{}]当前状态: {}", pluginId, state);
            
            // 对于仅注册但未加载的普通插件（CREATED状态），需要完整加载、初始化和启动
            if (state == PluginState.CREATED) {
                log.info("插件处于CREATED状态，开始完整加载流程: {}", pluginId);
                
                // 标记为启用
                pluginInfo.setEnabled(true);
                pluginRegistry.updatePlugin(pluginInfo);
                
                // 创建类加载器（如果不存在）
                if (pluginInfo.getClassLoader() == null && pluginInfo.getJarPath() != null) {
                    try {
                        log.info("为插件[{}]创建类加载器...", pluginId);
                        DynamicClassLoader classLoader = classLoaderFactory.createClassLoader(pluginId, pluginInfo.getJarPath().toFile());
                        pluginInfo.setClassLoader(classLoader);
                        pluginRegistry.updatePlugin(pluginInfo);
                        log.info("插件[{}]类加载器创建成功", pluginId);
                    } catch (IOException e) {
                        log.error("创建插件类加载器失败: {}", pluginId, e);
                        return false;
                    }
                }
                
                // 加载插件 - 这一步会将状态从CREATED更新为LOADED
                try {
                    log.info("开始加载插件[{}]...", pluginId);
                    if (!lifecycleManager.loadPlugin(pluginInfo)) {
                        log.error("加载插件失败: {}", pluginId);
                        return false;
                    }
                    log.info("插件[{}]加载成功", pluginId);
                } catch (Exception e) {
                    log.error("加载插件[{}]时发生异常: {}", pluginId, e.getMessage(), e);
                    return false;
                }
                
                // 初始化插件 - 这一步会将状态从LOADED更新为INITIALIZED
                try {
                    log.info("开始初始化插件[{}]...", pluginId);
                    if (!lifecycleManager.initializePlugin(pluginId)) {
                        log.error("初始化插件失败: {}", pluginId);
                        return false;
                    }
                    log.info("插件[{}]初始化成功", pluginId);
                } catch (Exception e) {
                    log.error("初始化插件[{}]时发生异常: {}", pluginId, e.getMessage(), e);
                    return false;
                }
                
                // 启动插件 - 这一步会将状态从INITIALIZED更新为RUNNING
                try {
                    log.info("开始启动插件[{}]...", pluginId);
                    if (!lifecycleManager.startPlugin(pluginId)) {
                        log.error("启动插件失败: {}", pluginId);
                        return false;
                    }
                    log.info("插件[{}]启动成功", pluginId);
                } catch (Exception e) {
                    log.error("启动插件[{}]时发生异常: {}", pluginId, e.getMessage(), e);
                    return false;
                }
                
                // 发布启用事件
                log.info("发布插件[{}]启用事件", pluginId);
                eventBus.postEvent(PluginEvent.createEnabledEvent(pluginId, version));
                
                log.info("插件[{}]启用并完成全部加载流程", pluginId);
                return true;
            } else if (state == PluginState.STARTED || state == PluginState.RUNNING) {
                // 已运行，只需标记为启用
                pluginInfo.setEnabled(true);
                pluginRegistry.updatePlugin(pluginInfo);
                log.info("插件已标记为启用: {}", pluginId);
                
                // 发布启用事件
                eventBus.postEvent(PluginEvent.createEnabledEvent(pluginId, version));
                return true;
            } else if (state == PluginState.STOPPED) {
                // 已停止，需要启动
                // 标记为启用
                pluginInfo.setEnabled(true);
                pluginRegistry.updatePlugin(pluginInfo);
                
                if (!lifecycleManager.startPlugin(pluginId)) {
                    log.error("启动插件失败: {}", pluginId);
                    return false;
                }
                
                // 发布启用事件
                eventBus.postEvent(PluginEvent.createEnabledEvent(pluginId, version));
                log.info("插件已启用: {}", pluginId);
                return true;
            } else if (state == PluginState.LOADED) {
                log.info("插件处于LOADED状态，继续初始化和启动流程: {}", pluginId);
                
                // 标记为启用
                pluginInfo.setEnabled(true);
                pluginRegistry.updatePlugin(pluginInfo);
                
                // 插件已加载，继续初始化
                if (!lifecycleManager.initializePlugin(pluginId)) {
                    log.error("初始化插件失败: {}", pluginId);
                    return false;
                }
                
                // 启动插件
                if (!lifecycleManager.startPlugin(pluginId)) {
                    log.error("启动插件失败: {}", pluginId);
                    return false;
                }
                
                // 发布启用事件
                eventBus.postEvent(PluginEvent.createEnabledEvent(pluginId, version));
                log.info("插件[{}]启用并完成初始化和启动流程", pluginId);
                return true;
            } else if (state == PluginState.INITIALIZED) {
                log.info("插件处于INITIALIZED状态，继续启动流程: {}", pluginId);
                
                // 标记为启用
                pluginInfo.setEnabled(true);
                pluginRegistry.updatePlugin(pluginInfo);
                
                // 插件已初始化，直接启动
                if (!lifecycleManager.startPlugin(pluginId)) {
                    log.error("启动插件失败: {}", pluginId);
                    return false;
                }
                
                // 发布启用事件
                eventBus.postEvent(PluginEvent.createEnabledEvent(pluginId, version));
                log.info("插件[{}]启用并完成启动流程", pluginId);
                return true;
            } else {
                log.error("插件状态不正确，无法启用: {}, 当前状态: {}", pluginId, state);
                return false;
            }
        } catch (Exception e) {
            log.error("启用插件异常: " + pluginId, e);
            return false;
        }
    }
    
    /**
     * 禁用插件
     *
     * @param pluginId 插件ID
     * @return 禁用是否成功
     */
    public boolean disablePlugin(String pluginId) {
        try {
            log.info("开始禁用插件: {}", pluginId);
            
            if (!pluginRegistry.hasPlugin(pluginId)) {
                log.error("插件不存在: {}", pluginId);
                return false;
            }
            
            Optional<PluginInfo> optPluginInfo = pluginRegistry.getPlugin(pluginId);
            if (optPluginInfo.isEmpty()) {
                log.error("无法获取插件信息: {}", pluginId);
                return false;
            }
            
            PluginInfo pluginInfo = optPluginInfo.get();
            String version = pluginInfo.getDescriptor().getVersion();
            
            // 如果已禁用，直接返回成功
            if (!pluginInfo.isEnabled()) {
                log.info("插件已经是禁用状态: {}", pluginId);
                return true;
            }
            
            // 插件状态检查
            PluginState state = pluginInfo.getState();
            if (state == PluginState.STARTED) {
                // 正在运行，需要停止
                if (!lifecycleManager.stopPlugin(pluginId)) {
                    log.error("停止插件失败: {}", pluginId);
                    return false;
                }
                
                // 发布禁用事件
                eventBus.postEvent(PluginEvent.createDisabledEvent(pluginId, version));
                log.info("插件已禁用: {}", pluginId);
                return true;
            } else {
                // 其他状态，只需标记为禁用
                pluginInfo.setEnabled(false);
                pluginRegistry.updatePlugin(pluginInfo);
                
                // 发布禁用事件
                eventBus.postEvent(PluginEvent.createDisabledEvent(pluginId, version));
                log.info("插件已标记为禁用: {}", pluginId);
                return true;
            }
        } catch (Exception e) {
            log.error("禁用插件异常: " + pluginId, e);
            return false;
        }
    }
    
    /**
     * 获取所有插件信息
     *
     * @return 所有插件信息
     */
    public Collection<PluginInfo> getAllPlugins() {
        return pluginRegistry.getAllPlugins();
    }
    
    /**
     * 获取已启用的插件
     *
     * @return 已启用的插件
     */
    public Collection<PluginInfo> getEnabledPlugins() {
        return pluginRegistry.getPluginsByState(true);
    }
    
    /**
     * 获取已禁用的插件
     *
     * @return 已禁用的插件
     */
    public Collection<PluginInfo> getDisabledPlugins() {
        return pluginRegistry.getPluginsByState(false);
    }
    
    /**
     * 获取插件信息
     *
     * @param pluginId 插件ID
     * @return 插件信息
     */
    public Optional<PluginInfo> getPlugin(String pluginId) {
        return pluginRegistry.getPlugin(pluginId);
    }
    
    /**
     * 获取插件实例
     *
     * @param pluginId 插件ID
     * @return 插件实例
     */
    public Optional<Plugin> getPluginInstance(String pluginId) {
        return lifecycleManager.getPluginInstance(pluginId);
    }
    
    /**
     * 关闭插件系统
     */
    @PreDestroy
    public void shutdown() {
        log.info("关闭插件系统...");
        
        // 先中断所有执行任务
        executor.shutdownNow();
        
        // 然后再关闭文件监控
        if (pluginWatchService != null) {
            try {
                pluginWatchService.close();
            } catch (IOException e) {
                log.error("关闭文件监控服务异常", e);
            }
        }
        
        if (tempWatchService != null) {
            try {
                tempWatchService.close();
            } catch (IOException e) {
                log.error("关闭临时文件监控服务异常", e);
            }
        }
        
        // 发布系统关闭事件
        eventBus.postEvent(new SystemShutdownEvent(SystemShutdownEvent.ShutdownReason.NORMAL));
        
        // 卸载所有插件
        for (PluginInfo pluginInfo : pluginRegistry.getAllPlugins()) {
            try {
                String pluginId = pluginInfo.getDescriptor().getPluginId();
                unloadPlugin(pluginId);
            } catch (Exception e) {
                log.error("卸载插件异常", e);
            }
        }
        
        // 清空注册表
        pluginRegistry.clear();
        
        // 清空状态记录
        stateManager.clearAllStateRecords();
        
        // 释放所有类加载器
        classLoaderFactory.releaseAllClassLoaders();
        
        log.info("插件系统已关闭");
    }
    
 
} 