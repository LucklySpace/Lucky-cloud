# im-connect 开发文档

## 目录

- [模块概述](#模块概述)
- [技术架构](#技术架构)
- [核心功能](#核心功能)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [核心组件](#核心组件)
- [协议说明](#协议说明)
- [消息流程](#消息流程)
- [服务注册与发现](#服务注册与发现)
- [开发指南](#开发指南)
- [部署运维](#部署运维)
- [常见问题](#常见问题)

---

## 模块概述

`im-connect` 是 Lucky Cloud IM 系统的**连接层服务**，负责处理客户端的 WebSocket 和 TCP 连接，是整个 IM 系统的入口网关。

### 核心职责

1. **连接管理**：维护客户端与服务器之间的长连接
2. **消息收发**：接收客户端消息并转发到消息队列
3. **用户会话**：管理用户登录状态和多设备连接
4. **心跳保活**：检测并清理僵尸连接
5. **服务注册**：向 Nacos 注册服务实例信息

### 技术特点

- **双协议支持**：同时支持 WebSocket 和 TCP 连接
- **多序列化**：支持 JSON 和 Protobuf 序列化协议
- **高性能**：基于 Netty NIO 框架，支持 EPOLL（Linux）
- **多端口**：支持绑定多个端口分散连接压力
- **多设备**：支持用户同时在多个设备类型上登录
- **虚拟线程**：使用 Java 21 虚拟线程提升并发性能

---

## 技术架构

### 技术栈

| 技术       | 版本            | 用途      |
|----------|---------------|---------|
| Java     | 21            | 基础运行环境  |
| Netty    | 4.2.6.Final   | 网络通信框架  |
| RabbitMQ | 5.20.0        | 消息队列    |
| Redis    | 4.4.0 (Jedis) | 缓存与路由   |
| Nacos    | 3.1.1         | 服务注册与发现 |
| Protobuf | 4.29.5        | 序列化协议   |
| Jackson  | 2.15.2        | JSON 处理 |

### 架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         客户端层                                │
│     Web 浏览器 │ 移动端 APP │ 桌面端 │ IoT 设备                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                    ┌─────────┴─────────┐
                    │   WebSocket / TCP   │
                    └─────────┬─────────┘
                              │
┌─────────────────────────────────────────────────────────────────┐
│                      im-connect 服务                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ WebSocket    │  │    TCP       │  │   连接管理    │          │
│  │  模板        │  │   模板       │  │ UserChannelMap│          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   登录处理   │  │  心跳处理    │  │   消息处理   │          │
│  │ LoginHandler │  │HeartBeatHandler│ MessageHandler│          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  编解码器    │  │  协议切换    │  │  限流保护    │          │
│  │Codec Handler │  │ JSON/Proto   │  │   Sentinel   │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
└─────────────────────────────────────────────────────────────────┘
                    │                        │
        ┌───────────┴──────────┐  ┌────────┴─────────┐
        │                      │  │                  │
   ┌────▼────┐          ┌─────▼──▼────┐    ┌──────▼──────┐
   │ RabbitMQ│          │    Nacos    │    │   Redis     │
   │ 消息队列  │          │  服务注册   │    │  路由缓存   │
   └─────────┘          └─────────────┘    └─────────────┘
```

---

## 核心功能

### 1. 连接接入

- **WebSocket 接入**：支持 WebSocket 协议，路径默认为 `/im`
- **TCP 接入**：支持原生 TCP Socket 连接
- **多端口监听**：可配置多个端口分散连接压力
- **自动重连**：客户端断线后可自动重连

### 2. 用户认证

- **Token 验证**：验证用户 Token 有效性
- **设备类型识别**：支持 Web、iOS、Android、桌面等多种设备类型
- **多设备管理**：支持用户多端同时在线，可配置同组互斥

### 3. 消息处理

- **消息类型**：
  - 单聊消息 (SINGLE_MESSAGE)
  - 群聊消息 (GROUP_MESSAGE)
  - 视频消息 (VIDEO_MESSAGE)
  - 强制下线 (FORCE_LOGOUT)
  - 群组操作 (GROUP_OPERATION)
  - 消息操作 (MESSAGE_OPERATION - 撤回/编辑)
- **消息流转**：客户端 → im-connect → RabbitMQ → 后端服务
- **错误处理**：消息处理失败发送到错误队列

### 4. 心跳保活

- **客户端心跳**：客户端定期发送心跳包
- **服务端检测**：检测连接空闲时间
- **超时清理**：超时连接自动断开并清理资源

### 5. 服务注册

- **Nacos 注册**：服务启动时向 Nacos 注册实例
- **元数据上报**：上报连接数、协议类型、端口等信息
- **周期性更新**：定期更新连接数等动态元数据

---

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.6+
- Redis 3.2+
- RabbitMQ 3.8+
- Nacos 2.0+

### 编译运行

```bash
# 进入项目目录
cd im-connect

# 编译项目
mvn clean compile

# 打包项目
mvn clean package

# 运行服务
java -jar target/im-connect-0.0.1-SNAPSHOT-jar-with-dependencies.jar
```

### Docker 部署

```bash
# 构建镜像
docker build -t im-connect:latest .

# 运行容器
docker run -d \
  --name im-connect \
  -p 19000:19000 \
  -p 19001:19001 \
  -p 19002:19002 \
  -e SPRING_PROFILES_ACTIVE=prod \
  im-connect:latest
```

---

## 配置说明

### 完整配置示例

```yaml
app:
  name: im-connect

spring:
  profiles:
    active: dev

# ===========================================
# RabbitMQ 配置
# ===========================================
rabbitmq:
  address: 124.220.10.188      # RabbitMQ 地址
  port: 5672                   # RabbitMQ 端口
  username: guest              # 用户名
  password: qsczse111          # 密码
  virtual: /                   # 虚拟主机
  exchange: IM-SERVER          # 交换机名称
  routingKeyPrefix: IM-        # 路由键前缀
  errorQueue: im.error         # 错误队列
  connectionTimeout: 60000     # 连接超时时间（毫秒）
  automaticRecovery: true      # 自动重连

# ===========================================
# Nacos 配置
# ===========================================
nacos:
  enable: true
  config:
    name: im-connect           # 服务名称
    address: localhost         # Nacos 地址
    port: 8848                 # Nacos 端口
    group: DEFAULT_GROUP       # 分组
    username: nacos            # 用户名
    password: nacos            # 密码
    version: 1.0.0             # 版本号
    namespace: ""              # 命名空间

# ===========================================
# Netty 配置
# ===========================================
netty:
  config:
    # 序列化协议: json 或 proto（推荐 proto）
    protocol: proto

    # 心跳超时时间（毫秒）
    heartBeatTime: 30000

    # 是否允许多设备登录
    multiDeviceEnabled: true

    # Boss 线程池大小（处理连接请求）
    bossThreadSize: 4

    # Worker 线程池大小（处理 I/O 操作）
    workThreadSize: 16

    # TCP 配置
    tcp:
      enable: true              # 是否启用 TCP
      port: # TCP 监听端口列表
        - 9000
        - 9001
        - 9002

    # WebSocket 配置
    websocket:
      enable: true              # 是否启用 WebSocket
      path: /im                 # WebSocket 路径
      port: # WebSocket 监听端口列表
        - 19000
        - 19001
        - 19002

# ===========================================
# Redis 配置
# ===========================================
redis:
  host: 124.220.10.188
  port: 6379
  password: Lucky20251001
  timeout: 10000               # 超时时间（毫秒）
  database: 0                  # 数据库索引
  maxTotal: 8                  # 最大连接数
  maxIdle: 8                   # 最大空闲连接
  minIdle: 0                   # 最小空闲连接

# ===========================================
# 认证配置
# ===========================================
auth:
  tokenExpired: 3              # Token 过期时间（天）
```

### 配置项详解

#### Netty 配置

| 配置项                               | 类型      | 默认值     | 说明                     |
|-----------------------------------|---------|---------|------------------------|
| `netty.config.protocol`           | String  | `proto` | 序列化协议：`json` 或 `proto` |
| `netty.config.heartBeatTime`      | int     | `30000` | 心跳超时时间（毫秒）             |
| `netty.config.multiDeviceEnabled` | boolean | `true`  | 是否允许多设备登录              |
| `netty.config.bossThreadSize`     | int     | `4`     | Boss 线程池大小             |
| `netty.config.workThreadSize`     | int     | `16`    | Worker 线程池大小           |

#### 协议选择建议

- **Protobuf**（推荐）：性能高、体积小，适合生产环境
- **JSON**：易调试、可读性好，适合开发测试环境

#### 端口规划建议

- WebSocket 端口：19000-19099（100个端口）
- TCP 端口：9000-9099（100个端口）

---

## 核心组件

### 1. WebSocketTemplate

**位置**：`com.xy.lucky.connect.netty.service.websocket.WebSocketTemplate`

**职责**：WebSocket 服务器实现

**核心功能**：

- 多端口绑定与监听
- 协议编解码器切换（JSON/Proto）
- 异步启动与优雅关闭
- Nacos 服务注册

**关键方法**：

```java
// 启动 WebSocket 服务器
public void run(ApplicationArguments args)

// 绑定新端口（运行时动态绑定）
public boolean bindNewPort(int port)

// 关闭指定端口
public boolean closePort(int port)

// 检查服务状态
public boolean isReady()

// 获取已绑定端口列表
public List<Integer> getBoundPorts()
```

**Pipeline 处理链**：

```
HTTP 请求 → HttpServerCodec → HttpObjectAggregator → ChunkedWriteHandler
→ AuthHandler → WebSocketServerProtocolHandler → ProtocolHandler → 业务处理
```

### 2. TCPSocketTemplate

**位置**：`com.xy.lucky.connect.netty.service.tcp.TCPSocketTemplate`

**职责**：TCP 服务器实现

**核心功能**：

- TCP Socket 连接管理
- 粘包/拆包处理
- 长度字段编解码

**协议格式**：

```
+--------+----------------+
| Length |    Payload     |
| 4 bytes|   N bytes      |
+--------+----------------+
```

**Pipeline 处理链**：

```
TCP 连接 → LengthFieldBasedFrameDecoder → LengthFieldPrepender
→ AuthHandler → ProtocolHandler → 业务处理
```

### 3. UserChannelMap

**位置**：`com.xy.lucky.connect.channel.UserChannelMap`

**职责**：用户与 Channel 的映射管理

**核心功能**：

- 用户多设备连接管理
- 同组设备互斥
- 连接状态维护

**设备分组**：

| 分组      | 设备类型                | 互斥规则 |
|---------|---------------------|------|
| MOBILE  | iOS、Android         | 同组互斥 |
| DESKTOP | Windows、macOS、Linux | 同组互斥 |
| WEB     | Web浏览器              | 同组互斥 |

**关键方法**：

```java
// 添加用户通道
public void addChannel(String userId, Channel ch, IMDeviceType deviceType)

// 获取用户指定设备的 Channel
public Channel getChannel(String userId, IMDeviceType deviceType)

// 获取用户所有在线 Channel
public Collection<Channel> getChannelsByUser(String userId)

// 移除用户特定设备的通道
public void removeChannel(String userId, String deviceTypeStr, boolean close)

// 根据 Channel 清理资源
public void removeByChannel(Channel channel)

// 获取在线用户数
public int getOnlineUserCount()

// 获取总连接数
public int getTotalConnectionCount()
```

### 4. MessageHandler

**位置**：`com.xy.lucky.connect.message.MessageHandler`

**职责**：消息分发与处理

**消息类型处理**：

```java
switch(msgType){
        case SINGLE_MESSAGE    → singleMessageProcess.

dispose()
    case GROUP_MESSAGE     → groupMessageProcess.

dispose()
    case VIDEO_MESSAGE     → videoMessageProcess.

dispose()
    case FORCE_LOGOUT      → forceLogoutProcess.

dispose()
    case GROUP_OPERATION   → groupOperationProcess.

dispose()
    case MESSAGE_OPERATION → messageActionProcess.

dispose()
}
```

### 5. LoginHandler

**位置**：`com.xy.lucky.connect.netty.LoginHandler`

**职责**：处理用户登录请求

**处理流程**：

1. 接收 REGISTER 类型消息
2. 验证 Token
3. 绑定用户与 Channel
4. 注册到 UserChannelMap
5. 同步路由信息到 Redis

### 6. HeartBeatHandler

**位置**：`com.xy.lucky.connect.netty.HeartBeatHandler`

**职责**：心跳处理与超时检测

**处理流程**：

1. 接收 HEART_BEAT 类型消息
2. 更新连接活跃时间
3. 检测空闲超时
4. 清理超时连接

### 7. RabbitTemplate

**位置**：`com.xy.lucky.connect.mq.RabbitTemplate`

**职责**：RabbitMQ 消息队列操作

**核心功能**：

- 队列声明与绑定
- 消息消费与发送
- 错误消息处理
- 自动重连

**队列说明**：

- **业务队列**：`IM-{brokerId}`，每个服务实例独有
- **错误队列**：`im.error`，存储处理失败的消息

### 8. RedisTemplate

**位置**：`com.xy.lucky.connect.redis.RedisTemplate`

**职责**：Redis 操作封装

**核心功能**：

- 连接池管理
- 批量操作（Pipeline）
- 常用数据结构操作

### 9. NacosTemplate

**位置**：`com.xy.lucky.connect.nacos.NacosTemplate`

**职责**：Nacos 服务注册与元数据管理

**核心功能**：

- 服务实例注册
- 元数据周期性上报
- 连接数统计上报

**上报的元数据**：

```json
{
  "brokerId": "机器码",
  "version": "1.0.0",
  "wsPath": "/im",
  "protocols": "[\"proto\"]",
  "connection": "连接数",
  "region": "cn-shanghai",
  "priority": "1"
}
```

---

## 协议说明

### Protobuf 消息格式

**定义文件**：`src/main/resources/im_message_wrap.proto`

```protobuf
message IMessageWrap {
  int32 code = 1;                    // 消息类型码
  string token = 2;                  // 认证令牌
  google.protobuf.Any data = 3;      // 消息数据（任意类型）
  map<string, string> metadata = 4;  // 元数据
  string message = 5;                // 消息内容
  string request_id = 6;             // 请求ID（链路追踪）
  int64 timestamp = 7;               // 时间戳（毫秒）
  string client_ip = 8;              // 客户端IP
  string user_agent = 9;             // 用户代理
  string device_name = 10;           // 设备名称
  string device_type = 11;           // 设备类型
}
```

### 消息类型枚举

| 枚举值               | Code | 说明          |
|-------------------|------|-------------|
| REGISTER          | 1    | 用户注册/登录     |
| HEART_BEAT        | 2    | 心跳          |
| SINGLE_MESSAGE    | 3    | 单聊消息        |
| GROUP_MESSAGE     | 4    | 群聊消息        |
| VIDEO_MESSAGE     | 5    | 视频消息        |
| FORCE_LOGOUT      | 6    | 强制下线        |
| GROUP_OPERATION   | 7    | 群组操作        |
| MESSAGE_OPERATION | 8    | 消息操作（撤回/编辑） |

### WebSocket 连接流程

```
客户端                    im-connect                  后端服务
  │                          │                           │
  │  1. WebSocket 握手       │                           │
  ├─────────────────────────>│                           │
  │                          │                           │
  │  2. 发送登录消息          │                           │
  ├─────────────────────────>│                           │
  │  (code=REGISTER, token)  │                           │
  │                          │                           │
  │                          │  3. 验证Token             │
  │                          │  4. 绑定Channel           │
  │                          │  5. 更新Redis路由         │
  │                          │                           │
  │  6. 登录成功响应          │                           │
  │<─────────────────────────┤                           │
  │                          │                           │
  │  7. 发送单聊消息          │                           │
  ├─────────────────────────>│                           │
  │  (code=SINGLE_MESSAGE)   │                           │
  │                          │  8. 转发到RabbitMQ        │
  │                          ├─────────────────────────>│
  │                          │                           │
  │                          │  9. 处理消息              │
  │                          │  10. 回推消息到MQ         │
  │                          │<─────────────────────────┤
  │                          │                           │
  │  11. 推送消息给接收方      │                           │
  │<─────────────────────────┤                           │
  │                          │                           │
  │  12. 定期发送心跳         │                           │
  ├─────────────────────────>│                           │
  │  (code=HEART_BEAT)       │                           │
```

---

## 消息流程

### 1. 登录流程

```
LoginHandler.channelRead0()
    ↓
LoginProcess.process()
    ↓
1. 验证 Token
2. 提取用户ID和设备类型
3. 检查是否需要踢出旧连接
4. UserChannelMap.addChannel()
5. RedisTemplate.setnxWithTimeOut()  # 更新路由
6. 返回登录成功响应
```

### 2. 消息发送流程

```
客户端发送消息
    ↓
ProtocolHandler.decode()
    ↓
MessageHandler.handleMessage()  # 监听 MessageEvent
    ↓
根据消息类型分发:
    - SingleMessageProcess
    - GroupMessageProcess
    - VideoMessageProcess
    ↓
RabbitTemplate.sendToBroker()
    ↓
RabbitMQ 队列
    ↓
后端服务消费处理
```

### 3. 消息接收流程

```
RabbitMQ 队列
    ↓
RabbitTemplate.deliverCallback()
    ↓
发布 MessageEvent 事件
    ↓
找到接收方所在 im-connect 实例
    ↓
UserChannelMap.getChannel()
    ↓
channel.writeAndFlush()  # 推送给客户端
```

### 4. 心跳流程

```
客户端定期发送心跳
    ↓
HeartBeatHandler.channelRead0()
    ↓
HeartBeatProcess.process()
    ↓
更新 Channel 活跃时间
    ↓
返回心跳响应
```

### 5. 超时断开流程

```
IdleStateEvent 触发
    ↓
HeartBeatHandler.userEventTriggered()
    ↓
检测到 ALL_IDLE 超时
    ↓
ChannelCleanupHelper.cleanup()
    ↓
1. UserChannelMap.removeByChannel()
2. RedisTemplate.del()  # 删除路由
3. channel.close()
```

---

## 服务注册与发现

### Nacos 注册流程

```
服务启动
    ↓
WebSocketTemplate.run()
    ↓
端口绑定成功
    ↓
NacosTemplate.batchRegisterNacos()
    ↓
构建 Instance 对象
    ↓
设置元数据 (brokerId, version, protocols, etc.)
    ↓
namingService.batchRegisterInstance()
    ↓
启动周期性上报任务 (10s 间隔)
    ↓
定期更新 connection 元数据
```

### 元数据结构

```java
{
        "brokerId":"a1b2c3d4-e5f6-7890-abcd-ef1234567890",  // 机器唯一标识
        "version":"1.0.0",                                    // 服务版本
        "wsPath":"/im",                                       // WebSocket 路径
        "protocols":"[\"proto\"]",                            // 支持的协议
        "connection":"1234",                                  // 当前连接数
        "region":"cn-shanghai",                               // 区域
        "priority":"1"                                        // 优先级
        }
```

### 获取服务列表

后端服务可以从 Nacos 获取所有 im-connect 实例：

```java
// 获取所有健康实例
List<Instance> instances = namingService.getAllInstances(
                "im-connect",
                "DEFAULT_GROUP",
                true  // 只获取健康实例
        );

// 选择连接数最少的实例
Instance target = instances.stream()
        .min(Comparator.comparing(i ->
                Integer.parseInt(i.getMetadata().get("connection"))))
        .orElseThrow();
```

---

## 开发指南

### 添加新的消息类型

#### 1. 定义消息类型枚举

在 `im-core` 模块的 `IMessageType` 枚举中添加：

```java
NEW_MESSAGE_TYPE(9,"新消息类型")
```

#### 2. 创建消息处理器

```java
package com.xy.lucky.connect.message.process.impl;

import com.xy.lucky.connect.message.process.MessageProcess;
import com.xy.lucky.core.enums.IMessageType;
import com.xy.lucky.core.model.IMessageWrap;
import com.xy.lucky.spring.annotations.core.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class NewMessageProcess implements MessageProcess<Object> {

  @Override
  public void dispose(IMessageWrap<Object> messageWrap) {
    // 处理逻辑
    log.info("处理新消息类型: {}", messageWrap);
  }

  @Override
  public IMessageType getSupportedType() {
    return IMessageType.NEW_MESSAGE_TYPE;
  }
}
```

#### 3. 注册到 MessageHandler

在 `MessageHandler` 中添加：

```java

@Autowired
private NewMessageProcess newMessageProcess;

// 在 switch 中添加 case
case NEW_MESSAGE_TYPE ->newMessageProcess.

dispose(messageWrap);
```

### 自定义编解码器

#### Protobuf 编解码器

```java

@Component
@ChannelHandler.Sharable
public class CustomProtobufHandler extends MessageToMessageCodec<ByteBuf, IMessageWrap> {

  @Override
  protected void encode(ChannelHandlerContext ctx, IMessageWrap msg, List<Object> out) {
    // 将 IMessageWrap 编码为 ByteBuf
    byte[] bytes = msg.toByteArray();
    out.add(Unpooled.wrappedBuffer(bytes));
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) {
    // 将 ByteBuf 解码为 IMessageWrap
    byte[] bytes = new byte[msg.readableBytes()];
    msg.readBytes(bytes);
    IMessageWrap message = IMessageWrap.parseFrom(bytes);
    out.add(message);
  }
}
```

### 自定义限流策略

使用 Sentinel 实现限流：

```java

@Component
public class CustomRateLimiter {

  public boolean checkLimit(String userId) {
    String resourceName = "connect:" + userId;
    Entry entry = null;
    try {
      entry = SphU.entry(resourceName);
      return true;  // 允许
    } catch (BlockException e) {
      return false;  // 被限流
    } finally {
      if (entry != null) {
        entry.exit();
      }
    }
  }
}
```

---

## 部署运维

### 系统要求

**最低配置**：

- CPU: 2 核
- 内存: 4 GB
- 网络: 100 Mbps

**推荐配置**：

- CPU: 4 核+
- 内存: 8 GB+
- 网络: 1 Gbps

### 容量规划

**单实例连接数**：建议 5000-10000

**多实例部署**：根据预估在线用户数计算

```
实例数 = ceil(预估在线用户数 / 单实例连接数)

例如：100 万在线用户，单实例 1 万连接
实例数 = ceil(1000000 / 10000) = 100 实例
```

### 监控指标

| 指标    | 说明                | 告警阈值       |
|-------|-------------------|------------|
| 在线用户数 | 当前在线用户数           | > 配置容量 80% |
| 总连接数  | TCP/WebSocket 总连接 | > 配置容量 80% |
| 消息吞吐量 | 每秒处理消息数           | > 10000/s  |
| 消息延迟  | 消息处理耗时            | > 100ms    |
| 错误率   | 消息处理失败率           | > 1%       |
| 心跳超时  | 心跳超时次数            | > 100/min  |

### 日志配置

**日志位置**：`src/main/resources/logback.xml`

**日志级别**：

- `TRACE`：详细调试信息
- `DEBUG`：调试信息
- `INFO`：关键业务流程
- `WARN`：警告信息
- `ERROR`：错误信息

**日志分类**：

| Topic                 | 说明          |
|-----------------------|-------------|
| LogConstant.Main      | 主程序日志       |
| LogConstant.Netty     | Netty 网络日志  |
| LogConstant.Login     | 登录处理日志      |
| LogConstant.HeartBeat | 心跳处理日志      |
| LogConstant.Message   | 消息处理日志      |
| LogConstant.Channel   | 通道管理日志      |
| LogConstant.Rabbit    | RabbitMQ 日志 |
| LogConstant.Redis     | Redis 操作日志  |
| LogConstant.Nacos     | Nacos 注册日志  |

### 性能优化

#### 1. EPOLL 开启（Linux）

```bash
java -Dnetty.epoll.enable=true -jar im-connect.jar
```

#### 2. JVM 参数优化

```bash
java -Xms4g -Xmx4g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/logs/heapdump.hprof \
     -jar im-connect.jar
```

#### 3. 线程池调优

```yaml
netty:
  config:
    bossThreadSize: 8   # 根据 CPU 核心数调整
    workThreadSize: 32  # 一般为 bossThreadSize * 4
```

---

## 常见问题

### 1. 连接失败

**问题**：客户端无法连接

**排查**：

1. 检查端口是否正确
2. 检查防火墙是否开放
3. 查看服务日志是否有异常
4. 使用 `telnet ip port` 测试端口连通性

### 2. 消息丢失

**问题**：消息发送后接收方未收到

**排查**：

1. 检查 RabbitMQ 队列是否正常
2. 查看错误队列是否有堆积
3. 检查接收方是否在线
4. 查看 Redis 路由信息是否正确

### 3. 内存泄漏

**问题**：服务运行一段时间后内存持续增长

**排查**：

1. 导出堆转储文件分析
2. 检查 Channel 是否正确关闭
3. 检查监听器是否正确移除
4. 使用 jmap/jstat 监控 JVM

### 4. CPU 飙高

**问题**：CPU 使用率过高

**排查**：

1. 使用 top 命令查看进程 CPU
2. 使用 jstack 导出线程栈
3. 分析是否有死循环
4. 检查 GC 是否频繁

### 5. 频繁断线重连

**问题**：客户端频繁掉线

**排查**：

1. 检查心跳超时配置是否过短
2. 检查网络质量
3. 检查服务端负载是否过高
4. 查看是否有异常日志

### 6. Nacos 注册失败

**问题**：服务启动后未在 Nacos 中注册

**排查**：

1. 检查 Nacos 地址配置是否正确
2. 检查网络是否可达
3. 查看 Nacos 服务端日志
4. 检查服务名称和分组配置

---

## 附录

### 相关文档

- [im-core 文档](../im-core/README.md)
- [im-gateway 文档](../im-gateway/README.md)
- [im-ai 文档](../im-ai/README.md)

### 依赖模块

- `im-core`：核心模型和工具类
- `im-spring`：自定义 Spring 框架
- `im-starter-core`：核心 Starter

### 更新日志

| 版本    | 日期         | 说明   |
|-------|------------|------|
| 0.0.1 | 2025-01-XX | 初始版本 |

### 联系方式

- 项目地址：[GitHub](https://github.com/your-org/lucky-cloud)
- 问题反馈：[Issues](https://github.com/your-org/lucky-cloud/issues)
