#!/bin/bash

# QTeamOS 插件测试脚本
# 用于测试插件与主程序的集成

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${YELLOW}=== QTeamOS 插件测试脚本 ===${NC}"

# 检查工作目录
if [ ! -d "qteamos-sdk" ]; then
  echo -e "${RED}错误: 请在项目根目录运行此脚本${NC}"
  exit 1
fi

# 确保插件已编译
echo -e "\n${YELLOW}[1/5] 编译插件...${NC}"
cd plugin-demos/hello-world-plugin || exit 1
mvn clean package
if [ $? -ne 0 ]; then
  echo -e "${RED}错误: 插件编译失败${NC}"
  exit 1
fi
cd ../..

# 检查插件JAR文件
PLUGIN_JAR="plugin-demos/hello-world-plugin/target/hello-world-plugin-1.0.0.jar"
if [ ! -f "$PLUGIN_JAR" ]; then
  echo -e "${RED}错误: 插件JAR文件未找到: $PLUGIN_JAR${NC}"
  exit 1
fi

# 创建测试目录
TEST_DIR="build/plugin-test"
mkdir -p "$TEST_DIR/plugins"
mkdir -p "$TEST_DIR/conf/plugins/hello-world"

# 复制插件JAR到测试插件目录
echo -e "\n${YELLOW}[2/5] 准备测试环境...${NC}"
cp "$PLUGIN_JAR" "$TEST_DIR/plugins/"
echo -e "${GREEN}已复制插件JAR到: $TEST_DIR/plugins/$(basename "$PLUGIN_JAR")${NC}"

# 创建配置文件
cat > "$TEST_DIR/conf/plugins/hello-world/config.yml" << EOF
# Hello World插件配置 - 测试环境
greeting:
  text: "你好，QTeamOS测试环境！"
  enabled: true
  interval: 3000

plugin:
  auto-start: true
  log-level: DEBUG
EOF
echo -e "${GREEN}已创建插件配置文件: $TEST_DIR/conf/plugins/hello-world/config.yml${NC}"

# 复制SDK JAR文件到测试lib目录
mkdir -p "$TEST_DIR/lib"
SDK_JAR="qteamos-sdk/target/qteamos-sdk-0.0.1-SNAPSHOT.jar"
if [ ! -f "$SDK_JAR" ]; then
  echo -e "${YELLOW}警告: SDK JAR文件未找到，尝试编译SDK...${NC}"
  cd qteamos-sdk || exit 1
  mvn clean package
  if [ $? -ne 0 ]; then
    echo -e "${RED}错误: SDK编译失败${NC}"
    exit 1
  fi
  cd ..
fi
cp "$SDK_JAR" "$TEST_DIR/lib/"
echo -e "${GREEN}已复制SDK JAR到: $TEST_DIR/lib/$(basename "$SDK_JAR")${NC}"

# 创建一个简单的测试主程序
echo -e "\n${YELLOW}[3/5] 创建测试主程序...${NC}"

TEST_APP_DIR="$TEST_DIR/test-app/src/main/java/com/xiaoqu/test"
mkdir -p "$TEST_APP_DIR"

cat > "$TEST_APP_DIR/PluginTester.java" << 'EOF'
package com.xiaoqu.test;

import java.io.File;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 一个简单的插件测试程序
 * 通过反射方式加载和测试插件，避免直接依赖具体实现
 */
public class PluginTester {
    
    public static void main(String[] args) {
        try {
            System.out.println("=== QTeamOS 插件测试器 ===");
            
            // 扫描插件目录
            System.out.println("\n[1] 扫描插件目录...");
            File pluginsDir = new File("plugins");
            System.out.println("插件目录: " + pluginsDir.getAbsolutePath());
            
            if (!pluginsDir.exists() || !pluginsDir.isDirectory()) {
                System.err.println("错误: 插件目录不存在!");
                return;
            }
            
            File[] pluginFiles = pluginsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
            if (pluginFiles == null || pluginFiles.length == 0) {
                System.err.println("错误: 插件目录中未找到JAR文件!");
                return;
            }
            
            System.out.println("找到 " + pluginFiles.length + " 个插件文件");
            for (File file : pluginFiles) {
                System.out.println("- " + file.getName());
            }
            
            // 使用反射方式加载插件
            System.out.println("\n[2] 初始化环境...");
            // 创建类加载器
            File sdkJarFile = new File("lib/qteamos-sdk-0.0.1-SNAPSHOT.jar");
            
            // 查找插件JAR
            File pluginJarFile = pluginFiles[0];
            String jarPath = pluginJarFile.getAbsolutePath();
            System.out.println("加载插件: " + jarPath);
            
            // 创建自定义类加载器
            ClassLoader parentClassLoader = PluginTester.class.getClassLoader();
            
            // 使用反射创建插件加载器
            System.out.println("\n[3] 通过反射创建插件加载器...");
            Class<?> urlClassLoaderClass = Class.forName("java.net.URLClassLoader");
            Method addUrlMethod = urlClassLoaderClass.getDeclaredMethod("addURL", java.net.URL.class);
            addUrlMethod.setAccessible(true);
            
            java.net.URLClassLoader classLoader = (java.net.URLClassLoader)parentClassLoader;
            addUrlMethod.invoke(classLoader, sdkJarFile.toURI().toURL());
            addUrlMethod.invoke(classLoader, pluginJarFile.toURI().toURL());
            
            // 加载插件主类
            System.out.println("\n[4] 加载插件主类...");
            Class<?> pluginClass = null;
            
            try {
                // 加载插件主类
                pluginClass = Class.forName("com.xiaoqu.plugins.hello.HelloWorldPlugin", true, classLoader);
                
                // 创建插件实例
                Object plugin = pluginClass.getDeclaredConstructor().newInstance();
                
                // 获取插件信息
                Method getIdMethod = pluginClass.getMethod("getId");
                Method getNameMethod = pluginClass.getMethod("getName");
                Method getVersionMethod = pluginClass.getMethod("getVersion");
                Method getDescriptionMethod = pluginClass.getMethod("getDescription");
                Method getAuthorMethod = pluginClass.getMethod("getAuthor");
                
                // 显示插件信息
                System.out.println("已加载插件:");
                System.out.println("- ID: " + getIdMethod.invoke(plugin));
                System.out.println("- 名称: " + getNameMethod.invoke(plugin));
                System.out.println("- 版本: " + getVersionMethod.invoke(plugin));
                System.out.println("- 描述: " + getDescriptionMethod.invoke(plugin));
                System.out.println("- 作者: " + getAuthorMethod.invoke(plugin));
                
                // 简单测试插件的sayHello方法
                System.out.println("\n[5] 调用插件方法...");
                
                try {
                    Method sayHelloMethod = pluginClass.getMethod("sayHello", String.class);
                    String result = (String) sayHelloMethod.invoke(plugin, "测试用户");
                    System.out.println("调用插件方法结果: " + result);
                } catch (Exception e) {
                    System.err.println("无法调用插件方法: " + e.getMessage());
                }
                
                System.out.println("\n[6] 测试完成");
                System.out.println("注意: 由于缺少必要的插件上下文环境，无法完整测试插件生命周期");
                System.out.println("这个测试只验证了插件类能被正确加载，并能创建实例和调用基本方法");
                System.out.println("要进行完整测试，需要与实际的应用程序环境集成");
                
            } catch (ClassNotFoundException e) {
                System.err.println("找不到插件主类: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
EOF

# 创建maven pom.xml文件
mkdir -p "$TEST_DIR/test-app"
cat > "$TEST_DIR/test-app/pom.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.xiaoqu.test</groupId>
    <artifactId>plugin-tester</artifactId>
    <version>1.0.0</version>

    <properties>
        <java.version>17</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>\${java.version}</maven.compiler.source>
        <maven.compiler.target>\${java.version}</maven.compiler.target>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>2.0.9</version>
        </dependency>
        
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.4.14</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>\${java.version}</source>
                    <target>\${java.version}</target>
                </configuration>
            </plugin>
            
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-assembly-plugin</artifactId>
                <version>3.6.0</version>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>com.xiaoqu.test.PluginTester</mainClass>
                        </manifest>
                    </archive>
                    <descriptorRefs>
                        <descriptorRef>jar-with-dependencies</descriptorRef>
                    </descriptorRefs>
                </configuration>
                <executions>
                    <execution>
                        <id>make-assembly</id>
                        <phase>package</phase>
                        <goals>
                            <goal>single</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
EOF

# 创建logback配置
mkdir -p "$TEST_DIR/test-app/src/main/resources"
cat > "$TEST_DIR/test-app/src/main/resources/logback.xml" << EOF
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
    </root>
    
    <logger name="com.xiaoqu" level="DEBUG" />
</configuration>
EOF

echo -e "${GREEN}已创建测试主程序${NC}"

# 编译测试程序
echo -e "\n${YELLOW}[4/5] 编译测试程序...${NC}"
cd "$TEST_DIR/test-app" || exit 1
mvn clean package assembly:single
if [ $? -ne 0 ]; then
  echo -e "${RED}错误: 测试程序编译失败${NC}"
  exit 1
fi
cd - > /dev/null

# 创建运行脚本
TEST_APP_JAR="$TEST_DIR/test-app/target/plugin-tester-1.0.0-jar-with-dependencies.jar"
if [ ! -f "$TEST_APP_JAR" ]; then
  echo -e "${RED}错误: 测试程序JAR文件未找到${NC}"
  exit 1
fi

echo -e "\n${YELLOW}[5/5] 创建运行脚本...${NC}"
cat > "$TEST_DIR/run-test.sh" << EOF
#!/bin/bash
cd "\$(dirname "\$0")"
java -jar test-app/target/plugin-tester-1.0.0-jar-with-dependencies.jar
EOF
chmod +x "$TEST_DIR/run-test.sh"

echo -e "${GREEN}测试环境创建完成${NC}"
echo -e "\n${YELLOW}运行以下命令开始测试:${NC}"
echo -e "${GREEN}cd $TEST_DIR && ./run-test.sh${NC}" 