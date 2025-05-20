package com.xiaoqu.qteamos.core.plugin.manager;

import com.xiaoqu.qteamos.common.utils.VersionUtils;
import com.xiaoqu.qteamos.core.plugin.manager.exception.PluginDependencyException;
import com.xiaoqu.qteamos.core.plugin.running.PluginDependency;
import com.xiaoqu.qteamos.core.plugin.running.PluginDescriptor;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 增强版插件依赖解析器
 * 支持版本冲突解决、依赖级联管理等高级功能
 *
 * @author yangqijun
 * @date 2024-07-18
 */
@Component
public class EnhancedDependencyResolver extends DependencyResolver {
    private static final Logger log = LoggerFactory.getLogger(EnhancedDependencyResolver.class);

    @Autowired
    private PluginRegistry pluginRegistry;
    
    @Autowired
    @Qualifier("enhancedPluginVersionManager")
    private EnhancedPluginVersionManager versionManager;
    
    // 定义依赖解析策略
    public enum ResolutionStrategy {
        NEWEST,        // 选择最新版本
        OLDEST,        // 选择最旧版本
        NEAREST,       // 选择依赖树中最近的版本
        HIGHEST_RANK   // 选择优先级最高的插件依赖的版本
    }
    
    // 默认的解析策略
    private ResolutionStrategy defaultStrategy = ResolutionStrategy.NEWEST;
    
    // 依赖层级缓存
    private final Map<String, Map<String, Integer>> dependencyLevels = new HashMap<>();
    
    // 版本冲突记录
    private final Map<String, List<VersionConflict>> versionConflicts = new HashMap<>();
    
    /**
     * 设置默认的版本冲突解析策略
     *
     * @param strategy 解析策略
     */
    public void setDefaultStrategy(ResolutionStrategy strategy) {
        this.defaultStrategy = strategy;
    }
    
    /**
     * 获取默认的版本冲突解析策略
     *
     * @return 解析策略
     */
    public ResolutionStrategy getDefaultStrategy() {
        return defaultStrategy;
    }
    
    /**
     * 检查插件依赖是否满足，并解决版本冲突
     *
     * @param descriptor 插件描述符
     * @return 所有依赖是否满足
     */
    @Override
    public boolean checkDependencies(PluginDescriptor descriptor) {
        // 基本依赖检查
        if (!super.checkDependencies(descriptor)) {
            return false;
        }
        
        // 分析依赖冲突并尝试解决
        try {
            List<VersionConflict> conflicts = detectDependencyConflicts(descriptor.getPluginId());
            if (!conflicts.isEmpty()) {
                log.info("检测到插件[{}]的依赖冲突，正在尝试解决...", descriptor.getPluginId());
                resolveVersionConflicts(conflicts);
            }
            return true;
        } catch (PluginDependencyException e) {
            log.error("解析插件依赖冲突失败: " + descriptor.getPluginId(), e);
            return false;
        }
    }
    
    /**
     * 检测插件的依赖冲突
     *
     * @param pluginId 插件ID
     * @return 冲突列表
     * @throws PluginDependencyException 依赖异常
     */
    public List<VersionConflict> detectDependencyConflicts(String pluginId) throws PluginDependencyException {
        List<VersionConflict> conflicts = new ArrayList<>();
        
        // 构建依赖图和层级信息
        Map<String, Set<DependencyEdge>> dependencyGraph = buildDependencyGraph();
        Map<String, Map<String, Integer>> pluginDependencyLevels = calculateDependencyLevels(dependencyGraph);
        
        // 记录层级信息
        dependencyLevels.putAll(pluginDependencyLevels);
        
        // 查找同一插件的不同版本要求
        Map<String, Map<String, Set<String>>> versionRequirements = new HashMap<>();
        
        // 遍历所有插件
        for (PluginInfo plugin : pluginRegistry.getAllPlugins()) {
            String currentPluginId = plugin.getDescriptor().getPluginId();
            List<PluginDependency> dependencies = plugin.getDescriptor().getDependencies();
            
            if (dependencies == null || dependencies.isEmpty()) {
                continue;
            }
            
            // 收集所有依赖版本要求
            for (PluginDependency dependency : dependencies) {
                String dependencyId = dependency.getPluginId();
                String versionRequirement = dependency.getVersionRequirement();
                
                if (versionRequirement == null || versionRequirement.isEmpty()) {
                    continue;
                }
                
                // 记录版本要求
                versionRequirements
                    .computeIfAbsent(dependencyId, k -> new HashMap<>())
                    .computeIfAbsent(versionRequirement, k -> new HashSet<>())
                    .add(currentPluginId);
            }
        }
        
        // 检查版本要求冲突
        for (Map.Entry<String, Map<String, Set<String>>> entry : versionRequirements.entrySet()) {
            String dependencyId = entry.getKey();
            Map<String, Set<String>> requirements = entry.getValue();
            
            // 如果一个依赖有多个不同的版本要求，可能存在冲突
            if (requirements.size() > 1) {
                VersionConflict conflict = new VersionConflict(dependencyId);
                
                // 当前插件版本
                Optional<PluginInfo> currentPlugin = pluginRegistry.getPlugin(dependencyId);
                if (currentPlugin.isPresent()) {
                    conflict.setCurrentVersion(currentPlugin.get().getDescriptor().getVersion());
                }
                
                // 添加所有的版本要求
                for (Map.Entry<String, Set<String>> reqEntry : requirements.entrySet()) {
                    String versionReq = reqEntry.getKey();
                    Set<String> dependents = reqEntry.getValue();
                    
                    // 添加冲突详情
                    for (String dependent : dependents) {
                        conflict.addRequirement(dependent, versionReq);
                    }
                }
                
                // 检查是否有不兼容的版本要求
                if (!isRequirementsCompatible(conflict.getRequirements())) {
                    conflicts.add(conflict);
                }
            }
        }
        
        // 保存冲突记录
        versionConflicts.put(pluginId, conflicts);
        
        return conflicts;
    }
    
    /**
     * 检查版本要求是否兼容
     *
     * @param requirements 版本要求
     * @return 是否兼容
     */
    private boolean isRequirementsCompatible(Map<String, String> requirements) {
        if (requirements.size() <= 1) {
            return true;
        }
        
        // 检查是否存在一个版本满足所有要求
        List<String> allVersions = versionManager.getAllAvailableVersions();
        for (String version : allVersions) {
            boolean satisfiesAll = true;
            
            for (String requirement : requirements.values()) {
                if (!VersionUtils.satisfiesRequirement(version, requirement)) {
                    satisfiesAll = false;
                    break;
                }
            }
            
            if (satisfiesAll) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 解决版本冲突
     *
     * @param conflicts 冲突列表
     * @throws PluginDependencyException 依赖异常
     */
    public void resolveVersionConflicts(List<VersionConflict> conflicts) throws PluginDependencyException {
        for (VersionConflict conflict : conflicts) {
            String dependencyId = conflict.getDependencyId();
            String resolvedVersion;
            
            // 根据策略解析冲突
            switch (defaultStrategy) {
                case NEWEST:
                    resolvedVersion = resolveNewestVersion(conflict);
                    break;
                case OLDEST:
                    resolvedVersion = resolveOldestVersion(conflict);
                    break;
                case NEAREST:
                    resolvedVersion = resolveNearestVersion(conflict);
                    break;
                case HIGHEST_RANK:
                    resolvedVersion = resolveHighestRankVersion(conflict);
                    break;
                default:
                    resolvedVersion = resolveNewestVersion(conflict);
            }
            
            if (resolvedVersion != null) {
                log.info("解决依赖冲突: {}, 选择版本: {}, 策略: {}", 
                        dependencyId, resolvedVersion, defaultStrategy);
                conflict.setResolvedVersion(resolvedVersion);
            } else {
                throw new PluginDependencyException(
                        "无法解决插件依赖冲突: " + dependencyId + "，没有找到满足所有要求的版本");
            }
        }
    }
    
    /**
     * 解析最新版本
     *
     * @param conflict 冲突
     * @return 解析的版本
     */
    private String resolveNewestVersion(VersionConflict conflict) {
        // 获取满足所有可能满足的版本
        List<String> candidates = findCompatibleVersions(conflict);
        if (candidates.isEmpty()) {
            return null;
        }
        
        // 按版本从高到低排序
        candidates.sort((v1, v2) -> -VersionUtils.compare(v1, v2));
        
        return candidates.get(0);
    }
    
    /**
     * 解析最旧版本
     *
     * @param conflict 冲突
     * @return 解析的版本
     */
    private String resolveOldestVersion(VersionConflict conflict) {
        // 获取满足所有可能满足的版本
        List<String> candidates = findCompatibleVersions(conflict);
        if (candidates.isEmpty()) {
            return null;
        }
        
        // 按版本从低到高排序
        candidates.sort(VersionUtils::compare);
        
        return candidates.get(0);
    }
    
    /**
     * 解析最近的版本(依赖层级最浅的)
     *
     * @param conflict 冲突
     * @return 解析的版本
     */
    private String resolveNearestVersion(VersionConflict conflict) {
        // 获取满足所有可能满足的版本
        List<String> candidates = findCompatibleVersions(conflict);
        if (candidates.isEmpty()) {
            return null;
        }
        
        // 计算每个依赖方的最小层级
        Map<String, Integer> requesterLevels = new HashMap<>();
        for (String requester : conflict.getRequirements().keySet()) {
            Map<String, Integer> levels = dependencyLevels.get(requester);
            if (levels != null && levels.containsKey(conflict.getDependencyId())) {
                requesterLevels.put(requester, levels.get(conflict.getDependencyId()));
            } else {
                requesterLevels.put(requester, Integer.MAX_VALUE);
            }
        }
        
        // 找出层级最小的依赖方
        String nearestRequester = requesterLevels.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        
        if (nearestRequester != null) {
            String versionReq = conflict.getRequirements().get(nearestRequester);
            
            // 在满足最近依赖方要求的版本中选择最高版本
            List<String> nearestCandidates = candidates.stream()
                    .filter(v -> VersionUtils.satisfiesRequirement(v, versionReq))
                    .collect(Collectors.toList());
            
            if (!nearestCandidates.isEmpty()) {
                nearestCandidates.sort((v1, v2) -> -VersionUtils.compare(v1, v2));
                return nearestCandidates.get(0);
            }
        }
        
        // 如果没有找到合适的版本，回退到最新版本策略
        return resolveNewestVersion(conflict);
    }
    
    /**
     * 解析优先级最高的版本
     *
     * @param conflict 冲突
     * @return 解析的版本
     */
    private String resolveHighestRankVersion(VersionConflict conflict) {
        // 获取满足所有可能满足的版本
        List<String> candidates = findCompatibleVersions(conflict);
        if (candidates.isEmpty()) {
            return null;
        }
        
        // 查找每个依赖方的优先级
        Map<String, Integer> requesterPriorities = new HashMap<>();
        for (String requester : conflict.getRequirements().keySet()) {
            // 根据插件的priority属性确定优先级，越小优先级越高
            Optional<PluginInfo> pluginInfo = pluginRegistry.getPlugin(requester);
            if (pluginInfo.isPresent()) {
                requesterPriorities.put(requester, pluginInfo.get().getDescriptor().getPriority());
            } else {
                requesterPriorities.put(requester, Integer.MAX_VALUE);
            }
        }
        
        // 找出优先级最高的依赖方
        String highestRankRequester = requesterPriorities.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        
        if (highestRankRequester != null) {
            String versionReq = conflict.getRequirements().get(highestRankRequester);
            
            // 在满足优先级最高依赖方要求的版本中选择最高版本
            List<String> highRankCandidates = candidates.stream()
                    .filter(v -> VersionUtils.satisfiesRequirement(v, versionReq))
                    .collect(Collectors.toList());
            
            if (!highRankCandidates.isEmpty()) {
                highRankCandidates.sort((v1, v2) -> -VersionUtils.compare(v1, v2));
                return highRankCandidates.get(0);
            }
        }
        
        // 如果没有找到合适的版本，回退到最新版本策略
        return resolveNewestVersion(conflict);
    }
    
    /**
     * 查找兼容的版本
     *
     * @param conflict 冲突
     * @return 兼容版本列表
     */
    private List<String> findCompatibleVersions(VersionConflict conflict) {
        String dependencyId = conflict.getDependencyId();
        List<String> allVersions = versionManager.getAllAvailableVersions();
        
        // 过滤出满足所有要求的版本
        return allVersions.stream()
                .filter(version -> {
                    for (String requirement : conflict.getRequirements().values()) {
                        if (!VersionUtils.satisfiesRequirement(version, requirement)) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 构建依赖图
     *
     * @return 依赖图
     */
    private Map<String, Set<DependencyEdge>> buildDependencyGraph() {
        Map<String, Set<DependencyEdge>> graph = new HashMap<>();
        
        // 初始化图
        for (PluginInfo plugin : pluginRegistry.getAllPlugins()) {
            String pluginId = plugin.getDescriptor().getPluginId();
            graph.put(pluginId, new HashSet<>());
        }
        
        // 添加边
        for (PluginInfo plugin : pluginRegistry.getAllPlugins()) {
            String pluginId = plugin.getDescriptor().getPluginId();
            List<PluginDependency> dependencies = plugin.getDescriptor().getDependencies();
            
            if (dependencies != null) {
                for (PluginDependency dependency : dependencies) {
                    String dependencyId = dependency.getPluginId();
                    
                    // 如果依赖存在，添加边
                    if (graph.containsKey(dependencyId)) {
                        DependencyEdge edge = new DependencyEdge(
                                dependencyId, pluginId, dependency.getVersionRequirement());
                        graph.get(dependencyId).add(edge);
                    }
                }
            }
        }
        
        return graph;
    }
    
    /**
     * 计算依赖层级
     *
     * @param graph 依赖图
     * @return 层级信息
     */
    private Map<String, Map<String, Integer>> calculateDependencyLevels(
            Map<String, Set<DependencyEdge>> graph) {
        Map<String, Map<String, Integer>> result = new HashMap<>();
        
        // 为每个插件计算所有依赖的层级
        for (String pluginId : graph.keySet()) {
            Map<String, Integer> levels = new HashMap<>();
            Queue<DependencyNode> queue = new LinkedList<>();
            
            // 将自身添加到队列，层级为0
            queue.add(new DependencyNode(pluginId, 0));
            
            // BFS遍历
            while (!queue.isEmpty()) {
                DependencyNode node = queue.poll();
                String currentId = node.getPluginId();
                int level = node.getLevel();
                
                // 已经访问过且层级更浅，跳过
                if (levels.containsKey(currentId) && levels.get(currentId) <= level) {
                    continue;
                }
                
                // 记录层级
                levels.put(currentId, level);
                
                // 遍历依赖
                for (DependencyEdge edge : graph.getOrDefault(currentId, Collections.emptySet())) {
                    queue.add(new DependencyNode(edge.getTarget(), level + 1));
                }
            }
            
            result.put(pluginId, levels);
        }
        
        return result;
    }
    
    /**
     * 获取版本冲突记录
     *
     * @param pluginId 插件ID
     * @return 冲突列表
     */
    public List<VersionConflict> getVersionConflicts(String pluginId) {
        return versionConflicts.getOrDefault(pluginId, Collections.emptyList());
    }
    
    /**
     * 获取所有插件的版本冲突
     *
     * @return 所有冲突
     */
    public Map<String, List<VersionConflict>> getAllVersionConflicts() {
        return Collections.unmodifiableMap(versionConflicts);
    }
    
    /**
     * 清除所有冲突记录
     */
    public void clearConflictRecords() {
        versionConflicts.clear();
        dependencyLevels.clear();
    }
    
    /**
     * 版本冲突类
     * 记录依赖冲突信息
     */
    public static class VersionConflict {
        private final String dependencyId;
        private final Map<String, String> requirements = new HashMap<>();
        private String currentVersion;
        private String resolvedVersion;
        
        public VersionConflict(String dependencyId) {
            this.dependencyId = dependencyId;
        }
        
        public String getDependencyId() {
            return dependencyId;
        }
        
        public void addRequirement(String requester, String versionRequirement) {
            requirements.put(requester, versionRequirement);
        }
        
        public Map<String, String> getRequirements() {
            return Collections.unmodifiableMap(requirements);
        }
        
        public String getCurrentVersion() {
            return currentVersion;
        }
        
        public void setCurrentVersion(String currentVersion) {
            this.currentVersion = currentVersion;
        }
        
        public String getResolvedVersion() {
            return resolvedVersion;
        }
        
        public void setResolvedVersion(String resolvedVersion) {
            this.resolvedVersion = resolvedVersion;
        }
        
        public boolean isResolved() {
            return resolvedVersion != null;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("依赖冲突: ").append(dependencyId);
            
            if (currentVersion != null) {
                sb.append(", 当前版本: ").append(currentVersion);
            }
            
            if (resolvedVersion != null) {
                sb.append(", 解决版本: ").append(resolvedVersion);
            }
            
            sb.append(", 版本要求: {");
            boolean first = true;
            for (Map.Entry<String, String> entry : requirements.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                sb.append(entry.getKey()).append(": ").append(entry.getValue());
            }
            sb.append("}");
            
            return sb.toString();
        }
    }
    
    /**
     * 依赖边
     * 表示依赖关系图中的边
     */
    private static class DependencyEdge {
        private final String source;
        private final String target;
        private final String versionRequirement;
        
        public DependencyEdge(String source, String target, String versionRequirement) {
            this.source = source;
            this.target = target;
            this.versionRequirement = versionRequirement;
        }
        
        public String getSource() {
            return source;
        }
        
        public String getTarget() {
            return target;
        }
        
        public String getVersionRequirement() {
            return versionRequirement;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DependencyEdge that = (DependencyEdge) o;
            return Objects.equals(source, that.source) &&
                    Objects.equals(target, that.target);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(source, target);
        }
    }
    
    /**
     * 依赖节点
     * 表示BFS遍历中的节点
     */
    private static class DependencyNode {
        private final String pluginId;
        private final int level;
        
        public DependencyNode(String pluginId, int level) {
            this.pluginId = pluginId;
            this.level = level;
        }
        
        public String getPluginId() {
            return pluginId;
        }
        
        public int getLevel() {
            return level;
        }
    }
} 