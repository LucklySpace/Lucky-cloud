# im-logging-rpc-api API 详细文档

## 模块概述

`im-logging-rpc-api` 是分布式日志服务模块的 RPC API 接口定义模块，基于 Dubbo 提供日志采集、查询和分析的远程调用能力。

### 核心功能

- **日志采集**: 接收并存储来自各个微服务的日志数据
- **日志查询**: 支持多维度条件查询和分页
- **日志分析**: 提供日志统计、聚合和趋势分析
- **日志管理**: 支持日志清理和维护操作
- **分布式追踪**: 集成 traceId 和 spanId 支持链路追踪

### 技术特性

- 基于 Dubbo 3.x 的 RPC 服务
- 支持 PostgreSQL/PostgreSQL+TimescaleDB 存储
- Redis 缓存提升查询性能
- 支持 Gzip 压缩传输
- 完整的日志级别分类（TRACE/DEBUG/INFO/WARN/ERROR）

### 应用场景

- 微服务架构下的集中式日志管理
- 分布式系统的日志聚合与分析
- 实时日志监控与告警
- 故障排查与问题定位
- 系统性能分析与优化

---

## 服务接口

### 1. 日志查询服务 (LogQueryDubboService)

#### 服务信息

- **接口全限定名**: `com.xy.lucky.logging.rpc.api.query.LogQueryDubboService`
- **版本**: ${revision}

#### 方法列表

##### 1.1 查询日志（分页）- 参数化方式

```java
PageResult<LogRecordVo> query(String module, LocalDateTime start, LocalDateTime end,
                              LogLevel level, String service, String env,
                              int page, int size, String keyword);
```

**功能描述**: 根据多个参数条件查询日志，支持分页

**参数说明**:

- `module`: 模块名（业务域/子系统名）
- `start`: 开始时间
- `end`: 结束时间
- `level`: 日志级别（TRACE/DEBUG/INFO/WARN/ERROR）
- `service`: 服务名（微服务名）
- `env`: 环境（dev/test/prod等）
- `page`: 页码（从1开始）
- `size`: 每页大小
- `keyword`: 关键字（用于模糊搜索日志内容）

**返回值**:

- 类型: `PageResult<LogRecordVo>`
- 描述: 分页日志结果，详见 [PageResult](#pagereset-分页结果-dto) 和 [LogRecordVo](#logrecordvo-日志记录-vo)

**使用示例**:

```java
// 引用服务
@DubboReference
private LogQueryDubboService logQueryService;

// 查询最近1小时的ERROR级别日志
PageResult<LogRecordVo> result = logQueryService.query(
        "user-service",                              // 模块
        LocalDateTime.now().minusHours(1),           // 开始时间
        LocalDateTime.now(),                         // 结束时间
        LogLevel.ERROR,                              // 日志级别
        "user-service",                              // 服务名
        "prod",                                      // 环境
        1,                                           // 第1页
        20,                                          // 每页20条
        "timeout"                                    // 关键字
);

System.out.

println("总记录数: "+result.getTotalElements());
        System.out.

println("总页数: "+result.getTotalPages());
        result.

getContent().

forEach(log ->{
        System.out.

println("["+log.getTimestamp() +"] "+log.

getLevel() +" - "+log.

getMessage());
        });
```

##### 1.2 查询日志（分页）- DTO方式

```java
PageResult<LogRecordVo> query(LogQueryDto queryDto);
```

**功能描述**: 根据查询DTO对象查询日志，支持更灵活的查询条件组合

**参数说明**:

- `queryDto`: 查询条件对象，详见 [LogQueryDto](#logquerydto-日志查询-dto)

**返回值**:

- 类型: `PageResult<LogRecordVo>`
- 描述: 分页日志结果

**使用示例**:

```java
// 构建查询条件
LogQueryDto query = LogQueryDto.builder()
                .module("order-service")
                .start(LocalDateTime.now().minusDays(7))
                .end(LocalDateTime.now())
                .level(LogLevel.ERROR)
                .service("order-service")
                .env("prod")
                .page(1)
                .size(50)
                .keyword("payment")
                .build();

// 执行查询
PageResult<LogRecordVo> result = logQueryService.query(query);

// 处理结果
List<LogRecordVo> logs = result.getContent();
logs.

forEach(log ->{
        System.out.

println("时间: "+log.getTimestamp());
        System.out.

println("级别: "+log.getLevel());
        System.out.

println("服务: "+log.getService());
        System.out.

println("消息: "+log.getMessage());
        if(log.

getException() !=null){
        System.out.

println("异常: "+log.getException());
        }
        System.out.

println("---");
});
```

##### 1.3 日志直方图统计

```java
Map<String, Long> histogram(String module, LocalDateTime start, LocalDateTime end,
                            LogLevel level, String service, String env,
                            String keyword, String interval);
```

**功能描述**: 查询指定时间范围内按时间间隔聚合的日志数量统计

**参数说明**:

- `module`: 模块名
- `start`: 开始时间
- `end`: 结束时间
- `level`: 日志级别
- `service`: 服务名
- `env`: 环境
- `keyword`: 关键字
- `interval`: 时间间隔（hour/minute）

**返回值**:

- 类型: `Map<String, Long>`
- 描述: 时间间隔内日志数量，key为时间点，value为日志数量

**使用示例**:

```java
// 统计最近24小时每小时ERROR日志数量
Map<String, Long> hourlyHistogram = logQueryService.histogram(
                "payment-service",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now(),
                LogLevel.ERROR,
                "payment-service",
                "prod",
                null,
                "hour"
        );

// 输出统计结果
hourlyHistogram.

forEach((time, count) ->{
        System.out.

println(time +": "+count+" 条ERROR日志");
});

// 统计最近60分钟每分钟日志数量
Map<String, Long> minuteHistogram = logQueryService.histogram(
        "api-gateway",
        LocalDateTime.now().minusMinutes(60),
        LocalDateTime.now(),
        null,
        "api-gateway",
        "prod",
        null,
        "minute"
);

minuteHistogram.

forEach((time, count) ->{
        System.out.

println(time +": "+count+" 条日志");
});
```

##### 1.4 获取服务列表

```java
List<String> listServices(String env);
```

**功能描述**: 获取指定环境下的所有服务列表

**参数说明**:

- `env`: 环境标识（dev/test/prod等）

**返回值**:

- 类型: `List<String>`
- 描述: 服务名称列表

**使用示例**:

```java
// 获取生产环境所有服务
List<String> prodServices = logQueryService.listServices("prod");
System.out.

println("生产环境服务数量: "+prodServices.size());
        prodServices.

forEach(service ->{
        System.out.

println("- "+service);
});

// 获取开发环境所有服务
List<String> devServices = logQueryService.listServices("dev");
```

##### 1.5 获取热门服务列表

```java
List<Map<String, Object>> topServices(LocalDateTime start, LocalDateTime end, int limit);
```

**功能描述**: 获取指定时间范围内日志数量最多的服务列表（按日志数量降序）

**参数说明**:

- `start`: 开始时间
- `end`: 结束时间
- `limit`: 限制返回数量

**返回值**:

- 类型: `List<Map<String, Object>>`
- 描述: 服务列表，每个Map包含 service（服务名）和 count（日志数量）

**使用示例**:

```java
// 获取最近24小时日志数量最多的前10个服务
List<Map<String, Object>> topServices = logQueryService.topServices(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now(),
                10
        );

System.out.

println("=== 最活跃服务 TOP 10 ===");
for(
int i = 0; i <topServices.

size();

i++){
Map<String, Object> service = topServices.get(i);
String serviceName = (String) service.get("service");
Long count = (Long) service.get("count");
    System.out.

println((i +1) +". "+serviceName +": "+count +" 条日志");
        }
```

##### 1.6 获取热门地址列表

```java
List<Map<String, Object>> topAddresses(LocalDateTime start, LocalDateTime end, int limit);
```

**功能描述**: 获取指定时间范围内日志数量最多的实例地址列表

**参数说明**:

- `start`: 开始时间
- `end`: 结束时间
- `limit`: 限制返回数量

**返回值**:

- 类型: `List<Map<String, Object>>`
- 描述: 地址列表，每个Map包含 address（实例地址）和 count（日志数量）

**使用示例**:

```java
// 获取最近1小时日志最多的前20个实例
List<Map<String, Object>> topAddresses = logQueryService.topAddresses(
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now(),
                20
        );

System.out.

println("=== 日志最多的实例 TOP 20 ===");
topAddresses.

forEach(addr ->{
String address = (String) addr.get("address");
Long count = (Long) addr.get("count");
    System.out.

println(address +": "+count+" 条日志");
});
```

##### 1.7 获取热门错误类型列表

```java
List<Map<String, Object>> topErrorTypes(LocalDateTime start, LocalDateTime end, int limit);
```

**功能描述**: 获取指定时间范围内出现最多的错误类型列表

**参数说明**:

- `start`: 开始时间
- `end`: 结束时间
- `limit`: 限制返回数量

**返回值**:

- 类型: `List<Map<String, Object>>`
- 描述: 错误类型列表，每个Map包含 errorType（错误类型）和 count（数量）

**使用示例**:

```java
// 获取最近7天最多的错误类型
List<Map<String, Object>> topErrors = logQueryService.topErrorTypes(
                LocalDateTime.now().minusDays(7),
                LocalDateTime.now(),
                15
        );

System.out.

println("=== 最常见错误 TOP 15 ===");
topErrors.

forEach(error ->{
String errorType = (String) error.get("errorType");
Long count = (Long) error.get("count");
    System.out.

println(errorType +": "+count+" 次");
});
```

##### 1.8 删除指定时间之前的日志

```java
void deleteBefore(LocalDateTime cutoff);
```

**功能描述**: 删除所有在指定时间之前产生的日志（用于日志清理）

**参数说明**:

- `cutoff`: 截止时间，删除此时间之前的所有日志

**返回值**: 无

**使用示例**:

```java
// 删除30天前的所有日志
LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
logQueryService.

deleteBefore(cutoff);
System.out.

println("已删除30天前的日志");
```

##### 1.9 删除指定模块在指定时间之前的日志

```java
void deleteModuleBefore(String module, LocalDateTime cutoff);
```

**功能描述**: 删除指定模块在指定时间之前的日志

**参数说明**:

- `module`: 模块名
- `cutoff`: 截止时间

**返回值**: 无

**使用示例**:

```java
// 只清理某个模块的旧日志，保留其他模块
logQueryService.deleteModuleBefore("legacy-service",LocalDateTime.now().

minusDays(90));
        System.out.

println("已清理legacy-service模块90天前的日志");
```

---

### 2. 日志采集服务 (LogIngestDubboService)

#### 服务信息

- **接口全限定名**: `com.xy.lucky.logging.rpc.api.ingest.LogIngestDubboService`
- **版本**: ${revision}

#### 方法列表

##### 2.1 单条日志入库

```java
void ingest(LogRecordVo record);
```

**功能描述**: 接收并存储单条日志记录

**参数说明**:

- `record`: 日志记录对象，详见 [LogRecordVo](#logrecordvo-日志记录-vo)

**返回值**: 无

**使用示例**:

```java
// 引用服务
@DubboReference
private LogIngestDubboService logIngestService;

// 构建日志记录
LogRecordVo log = LogRecordVo.builder()
        .timestamp(LocalDateTime.now())
        .level(LogLevel.ERROR)
        .module("payment-service")
        .service("payment-service")
        .address("192.168.1.100:8080")
        .env("prod")
        .traceId("trace-123456")
        .spanId("span-789")
        .thread("http-nio-8080-exec-1")
        .message("Payment processing failed")
        .exception("java.sql.SQLException: Connection timeout\n\tat com.example.DB.getConnection(DB.java:45)")
        .tags(Set.of("payment", "database", "error"))
        .context(Map.of(
                "userId", "user123",
                "orderId", "order456",
                "amount", 99.99,
                "httpMethod", "POST",
                "httpUrl", "/api/payment/process"
        ))
        .build();

// 发送日志
logIngestService.

ingest(log);
```

##### 2.2 批量日志入库

```java
void ingestBatch(List<LogRecordVo> records);
```

**功能描述**: 批量接收并存储多条日志记录，提高吞吐量

**参数说明**:

- `records`: 日志记录列表

**返回值**: 无

**使用示例**:

```java
// 批量发送日志（推荐方式）
List<LogRecordVo> logs = new ArrayList<>();

// 收集日志
for(
int i = 0;
i< 100;i++){
LogRecordVo log = LogRecordVo.builder()
        .timestamp(LocalDateTime.now())
        .level(LogLevel.INFO)
        .module("batch-processor")
        .service("batch-service")
        .address("192.168.1.100:8080")
        .env("prod")
        .thread("batch-thread-" + i)
        .message("Processing item " + i)
        .context(Map.of(
                "batchId", "batch-" + System.currentTimeMillis(),
                "itemId", i
        ))
        .build();
    logs.

add(log);
}

// 批量发送（推荐每批100-1000条）
        logIngestService.

ingestBatch(logs);
```

**最佳实践**:

```java

@Component
public class LogBuffer {

    @DubboReference
    private LogIngestDubboService logIngestService;

    private List<LogRecordVo> buffer = new ArrayList<>();
    private static final int BUFFER_SIZE = 500;

    @Scheduled(fixedDelay = 5000)  // 每5秒或缓冲区满时发送
    public void flush() {
        if (buffer.isEmpty()) {
            return;
        }

        try {
            logIngestService.ingestBatch(new ArrayList<>(buffer));
            buffer.clear();
        } catch (Exception e) {
            System.err.println("日志发送失败: " + e.getMessage());
        }
    }

    public void addLog(LogRecordVo log) {
        buffer.add(log);
        if (buffer.size() >= BUFFER_SIZE) {
            flush();
        }
    }
}
```

---

### 3. 日志分析服务 (LogAnalysisDubboService)

#### 服务信息

- **接口全限定名**: `com.xy.lucky.logging.rpc.api.analysis.LogAnalysisDubboService`
- **版本**: ${revision}

#### 方法列表

##### 3.1 聚合日志统计

```java
void aggregate(Object record);
```

**功能描述**: 对日志记录进行聚合统计，更新Redis中的级别、模块和小时桶计数

**参数说明**:

- `record`: 日志记录对象（通常是 LogRecordVo）

**返回值**: 无

**说明**: 此方法会自动更新以下Redis计数器：

- 级别计数: `log:level:{level}` -> 计数
- 模块计数: `log:module:{module}` -> 计数
- 小时桶计数: `log:hour:{level}:{hour}` -> 计数

**使用示例**:

```java
// 引用服务
@DubboReference
private LogAnalysisDubboService logAnalysisService;

// 日志入库后进行聚合分析
LogRecordVo log = buildLogRecord();
logIngestService.

ingest(log);

// 更新统计信息
logAnalysisService.

aggregate(log);
```

##### 3.2 获取日志统计概览

```java
Map<String, Object> overview();
```

**功能描述**: 获取日志统计概览信息，包括各级别日志数量、模块统计等

**参数说明**: 无

**返回值**:

- 类型: `Map<String, Object>`
- 描述: 统计数据，可能包含以下字段：
    - `totalLogs`: 总日志数
    - `byLevel`: 按级别分组的统计（Map<LogLevel, Long>）
    - `byModule`: 按模块分组的统计（Map<String, Long>）
    - `byService`: 按服务分组的统计（Map<String, Long>）

**使用示例**:

```java
// 获取日志统计概览
Map<String, Object> overview = logAnalysisService.overview();

// 输出统计信息
Long totalLogs = (Long) overview.get("totalLogs");
System.out.

println("总日志数: "+totalLogs);

Map<String, Long> byLevel = (Map<String, Long>) overview.get("byLevel");
System.out.

println("\n按级别统计:");
byLevel.

forEach((level, count) ->{
        System.out.

println("  "+level +": "+count);
});

Map<String, Long> byModule = (Map<String, Long>) overview.get("byModule");
System.out.

println("\n按模块统计:");
byModule.

entrySet().

stream()
    .

sorted(Map.Entry .<String, Long>comparingByValue().

reversed())
        .

limit(10)
    .

forEach(entry ->{
        System.out.

println("  "+entry.getKey() +": "+entry.

getValue());
        });
```

##### 3.3 获取小时级日志序列

```java
Map<String, Long> hourlySeries(String level, int hours);
```

**功能描述**: 获取指定级别的最近N小时的时间序列数据

**参数说明**:

- `level`: 日志级别（ERROR/INFO等），传null表示所有级别
- `hours`: 最近小时数（如24表示最近24小时）

**返回值**:

- 类型: `Map<String, Long>`
- 描述: 时间序列数据，key为时间（格式：yyyy-MM-dd HH:mm），value为该小时的日志数量

**使用示例**:

```java
// 获取最近24小时的ERROR级别日志时间序列
Map<String, Long> errorSeries = logAnalysisService.hourlySeries("ERROR", 24);

System.out.

println("=== 最近24小时ERROR日志趋势 ===");
errorSeries.

forEach((time, count) ->{
        System.out.

println(time +": "+count+" 条");
});

// 获取最近48小时所有日志的趋势
Map<String, Long> allSeries = logAnalysisService.hourlySeries(null, 48);

// 可用于绘制图表
List<String> times = new ArrayList<>(allSeries.keySet());
List<Long> counts = new ArrayList<>(allSeries.values());

// 传递给前端绘制折线图
model.

addAttribute("times",times);
model.

addAttribute("counts",counts);
```

---

## 数据模型

### LogRecordVo - 日志记录 VO

用于采集和查询日志记录。

| 字段名       | 类型                    | 必填 | 说明                                |
|-----------|-----------------------|----|-----------------------------------|
| id        | String                | 否  | 日志ID（由服务端生成，UUID）                 |
| timestamp | LocalDateTime         | 是  | 日志时间戳（ISO-8601）                   |
| level     | LogLevel              | 是  | 日志级别（TRACE/DEBUG/INFO/WARN/ERROR） |
| module    | String                | 是  | 模块名（业务域/子系统名）                     |
| service   | String                | 是  | 服务名（微服务名）                         |
| address   | String                | 否  | 来源地址（实例IP:端口）                     |
| env       | String                | 是  | 环境（dev/test/prod等）                |
| traceId   | String                | 否  | 分布式链路追踪ID                         |
| spanId    | String                | 否  | 分布式链路跨度ID                         |
| thread    | String                | 否  | 线程名                               |
| message   | String                | 是  | 日志内容                              |
| exception | String                | 否  | 异常信息/堆栈                           |
| tags      | Set\<String\>         | 否  | 标签（用于快速过滤）                        |
| context   | Map\<String, Object\> | 否  | 上下文（结构化字段）                        |

**示例**:

```java
LogRecordVo log = LogRecordVo.builder()
        // 基本信息
        .timestamp(LocalDateTime.now())
        .level(LogLevel.ERROR)
        .module("payment-service")
        .service("payment-service")
        .address("192.168.1.100:8080")
        .env("prod")

        // 分布式追踪
        .traceId("trace-123456")
        .spanId("span-789")
        .thread("http-nio-8080-exec-1")

        // 日志内容
        .message("Payment processing failed for order #12345")
        .exception("java.sql.SQLException: Connection timeout\n" +
                "\tat com.example.DB.getConnection(DB.java:45)\n" +
                "\tat com.example.PaymentService.process(PaymentService.java:123)")

        // 标签（用于快速过滤）
        .tags(Set.of("payment", "database", "timeout", "critical"))

        // 上下文（结构化数据）
        .context(Map.of(
                "userId", "user123",
                "orderId", "order456",
                "amount", 99.99,
                "currency", "USD",
                "paymentMethod", "CREDIT_CARD",
                "httpMethod", "POST",
                "httpUrl", "/api/payment/process",
                "httpStatusCode", 500,
                "duration", 2345L
        ))
        .build();
```

---

### LogQueryDto - 日志查询 DTO

用于日志查询条件。

| 字段名     | 类型            | 必填 | 说明            |
|---------|---------------|----|---------------|
| module  | String        | 否  | 模块名           |
| start   | LocalDateTime | 否  | 开始时间          |
| end     | LocalDateTime | 否  | 结束时间          |
| level   | LogLevel      | 否  | 日志级别          |
| service | String        | 否  | 服务名           |
| env     | String        | 否  | 环境            |
| page    | Integer       | 否  | 页码（从1开始）      |
| size    | Integer       | 否  | 每页大小          |
| keyword | String        | 否  | 关键字（模糊搜索日志内容） |

**示例**:

```java
LogQueryDto query = LogQueryDto.builder()
        .module("order-service")
        .start(LocalDateTime.now().minusDays(7))
        .end(LocalDateTime.now())
        .level(LogLevel.ERROR)
        .service("order-service")
        .env("prod")
        .page(1)
        .size(50)
        .keyword("payment")
        .build();
```

---

### PageResult\<T\> - 分页结果 DTO

通用的分页结果封装。

| 字段名           | 类型        | 说明         |
|---------------|-----------|------------|
| content       | List\<T\> | 数据列表       |
| pageNumber    | Integer   | 当前页码（从0开始） |
| pageSize      | Integer   | 每页大小       |
| totalElements | Long      | 总元素数       |
| totalPages    | Integer   | 总页数        |
| hasNext       | Boolean   | 是否有下一页     |
| hasPrevious   | Boolean   | 是否有上一页     |

**示例**:

```java
PageResult<LogRecordVo> result = logQueryService.query(query);

// 访问分页信息
List<LogRecordVo> logs = result.getContent();
int currentPage = result.getPageNumber();  // 注意：从0开始
int pageSize = result.getPageSize();
long totalElements = result.getTotalElements();
int totalPages = result.getTotalPages();

System.out.

println("当前页: "+(currentPage +1) +"/"+totalPages);
        System.out.

println("每页: "+pageSize +" 条，总计: "+totalElements+" 条");

// 分页导航
if(result.

hasPrevious()){
        System.out.

println("有上一页");
}
        if(result.

hasNext()){
        System.out.

println("有下一页");
}
```

---

## 枚举类型

### LogLevel - 日志级别

| 枚举值   | 描述 | 使用场景                |
|-------|----|---------------------|
| TRACE | 追踪 | 最详细的调试信息，通常只在开发阶段使用 |
| DEBUG | 调试 | 调试信息，用于开发和测试        |
| INFO  | 信息 | 关键业务流程信息            |
| WARN  | 警告 | 潜在问题，但不影响系统运行       |
| ERROR | 错误 | 错误信息，需要关注和处理        |

**使用示例**:

```java
// 查询不同级别的日志
logQueryService.query(
    "payment-service",
    LocalDateTime.now().

minusHours(1),
    LocalDateTime.

now(),

LogLevel.ERROR,    // 只查询ERROR级别
        "payment-service",
        "prod",
        1,
        20,
        null
        );

// 查询WARN及以上级别（WARN + ERROR）
// 通常在应用层实现，先查WARN，再查ERROR
```

---

## 完整使用示例

### 示例1：日志采集（应用端）

```java

@Component
public class AppLogCollector {

    @DubboReference
    private LogIngestDubboService logIngestService;

    @DubboReference
    private LogAnalysisDubboService logAnalysisService;

    private static final int BATCH_SIZE = 100;
    private List<LogRecordVo> logBuffer = new ArrayList<>();

    /**
     * 记录INFO级别日志
     */
    public void logInfo(String message, Map<String, Object> context) {
        LogRecordVo log = LogRecordVo.builder()
                .timestamp(LocalDateTime.now())
                .level(LogLevel.INFO)
                .module(getModuleName())
                .service(getServiceName())
                .address(getServiceAddress())
                .env(getEnvironment())
                .traceId(MDC.get("traceId"))
                .spanId(MDC.get("spanId"))
                .thread(Thread.currentThread().getName())
                .message(message)
                .context(context)
                .build();

        sendLog(log);
    }

    /**
     * 记录ERROR级别日志
     */
    public void logError(String message, Throwable exception, Map<String, Object> context) {
        LogRecordVo log = LogRecordVo.builder()
                .timestamp(LocalDateTime.now())
                .level(LogLevel.ERROR)
                .module(getModuleName())
                .service(getServiceName())
                .address(getServiceAddress())
                .env(getEnvironment())
                .traceId(MDC.get("traceId"))
                .spanId(MDC.get("spanId"))
                .thread(Thread.currentThread().getName())
                .message(message)
                .exception(getStackTrace(exception))
                .tags(Set.of("error", exception.getClass().getSimpleName()))
                .context(context)
                .build();

        sendLog(log);
    }

    /**
     * 发送日志（支持批量）
     */
    private synchronized void sendLog(LogRecordVo log) {
        logBuffer.add(log);

        // 缓冲区满或定期刷新
        if (logBuffer.size() >= BATCH_SIZE) {
            flush();
        }
    }

    /**
     * 刷新缓冲区
     */
    @Scheduled(fixedDelay = 5000)  // 每5秒刷新一次
    public void flush() {
        if (logBuffer.isEmpty()) {
            return;
        }

        try {
            // 批量发送日志
            logIngestService.ingestBatch(new ArrayList<>(logBuffer));

            // 聚合统计
            logBuffer.forEach(logAnalysisService::aggregate);

            logBuffer.clear();
        } catch (Exception e) {
            System.err.println("日志发送失败: " + e.getMessage());
        }
    }

    /**
     * 获取异常堆栈
     */
    private String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}
```

### 示例2：日志查询（管理端）

```java

@Service
public class LogQueryService {

    @DubboReference
    private LogQueryDubboService logQueryService;

    /**
     * 查询最近的ERROR日志
     */
    public List<LogRecordVo> getRecentErrors(int limit) {
        PageResult<LogRecordVo> result = logQueryService.query(
                null,  // 所有模块
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now(),
                LogLevel.ERROR,
                null,  // 所有服务
                "prod",
                1,
                limit,
                null
        );

        return result.getContent();
    }

    /**
     * 查询特定服务的ERROR日志
     */
    public List<LogRecordVo> getErrorsByService(String serviceName, int hours) {
        LogQueryDto query = LogQueryDto.builder()
                .service(serviceName)
                .start(LocalDateTime.now().minusHours(hours))
                .end(LocalDateTime.now())
                .level(LogLevel.ERROR)
                .env("prod")
                .page(1)
                .size(100)
                .build();

        PageResult<LogRecordVo> result = logQueryService.query(query);
        return result.getContent();
    }

    /**
     * 根据traceId查询完整链路日志
     */
    public List<LogRecordVo> getLogsByTraceId(String traceId) {
        // 使用关键字搜索
        PageResult<LogRecordVo> result = logQueryService.query(
                null,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now(),
                null,
                null,
                null,
                1,
                100,
                traceId
        );

        // 过滤出匹配的日志
        return result.getContent().stream()
                .filter(log -> traceId.equals(log.getTraceId()))
                .collect(Collectors.toList());
    }

    /**
     * 分页查询日志（支持翻页）
     */
    public void searchLogs(int page, int size) {
        LogQueryDto query = LogQueryDto.builder()
                .start(LocalDateTime.now().minusDays(1))
                .end(LocalDateTime.now())
                .env("prod")
                .page(page)
                .size(size)
                .build();

        PageResult<LogRecordVo> result = logQueryService.query(query);

        System.out.println("=== 第 " + page + " 页 ===");
        result.getContent().forEach(log -> {
            System.out.printf("[%s] %s %s - %s%n",
                    log.getTimestamp(),
                    log.getLevel(),
                    log.getService(),
                    log.getMessage()
            );
        });

        System.out.printf("总计: %d 条，共 %d 页%n",
                result.getTotalElements(),
                result.getTotalPages());

        if (result.hasNext()) {
            System.out.println("提示: 有下一页");
        }
    }
}
```

### 示例3：日志统计与监控

```java

@Service
public class LogMonitorService {

    @DubboReference
    private LogQueryDubboService logQueryService;

    @DubboReference
    private LogAnalysisDubboService logAnalysisService;

    /**
     * 获取日志统计概览
     */
    public Map<String, Object> getLogOverview() {
        Map<String, Object> overview = logAnalysisService.overview();

        Long totalLogs = (Long) overview.get("totalLogs");
        Map<String, Long> byLevel = (Map<String, Long>) overview.get("byLevel");
        Map<String, Long> byModule = (Map<String, Long>) overview.get("byModule");

        System.out.println("=== 日志统计概览 ===");
        System.out.println("总日志数: " + totalLogs);
        System.out.println("\n按级别:");
        byLevel.forEach((level, count) -> {
            System.out.println("  " + level + ": " + count);
        });

        return overview;
    }

    /**
     * 获取ERROR日志趋势（最近24小时）
     */
    public Map<String, Long> getErrorTrend() {
        Map<String, Long> trend = logAnalysisService.hourlySeries("ERROR", 24);

        System.out.println("=== ERROR日志趋势（最近24小时）===");
        trend.forEach((time, count) -> {
            System.out.println(time + ": " + count);
        });

        return trend;
    }

    /**
     * 获取最活跃的服务TOP 10
     */
    public List<Map<String, Object>> getTopServices() {
        List<Map<String, Object>> topServices = logQueryService.topServices(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now(),
                10
        );

        System.out.println("=== 最活跃服务 TOP 10 ===");
        for (int i = 0; i < topServices.size(); i++) {
            Map<String, Object> service = topServices.get(i);
            System.out.printf("%d. %s: %d 条日志%n",
                    i + 1,
                    service.get("service"),
                    service.get("count")
            );
        }

        return topServices;
    }

    /**
     * 获取最常见的错误类型
     */
    public List<Map<String, Object>> getTopErrorTypes() {
        List<Map<String, Object>> topErrors = logQueryService.topErrorTypes(
                LocalDateTime.now().minusDays(7),
                LocalDateTime.now(),
                20
        );

        System.out.println("=== 最常见错误 TOP 20 ===");
        topErrors.forEach(error -> {
            System.out.printf("%s: %d 次%n",
                    error.get("errorType"),
                    error.get("count")
            );
        });

        return topErrors;
    }

    /**
     * 获取每小时ERROR日志数量（用于图表）
     */
    public Map<String, Long> getHourlyErrorHistogram() {
        return logQueryService.histogram(
                null,
                LocalDateTime.now().minusHours(24),
                LocalDateTime.now(),
                LogLevel.ERROR,
                null,
                "prod",
                null,
                "hour"
        );
    }
}
```

### 示例4：日志清理

```java

@Service
public class LogMaintenanceService {

    @DubboReference
    private LogQueryDubboService logQueryService;

    /**
     * 定期清理旧日志
     */
    @Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨2点执行
    public void cleanupOldLogs() {
        // 清理90天前的所有日志
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        System.out.println("开始清理 " + cutoff + " 之前的日志...");
        logQueryService.deleteBefore(cutoff);
        System.out.println("日志清理完成");
    }

    /**
     * 清理特定模块的旧日志
     */
    public void cleanupModuleLogs(String module, int retentionDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        System.out.println("清理模块 " + module + " " + cutoff + " 之前的日志...");
        logQueryService.deleteModuleBefore(module, cutoff);
        System.out.println("模块 " + module + " 日志清理完成");
    }

    /**
     * 按环境清理日志
     */
    public void cleanupByEnv(String env, int retentionDays) {
        // 注意：这个操作需要谨慎，建议先查询确认
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

        // 先查询确认要删除的日志数量
        PageResult<LogRecordVo> result = logQueryService.query(
                null,
                LocalDateTime.now().minusYears(10),  // 很早的时间
                cutoff,
                null,
                null,
                env,
                1,
                1,
                null
        );

        System.out.println("环境 " + env + " 将删除约 " +
                result.getTotalElements() + " 条日志");

        // 注意：实际删除前应该再次确认
        // logQueryService.deleteBefore(cutoff);  // 这会删除所有环境的日志
    }
}
```

### 示例5：集成Spring Boot日志框架

```java

@Configuration
public class LoggingConfig {

    @DubboReference
    private LogIngestDubboService logIngestService;

    @DubboReference
    private LogAnalysisDubboService logAnalysisService;

    /**
     * 自定义Logback Appender，将日志发送到日志服务
     */
    @PostConstruct
    public void configureLogback() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        // 创建自定义Appender
        DubboLogAppender appender = new DubboLogAppender();
        appender.setContext(loggerContext);
        appender.setName("DUBBO_LOG");
        appender.setLogIngestService(logIngestService);
        appender.setLogAnalysisService(logAnalysisService);

        // 设置过滤器（只发送INFO及以上级别）
        ThresholdFilter filter = new ThresholdFilter();
        filter.setLevel("INFO");
        filter.start();
        appender.addFilter(filter);

        // 设置编码器
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n");
        encoder.start();
        appender.setEncoder(encoder);

        appender.start();

        // 添加到root logger
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(appender);
    }
}

/**
 * Dubbo日志Appender
 */
public class DubboLogAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private LogIngestDubboService logIngestService;
    private LogAnalysisDubboService logAnalysisService;

    private static final int BATCH_SIZE = 100;
    private List<LogRecordVo> logBuffer = new ArrayList<>();

    @Override
    protected void append(ILoggingEvent event) {
        try {
            // 转换为LogRecordVo
            LogRecordVo log = convertToLogRecord(event);
            logBuffer.add(log);

            // 批量发送
            if (logBuffer.size() >= BATCH_SIZE) {
                flush();
            }
        } catch (Exception e) {
            addError("Failed to send log to dubbo service", e);
        }
    }

    private LogRecordVo convertToLogRecord(ILoggingEvent event) {
        LogLevel level = mapLevel(event.getLevel());

        return LogRecordVo.builder()
                .timestamp(LocalDateTime.now())
                .level(level)
                .module(getModuleName())
                .service(getServiceName())
                .address(getServiceAddress())
                .env(getEnvironment())
                .traceId(MDC.get("traceId"))
                .spanId(MDC.get("spanId"))
                .thread(event.getThreadName())
                .message(event.getFormattedMessage())
                .exception(event.getThrowableProxy() != null ?
                        throwableProxyToString(event.getThrowableProxy()) : null)
                .context(extractContext(MDC.getCopyOfContextMap()))
                .build();
    }

    @Scheduled(fixedDelay = 5000)
    public void flush() {
        if (logBuffer.isEmpty()) {
            return;
        }

        try {
            logIngestService.ingestBatch(new ArrayList<>(logBuffer));
            logBuffer.forEach(logAnalysisService::aggregate);
            logBuffer.clear();
        } catch (Exception e) {
            addError("Failed to flush logs", e);
        }
    }

    // Setter方法
    public void setLogIngestService(LogIngestDubboService logIngestService) {
        this.logIngestService = logIngestService;
    }

    public void setLogAnalysisService(LogAnalysisDubboService logAnalysisService) {
        this.logAnalysisService = logAnalysisService;
    }

    // ... 其他辅助方法
}
```

---

## 性能优化建议

### 1. 批量采集

```java
// 推荐：批量发送
logIngestService.ingestBatch(logs);

// 避免：单条发送（性能差）
for(
LogRecordVo log :logs){
        logIngestService.

ingest(log);  // 不推荐
}
```

### 2. 异步发送

```java

@Async
public void asyncSendLog(LogRecordVo log) {
    logIngestService.ingest(log);
}
```

### 3. 压缩传输

```yaml
# application.yml
logging:
  compress: true  # 启用Gzip压缩
```

### 4. 缓冲区管理

```java
// 合理设置缓冲区大小
private static final int BUFFER_SIZE = 500;  // 100-1000之间
private static final int FLUSH_INTERVAL = 5000;  // 5秒
```

### 5. 日志级别控制

```java
// 开发环境：记录DEBUG及以上
if(isDevEnv()){
        logIngestService.

ingest(debugLog);
}

// 生产环境：只记录INFO及以上
        if(log.

getLevel().

ordinal() >=LogLevel.INFO.

ordinal()){
        logIngestService.

ingest(log);
}
```

---

## 版本信息

- **当前版本**: ${revision}
- **Dubbo版本**: 3.x
- **Java版本**: 17+
- **更新日期**: 2025-02-06

---

## 相关文档

- [PostgreSQL TimescaleDB 文档](https://docs.timescale.com/)
- [Dubbo 用户指南](https://dubbo.apache.org/zh/docs3-v2/java-sdk/reference-manual/)
- [分布式追踪最佳实践](https://opentelemetry.io/docs/instrumentation/java/)
