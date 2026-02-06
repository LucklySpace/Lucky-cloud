# im-logging-rpc-api

## 模块概述

`im-logging-rpc-api` 是 Lucky Cloud 日志服务的 RPC API 模块，定义了日志服务的 Dubbo RPC 接口规范。该模块作为日志服务的 API
定义层，为所有需要调用日志服务的微服务提供统一的接口契约。

### 核心功能

本模块提供三大核心服务接口：

1. **日志查询服务** (`LogQueryDubboService`) - 支持多维度日志查询与统计
2. **日志采集服务** (`LogIngestDubboService`) - 提供高性能日志采集接口
3. **日志分析服务** (`LogAnalysisDubboService`) - 提供日志统计分析能力

### 技术特性

- 基于 Dubbo 3.x 的 RPC 通信
- 支持 Spring Boot 3.x
- 使用 JDK 17+
- 提供 OpenAPI 3.0 规范文档
- 完整的类型安全接口定义

---

## 模块架构

```
im-logging-rpc-api/
├── src/main/java/com/xy/lucky/logging/rpc/api/
│   ├── query/                    # 日志查询接口
│   │   └── LogQueryDubboService.java
│   ├── ingest/                   # 日志采集接口
│   │   └── LogIngestDubboService.java
│   ├── analysis/                 # 日志分析接口
│   │   └── LogAnalysisDubboService.java
│   ├── dto/                      # 数据传输对象
│   │   ├── LogQueryDto.java
│   │   └── PageResult.java
│   ├── vo/                       # 视图对象
│   │   └── LogRecordVo.java
│   └── enums/                    # 枚举定义
│       └── LogLevel.java
└── src/main/resources/
    └── openapi/                  # OpenAPI 规范
        └── logging-rpc-api.yaml
```

---

## 快速开始

### 1. 添加 Maven 依赖

在调用服务的 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>com.xy.lucky</groupId>
    <artifactId>im-logging-rpc-api</artifactId>
    <version>${revision}</version>
</dependency>
```

### 2. 配置 Dubbo 引用

在 Spring Boot 应用中通过 `@DubboReference` 注入服务：

```java
import com.xy.lucky.rpc.api.logging.query.LogQueryDubboService;
import com.xy.lucky.rpc.api.logging.ingest.LogIngestDubboService;
import com.xy.lucky.rpc.api.logging.analysis.LogAnalysisDubboService;
import org.apache.dubbo.config.annotation.DubboReference;

@Service
public class YourService {

    @DubboReference
    private LogQueryDubboService logQueryService;

    @DubboReference
    private LogIngestDubboService logIngestService;

    @DubboReference
    private LogAnalysisDubboService logAnalysisService;
}
```

### 3. 调用服务

```java
// 示例1: 查询 ERROR 级别日志
PageResult<LogRecordVo> result = logQueryService.query(
    "im-auth",                          // module
    LocalDateTime.now().minusDays(1),   // start
    LocalDateTime.now(),                // end
    LogLevel.ERROR,                     // level
    "auth-service",                     // service
    "prod",                             // env
    1,                                  // page
    20,                                 // size
    "login failed"                      // keyword
);

// 示例2: 采集单条日志
LogRecordVo record = LogRecordVo.builder()
    .level(LogLevel.ERROR)
    .module("im-auth")
    .service("auth-service")
    .env("prod")
    .message("用户登录失败")
    .timestamp(LocalDateTime.now())
    .build();
logIngestService.ingest(record);

// 示例3: 获取统计概览
Map<String, Object> overview = logAnalysisService.overview();
Map<String, Long> levels = (Map<String, Long>) overview.get("levels");
System.out.println("ERROR 日志数量: " + levels.get("ERROR"));
```

---

## 接口详细说明

### 一、日志查询服务 (LogQueryDubboService)

#### 1.1 分页查询日志

```java
PageResult<LogRecordVo> query(
    String module,           // 模块名（可选）
    LocalDateTime start,     // 开始时间（可选）
    LocalDateTime end,       // 结束时间（可选）
    LogLevel level,          // 日志级别（可选）
    String service,          // 服务名（可选）
    String env,              // 环境（可选）
    int page,                // 页码（从1开始）
    int size,                // 每页大小
    String keyword           // 关键字（可选）
);
```

**功能说明**: 根据多个条件组合查询日志，支持分页返回

**使用示例**:

```java
// 查询最近1小时的 ERROR 日志
PageResult<LogRecordVo> errors = logQueryService.query(
    null,                           // 所有模块
    LocalDateTime.now().minusHours(1),
    LocalDateTime.now(),
    LogLevel.ERROR,
    null,                           // 所有服务
    "prod",
    1,
    50,
    null
);

// 遍历结果
for (LogRecordVo log : errors.getContent()) {
    System.out.println(log.getMessage());
}
```

#### 1.2 使用 DTO 查询

```java
PageResult<LogRecordVo> query(LogQueryDto queryDto);
```

**使用示例**:

```java
LogQueryDto queryDto = LogQueryDto.builder()
    .module("im-auth")
    .start(LocalDateTime.now().minusDays(7))
    .end(LocalDateTime.now())
    .level(LogLevel.WARN)
    .page(1)
    .size(100)
    .build();

PageResult<LogRecordVo> result = logQueryService.query(queryDto);
```

#### 1.3 直方图统计

```java
Map<String, Long> histogram(
    String module,
    LocalDateTime start,
    LocalDateTime end,
    LogLevel level,
    String service,
    String env,
    String keyword,
    String interval    // "hour" 或 "minute"
);
```

**功能说明**: 按时间间隔统计日志数量，适用于绘制趋势图

**使用示例**:

```java
// 统计每小时的 ERROR 日志数量
Map<String, Long> hourlyData = logQueryService.histogram(
    "im-auth",
    LocalDateTime.now().minusDays(1),
    LocalDateTime.now(),
    LogLevel.ERROR,
    null,
    "prod",
    null,
    "hour"
);

// 输出示例: {"2025-12-24 08": 15, "2025-12-24 09": 23, ...}
hourlyData.forEach((time, count) -> {
    System.out.println(time + ": " + count + " 条");
});
```

#### 1.4 获取服务列表

```java
List<String> listServices(String env);
```

**使用示例**:

```java
List<String> services = logQueryService.listServices("prod");
// 输出: ["auth-service", "user-service", "order-service", ...]
```

#### 1.5 热门服务统计

```java
List<Map<String, Object>> topServices(
    LocalDateTime start,
    LocalDateTime end,
    int limit
);
```

**返回格式**:

```java
[
    {"name": "auth-service", "count": 15234},
    {"name": "order-service", "count": 12456},
    {"name": "user-service", "count": 9876}
]
```

#### 1.6 热门地址统计

```java
List<Map<String, Object>> topAddresses(
    LocalDateTime start,
    LocalDateTime end,
    int limit
);
```

**功能说明**: 统计日志数量最多的实例地址

**使用示例**:

```java
List<Map<String, Object>> topAddrs = logQueryService.topAddresses(
    LocalDateTime.now().minusDays(1),
    LocalDateTime.now(),
    10
);
// 输出: [{"name": "10.0.1.100:8080", "count": 5432}, ...]
```

#### 1.7 热门错误类型统计

```java
List<Map<String, Object>> topErrorTypes(
    LocalDateTime start,
    LocalDateTime end,
    int limit
);
```

**使用示例**:

```java
List<Map<String, Object>> topErrors = logQueryService.topErrorTypes(
    LocalDateTime.now().minusDays(7),
    LocalDateTime.now(),
    20
);
// 输出: [{"name": "java.lang.NullPointerException", "count": 234}, ...]
```

#### 1.8 删除历史日志

```java
// 删除所有指定时间之前的日志
void deleteBefore(LocalDateTime cutoff);

// 删除指定模块的日志
void deleteModuleBefore(String module, LocalDateTime cutoff);
```

**使用示例**:

```java
// 清理90天前的日志
LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
logQueryService.deleteBefore(cutoff);

// 只清理 im-auth 模块60天前的日志
logQueryService.deleteModuleBefore("im-auth", LocalDateTime.now().minusDays(60));
```

---

### 二、日志采集服务 (LogIngestDubboService)

#### 2.1 单条日志采集

```java
void ingest(LogRecordVo record);
```

**使用示例**:

```java
LogRecordVo record = LogRecordVo.builder()
    .level(LogLevel.ERROR)
    .module("im-auth")
    .service("auth-service")
    .address("10.0.1.100:8080")
    .env("prod")
    .message("用户认证失败")
    .exception("java.lang.IllegalArgumentException: Invalid token")
    .timestamp(LocalDateTime.now())
    .traceId("0af7651916cd43dd8448eb211c80319c")
    .spanId("b7ad6b7169203331")
    .thread("http-nio-8080-exec-12")
    .tags(Set.of("security", "audit"))
    .context(Map.of(
        "userId", "U-10001",
        "ip", "192.168.1.100"
    ))
    .build();

logIngestService.ingest(record);
```

#### 2.2 批量日志采集

```java
void ingestBatch(List<LogRecordVo> records);
```

**使用示例**:

```java
List<LogRecordVo> batch = new ArrayList<>();
for (int i = 0; i < 100; i++) {
    LogRecordVo record = LogRecordVo.builder()
        .level(LogLevel.INFO)
        .module("im-order")
        .service("order-service")
        .message("订单处理: " + i)
        .timestamp(LocalDateTime.now())
        .build();
    batch.add(record);
}

logIngestService.ingestBatch(batch);
```

**性能建议**:

- 批量大小建议在 100-500 条之间
- 对于高吞吐场景，推荐使用批量接口
- 采集是异步操作，不会阻塞调用方

---

### 三、日志分析服务 (LogAnalysisDubboService)

#### 3.1 实时聚合统计

```java
void aggregate(Object record);
```

**功能说明**: 采集日志时自动调用，用于实时更新统计信息

**注意**: 通常不需要手动调用，由 `LogIngestDubboService` 内部自动触发

#### 3.2 获取统计概览

```java
Map<String, Object> overview();
```

**返回格式**:

```json
{
  "levels": {
    "TRACE": 0,
    "DEBUG": 15234,
    "INFO": 523456,
    "WARN": 12345,
    "ERROR": 2345
  },
  "modules": {}
}
```

**使用示例**:

```java
Map<String, Object> overview = logAnalysisService.overview();
Map<String, Long> levels = (Map<String, Long>) overview.get("levels");

System.out.println("总日志数: " + levels.values().stream().mapToLong(Long::longValue).sum());
System.out.println("ERROR 日志占比: " +
    (levels.get("ERROR").doubleValue() / levels.values().stream().mapToLong(Long::longValue).sum() * 100) + "%");
```

#### 3.3 小时级时间序列

```java
Map<String, Long> hourlySeries(String level, int hours);
```

**功能说明**: 获取指定级别日志在最近 N 小时内的数量趋势

**使用示例**:

```java
// 获取最近24小时的 ERROR 日志趋势
Map<String, Long> series = logAnalysisService.hourlySeries("ERROR", 24);

// 按时间排序
series.entrySet().stream()
    .sorted(Map.Entry.comparingByKey())
    .forEach(entry -> {
        System.out.println(entry.getKey() + ": " + entry.getValue() + " 条");
    });

// 输出示例:
// 2025122400: 45 条
// 2025122401: 52 条
// 2025122402: 38 条
// ...
```

---

## 数据模型

### LogLevel 枚举

| 枚举值     | 说明   | 使用场景     |
|---------|------|----------|
| `TRACE` | 跟踪信息 | 最详细的调试信息 |
| `DEBUG` | 调试信息 | 开发调试阶段使用 |
| `INFO`  | 信息   | 一般性信息记录  |
| `WARN`  | 警告   | 潜在问题警告   |
| `ERROR` | 错误   | 错误和异常信息  |

### LogRecordVo（日志记录）

| 字段          | 类型                  | 必填 | 说明           | 示例                                        |
|-------------|---------------------|----|--------------|-------------------------------------------|
| `id`        | String              | 否  | 日志ID（UUID）   | "123e4567-e89b-12d3-a456-426614174000"    |
| `timestamp` | LocalDateTime       | 否  | 时间戳（默认当前时间）  | "2025-12-24T08:30:15"                     |
| `level`     | LogLevel            | 是  | 日志级别         | `LogLevel.ERROR`                          |
| `module`    | String              | 是  | 模块名          | "im-auth"                                 |
| `service`   | String              | 是  | 服务名          | "auth-service"                            |
| `address`   | String              | 否  | 来源地址         | "10.0.1.100:8080"                         |
| `env`       | String              | 否  | 环境           | "prod"                                    |
| `traceId`   | String              | 否  | 分布式追踪ID      | "0af7651916cd43dd8448eb211c80319c"        |
| `spanId`    | String              | 否  | 分布式追踪Span ID | "b7ad6b7169203331"                        |
| `thread`    | String              | 否  | 线程名          | "http-nio-8080-exec-12"                   |
| `message`   | String              | 是  | 日志内容         | "用户登录失败"                                  |
| `exception` | String              | 否  | 异常堆栈         | "java.lang.IllegalArgumentException: ..." |
| `tags`      | Set<String>         | 否  | 标签           | ["security", "audit"]                     |
| `context`   | Map<String, Object> | 否  | 上下文信息        | {"userId": "U-10001"}                     |

### LogQueryDto（查询条件）

| 字段        | 类型            | 必填 | 默认值        | 说明               |
|-----------|---------------|----|------------|------------------|
| `module`  | String        | 否  | null       | 模块名过滤            |
| `start`   | LocalDateTime | 否  | 1970-01-01 | 开始时间             |
| `end`     | LocalDateTime | 否  | 当前时间       | 结束时间             |
| `level`   | LogLevel      | 否  | null       | 日志级别过滤           |
| `service` | String        | 否  | null       | 服务名过滤            |
| `env`     | String        | 否  | null       | 环境过滤             |
| `page`    | Integer       | 否  | 1          | 页码（从1开始）         |
| `size`    | Integer       | 否  | 10         | 每页大小             |
| `keyword` | String        | 否  | null       | 关键字搜索（message字段） |

### PageResult<T>（分页结果）

| 字段              | 类型      | 说明         |
|-----------------|---------|------------|
| `content`       | List<T> | 数据列表       |
| `pageNumber`    | Integer | 当前页码（从0开始） |
| `pageSize`      | Integer | 每页大小       |
| `totalElements` | Long    | 总元素数       |
| `totalPages`    | Integer | 总页数        |
| `hasNext`       | Boolean | 是否有下一页     |
| `hasPrevious`   | Boolean | 是否有上一页     |

---

## 最佳实践

### 1. 日志采集

```java
// ✅ 推荐：设置完整的上下文信息
LogRecordVo record = LogRecordVo.builder()
    .level(LogLevel.ERROR)
    .module("im-auth")
    .service("auth-service")
    .env("prod")
    .message("用户认证失败")
    .context(Map.of(
        "userId", user.getId(),
        "ip", request.getRemoteAddr(),
        "userAgent", request.getHeader("User-Agent")
    ))
    .tags(Set.of("security", "audit"))
    .traceId(MDC.get("traceId"))
    .timestamp(LocalDateTime.now())
    .build();

// ❌ 避免：缺少关键信息
LogRecordVo badRecord = LogRecordVo.builder()
    .level(LogLevel.ERROR)
    .message("错误")  // 缺少模块、服务等关键信息
    .build();
```

### 2. 日志查询

```java
// ✅ 推荐：合理设置时间范围
LocalDateTime now = LocalDateTime.now();
PageResult<LogRecordVo> result = logQueryService.query(
    "im-auth",
    now.minusHours(1),    // 只查询1小时内的数据
    now,
    LogLevel.ERROR,
    null,
    "prod",
    1,
    50,
    null
);

// ❌ 避免：时间跨度过大
PageResult<LogRecordVo> badResult = logQueryService.query(
    null,
    LocalDateTime.of(2020, 1, 1, 0, 0),  // 跨度5年，查询很慢
    LocalDateTime.now(),
    null,
    null,
    null,
    1,
    1000,
    null
);
```

### 3. 批量采集

```java
// ✅ 推荐：控制批量大小
List<LogRecordVo> batch = new ArrayList<>();
for (LogRecordVo record : records) {
    batch.add(record);
    if (batch.size() >= 200) {  // 每200条提交一次
        logIngestService.ingestBatch(batch);
        batch.clear();
    }
}
if (!batch.isEmpty()) {
    logIngestService.ingestBatch(batch);
}

// ❌ 避免：批量过大
List<LogRecordVo> hugeBatch = new ArrayList<>();
for (int i = 0; i < 100000; i++) {  // 10万条，可能导致内存溢出
    hugeBatch.add(createLogRecord(i));
}
logIngestService.ingestBatch(hugeBatch);
```

### 4. 统计分析

```java
// ✅ 推荐：结合时间序列分析
Map<String, Long> hourlyErrors = logAnalysisService.hourlySeries("ERROR", 24);
// 找出异常峰值
long avg = hourlyErrors.values().stream().mapToLong(Long::longValue).sum() / 24;
hourlyErrors.entrySet().stream()
    .filter(e -> e.getValue() > avg * 2)
    .forEach(e -> System.out.println("异常峰值: " + e.getKey() + " -> " + e.getValue()));

// ✅ 推荐：定期清理历史日志
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
scheduler.scheduleAtFixedRate(() -> {
    LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
    logQueryService.deleteBefore(cutoff);
    log.info("已清理90天前的历史日志");
}, 1, 1, TimeUnit.DAYS);
```

---

## 错误处理

### 常见异常

1. **超时异常**

```java
try {
    PageResult<LogRecordVo> result = logQueryService.query(...);
} catch (RpcTimeoutException e) {
    log.error("查询超时，请缩小查询范围", e);
    // 降级处理：缩小时间范围后重试
}
```

2. **网络异常**

```java
try {
    logIngestService.ingestBatch(records);
} catch (RpcException e) {
    log.error("RPC调用失败", e);
    // 使用本地日志降级
    records.forEach(r -> log.error("日志记录: {}", r.getMessage()));
}
```

---

## 版本要求

| 组件                   | 版本要求    |
|----------------------|---------|
| JDK                  | 17+     |
| Spring Boot          | 3.x     |
| Dubbo                | 3.x     |
| Spring Cloud Alibaba | 2022.x+ |

---

## 相关文档

- [API 详细文档](./API.md)
- [OpenAPI 规范](./src/main/resources/openapi/logging-rpc-api.yaml)
- [使用指南](./HELP.md)

---

## 联系方式

- 团队: Lucky Cloud Team
- 项目: Lucky Cloud
- 版本: ${revision}
