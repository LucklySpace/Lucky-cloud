# im-starter-job 分布式任务客户端 SDK

## 简介

`im-starter-job` 是 Lucky Cloud 分布式任务调度系统的客户端 SDK。它允许微服务应用轻松接入调度中心，实现任务的自动注册、调度执行和状态上报。

## 功能特性

- **自动注册**：应用启动时自动将服务信息和任务列表注册到调度中心。
- **任务注解**：使用 `@LuckyJob` 注解轻松定义任务方法。
- **HTTP 调度**：通过 HTTP 接口接收调度中心的执行请求。
- **参数传递**：支持传递任务参数。

## 快速开始

### 1. 引入依赖

在项目的 `pom.xml` 中引入依赖：

```xml
<dependency>
    <groupId>com.xy.lucky</groupId>
    <artifactId>im-starter-job</artifactId>
    <version>${revision}</version>
</dependency>
```

### 2. 配置属性

在 `application.yml` 中配置调度中心地址：

```yaml
lucky:
  job:
    enabled: true
    admin-addresses: http://localhost:8080/im-quartz
    app-name: ${spring.application.name}
    # ip: 127.0.0.1 # 可选，默认自动获取
    # port: 8081 # 可选，默认自动获取 server.port
    # access-token: xxx # 可选，鉴权令牌
```

### 3. 编写任务

在 Spring Bean 的方法上添加 `@LuckyJob` 注解：

```java
@Component
public class MyJobTask {

    @LuckyJob("demoJob")
    public void demoJob() {
        System.out.println("Demo job executed!");
    }

    @LuckyJob(value = "paramJob", description = "带参数的任务")
    public void paramJob(String params) {
        System.out.println("Param job executed with: " + params);
    }
}
```

## 架构说明

- **JobRegistry**: 扫描并注册本地任务。
- **JobAdminClient**: 定期向调度中心发送心跳和注册信息。
- **JobExecutorController**: 暴露 HTTP 接口供调度中心调用。
