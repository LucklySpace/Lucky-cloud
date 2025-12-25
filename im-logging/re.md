二、核心功能（MVP 必备）

日志接入

支持协议：HTTP/HTTPS（JSON、NDJSON）、gRPC、TCP、Kafka、Syslog、Fluent/Fluent Bit/Vector/Logstash 采集器。

Agent/Sidecar：Fluent Bit/Vector/自研轻量 agent 支持文件、stdout（容器）、 journald、socket。

批量/压缩/并发上报，回压（flow-control）。

解析与处理管道（Ingest Pipeline）

自动/可配置解析（JSON、text、key=value、regex、grok、protobuf）。

处理器：字段提取、时间戳解析、IP/Geo、用户代理解析、等级提升、标签注入（service、env、pod、node）。

可插入自定义脚本（Lua/Python）或 WASM 处理器。

结构化日志支持

优先推荐 JSON 日志（字段可索引），同时兼容非结构化文本。

统一日志模型字段（见下面“数据模型”）。

索引与存储

热/冷/归档分层：热索引（快速检索）、冷存（分段存储）、归档到对象存储（S3/MinIO）。

索引策略：可选择全文索引、字段索引或只存储但不索引（节省成本）。

支持 ILM（Index Lifecycle Management）或等效机制。

查询引擎与 DSL

支持全文检索、结构化过滤、time-range、聚合（histogram/topN）、字段统计。

支持复杂布尔和正则表达式、字段存在/缺失判断、样例采样查询（sampling）。

支持速率限制与查询成本估算。

实时与流式能力

实时 tail（follow）功能，推送到 UI 或 websocket 客户端。

实时聚合/指标（例如每秒错误数、latency bucket）。

链路追踪（Trace）关联

自动从日志中识别 traceId/spanId，提供“跳转到 Trace”/“从 Trace 跳到相关日志”的能力（与 Jaeger/Zipkin/OTel 后端集成）。

在日志模型中强制或推荐 trace_id 字段。

告警与通知

基于日志的告警规则（查询触发、阈值、速率、异常检测）。

支持通知渠道：Email、Slack、Teams、Webhook、PagerDuty、钉钉。

支持抑制、告警分组、去重与延迟触发（for flapping）。

安全与访问控制

TLS 加密传输、OAuth2/JWT、API Key、mTLS（可选）。

RBAC：按团队/项目控制读写/导出权限。

审计日志：谁执行了哪些查询/导出/修改告警。

审计与合规

日志保留策略、删除（合规删除）、敏感信息屏蔽（PII redaction）、加密 at-rest。

支持审计导出与合规报表。

运维与监控

自身监控：吞吐量、延迟、错误率、队列长度、磁盘使用、索引大小。

健康检查、指标暴露（Prometheus）、告警策略。

横向扩展、节点故障恢复、数据副本策略。

导出、备份、归档

支持 CSV/NDJSON 导出、对象存储归档、按时间段备份/恢复。

支持 Log Replay（重放）到下游系统或重跑 pipeline。

三、高级功能（推荐/可选）

智能异常检测（基于规则/ML 的异常模式识别）。

日志采样/压缩策略（trace-sampling-aware，保留异常事件且对普通请求采样）。

多租户隔离（命名空间、配额、计费）。

日志等级动态调整（支持按服务远程提升 DEBUG 等级并回收）。

索引自动优化及冷热迁移。

日志变更通知（变更点检测）。

可插拔存储后端（ES, ClickHouse, Loki, Druid 等）。

SQL-on-logs 查询/BI 报表（ClickHouse、Presto 适配）。

User defined metrics from logs（将日志转换为时序指标）。

四、数据模型（建议字段）

每条日志建议包含（JSON）：

{
"timestamp":"2025-12-25T13:45:12.345Z",
"level":"ERROR",
"service":"user-service",
"env":"prod",
"host":"node-12",
"pod":"user-svc-abc123",
"thread":"http-nio-1",
"logger":"com.xy.user.UserController",
"message":"User login failed: ...",
"trace_id":"abcd1234",
"span_id":"efgh5678",
"request_id":"req-xxxxx",
"http": { "method":"POST","path":"/api/login","status":500,"latency_ms":123 },
"user": { "id":"u-123", "ip":"1.2.3.4" },
"tags": { "feature":"login","release":"v1.2.3" },
"exception": { "type":"NullPointerException","stack":"..."}
}

说明：结构化字段便于索引、聚合与过滤；trace_id / request_id 是关键的关联字段。

五、开发者 HTML 页面（详细功能与交互设计）

下面为开发者 / SRE 页面提供详尽的功能、组件与行为规范，既适合单页面应用（React/Vue）也适合嵌入到运维平台。

A. 页面总体布局

顶部工具栏：时间范围选择器（快捷 + 自定义）、环境选择（prod/stage/dev）、服务下拉（自动列出注册服务）、全局搜索栏（query
DSL）、用户信息与快捷操作（保存查询、导出）。

左侧栏（可折叠）：服务/来源树（服务 → 实例 → 日志流）、收藏查询、告警面板、索引/管道管理入口。

主区域：查询与结果面板（分为查询输入、时间直方图、结果列表、详情侧边栏）。

底部或右侧：Trace/Metric 快速面板、操作日志/通知。

B. 查询输入区域（中央）

支持两种模式切换：

简易模式（可视化构建过滤器：字段选择 + 条件 + value）。

高级模式（文本 DSL / Lucene-like / SQL-like，可折叠历史记录）。

提示/自动补全：

字段建议（基于索引字段）、操作符建议、常用时间短语（last 15m, today）。

快捷按钮：

Save、Share Link（生成短链）、Export（CSV/NDJSON）、Run、Tail（实时跟随）。

C. 时间直方图 / 聚合视图

显示选定时间范围内日志量折线/柱状图，支持按 level/service/host 切换聚合维度。

鼠标框选一段时间后自动缩小时间范围并刷新结果。

支持叠加告警阈值线，点击图表上的异常点可跳转到对应日志片段。

D. 结果列表（日志列表）

每行展示：时间、level 彩色标识、service、短 message（高亮匹配关键字）、traceId（可点击）、tags badges。

支持：多选、批量导出、右键菜单（复制 raw、复制 JSON、open trace、open request）。

支持列自定义（选择要显示的字段并保存为视图）。

支持分页与无限滚动（tail 模式下为流式追加）。

E. 日志详情侧边栏 / 弹窗

原始 JSON 展示（折叠/展开树形）、Pretty View、Raw Text、Stacktrace 折叠/展开与智能跳转到源码（若集成了 repo 链接）。

“上下文”按钮：显示该条日志前后 N 行（默认前后 50 行，可配置）。

Trace 快捷：若含 trace_id，显示 Trace 概览（duration、root service、span count），并提供“查看 Trace”链接。

Metric Links：展示基于该日志相关的 Metric（14m trend）。

注释/标记：开发者可对某条日志打 tag/注释（保存在日志存储或单独注释表中）。

F. 实时 Tail（Follow）

WebSocket 推送，自动滚动/暂停、速度控制（限流）、自动跳转到错误级别条目。

支持过滤器实时生效（只显示匹配的日志）。

G. 搜索与聚合工具

Field explorer：列出当前结果集常见字段和值（Top N），可点击值进行“按该值过滤”。

聚合卡片（Top hosts, Top messages, Error types），点击可进一步钻取到日志列表。

频率/趋势图（每分钟/每秒统计）。

H. 保存与共享

保存查询（带标签、说明、权限），支持公开/私有/团队共享。

生成短链或外部分享（可设置时效、访问权限）。

导出：CSV/NDJSON，包含可选列与时间范围。

I. 告警管理 UI

告警规则列表（创建表单、编辑、启停）。

规则类型：基于查询的阈值、速率阈值、异常检测型。

测试/回测功能：在 UI 中运行历史回测并展示触发次数与样例日志。

告警历史/审计：展示告警触发记录与处理状态、Tikcet 链接。

J. 管理与运维页面（Admin）

集群状态：节点列表、磁盘/内存/CPU、索引分布、分片健康。

索引管理：查看索引大小、热冷分层、ILM 策略、强制回收/删除。

Ingest Pipeline 管理：查看当前管道、pipeline 排序、版本化、测试输入输出。

配额与租户：流量配额、存储配额、配额告警。

Key/Secret 管理：API Key 列表、启停、权限范围设置。

备份与恢复操作界面。

日志自检（自服务日志服务自身日志）、节点日志下载。

K. 安全与审计 UI

用户与角色管理、单点登录接入（OAuth2 / OIDC）。

审计日志（谁运行了哪条查询，谁导出了数据，谁修改了规则）。

L. UX 细节（开发者体验）

结果高亮：查询词/正则高亮、差异高亮（与上一次查询对比）。

Stacktrace 对齐与折叠，支持跳至异常发生代码行（若可跳转）。

键盘快捷键（运行查询、切换 Tail、复制）。

查询历史（最近 50 条）与自动保存草稿。

响应式设计，便于在运维面板或 IDE 插件中嵌入。

六、API 设计（示例）

/ingest (POST) — 单条或批量上报（NDJSON 支持），返回 ingestion id / ack。

/search (POST) — 接受 DSL：{query, from, size, sort, aggs}，返回 hits + aggs + stats。

/tail (WS) — 实时推送匹配日志。

/pipelines (GET/POST/PUT/DELETE) — ingest pipeline 管理。

/alerts (CRUD) — 告警规则管理与测试。

/export (POST) — 请求导出并返回下载链接。

/admin/indices, /admin/health, /admin/backup 等管理 API。

Auth：OAuth2 Bearer / API Key header X-Api-Key。