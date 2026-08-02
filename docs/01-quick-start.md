# 快速开始指南

## 环境要求

| 工具 | 版本 | 说明 |
|---|---|---|
| JDK | 21+ | 建议用 Eclipse Temurin 或 Oracle OpenJDK |
| Maven | 3.9+ | 构建工具 |
| MySQL | 8.0+ | 关系数据库 |
| Redis | 6.0+ | 缓存 + Sa-Token 会话 |
| IDEA | 2024+ | 推荐 IntelliJ IDEA（安装 Lombok + MyBatisX 插件） |

## 项目初始化

### 第一步：创建根 POM

创建项目根目录 `nova-fs/`，生成根 `pom.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>io.novafs</groupId>
    <artifactId>nova-fs</artifactId>
    <version>${revision}</version>
    <packaging>pom</packaging>

    <modules>
        <module>fs-dependencies</module>
        <module>fs-framework</module>
        <module>fs-modules</module>
        <module>fs-admin</module>
    </modules>

    <properties>
        <revision>1.0.0-SNAPSHOT</revision>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <skipTests>true</skipTests>
        <spring-boot.version>3.4.4</spring-boot.version>
        <mybatis-flex.version>1.11.6</mybatis-flex.version>
        <sa-token.version>1.45.0</sa-token.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <version>${spring-boot.version}</version>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

### 第二步：创建 BOM 模块

```bash
mkdir fs-dependencies
```

`fs-dependencies/pom.xml`：集中管理所有第三方依赖版本。

### 第三步：创建基础模块

```bash
# 基础设施模块（先建前 3 个）
mkdir -p fs-framework/fs-common-core
mkdir -p fs-framework/fs-orm
mkdir -p fs-framework/fs-security

# 业务模块（先建 1 个）
mkdir -p fs-modules/fs-system

# 启动模块
mkdir -p fs-admin
```

然后按顺序创建每个子模块的 `pom.xml`（相互依赖关系见下方）。

### 第四步：创建启动入口

```java
// fs-admin/src/main/java/com/xddcodec/fs/FsAdminApplication.java
package io.novafs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NovaApplication {
    public static void main(String[] args) {
        SpringApplication.run(NovaApplication.class, args);
    }
}
```

### 第五步：配置 application.yml

```yaml
# fs-admin/src/main/resources/application.yml
server:
  port: 8080

spring:
  application:
    name: nova-fs
  datasource:
    url: jdbc:mysql://localhost:3306/free_fs?useUnicode=true&characterEncoding=utf-8
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  data:
    redis:
      host: localhost
      port: 6379
      database: 0

sa-token:
  token-name: Authorization
  timeout: 2592000   # 30天
  activity-timeout: -1
  is-concurrent: true
  is-share: false
  token-style: uuid
  is-log: false
```

## 模块依赖关系

```
fs-admin
  ├── fs-system
  │     ├── fs-common-core
  │     ├── fs-orm
  │     ├── fs-security
  │     ├── fs-redis
  │     └── fs-notify
  ├── fs-storage
  │     ├── fs-common-core
  │     ├── fs-orm
  │     └── storage-plugin-boot
  ├── fs-file
  │     ├── fs-common-core
  │     ├── fs-orm
  │     ├── fs-redis
  │     ├── fs-sse
  │     ├── fs-preview
  │     └── fs-storage
  ├── fs-log
  │     ├── fs-common-core
  │     └── fs-orm
  ├── fs-swagger
  ├── fs-security
  ├── fs-redis
  ├── fs-sse
  ├── fs-notify
  └── fs-preview
```

## 常用 Maven 命令

```bash
# 编译所有模块
mvn clean install -DskipTests

# 单独编译某个模块
mvn clean install -pl fs-admin -am

# 运行
mvn spring-boot:run -pl fs-admin

# 打包
mvn clean package -DskipTests

# 运行打好的包
java -jar fs-admin/target/fs-admin-1.0.0-SNAPSHOT.jar
```

## IDEA 配置建议

1. **安装插件**：
   - Lombok（必装）
   - MyBatisX（MyBatis-Flex 支持）
   - Maven Helper（依赖分析）

2. **开启注解处理器**：
   - Settings → Build → Compiler → Annotation Processors → 勾选 "Enable annotation processing"

3. **运行配置**：
   - 创建 Spring Boot 运行配置，Main class: `io.novafs.NovaApplication`

## 验证清单

启动应用后，验证以下端点：

```bash
# 健康检查
curl http://localhost:8080/api/health
# 期望: {"code":200,"msg":"ok","data":{"status":"UP"}}

# Swagger 文档
# 浏览器打开 http://localhost:8080/swagger-ui/index.html

# 注册
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456","email":"admin@example.com"}'

# 登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
# 返回: {"code":200,"data":{"token":"xxxx-xxxx-xxxx"}}
```
