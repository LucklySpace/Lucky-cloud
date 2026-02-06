# im-leaf 分布式 ID 生成服务

## 项目简介

im-leaf 是一个高性能、高可用的分布式 ID 生成服务，基于美团 Leaf 算法进行优化和扩展。该服务提供多种 ID 生成策略，支持 REST
API 和 Dubbo RPC 两种调用方式，适用于微服务架构下的全局唯一 ID 生成场景。

## 核心特性

### 多种 ID 生成策略

- **Snowflake（雪花算法）**：基于时间戳、机器 ID 和序列号的分布式 ID 生成算法
    - 64 位长整型 ID
    - 高性能无锁实现（使用 CAS 操作）
    - 支持时钟回拨处理（最大 5 秒）
    - 每毫秒可生成 4096 个 ID
    - 支持最多 1024 个工作节点

- **Redis Segment（号段模式）**：基于 Redis 的号段分配策略
    - 双缓冲区设计，提高并发性能
    - 异步预加载机制，减少等待时间
    - 本地缓存和文件持久化，提高可靠性
    - 基于分布式锁的并发安全保证

- **UUID**：标准 UUID 生成
    - 128 位唯一标识符
    - 全局唯一性保证

- **UID**：自定义 UID 生成策略
    - 灵活的 ID 格式定制

### 技术亮点

- **响应式编程**：基于 Spring WebFlux，支持高并发异步处理
- **分布式架构**：支持多节点部署，通过 Nacos 实现服务注册与发现
- **高可用性**：
    - 时钟回拨处理机制
    - 本地缓存 + Redis 双重保障
    - 文件持久化，支持快速恢复
- **高性能**：
    - 无锁 CAS 操作
    - 异步预加载
    - 双缓冲区设计
- **可观测性**：集成 Sentinel 流量控制、Actuator 健康检查

## 模块结构

```
im-leaf/
├── im-leaf-domain/           # 领域模型模块
│   └── pom.xml
├── im-leaf-rpc-api/          # RPC API 模块（Dubbo 服务接口）
│   ├── src/main/java/com/xy/lucky/api/
│   │   └── ImIdDubboService.java
│   └── pom.xml
├── im-leaf-service/          # 服务实现模块
│   ├── src/main/java/com/xy/lucky/leaf/
│   │   ├── config/          # 配置类
│   │   │   ├── MyRedisSerializationContext.java
│   │   │   ├── NacosSnowflakeWorkerIdAllocator.java
│   │   │   └── RedisConfig.java
│   │   ├── controller/      # REST API 控制器
│   │   │   └── IdController.java
│   │   ├── core/            # 核心 ID 生成逻辑
│   │   │   ├── IDGen.java
│   │   │   ├── IdType.java
│   │   │   └── impl/
│   │   │       ├── SnowflakeIDGenImpl.java
│   │   │       ├── RedisSegmentIDGenImpl.java
│   │   │       ├── UIDGenImpl.java
│   │   │       └── UuidGenImpl.java
│   │   ├── exception/       # 异常处理
│   │   │   └── handler/
│   │   │       └── GlobalExceptionHandler.java
│   │   ├── model/           # 数据模型
│   │   │   ├── IdMetaInfo.java
│   │   │   ├── IdRingBuffer.java
│   │   │   └── Segment.java
│   │   ├── repository/      # 数据访问层
│   │   │   └── IdMetaInfoRepository.java
│   │   ├── service/         # 业务服务层
│   │   │   └── IdService.java
│   │   ├── utils/           # 工具类
│   │   │   └── StrategyContext.java
│   │   ├── work/            # Worker ID 分配器
│   │   │   └── WorkerIdAssigner.java
│   │   └── ImLeafApplication.java
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-dev.yml
│       ├── application-prod.yml
│       └── application-undertow.yml
└── pom.xml
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- Redis 5.0+
- PostgreSQL 12+ 或 MySQL 8.0+
- Nacos 2.x

### 配置说明

#### 1. 数据库配置

创建 `id_meta_info` 表：

```sql
CREATE TABLE id_meta_info (
    id VARCHAR(64) PRIMARY KEY,
    max_id BIGINT NOT NULL,
    step INT NOT NULL,
    update_time TIMESTAMP NOT NULL,
    version INT NOT NULL
);
```

#### 2. Redis 配置

在 `application-dev.yml` 或 `application-prod.yml` 中配置 Redis 连接信息。

#### 3. Nacos 配置

确保 Nacos 服务正常运行，并配置好服务注册地址。

#### 4. 应用配置

```yaml
generate:
  step: 1000                    # Redis Segment 默认步长
  initialId: 0                  # 初始 ID
  prefetchThreshold: 0.2        # 预加载阈值（剩余 ID 比例）
  lockWaitSeconds: 5            # 分布式锁等待时间（秒）
  lockLeaseSeconds: 60          # 分布式锁租约时间（秒）
```

### 启动服务

```bash
# 编译项目
mvn clean install

# 启动服务（开发环境）
cd im-leaf-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 启动服务（生产环境）
java -jar im-leaf-service.jar --spring.profiles.active=prod
```

### 使用示例

#### REST API 调用

**生成单个 ID**

```bash
curl "http://localhost:8080/api/generator/id?type=snowflake&key=order"
```

响应示例：

```json
{
  "metaId": 1234567890123456789,
  "stringId": "1234567890123456789",
  "longId": 1234567890123456789
}
```

**批量生成 ID**

```bash
curl "http://localhost:8080/api/generator/ids?type=snowflake&key=order&count=10"
```

响应示例：

```json
[
  {
    "metaId": 1234567890123456789,
    "stringId": "1234567890123456789",
    "longId": 1234567890123456789
  },
  {
    "metaId": 1234567890123456790,
    "stringId": "1234567890123456790",
    "longId": 1234567890123456790
  }
]
```

#### Dubbo RPC 调用

**服务引用配置**

```yaml
dubbo:
  consumer:
    timeout: 5000
    retries: 3
  reference:
    com.xy.lucky.rpc.api.leaf.ImIdDubboService:
      url: dubbo://localhost:20880
```

**Java 客户端调用**

```java
import com.xy.lucky.rpc.api.leaf.ImIdDubboService;
import com.xy.lucky.core.model.IMetaId;
import org.apache.dubbo.config.annotation.DubboReference;

public class IdConsumer {

    @DubboReference
    private ImIdDubboService idDubboService;

    public void generateId() {
        // 生成单个 ID
        IMetaId id = idDubboService.generateId("snowflake", "order");
        System.out.println("Generated ID: " + id.getLongId());

        // 批量生成 ID
        List<IMetaId> ids = idDubboService.generateIds("snowflake", "order", 10);
        ids.forEach(metaId -> System.out.println("ID: " + metaId.getLongId()));

        // 类型安全的方式获取 ID
        Long userId = idDubboService.getId("redis", "user", Long.class);
        System.out.println("User ID: " + userId);
    }
}
```

## ID 生成策略详解

### 1. Snowflake（雪花算法）

**算法原理**

```
0 | 00011001001001001001010101000110 | 0001000001 | 000000000000
↑ |               ↑                   |      ↑     |       ↑
符号位 |        41 位时间戳            | 10 位工作ID | 12 位序列号
```

- **时间戳**（41 位）：毫秒级精度，从 2021-06-01 00:00:00 开始，可用约 69 年
- **工作节点 ID**（10 位）：支持 1024 个节点，通过 Nacos 自动分配
- **序列号**（12 位）：每毫秒可生成 4096 个 ID

**特性**

- 高性能：基于 CAS 的无锁实现
- 时钟回拨处理：
    - 小回拨（< 5 秒）：自旋等待
    - 大回拨（> 5 秒）：抛出异常
- 趋势递增：按时间递增，有利于数据库索引

**适用场景**

- 需要高性能 ID 生成的场景
- 可以接受短时间 ID 不连续
- 分布式系统中的订单 ID、消息 ID 等

### 2. Redis Segment（号段模式）

**工作原理**

```
数据库/Redis: [1-1000] [1001-2000] [2001-3000] ...
                  ↓
              预加载到内存
                  ↓
           +-------------+
           |  Current    |  ← 正在使用的号段
           |  [1-1000]   |
           +-------------+
           |  Next       |  ← 预加载的备用号段
           |  [1001-2000]|
           +-------------+
```

**特性**

- 双缓冲区：当前号段用尽时，无缝切换到预加载的备用号段
- 异步加载：后台线程提前加载下一号段
- 本地缓存：内存中缓存号段，减少 Redis 访问
- 文件持久化：定期将号段状态持久化到文件，支持快速恢复
- 分布式锁：使用 Redisson 实现分布式锁，保证多节点安全

**适用场景**

- 需要 ID 连续的场景
- 对性能要求不是极端高
- 可以容忍少量 ID 浪费

### 3. UUID

**特性**

- 标准 UUID v4
- 128 位全局唯一标识符
- 无需中心化协调

**适用场景**

- 需要真正随机的 ID
- 不关心 ID 长度和可读性
- 本地临时数据标识

### 4. UID

**特性**

- 自定义 ID 格式
- 灵活的业务前缀支持
- 可配置的编码规则

**适用场景**

- 需要特定格式的 ID
- 业务前缀要求
- ID 可读性要求

## 性能指标

### 单机性能

| 策略            | QPS   | 延迟（P99） | 并发数   |
|---------------|-------|---------|-------|
| Snowflake     | 300W+ | < 1ms   | 16 线程 |
| Redis Segment | 100W+ | < 5ms   | 16 线程 |
| UUID          | 50W+  | < 2ms   | 16 线程 |

### 集群性能

- 支持 1024 个节点水平扩展
- 线性扩展能力
- 通过 Nacos 自动管理节点 ID

## 监控与运维

### 健康检查

```bash
# 查看服务健康状态
curl http://localhost:8080/actuator/health

# 查看详细信息
curl http://localhost:8080/actuator/health/detail
```

### 监控指标

- ID 生成成功率
- ID 生成延迟（P50、P99、P999）
- 号段加载次数
- Redis 连接状态
- Worker ID 分配状态

### 日志

```yaml
logging:
  level:
    com.xy.lucky.leaf: INFO
    com.xy.lucky.leaf.core: DEBUG
```

### 常见问题

**Q: Snowflake 算法时钟回拨怎么办？**

A: 系统会自动处理：

- 小回拨（< 5 秒）：自旋等待时钟恢复
- 大回拨（> 5 秒）：抛出异常，建议检查系统时间并重启服务

**Q: Redis Segment 模式号段耗尽怎么办？**

A: 系统会自动加载新号段：

- 当前号段剩余 < 20% 时，触发异步预加载
- 号段耗尽时，立即切换到备用号段
- 如果备用号段也未就绪，会短暂等待（最多 200 次重试）

**Q: 如何保证 ID 不重复？**

A: 多重保障：

- Snowflake：通过时间戳 + WorkerID + 序列号保证
- Redis Segment：通过 Redis 原子递增 + 分布式锁保证
- 单节点：通过无锁 CAS 操作保证

**Q: 服务重启后会丢失 ID 吗？**

A: 不会：

- Snowflake：基于时间戳，重启不影响
- Redis Segment：从文件缓存快速恢复，Redis 中记录了已分配的最大 ID

## 技术栈

- **框架**：Spring Boot 3.x、Spring Cloud 2022.x
- **响应式**：Spring WebFlux、Project Reactor
- **服务注册**：Nacos
- **分布式锁**：Redisson
- **数据库**：PostgreSQL / MySQL + JPA
- **缓存**：Redis（Reactive）
- **RPC**：Dubbo 3.x
- **熔断降级**：Sentinel、Resilience4j
- **监控**：Spring Boot Actuator
- **构建工具**：Maven

## 版本历史

### v1.0.0（当前版本）

- 实现 Snowflake 算法（高性能无锁版本）
- 实现 Redis Segment 号段模式
- 支持 UUID 和 UID 生成策略
- 集成 Dubbo RPC 服务
- 集成 Nacos 服务注册与发现
- 集成 Sentinel 流量控制
- 支持文件持久化和快速恢复

## 贡献指南

欢迎提交 Issue 和 Pull Request！

## 许可证

Apache License 2.0

## 联系方式

- 项目地址：https://github.com/your-org/Lucky-cloud/tree/main/im-leaf
- 问题反馈：https://github.com/your-org/Lucky-cloud/issues
