# im-quartz-rpc-api 快速入门指南

## 目录

- [快速入门](#快速入门)
- [服务接口速查表](#服务接口速查表)
- [常见问题解答](#常见问题解答)
- [最佳实践](#最佳实践)
- [进阶技巧](#进阶技巧)

---

## 快速入门

### 1. 添加依赖

在您的项目中添加 `im-quartz-rpc-api` 依赖：

```xml
<dependency>
    <groupId>com.xy.lucky</groupId>
    <artifactId>im-quartz-rpc-api</artifactId>
    <version>${revision}</version>
</dependency>
```

### 2. 配置 Dubbo 引用

在 Spring Boot 应用中配置 Dubbo 服务引用：

```java
@Configuration
public class DubboConfig {

    @Bean
    public TaskDubboService taskDubboService() {
        return DubboReferenceBuilder
            .builder()
            .interfaceClass(TaskDubboService.class)
            .build();
    }

    @Bean
    public AlarmDubboService alarmDubboService() {
        return DubboReferenceBuilder
            .builder()
            .interfaceClass(AlarmDubboService.class)
            .build();
    }
}
```

或者使用注解方式：

```java
@Service
public class MyService {

    @DubboReference
    private TaskDubboService taskDubboService;

    @DubboReference
    private AlarmDubboService alarmDubboService;
}
```

### 3. 第一个定时任务

创建一个每分钟执行的简单任务：

```java
@Service
public class QuickStartService {

    @DubboReference
    private TaskDubboService taskDubboService;

    public void createFirstTask() {
        // 1. 构建任务信息
        TaskInfoDto task = new TaskInfoDto();
        task.setJobName("测试任务-每分钟执行");
        task.setJobGroup("TEST");
        task.setDescription("我的第一个定时任务");
        task.setJobClass("com.example.task.DemoTask");
        task.setScheduleType(ScheduleType.CRON);
        task.setCronExpression("0 * * * * ?");
        task.setConcurrencyStrategy(ConcurrencyStrategy.SERIAL);
        task.setTriggerType(TriggerType.LOCAL);

        // 2. 创建任务
        Long taskId = taskDubboService.addTask(task);
        System.out.println("任务创建成功，ID: " + taskId);

        // 3. 启动任务
        Boolean started = taskDubboService.startTask(taskId);
        if (started) {
            System.out.println("任务启动成功！");
        }
    }
}
```

### 4. 查询任务状态

```java
public void checkTaskStatus(Long taskId) {
    TaskInfoVo task = taskDubboService.findById(taskId);

    System.out.println("任务名称: " + task.getJobName());
    System.out.println("任务状态: " + task.getStatus().getDesc());
    System.out.println("下次执行时间: " + task.getNextFireTime());
    System.out.println("上次执行时间: " + task.getPreviousFireTime());
}
```

### 5. 停止和删除任务

```java
public void removeTask(Long taskId) {
    // 1. 停止任务
    taskDubboService.stopTask(taskId);

    // 2. 删除任务
    Boolean deleted = taskDubboService.deleteTask(taskId);
    if (deleted) {
        System.out.println("任务已删除");
    }
}
```

---

## 服务接口速查表

### TaskDubboService - 任务管理服务

| 方法                | 功能   | 参数           | 返回值                | 使用场景      |
|-------------------|------|--------------|--------------------|-----------|
| `addTask()`       | 创建任务 | TaskInfoDto  | Long (任务ID)        | 新建定时任务    |
| `updateTask()`    | 更新任务 | TaskInfoDto  | Boolean            | 修改任务配置    |
| `startTask()`     | 启动任务 | Long (任务ID)  | Boolean            | 启动已停止的任务  |
| `stopTask()`      | 停止任务 | Long (任务ID)  | Boolean            | 停止运行中的任务  |
| `pauseTask()`     | 暂停任务 | Long (任务ID)  | Boolean            | 暂停任务（可恢复） |
| `resumeTask()`    | 恢复任务 | Long (任务ID)  | Boolean            | 恢复暂停的任务   |
| `deleteTask()`    | 删除任务 | Long (任务ID)  | Boolean            | 删除不需要的任务  |
| `triggerTask()`   | 立即触发 | Long (任务ID)  | Boolean            | 立即执行一次任务  |
| `findById()`      | ID查询 | Long (任务ID)  | TaskInfoVo         | 查询单个任务详情  |
| `findByJobName()` | 名称查询 | String (任务名) | TaskInfoVo         | 根据任务名查询   |
| `findAll()`       | 查询全部 | 无            | List\<TaskInfoVo\> | 获取所有任务    |
| `findByPage()`    | 分页查询 | TaskQueryDto | List\<TaskInfoVo\> | 条件查询+分页   |
| `batchStart()`    | 批量启动 | List\<Long\> | Integer (成功数)      | 批量启动多个任务  |
| `batchStop()`     | 批量停止 | List\<Long\> | Integer (成功数)      | 批量停止多个任务  |

### AlarmDubboService - 告警服务

| 方法                       | 功能   | 参数            | 返回值           | 使用场景     |
|--------------------------|------|---------------|---------------|----------|
| `sendAlarm()`            | 发送告警 | AlarmSendDto  | Boolean       | 发送单个告警邮件 |
| `batchSendAlarm()`       | 批量告警 | 邮箱列表、主题、内容    | Integer (成功数) | 群发告警     |
| `sendTaskFailureAlarm()` | 失败告警 | 任务名、错误信息、邮箱列表 | Boolean       | 任务失败通知   |
| `sendTaskTimeoutAlarm()` | 超时告警 | 任务名、超时时间、邮箱列表 | Boolean       | 任务超时通知   |

---

## 常见问题解答

### Q1: 如何创建不同类型的定时任务？

**A:** 根据需求选择不同的调度类型：

```java
// 1. Cron表达式 - 最灵活
task.setScheduleType(ScheduleType.CRON);
task.setCronExpression("0 0 2 * * ?");  // 每天凌晨2点

// 2. 固定频率 - 简单固定间隔
task.setScheduleType(ScheduleType.FIXED_RATE);
task.setRepeatInterval(300000L);  // 每5分钟

// 3. 固定延迟 - 任务完成后延迟
task.setScheduleType(ScheduleType.FIXED_DELAY);
task.setRepeatInterval(60000L);  // 完成后延迟1分钟再执行
```

**常用Cron表达式**：

```
0 * * * * ?        // 每分钟
0 0 * * * ?        // 每小时
0 0 2 * * ?        // 每天凌晨2点
0 0 2 * * MON      // 每周一凌晨2点
0 0 0 1 * ?        // 每月1号凌晨
0 0/5 * * * ?      // 每5分钟
0 0 9-17 * * MON-FRI  // 工作日9点到17点每小时
```

### Q2: 任务执行时间过长怎么办？

**A:** 设置合理的超时和重试策略：

```java
task.setTimeout(600);      // 10分钟超时
task.setRetryCount(3);     // 失败后重试3次
task.setRetryInterval(30); // 每次重试间隔30秒
task.setAlarmEmail("admin@example.com"); // 超时发送告警
```

### Q3: 如何避免任务重复执行？

**A:** 使用串行并发策略：

```java
task.setConcurrencyStrategy(ConcurrencyStrategy.SERIAL);
// 这样会等待上一次任务完成后再执行下一次
```

如果需要允许并发执行：

```java
task.setConcurrencyStrategy(ConcurrencyStrategy.PARALLEL);
```

### Q4: 如何传递参数给任务？

**A:** 使用 `jobData` 字段传递JSON格式的参数：

```java
Map<String, Object> params = new HashMap<>();
params.put("source", "mysql");
params.put("target", "redis");
params.put("batchSize", 1000);
params.put("enabled", true);

ObjectMapper mapper = new ObjectMapper();
task.setJobData(mapper.writeValueAsString(params));
```

在任务执行类中读取参数：

```java
@Component
public class MyTask implements ITask {

    @Override
    public void execute(String jobData) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> params = mapper.readValue(jobData, new TypeReference<Map<String, Object>>() {});

        String source = (String) params.get("source");
        Integer batchSize = (Integer) params.get("batchSize");

        // 执行业务逻辑
    }
}
```

### Q5: 本地任务和远程任务有什么区别？

**A:**

**本地任务 (TriggerType.LOCAL)**：

- 任务在当前服务内执行
- 需要提供任务类的全限定名或Bean名称
- 适合独立的服务模块

```java
task.setTriggerType(TriggerType.LOCAL);
task.setJobClass("com.example.task.LocalTask");
```

**远程任务 (TriggerType.REMOTE)**：

- 任务通过HTTP调用远程服务执行
- 需要提供目标应用名称和任务处理器名称
- 适合跨服务调用的场景

```java
task.setTriggerType(TriggerType.REMOTE);
task.setAppName("remote-service");      // 目标应用
task.setJobHandler("dataSyncHandler");  // 处理器名称
```

### Q6: 如何批量管理任务？

**A:** 使用批量操作接口：

```java
// 批量启动
List<Long> taskIds = Arrays.asList(1L, 2L, 3L, 4L, 5L);
Integer successCount = taskDubboService.batchStart(taskIds);
System.out.println("成功启动 " + successCount + " 个任务");

// 批量停止
Integer stoppedCount = taskDubboService.batchStop(taskIds);

// 分页查询
TaskQueryDto query = new TaskQueryDto();
query.setJobGroup("BATCH_JOB");
query.setPageNum(1);
query.setPageSize(100);
List<TaskInfoVo> tasks = taskDubboService.findByPage(query);
```

### Q7: 如何监控任务执行情况？

**A:** 查询任务状态和下次执行时间：

```java
public void monitorTasks() {
    List<TaskInfoVo> tasks = taskDubboService.findAll();

    for (TaskInfoVo task : tasks) {
        System.out.println("任务: " + task.getJobName());
        System.out.println("状态: " + task.getStatus().getDesc());
        System.out.println("下次执行: " + task.getNextFireTime());
        System.out.println("上次执行: " + task.getPreviousFireTime());

        if (task.getStatus() == TaskStatus.RUNNING) {
            System.out.println("任务运行中...");
        }
    }
}
```

### Q8: 任务失败后如何发送告警？

**A:** 使用告警服务：

```java
@DubboReference
private AlarmDubboService alarmDubboService;

public void notifyTaskFailure(String taskName, String errorMsg) {
    List<String> emails = Arrays.asList(
        "admin@example.com",
        "ops@example.com"
    );

    alarmDubboService.sendTaskFailureAlarm(
        taskName,    // 任务名称
        errorMsg,    // 错误信息
        emails       // 接收人列表
    );
}
```

或者手动发送自定义告警：

```java
AlarmSendDto alarm = new AlarmSendDto();
alarm.setEmail("admin@example.com");
alarm.setSubject("任务执行失败");
alarm.setContent("任务【数据同步】执行失败：连接超时");
alarm.setIsHtml(false);

alarmDubboService.sendAlarm(alarm);
```

### Q9: 如何立即执行任务而不影响定时调度？

**A:** 使用 `triggerTask()` 方法：

```java
// 立即执行一次，不影响正常的定时调度
Boolean result = taskDubboService.triggerTask(taskId);
```

这会立即触发任务执行，但不会改变任务的Cron调度时间。

### Q10: 更新任务时需要注意什么？

**A:**

1. **必须先停止任务**：

```java
// 1. 先停止
taskDubboService.stopTask(taskId);

// 2. 再更新
taskInfo.setId(taskId);
taskInfo.setCronExpression("0 30 * * * ?");  // 修改Cron表达式
taskDubboService.updateTask(taskInfo);

// 3. 重新启动
taskDubboService.startTask(taskId);
```

2. **必须提供任务ID**：

```java
TaskInfoDto taskInfo = new TaskInfoDto();
taskInfo.setId(taskId);  // 必须设置ID
// ... 设置其他字段
taskDubboService.updateTask(taskInfo);
```

3. **必填字段不能为空**：

```java
// 这些字段在更新时也不能为空
taskInfo.setJobName("任务名称");
taskInfo.setJobGroup("任务分组");
taskInfo.setJobClass("执行类");
taskInfo.setScheduleType(ScheduleType.CRON);
taskInfo.setConcurrencyStrategy(ConcurrencyStrategy.SERIAL);
taskInfo.setTriggerType(TriggerType.LOCAL);
```

---

## 最佳实践

### 1. 任务命名规范

```java
// 推荐格式：{业务模块}-{具体功能}
task.setJobName("数据同步-从MySQL到Redis");
task.setJobGroup("DATA_SYNC");

// 或使用层级结构
task.setJobName("DATA_SYNC.MYSQL_TO_REDIS");
task.setJobGroup("ETL");
```

### 2. 合理设置任务分组

```java
// 按功能分组
task.setJobGroup("DATA_SYNC");    // 数据同步
task.setJobGroup("REPORT");       // 报表生成
task.setJobGroup("MAINTENANCE");  // 维护任务
task.setJobGroup("MONITOR");      // 监控任务
```

### 3. 并发策略选择

```java
// 有状态的任务 - 使用串行
task.setConcurrencyStrategy(ConcurrencyStrategy.SERIAL);
// 适用场景：数据同步、文件处理、状态计算等

// 无状态的任务 - 使用并行
task.setConcurrencyStrategy(ConcurrencyStrategy.PARALLEL);
// 适用场景：数据统计、健康检查、通知发送等
```

### 4. 超时和重试配置

```java
// 快速任务（秒级）
task.setTimeout(60);      // 1分钟超时
task.setRetryCount(3);    // 重试3次
task.setRetryInterval(10); // 间隔10秒

// 中等任务（分钟级）
task.setTimeout(600);     // 10分钟超时
task.setRetryCount(2);    // 重试2次
task.setRetryInterval(60); // 间隔1分钟

// 长时任务（小时级）
task.setTimeout(3600);    // 1小时超时
task.setRetryCount(0);    // 不重试或重试1次
task.setRetryInterval(300); // 间隔5分钟
```

### 5. 告警配置

```java
// 单个接收人
task.setAlarmEmail("admin@example.com");

// 多个接收人（逗号分隔）
task.setAlarmEmail("admin@example.com,ops@example.com,dev@example.com");

// 按角色分组
task.setAlarmEmail("admin@example.com");       // 管理员
task.setAlarmEmail("ops@example.com");         // 运维
task.setAlarmEmail("dev@example.com");         // 开发
```

### 6. 参数传递规范

```java
// 使用结构化的JSON数据
Map<String, Object> params = new HashMap<>();

// 基本类型
params.put("enabled", true);
params.put("retryCount", 3);
params.put("timeout", 600);

// 字符串
params.put("source", "mysql");
params.put("target", "redis");
params.put("tableName", "user_data");

// 集合类型
params.put("fields", Arrays.asList("id", "name", "email"));
params.put("filters", Map.of("status", "active"));

// 嵌套对象
Map<String, Object> connection = new HashMap<>();
connection.put("host", "localhost");
connection.put("port", 3306);
connection.put("database", "mydb");
params.put("connection", connection);

task.setJobData(new ObjectMapper().writeValueAsString(params));
```

### 7. 任务生命周期管理

```java
@Service
public class TaskLifecycleManager {

    @DubboReference
    private TaskDubboService taskDubboService;

    // 创建任务的标准流程
    public Long createTask(TaskInfoDto taskInfo) {
        // 1. 创建任务
        Long taskId = taskDubboService.addTask(taskInfo);
        System.out.println("任务已创建: " + taskId);

        // 2. 启动任务
        Boolean started = taskDubboService.startTask(taskId);
        if (!started) {
            System.err.println("任务启动失败: " + taskId);
        }

        return taskId;
    }

    // 更新任务的标准流程
    public void updateTask(Long taskId, TaskInfoDto newInfo) {
        // 1. 停止任务
        taskDubboService.stopTask(taskId);

        // 2. 等待停止完成
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 3. 更新任务
        newInfo.setId(taskId);
        taskDubboService.updateTask(newInfo);

        // 4. 重新启动
        taskDubboService.startTask(taskId);
    }

    // 删除任务的标准流程
    public void deleteTask(Long taskId) {
        // 1. 停止任务
        taskDubboService.stopTask(taskId);

        // 2. 删除任务
        Boolean deleted = taskDubboService.deleteTask(taskId);
        if (deleted) {
            System.out.println("任务已删除: " + taskId);
        }
    }

    // 暂停和恢复
    public void pauseAndResume(Long taskId) {
        // 暂停任务
        taskDubboService.pauseTask(taskId);

        // ... 执行某些操作 ...

        // 恢复任务
        taskDubboService.resumeTask(taskId);
    }
}
```

### 8. 错误处理

```java
@Service
public class RobustTaskService {

    @DubboReference
    private TaskDubboService taskDubboService;

    @DubboReference
    private AlarmDubboService alarmDubboService;

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
                sendAlarm("任务启动失败", task.getJobName());
            }

        } catch (RpcException e) {
            System.err.println("RPC调用失败: " + e.getMessage());
            // 可以实现重试逻辑
        } catch (Exception e) {
            System.err.println("未知错误: " + e.getMessage());
            sendAlarm("任务异常", e.getMessage());
        }
    }

    private void sendAlarm(String subject, String content) {
        AlarmSendDto alarm = new AlarmSendDto();
        alarm.setEmail("admin@example.com");
        alarm.setSubject(subject);
        alarm.setContent(content);
        alarmDubboService.sendAlarm(alarm);
    }
}
```

---

## 进阶技巧

### 1. 动态任务管理

根据业务需求动态创建和销毁任务：

```java
@Service
public class DynamicTaskManager {

    @DubboReference
    private TaskDubboService taskDubboService;

    private Map<String, Long> taskCache = new ConcurrentHashMap<>();

    // 为用户创建个性化任务
    public Long createUserTask(String userId, String cron) {
        String taskName = "USER_TASK_" + userId;

        // 检查是否已存在
        if (taskCache.containsKey(taskName)) {
            return taskCache.get(taskName);
        }

        // 创建新任务
        TaskInfoDto task = new TaskInfoDto();
        task.setJobName(taskName);
        task.setJobGroup("USER_TASK");
        task.setDescription("用户 " + userId + " 的个性化任务");
        task.setJobClass("com.example.task.UserTask");
        task.setScheduleType(ScheduleType.CRON);
        task.setCronExpression(cron);
        task.setConcurrencyStrategy(ConcurrencyStrategy.SERIAL);
        task.setTriggerType(TriggerType.LOCAL);

        // 设置用户参数
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        task.setJobData(new ObjectMapper().writeValueAsString(params));

        Long taskId = taskDubboService.addTask(task);
        taskDubboService.startTask(taskId);

        taskCache.put(taskName, taskId);
        return taskId;
    }

    // 删除用户任务
    public void deleteUserTask(String userId) {
        String taskName = "USER_TASK_" + userId;
        Long taskId = taskCache.get(taskName);

        if (taskId != null) {
            taskDubboService.stopTask(taskId);
            taskDubboService.deleteTask(taskId);
            taskCache.remove(taskName);
        }
    }
}
```

### 2. 任务监控和统计

```java
@Service
public class TaskMonitorService {

    @DubboReference
    private TaskDubboService taskDubboService;

    @Scheduled(cron = "0 */5 * * * ?")  // 每5分钟执行
    public void monitorTasks() {
        List<TaskInfoVo> tasks = taskDubboService.findAll();

        Map<TaskStatus, Long> statusCount = tasks.stream()
            .collect(Collectors.groupingBy(
                TaskInfoVo::getStatus,
                Collectors.counting()
            ));

        System.out.println("=== 任务统计 ===");
        System.out.println("总任务数: " + tasks.size());
        System.out.println("运行中: " + statusCount.getOrDefault(TaskStatus.RUNNING, 0L));
        System.out.println("已停止: " + statusCount.getOrDefault(TaskStatus.STOPPED, 0L));
        System.out.println("已暂停: " + statusCount.getOrDefault(TaskStatus.PAUSED, 0L));

        // 检查异常任务
        tasks.stream()
            .filter(t -> t.getStatus() == TaskStatus.STOPPED)
            .filter(t -> t.getJobGroup().equals("CRITICAL"))
            .forEach(t -> {
                System.err.println("关键任务已停止: " + t.getJobName());
                // 发送告警
            });
    }
}
```

### 3. 分批处理大量任务

```java
@Service
public class BatchTaskProcessor {

    @DubboReference
    private TaskDubboService taskDubboService;

    private static final int BATCH_SIZE = 50;

    public void batchProcessAllTasks() {
        TaskQueryDto query = new TaskQueryDto();
        query.setPageNum(1);
        query.setPageSize(BATCH_SIZE);

        int page = 1;
        while (true) {
            query.setPageNum(page);
            List<TaskInfoVo> tasks = taskDubboService.findByPage(query);

            if (tasks.isEmpty()) {
                break;
            }

            // 处理当前批次
            processBatch(tasks);

            page++;
        }
    }

    private void processBatch(List<TaskInfoVo> tasks) {
        List<Long> taskIds = tasks.stream()
            .map(TaskInfoVo::getId)
            .collect(Collectors.toList());

        // 批量启动
        Integer started = taskDubboService.batchStart(taskIds);
        System.out.println("批次处理完成，启动 " + started + " 个任务");

        // 避免过快
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### 4. 任务依赖管理

实现任务之间的依赖关系：

```java
@Service
public class TaskDependencyManager {

    @DubboReference
    private TaskDubboService taskDubboService;

    // 创建依赖任务链
    public void createTaskChain() {
        // 1. 创建第一个任务（数据抽取）
        TaskInfoDto task1 = new TaskInfoDto();
        task1.setJobName("1-数据抽取");
        task1.setJobGroup("ETL_CHAIN");
        task1.setJobClass("com.example.task.ExtractTask");
        task1.setScheduleType(ScheduleType.CRON);
        task1.setCronExpression("0 0 2 * * ?");
        task1.setConcurrencyStrategy(ConcurrencyStrategy.SERIAL);
        task1.setTriggerType(TriggerType.LOCAL);

        Long taskId1 = taskDubboService.addTask(task1);

        // 2. 创建第二个任务（数据转换）
        TaskInfoDto task2 = new TaskInfoDto();
        task2.setJobName("2-数据转换");
        task2.setJobGroup("ETL_CHAIN");
        task2.setJobClass("com.example.task.TransformTask");
        task2.setScheduleType(ScheduleType.CRON);
        task2.setCronExpression("0 30 2 * * ?");  // 抽取后30分钟
        task2.setConcurrencyStrategy(ConcurrencyStrategy.SERIAL);
        task2.setTriggerType(TriggerType.LOCAL);

        Long taskId2 = taskDubboService.addTask(task2);

        // 3. 创建第三个任务（数据加载）
        TaskInfoDto task3 = new TaskInfoDto();
        task3.setJobName("3-数据加载");
        task3.setJobGroup("ETL_CHAIN");
        task3.setJobClass("com.example.task.LoadTask");
        task3.setScheduleType(ScheduleType.CRON);
        task3.setCronExpression("0 0 3 * * ?");  // 转换后30分钟
        task3.setConcurrencyStrategy(ConcurrencyStrategy.SERIAL);
        task3.setTriggerType(TriggerType.LOCAL);

        Long taskId3 = taskDubboService.addTask(task3);

        // 按顺序启动
        taskDubboService.startTask(taskId1);
        taskDubboService.startTask(taskId2);
        taskDubboService.startTask(taskId3);
    }
}
```

### 5. 任务模板管理

创建可复用的任务模板：

```java
@Component
public class TaskTemplate {

    @DubboReference
    private TaskDubboService taskDubboService;

    // 每小时任务模板
    public Long createHourlyTask(String name, String jobClass, Map<String, Object> params) {
        TaskInfoDto task = new TaskInfoDto();
        task.setJobName(name);
        task.setJobGroup("HOURLY");
        task.setJobClass(jobClass);
        task.setScheduleType(ScheduleType.CRON);
        task.setCronExpression("0 0 * * * ?");
        task.setConcurrencyStrategy(ConcurrencyStrategy.SERIAL);
        task.setTriggerType(TriggerType.LOCAL);
        task.setRetryCount(2);
        task.setTimeout(300);

        if (params != null) {
            task.setJobData(new ObjectMapper().writeValueAsString(params));
        }

        Long taskId = taskDubboService.addTask(task);
        taskDubboService.startTask(taskId);
        return taskId;
    }

    // 每天任务模板
    public Long createDailyTask(String name, String jobClass, String cron, Map<String, Object> params) {
        TaskInfoDto task = new TaskInfoDto();
        task.setJobName(name);
        task.setJobGroup("DAILY");
        task.setJobClass(jobClass);
        task.setScheduleType(ScheduleType.CRON);
        task.setCronExpression(cron);
        task.setConcurrencyStrategy(ConcurrencyStrategy.SERIAL);
        task.setTriggerType(TriggerType.LOCAL);
        task.setRetryCount(3);
        task.setTimeout(1800);

        if (params != null) {
            task.setJobData(new ObjectMapper().writeValueAsString(params));
        }

        Long taskId = taskDubboService.addTask(task);
        taskDubboService.startTask(taskId);
        return taskId;
    }

    // 固定频率任务模板
    public Long createFixedRateTask(String name, String jobClass, long interval, ConcurrencyStrategy strategy) {
        TaskInfoDto task = new TaskInfoDto();
        task.setJobName(name);
        task.setJobGroup("FIXED_RATE");
        task.setJobClass(jobClass);
        task.setScheduleType(ScheduleType.FIXED_RATE);
        task.setRepeatInterval(interval);
        task.setConcurrencyStrategy(strategy);
        task.setTriggerType(TriggerType.LOCAL);
        task.setRetryCount(1);
        task.setTimeout(60);

        Long taskId = taskDubboService.addTask(task);
        taskDubboService.startTask(taskId);
        return taskId;
    }
}

// 使用模板
@Service
public class TaskService {

    @Autowired
    private TaskTemplate taskTemplate;

    public void createTasks() {
        // 使用模板创建任务
        taskTemplate.createHourlyTask(
            "用户数据统计",
            "com.example.task.UserStatisticsTask",
            Map.of("metrics", Arrays.asList("active", "new"))
        );

        taskTemplate.createDailyTask(
            "数据备份",
            "com.example.task.BackupTask",
            "0 0 3 * * ?",  // 每天凌晨3点
            Map.of("target", "s3")
        );

        taskTemplate.createFixedRateTask(
            "健康检查",
            "com.example.task.HealthCheckTask",
            60000,  // 每分钟
            ConcurrencyStrategy.PARALLEL
        );
    }
}
```

---

## 附录

### A. Cron表达式快速参考

| 表达式                    | 说明           |
|------------------------|--------------|
| `0 * * * * ?`          | 每分钟          |
| `0 0 * * * ?`          | 每小时          |
| `0 0 0 * * ?`          | 每天凌晨         |
| `0 0 0 * * MON`        | 每周一凌晨        |
| `0 0 0 1 * ?`          | 每月1号凌晨       |
| `0 0/5 * * * ?`        | 每5分钟         |
| `0 0 9-17 * * MON-FRI` | 工作日9点到17点每小时 |
| `0 0,30 * * * ?`       | 每小时的0分和30分   |
| `0 0 8 ? * MON-FRI`    | 工作日上午8点      |
| `0 0 0 1 1 ?`          | 每年1月1号凌晨     |

**Cron表达式格式**：

```
秒 分 时 日 月 周 [年]

字段  允许值  允许特殊字符
秒    0-59    , - * /
分    0-59    , - * /
时    0-23    , - * /
日    1-31    , - * ? / L W
月    1-12    , - * /
周    1-7     , - * ? / L #
```

### B. 任务状态转换图

```
STOPPED --[startTask()]--> RUNNING
RUNNING --[stopTask()]--> STOPPED
RUNNING --[pauseTask()]--> PAUSED
PAUSED --[resumeTask()]--> RUNNING
STOPPED --[deleteTask()]--> [DELETED]
```

### C. 联系方式

如有问题或建议，请联系开发团队或查阅以下资源：

- 项目文档: [项目地址]
- 问题反馈: [Issue Tracker]
- 技术支持: [技术支持邮箱]

---

**版本**: ${revision}
**更新日期**: 2025-02-06
