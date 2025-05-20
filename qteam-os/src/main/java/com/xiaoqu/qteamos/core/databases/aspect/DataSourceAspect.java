package com.xiaoqu.qteamos.core.databases.aspect;

import com.xiaoqu.qteamos.core.databases.annotation.DataSource;
import com.xiaoqu.qteamos.core.databases.core.DataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 数据源切面
 * 根据@DataSource注解切换数据源
 *
 * @author yangqijun
 * @date 2025-05-02
 */
@Aspect
@Order(1) // 确保该AOP在事务AOP之前执行
@Component
@Slf4j
public class DataSourceAspect {

    /**
     * 定义切点：所有使用@DataSource注解的方法
     */
    @Pointcut("@annotation(com.xiaoqu.qteamos.core.databases.annotation.DataSource)")
    public void dataSourcePointCut() {
    }

    /**
     * 环绕通知：在方法执行前后切换数据源
     *
     * @param point 连接点
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("dataSourcePointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        // 获取目标方法的签名
        MethodSignature signature = (MethodSignature) point.getSignature();
        
        // 获取目标方法
        Method method = signature.getMethod();
        
        // 获取目标方法上的@DataSource注解
        DataSource dataSource = method.getAnnotation(DataSource.class);
        
        // 如果方法上没有注解，检查类上是否有注解
        if (dataSource == null) {
            dataSource = point.getTarget().getClass().getAnnotation(DataSource.class);
        }
        
        // 如果有注解，则切换数据源
        if (dataSource != null) {
            String dataSourceName = dataSource.value();
            log.debug("切换数据源到: {}", dataSourceName);
            DataSourceContextHolder.setDataSource(dataSourceName);
        }
        
        try {
            // 执行目标方法
            return point.proceed();
        } finally {
            // 清除数据源上下文
            if (dataSource != null) {
                log.debug("清除数据源设置");
                DataSourceContextHolder.clearDataSource();
            }
        }
    }
} 