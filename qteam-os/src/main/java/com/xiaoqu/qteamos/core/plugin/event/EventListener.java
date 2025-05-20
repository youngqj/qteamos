package com.xiaoqu.qteamos.core.plugin.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 事件监听器注解
 * 用于标记方法为事件处理方法，自动注册到事件总线
 *
 * @author yangqijun
 * @date 2024-07-03
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventListener {
    
    /**
     * 事件主题
     * 可以使用通配符"*"表示监听所有主题
     *
     * @return 事件主题数组
     */
    String[] topics() default {"*"};
    
    /**
     * 事件类型
     * 可以使用通配符"*"表示监听所有类型
     *
     * @return 事件类型数组
     */
    String[] types() default {"*"};
    
    /**
     * 监听器优先级
     * 数值越大优先级越高，默认为0
     *
     * @return 优先级
     */
    int priority() default 0;
    
    /**
     * 是否同步处理事件
     * 如果为true，事件将在发布线程中同步处理
     * 如果为false，事件将在事件处理线程池中异步处理
     *
     * @return 是否同步处理
     */
    boolean synchronous() default true;
    
    /**
     * 是否在处理事件发生异常时继续传播事件
     * 如果为true，即使处理过程中发生异常也会继续传播事件
     * 如果为false，发生异常将终止事件传播
     *
     * @return 是否继续传播
     */
    boolean continueOnError() default false;
} 