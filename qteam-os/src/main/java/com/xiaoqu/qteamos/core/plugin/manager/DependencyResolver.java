package com.xiaoqu.qteamos.core.plugin.manager;

import com.xiaoqu.qteamos.core.plugin.manager.exception.PluginDependencyException;
import com.xiaoqu.qteamos.core.plugin.running.PluginDependency;
import com.xiaoqu.qteamos.core.plugin.running.PluginDescriptor;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import com.xiaoqu.qteamos.core.plugin.running.PluginState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 插件依赖解析器
 * 负责检查和解析插件之间的依赖关系
 *
 * @author yangqijun
 * @date 2024-07-02
 */
@Component
public class DependencyResolver {
    private static final Logger log = LoggerFactory.getLogger(DependencyResolver.class);

    @Autowired
    private PluginRegistry pluginRegistry;
    
    /**
     * 检查插件依赖是否满足
     *
     * @param descriptor 插件描述符
     * @return 所有依赖是否满足
     */
    public boolean checkDependencies(PluginDescriptor descriptor) {
        List<PluginDependency> dependencies = descriptor.getDependencies();
        if (dependencies == null || dependencies.isEmpty()) {
            log.info("插件无依赖: {}", descriptor.getPluginId());
            return true;
        }
        
        for (PluginDependency dependency : dependencies) {
            if (!checkSingleDependency(dependency)) {
                log.error("插件依赖不满足: {}, 依赖: {}", descriptor.getPluginId(), dependency);
                return false;
            }
        }
        
        log.info("插件依赖检查通过: {}", descriptor.getPluginId());
        return true;
    }
    
    /**
     * 检查单个依赖是否满足
     *
     * @param dependency 依赖信息
     * @return 依赖是否满足
     */
    private boolean checkSingleDependency(PluginDependency dependency) {
        String pluginId = dependency.getPluginId();
        String requiredVersion = dependency.getVersion();
        boolean isOptional = dependency.isOptional();
        
        // 获取依赖的插件信息
        Optional<PluginInfo> optionalPlugin = pluginRegistry.getPlugin(pluginId);
        
        // 如果依赖不存在且为必需依赖，则返回失败
        if (optionalPlugin.isEmpty()) {
            if (isOptional) {
                log.info("可选依赖插件不存在: {}", pluginId);
                return true;
            } else {
                log.error("必需依赖插件不存在: {}", pluginId);
                return false;
            }
        }
        
        PluginInfo dependencyPlugin = optionalPlugin.get();
        
        // 检查依赖插件状态
        if (dependencyPlugin.getState() != PluginState.RUNNING) {
            if (isOptional) {
                log.info("可选依赖插件未运行: {}, 当前状态: {}", pluginId, dependencyPlugin.getState());
                return true;
            } else {
                log.error("必需依赖插件未运行: {}, 当前状态: {}", pluginId, dependencyPlugin.getState());
                return false;
            }
        }
        
        // 检查版本是否兼容
        if (requiredVersion != null && !requiredVersion.isEmpty()) {
            String actualVersion = dependencyPlugin.getDescriptor().getVersion();
            if (!dependency.isSatisfiedBy(actualVersion)) {
                log.error("依赖插件版本不兼容: {}, 需要: {}, 实际: {}", 
                        pluginId, requiredVersion, actualVersion);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 获取插件的所有依赖（包括传递依赖）
     *
     * @param pluginId 插件ID
     * @return 所有依赖的插件ID列表
     * @throws PluginDependencyException 依赖异常
     */
    public List<String> getAllDependencies(String pluginId) throws PluginDependencyException {
        Set<String> visited = new HashSet<>();
        List<String> result = new ArrayList<>();
        
        if (!collectDependencies(pluginId, visited, result, new HashSet<>())) {
            throw new PluginDependencyException("插件存在循环依赖: " + pluginId);
        }
        
        return result;
    }
    
    /**
     * 递归收集所有依赖
     *
     * @param pluginId 当前插件ID
     * @param visited 已访问的插件ID集合
     * @param result 结果列表
     * @param path 当前依赖路径，用于检测循环依赖
     * @return 是否成功收集
     */
    private boolean collectDependencies(String pluginId, Set<String> visited, List<String> result, Set<String> path) {
        // 检查循环依赖
        if (path.contains(pluginId)) {
            log.error("检测到循环依赖: {}", path);
            return false;
        }
        
        // 已访问过，跳过
        if (visited.contains(pluginId)) {
            return true;
        }
        
        // 标记为已访问
        visited.add(pluginId);
        path.add(pluginId);
        
        // 获取插件信息
        Optional<PluginInfo> optionalPlugin = pluginRegistry.getPlugin(pluginId);
        if (optionalPlugin.isEmpty()) {
            log.warn("插件不存在: {}", pluginId);
            path.remove(pluginId);
            return true;
        }
        
        PluginInfo pluginInfo = optionalPlugin.get();
        List<PluginDependency> dependencies = pluginInfo.getDescriptor().getDependencies();
        
        // 递归处理依赖
        if (dependencies != null) {
            for (PluginDependency dependency : dependencies) {
                String dependencyId = dependency.getPluginId();
                
                // 跳过可选依赖
                if (dependency.isOptional()) {
                    continue;
                }
                
                if (!collectDependencies(dependencyId, visited, result, new HashSet<>(path))) {
                    return false;
                }
            }
        }
        
        // 从路径中移除
        path.remove(pluginId);
        
        // 添加到结果
        result.add(pluginId);
        
        return true;
    }
    
    /**
     * 获取插件的拓扑排序结果（依赖在前，被依赖在后）
     *
     * @return 拓扑排序后的插件ID列表
     * @throws PluginDependencyException 依赖异常
     */
    public List<String> getTopologicalOrder() throws PluginDependencyException {
        // 所有插件ID
        Collection<PluginInfo> allPlugins = pluginRegistry.getAllPlugins();
        
        // 构建依赖图
        Map<String, Set<String>> graph = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        
        // 初始化图
        for (PluginInfo pluginInfo : allPlugins) {
            String pluginId = pluginInfo.getDescriptor().getPluginId();
            graph.put(pluginId, new HashSet<>());
            inDegree.put(pluginId, 0);
        }
        
        // 构建边和入度
        for (PluginInfo pluginInfo : allPlugins) {
            String pluginId = pluginInfo.getDescriptor().getPluginId();
            List<PluginDependency> dependencies = pluginInfo.getDescriptor().getDependencies();
            
            if (dependencies != null) {
                for (PluginDependency dependency : dependencies) {
                    if (dependency.isOptional()) {
                        continue;
                    }
                    
                    String dependencyId = dependency.getPluginId();
                    if (graph.containsKey(dependencyId)) {
                        // 添加依赖边：dependencyId -> pluginId
                        graph.get(dependencyId).add(pluginId);
                        // 增加入度
                        inDegree.put(pluginId, inDegree.get(pluginId) + 1);
                    }
                }
            }
        }
        
        // 拓扑排序
        List<String> result = new ArrayList<>();
        Queue<String> queue = new LinkedList<>();
        
        // 添加入度为0的节点
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }
        
        while (!queue.isEmpty()) {
            String current = queue.poll();
            result.add(current);
            
            // 遍历邻居
            for (String neighbor : graph.get(current)) {
                // 减少入度
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                // 如果入度为0，加入队列
                if (inDegree.get(neighbor) == 0) {
                    queue.add(neighbor);
                }
            }
        }
        
        // 检查是否有环
        if (result.size() != allPlugins.size()) {
            throw new PluginDependencyException("插件依赖图中存在循环依赖");
        }
        
        return result;
    }
    
    /**
     * 获取依赖特定插件的所有插件
     *
     * @param pluginId 插件ID
     * @return 依赖该插件的插件ID列表
     */
    public List<String> getDependentPlugins(String pluginId) {
        List<String> result = new ArrayList<>();
        
        for (PluginInfo pluginInfo : pluginRegistry.getAllPlugins()) {
            List<PluginDependency> dependencies = pluginInfo.getDescriptor().getDependencies();
            if (dependencies != null) {
                for (PluginDependency dependency : dependencies) {
                    if (dependency.getPluginId().equals(pluginId)) {
                        result.add(pluginInfo.getDescriptor().getPluginId());
                        break;
                    }
                }
            }
        }
        
        return result;
    }
} 