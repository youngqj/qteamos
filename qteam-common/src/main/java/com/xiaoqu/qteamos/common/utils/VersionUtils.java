/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-27 19:18:11
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-04-27 19:12:38
 * @FilePath: /QEleBase/qelebase-core/src/main/java/com/xiaoqu/qelebase/core/pluginSource/util/VersionUtils.java
 * @Description: 版本工具类，实现语义化版本比较功能
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 版本工具类
 * 实现语义化版本比较、解析和验证功能
 * 支持语义化版本规范 (SemVer) 2.0.0
 * 
 * @author yangqijun
 * @version 1.0.0
 */
public class VersionUtils {

    /**
     * 语义化版本正则表达式
     * 匹配格式：主版本号.次版本号.修订号[-预发布版本][+构建元数据]
     * 例如：1.2.3、1.0.0-alpha.1、1.0.0-beta+exp.sha.5114f85
     */
    private static final Pattern SEMVER_PATTERN = Pattern.compile(
            "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$");

    /**
     * 版本范围操作符正则表达式
     * 支持 >、>=、<、<=、=、~、^
     */
    private static final Pattern RANGE_PATTERN = Pattern.compile(
            "^([\\^~><=]{1,2})\\s*(\\d+\\.\\d+\\.\\d+(?:-[\\w.-]+)?(?:\\+[\\w.-]+)?)$");

    /**
     * 版本范围组合正则表达式
     * 匹配如 ">=1.0.0 <2.0.0" 的格式
     */
    private static final Pattern RANGE_SET_PATTERN = Pattern.compile(
            "((?:[\\^~><=]{1,2})\\s*(?:\\d+\\.\\d+\\.\\d+(?:-[\\w.-]+)?(?:\\+[\\w.-]+)?))(?:\\s+|\\s*,\\s*)");

    /**
     * 版本通配符正则表达式
     * 匹配如 "1.x" 或 "1.2.*" 的格式
     */
    private static final Pattern WILDCARD_PATTERN = Pattern.compile(
            "^(\\d+)(?:\\.(\\d+|[xX*]))?(?:\\.(\\d+|[xX*]))?$");

    /**
     * 比较两个版本的大小
     * 
     * @param version1 版本1
     * @param version2 版本2
     * @return 如果version1 > version2返回正数，如果version1 < version2返回负数，相等返回0
     * @throws IllegalArgumentException 如果版本格式不正确
     */
    public static int compare(String version1, String version2) {
        Version v1 = parseVersion(version1);
        Version v2 = parseVersion(version2);
        
        // 比较主版本号
        int result = Integer.compare(v1.getMajor(), v2.getMajor());
        if (result != 0) {
            return result;
        }
        
        // 比较次版本号
        result = Integer.compare(v1.getMinor(), v2.getMinor());
        if (result != 0) {
            return result;
        }
        
        // 比较修订号
        result = Integer.compare(v1.getPatch(), v2.getPatch());
        if (result != 0) {
            return result;
        }
        
        // 如果都没有预发布版本，则版本相等
        if (v1.getPreRelease() == null && v2.getPreRelease() == null) {
            return 0;
        }
        
        // 有预发布版本的比没有预发布版本的低
        if (v1.getPreRelease() != null && v2.getPreRelease() == null) {
            return -1;
        }
        
        if (v1.getPreRelease() == null && v2.getPreRelease() != null) {
            return 1;
        }
        
        // 预发布版本的比较
        return comparePreRelease(v1.getPreRelease(), v2.getPreRelease());
    }

    /**
     * 比较预发布版本
     * 
     * @param preRelease1 预发布版本1
     * @param preRelease2 预发布版本2
     * @return 如果preRelease1 > preRelease2返回正数，反之返回负数，相等返回0
     */
    private static int comparePreRelease(String preRelease1, String preRelease2) {
        String[] parts1 = preRelease1.split("\\.");
        String[] parts2 = preRelease2.split("\\.");
        
        int minLength = Math.min(parts1.length, parts2.length);
        
        for (int i = 0; i < minLength; i++) {
            String part1 = parts1[i];
            String part2 = parts2[i];
            
            // 如果两个标识符都是数字，则进行数值比较
            boolean isDigit1 = isNumeric(part1);
            boolean isDigit2 = isNumeric(part2);
            
            if (isDigit1 && isDigit2) {
                int num1 = Integer.parseInt(part1);
                int num2 = Integer.parseInt(part2);
                int result = Integer.compare(num1, num2);
                if (result != 0) {
                    return result;
                }
            } else if (isDigit1) {
                // 数字标识符小于非数字标识符
                return -1;
            } else if (isDigit2) {
                // 数字标识符小于非数字标识符
                return 1;
            } else {
                // 两个都是非数字标识符，进行字典顺序比较
                int result = part1.compareTo(part2);
                if (result != 0) {
                    return result;
                }
            }
        }
        
        // 如果前面都相等，则标识符数量更多的版本优先级更高
        return Integer.compare(parts1.length, parts2.length);
    }

    /**
     * 判断字符串是否为纯数字
     */
    private static boolean isNumeric(String str) {
        return str.matches("^[0-9]+$");
    }

    /**
     * 解析版本字符串为Version对象
     * 
     * @param versionStr 版本字符串
     * @return Version对象
     * @throws IllegalArgumentException 如果版本格式不正确
     */
    public static Version parseVersion(String versionStr) {
        if (StringUtils.isBlank(versionStr)) {
            throw new IllegalArgumentException("版本字符串不能为空");
        }
        
        Matcher matcher = SEMVER_PATTERN.matcher(versionStr);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("无效的语义化版本格式: " + versionStr);
        }
        
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = Integer.parseInt(matcher.group(3));
        String preRelease = matcher.group(4);
        String buildMetadata = matcher.group(5);
        
        return new Version(major, minor, patch, preRelease, buildMetadata);
    }

    /**
     * 验证版本字符串是否符合语义化版本规范
     * 
     * @param versionStr 版本字符串
     * @return 是否有效
     */
    public static boolean isValidVersion(String versionStr) {
        if (StringUtils.isBlank(versionStr)) {
            return false;
        }
        return SEMVER_PATTERN.matcher(versionStr).matches();
    }

    /**
     * 检查版本是否满足版本要求
     * 
     * @param version 要检查的版本
     * @param requirement 版本要求
     * @return 是否满足要求
     */
    public static boolean satisfiesRequirement(String version, String requirement) {
        if (StringUtils.isBlank(version) || StringUtils.isBlank(requirement)) {
            return false;
        }
        
        // 通配符版本检查，如 "1.*" 或 "1.2.x"
        Matcher wildcardMatcher = WILDCARD_PATTERN.matcher(requirement);
        if (wildcardMatcher.matches()) {
            return checkWildcardRequirement(version, requirement);
        }
        
        // 精确版本匹配
        if (isValidVersion(requirement)) {
            return version.equals(requirement);
        }
        
        // 范围组合匹配，如 ">=1.0.0 <2.0.0"
        if (requirement.contains(" ") || requirement.contains(",")) {
            return checkRangeSetRequirement(version, requirement);
        }
        
        // 单个范围匹配，如 ">=1.0.0"
        Matcher rangeMatcher = RANGE_PATTERN.matcher(requirement);
        if (rangeMatcher.matches()) {
            String operator = rangeMatcher.group(1);
            String requiredVersion = rangeMatcher.group(2);
            return checkSingleRangeRequirement(version, operator, requiredVersion);
        }
        
        return false;
    }

    /**
     * 检查版本是否满足通配符要求
     */
    private static boolean checkWildcardRequirement(String version, String requirement) {
        Matcher wildcardMatcher = WILDCARD_PATTERN.matcher(requirement);
        if (!wildcardMatcher.matches()) {
            return false;
        }
        
        try {
            Version v = parseVersion(version);
            
            // 解析通配符中的版本部分
            int reqMajor = Integer.parseInt(wildcardMatcher.group(1));
            String minorStr = wildcardMatcher.group(2);
            String patchStr = wildcardMatcher.group(3);
            
            // 检查主版本号是否匹配
            if (v.getMajor() != reqMajor) {
                return false;
            }
            
            // 检查次版本号是否匹配（如果指定了）
            if (minorStr != null && !isWildcard(minorStr)) {
                int reqMinor = Integer.parseInt(minorStr);
                if (v.getMinor() != reqMinor) {
                    return false;
                }
                
                // 检查修订号是否匹配（如果指定了）
                if (patchStr != null && !isWildcard(patchStr)) {
                    int reqPatch = Integer.parseInt(patchStr);
                    return v.getPatch() == reqPatch;
                }
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查字符串是否为通配符（x、X或*）
     */
    private static boolean isWildcard(String str) {
        return "x".equalsIgnoreCase(str) || "*".equals(str);
    }

    /**
     * 检查版本是否满足范围组合要求
     */
    private static boolean checkRangeSetRequirement(String version, String requirement) {
        // 分割范围表达式
        String[] ranges = requirement.split("\\s+|\\s*,\\s*");
        
        if (ranges.length == 0) {
            return false;
        }
        
        // 对于由空格或逗号分隔的范围，必须满足所有条件
        for (String range : ranges) {
            if (StringUtils.isBlank(range)) {
                continue;
            }
            
            Matcher rangeMatcher = RANGE_PATTERN.matcher(range);
            if (!rangeMatcher.matches()) {
                return false;
            }
            
            String operator = rangeMatcher.group(1);
            String requiredVersion = rangeMatcher.group(2);
            
            if (!checkSingleRangeRequirement(version, operator, requiredVersion)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 检查版本是否满足单个范围要求
     */
    private static boolean checkSingleRangeRequirement(String version, String operator, String requiredVersion) {
        if (!isValidVersion(version) || !isValidVersion(requiredVersion)) {
            return false;
        }
        
        int compareResult = compare(version, requiredVersion);
        
        switch (operator) {
            case "=":
                return compareResult == 0;
            case ">":
                return compareResult > 0;
            case ">=":
                return compareResult >= 0;
            case "<":
                return compareResult < 0;
            case "<=":
                return compareResult <= 0;
            case "~": // 允许补丁版本的更新，但不允许次版本或主版本的更新
                Version v1 = parseVersion(version);
                Version v2 = parseVersion(requiredVersion);
                return v1.getMajor() == v2.getMajor() && v1.getMinor() == v2.getMinor() && v1.getPatch() >= v2.getPatch();
            case "^": // 允许次版本和补丁版本的更新，但不允许主版本的更新
                Version v3 = parseVersion(version);
                Version v4 = parseVersion(requiredVersion);
                return v3.getMajor() == v4.getMajor() && 
                       (v3.getMinor() > v4.getMinor() || 
                        (v3.getMinor() == v4.getMinor() && v3.getPatch() >= v4.getPatch()));
            default:
                return false;
        }
    }

    /**
     * 获取下一个主版本号
     * 例如：1.2.3 -> 2.0.0
     */
    public static String getNextMajorVersion(String version) {
        Version v = parseVersion(version);
        return (v.getMajor() + 1) + ".0.0";
    }

    /**
     * 获取下一个次版本号
     * 例如：1.2.3 -> 1.3.0
     */
    public static String getNextMinorVersion(String version) {
        Version v = parseVersion(version);
        return v.getMajor() + "." + (v.getMinor() + 1) + ".0";
    }

    /**
     * 获取下一个修订版本号
     * 例如：1.2.3 -> 1.2.4
     */
    public static String getNextPatchVersion(String version) {
        Version v = parseVersion(version);
        return v.getMajor() + "." + v.getMinor() + "." + (v.getPatch() + 1);
    }

    /**
     * 将版本范围解析为最小和最大版本
     * 例如：">=1.0.0 <2.0.0" -> [1.0.0, 2.0.0)
     * 
     * @param range 版本范围
     * @return 版本范围对象，包含最小和最大版本
     */
    public static VersionRange parseRange(String range) {
        if (StringUtils.isBlank(range)) {
            throw new IllegalArgumentException("版本范围不能为空");
        }
        
        // 处理单个版本（精确匹配）
        if (isValidVersion(range)) {
            return new VersionRange(range, range, true, true);
        }
        
        // 处理通配符
        Matcher wildcardMatcher = WILDCARD_PATTERN.matcher(range);
        if (wildcardMatcher.matches()) {
            return parseWildcardRange(range);
        }
        
        // 处理范围操作符
        Matcher rangeMatcher = RANGE_PATTERN.matcher(range);
        if (rangeMatcher.matches()) {
            String operator = rangeMatcher.group(1);
            String version = rangeMatcher.group(2);
            
            return parseSingleOperatorRange(operator, version);
        }
        
        // 处理复合范围
        if (range.contains(" ") || range.contains(",")) {
            return parseComplexRange(range);
        }
        
        throw new IllegalArgumentException("不支持的版本范围格式: " + range);
    }

    /**
     * 解析通配符范围
     */
    private static VersionRange parseWildcardRange(String range) {
        Matcher wildcardMatcher = WILDCARD_PATTERN.matcher(range);
        if (!wildcardMatcher.matches()) {
            throw new IllegalArgumentException("无效的通配符版本: " + range);
        }
        
        int major = Integer.parseInt(wildcardMatcher.group(1));
        String minorStr = wildcardMatcher.group(2);
        String patchStr = wildcardMatcher.group(3);
        
        String minVersion;
        String maxVersion;
        
        if (minorStr == null || isWildcard(minorStr)) {
            // 形如 "1.*"
            minVersion = major + ".0.0";
            maxVersion = (major + 1) + ".0.0";
            return new VersionRange(minVersion, maxVersion, true, false);
        } else {
            int minor = Integer.parseInt(minorStr);
            
            if (patchStr == null || isWildcard(patchStr)) {
                // 形如 "1.2.*"
                minVersion = major + "." + minor + ".0";
                maxVersion = major + "." + (minor + 1) + ".0";
                return new VersionRange(minVersion, maxVersion, true, false);
            } else {
                // 精确版本 "1.2.3"
                int patch = Integer.parseInt(patchStr);
                String version = major + "." + minor + "." + patch;
                return new VersionRange(version, version, true, true);
            }
        }
    }

    /**
     * 解析单个操作符的范围
     */
    private static VersionRange parseSingleOperatorRange(String operator, String version) {
        Version v = parseVersion(version);
        String minVersion = null;
        String maxVersion = null;
        boolean includeMin = false;
        boolean includeMax = false;
        
        switch (operator) {
            case "=":
                minVersion = version;
                maxVersion = version;
                includeMin = true;
                includeMax = true;
                break;
            case ">":
                minVersion = version;
                includeMin = false;
                break;
            case ">=":
                minVersion = version;
                includeMin = true;
                break;
            case "<":
                maxVersion = version;
                includeMax = false;
                break;
            case "<=":
                maxVersion = version;
                includeMax = true;
                break;
            case "~":
                // 允许补丁版本更新，例如 ~1.2.3 表示 >=1.2.3 <1.3.0
                minVersion = version;
                maxVersion = v.getMajor() + "." + (v.getMinor() + 1) + ".0";
                includeMin = true;
                includeMax = false;
                break;
            case "^":
                // 允许次版本和补丁版本更新，例如 ^1.2.3 表示 >=1.2.3 <2.0.0
                minVersion = version;
                maxVersion = (v.getMajor() + 1) + ".0.0";
                includeMin = true;
                includeMax = false;
                break;
            default:
                throw new IllegalArgumentException("不支持的操作符: " + operator);
        }
        
        return new VersionRange(minVersion, maxVersion, includeMin, includeMax);
    }

    /**
     * 解析复杂范围
     */
    private static VersionRange parseComplexRange(String range) {
        String[] parts = range.split("\\s+|\\s*,\\s*");
        
        if (parts.length == 0) {
            throw new IllegalArgumentException("无效的版本范围: " + range);
        }
        
        String minVersion = null;
        String maxVersion = null;
        boolean includeMin = false;
        boolean includeMax = false;
        
        for (String part : parts) {
            if (StringUtils.isBlank(part)) {
                continue;
            }
            
            Matcher rangeMatcher = RANGE_PATTERN.matcher(part);
            if (!rangeMatcher.matches()) {
                throw new IllegalArgumentException("无效的范围表达式: " + part);
            }
            
            String operator = rangeMatcher.group(1);
            String version = rangeMatcher.group(2);
            
            if (">".equals(operator) || ">=".equals(operator)) {
                String newMin = version;
                boolean newIncludeMin = ">=".equals(operator);
                
                if (minVersion == null || compare(newMin, minVersion) > 0 || 
                    (compare(newMin, minVersion) == 0 && !newIncludeMin && includeMin)) {
                    minVersion = newMin;
                    includeMin = newIncludeMin;
                }
            } else if ("<".equals(operator) || "<=".equals(operator)) {
                String newMax = version;
                boolean newIncludeMax = "<=".equals(operator);
                
                if (maxVersion == null || compare(newMax, maxVersion) < 0 || 
                    (compare(newMax, maxVersion) == 0 && !newIncludeMax && includeMax)) {
                    maxVersion = newMax;
                    includeMax = newIncludeMax;
                }
            } else if ("=".equals(operator)) {
                // 精确匹配覆盖其他范围
                return new VersionRange(version, version, true, true);
            } else if ("~".equals(operator) || "^".equals(operator)) {
                VersionRange singleRange = parseSingleOperatorRange(operator, version);
                
                if (minVersion == null || compare(singleRange.getMinVersion(), minVersion) > 0) {
                    minVersion = singleRange.getMinVersion();
                    includeMin = singleRange.isIncludeMin();
                }
                
                if (maxVersion == null || compare(singleRange.getMaxVersion(), maxVersion) < 0) {
                    maxVersion = singleRange.getMaxVersion();
                    includeMax = singleRange.isIncludeMax();
                }
            }
        }
        
        // 验证范围的有效性
        if (minVersion != null && maxVersion != null && compare(minVersion, maxVersion) > 0) {
            throw new IllegalArgumentException("无效的版本范围，最小版本大于最大版本: " + minVersion + " > " + maxVersion);
        }
        
        return new VersionRange(minVersion, maxVersion, includeMin, includeMax);
    }

    /**
     * 版本对象，表示语义化版本
     */
    public static class Version {
        private final int major;
        private final int minor;
        private final int patch;
        private final String preRelease;
        private final String buildMetadata;
        
        public Version(int major, int minor, int patch, String preRelease, String buildMetadata) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.preRelease = preRelease;
            this.buildMetadata = buildMetadata;
        }
        
        public int getMajor() {
            return major;
        }
        
        public int getMinor() {
            return minor;
        }
        
        public int getPatch() {
            return patch;
        }
        
        public String getPreRelease() {
            return preRelease;
        }
        
        public String getBuildMetadata() {
            return buildMetadata;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(major).append('.').append(minor).append('.').append(patch);
            
            if (preRelease != null && !preRelease.isEmpty()) {
                sb.append('-').append(preRelease);
            }
            
            if (buildMetadata != null && !buildMetadata.isEmpty()) {
                sb.append('+').append(buildMetadata);
            }
            
            return sb.toString();
        }
    }

    /**
     * 版本范围对象
     */
    public static class VersionRange {
        private final String minVersion;
        private final String maxVersion;
        private final boolean includeMin;
        private final boolean includeMax;
        
        public VersionRange(String minVersion, String maxVersion, boolean includeMin, boolean includeMax) {
            this.minVersion = minVersion;
            this.maxVersion = maxVersion;
            this.includeMin = includeMin;
            this.includeMax = includeMax;
        }
        
        public String getMinVersion() {
            return minVersion;
        }
        
        public String getMaxVersion() {
            return maxVersion;
        }
        
        public boolean isIncludeMin() {
            return includeMin;
        }
        
        public boolean isIncludeMax() {
            return includeMax;
        }
        
        /**
         * 检查给定版本是否在范围内
         * 
         * @param version 要检查的版本
         * @return 是否在范围内
         */
        public boolean contains(String version) {
            if (!isValidVersion(version)) {
                return false;
            }
            
            boolean satisfiesMin = true;
            boolean satisfiesMax = true;
            
            if (minVersion != null) {
                int compareMin = compare(version, minVersion);
                satisfiesMin = compareMin > 0 || (includeMin && compareMin == 0);
            }
            
            if (maxVersion != null) {
                int compareMax = compare(version, maxVersion);
                satisfiesMax = compareMax < 0 || (includeMax && compareMax == 0);
            }
            
            return satisfiesMin && satisfiesMax;
        }
        
        @Override
        public String toString() {
            if (minVersion == null && maxVersion == null) {
                return "*";
            }
            
            if (minVersion != null && maxVersion != null && minVersion.equals(maxVersion) && includeMin && includeMax) {
                return "=" + minVersion;
            }
            
            StringBuilder sb = new StringBuilder();
            
            if (minVersion != null) {
                sb.append(includeMin ? ">=" : ">").append(minVersion);
            }
            
            if (maxVersion != null) {
                if (minVersion != null) {
                    sb.append(" ");
                }
                sb.append(includeMax ? "<=" : "<").append(maxVersion);
            }
            
            return sb.toString();
        }
    }
}