package com.xiaoqu.qteamos.core.plugin.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件监听器处理器
 * 用于自动扫描事件监听器注解并注册到事件总线
 *
 * @author yangqijun
 * @date 2024-07-03
 */
@Component
public class EventListenerProcessor implements BeanPostProcessor, ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(EventListenerProcessor.class);
    
    private EventBus eventBus;
    private ApplicationContext applicationContext;
    
    // 已注册的监听器集合
    private final Set<String> registeredListeners = ConcurrentHashMap.newKeySet();
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (eventBus == null) {
            try {
                eventBus = applicationContext.getBean(EventBus.class);
            } catch (BeansException e) {
                log.warn("无法获取EventBus实例，事件监听器将不会被注册", e);
                return bean;
            }
        }
        
        if (registeredListeners.contains(beanName)) {
            return bean;
        }
        
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        Map<Method, EventListener> annotatedMethods = MethodIntrospector.selectMethods(
                targetClass,
                (MethodIntrospector.MetadataLookup<EventListener>) method ->
                        AnnotatedElementUtils.findMergedAnnotation(method, EventListener.class)
        );
        
        if (annotatedMethods.isEmpty()) {
            return bean;
        }
        
        for (Map.Entry<Method, EventListener> entry : annotatedMethods.entrySet()) {
            Method method = entry.getKey();
            EventListener annotation = entry.getValue();
            
            // 验证方法参数
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != 1 || !Event.class.isAssignableFrom(paramTypes[0])) {
                log.warn("事件处理方法参数不正确，应该只有一个Event类型参数: {}.{}", 
                        bean.getClass().getName(), method.getName());
                continue;
            }
            
            // 注册监听器
            Method methodToUse = ReflectionUtils.findMethod(
                    bean.getClass(),
                    method.getName(),
                    method.getParameterTypes()
            );
            
            if (methodToUse != null) {
                MethodEventHandler eventHandler = new MethodEventHandler(
                        bean,
                        methodToUse,
                        annotation.topics(),
                        annotation.types(),
                        annotation.priority(),
                        annotation.synchronous(),
                        annotation.continueOnError()
                );
                
                eventBus.registerHandler(eventHandler);
                log.debug("注册事件监听器: {}.{}, 主题: {}, 类型: {}", 
                        bean.getClass().getName(), method.getName(), 
                        String.join(", ", annotation.topics()), 
                        String.join(", ", annotation.types()));
            }
        }
        
        registeredListeners.add(beanName);
        return bean;
    }
    
    /**
     * 基于方法的事件处理器
     */
    private static class MethodEventHandler implements EventHandler {
        private final Object target;
        private final Method method;
        private final String[] topics;
        private final String[] types;
        private final int priority;
        private final boolean synchronous;
        private final boolean continueOnError;
        
        public MethodEventHandler(Object target, Method method, String[] topics, String[] types,
                                  int priority, boolean synchronous, boolean continueOnError) {
            this.target = target;
            this.method = method;
            this.topics = topics;
            this.types = types;
            this.priority = priority;
            this.synchronous = synchronous;
            this.continueOnError = continueOnError;
            
            ReflectionUtils.makeAccessible(method);
        }
        
        @Override
        public boolean handle(Event event) {
            try {
                // 添加基本调试日志
                Logger logger = LoggerFactory.getLogger(target.getClass());
                logger.debug("处理事件: 类型={}, 方法={}", 
                        event.getClass().getName(), method.getName());
                
                Object result = method.invoke(target, event);
                return result instanceof Boolean ? (Boolean) result : true;
            } catch (Exception e) {
                Logger logger = LoggerFactory.getLogger(target.getClass());
                logger.error("处理事件异常: 事件类型={}", event.getClass().getName(), e);
                
                // 添加更多异常信息
                if (e instanceof java.lang.reflect.InvocationTargetException) {
                    Throwable targetException = ((java.lang.reflect.InvocationTargetException) e).getTargetException();
                    logger.error("调用目标异常: {}", targetException.getMessage(), targetException);
                }
                
                logger.error("方法: {}, 参数类型: {}, 事件类型: {}", 
                      method.getName(), 
                      method.getParameterTypes()[0].getName(), 
                      event.getClass().getName());
                
                return continueOnError;
            }
        }
        
        @Override
        public int getPriority() {
            return priority;
        }
        
        @Override
        public String[] getTopics() {
            return topics;
        }
        
        @Override
        public String[] getTypes() {
            return types;
        }
        
        @Override
        public boolean isSynchronous() {
            return synchronous;
        }
        
        @Override
        public boolean isContinueOnError() {
            return continueOnError;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            MethodEventHandler that = (MethodEventHandler) obj;
            return target.equals(that.target) && method.equals(that.method);
        }
        
        @Override
        public int hashCode() {
            return 31 * target.hashCode() + method.hashCode();
        }
    }
} 