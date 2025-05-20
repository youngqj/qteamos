package com.xiaoqu.qteamos.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁工具类
 * 提供基于Redis的分布式锁实现，通过Redisson客户端实现
 *
 * @author yangqijun@xiaoquio.com
 * @version 1.0.0
 * @copyright 浙江小趣信息技术有限公司
 */
@Component
@Slf4j
public class DistributedLockUtils {

    private final RedissonClient redissonClient;

    @Autowired
    public DistributedLockUtils(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 执行带分布式锁的任务
     *
     * @param lockName     锁名称
     * @param waitTime     等待时间
     * @param leaseTime    持有锁时间
     * @param timeUnit     时间单位
     * @param task         要执行的任务
     * @param <T>          任务返回类型
     * @return 任务执行结果
     * @throws InterruptedException 获取锁被中断
     */
    public <T> T executeWithLock(String lockName, long waitTime, long leaseTime, TimeUnit timeUnit, Supplier<T> task) throws InterruptedException {
        RLock lock = redissonClient.getLock(formatLockName(lockName));
        boolean locked = false;
        try {
            locked = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (locked) {
                log.debug("获取分布式锁成功: {}", lockName);
                return task.get();
            } else {
                log.warn("获取分布式锁失败: {}", lockName);
                throw new RuntimeException("获取分布式锁失败: " + lockName);
            }
        } finally {
            if (locked) {
                lock.unlock();
                log.debug("释放分布式锁: {}", lockName);
            }
        }
    }

    /**
     * 执行带分布式锁的任务（无返回值）
     *
     * @param lockName     锁名称
     * @param waitTime     等待时间
     * @param leaseTime    持有锁时间
     * @param timeUnit     时间单位
     * @param task         要执行的任务
     * @throws InterruptedException 获取锁被中断
     */
    public void executeWithLock(String lockName, long waitTime, long leaseTime, TimeUnit timeUnit, Runnable task) throws InterruptedException {
        RLock lock = redissonClient.getLock(formatLockName(lockName));
        boolean locked = false;
        try {
            locked = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (locked) {
                log.debug("获取分布式锁成功: {}", lockName);
                task.run();
            } else {
                log.warn("获取分布式锁失败: {}", lockName);
                throw new RuntimeException("获取分布式锁失败: " + lockName);
            }
        } finally {
            if (locked) {
                lock.unlock();
                log.debug("释放分布式锁: {}", lockName);
            }
        }
    }

    /**
     * 使用默认等待和持有时间执行带锁任务
     *
     * @param lockName 锁名称
     * @param task     要执行的任务
     * @param <T>      任务返回类型
     * @return 任务执行结果
     */
    public <T> T executeWithLock(String lockName, Supplier<T> task) {
        try {
            return executeWithLock(lockName, 5, 30, TimeUnit.SECONDS, task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取分布式锁被中断: " + lockName, e);
        }
    }

    /**
     * 使用默认等待和持有时间执行带锁任务（无返回值）
     *
     * @param lockName 锁名称
     * @param task     要执行的任务
     */
    public void executeWithLock(String lockName, Runnable task) {
        try {
            executeWithLock(lockName, 5, 30, TimeUnit.SECONDS, task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取分布式锁被中断: " + lockName, e);
        }
    }
    
    /**
     * 检查是否可以获取锁（不实际获取）
     * 
     * @param lockName 锁名称
     * @return 锁是否可用
     */
    public boolean isLockAvailable(String lockName) {
        RLock lock = redissonClient.getLock(formatLockName(lockName));
        return !lock.isLocked();
    }

    /**
     * 格式化锁名称，添加统一前缀
     *
     * @param lockName 原始锁名称
     * @return 格式化后的锁名称
     */
    private String formatLockName(String lockName) {
        return "qelebase:lock:" + lockName;
    }
} 