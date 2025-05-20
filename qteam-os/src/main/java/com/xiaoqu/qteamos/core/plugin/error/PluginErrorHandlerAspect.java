package com.xiaoqu.qteamos.core.plugin.error;

import com.xiaoqu.qteamos.core.plugin.service.PluginServiceApiImpl;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 插件错误处理切面
 * 用于在调用插件方法时自动捕获异常并处理
 *
 * @author yangqijun
 * @date 2025-05-01
 */
@Aspect
@Component
public class PluginErrorHandlerAspect {

    private static final Logger log = LoggerFactory.getLogger(PluginErrorHandlerAspect.class);

    @Autowired
    private PluginErrorHandler errorHandler;

    @Autowired
    private PluginServiceApiImpl pluginServiceApi;

    /**
     * 定义切点 - 所有插件实现类的方法
     */
    @Pointcut("execution(* com.xiaoqu.qteamos.api.core.Plugin+.*(..))")
    public void pluginMethodExecution() {
    }

    /**
     * 环绕通知 - 捕获插件方法执行过程中的异常并处理
     *
     * @param joinPoint 连接点
     * @return 方法执行结果
     * @throws Throwable 执行异常
     */
    @Around("pluginMethodExecution()")
    public Object handlePluginMethodError(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String pluginId = "";
        
        try {
            pluginId = getCurrentPluginId(joinPoint);
            
            // 方法执行前记录
            log.debug("执行插件[{}]方法: {}", pluginId, methodName);
            
            // 执行原方法
            return joinPoint.proceed();
        } catch (Throwable e) {
            // 处理异常
            log.error("插件[{}]方法[{}]执行异常: {}", pluginId, methodName, e.getMessage(), e);
            
            // 获取操作类型
            PluginErrorHandler.OperationType operationType = getOperationType(methodName);
            
            // 记录错误
            errorHandler.handlePluginError(pluginId, e, operationType);
            
            // 重新抛出异常
            throw e;
        }
    }

    /**
     * 获取当前插件ID
     */
    private String getCurrentPluginId(ProceedingJoinPoint joinPoint) {
        try {
            // 尝试从目标对象中获取插件ID
            Object target = joinPoint.getTarget();
            
            // 这里需要实现从插件实例获取插件ID的逻辑
            // 临时返回一个占位符，实际实现需要考虑如何从插件实例获取ID
            return target.getClass().getName();
        } catch (Exception e) {
            log.warn("无法获取插件ID: {}", e.getMessage());
            return "unknown";
        }
    }

    /**
     * 根据方法名获取操作类型
     */
    private PluginErrorHandler.OperationType getOperationType(String methodName) {
        switch (methodName) {
            case "init":
                return PluginErrorHandler.OperationType.INIT;
            case "start":
                return PluginErrorHandler.OperationType.START;
            case "stop":
                return PluginErrorHandler.OperationType.STOP;
            case "destroy":
                return PluginErrorHandler.OperationType.UNLOAD;
            default:
                return PluginErrorHandler.OperationType.RUNTIME;
        }
    }
} 