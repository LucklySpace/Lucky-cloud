# im-quartz-rpc-api API 详细文档

## 模块概述

`im-quartz-rpc-api` 是分布式任务调度模块的 RPC API 接口定义模块，基于 Dubbo 提供任务管理和告警服务的远程调用能力。

### 核心功能

- **任务管理**：提供任务的创建、更新、启动、停止、删除等完整生命周期管理
- **任务调度**：支持 Cron 表达式、固定频率、固定延迟等多种调度策略
- **并发控制**：支持串行和并行两种并发执行策略
- **任务触发**：支持本地和远程两种触发类型
- **告警通知**：提供任务失败和超时的邮件告警功能
- **任务监控**：记录任务执行日志，支持执行历史查询

### 技术特性

- 基于 Dubbo 3.x 的 RPC 服务
- 支持负载均衡（轮询策略）
- 完整的参数校验（Jakarta Validation）
- Swagger/OpenAPI 3.0 文档支持

---

## 服务接口

### 1. 任务管理服务 (TaskDubboService)

#### 服务信息

- **接口全限定名**: `com.xy.lucky.quartz.rpc.api.task.TaskDubboService`
- **负载均衡策略**: 轮询 (ROUND_ROBIN)
- **版本**: ${revision}

#### 方法列表

##### 1.1 添加任务

```java
Long addTask(TaskInfoDto taskInfoDto);
```

**功能描述**: 创建一个新的定时任务

**参数说明**:

- `taskInfoDto`: 任务信息对象，详见 [TaskInfoDto](#taskinfodto-任务信息-dto)

**返回值**:

- 类型: `Long`
- 描述: 新创建任务的ID

**异常说明**:

- `javax.validation.ValidationException`: 参数校验失败时抛出
- `org.apache.dubbo.rpc.RpcException`: RPC调用异常

**使用示例**:

```java
// 引用服务
@DubboReference
private TaskDubboService taskDubboService;

// 创建任务信息
TaskInfoDto taskInfo = new TaskInfoDto();
taskInfo.setJobName("数据同步任务");
taskInfo.setJobGroup("DATA_SYNC");
taskInfo.setDescription("每小时同步用户数据");
taskInfo.setJobClass("com.example.task.DataSyncTask");
taskInfo.setScheduleType(ScheduleType.CRON);
taskInfo.setCronExpression("0 0 * * * ?");
taskInfo.setConcurrencyStrategy(ConcurrencyStrategy.SERIAL);
taskInfo.setTriggerType(TriggerType.LOCAL);
taskInfo.setRetryCount(3);
taskInfo.setRetryInterval(10);
taskInfo.setTimeout(300);
taskInfo.setAlarmEmail("admin@example.com");
taskInfo.setJobData("{\"source\":\"db1\",\"target\":\"db2\"}");

// 调用服务
Long taskId = taskDubboService.addTask(taskInfo);
System.out.println("任务创建成功，ID: " + taskId);
```

##### 1.2 更新任务

```java
Boolean updateTask(TaskInfoDto taskInfoDto);
```

**功能描述**: 更新已有任务的信息（注意：需要先停止任务才能更新）

**参数说明**:

- `taskInfoDto`: 任务信息对象，必须包含任务ID

**返回值**:

- 类型: `Boolean`
- 描述: true表示更新成功，false表示更新失败

**使用示例**:

```java
// 先停止任务
taskDubboService.stopTask(123L);

// 更新任务信息
TaskInfoDto taskInfo = new TaskInfoDto();
taskInfo.setId(123L);
taskInfo.setJobName("数据同步任务（已更新）");
taskInfo.setCronExpression("0 30 * * * ?");  // 修改为每30分钟执行
taskInfo.setJobGroup("DATA_SYNC");
taskInfo.setJobClass("com.example.task.DataSyncTask");
taskInfo.setScheduleType(ScheduleType.CRON);
taskInfo.setConcurrencyStrategy(ConcurrencyStrategy.SERIAL);
taskInfo.setTriggerType(TriggerType.LOCAL);

Boolean result = taskDubboService.updateTask(taskInfo);
```

##### 1.3 启动任务

```java
Boolean startTask(Long id);
```

**功能描述**: 启动指定的定时任务

**参数说明**:

- `id`: 任务ID

**返回值**:

- 类型: `Boolean`
- 描述: true表示启动成功，false表示启动失败

**使用示例**:

```java
Long taskId = 123L;
Boolean result = taskDubboService.startTask(taskId);
if (result) {
    System.out.println("任务启动成功");
} else {
    System.out.println("任务启动失败");
}
```

##### 1.4 停止任务

```java
Boolean stopTask(Long id);
```

**功能描述**: 停止正在运行的任务

**参数说明**:

- `id`: 任务ID

**返回值**:

- 类型: `Boolean`
- 描述: true表示停止成功，false表示停止失败

**使用示例**:

```java
Boolean result = taskDubboService.stopTask(123L);
```

##### 1.5 删除任务

```java
Boolean deleteTask(Long id);
```

**功能描述**: 删除指定任务（会先停止任务，然后删除）

**参数说明**:

- `id`: 任务ID

**返回值**:

- 类型: `Boolean`
- 描述: true表示删除成功，false表示删除失败

**使用示例**:

```java
Boolean result = taskDubboService.deleteTask(123L);
```

##### 1.6 立即触发任务

```java
Boolean triggerTask(Long id);
```

**功能描述**: 立即执行一次任务，不影响定时调度

**参数说明**:

- `id`: 任务ID

**返回值**:

- 类型: `Boolean`
- 描述: true表示触发成功，false表示触发失败

**使用示例**:

```java
// 立即执行一次任务
Boolean result = taskDubboService.triggerTask(123L);
```

##### 1.7 查询所有任务

```java
List<TaskInfoVo> findAll();
```

**功能描述**: 获取所有任务列表

**返回值**:

- 类型: `List<TaskInfoVo>`
- 描述: 任务信息列表，详见 [TaskInfoVo](#taskinfovo-任务信息-vo)

**使用示例**:

```java
List<TaskInfoVo> tasks = taskDubboService.findAll();
tasks.forEach(task -> {
    System.out.println("任务名称: " + task.getJobName());
    System.out.println("任务状态: " + task.getStatus().getDesc());
    System.out.println("下次执行时间: " + task.getNextFireTime());
});
```

##### 1.8 根据ID查询任务

```java
TaskInfoVo findById(Long id);
```

**功能描述**: 根据任务ID查询任务详情

**参数说明**:

- `id`: 任务ID

**返回值**:

- 类型: `TaskInfoVo`
- 描述: 任务信息对象，如果不存在返回null

**使用示例**:

```java
TaskInfoVo task = taskDubboService.findById(123L);
if (task != null) {
    System.out.println("任务名称: " + task.getJobName());
    System.out.println("任务描述: " + task.getDescription());
} else {
    System.out.println("任务不存在");
}
```

##### 1.9 分页查询任务

```java
List<TaskInfoVo> findByPage(TaskQueryDto queryDto);
```

**功能描述**: 根据查询条件分页查询任务列表

**参数说明**:

- `queryDto`: 查询条件对象，详见 [TaskQueryDto](#taskquerydto-任务查询-dto)

**返回值**:

- 类型: `List<TaskInfoVo>`
- 描述: 符合条件的任务列表

**使用示例**:

```java
// 构建查询条件
TaskQueryDto queryDto = new TaskQueryDto();
queryDto.setJobName("数据同步");  // 模糊查询
queryDto.setStatus(TaskStatus.RUNNING);
queryDto.setCreatedTimeStart(LocalDateTime.now().minusDays(7));
queryDto.setCreatedTimeEnd(LocalDateTime.now());
queryDto.setPageNum(1);
queryDto.setPageSize(10);
queryDto.setSortField("createdTime");
queryDto.setSortOrder("DESC");

// 执行查询
List<TaskInfoVo> tasks = taskDubboService.findByPage(queryDto);
```

##### 1.10 根据任务名称查询

```java
TaskInfoVo findByJobName(String jobName);
```

**功能描述**: 根据任务名称查询任务详情

**参数说明**:

- `jobName`: 任务名称

**返回值**:

- 类型: `TaskInfoVo`
- 描述: 任务信息对象，如果不存在返回null

**使用示例**:

```java
TaskInfoVo task = taskDubboService.findByJobName("数据同步任务");
if (task != null) {
    System.out.println("任务ID: " + task.getId());
    System.out.println("任务状态: " + task.getStatus());
}
```

##### 1.11 批量启动任务

```java
Integer batchStart(List<Long> ids);
```

**功能描述**: 批量启动多个任务

**参数说明**:

- `ids`: 任务ID列表

**返回值**:

- 类型: `Integer`
- 描述: 成功启动的任务数量

**使用示例**:

```java
List<Long> taskIds = Arrays.asList(1L, 2L, 3L, 4L, 5L);
Integer successCount = taskDubboService.batchStart(taskIds);
System.out.println("成功启动 " + successCount + " 个任务");
```

##### 1.12 批量停止任务

```java
Integer batchStop(List<Long> ids);
```

**功能描述**: 批量停止多个任务

**参数说明**:

- `ids`: 任务ID列表

**返回值**:

- 类型: `Integer`
- 描述: 成功停止的任务数量

**使用示例**:

```java
List<Long> taskIds = Arrays.asList(1L, 2L, 3L, 4L, 5L);
Integer successCount = taskDubboService.batchStop(taskIds);
System.out.println("成功停止 " + successCount + " 个任务");
```

##### 1.13 暂停任务

```java
Boolean pauseTask(Long id);
```

**功能描述**: 暂停任务（暂停后可以恢复）

**参数说明**:

- `id`: 任务ID

**返回值**:

- 类型: `Boolean`
- 描述: true表示暂停成功，false表示暂停失败

**使用示例**:

```java
Boolean result = taskDubboService.pauseTask(123L);
```

##### 1.14 恢复任务

```java
Boolean resumeTask(Long id);
```

**功能描述**: 恢复已暂停的任务

**参数说明**:

- `id`: 任务ID

**返回值**:

- 类型: `Boolean`
- 描述: true表示恢复成功，false表示恢复失败

**使用示例**:

```java
Boolean result = taskDubboService.resumeTask(123L);
```

---

### 2. 告警服务 (AlarmDubboService)

#### 服务信息

- **接口全限定名**: `com.xy.lucky.quartz.rpc.api.alarm.AlarmDubboService`
- **负载均衡策略**: 轮询 (ROUND_ROBIN)
- **版本**: ${revision}

#### 方法列表

##### 2.1 发送单个告警

```java
Boolean sendAlarm(AlarmSendDto alarmSendDto);
```

**功能描述**: 发送单个邮件告警

**参数说明**:

- `alarmSendDto`: 告警信息对象，详见 [AlarmSendDto](#alarmsenddto-告警发送-dto)

**返回值**:

- 类型: `Boolean`
- 描述: true表示发送成功，false表示发送失败

**使用示例**:

```java
// 引用服务
@DubboReference
private AlarmDubboService alarmDubboService;

// 创建告警信息
AlarmSendDto alarm = new AlarmSendDto();
alarm.setEmail("admin@example.com");
alarm.setSubject("任务执行失败告警");
alarm.setContent("任务【数据同步】执行失败，错误信息：连接超时");
alarm.setIsHtml(false);

// 发送告警
Boolean result = alarmDubboService.sendAlarm(alarm);
```

##### 2.2 批量发送告警

```java
Integer batchSendAlarm(List<String> emails, String subject, String content);
```

**功能描述**: 批量发送邮件告警给多个接收人

**参数说明**:

- `emails`: 接收人邮箱列表
- `subject`: 邮件主题
- `content`: 邮件内容

**返回值**:

- 类型: `Integer`
- 描述: 成功发送的数量

**使用示例**:

```java
List<String> emails = Arrays.asList(
    "admin1@example.com",
    "admin2@example.com",
    "admin3@example.com"
);

String subject = "系统告警：多个任务执行失败";
String content = "以下任务执行失败：\n1. 数据同步任务\n2. 报表生成任务";

Integer successCount = alarmDubboService.batchSendAlarm(emails, subject, content);
System.out.println("成功发送给 " + successCount + " 个接收人");
```

##### 2.3 发送任务失败告警

```java
Boolean sendTaskFailureAlarm(String taskName, String errorMsg, List<String> emails);
```

**功能描述**: 发送任务失败的专用告警邮件

**参数说明**:

- `taskName`: 任务名称
- `errorMsg`: 错误信息
- `emails`: 接收人邮箱列表

**返回值**:

- 类型: `Boolean`
- 描述: true表示发送成功，false表示发送失败

**使用示例**:

```java
String taskName = "数据同步任务";
String errorMsg = "Connection timeout: Failed to connect to database after 30000ms";
List<String> emails = Arrays.asList("admin@example.com", "ops@example.com");

Boolean result = alarmDubboService.sendTaskFailureAlarm(taskName, errorMsg, emails);
```

##### 2.4 发送任务超时告警

```java
Boolean sendTaskTimeoutAlarm(String taskName, Long timeoutSeconds, List<String> emails);
```

**功能描述**: 发送任务超时的专用告警邮件

**参数说明**:

- `taskName`: 任务名称
- `timeoutSeconds`: 超时时间（秒）
- `emails`: 接收人邮箱列表

**返回值**:

- 类型: `Boolean`
- 描述: true表示发送成功，false表示发送失败

**使用示例**:

```java
String taskName = "大数据分析任务";
Long timeoutSeconds = 600L;  // 10分钟
List<String> emails = Arrays.asList("admin@example.com");

Boolean result = alarmDubboService.sendTaskTimeoutAlarm(taskName, timeoutSeconds, emails);
```

---

## 数据模型

### TaskInfoDto - 任务信息 DTO

用于创建或更新任务时传递数据。

| 字段名                 | 类型                  | 必填 | 说明                                  |
|---------------------|---------------------|----|-------------------------------------|
| id                  | Long                | 否  | 任务ID（更新时必填）                         |
| jobName             | String              | 是  | 任务名称                                |
| jobGroup            | String              | 是  | 任务分组                                |
| description         | String              | 否  | 任务描述                                |
| jobClass            | String              | 是  | 任务执行类全限定名或Bean名称                    |
| cronExpression      | String              | 否  | Cron表达式（CRON类型必填）                   |
| repeatInterval      | Long                | 否  | 执行间隔毫秒数（FIXED_RATE/FIXED_DELAY类型必填） |
| scheduleType        | ScheduleType        | 是  | 调度类型枚举                              |
| concurrencyStrategy | ConcurrencyStrategy | 是  | 并发策略枚举                              |
| triggerType         | TriggerType         | 是  | 触发类型枚举                              |
| appName             | String              | 否  | 目标应用名称（REMOTE类型必填）                  |
| jobHandler          | String              | 否  | 任务处理器名称（REMOTE类型必填）                 |
| retryCount          | Integer             | 否  | 重试次数，默认0                            |
| retryInterval       | Integer             | 否  | 重试间隔（秒），默认10                        |
| timeout             | Integer             | 否  | 任务超时时间（秒），默认0                       |
| alarmEmail          | String              | 否  | 报警邮件地址（多个用逗号分隔）                     |
| jobData             | String              | 否  | 任务参数（JSON字符串）                       |
| createdTime         | LocalDateTime       | 否  | 创建时间                                |
| updatedTime         | LocalDateTime       | 否  | 更新时间                                |

**示例**:

```java
TaskInfoDto task = new TaskInfoDto();
task.setJobName("每小时数据统计");
task.setJobGroup("STATISTICS");
task.setDescription("统计每小时的用户活跃数据");
task.setJobClass("com.example.task.UserStatisticsTask");
task.setScheduleType(ScheduleType.CRON);
task.setCronExpression("0 0 * * * ?");
task.setConcurrencyStrategy(ConcurrencyStrategy.SERIAL);
task.setTriggerType(TriggerType.LOCAL);
task.setRetryCount(2);
task.setRetryInterval(5);
task.setTimeout(300);
task.setAlarmEmail("admin@example.com,ops@example.com");
task.setJobData("{\"metrics\":[\"active_users\",\"new_users\"]}");
```

---

### TaskQueryDto - 任务查询 DTO

用于分页查询任务时传递查询条件。

| 字段名              | 类型            | 必填 | 说明                    |
|------------------|---------------|----|-----------------------|
| jobName          | String        | 否  | 任务名称（支持模糊查询）          |
| jobGroup         | String        | 否  | 任务分组（精确匹配）            |
| status           | TaskStatus    | 否  | 任务状态枚举                |
| triggerType      | String        | 否  | 触发类型                  |
| createdTimeStart | LocalDateTime | 否  | 创建时间起始值               |
| createdTimeEnd   | LocalDateTime | 否  | 创建时间结束值               |
| pageNum          | Integer       | 否  | 页码，默认1                |
| pageSize         | Integer       | 否  | 每页大小，默认10             |
| sortField        | String        | 否  | 排序字段，默认createdTime    |
| sortOrder        | String        | 否  | 排序方向（ASC/DESC），默认DESC |

**示例**:

```java
TaskQueryDto query = new TaskQueryDto();
query.setJobName("数据");
query.setJobGroup("DATA_SYNC");
query.setStatus(TaskStatus.RUNNING);
query.setCreatedTimeStart(LocalDateTime.now().minusDays(30));
query.setCreatedTimeEnd(LocalDateTime.now());
query.setPageNum(1);
query.setPageSize(20);
query.setSortField("createdTime");
query.setSortOrder("DESC");
```

---

### TaskInfoVo - 任务信息 VO

用于返回任务详细信息，包含执行状态和时间信息。

| 字段名                 | 类型                  | 说明               |
|---------------------|---------------------|------------------|
| id                  | Long                | 任务ID             |
| jobName             | String              | 任务名称             |
| jobGroup            | String              | 任务分组             |
| description         | String              | 任务描述             |
| jobClass            | String              | 任务执行类全限定名或Bean名称 |
| cronExpression      | String              | Cron表达式          |
| repeatInterval      | Long                | 执行间隔（毫秒）         |
| scheduleType        | ScheduleType        | 调度类型             |
| status              | TaskStatus          | 任务状态             |
| concurrencyStrategy | ConcurrencyStrategy | 并发策略             |
| triggerType         | TriggerType         | 触发类型             |
| appName             | String              | 目标应用名称           |
| jobHandler          | String              | 任务处理器名称          |
| retryCount          | Integer             | 重试次数             |
| retryInterval       | Integer             | 重试间隔（秒）          |
| timeout             | Integer             | 任务超时时间（秒）        |
| alarmEmail          | String              | 报警邮件地址           |
| jobData             | String              | 任务参数（JSON字符串）    |
| createdTime         | LocalDateTime       | 创建时间             |
| updatedTime         | LocalDateTime       | 更新时间             |
| nextFireTime        | LocalDateTime       | 下次执行时间           |
| previousFireTime    | LocalDateTime       | 上次执行时间           |

---

### TaskLogVo - 任务日志 VO

用于返回任务执行日志信息。

| 字段名           | 类型            | 说明                      |
|---------------|---------------|-------------------------|
| id            | Long          | 日志ID                    |
| taskId        | Long          | 任务ID                    |
| jobName       | String        | 任务名称                    |
| jobGroup      | String        | 任务分组                    |
| startTime     | LocalDateTime | 开始时间                    |
| endTime       | LocalDateTime | 结束时间                    |
| executionTime | Long          | 执行耗时（毫秒）                |
| success       | Boolean       | 是否成功                    |
| exceptionInfo | String        | 异常信息                    |
| resultMsg     | String        | 执行结果消息                  |
| progress      | Integer       | 执行进度（0-100）             |
| status        | Integer       | 执行状态（0:运行中, 1:成功, 2:失败） |
| retryCount    | Integer       | 重试次数                    |
| executionNode | String        | 执行节点                    |

---

### AlarmSendDto - 告警发送 DTO

用于发送告警邮件。

| 字段名     | 类型      | 必填 | 说明               |
|---------|---------|----|------------------|
| email   | String  | 是  | 接收人邮箱（符合邮箱格式）    |
| subject | String  | 是  | 邮件主题             |
| content | String  | 是  | 邮件内容             |
| isHtml  | Boolean | 否  | 是否HTML格式，默认false |

**示例**:

```java
// 纯文本邮件
AlarmSendDto alarm1 = new AlarmSendDto();
alarm1.setEmail("admin@example.com");
alarm1.setSubject("任务失败告警");
alarm1.setContent("任务执行失败，请检查日志");
alarm1.setIsHtml(false);

// HTML邮件
AlarmSendDto alarm2 = new AlarmSendDto();
alarm2.setEmail("admin@example.com");
alarm2.setSubject("任务失败告警");
alarm2.setContent("<h2>任务执行失败</h2><p>请检查<a href='/logs'>日志</a></p>");
alarm2.setIsHtml(true);
```

---

### RegistryParamDto - 注册参数 DTO

用于远程任务执行器注册。

| 字段名     | 类型                 | 说明             |
|---------|--------------------|----------------|
| appName | String             | 应用名称           |
| address | String             | 执行器地址（ip:port） |
| jobs    | List\<JobInfoDto\> | 任务列表           |

**JobInfoDto 内部类**:
| 字段名 | 类型 | 说明 |
|--------|------|------|
| name | String | 任务名称 |
| description | String | 任务描述 |
| initParams | String | 初始化参数 |

---

## 枚举类型

### ScheduleType - 调度类型

| 枚举值         | 编码 | 描述        |
|-------------|----|-----------|
| CRON        | 0  | Cron表达式调度 |
| FIXED_RATE  | 1  | 固定频率调度    |
| FIXED_DELAY | 2  | 固定延迟调度    |

**使用示例**:

```java
// Cron表达式方式（每天凌晨2点执行）
taskInfo.setScheduleType(ScheduleType.CRON);
taskInfo.setCronExpression("0 0 2 * * ?");

// 固定频率方式（每5分钟执行一次）
taskInfo.setScheduleType(ScheduleType.FIXED_RATE);
taskInfo.setRepeatInterval(300000L);  // 5分钟 = 300000毫秒

// 固定延迟方式（上次执行完成后延迟5分钟再执行）
taskInfo.setScheduleType(ScheduleType.FIXED_DELAY);
taskInfo.setRepeatInterval(300000L);
```

---

### ConcurrencyStrategy - 并发策略

| 枚举值      | 编码 | 描述                  |
|----------|----|---------------------|
| SERIAL   | 0  | 串行执行（等待上一次任务完成后再执行） |
| PARALLEL | 1  | 并行执行（允许同时执行多个实例）    |

**使用示例**:

```java
// 串行执行（适合有状态的任务）
taskInfo.setConcurrencyStrategy(ConcurrencyStrategy.SERIAL);

// 并行执行（适合无状态的任务）
taskInfo.setConcurrencyStrategy(ConcurrencyStrategy.PARALLEL);
```

---

### TaskStatus - 任务状态

| 枚举值     | 编码 | 描述  |
|---------|----|-----|
| STOPPED | 0  | 停止  |
| RUNNING | 1  | 运行中 |
| PAUSED  | 2  | 暂停  |

**使用示例**:

```java
// 查询运行中的任务
queryDto.setStatus(TaskStatus.RUNNING);
List<TaskInfoVo> runningTasks = taskDubboService.findByPage(queryDto);

// 检查任务状态
TaskInfoVo task = taskDubboService.findById(123L);
if (task.getStatus() == TaskStatus.RUNNING) {
    System.out.println("任务正在运行");
}
```

---

### TriggerType - 触发类型

| 枚举值    | 编码 | 描述                     |
|--------|----|------------------------|
| LOCAL  | 0  | 本地触发（任务在本服务内执行）        |
| REMOTE | 1  | 远程触发（任务通过HTTP调用远程服务执行） |

**使用示例**:

```java
// 本地任务
taskInfo.setTriggerType(TriggerType.LOCAL);
taskInfo.setJobClass("com.example.task.LocalTask");

// 远程任务
taskInfo.setTriggerType(TriggerType.REMOTE);
taskInfo.setAppName("remote-task-service");
taskInfo.setJobHandler("dataSyncHandler");
```

---

## 完整使用示例

### 示例1：创建和管理本地定时任务

```java
@Service
public class TaskManagementService {

    @DubboReference
    private TaskDubboService taskDubboService;

    /**
     * 创建每小时执行的本地任务
     */
    public void createHourlyTask() {
        TaskInfoDto task = new TaskInfoDto();
        task.setJobName("用户数据同步");
        task.setJobGroup("DATA_SYNC");
        task.setDescription("每小时从数据库同步用户数据到缓存");
        task.setJobClass("com.example.task.UserDataSyncTask");
        task.setScheduleType(ScheduleType.CRON);
        task.setCronExpression("0 0 * * * ?");
        task.setConcurrencyStrategy(ConcurrencyStrategy.SERIAL);
        task.setTriggerType(TriggerType.LOCAL);
        task.setRetryCount(3);
        task.setRetryInterval(10);
        task.setTimeout(300);
        task.setAlarmEmail("admin@example.com");

        // 设置任务参数
        Map<String, Object> params = new HashMap<>();
        params.put("source", "mysql");
        params.put("target", "redis");
        params.put("batchSize", 1000);
        task.setJobData(new ObjectMapper().writeValueAsString(params));

        // 创建任务
        Long taskId = taskDubboService.addTask(task);
        System.out.println("任务创建成功，ID: " + taskId);

        // 启动任务
        Boolean started = taskDubboService.startTask(taskId);
        if (started) {
            System.out.println("任务启动成功");
        }
    }

    /**
     * 查询并管理运行中的任务
     */
    public void manageRunningTasks() {
        // 查询所有任务
        List<TaskInfoVo> allTasks = taskDubboService.findAll();
        System.out.println("总任务数: " + allTasks.size());

        // 查询运行中的任务
        TaskQueryDto query = new TaskQueryDto();
        query.setStatus(TaskStatus.RUNNING);
        query.setPageNum(1);
        query.setPageSize(100);

        List<TaskInfoVo> runningTasks = taskDubboService.findByPage(query);
        System.out.println("运行中的任务数: " + runningTasks.size());

        // 暂停部分任务
        List<Long> toPause = runningTasks.stream()
            .filter(t -> t.getJobName().contains("测试"))
            .map(TaskInfoVo::getId)
            .collect(Collectors.toList());

        toPause.forEach(id -> {
            Boolean paused = taskDubboService.pauseTask(id);
            if (paused) {
                System.out.println("任务 " + id + " 已暂停");
            }
        });
    }
}
```

### 示例2：创建远程任务

```java
@Service
public class RemoteTaskService {

    @DubboReference
    private TaskDubboService taskDubboService;

    /**
     * 创建远程HTTP调用任务
     */
    public void createRemoteTask() {
        TaskInfoDto task = new TaskInfoDto();
        task.setJobName("远程报表生成");
        task.setJobGroup("REPORT");
        task.setDescription("调用远程服务生成报表");
        task.setScheduleType(ScheduleType.CRON);
        task.setCronExpression("0 0 4 * * ?");  // 每天凌晨4点
        task.setConcurrencyStrategy(ConcurrencyStrategy.SERIAL);
        task.setTriggerType(TriggerType.REMOTE);  // 远程触发
        task.setAppName("report-service");  // 目标应用
        task.setJobHandler("generateReport");  // 任务处理器
        task.setRetryCount(2);
        task.setTimeout(1800);  // 30分钟超时
        task.setAlarmEmail("report@example.com");

        // 设置远程任务参数
        Map<String, Object> params = new HashMap<>();
        params.put("reportType", "daily");
        params.put("format", "pdf");
        task.setJobData(new ObjectMapper().writeValueAsString(params));

        Long taskId = taskDubboService.addTask(task);
        taskDubboService.startTask(taskId);
    }
}
```

### 示例3：使用告警服务

```java
@Service
public class AlarmService {

    @DubboReference
    private AlarmDubboService alarmDubboService;

    @DubboReference
    private TaskDubboService taskDubboService;

    /**
     * 监控任务并发送告警
     */
    @Scheduled(cron = "0 */5 * * * ?")  // 每5分钟检查一次
    public void monitorTasks() {
        // 查询所有任务
        List<TaskInfoVo> tasks = taskDubboService.findAll();

        for (TaskInfoVo task : tasks) {
            // 检查任务状态
            if (task.getStatus() == TaskStatus.STOPPED &&
                "ERROR".equals(task.getJobGroup())) {

                // 发送告警
                String errorMsg = "任务已意外停止";
                List<String> emails = Arrays.asList(
                    task.getAlarmEmail().split(",")
                );

                alarmDubboService.sendTaskFailureAlarm(
                    task.getJobName(),
                    errorMsg,
                    emails
                );
            }
        }
    }

    /**
     * 批量发送运维通知
     */
    public void sendOpsNotification(String message) {
        List<String> opsTeam = Arrays.asList(
            "ops1@example.com",
            "ops2@example.com",
            "ops3@example.com"
        );

        Integer count = alarmDubboService.batchSendAlarm(
            opsTeam,
            "系统通知",
            message
        );

        System.out.println("已通知 " + count + " 位运维人员");
    }
}
```

### 示例4：固定频率任务

```java
@Service
public class FixedRateTaskService {

    @DubboReference
    private TaskDubboService taskDubboService;

    /**
     * 创建每30秒执行一次的任务
     */
    public void createFixedRateTask() {
        TaskInfoDto task = new TaskInfoDto();
        task.setJobName("健康检查任务");
        task.setJobGroup("HEALTH_CHECK");
        task.setDescription("每30秒检查服务健康状态");
        task.setJobClass("com.example.task.HealthCheckTask");
        task.setScheduleType(ScheduleType.FIXED_RATE);  // 固定频率
        task.setRepeatInterval(30000L);  // 30秒 = 30000毫秒
        task.setConcurrencyStrategy(ConcurrencyStrategy.PARALLEL);  // 允许并行
        task.setTriggerType(TriggerType.LOCAL);
        task.setRetryCount(1);
        task.setTimeout(60);

        Long taskId = taskDubboService.addTask(task);
        taskDubboService.startTask(taskId);
    }

    /**
     * 创建固定延迟任务
     */
    public void createFixedDelayTask() {
        TaskInfoDto task = new TaskInfoDto();
        task.setJobName("邮件发送任务");
        task.setJobGroup("EMAIL");
        task.setDescription("上一次发送完成后延迟5分钟再发送");
        task.setJobClass("com.example.task.EmailSendTask");
        task.setScheduleType(ScheduleType.FIXED_DELAY);  // 固定延迟
        task.setRepeatInterval(300000L);  // 5分钟
        task.setConcurrencyStrategy(ConcurrencyStrategy.SERIAL);  // 串行
        task.setTriggerType(TriggerType.LOCAL);
        task.setRetryCount(3);
        task.setTimeout(120);

        Long taskId = taskDubboService.addTask(task);
        taskDubboService.startTask(taskId);
    }
}
```

### 示例5：批量操作任务

```java
@Service
public class BatchTaskService {

    @DubboReference
    private TaskDubboService taskDubboService;

    /**
     * 批量启动任务
     */
    public void batchStartTasks(List<Long> taskIds) {
        Integer successCount = taskDubboService.batchStart(taskIds);
        System.out.println("成功启动 " + successCount + " / " + taskIds.size() + " 个任务");
    }

    /**
     * 批量停止任务
     */
    public void batchStopTasks(List<Long> taskIds) {
        Integer successCount = taskDubboService.batchStop(taskIds);
        System.out.println("成功停止 " + successCount + " / " + taskIds.size() + " 个任务");
    }

    /**
     * 定时批量操作示例
     */
    public void scheduledBatchOperation() {
        // 查询特定分组的所有任务
        TaskQueryDto query = new TaskQueryDto();
        query.setJobGroup("BATCH_JOB");

        List<TaskInfoVo> tasks = taskDubboService.findByPage(query);

        // 提取所有任务ID
        List<Long> taskIds = tasks.stream()
            .map(TaskInfoVo::getId)
            .collect(Collectors.toList());

        if (!taskIds.isEmpty()) {
            // 批量启动
            Integer started = taskDubboService.batchStart(taskIds);
            System.out.println("批量启动完成，成功: " + started);
        }
    }
}
```

---

## 异常处理

### 常见异常类型

1. **参数校验异常**
    - 异常类: `javax.validation.ValidationException`
    - 触发条件: 必填参数为空、格式错误等
    - 处理建议: 检查参数是否符合要求

2. **RPC调用异常**
    - 异常类: `org.apache.dubbo.rpc.RpcException`
    - 触发条件: 网络问题、服务不可用等
    - 处理建议: 检查网络连接和服务状态

3. **任务操作异常**
    - 异常类: `com.xy.lucky.quartz.exception.TaskException`
    - 触发条件: 任务不存在、状态不允许操作等
    - 处理建议: 检查任务ID和当前状态

### 异常处理示例

```java
@Service
public class TaskServiceWithExceptionHandling {

    @DubboReference
    private TaskDubboService taskDubboService;

    public void safeStartTask(Long taskId) {
        try {
            // 检查任务是否存在
            TaskInfoVo task = taskDubboService.findById(taskId);
            if (task == null) {
                System.err.println("任务不存在: " + taskId);
                return;
            }

            // 检查任务状态
            if (task.getStatus() == TaskStatus.RUNNING) {
                System.out.println("任务已在运行中: " + taskId);
                return;
            }

            // 启动任务
            Boolean result = taskDubboService.startTask(taskId);
            if (result) {
                System.out.println("任务启动成功: " + taskId);
            } else {
                System.err.println("任务启动失败: " + taskId);
            }

        } catch (ValidationException e) {
            System.err.println("参数校验失败: " + e.getMessage());
        } catch (RpcException e) {
            System.err.println("RPC调用失败: " + e.getMessage());
            // 可以在这里实现重试逻辑
        } catch (Exception e) {
            System.err.println("未知异常: " + e.getMessage());
        }
    }
}
```

---

## 最佳实践

### 1. 任务命名规范

```java
// 推荐：使用有意义的名称和分组
task.setJobName("用户数据同步-从MySQL到Redis");
task.setJobGroup("DATA_SYNC");

// 不推荐：使用模糊的名称
task.setJobName("任务1");
task.setJobGroup("GROUP1");
```

### 2. 合理设置并发策略

```java
// 有状态的任务使用串行
task.setConcurrencyStrategy(ConcurrencyStrategy.SERIAL);

// 无状态的任务使用并行
task.setConcurrencyStrategy(ConcurrencyStrategy.PARALLEL);
```

### 3. 设置合理的超时和重试

```java
// 短任务：超时1分钟，重试3次
task.setTimeout(60);
task.setRetryCount(3);
task.setRetryInterval(10);

// 长任务：超时1小时，不重试
task.setTimeout(3600);
task.setRetryCount(0);
```

### 4. 使用任务参数传递配置

```java
Map<String, Object> params = new HashMap<>();
params.put("source", "mysql");
params.put("target", "redis");
params.put("batchSize", 1000);
params.put("enabled", true);
task.setJobData(new ObjectMapper().writeValueAsString(params));
```

### 5. 批量操作提高效率

```java
// 批量启动多个任务
List<Long> taskIds = Arrays.asList(1L, 2L, 3L, 4L, 5L);
Integer successCount = taskDubboService.batchStart(taskIds);
```

### 6. 及时关闭不再使用的任务

```java
// 删除前先停止
taskDubboService.stopTask(taskId);
taskDubboService.deleteTask(taskId);
```

### 7. 合理配置告警

```java
// 多个告警接收人用逗号分隔
task.setAlarmEmail("admin@example.com,ops@example.com,dev@example.com");
```

---

## 版本信息

- **当前版本**: ${revision}
- **Dubbo版本**: 3.x
- **Java版本**: 17+
- **更新日期**: 2025-02-06

---

## 相关文档

- [im-quartz 快速入门](./HELP.md)
- [Quartz 官方文档](http://www.quartz-scheduler.org/documentation/)
- [Dubbo 用户指南](https://dubbo.apache.org/zh/docs3-v2/java-sdk/reference-manual/)
