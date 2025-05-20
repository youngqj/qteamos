package com.xiaoqu.qteamos.core.plugin.web;


import com.xiaoqu.qteamos.common.utils.EncryptionUtils;
import com.xiaoqu.qteamos.core.plugin.manager.PluginRegistry;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import com.xiaoqu.qteamos.core.plugin.running.PluginState;
import com.xiaoqu.qteamos.core.plugin.manager.PluginStateManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;

import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;



/**
 * 插件请求映射处理器
 * 负责注册和管理插件中的Controller和RequestMapping
 *
 * @author yangqijun
 * @date 2024-07-19
 */
@Component
public class PluginRequestMappingHandlerMapping implements ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(PluginRequestMappingHandlerMapping.class);

    private ApplicationContext applicationContext;
    
    @Autowired
    private PluginRegistry pluginRegistry;

    @Value("${qteamos.gateway.api-prefix:/api}")
    private String apiPrefix;
    
    @Value("${qteamos.gateway.html-path-prefix:/html}")
    private String htmlPrefix;
    
    @Value("${qteamos.gateway.encrypt-plugin-id:true}")
    private boolean isEncryptPluginId;


    
    // 用于缓存加密后的插件ID，避免重复加密
    private final Map<String, String> encryptedPluginIdCache = new ConcurrentHashMap<>();
    
    // 用于缓存解密后的插件ID，避免重复解密
    private final Map<String, String> decryptedPluginIdCache = new ConcurrentHashMap<>();
    
    // 用于存储已注册的Controller映射
    private final Map<String, Set<RequestMappingInfo>> registeredMappings = new ConcurrentHashMap<>();
    
    /**
     * 设置应用上下文
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    /**
     * 在应用启动完成后触发控制器注册
     */
    @EventListener
    public void handleContextRefresh(ContextRefreshedEvent event) {
        // 只在Web应用上下文中进行处理
        if (event.getApplicationContext() instanceof WebApplicationContext) {
            log.info("收到ContextRefreshedEvent事件，ApplicationContext: {}", event.getApplicationContext().getDisplayName());
            
            // 在所有Bean初始化完成后再执行注册
            try {
                // 确保在根上下文刷新时执行，以确保所有Bean都已初始化
                if (event.getApplicationContext().equals(applicationContext) || 
                    event.getApplicationContext().getParent() == applicationContext ||
                    (applicationContext != null && applicationContext.equals(event.getApplicationContext().getParent()))) {
                    
                    // 修改延迟线程部分
                    Thread pluginRegistrationThread = new Thread(() -> {
                        try {
                            log.info("等待插件加载完成...");
                            // 不使用固定时间，而是使用轮询检查插件状态
                            int attempts = 0;
                            while (attempts < 10) {
                                Collection<PluginInfo> runningPlugins = pluginRegistry.getAllPlugins().stream()
                                    .filter(p -> p.getState() == PluginState.RUNNING)
                                    .toList();
                                    
                                if (!runningPlugins.isEmpty()) {
                                    log.info("发现{}个运行中的插件，开始注册控制器", runningPlugins.size());
                                    registerAllPluginControllers();
                                    break;
                                }
                                
                                log.info("尚未发现运行中的插件，等待500ms后重试...");
                                Thread.sleep(500);
                                attempts++;
                            }
                        } catch (Exception e) {
                            log.error("插件Controller注册失败", e);
                        }
                    }, "plugin-controller-registration");
                    
                    pluginRegistrationThread.setDaemon(true);
                    pluginRegistrationThread.start();
                    log.info("已启动插件控制器注册线程");
                }
            } catch (Exception e) {
                log.error("注册插件Controller失败", e);
            }
        }
    }
    
    /**
     * 监听插件状态变更事件，当插件进入RUNNING状态时注册其控制器，
     * 当插件状态变为STOPPED或UNLOADED时注销其控制器
     */
    @EventListener
    public void onPluginStateChange(PluginStateManager.PluginStateChangeEvent event) {
        try {
            String pluginId = event.getPluginId();
            PluginState newState = event.getNewState();
            
            if (newState == PluginState.RUNNING) {
                log.info("监听到插件[{}]状态变更为RUNNING，准备注册控制器", pluginId);
                
                pluginRegistry.getPlugin(pluginId).ifPresent(plugin -> {
                    try {
                        registerPluginControllers(plugin);
                    } catch (Exception e) {
                        log.error("注册插件[{}]控制器失败", pluginId, e);
                    }
                });
            } else if (newState == PluginState.STOPPED || newState == PluginState.UNLOADED) {
                log.info("监听到插件[{}]状态变更为{}，准备注销控制器", pluginId, newState);
                try {
                    unregisterPluginControllers(pluginId);
                } catch (Exception e) {
                    log.error("注销插件[{}]控制器失败", pluginId, e);
                }
            }
        } catch (Exception e) {
            log.error("处理插件状态变更事件失败", e);
        }
    }
    
    /**
     * 注册所有插件中的控制器
     */
    public void registerAllPluginControllers() {
        // 获取所有运行中的插件
        Collection<PluginInfo> plugins = pluginRegistry.getAllPlugins().stream()
                .filter(p -> p.getState() == PluginState.RUNNING)
                .toList();
        
        log.info("开始注册所有插件中的Controller，共有{}个插件", plugins.size());
        
        for (PluginInfo plugin : plugins) {
            try {
                registerPluginControllers(plugin);
            } catch (Exception e) {
                log.error("注册插件Controller失败: " + plugin.getPluginId(), e);
            }
        }
        
        // 在注册完成后，打印所有已注册的映射，用于调试
        if (applicationContext != null) {
            try {
                // 使用确切的bean名称避免NoUniqueBeanDefinitionException
                RequestMappingHandlerMapping handlerMapping = applicationContext.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
                Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();
                log.info("当前系统中所有注册的RequestMapping数量: {}", handlerMethods.size());
                
                // 只打印插件相关的映射，避免日志过多
                handlerMethods.forEach((info, method) -> {
                    if (method.getBeanType().getName().contains("plugin")) {
                        log.info("映射: {} -> {}.{}", 
                                info, method.getBeanType().getSimpleName(), method.getMethod().getName());
                    }
                });
            } catch (Exception e) {
                log.error("获取已注册映射失败", e);
            }
        }
    }
    
    /**
     * 注册单个插件的控制器
     *
     * @param plugin 插件信息
     */
    public void registerPluginControllers(PluginInfo plugin) {
        String pluginId = plugin.getPluginId();
        log.info("开始注册插件[{}]的Controller", pluginId);
        
        try {
            // 通过名称获取RequestMappingHandlerMapping
            RequestMappingHandlerMapping handlerMapping = applicationContext.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);

            if (handlerMapping == null) {
                log.error("无法获取RequestMappingHandlerMapping bean，插件控制器无法注册！");
                return;
            }

            // 获取插件类加载器
            ClassLoader pluginClassLoader = plugin.getClassLoader();
            if (pluginClassLoader == null) {
                log.error("插件类加载器为空: {}", pluginId);
                return;
            }
            
            // 在插件中查找所有的控制器类
            List<Class<?>> controllerClasses = findControllerClasses(plugin);
            
            log.info("在插件[{}]中找到{}个控制器类", pluginId, controllerClasses.size());
            
            // 注册每个控制器
            for (Class<?> controllerClass : controllerClasses) {
                registerControllerClass(handlerMapping, pluginId, controllerClass);
            }
            
            log.info("插件[{}]的Controller注册完成，共{}个", pluginId, controllerClasses.size());
        } catch (Exception e) {
            log.error("注册插件Controller时发生异常: " + pluginId, e);
        }
    }
    
    /**
     * 判断类是否属于插件控制器
     * 用于确保只处理插件控制器，不处理核心系统控制器
     * 
     * @param clazz 要判断的类
     * @return 如果是插件控制器返回true，否则返回false
     */
    private boolean isPluginController(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        
        // 通过包名判断是否是插件控制器
        String packageName = clazz.getPackage().getName();
        
        // 核心系统控制器通常在core包下，不应该由插件处理器处理
        boolean isCoreController = packageName.startsWith("com.xiaoqu.qteamos.core");
        
        // 检查是否在插件包中
        boolean isInPluginPackage = packageName.contains(".plugin.");
        
        // 日志记录
        if (isCoreController && isInPluginPackage) {
            log.warn("检测到在core包下的插件控制器: {} - 这可能是一个设计问题", clazz.getName());
        }
        
        return isInPluginPackage && !isCoreController;
    }
    
    /**
     * 查找插件中的所有控制器类
     *
     * @param plugin 插件信息
     * @return 控制器类列表
     */
    private List<Class<?>> findControllerClasses(PluginInfo plugin) {
        List<Class<?>> controllerClasses = new ArrayList<>();
        
        try {
            // 获取插件类加载器
            ClassLoader pluginClassLoader = plugin.getClassLoader();
            if (pluginClassLoader == null) {
                log.error("插件类加载器为空: {}", plugin.getPluginId());
                return controllerClasses;
            }
            
            // 1. 首先从插件元数据中获取显式声明的控制器
            List<String> declaredControllers = getControllerClassNamesFromPlugin(plugin);
            for (String className : declaredControllers) {
                try {
                    Class<?> clazz = pluginClassLoader.loadClass(className);
                    // 增加插件控制器筛选
                    if (isPluginController(clazz)) {
                        controllerClasses.add(clazz);
                    } else {
                        log.info("跳过非插件控制器: {}", clazz.getName());
                    }
                } catch (ClassNotFoundException e) {
                    log.warn("找不到插件声明的控制器类: {}", className);
                }
            }
            
            // 2. 如果插件提供了扫描方法，使用它来获取控制器
            if (plugin.getPluginInstance() != null) {
                try {
                    Method getControllersMethod = plugin.getPluginInstance().getClass()
                            .getDeclaredMethod("getControllerClasses");
                    if (getControllersMethod != null) {
                        getControllersMethod.setAccessible(true);
                        Object result = getControllersMethod.invoke(plugin.getPluginInstance());
                        if (result instanceof List) {
                            List<?> controllers = (List<?>) result;
                            for (Object controller : controllers) {
                                if (controller instanceof Class) {
                                    Class<?> clazz = (Class<?>) controller;
                                    // 增加插件控制器筛选
                                    if (isPluginController(clazz)) {
                                        controllerClasses.add(clazz);
                                    } else {
                                        log.info("跳过非插件控制器: {}", clazz.getName());
                                    }
                                } else if (controller instanceof String) {
                                    try {
                                        Class<?> clazz = pluginClassLoader.loadClass((String) controller);
                                        // 增加插件控制器筛选
                                        if (isPluginController(clazz)) {
                                            controllerClasses.add(clazz);
                                        } else {
                                            log.info("跳过非插件控制器: {}", clazz.getName());
                                        }
                                    } catch (ClassNotFoundException e) {
                                        log.warn("找不到插件提供的控制器类: {}", controller);
                                    }
                                }
                            }
                        }
                    }
                } catch (NoSuchMethodException e) {
                    // 插件没有提供此方法，忽略
                } catch (Exception e) {
                    log.warn("调用插件的getControllerClasses方法失败: {}", e.getMessage());
                }
            }
            
            // 3. 如果还没有找到控制器，尝试扫描插件包
            if (controllerClasses.isEmpty() && plugin.getDescriptor().getMainClass() != null) {
                // 获取插件主类的包名作为扫描基础
                String mainClassName = plugin.getDescriptor().getMainClass();
                String basePackage = mainClassName.substring(0, mainClassName.lastIndexOf('.'));
                
                // 扫描插件包中的控制器类
                Set<Class<?>> foundControllers = scanControllersInPackage(pluginClassLoader, basePackage);
                
                // 过滤非插件控制器
                for (Class<?> clazz : foundControllers) {
                    if (isPluginController(clazz)) {
                        controllerClasses.add(clazz);
                    } else {
                        log.info("扫描时跳过非插件控制器: {}", clazz.getName());
                    }
                }
                
                log.info("在包{}中扫描到{}个符合条件的插件控制器类", basePackage, controllerClasses.size());
            }
            
            log.info("在插件[{}]中找到{}个符合条件的控制器类", plugin.getPluginId(), controllerClasses.size());
        } catch (Exception e) {
            log.error("查找插件控制器类失败: " + plugin.getPluginId(), e);
        }
        
        return controllerClasses;
    }
    
    /**
     * 从插件中获取控制器类名列表
     * 通过以下策略获取：
     * 1. 从插件描述符的controllers配置项中直接获取
     * 2. 基于插件主类所在包的约定获取
     */
    @SuppressWarnings("unchecked")
    private List<String> getControllerClassNamesFromPlugin(PluginInfo plugin) {
        List<String> controllerClassNames = new ArrayList<>();
        
        try {
            // 1. 尝试从插件描述符的metadata中获取控制器列表
        Map<String, Object> metadata = plugin.getDescriptor().getMetadata();
        if (metadata != null && metadata.containsKey("controllers")) {
            Object controllers = metadata.get("controllers");
            if (controllers instanceof List) {
                return (List<String>) controllers;
                } else if (controllers instanceof String) {
                    // 支持以逗号分隔的字符串格式
                    String controllersStr = (String) controllers;
                    return Arrays.asList(controllersStr.split(","));
                }
            }
            
            // 2. 如果没有明确配置，获取插件主类包名，采用通配符模式
            String mainClassName = plugin.getDescriptor().getMainClass();
            if (mainClassName != null && !mainClassName.isEmpty()) {
                String basePackage = mainClassName.substring(0, mainClassName.lastIndexOf('.'));
                
                // 为整个包下的所有类添加通配符模式，不预先指定包名模式
                // 由类加载器在加载后进行过滤
                controllerClassNames.add(basePackage + ".**");
                
                log.info("插件[{}]未明确配置controllers，将扫描整个包: {}", 
                        plugin.getPluginId(), basePackage);
            }
            
            // 3. 如果插件有实现getControllerPackages方法
            if (plugin.getPluginInstance() != null) {
                try {
                    Method getPackagesMethod = plugin.getPluginInstance().getClass()
                            .getDeclaredMethod("getControllerPackages");
                    if (getPackagesMethod != null) {
                        getPackagesMethod.setAccessible(true);
                        Object result = getPackagesMethod.invoke(plugin.getPluginInstance());
                        if (result instanceof List) {
                            List<String> packages = (List<String>) result;
                            for (String pkg : packages) {
                                // 为每个包添加通配符
                                controllerClassNames.add(pkg + ".**");
                            }
                        } else if (result instanceof String) {
                            String pkg = (String) result;
                            controllerClassNames.add(pkg + ".**");
                        }
                    }
                } catch (NoSuchMethodException e) {
                    // 插件没有实现此方法，忽略
                } catch (Exception e) {
                    log.warn("调用插件的getControllerPackages方法失败: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("获取插件控制器类名列表失败: {}", e.getMessage(), e);
        }
        
        return controllerClassNames;
    }
    
    /**
     * 注册控制器类
     *
     * @param handlerMapping Spring MVC的处理器映射
     * @param pluginId 插件ID
     * @param controllerClass 控制器类
     */
    private void registerControllerClass(RequestMappingHandlerMapping handlerMapping, 
                                        String pluginId, Class<?> controllerClass) {
        try {
            // 确认是插件控制器
            if (!isPluginController(controllerClass)) {
                log.warn("尝试注册非插件控制器: {} - 已跳过", controllerClass.getName());
                return;
            }
            
            // 创建控制器实例
            Object controllerInstance = createControllerInstance(controllerClass);
            
            // 获取所有带有@RequestMapping的方法
            Method[] methods = controllerClass.getMethods();
            Set<RequestMappingInfo> mappingInfos = new HashSet<>();
            
            for (Method method : methods) {
                RequestMapping methodMapping = AnnotationUtils.findAnnotation(method, RequestMapping.class);
                if (methodMapping != null) {
                    // 创建RequestMappingInfo
                    RequestMappingInfo mappingInfo = createMappingInfo(pluginId, controllerClass, method);
                    
                    // 注册Handler
                    registerHandler(handlerMapping, mappingInfo, controllerInstance, method);
                    
                    // 发布API注册事件，让关注此事件的模块（如网关）可以处理
                    publishApiRegistrationEvent(pluginId, mappingInfo, method);
                    
                    // 记录已注册的映射
                    mappingInfos.add(mappingInfo);
                }
            }
            
            // 保存插件控制器映射，用于卸载时注销
            registeredMappings.put(pluginId + ":" + controllerClass.getName(), mappingInfos);
            
            log.info("注册插件控制器: {}.{}", pluginId, controllerClass.getSimpleName());
        } catch (Exception e) {
            log.error("注册控制器类失败: " + controllerClass.getName(), e);
        }
    }
    
    /**
     * 发布API注册事件
     * 让关注API注册的其他模块（如网关）可以获取API信息
     */
    private void publishApiRegistrationEvent(String pluginId, RequestMappingInfo mappingInfo, Method method) {
        if (applicationContext != null) {
            try {
                // 创建事件对象，包含必要信息
                PluginApiRegistrationEvent event = new PluginApiRegistrationEvent(
                    this, pluginId, mappingInfo, method.getDeclaringClass(), method);
                
                // 发布事件
                applicationContext.publishEvent(event);
            } catch (Exception e) {
                log.warn("发布API注册事件失败: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 创建控制器实例
     * 支持依赖注入
     */
    private Object createControllerInstance(Class<?> controllerClass) throws Exception {
        // 1. 首先尝试从Spring容器中获取
        try {
            // 检查Spring容器中是否已存在此Bean
            String beanName = controllerClass.getSimpleName().substring(0, 1).toLowerCase() + 
                            controllerClass.getSimpleName().substring(1);
            if (applicationContext.containsBean(beanName)) {
                return applicationContext.getBean(beanName);
            }
        } catch (Exception e) {
            // 忽略异常，继续尝试其他方式
        }
        
        // 2. 使用Spring的BeanUtils创建实例并注入依赖
        try {
            // 创建实例
            Object instance = controllerClass.getDeclaredConstructor().newInstance();
            
            // 自动注入依赖
            org.springframework.beans.factory.config.AutowireCapableBeanFactory factory = 
                    applicationContext.getAutowireCapableBeanFactory();
            factory.autowireBean(instance);
            factory.initializeBean(instance, controllerClass.getName());
            
            return instance;
        } catch (Exception e) {
            log.warn("使用Spring注入依赖失败: {}", e.getMessage());
            // 失败则使用简单实例化
            return controllerClass.getDeclaredConstructor().newInstance();
        }
    }
    
    /**
     * 加密插件ID
     * 用于在URL中隐藏真实的插件ID
     * 
     * @param pluginId 原始插件ID
     * @return 加密后的插件ID
     */
    private String encryptPluginId(String pluginId) {
        if (pluginId == null || pluginId.isEmpty() || !isEncryptPluginId) {
            return pluginId;
        }
        
        // 检查缓存中是否已有此插件ID的加密结果
        return encryptedPluginIdCache.computeIfAbsent(pluginId, id -> {
            try {
                // 使用工具类加密，加密后的字符串可能包含特殊字符，需要进行URL编码
                String encrypted = EncryptionUtils.encrypt(id);
                
                // 对特殊字符进行替换，确保URL安全
                // 去除可能的等号，避免URL解析问题
                encrypted = encrypted.replace("=", "");
                // 替换斜杠为下划线
                encrypted = encrypted.replace("/", "_");
                // 替换加号为减号
                encrypted = encrypted.replace("+", "-");
                
                log.debug("插件ID加密: {} -> {}", id, encrypted);
                return encrypted;
            } catch (Exception e) {
                log.error("插件ID加密失败: {}, 使用原始ID", e.getMessage());
                return id;
            }
        });
    }
    
    /**
     * 解密插件ID
     * 用于从URL中的加密ID恢复真实的插件ID
     * 
     * @param encryptedId 加密后的插件ID
     * @return 解密后的原始插件ID
     */
    public String decryptPluginId(String encryptedId) {
        if (encryptedId == null || encryptedId.isEmpty() || !isEncryptPluginId) {
            return encryptedId;
        }
        
        // 检查缓存中是否已有此加密ID的解密结果
        return decryptedPluginIdCache.computeIfAbsent(encryptedId, id -> {
            try {
                // 还原URL安全字符替换
                String restored = id;
                // 将下划线替换回斜杠
                restored = restored.replace("_", "/");
                // 将减号替换回加号
                restored = restored.replace("-", "+");
                
                // 使用工具类解密
                String decrypted = EncryptionUtils.decrypt(restored);
                
                log.debug("插件ID解密: {} -> {}", id, decrypted);
                return decrypted;
            } catch (Exception e) {
                log.error("插件ID解密失败: {}, 使用原始ID", e.getMessage());
                return id;
            }
        });
    }
    
    /**
     * 创建RequestMappingInfo
     */
    private RequestMappingInfo createMappingInfo(String pluginId, Class<?> controllerClass, Method method) {
        log.info("为插件[{}]创建RequestMappingInfo, 类: {}, 方法: {}", pluginId, controllerClass.getName(), method.getName());
        
        // 获取类和方法上的RequestMapping注解
        RequestMapping classMapping = AnnotationUtils.findAnnotation(controllerClass, RequestMapping.class);
        
        // 直接尝试从方法中提取所有可能的路径信息
        String[] methodPaths = null;
        org.springframework.web.bind.annotation.RequestMethod[] requestMethods = null;
        String[] consumes = null;
        String[] produces = null;
        String[] headers = null;
        String[] params = null;
        
        // 尝试不同类型的注解 - 直接使用Spring的注解类型，避免类加载器问题
        try {
            // 1. 尝试GetMapping
            org.springframework.web.bind.annotation.GetMapping getMapping = 
                AnnotationUtils.findAnnotation(method, org.springframework.web.bind.annotation.GetMapping.class);
            if (getMapping != null) {
                methodPaths = getMapping.value().length > 0 ? getMapping.value() : getMapping.path();
                requestMethods = new org.springframework.web.bind.annotation.RequestMethod[]{org.springframework.web.bind.annotation.RequestMethod.GET};
                consumes = getMapping.consumes();
                produces = getMapping.produces();
                headers = getMapping.headers();
                params = getMapping.params();
                log.info("找到@GetMapping注解，路径: {}", Arrays.toString(methodPaths));
            }
            
            // 2. 尝试PostMapping
            if (methodPaths == null) {
                org.springframework.web.bind.annotation.PostMapping postMapping = 
                    AnnotationUtils.findAnnotation(method, org.springframework.web.bind.annotation.PostMapping.class);
                if (postMapping != null) {
                    methodPaths = postMapping.value().length > 0 ? postMapping.value() : postMapping.path();
                    requestMethods = new org.springframework.web.bind.annotation.RequestMethod[]{org.springframework.web.bind.annotation.RequestMethod.POST};
                    consumes = postMapping.consumes();
                    produces = postMapping.produces();
                    headers = postMapping.headers();
                    params = postMapping.params();
                    log.info("找到@PostMapping注解，路径: {}", Arrays.toString(methodPaths));
                }
            }
            
            // 3. 尝试PutMapping
            if (methodPaths == null) {
                org.springframework.web.bind.annotation.PutMapping putMapping = 
                    AnnotationUtils.findAnnotation(method, org.springframework.web.bind.annotation.PutMapping.class);
                if (putMapping != null) {
                    methodPaths = putMapping.value().length > 0 ? putMapping.value() : putMapping.path();
                    requestMethods = new org.springframework.web.bind.annotation.RequestMethod[]{org.springframework.web.bind.annotation.RequestMethod.PUT};
                    consumes = putMapping.consumes();
                    produces = putMapping.produces();
                    headers = putMapping.headers();
                    params = putMapping.params();
                    log.info("找到@PutMapping注解，路径: {}", Arrays.toString(methodPaths));
                }
            }
            
            // 4. 尝试DeleteMapping
            if (methodPaths == null) {
                org.springframework.web.bind.annotation.DeleteMapping deleteMapping = 
                    AnnotationUtils.findAnnotation(method, org.springframework.web.bind.annotation.DeleteMapping.class);
                if (deleteMapping != null) {
                    methodPaths = deleteMapping.value().length > 0 ? deleteMapping.value() : deleteMapping.path();
                    requestMethods = new org.springframework.web.bind.annotation.RequestMethod[]{org.springframework.web.bind.annotation.RequestMethod.DELETE};
                    consumes = deleteMapping.consumes();
                    produces = deleteMapping.produces();
                    headers = deleteMapping.headers();
                    params = deleteMapping.params();
                    log.info("找到@DeleteMapping注解，路径: {}", Arrays.toString(methodPaths));
                }
            }
            
            // 5. 尝试PatchMapping
            if (methodPaths == null) {
                org.springframework.web.bind.annotation.PatchMapping patchMapping = 
                    AnnotationUtils.findAnnotation(method, org.springframework.web.bind.annotation.PatchMapping.class);
                if (patchMapping != null) {
                    methodPaths = patchMapping.value().length > 0 ? patchMapping.value() : patchMapping.path();
                    requestMethods = new org.springframework.web.bind.annotation.RequestMethod[]{org.springframework.web.bind.annotation.RequestMethod.PATCH};
                    consumes = patchMapping.consumes();
                    produces = patchMapping.produces();
                    headers = patchMapping.headers();
                    params = patchMapping.params();
                    log.info("找到@PatchMapping注解，路径: {}", Arrays.toString(methodPaths));
                }
            }
            
            // 6. 最后尝试RequestMapping (已包含所有HTTP方法)
            if (methodPaths == null) {
                RequestMapping methodMapping = AnnotationUtils.findAnnotation(method, RequestMapping.class);
                if (methodMapping != null) {
                    methodPaths = methodMapping.value().length > 0 ? methodMapping.value() : methodMapping.path();
                    requestMethods = methodMapping.method();
                    consumes = methodMapping.consumes();
                    produces = methodMapping.produces();
                    headers = methodMapping.headers();
                    params = methodMapping.params();
                    log.info("找到@RequestMapping注解，路径: {}", Arrays.toString(methodPaths));
                }
            }
        } catch (Exception e) {
            log.error("提取注解信息时出错: {}", e.getMessage(), e);
        }
        
        // 如果仍然未找到路径，使用方法名作为默认路径
        if (methodPaths == null || methodPaths.length == 0) {
            methodPaths = new String[]{method.getName()};
            log.info("未找到任何路径注解，使用方法名[{}]作为路径", method.getName());
        }
        
        // 使用RequestMappingInfo.Builder创建RequestMappingInfo
        RequestMappingInfo.Builder builder = RequestMappingInfo.paths();
        
        // 判断是RestController还是普通Controller
        boolean isRestController = AnnotationUtils.findAnnotation(controllerClass, RestController.class) != null;

        // 根据控制器类型选择前缀
        String prefix = isRestController ? apiPrefix : htmlPrefix;
        
        // 构建插件基础路径，使用加密后的插件ID
        String encryptedId = encryptPluginId(pluginId);
        String pluginBasePath = prefix.endsWith("/") 
            ? prefix + "p-" + encryptedId 
            : prefix + "/p-" + encryptedId;
        
        log.debug("插件[{}]基础路径: {}, 加密前缀: {}, 加密后前缀: {}, 加密已{}",
                pluginId, pluginBasePath, prefix + "/p-" + pluginId, 
                prefix + "/p-" + encryptedId,  isEncryptPluginId ? "启用" : "禁用");
        
        // 处理类级别的路径，去掉类级别RequestMapping中可能包含的/p-前缀
        String[] classPaths = new String[]{""};
        if (classMapping != null && classMapping.value().length > 0) {
            classPaths = classMapping.value();
            
            // 检查类路径是否已经包含了plugin前缀
            for (int i = 0; i < classPaths.length; i++) {
                String path = classPaths[i];
                
                // 如果路径包含/p-pluginId，去掉这部分，避免重复
                if (path.contains("/p-" + pluginId)) {
                    path = path.replace("/p-" + pluginId, "");
                }
                
                // 如果路径以/p-开头，去掉这部分，避免路径冲突
                if (path.startsWith("/p-")) {
                    path = path.substring(3); // 去掉/p-
                }
                
                // 如果路径重复包含了/api前缀，去掉
                if (path.startsWith(apiPrefix)) {
                    path = path.substring(apiPrefix.length());
                }
                
                classPaths[i] = path;
            }
        }
        
        log.info("处理后的类路径: {}", Arrays.toString(classPaths));
        log.info("方法原始路径: {}", Arrays.toString(methodPaths));
        
        // 组合所有可能的路径组合
        List<String> combinedPaths = new ArrayList<>();
        for (String classPath : classPaths) {
            for (String methodPath : methodPaths) {
                // 确保方法路径不为空
                if (methodPath == null || methodPath.isEmpty()) {
                    methodPath = method.getName();
                    log.info("方法路径为空，使用方法名[{}]作为路径", method.getName());
                }
                
                // 使用改进的combinePath方法保留完整路径结构
                String fullPath = combinePath(pluginBasePath, classPath, methodPath);
                combinedPaths.add(fullPath);
                
                // 同时记录日志，帮助调试
                log.info("为插件[{}]创建路径映射: {}", pluginId, fullPath);
            }
        }
        
        // 设置路径
        builder = builder.paths(combinedPaths.toArray(new String[0]));
        
        // 设置其他RequestMapping属性
        if (requestMethods != null && requestMethods.length > 0) {
            builder = builder.methods(requestMethods);
        }
        
        if (params != null && params.length > 0) {
            builder = builder.params(params);
        }
        
        if (headers != null && headers.length > 0) {
            builder = builder.headers(headers);
        }
        
        if (consumes != null && consumes.length > 0) {
            builder = builder.consumes(consumes);
        }
        
        if (produces != null && produces.length > 0) {
            builder = builder.produces(produces);
        }
        
        // 创建最终的RequestMappingInfo
        RequestMappingInfo mappingInfo = builder.build();
        log.info("为插件[{}]创建RequestMappingInfo: {}", pluginId, mappingInfo);
        
        return mappingInfo;
    }
    
    /**
     * 组合路径，保留完整的路径结构
     */
    private String combinePath(String basePath, String classPath, String methodPath) {
        StringBuilder pathBuilder = new StringBuilder(basePath);
        
        // 添加类路径
        if (classPath != null && !classPath.isEmpty()) {
            if (!classPath.startsWith("/") && !basePath.endsWith("/")) {
                pathBuilder.append("/");
            }
            pathBuilder.append(classPath);
        }
        
        // 添加方法路径 - 关键是将methodPath视为不可分割的整体
        if (methodPath != null && !methodPath.isEmpty()) {
            // 处理methodPath开头的斜杠情况
            if (methodPath.startsWith("/")) {
                // 如果methodPath以/开头，确保不会出现双斜杠
                if (pathBuilder.toString().endsWith("/")) {
                    // 如果路径已经以/结尾，去掉methodPath开头的/
                    methodPath = methodPath.substring(1);
                }
                // 否则保留methodPath开头的/
            } else {
                // 如果methodPath不以/开头，确保有一个连接斜杠
                if (!pathBuilder.toString().endsWith("/")) {
                    pathBuilder.append("/");
                }
            }
            
            // 保持methodPath的完整性，不要拆分或假设它是单级路径
            pathBuilder.append(methodPath);
        }
        
        // 规范化路径，处理可能的双斜杠问题
        String path = pathBuilder.toString().replaceAll("//+", "/");
        
        log.info("合并路径: 基础={}, 类={}, 方法={}, 结果={}", basePath, classPath, methodPath, path);
        
        return path;
    }
    
    /**
     * 注册请求处理器
     */
    private void registerHandler(RequestMappingHandlerMapping handlerMapping, 
                               RequestMappingInfo mappingInfo, Object handler, Method method) {
        try {
            // 再次确认是插件控制器
            if (!isPluginController(handler.getClass())) {
                log.warn("尝试注册非插件控制器处理器: {}.{} - 已跳过", 
                       handler.getClass().getName(), method.getName());
                return;
            }
            
            log.info("注册插件请求处理器: {} -> {}.{}", 
                    mappingInfo, handler.getClass().getName(), method.getName());
            
            // 使用反射调用registerHandlerMethod方法
            Method registerMethod = RequestMappingHandlerMapping.class.getDeclaredMethod(
                    "registerHandlerMethod", Object.class, Method.class, Object.class);
            registerMethod.setAccessible(true);
            registerMethod.invoke(handlerMapping, handler, method, mappingInfo);
            
            log.info("成功注册插件请求映射: {} -> {}.{}", 
                     mappingInfo, handler.getClass().getSimpleName(), method.getName());
        } catch (Exception e) {
            log.error("注册请求处理器失败: {} -> {}.{}, 错误: {}", 
                     mappingInfo, handler.getClass().getSimpleName(), method.getName(), e.getMessage(), e);
        }
    }
    
    /**
     * 卸载插件控制器
     *
     * @param pluginId 插件ID
     */
    public void unregisterPluginControllers(String pluginId) {
        log.info("开始卸载插件[{}]的Controller", pluginId);
        
        try {
            // 获取主RequestMappingHandlerMapping，使用明确的bean名称避免NoUniqueBeanDefinitionException
            RequestMappingHandlerMapping handlerMapping = applicationContext.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
            
            // 获取此插件注册的所有控制器映射
            Set<String> keysToRemove = new HashSet<>();
            
            for (String key : registeredMappings.keySet()) {
                if (key.startsWith(pluginId + ":")) {
                    // 卸载每个映射
                    Set<RequestMappingInfo> mappings = registeredMappings.get(key);
                    for (RequestMappingInfo mapping : mappings) {
                        // 发布API注销事件
                        publishApiUnregistrationEvent(pluginId, mapping);
                        
                        // 注销处理器
                        unregisterHandler(handlerMapping, mapping);
                    }
                    
                    keysToRemove.add(key);
                }
            }
            
            // 移除记录
            for (String key : keysToRemove) {
                registeredMappings.remove(key);
            }
            
            log.info("插件[{}]的Controller卸载完成", pluginId);
        } catch (Exception e) {
            log.error("卸载插件Controller时发生异常: " + pluginId, e);
        }
    }
    
    /**
     * 发布API注销事件
     */
    private void publishApiUnregistrationEvent(String pluginId, RequestMappingInfo mappingInfo) {
        if (applicationContext != null) {
            try {
                // 创建事件对象
                PluginApiUnregistrationEvent event = new PluginApiUnregistrationEvent(
                    this, pluginId, mappingInfo);
                
                // 发布事件
                applicationContext.publishEvent(event);
            } catch (Exception e) {
                log.warn("发布API注销事件失败: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 注销Handler
     */
    private void unregisterHandler(RequestMappingHandlerMapping handlerMapping, RequestMappingInfo mappingInfo) {
        try {
            // 使用反射调用unregisterMapping方法
            Method unregisterMethod = RequestMappingHandlerMapping.class.getDeclaredMethod(
                    "unregisterMapping", RequestMappingInfo.class);
            unregisterMethod.setAccessible(true);
            unregisterMethod.invoke(handlerMapping, mappingInfo);
            
            log.debug("注销请求映射: {}", mappingInfo);
        } catch (Exception e) {
            log.error("注销请求处理器失败", e);
        }
    }
    
    /**
     * 插件API注册事件
     * 用于通知其他模块（如网关）有新的API注册
     */
    public static class PluginApiRegistrationEvent extends org.springframework.context.ApplicationEvent {
        private final String pluginId;
        private final RequestMappingInfo mappingInfo;
        private final Class<?> controllerClass;
        private final Method method;
        
        public PluginApiRegistrationEvent(Object source, String pluginId, 
                                        RequestMappingInfo mappingInfo, 
                                        Class<?> controllerClass, Method method) {
            super(source);
            this.pluginId = pluginId;
            this.mappingInfo = mappingInfo;
            this.controllerClass = controllerClass;
            this.method = method;
        }
        
        public String getPluginId() { return pluginId; }
        public RequestMappingInfo getMappingInfo() { return mappingInfo; }
        public Class<?> getControllerClass() { return controllerClass; }
        public Method getMethod() { return method; }
    }
    
    /**
     * 插件API注销事件
     * 用于通知其他模块（如网关）有API被注销
     */
    public static class PluginApiUnregistrationEvent extends org.springframework.context.ApplicationEvent {
        private final String pluginId;
        private final RequestMappingInfo mappingInfo;
        
        public PluginApiUnregistrationEvent(Object source, String pluginId, RequestMappingInfo mappingInfo) {
            super(source);
            this.pluginId = pluginId;
            this.mappingInfo = mappingInfo;
        }
        
        public String getPluginId() { return pluginId; }
        public RequestMappingInfo getMappingInfo() { return mappingInfo; }
    }
    
    /**
     * 扫描指定包中的控制器类
     * 查找带有@Controller或@RestController注解的类
     * 
     * @param classLoader 类加载器
     * @param basePackage 基础包名
     * @return 找到的控制器类集合
     */
    private Set<Class<?>> scanControllersInPackage(ClassLoader classLoader, String basePackage) {
        Set<Class<?>> controllerClasses = new HashSet<>();
        
        try {
            // 检查是否有Reflections库
            try {
                Class.forName("org.reflections.Reflections");
                // 如果有Reflections库，使用它进行扫描
                return scanWithReflections(classLoader, basePackage);
            } catch (ClassNotFoundException e) {
                // 如果没有Reflections库，使用自定义实现
                log.info("未找到Reflections库，使用自定义扫描逻辑");
            }
            
            // 自定义扫描实现
            // 1. 将包名转换为路径
            String path = basePackage.replace('.', '/');
            
            // 2. 获取类路径下的所有资源
            Enumeration<java.net.URL> resources = classLoader.getResources(path);
            
            // 3. 遍历找到的资源URL
            while (resources.hasMoreElements()) {
                java.net.URL resource = resources.nextElement();
                
                // 处理文件系统中的资源
                if (resource.getProtocol().equals("file")) {
                    scanClassesInDirectory(new java.io.File(resource.toURI()), basePackage, classLoader, controllerClasses);
                }
                // 处理JAR包中的资源
                else if (resource.getProtocol().equals("jar")) {
                    scanClassesInJar(resource, path, basePackage, classLoader, controllerClasses);
                }
            }
        } catch (Exception e) {
            log.error("扫描控制器类失败", e);
        }
        
        return controllerClasses;
    }
    
    /**
     * 使用Reflections库扫描控制器
     */
    private Set<Class<?>> scanWithReflections(ClassLoader classLoader, String basePackage) {
        Set<Class<?>> controllerClasses = new HashSet<>();
        
        try {
            // 动态创建Reflections实例并调用方法
            Class<?> reflectionsClass = Class.forName("org.reflections.Reflections");
            Object reflections = reflectionsClass.getConstructor(String.class, ClassLoader.class)
                    .newInstance(basePackage, classLoader);
            
            // 扫描@RestController注解
            Method getTypesAnnotatedWith = reflectionsClass.getMethod("getTypesAnnotatedWith", Class.class);
            Set<?> restControllers = (Set<?>) getTypesAnnotatedWith.invoke(
                    reflections, Class.forName("org.springframework.web.bind.annotation.RestController"));
            for (Object clazz : restControllers) {
                controllerClasses.add((Class<?>) clazz);
            }
            
            // 扫描@Controller注解
            Set<?> controllers = (Set<?>) getTypesAnnotatedWith.invoke(
                    reflections, Class.forName("org.springframework.stereotype.Controller"));
            for (Object clazz : controllers) {
                controllerClasses.add((Class<?>) clazz);
            }
        } catch (Exception e) {
            log.error("使用Reflections扫描失败", e);
        }
        
        return controllerClasses;
    }
    
    /**
     * 扫描目录中的类文件
     */
    private void scanClassesInDirectory(java.io.File directory, String packageName, 
                                      ClassLoader classLoader, Set<Class<?>> controllerClasses) {
        if (!directory.exists()) {
            return;
        }
        
        // 获取目录中的所有文件
        java.io.File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        
        for (java.io.File file : files) {
            String fileName = file.getName();
            
            // 如果是目录，递归扫描
            if (file.isDirectory()) {
                scanClassesInDirectory(file, packageName + "." + fileName, classLoader, controllerClasses);
            } 
            // 如果是类文件
            else if (fileName.endsWith(".class")) {
                try {
                    // 构建类名
                    String className = packageName + "." + fileName.substring(0, fileName.length() - 6);
                    
                    // 加载类
                    Class<?> clazz = classLoader.loadClass(className);
                    
                    // 检查是否有控制器注解
                    if (isControllerClass(clazz)) {
                        controllerClasses.add(clazz);
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    // 忽略找不到的类
                    log.debug("加载类失败: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * 扫描JAR包中的类
     */
    private void scanClassesInJar(java.net.URL jarUrl, String path, String packageName, 
                                ClassLoader classLoader, Set<Class<?>> controllerClasses) {
        try {
            String jarPath = jarUrl.getPath().substring(5, jarUrl.getPath().indexOf("!"));
            try (java.util.jar.JarFile jar = new java.util.jar.JarFile(java.net.URLDecoder.decode(jarPath, "UTF-8"))) {
                // 遍历JAR包中的所有条目
                java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    java.util.jar.JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    
                    // 检查是否属于指定包，并且是类文件
                    if (entryName.startsWith(path) && entryName.endsWith(".class")) {
                        // 将路径转换为类名
                        String className = entryName.substring(0, entryName.length() - 6).replace('/', '.');
                        
                        try {
                            // 加载类
                            Class<?> clazz = classLoader.loadClass(className);
                            
                            // 检查是否有控制器注解
                            if (isControllerClass(clazz)) {
                                controllerClasses.add(clazz);
                            }
                        } catch (ClassNotFoundException | NoClassDefFoundError e) {
                            // 忽略找不到的类
                            log.debug("加载类失败: {}", e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("扫描JAR包失败", e);
        }
    }
    
    /**
     * 检查类是否是控制器
     */
    private boolean isControllerClass(Class<?> clazz) {
        try {
            // 检查类上是否有@Controller或@RestController注解
            boolean isController = AnnotationUtils.findAnnotation(clazz, org.springframework.stereotype.Controller.class) != null;
            boolean isRestController = AnnotationUtils.findAnnotation(clazz, RestController.class) != null;
            
            if (isRestController || isController) {
                log.debug("找到控制器类: {}，RestController={}, Controller={}", 
                         clazz.getName(), isRestController, isController);
                return true;
            }
            
            // 还可以检查类是否实现特定接口或符合其他控制器条件
            return false;
        } catch (Throwable t) {
            // 处理类加载问题
            log.debug("检查控制器类失败: {} - {}", clazz.getName(), t.getMessage());
            return false;
        }
    }
} 