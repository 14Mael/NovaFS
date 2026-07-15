# 🚀 第一阶段任务清单（P0 — 基础设施搭建）

你现在就可以开始写了。以下是**从零到跑通第一个请求**的具体步骤，按顺序执行。

---

## 📋 任务列表

### 任务 0.1：创建项目根目录和根 POM

```bash
mkdir nova-fs
cd nova-fs
```

在根目录创建 `pom.xml`，内容参考 `docs/01-quick-start.md` 中的根 POM。

**关键点**：
- `<packaging>pom</packaging>`
- 声明 4 个子模块：`fs-dependencies`、`fs-framework`、`fs-modules`、`fs-admin`
- 版本用 `${revision}` = `1.0.0-SNAPSHOT`
- Java 版本 21

---

### 任务 0.2：创建 BOM 模块（fs-dependencies）

```bash
mkdir fs-dependencies
```

创建 `fs-dependencies/pom.xml`，管理所有三方依赖版本：

```xml
<groupId>io.novafs</groupId>
<artifactId>fs-dependencies</artifactId>
<version>${revision}</version>
<packaging>pom</packaging>
```

在 `<dependencyManagement>` 中声明以下依赖的版本：

| 依赖 | 版本 |
|---|---|
| `spring-boot-dependencies` | 4.0.3 |
| `mybatis-flex-spring-boot4-starter` | 1.11.6 |
| `sa-token-spring-boot4-starter` | 1.45.0 |
| `sa-token-jwt` | 1.45.0 |
| `sa-token-redis-jackson` | 1.45.0 |
| `mapstruct-plus-spring-boot-starter` | 1.5.0 |
| `hutool-all` | 5.8.28 |
| `springdoc-openapi-starter-webmvc-ui` | 3.0.2 |
| `caffeine` | 3.2.3 |

---

### 任务 0.3：创建公共核心模块（fs-common-core）

```bash
mkdir -p fs-framework/fs-common-core/src/main/java/com/xddcodec/fs/framework/common
```

创建 `pom.xml`，依赖：无（最底层模块）。

**需要创建的类**：

```
fs-common-core/src/main/java/com/xddcodec/fs/framework/common/
├── model/
│   ├── Result.java             ← 统一返回结果
│   ├── PageQuery.java          ← 分页查询参数
│   └── PageResult.java         ← 分页返回结果
├── exception/
│   ├── BaseException.java      ← 业务异常基类
│   ├── GlobalExceptionHandler.java ← @RestControllerAdvice
│   └── ErrorCode.java          ← 错误码枚举
├── constant/
│   └── CommonConstant.java     ← 通用常量
└── util/
    ├── JsonUtils.java          ← JSON 工具类（基于 Jackson）
    └── SpringUtils.java        ← Spring 上下文工具
```

**关键代码：Result.java**

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "ok", data);
    }

    public static <T> Result<T> fail(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    public static <T> Result<T> fail(String msg) {
        return new Result<>(500, msg, null);
    }
}
```

**关键代码：BaseException.java**

```java
@Getter
public class BaseException extends RuntimeException {
    private final int code;

    public BaseException(int code, String message) {
        super(message);
        this.code = code;
    }
}
```

---

### 任务 0.4：创建 ORM 模块（fs-orm）

```bash
mkdir -p fs-framework/fs-orm/src/main/java/com/xddcodec/fs/framework/orm
```

`pom.xml` 依赖：`fs-common-core`、`mybatis-flex-spring-boot4-starter`、`spring-boot-starter-jdbc`、`mysql-connector-j`（runtime）

**需要创建的类**：

```
fs-orm/src/main/java/com/xddcodec/fs/framework/orm/
├── base/
│   └── BaseEntity.java           ← 基础实体（id, createdAt, updatedAt）
├── config/
│   └── MyBatisFlexAutoConfig.java ← MyBatis-Flex 自动配置
└── handler/
    └── JsonStringTypeHandler.java ← JSON 类型处理器
```

**BaseEntity.java 示例：**

```java
@Getter
@Setter
public class BaseEntity {
    @Id(keyType = KeyType.Auto)
    private Long id;

    @Column(value = "created_at", onInsertValue = "NOW()")
    private LocalDateTime createdAt;

    @Column(value = "updated_at", onInsertValue = "NOW()", onUpdateValue = "NOW()")
    private LocalDateTime updatedAt;
}
```

---

### 任务 0.5：创建启动模块（fs-admin）

```bash
mkdir -p fs-admin/src/main/java/com/xddcodec/fs
mkdir -p fs-admin/src/main/resources
mkdir -p fs-admin/src/test/java/com/xddcodec/fs
```

`pom.xml` 依赖：
- `fs-common-core`
- `fs-orm`
- `spring-boot-starter-web`
- `spring-boot-starter-test`（test）

**创建启动类：**

```java
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

**创建配置文件：**

```yaml
# application.yml
server:
  port: 8080

spring:
  application:
    name: nova-fs
  datasource:
    url: jdbc:mysql://localhost:3306/free_fs?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis-flex:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

---

### 任务 0.6：创建测试 Controller 验证链路

在 `fs-admin` 中创建：

```java
package io.novafs.controller;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Result<Map<String, String>> health() {
        return Result.ok(Map.of(
            "status", "UP",
            "version", "1.0.0"
        ));
    }
}
```

---

### 任务 0.7：编译并运行

```bash
# 编译
mvn clean install -DskipTests

# 运行
mvn spring-boot:run -pl fs-admin

# 验证
curl http://localhost:8080/api/health
# 期望: {"code":200,"msg":"ok","data":{"status":"UP","version":"1.0.0"}}
```

---

## 🎯 验收标准

- [ ] `mvn clean install` 编译**零错误**
- [ ] Spring Boot 启动日志正常，无异常
- [ ] `curl http://localhost:8080/api/health` 返回正确的 JSON
- [ ] 能连接到 MySQL 数据库（虽然还没建表）

---

## 📁 完成后的目录结构

```
nova-fs/
├── pom.xml                          # 根 POM
├── fs-dependencies/
│   └── pom.xml                      # BOM 依赖管理
├── fs-framework/
│   ├── pom.xml                      # 聚合 POM（引用子模块）
│   ├── fs-common-core/
│   │   ├── pom.xml
│   │   └── src/main/java/.../
│   │       ├── model/Result.java
│   │       ├── exception/BaseException.java
│   │       └── ...
│   └── fs-orm/
│       ├── pom.xml
│       └── src/main/java/.../
│           ├── base/BaseEntity.java
│           └── config/MyBatisFlexAutoConfig.java
├── fs-modules/
│   └── pom.xml                      # 聚合 POM（先空着）
├── fs-admin/
│   ├── pom.xml
│   └── src/main/java/.../
│       ├── FsAdminApplication.java
│       └── controller/HealthController.java
├── AGENTS.md                        # 已创建
├── ARCHITECTURE.md                  # 已创建
├── ROADMAP.md                       # 已创建
└── docs/
    ├── 01-quick-start.md
    ├── 02-database-design.md
    ├── 03-storage-plugin.md
    └── 04-chunk-upload.md
```

## 第一阶段完成后，下一步做什么？

P0 完成后，进入 **P1 — 用户认证系统**：

1. 引入 Sa-Token，创建 `fs-security` 模块
2. 执行 SQL 建表（`sys_user`、`sys_role`、`sys_permission` 等）
3. 实现注册 API（密码用 BCrypt）
4. 实现登录 API（返回 Token）
5. 配置 Sa-Token 拦截器

详细的任务清单已在 `ROADMAP.md` 中列出，每完成一阶段回来查看下一阶段。

---

**准备好了吗？从任务 0.1 开始，一个一个做，遇到问题随时问我。** 🚀
