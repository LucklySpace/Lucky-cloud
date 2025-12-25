# im-logging 日志服务集成指南

## 概述

- 提供统一的日志采集、日志管理与日志分析能力
- 采集链路：REST → Disruptor → 仓储（ClickHouse/JPA）→ 统计聚合（Redis）
- 查询能力：按模块、级别、时间范围、关键字分页检索
- 统计能力：级别分布、按小时序列趋势
- 文档与调试：`/doc.html`（Knife4j），健康检查：`/actuator/health`

## 运行准备

- 启动 ClickHouse 并创建库与表（示例库名：`im_logs`）
- Redis 用于统计与缓存（默认 `localhost:6379`）
- 配置文件：`src/main/resources/application.yml`

### ClickHouse 表结构建议

```sql
CREATE
DATABASE IF NOT EXISTS im_logs;

CREATE TABLE IF NOT EXISTS im_logs.logs
(
    id
    String,
    ts
    DateTime64
(
    3
),
    level LowCardinality
(
    String
),
    module LowCardinality
(
    String
),
    service String,
    host String,
    trace_id String,
    span_id String,
    thread String,
    message String,
    exception String,
    tags String, -- 以逗号分隔
    context String -- 原始JSON文本
    )
    ENGINE = MergeTree
    PARTITION BY toYYYYMMDD
(
    ts
)
    ORDER BY
(
    ts,
    id
)
    TTL ts + INTERVAL 7 DAY
    SETTINGS index_granularity = 8192;
```

> 提示：`TTL` 可按需调整；如需更精细的删除策略，可使用 `ALTER TABLE ... DELETE WHERE ...`

## 关键配置

`src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:clickhouse://localhost:8123/im_logs
    driver-class-name: com.clickhouse.jdbc.ClickHouseDriver
    username: default
    password:
  jpa:
    show-sql: false
    open-in-view: false
    hibernate:
      ddl-auto: none
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect

springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    path: /doc.html

knife4j:
  enable: true

logging:
  pipeline:
    ring-buffer-size: 65536
    thread-count: 4
  retention:
    days: 7
    cleanup-interval-ms: 300000
```

## 主要代码结构

- 启动与文档：`com.xy.lucky.logging.ImLoggingApplication`
- 采集接口：`com.xy.lucky.logging.controller.LogController`
- 采集服务：`com.xy.lucky.logging.service.LogIngestService`
- 流水线：`com.xy.lucky.logging.config.DisruptorConfig`、`disruptor.*`
- 仓储（ClickHouse/JPA）：`com.xy.lucky.logging.jpa.*`、`repository.ClickHouseLogRepository`
- 统计与清理：`service.LogAnalysisService`、`service.LogMaintenanceService`

## 表实体与JPA仓库

- 实体：`com.xy.lucky.logging.domain.po.LogPo`
    - 对应 `logs` 表
    - 字段与 `LogRecord` 一致（`tags` 为逗号分隔、`context` 为JSON文本）
- 仓库：`com.xy.lucky.logging.jpa.LogRepository`
    - 使用原生 SQL 进行范围查询与分页
    - 删除采用 `JdbcTemplate` 执行 `ALTER TABLE ... DELETE` 语句

## REST API

- 采集单条日志
    - `POST /api/logs`
    - 请求示例：
      ```json
      {
        "level": "ERROR",
        "module": "im-server",
        "service": "order",
        "host": "node-1",
        "traceId": "abc-123",
        "spanId": "def-456",
        "thread": "http-nio-1",
        "message": "order create failed",
        "exception": "java.lang.IllegalStateException: x",
        "tags": ["biz","order"],
        "context": {"orderId": "O-10001"}
      }
      ```
- 批量采集：`POST /api/logs/batch`
- 查询日志：`GET /api/logs?module=&level=&start=&end=&page=&size=&keyword=`
- 删除（按时间）：`DELETE /api/logs/before?cutoff=2025-12-24T00:00:00Z`
- 删除（按模块+时间）：`DELETE /api/logs/module/{module}/before?cutoff=...`
- 统计概览：`GET /api/logs/stats/overview`
- 小时序列：`GET /api/logs/stats/hourly?level=ERROR&hours=24`

## 运行与测试

- 启动 ClickHouse 与 Redis
- 启动 `im-logging`，访问文档：`http://localhost:8080/doc.html`
- 通过文档页面进行接口调试与验证

## 性能与优化建议

- Disruptor 多生产者模式、合理的 `ring-buffer-size` 与消费线程数
- 将查询走 ClickHouse，统计走 Redis；分层保证写入与分析互不影响
- 表分区按日、主键 `(ts,id)`，结合 `TTL` 自动清理历史数据
- 批量采集建议在客户端做批量合并，减少网络与持久化压力

## 运维与清理

- 定时清理：`logging.retention.cleanup-interval-ms`
- 按需手动清理：`ALTER TABLE logs DELETE WHERE ts <= ...`
- 监控：`/actuator/health` 与 ClickHouse 自身指标

## 常见问题

- JPA 不负责表的自动建表：请使用上述 DDL 或外部迁移工具
- 删除语义为异步清理（ClickHouse）：大数据量下清理需等待后台合并完成
- 关键字查询采用 `lower(message) LIKE ...`，如需更强检索建议接入外部全文索引
