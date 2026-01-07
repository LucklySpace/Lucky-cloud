# im-spring 配置文档

## 概述

im-spring 模块提供了类似 Spring Boot 的配置管理功能，支持 YAML 配置文件、多环境 Profile、配置属性绑定等特性。

---

## 配置文件

### 配置文件位置

配置文件需放置在 `src/main/resources/` 目录下：

```
src/main/resources/
├── application.yml           # 主配置文件（必需）
├── application-dev.yml       # 开发环境配置（可选）
├── application-test.yml      # 测试环境配置（可选）
└── application-prod.yml      # 生产环境配置（可选）
```

### 配置加载顺序

配置按以下优先级加载（后加载的会覆盖先加载的）：

1. `application.yml` - 基础配置
2. `application-{profile}.yml` - Profile 特定配置
3. 命令行参数 `--key=value`

---

## Profile 配置

### 激活 Profile

**方式一：配置文件中指定**

```yaml
spring:
  profiles:
    active: dev
```

**方式二：命令行参数**

```bash
java -jar app.jar --spring.profiles.active=prod
```

### Profile 配置文件示例

**application.yml（主配置）**

```yaml
app:
  name: my-app

spring:
  profiles:
    active: dev
```

**application-dev.yml（开发环境）**

```yaml
redis:
  host: localhost
  port: 6379
```

**application-prod.yml（生产环境）**

```yaml
redis:
  host: redis.prod.example.com
  port: 6379
  password: ${REDIS_PASSWORD}
```

---

## 配置属性绑定

### @ConfigurationProperties 注解

将配置绑定到 Java 类，推荐用于复杂配置场景。

**定义配置类：**

```java
import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.boot.annotation.ConfigurationProperties;
import com.xy.lucky.spring.boot.annotation.NestedConfigurationProperty;
import lombok.Data;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "netty.config")
public class NettyProperties {

    // 简单属性
    private String protocol = "proto";
    private int heartBeatTime = 30000;
    private int bossThreadSize = 4;
    private int workThreadSize = 16;

    // 嵌套配置
    @NestedConfigurationProperty
    private TcpConfig tcp = new TcpConfig();

    @NestedConfigurationProperty
    private WebSocketConfig websocket = new WebSocketConfig();

    @Data
    public static class TcpConfig {
        private boolean enable = false;
        private List<Integer> port;
    }

    @Data
    public static class WebSocketConfig {
        private boolean enable = true;
        private String path = "/im";
        private List<Integer> port;
    }
}
```

**对应 YAML 配置：**

```yaml
netty:
  config:
    protocol: proto
    heartBeatTime: 30000
    bossThreadSize: 4
    workThreadSize: 16
    tcp:
      enable: false
      port:
        - 9000
        - 9001
    websocket:
      enable: true
      path: /im
      port:
        - 19000
        - 19001
```

**使用配置类：**

```java

@Component
public class NettyServer {

    @Autowired
    private NettyProperties nettyProperties;

    @PostConstruct
    public void init() {
        if (nettyProperties.getWebsocket().isEnable()) {
            List<Integer> ports = nettyProperties.getWebsocket().getPort();
            // 启动 WebSocket 服务...
        }
    }
}
```

### @ConfigurationProperties 参数

| 参数                    | 说明       | 默认值     |
|-----------------------|----------|---------|
| `prefix`              | 配置前缀     | `""`    |
| `value`               | 配置前缀（别名） | `""`    |
| `ignoreInvalidFields` | 忽略无效字段   | `false` |
| `ignoreUnknownFields` | 忽略未知字段   | `true`  |

### @NestedConfigurationProperty 注解

用于标记嵌套的配置属性类，确保递归绑定：

```java

@Data
@ConfigurationProperties(prefix = "server")
public class ServerProperties {

    @NestedConfigurationProperty
    private SslConfig ssl = new SslConfig();

    @Data
    public static class SslConfig {
        private boolean enabled = false;
        private String keyStore;
        private String keyStorePassword;
    }
}
```

---

## @Value 注解

用于注入单个配置值，适合简单场景。

### 基本用法

```java

@Component
public class MyService {

    @Value("${app.name}")
    private String appName;

    @Value("${server.port}")
    private int port;

    @Value("${feature.enabled}")
    private boolean enabled;
}
```

### 默认值

使用 `:` 指定默认值：

```java

@Value("${app.name:defaultName}")
private String appName;

@Value("${server.port:8080}")
private int port;

@Value("${feature.enabled:false}")
private boolean enabled;
```

### 支持的类型

- `String`
- `int` / `Integer`
- `long` / `Long`
- `boolean` / `Boolean`
- `double` / `Double`
- `float` / `Float`

---

## 支持的 YAML 语法

### 基本类型

```yaml
app:
  name: my-application
  port: 8080
  enabled: true
  timeout: 30.5
```

### 列表（List）

**方式一：YAML 数组语法**

```yaml
server:
  ports:
    - 8080
    - 8081
    - 8082
```

**方式二：行内数组**

```yaml
server:
  ports: [ 8080, 8081, 8082 ]
```

### 嵌套对象

```yaml
database:
  master:
    host: localhost
    port: 3306
  slave:
    host: slave.db.com
    port: 3306
```

### 多行字符串

```yaml
app:
  description: |
    这是一个多行文本
    保留换行符
```

---

## 占位符

### 标准占位符 `${}`

支持 `${key}` 和 `${key:default}` 语法，从配置和环境变量中读取：

```yaml
app:
  name: my-app
  displayName: ${app.name}-service

server:
  port: ${SERVER_PORT:8080}
```

### 环境变量占位符 `@@`

支持 `@key@` 和 `@key:default@` 语法，**优先从环境变量读取**：

```yaml
# 从环境变量 REDIS_HOST 读取，默认 localhost
redis:
  host: @REDIS_HOST:localhost@
  port: @REDIS_PORT:6379@
  password: @REDIS_PASSWORD@

# 支持自动转换格式：redis.password -> REDIS_PASSWORD
rabbitmq:
  username: @rabbitmq.username:guest@
  password: @RABBITMQ_PASSWORD:guest@
```

### 两种占位符对比

| 语法       | 说明           | 查找顺序               |
|----------|--------------|--------------------|
| `${key}` | 标准 Spring 风格 | 配置文件 → 系统属性 → 环境变量 |
| `@key@`  | 环境变量风格       | 环境变量 → 系统属性        |

### Docker 环境示例

**Dockerfile：**

```dockerfile
ENV REDIS_HOST=redis
ENV REDIS_PORT=6379
ENV RABBITMQ_PASSWORD=secret
```

**application.yml：**

```yaml
redis:
  host: @REDIS_HOST:localhost@
  port: @REDIS_PORT:6379@

rabbitmq:
  password: @RABBITMQ_PASSWORD:guest@
```

**docker run 命令：**

```bash
docker run -d \
  -e REDIS_HOST=redis.prod.com \
  -e REDIS_PORT=6380 \
  -e RABBITMQ_PASSWORD=prod_secret \
  my-app:1.0.0
```

### Key 格式自动转换

`@@` 占位符会自动尝试以下格式：

| 配置中的 Key      | 尝试的环境变量                       |
|---------------|-------------------------------|
| `redis.host`  | `redis.host` → `REDIS_HOST`   |
| `my-property` | `my-property` → `MY_PROPERTY` |
| `server.port` | `server.port` → `SERVER_PORT` |

---

## Banner 配置

### 自定义 Banner 文件

支持从 classpath 或文件系统加载自定义 Banner。

**配置示例：**

```yaml
spring:
  banner:
    # Banner 文件位置
    # classpath: 从类路径加载
    # file: 从文件系统加载
    location: classpath:banner.txt
    # 字符编码（可选，默认 UTF-8）
    charset: UTF-8
  main:
    # Banner 显示模式: off, console, log
    banner-mode: console
```

### Banner 文件示例

创建 `src/main/resources/banner.txt`：

```
  _                _          
 | |    _   _  ___| | ___   _ 
 | |   | | | |/ __| |/ / | | |
 | |___| |_| | (__|   <| |_| |
 |_____|\__,_|\___|_|\_\\__, |
                        |___/ 
 :: ${application.name} :: v${app.version:1.0.0}
```

### 支持的占位符

Banner 文件中可使用以下占位符：

| 占位符                         | 说明                   |
|-----------------------------|----------------------|
| `${application.name}`       | 应用名称（主类名）            |
| `${application.title}`      | 应用标题（同上）             |
| `${spring.profiles.active}` | 激活的 Profile          |
| `${任意配置key}`                | 从 Environment 获取的配置值 |
| `${key:default}`            | 带默认值的占位符             |

### Banner 模式

| 模式        | 说明         |
|-----------|------------|
| `console` | 输出到控制台（默认） |
| `log`     | 输出到日志      |
| `off`     | 关闭 Banner  |

### 关闭 Banner

```yaml
spring:
  main:
    banner-mode: off
```

### 代码方式设置 Banner

```java
public static void main(String[] args) {
    SpringApplication app = new SpringApplication(MyApplication.class);
    // 关闭 Banner
    app.setBannerMode(Banner.Mode.OFF);
    // 或设置自定义 Banner
    app.setBanner((env, sourceClass, out) -> {
        out.println("=== My Custom Banner ===");
    });
    app.run(args);
}
```

---

## 完整示例

### 配置文件 (application.yml)

```yaml
# ===========================================
# 应用基础配置
# ===========================================
app:
  name: im-connect
  version: 1.0.0

# ===========================================
# Profile 配置
# ===========================================
spring:
  profiles:
    active: dev

# ===========================================
# RabbitMQ 配置
# ===========================================
rabbitmq:
  address: localhost
  port: 5672
  username: guest
  password: guest
  virtual: /
  exchange: IM-SERVER
  routingKeyPrefix: IM-
  errorQueue: im.error

# ===========================================
# Nacos 配置
# ===========================================
nacos:
  enable: true
  config:
    name: im-connect
    address: localhost
    port: 8848
    group: DEFAULT_GROUP
    username: nacos
    password: nacos

# ===========================================
# Netty 配置
# ===========================================
netty:
  config:
    protocol: proto
    heartBeatTime: 30000
    bossThreadSize: 4
    workThreadSize: 16
    tcp:
      enable: false
      port:
        - 9000
        - 9001
    websocket:
      enable: true
      path: /im
      port:
        - 19000
        - 19001

# ===========================================
# Redis 配置
# ===========================================
redis:
  host: localhost
  port: 6379
  password: ""
  timeout: 10000
  database: 0
```

### 配置类定义

```java
// RabbitMQ 配置类
@Data
@Component
@ConfigurationProperties(prefix = "rabbitmq")
public class RabbitMQProperties {
    private String address;
    private int port = 5672;
    private String username = "guest";
    private String password = "guest";
    private String virtual = "/";
    private String exchange;
    private String routingKeyPrefix;
    private String errorQueue;
}

// Redis 配置类
@Data
@Component
@ConfigurationProperties(prefix = "redis")
public class RedisProperties {
    private String host;
    private int port = 6379;
    private String password;
    private int timeout = 10000;
    private int database = 0;
}
```

---

## 启动应用

```java

@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

### 命令行参数覆盖配置

```bash
java -jar app.jar --spring.profiles.active=prod --server.port=9090
```

---

## 注意事项

1. **字段名匹配**：YAML 中的 key 需与 Java 字段名一致（支持驼峰）
2. **默认值**：在配置类中设置默认值，确保未配置时有合理的默认行为
3. **嵌套配置**：复杂嵌套对象需要使用 `@NestedConfigurationProperty` 注解
4. **类型转换**：框架自动处理基本类型转换，复杂类型需确保有无参构造函数
5. **列表类型**：支持 `List<Integer>`、`List<String>` 等泛型列表

