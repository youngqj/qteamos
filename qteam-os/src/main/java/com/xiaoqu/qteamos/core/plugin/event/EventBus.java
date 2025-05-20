package com.xiaoqu.qteamos.core.plugin.event;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 事件总线
 * 负责事件的注册、分发和管理
 *
 * @author yangqijun
 * @date 2024-07-03
 */
@Component
public class EventBus {
    private static final Logger log = LoggerFactory.getLogger(EventBus.class);
    
    // 线程池配置
    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 10;
    private static final int QUEUE_CAPACITY = 100;
    private static final long KEEP_ALIVE_TIME = 60L;
    
    // 事件处理器集合，按主题和类型分类
    private final Map<String, Map<String, List<EventHandler>>> handlers = new ConcurrentHashMap<>();
    
    // 异步事件处理线程池
    private final ExecutorService executorService;
    
    /**
     * 构造函数
     */
    public EventBus() {
        // 创建线程池
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                new ThreadFactory() {
                    private int counter = 0;
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r, "event-bus-" + counter++);
                        thread.setDaemon(true);
                        return thread;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        this.executorService = executor;
        log.info("事件总线初始化完成");
    }
    
    /**
     * 注册事件处理器
     *
     * @param handler 事件处理器
     */
    public void registerHandler(EventHandler handler) {
        if (handler == null) {
            return;
        }
        
        String[] topics = handler.getTopics();
        String[] types = handler.getTypes();
        
        if (topics == null || topics.length == 0 || types == null || types.length == 0) {
            log.warn("事件处理器缺少主题或类型定义，跳过注册: {}", handler.getClass().getName());
            return;
        }
        
        for (String topic : topics) {
            Map<String, List<EventHandler>> topicHandlers = handlers.computeIfAbsent(topic, k -> new ConcurrentHashMap<>());
            
            for (String type : types) {
                List<EventHandler> typeHandlers = topicHandlers.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>());
                
                // 按优先级插入处理器
                addHandlerWithPriority(typeHandlers, handler);
                
                log.debug("注册事件处理器: {}，主题: {}，类型: {}", handler.getClass().getName(), topic, type);
            }
        }
    }
    
    /**
     * 按优先级添加处理器
     *
     * @param handlers 处理器列表
     * @param handler 要添加的处理器
     */
    private void addHandlerWithPriority(List<EventHandler> handlers, EventHandler handler) {
        // 如果已存在相同处理器，先移除
        handlers.remove(handler);
        
        int priority = handler.getPriority();
        int index = 0;
        
        // 找到合适的插入位置
        for (; index < handlers.size(); index++) {
            if (handlers.get(index).getPriority() < priority) {
                break;
            }
        }
        
        handlers.add(index, handler);
    }
    
    /**
     * 注销事件处理器
     *
     * @param handler 事件处理器
     */
    public void unregisterHandler(EventHandler handler) {
        if (handler == null) {
            return;
        }
        
        String[] topics = handler.getTopics();
        String[] types = handler.getTypes();
        
        if (topics == null || topics.length == 0 || types == null || types.length == 0) {
            return;
        }
        
        for (String topic : topics) {
            Map<String, List<EventHandler>> topicHandlers = handlers.get(topic);
            if (topicHandlers == null) {
                continue;
            }
            
            for (String type : types) {
                List<EventHandler> typeHandlers = topicHandlers.get(type);
                if (typeHandlers != null) {
                    typeHandlers.remove(handler);
                    log.debug("注销事件处理器: {}，主题: {}，类型: {}", handler.getClass().getName(), topic, type);
                }
            }
            
            // 如果主题下没有处理器了，移除主题
            if (topicHandlers.isEmpty()) {
                handlers.remove(topic);
            }
        }
    }
    
    /**
     * 发布事件
     *
     * @param event 要发布的事件
     */
    public void postEvent(Event event) {
        if (event == null) {
            log.warn("尝试发布空事件");
            return;
        }
        
        log.debug("发布事件: {}", event);
        log.debug("事件类型: {}, 类加载器: {}", event.getClass().getName(), event.getClass().getClassLoader());
        
        // 获取匹配的处理器列表
        List<EventHandler> matchedHandlers = getMatchedHandlers(event);
        
        if (matchedHandlers.isEmpty()) {
            log.debug("没有匹配的处理器处理事件: {}", event);
            return;
        }
        
        // 分发事件到处理器
        dispatchEvent(event, matchedHandlers);
    }
    
    /**
     * 获取匹配事件的处理器列表
     *
     * @param event 事件
     * @return 匹配的处理器列表
     */
    private List<EventHandler> getMatchedHandlers(Event event) {
        List<EventHandler> result = new ArrayList<>();
        String topic = event.getTopic();
        String type = event.getType();
        
        // 检查特定主题特定类型
        collectHandlers(result, topic, type);
        
        // 检查特定主题所有类型
        collectHandlers(result, topic, "*");
        
        // 检查所有主题特定类型
        collectHandlers(result, "*", type);
        
        // 检查所有主题所有类型
        collectHandlers(result, "*", "*");
        
        return result;
    }
    
    /**
     * 收集指定主题和类型的处理器
     *
     * @param result 结果列表
     * @param topic 主题
     * @param type 类型
     */
    private void collectHandlers(List<EventHandler> result, String topic, String type) {
        Map<String, List<EventHandler>> topicHandlers = handlers.get(topic);
        if (topicHandlers != null) {
            List<EventHandler> typeHandlers = topicHandlers.get(type);
            if (typeHandlers != null) {
                result.addAll(typeHandlers);
            }
        }
    }
    
    /**
     * 分发事件到处理器
     *
     * @param event 事件
     * @param handlers 处理器列表
     */
    private void dispatchEvent(Event event, List<EventHandler> handlers) {
        for (EventHandler handler : handlers) {
            // 如果事件已取消，停止传播
            if (event.isCancelled()) {
                log.debug("事件已取消，停止传播: {}", event);
                break;
            }
            
            // 根据处理器配置决定同步/异步处理
            if (handler.isSynchronous()) {
                handleEventSynchronously(event, handler);
            } else {
                handleEventAsynchronously(event, handler);
            }
        }
    }
    
    /**
     * 同步处理事件
     *
     * @param event 事件
     * @param handler 处理器
     */
    private void handleEventSynchronously(Event event, EventHandler handler) {
        try {
            log.debug("同步处理事件: {}, 处理器: {}", event.getClass().getName(), handler.getClass().getName());
            
            boolean continueDispatch = handler.handle(event);
            
            if (!continueDispatch) {
                log.debug("处理器要求停止事件传播: {}, {}", handler.getClass().getName(), event);
                if (event.isCancellable()) {
                    event.cancel();
                }
            }
        } catch (Exception e) {
            log.error("事件处理异常: " + handler.getClass().getName(), e);
            
            // 如果配置了异常时不继续传播，则取消事件
            if (!handler.isContinueOnError() && event.isCancellable()) {
                event.cancel();
            }
        }
    }
    
    /**
     * 异步处理事件
     *
     * @param event 事件
     * @param handler 处理器
     */
    private void handleEventAsynchronously(Event event, EventHandler handler) {
        executorService.submit(() -> {
            try {
                handler.handle(event);
            } catch (Exception e) {
                log.error("异步事件处理异常: " + handler.getClass().getName(), e);
            }
        });
    }
    
    /**
     * 关闭事件总线
     */
    @PreDestroy
    public void shutdown() {
        log.info("关闭事件总线...");
        
        // 关闭线程池
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // 清空处理器
        handlers.clear();
        
        log.info("事件总线已关闭");
    }
    
    /**
     * 获取已注册的处理器数量
     *
     * @return 处理器数量
     */
    public int getHandlerCount() {
        int count = 0;
        for (Map<String, List<EventHandler>> topicHandlers : handlers.values()) {
            for (List<EventHandler> typeHandlers : topicHandlers.values()) {
                count += typeHandlers.size();
            }
        }
        return count;
    }
} 