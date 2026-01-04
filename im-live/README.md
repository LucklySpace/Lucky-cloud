# im-streaming 流媒体服务器

基于 Netty + 虚拟线程（JDK 21）+ 自研轻量容器 `im-spring` 的高并发流媒体服务器，支持：

- WebRTC 信令交换（多人房间、SDP/ICE透传）
- RTMP 基础推/拉（connect/createStream/publish/play 命令处理）

参考实现思路借鉴了 webrtc-netty 的信令结构与房间机制。

## 架构设计

- 核心模块
    - `webrtc`：`SignalingServer` + `RoomService`，负责房间与信令消息路由（WebSocket）
    - `rtmp`：`RtmpServer` + `RtmpHandshakeHandler` + `RtmpCommandHandler` + `StreamRegistry`
- 容器与生命周期
    - 使用 `im-spring` 的 `@SpringApplication` 扫描与 `ApplicationRunner` 启动
    - Bean 构造、注入、AOP、异步由容器处理，优雅关闭实现 `DisposableBean`
- 并发模型
    - Netty NIO 作为网络事件主循环
    - 计算/阻塞任务通过 `virtualThreadExecutor` 分发，降低 eventloop 压力

## 高并发与高可用

- 高并发
    - Netty 的零拷贝 `ByteBuf` 与无锁读写（信令）
    - 关键结构使用 `ConcurrentHashMap` 与 `ChannelGroup`
- 高可用（演进建议）
    - 将 `StreamRegistry` 替换为共享存储（如 Redis）实现跨节点流注册
    - 借助 Nacos/网关做实例发现与负载均衡
    - 使用 TURN 服务保障复杂 NAT 环境下的连接成功率

## 端口与配置

编辑 `src/main/resources/application.yml`：

```yaml
streaming:
  signaling:
    port: 8082
  rtmp:
    port: 1935
```

## 启动与验证

1. 编译：
   ```bash
   mvn -q -DskipTests package
   ```
2. 运行：
   ```bash
   java -jar im-streaming/target/im-streaming-0.0.1-SNAPSHOT.jar
   ```
3. WebRTC 信令：
    - 客户端通过 `ws://localhost:8082/ws` 发送 JSON 消息：
      ```json
      {"type":"join","roomId":"r1","userId":"u1"}
      {"type":"offer","roomId":"r1","userId":"u1","targetId":"u2","payload":{...}}
      ```
4. RTMP 推流/拉流（基础命令）：
    - 推流：`rtmp://localhost:1935/live/stream`
    - 拉流：`rtmp://localhost:1935/live/stream`
    - 当前示例实现仅处理握手与命令，数据通道留作后续完善

## 异常处理与安全

- 服务器启动失败会记录堆栈并停止绑定
- 连接断开自动清理房间/订阅索引，避免泄漏
- 生产环境建议使用 TLS（WSS/RTMPS）、配合鉴权与限流

## 后续扩展建议

- 实现 RTMP chunk 分片聚合与音视频包转发（或转封装为 FLV/TS）
- 引入 SFU（如 Janus/Kurento）实现多方 WebRTC 中继
- 录制与回放：媒体写盘、HLS/DASH 切片服务
- 链路观测：QoS 指标采集、丢包重传、ABR 自适应

