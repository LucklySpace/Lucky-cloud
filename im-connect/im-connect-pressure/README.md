# im-connect-pressure

`im-connect-pressure` 是 `im-connect` 下的 WebSocket 长连接压测模块，支持 `protobuf` 与 `json` 两种序列化协议，支持 URL/Header/Cookie 三种鉴权传递方式，覆盖连接建立、注册、心跳保活、业务消息发送与核心指标统计。

## 能力

- 支持并发连接压测与连接速率控制
- 支持自动发送 REGISTER 与 HEART_BEAT_PING
- 支持可选业务消息周期发送
- 支持连接断开自动重连
- 支持周期性输出关键指标

## 构建

在仓库根目录执行：

```bash
mvn -pl im-connect/im-connect-pressure -am -DskipTests package
```

产物：

- `im-connect/im-connect-pressure/target/im-connect-pressure.jar`

## 启动

```bash
java -jar im-connect/im-connect-pressure/target/im-connect-pressure.jar \
  --url=ws://127.0.0.1:19000/im \
  --protocol=proto \
  --authMode=url \
  --token=你的JWT \
  --connections=200 \
  --connectRate=50 \
  --durationSeconds=180 \
  --heartbeatIntervalMs=25000 \
  --printIntervalSeconds=5
```

## 脚本启动

也可以使用封装脚本 `im-connect/scripts/ws_pressure_test.sh` 启动压测：

```bash
cd /Users/dennis/code/LucklySpace/lucky-cloud/im-connect
./scripts/ws_pressure_test.sh --build \
  --url=ws://127.0.0.1:19000/im \
  --protocol=proto \
  --authMode=url \
  --token=你的JWT \
  --connections=200 \
  --connectRate=50 \
  --durationSeconds=180
```

如果已经构建过 jar，可省略 `--build`：

```bash
cd /Users/dennis/code/LucklySpace/lucky-cloud/im-connect
./scripts/ws_pressure_test.sh \
  --protocol=json \
  --authMode=header \
  --tokenFile=/absolute/path/tokens.txt \
  --connections=500 \
  --connectRate=120 \
  --durationSeconds=600
```

## 参数

- `--url` WebSocket 地址，默认 `ws://127.0.0.1:19000/im`
- `--protocol` `proto` 或 `json`，默认 `proto`
- `--authMode` `url`、`header` 或 `cookie`，默认 `url`
- `--instanceTag` 实例标签，用于 requestId 区分，默认 `pressure`
- `--shardTotal` 分片总数（多实例压测时使用），默认 `1`
- `--shardIndex` 当前实例分片下标（从 `0` 开始），默认 `0`
- `--token` 单 token 压测
- `--tokenFile` token 文件路径（每行一个 token），多用户压测
- `--deviceType` 设备类型，默认 `WEB`
- `--connections` 目标连接数，默认 `100`
- `--connectRate` 每秒建连数，默认 `50`
- `--durationSeconds` 压测时长，默认 `120`
- `--heartbeatIntervalMs` 心跳间隔，默认 `25000`
- `--messageIntervalMs` 业务消息发送间隔，`0` 表示关闭，默认 `0`
- `--messageCode` 业务消息 code，默认 `1000`
- `--registerCode` 注册消息 code，默认 `200`
- `--registerOnConnect` 是否握手后发送注册消息，默认 `true`
- `--connectTimeoutMs` 建连超时，默认 `8000`
- `--maxFramePayloadLength` 最大 WebSocket 帧长度，默认 `655360`
- `--printIntervalSeconds` 指标打印周期，默认 `5`
- `--autoReconnect` 是否自动重连，默认 `true`
- `--reconnectDelayMs` 重连等待毫秒，默认 `3000`
- `--workerThreads` 客户端 EventLoop 线程数，默认 `4`

## 示例

JSON 协议 + Header 鉴权：

```bash
java -jar im-connect/im-connect-pressure/target/im-connect-pressure.jar \
  --url=ws://127.0.0.1:19000/im \
  --protocol=json \
  --authMode=header \
  --token=你的JWT \
  --connections=300 \
  --connectRate=100 \
  --durationSeconds=300 \
  --heartbeatIntervalMs=20000 \
  --messageIntervalMs=10000 \
  --messageCode=1000
```

Proto 协议 + URL 鉴权 + token 池：

```bash
java -jar im-connect/im-connect-pressure/target/im-connect-pressure.jar \
  --url=ws://127.0.0.1:19000/im \
  --protocol=proto \
  --authMode=url \
  --tokenFile=/absolute/path/tokens.txt \
  --connections=500 \
  --connectRate=120 \
  --durationSeconds=600 \
  --heartbeatIntervalMs=25000 \
  --autoReconnect=true
```

## 10w+ 压测建议

- 单实例建议控制在 `2w~5w` 连接，`10w+` 采用多实例分片并发压测
- 每个实例设置不同 `--shardIndex`，并保持相同 `--shardTotal`
- 优先使用 `--protocol=proto`，降低编码开销与网络负载
- `--connectRate` 建议按机器能力逐步爬升，避免瞬时建连洪峰
- 客户端机器建议提升 `ulimit -n` 与端口范围，避免句柄与端口耗尽

四实例打满 12 万连接示例（每实例 3 万）：

```bash
cd /Users/dennis/code/LucklySpace/lucky-cloud/im-connect

JAVA_OPTS="-Xms3g -Xmx3g -XX:+UseG1GC -XX:+AlwaysPreTouch" \
./scripts/ws_pressure_test.sh --build \
  --url=ws://127.0.0.1:19000/im --protocol=proto --authMode=url \
  --tokenFile=/absolute/path/tokens.txt \
  --connections=30000 --connectRate=3000 --durationSeconds=900 \
  --workerThreads=8 --shardTotal=4 --shardIndex=0 --instanceTag=ws-p0

JAVA_OPTS="-Xms3g -Xmx3g -XX:+UseG1GC -XX:+AlwaysPreTouch" \
./scripts/ws_pressure_test.sh \
  --url=ws://127.0.0.1:19000/im --protocol=proto --authMode=url \
  --tokenFile=/absolute/path/tokens.txt \
  --connections=30000 --connectRate=3000 --durationSeconds=900 \
  --workerThreads=8 --shardTotal=4 --shardIndex=1 --instanceTag=ws-p1
```
