package com.xiaoqu.qteamos.core.plugin.loader;

import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import com.xiaoqu.qteamos.core.plugin.loader.ClassLoadingException;
import com.xiaoqu.qteamos.core.plugin.loader.ClassLoadingException.ErrorType;

/**
 * 动态类加载器
 * 用于加载插件类和资源，支持类隔离和内存泄漏防护
 * 
 * @author yangqijun
 * @version 1.1.0
 */
@Slf4j
public class DynamicClassLoader extends URLClassLoader {
    
    /**
     * 插件ID
     */
    private final String pluginId;
    
    /**
     * 类加载器配置
     */
    private final ClassLoaderConfiguration configuration;
    
    /**
     * 类缓存，避免重复加载
     */
    private final Map<String, Class<?>> loadedClassCache = new ConcurrentHashMap<>();
    
    /**
     * 预加载类缓存
     */
    private final Map<String, SoftReference<Class<?>>> preloadedClassCache = new ConcurrentHashMap<>();
    
    /**
     * 类加载计数器，用于监控类加载活动
     */
    private final AtomicInteger classLoadCount = new AtomicInteger(0);
    
    /**
     * 资源加载计数器，用于监控资源加载活动
     */
    private final AtomicInteger resourceLoadCount = new AtomicInteger(0);
    
    /**
     * 类加载时间统计(毫秒)
     */
    private final Map<String, Long> classLoadingTimes = new ConcurrentHashMap<>();
    
    /**
     * 标记类加载器是否已关闭
     */
    private volatile boolean closed = false;
    
    /**
     * JAR文件集合，用于在关闭时释放资源
     */
    private final Set<JarFile> jarFiles = Collections.synchronizedSet(new HashSet<>());
    
    /**
     * JAR文件路径映射，用于追踪JAR文件来源
     */
    private final Map<String, Path> jarPathMapping = new ConcurrentHashMap<>();
    
    /**
     * 需要在关闭时释放的资源集合
     */
    private final Set<WeakReference<Closeable>> closeableResources = 
            Collections.synchronizedSet(new HashSet<>());
    
    /**
     * 资源使用计数器，用于检测资源泄漏
     */
    private final Map<String, AtomicInteger> resourceUsageCounter = new ConcurrentHashMap<>();
    
    /**
     * 类加载冲突记录
     */
    private final Set<String> conflictedClasses = Collections.synchronizedSet(new HashSet<>());
    
    /**
     * 创建时间
     */
    private final long creationTime = System.currentTimeMillis();
    
    /**
     * 最后使用时间
     */
    private volatile long lastUsedTime = System.currentTimeMillis();
    
    /**
     * 构造函数
     * 
     * @param pluginId 插件ID
     * @param urls 类路径URL数组
     * @param parent 父类加载器
     * @param configuration 类加载器配置
     */
    public DynamicClassLoader(String pluginId, URL[] urls, ClassLoader parent, 
                             ClassLoaderConfiguration configuration) {
        super(urls, parent);
        this.pluginId = pluginId;
        this.configuration = configuration != null ? 
                configuration : new ClassLoaderConfiguration();
        
        // 注册JVM关闭钩子，确保资源释放
        registerShutdownHook();
        
        log.debug("创建插件[{}]类加载器，使用策略: {}", pluginId, this.configuration.getStrategy());
    }
    
    /**
     * 注册JVM关闭钩子
     */
    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!closed) {
                log.info("JVM关闭，释放插件[{}]类加载器资源", pluginId);
                try {
                    close();
                } catch (IOException e) {
                    log.error("关闭类加载器时出错: {}", e.getMessage());
                }
            }
        }));
    }
    
    /**
     * 添加一个URL到类路径
     * 
     * @param url 要添加的URL
     */
    @Override
    public void addURL(URL url) {
        super.addURL(url);
        updateLastUsedTime();
        log.debug("插件[{}]类加载器添加URL: {}", pluginId, url);
    }
    
    /**
     * 添加JAR文件到类加载器
     * 
     * @param jarFile JAR文件
     * @throws ClassLoadingException 如果添加失败
     */
    public void addJarFile(File jarFile) throws ClassLoadingException {
        if (closed) {
            throw new ClassLoadingException(
                "ClassLoader已关闭，无法添加JAR文件: " + jarFile.getName(), 
                ErrorType.CLASSLOADER_CLOSED
            );
        }
        
        try {
            JarFile jar = new JarFile(jarFile);
            jarFiles.add(jar);
            URL jarUrl = jarFile.toURI().toURL();
            addURL(jarUrl);
            jarPathMapping.put(jarFile.getName(), jarFile.toPath());
            
            // 如果配置了类预加载，则扫描和预加载常用类
            if (configuration.isClassCachingEnabled()) {
                preloadCommonClasses(jar);
            }
            
            log.debug("插件[{}]类加载器添加JAR文件: {}", pluginId, jarFile.getAbsolutePath());
        } catch (IOException e) {
            throw new ClassLoadingException(
                "添加JAR文件失败: " + jarFile.getName(), 
                e, 
                ErrorType.CLASS_LOADING_FAILED
            );
        }
    }
    
    /**
     * 预加载常用类
     * 
     * @param jar JAR文件
     */
    private void preloadCommonClasses(JarFile jar) {
        try {
            // 预加载一些常用的类，如服务接口等
            List<String> commonClassPatterns = Arrays.asList(
                "**/*Service.class", 
                "**/*Controller.class", 
                "**/*Repository.class"
            );
            
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    String className = entry.getName().replace("/", ".")
                            .substring(0, entry.getName().length() - 6);
                    
                    // 判断是否是常用类
                    boolean isCommonClass = commonClassPatterns.stream()
                            .anyMatch(pattern -> matchWildcard(className, pattern));
                    
                    if (isCommonClass) {
                        try {
                            Class<?> clazz = loadClass(className, false);
                            preloadedClassCache.put(className, new SoftReference<>(clazz));
                            log.debug("插件[{}]预加载类: {}", pluginId, className);
                        } catch (Exception e) {
                            log.debug("插件[{}]预加载类失败: {}, 原因: {}", 
                                    pluginId, className, e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("预加载插件[{}]JAR文件类失败: {}", pluginId, e.getMessage());
        }
    }
    
    /**
     * 通配符匹配
     * 
     * @param text 要匹配的文本
     * @param pattern 通配符模式
     * @return 是否匹配
     */
    private boolean matchWildcard(String text, String pattern) {
        String[] cards = pattern.split("\\*");
        int idx = 0;
        
        for (String card : cards) {
            idx = text.indexOf(card, idx);
            if (idx == -1) {
                return false;
            }
            idx += card.length();
        }
        
        return true;
    }
    
    /**
     * 注册一个需要在类加载器关闭时释放的资源
     * 
     * @param closeable 可关闭的资源
     * @param resourceName 资源名称，用于跟踪
     */
    public void registerCloseable(Closeable closeable, String resourceName) {
        if (closeable == null) {
            return;
        }
        
        closeableResources.add(new WeakReference<>(closeable));
        
        // 记录资源使用
        if (resourceName != null) {
            resourceUsageCounter.computeIfAbsent(resourceName, k -> new AtomicInteger(0))
                .incrementAndGet();
        }
        
        log.debug("插件[{}]注册可关闭资源: {}", pluginId, resourceName != null ? resourceName : closeable.getClass().getName());
    }
    
    /**
     * 注册资源使用
     * 
     * @param resourceName 资源名称
     */
    public void registerResourceUsage(String resourceName) {
        if (resourceName != null) {
            resourceUsageCounter.computeIfAbsent(resourceName, k -> new AtomicInteger(0))
                .incrementAndGet();
            log.debug("插件[{}]注册资源使用: {}", pluginId, resourceName);
        }
    }
    
    /**
     * 注销资源使用
     * 
     * @param resourceName 资源名称
     */
    public void unregisterResourceUsage(String resourceName) {
        if (resourceName != null) {
            AtomicInteger counter = resourceUsageCounter.get(resourceName);
            if (counter != null && counter.decrementAndGet() <= 0) {
                resourceUsageCounter.remove(resourceName);
            }
            log.debug("插件[{}]注销资源使用: {}", pluginId, resourceName);
        }
    }
    
    /**
     * 获取资源使用信息
     * 
     * @return 资源使用计数映射
     */
    public Map<String, Integer> getResourceUsage() {
        return resourceUsageCounter.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
    }
    
    /**
     * 检查某个类是否被阻止加载（安全检查）
     * 
     * @param className 类名
     * @return 如果被阻止加载返回true，否则返回false
     */
    private boolean isClassBlocked(String className) {
        // 允许java基础包下的所有类，这些是安全的
        if (className.startsWith("java.lang.") || 
            className.startsWith("java.util.") || 
            className.startsWith("java.io.") || 
            className.startsWith("java.time.") || 
            className.startsWith("java.math.") ||
            className.startsWith("java.text.") ||
            className.startsWith("java.net.") ||
            className.startsWith("java.nio.") ||
            className.startsWith("org.slf4j.")) {
            return false;
        }
        
        // 只阻止可能导致安全问题的Java内部实现类
        if ((className.startsWith("sun.") && !className.startsWith("sun.reflect.")) ||
            (className.startsWith("com.sun.") && !className.startsWith("com.sun.proxy.")) ||
            className.startsWith("jdk.internal.") ||
            className.startsWith("java.lang.invoke.MethodHandleImpl")) {
            log.debug("阻止加载Java内部实现类: {}", className);
            return true;
        }
        
        // 检查自定义的黑名单
        String[] blockedPrefixes = {"org.springframework.boot.loader", "com.xiaoqu.qteamos.core.plugin.internal"};
        for (String blockedPrefix : blockedPrefixes) {
            if (className.startsWith(blockedPrefix)) {
                log.debug("阻止加载受限类: {}", className);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 加载类
     * 
     * @param name 类名
     * @param resolve 是否解析类
     * @return 加载的类
     * @throws ClassNotFoundException 类未找到
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (closed) {
            throw new ClassNotFoundException("无法加载类：类加载器已关闭");
        }
        
        updateLastUsedTime();
        classLoadCount.incrementAndGet();
        
        // 首先检查类名是否合法
        if (name == null || name.isEmpty()) {
            throw new ClassNotFoundException("类名不能为空");
        }
        
        // 检查此类是否被阻止加载（安全检查）
        if (isClassBlocked(name)) {
            log.warn("插件[{}]尝试加载被阻止的类: {}", pluginId, name);
            throw new ClassNotFoundException("类 " + name + " 被阻止加载，可能存在安全风险");
        }
        
        // 首先从缓存中查找
        Class<?> loadedClass = loadedClassCache.get(name);
        if (loadedClass != null) {
            if (resolve) {
                resolveClass(loadedClass);
            }
            return loadedClass;
        }
        
        // 检查是否已加载
        try {
            loadedClass = findLoadedClass(name);
            if (loadedClass != null) {
                if (resolve) {
                    resolveClass(loadedClass);
                }
                loadedClassCache.put(name, loadedClass);
                return loadedClass;
            }
        } catch (Exception e) {
            log.error("检查已加载类时出错: {}", e.getMessage());
            throw new ClassNotFoundException("检查已加载类时出错: " + name, e);
        }
        
        // 记录开始时间，用于计算加载时间
        long startTime = System.currentTimeMillis();
        
        try {
            // 判断是否应该由父类加载器加载
            if (shouldLoadFromParent(name)) {
                try {
                    loadedClass = getParent().loadClass(name);
                    if (loadedClass != null) {
                        if (resolve) {
                            resolveClass(loadedClass);
                        }
                        loadedClassCache.put(name, loadedClass);
                        return loadedClass;
                    }
                } catch (ClassNotFoundException e) {
                    // 父类加载器找不到类，继续由本类加载器加载
                }
            }
            
            try {
                // 尝试由自己加载
                loadedClass = findClass(name);
                if (loadedClass != null) {
                    // 检查是否存在类加载冲突
                    checkClassConflict(name, loadedClass);
                    
                    if (resolve) {
                        resolveClass(loadedClass);
                    }
                    
                    loadedClassCache.put(name, loadedClass);
                    return loadedClass;
                }
            } catch (ClassNotFoundException e) {
                // 本类加载器找不到类，尝试由父类加载器加载（如果之前没尝试过）
                if (!shouldLoadFromParent(name)) {
                    try {
                        loadedClass = getParent().loadClass(name);
                        if (loadedClass != null) {
                            if (resolve) {
                                resolveClass(loadedClass);
                            }
                            loadedClassCache.put(name, loadedClass);
                            return loadedClass;
                        }
                    } catch (ClassNotFoundException ex) {
                        // 所有方式都找不到类，记录详细错误并抛出标准异常
                        log.warn("找不到类: {}，类加载路径搜索失败", name);
                        throw new ClassNotFoundException("找不到类: " + name, e);
                    }
                } else {
                    // 两种方式都尝试过了，仍然找不到类
                    log.warn("无法加载类: {}, 父类加载器和当前类加载器都无法找到", name);
                    throw new ClassNotFoundException("找不到类: " + name, e);
                }
            }
        } catch (Error | RuntimeException e) {
            // 其他任何未预期的异常
            log.error("加载类时发生未预期的错误: {}", e.getMessage());
            throw new ClassNotFoundException("加载类时发生未预期的错误: " + name, e);
        } finally {
            // 记录类加载时间
            long loadTime = System.currentTimeMillis() - startTime;
            classLoadingTimes.put(name, loadTime);
            if (loadTime > 1000) {
                log.warn("插件[{}]加载类耗时过长: {} - {}ms", pluginId, name, loadTime);
            }
        }
        
        // 应该不会执行到这里，但为了安全起见
        throw new ClassNotFoundException("无法加载类: " + name);
    }
    
    /**
     * 检查类冲突
     * 
     * @param className 类名
     * @param loadedClass 加载的类
     */
    private void checkClassConflict(String className, Class<?> loadedClass) {
        try {
            // 获取类的来源
            ProtectionDomain protectionDomain = loadedClass.getProtectionDomain();
            if (protectionDomain != null) {
                CodeSource codeSource = protectionDomain.getCodeSource();
                if (codeSource != null) {
                    // 检查父加载器中是否有同名类
                    try {
                        Class<?> parentClass = getParent().loadClass(className);
                        if (parentClass != loadedClass) {
                            CodeSource parentCodeSource = parentClass.getProtectionDomain().getCodeSource();
                            if (parentCodeSource != null && !codeSource.equals(parentCodeSource)) {
                                log.warn("插件[{}]类冲突: {} 同时存在于 {} 和 {}", 
                                        pluginId, className, 
                                        codeSource.getLocation(), 
                                        parentCodeSource.getLocation());
                                conflictedClasses.add(className);
                            }
                        }
                    } catch (ClassNotFoundException ignored) {
                        // 父加载器中没有此类，不存在冲突
                    }
                }
            }
        } catch (Exception e) {
            log.debug("检查类冲突时出错: {}", e.getMessage());
        }
    }
    
    /**
     * 判断是否应该从父加载器加载类
     * 
     * @param className 类名
     * @return 是否应该从父加载器加载
     */
    private boolean shouldLoadFromParent(String className) {
        // 如果类名为空，返回true
        if (className == null || className.isEmpty()) {
            return true;
        }
        
        // 检查是否是共享包
        if (configuration.isShared(className)) {
            return true;
        }
        
        // 检查是否是隔离包
        if (configuration.isIsolated(className)) {
            return false;
        }
        
        // 检查包优先级
        int pluginPriority = configuration.getPackagePriority(className);
        if (pluginPriority > 0) {
            return false; // 优先从插件加载
        }
        
        // 根据配置的策略决定
        return configuration.getStrategy() == ClassLoadingStrategy.PARENT_FIRST;
    }
    
    /**
     * 获取资源
     * 
     * @param name 资源名称
     * @return 资源URL
     */
    @Override
    public URL getResource(String name) {
        if (closed) {
            log.warn("类加载器已关闭，无法获取资源: {}", name);
            return null;
        }
        
        updateLastUsedTime();
        resourceLoadCount.incrementAndGet();
        registerResourceUsage(name);
        
        try {
            // 尝试由自己加载
            URL url = findResource(name);
            if (url != null) {
                return url;
            }
            
            // 尝试由父类加载器加载
            return getParent().getResource(name);
        } catch (Exception e) {
            log.error("获取资源时出错: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 封装资源获取异常
     * 获取资源并处理可能的异常，转换为ClassLoadingException
     * 
     * @param name 资源名称
     * @return 资源URL
     * @throws ClassLoadingException 如果类加载器已关闭或发生其他错误
     */
    public URL getResourceWithExceptions(String name) throws ClassLoadingException {
        if (closed) {
            throw new ClassLoadingException(
                "无法获取资源：类加载器已关闭", 
                ErrorType.CLASSLOADER_CLOSED
            );
        }
        
        URL url = getResource(name);
        if (url == null) {
            throw new ClassLoadingException(
                "找不到资源: " + name,
                ErrorType.CLASS_NOT_FOUND
            );
        }
        
        return url;
    }
    
    /**
     * 获取资源流
     * 
     * @param name 资源名称
     * @return 资源输入流
     */
    @Override
    public InputStream getResourceAsStream(String name) {
        if (closed) {
            log.warn("类加载器已关闭，无法获取资源流: {}", name);
            return null;
        }
        
        updateLastUsedTime();
        resourceLoadCount.incrementAndGet();
        
        try {
            URL url = getResource(name);
            if (url != null) {
                InputStream is = url.openStream();
                if (is != null) {
                    registerResourceUsage(name);
                    return new ResourceTrackingInputStream(is, name);
                }
            }
            return null;
        } catch (IOException e) {
            log.error("获取资源流时出错: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 封装资源流获取异常
     * 获取资源流并处理可能的异常，转换为ClassLoadingException
     * 
     * @param name 资源名称
     * @return 资源输入流
     * @throws ClassLoadingException 如果类加载器已关闭或发生其他错误
     */
    public InputStream getResourceAsStreamWithExceptions(String name) throws ClassLoadingException {
        if (closed) {
            throw new ClassLoadingException(
                "无法获取资源流：类加载器已关闭", 
                ErrorType.CLASSLOADER_CLOSED
            );
        }
        
        InputStream is = getResourceAsStream(name);
        if (is == null) {
            throw new ClassLoadingException(
                "找不到资源流: " + name,
                ErrorType.CLASS_NOT_FOUND
            );
        }
        
        return is;
    }
    
    /**
     * 资源跟踪输入流，自动管理资源使用计数
     */
    private class ResourceTrackingInputStream extends InputStream {
        private final InputStream delegate;
        private final String resourceName;
        private boolean closed = false;
        
        public ResourceTrackingInputStream(InputStream delegate, String resourceName) {
            this.delegate = delegate;
            this.resourceName = resourceName;
        }
        
        @Override
        public int read() throws IOException {
            return delegate.read();
        }
        
        @Override
        public int read(byte[] b) throws IOException {
            return delegate.read(b);
        }
        
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return delegate.read(b, off, len);
        }
        
        @Override
        public long skip(long n) throws IOException {
            return delegate.skip(n);
        }
        
        @Override
        public int available() throws IOException {
            return delegate.available();
        }
        
        @Override
        public void close() throws IOException {
            if (!closed) {
                delegate.close();
                unregisterResourceUsage(resourceName);
                closed = true;
            }
        }
        
        @Override
        public void mark(int readlimit) {
            delegate.mark(readlimit);
        }
        
        @Override
        public void reset() throws IOException {
            delegate.reset();
        }
        
        @Override
        public boolean markSupported() {
            return delegate.markSupported();
        }
    }
    
    /**
     * 关闭可关闭资源
     * 
     * @param exceptions 异常集合，用于收集关闭过程中的异常
     */
    private void closeRegularResources(Set<Exception> exceptions) {
        // 关闭所有注册的资源
        List<Closeable> resourcesToClose = new ArrayList<>();
        for (WeakReference<Closeable> ref : closeableResources) {
            Closeable closeable = ref.get();
            if (closeable != null) {
                resourcesToClose.add(closeable);
            }
        }
        
        // 先清除集合，避免在关闭过程中发生异常导致重复关闭
        closeableResources.clear();
        
        // 关闭收集到的资源
        for (Closeable closeable : resourcesToClose) {
            try {
                closeable.close();
            } catch (IOException e) {
                exceptions.add(e);
                log.warn("关闭资源时出错: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 清理类加载器可能的内存泄漏
     */
    private void cleanClassLoaderLeak() {
        if (configuration.isMemoryLeakProtectionEnabled()) {
            System.gc();
            log.debug("触发垃圾回收以帮助清理类加载器泄漏");
        }
    }
    
    /**
     * 关闭类加载器
     * 
     * @throws IOException IO异常
     */
    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        
        synchronized (this) {
            if (closed) {
                return;
            }
            
            closed = true;
            log.info("正在关闭插件[{}]类加载器，已加载{}个类", pluginId, loadedClassCache.size());
            
            try {
                // 关闭所有资源
                Set<Exception> exceptions = new HashSet<>();
                
                // 关闭JAR文件
                for (JarFile jarFile : jarFiles) {
                    try {
                        jarFile.close();
                    } catch (IOException e) {
                        exceptions.add(e);
                        log.error("关闭JAR文件时出错: {}", e.getMessage());
                    }
                }
                jarFiles.clear();
                
                // 关闭可关闭资源
                closeRegularResources(exceptions);
                
                // 清理内部缓存
                clearInternalCaches();
                
                // 尝试清理类加载器泄漏
                cleanClassLoaderLeak();
                
                if (!exceptions.isEmpty()) {
                    // 记录所有异常但只抛出第一个
                    IOException firstEx = new IOException("关闭类加载器资源时发生异常");
                    firstEx.initCause(exceptions.iterator().next());
                    throw firstEx;
                }
            } catch (Exception e) {
                if (e instanceof IOException) {
                    throw (IOException) e;
                } else {
                    IOException wrappedEx = new IOException("关闭类加载器时发生未预期的错误");
                    wrappedEx.initCause(e);
                    throw wrappedEx;
                }
            } finally {
                log.info("插件[{}]类加载器已关闭", pluginId);
                super.close();
            }
        }
    }
    
    /**
     * 封装关闭异常
     * 关闭类加载器并处理可能的异常，转换为ClassLoadingException
     * 
     * @throws ClassLoadingException 如果关闭过程中发生错误
     */
    public void closeWithExceptions() throws ClassLoadingException {
        try {
            close();
        } catch (IOException e) {
            throw new ClassLoadingException(
                "关闭类加载器资源时发生异常", 
                e, 
                ErrorType.CLASSLOADER_CLOSED
            );
        } catch (Exception e) {
            throw new ClassLoadingException(
                "关闭类加载器时发生未预期的错误", 
                e, 
                ErrorType.CLASSLOADER_CLOSED
            );
        }
    }
    
    /**
     * 清理URLClassLoader内部缓存
     */
    private void clearInternalCaches() {
        try {
            // 尝试清理URLClassLoader中的ucp缓存
            Field ucpField = URLClassLoader.class.getDeclaredField("ucp");
            ucpField.setAccessible(true);
            Object ucp = ucpField.get(this);
            
            // 清理URLClassPath中的缓存
            if (ucp != null) {
                // 清理loaders缓存
                Field loadersField = ucp.getClass().getDeclaredField("loaders");
                loadersField.setAccessible(true);
                Object loaders = loadersField.get(ucp);
                if (loaders instanceof Collection) {
                    ((Collection<?>) loaders).clear();
                }
                
                // 清理lmap缓存
                Field lmapField = ucp.getClass().getDeclaredField("lmap");
                lmapField.setAccessible(true);
                Object lmap = lmapField.get(ucp);
                if (lmap instanceof Map) {
                    ((Map<?, ?>) lmap).clear();
                }
                
                // 清理其他可能的缓存字段
                try {
                    Field pathField = ucp.getClass().getDeclaredField("path");
                    pathField.setAccessible(true);
                    Object path = pathField.get(ucp);
                    if (path instanceof Collection) {
                        ((Collection<?>) path).clear();
                    }
                } catch (NoSuchFieldException ignored) {}
            }
        } catch (Exception e) {
            log.debug("清理类加载器内部缓存失败: {}", e.getMessage());
        }
    }
    
    /**
     * 获取所有已加载的类
     * 
     * @return 已加载的类集合
     */
    public Set<Class<?>> getLoadedClasses() {
        updateLastUsedTime();
        return new HashSet<>(loadedClassCache.values());
    }
    
    /**
     * 获取类加载冲突信息
     * 
     * @return 冲突的类名集合
     */
    public Set<String> getConflictedClasses() {
        return new HashSet<>(conflictedClasses);
    }
    
    /**
     * 获取插件ID
     * 
     * @return 插件ID
     */
    public String getPluginId() {
        return pluginId;
    }
    
    /**
     * 判断类加载器是否已关闭
     * 
     * @return 是否已关闭
     */
    public boolean isClosed() {
        return closed;
    }
    
    /**
     * 获取当前类加载器的URL数组
     * 
     * @return URL数组
     */
    @Override
    public URL[] getURLs() {
        updateLastUsedTime();
        return super.getURLs();
    }
    
    /**
     * 获取加载的类数量
     * 
     * @return 加载的类数量
     */
    public int getLoadedClassCount() {
        return loadedClassCache.size();
    }
    
    /**
     * 获取类加载次数
     * 
     * @return 类加载次数
     */
    public int getClassLoadCount() {
        return classLoadCount.get();
    }
    
    /**
     * 获取资源加载次数
     * 
     * @return 资源加载次数
     */
    public int getResourceLoadCount() {
        return resourceLoadCount.get();
    }
    
    /**
     * 获取类加载时间统计
     * 
     * @return 类加载时间统计(类名 -> 加载时间(毫秒))
     */
    public Map<String, Long> getClassLoadingTimes() {
        return new HashMap<>(classLoadingTimes);
    }
    
    /**
     * 获取创建时间
     * 
     * @return 创建时间
     */
    public long getCreationTime() {
        return creationTime;
    }
    
    /**
     * 获取最后使用时间
     * 
     * @return 最后使用时间
     */
    public long getLastUsedTime() {
        return lastUsedTime;
    }
    
    /**
     * 更新最后使用时间
     */
    private void updateLastUsedTime() {
        lastUsedTime = System.currentTimeMillis();
    }
    
    /**
     * 获取类加载器的运行时间
     * 
     * @return 运行时间(毫秒)
     */
    public long getUptime() {
        return System.currentTimeMillis() - creationTime;
    }
    
    /**
     * 获取加载器的空闲时间
     * 
     * @return 空闲时间(毫秒)
     */
    public long getIdleTime() {
        return System.currentTimeMillis() - lastUsedTime;
    }
    
    /**
     * 获取加载器的内存使用估计
     * 
     * @return 内存使用估计（字节）
     */
    public long getEstimatedMemoryUsage() {
        // 简单估算，实际内存使用需要更复杂的计算
        return loadedClassCache.size() * 50 * 1024; // 假设每个类占用约50KB
    }
    
    @Override
    public String toString() {
        return "DynamicClassLoader[pluginId=" + pluginId + 
               ", strategy=" + configuration.getStrategy() + 
               ", loadedClasses=" + loadedClassCache.size() + 
               ", resourceLoads=" + resourceLoadCount.get() +
               ", uptime=" + getUptime() / 1000 + "s" +
               ", closed=" + closed + "]";
    }
    
    /**
     * 封装ClassLoadingException处理
     * 在加载类时捕获常规异常并转换为ClassLoadingException
     * 
     * @param name 类名
     * @param resolve 是否解析类
     * @return 加载的类
     * @throws ClassLoadingException 类加载异常
     */
    public Class<?> loadClassWithExceptions(String name, boolean resolve) throws ClassLoadingException {
        try {
            return loadClass(name, resolve);
        } catch (ClassNotFoundException e) {
            throw new ClassLoadingException(
                "找不到类: " + name, 
                e,
                ErrorType.CLASS_NOT_FOUND
            );
        } catch (Exception e) {
            throw new ClassLoadingException(
                "加载类时出错: " + name, 
                e,
                ErrorType.CLASS_LOADING_FAILED
            );
        }
    }
} 