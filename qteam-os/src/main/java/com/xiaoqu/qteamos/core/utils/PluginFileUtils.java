/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-27 20:44:49
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-05-05 00:48:59
 * @FilePath: /QTeam/qteam-os/src/main/java/com/xiaoqu/qteamos/common/utils/PluginFileUtils.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.core.utils;


import com.xiaoqu.qteamos.core.plugin.running.PluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PluginFileUtils {
    private static final Logger log = LoggerFactory.getLogger(PluginFileUtils.class);
    
    /**
     * 从JAR文件解析插件描述符
     */
    public static PluginDescriptor parsePluginDescriptor(Path jarPath) {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            // 查找plugin.yml文件
            JarEntry entry = jarFile.getJarEntry("plugin.yml");
            if (entry == null) {
                log.error("插件JAR中未找到plugin.yml文件: {}", jarPath);
                return null;
            }
            
            try (InputStream is = jarFile.getInputStream(entry)) {
                return parseYamlInputStream(is);
            }
        } catch (Exception e) {
            log.error("解析插件描述符失败: " + jarPath, e);
            return null;
        }
    }
    
    /**
     * 从YML文件解析插件描述符
     */
    public static PluginDescriptor parsePluginYml(File ymlFile) {
        try (FileInputStream fis = new FileInputStream(ymlFile)) {
            return parseYamlInputStream(fis);
        } catch (Exception e) {
            log.error("解析插件YML文件失败: " + ymlFile, e);
            return null;
        }
    }
    
    /**
     * 从输入流解析YAML为插件描述符
     */
    private static PluginDescriptor parseYamlInputStream(InputStream is) {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> map = yaml.load(is);
            
            // 使用Builder模式创建描述符对象
            return PluginDescriptor.builder()
                .pluginId((String) map.get("pluginId"))
                .name((String) map.get("name"))
                .version((String) map.get("version"))
                .description((String) map.get("description"))
                .author((String) map.get("author"))
                .mainClass((String) map.get("mainClass"))
                .type((String) map.getOrDefault("type", "normal"))
                .trust((String) map.getOrDefault("trust", "untrusted"))
                .requiredSystemVersion((String) map.getOrDefault("requiredSystemVersion", "1.0.0"))
                .build();
        } catch (Exception e) {
            log.error("解析YAML内容失败", e);
            return null;
        }
    }
    
    /**
     * 复制文件
     */
    public static void copyFile(File source, File target) throws IOException {
        Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
    
    /**
     * 从JAR文件提取plugin.yml到目标文件
     */
    public static void extractPluginYml(Path jarPath, File targetFile) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            // 查找plugin.yml文件
            JarEntry entry = jarFile.getJarEntry("plugin.yml");
            if (entry == null) {
                throw new IOException("插件JAR中未找到plugin.yml文件: " + jarPath);
            }
            
            try (InputStream is = jarFile.getInputStream(entry)) {
                Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            throw new IOException("从JAR文件提取plugin.yml失败: " + jarPath, e);
        }
    }
}
