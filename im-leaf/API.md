# im-leaf API 接口文档

## 目录

- [概述](#概述)
- [REST API](#rest-api)
    - [生成单个 ID](#生成单个-id)
    - [批量生成 ID](#批量生成-id)
- [Dubbo RPC API](#dubbo-rpc-api)
    - [服务接口定义](#服务接口定义)
    - [使用示例](#使用示例)
- [数据模型](#数据模型)
    - [IMetaId](#imetaid)
- [错误码说明](#错误码说明)

## 概述

im-leaf 提供两种 API 调用方式：

1. **REST API**：基于 HTTP/HTTPS 的 RESTful 接口，使用 JSON 格式交换数据
2. **Dubbo RPC API**：基于 Dubbo 协议的 RPC 接口，适用于微服务内部调用

两种方式提供相同的功能，支持以下 ID 生成策略：

- `snowflake`：雪花算法，高性能分布式 ID
- `redis`：Redis 号段模式，连续 ID
- `uid`：自定义 UID 生成
- `uuid`：标准 UUID 生成

---

## REST API

### 基础信息

- **Base URL**：`http://localhost:8080`
- **Content-Type**：`application/json`
- **响应格式**：JSON

### 生成单个 ID

根据指定的策略和业务标识生成单个 ID。

#### 请求

```http
GET /api/generator/id?type={type}&key={key}
```

或支持版本号：

```http
GET /api/v1/generator/id?type={type}&key={key}
```

#### 请求参数

| 参数名  | 类型     | 必填 | 说明               | 示例值                              |
|------|--------|----|------------------|----------------------------------|
| type | String | 是  | ID 生成策略          | `snowflake`、`redis`、`uid`、`uuid` |
| key  | String | 是  | 业务标识，用于区分不同的业务场景 | `order`、`user`、`message`         |

#### 响应

**成功响应（200 OK）**

```json
{
  "metaId": 1234567890123456789,
  "stringId": "1234567890123456789",
  "longId": 1234567890123456789
}
```

**响应字段说明**

| 字段名      | 类型     | 说明        |
|----------|--------|-----------|
| metaId   | Object | 原始 ID 对象  |
| stringId | String | 字符串格式的 ID |
| longId   | Long   | 长整型格式的 ID |

**错误响应（4xx/5xx）**

```json
{
  "timestamp": "2026-02-06T10:30:00.123+08:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid type parameter",
  "path": "/api/generator/id"
}
```

#### 示例

**请求示例**

```bash
# 使用 Snowflake 策略生成订单 ID
curl -X GET "http://localhost:8080/api/generator/id?type=snowflake&key=order"

# 使用 Redis 号段模式生成用户 ID
curl -X GET "http://localhost:8080/api/generator/id?type=redis&key=user"

# 生成 UUID
curl -X GET "http://localhost:8080/api/generator/id?type=uuid&key=session"
```

**响应示例**

```json
{
  "metaId": 1234567890123456789,
  "stringId": "1234567890123456789",
  "longId": 1234567890123456789
}
```

---

### 批量生成 ID

根据指定的策略和业务标识批量生成多个 ID。

#### 请求

```http
GET /api/generator/ids?type={type}&key={key}&count={count}
```

或支持版本号：

```http
GET /api/v1/generator/ids?type={type}&key={key}&count={count}
```

#### 请求参数

| 参数名   | 类型      | 必填 | 说明      | 示例值                              | 约束               |
|-------|---------|----|---------|----------------------------------|------------------|
| type  | String  | 是  | ID 生成策略 | `snowflake`、`redis`、`uid`、`uuid` | -                |
| key   | String  | 是  | 业务标识    | `order`、`user`、`message`         | -                |
| count | Integer | 是  | 生成数量    | `10`、`100`                       | 1 ≤ count ≤ 1000 |

#### 响应

**成功响应（200 OK）**

```json
[
  {
    "metaId": 1234567890123456789,
    "stringId": "1234567890123456789",
    "longId": 1234567890123456789
  },
  {
    "metaId": 1234567890123456790,
    "stringId": "1234567890123456790",
    "longId": 1234567890123456790
  },
  {
    "metaId": 1234567890123456791,
    "stringId": "1234567890123456791",
    "longId": 1234567890123456791
  }
]
```

**错误响应（4xx/5xx）**

```json
{
  "timestamp": "2026-02-06T10:30:00.123+08:00",
  "status": 400,
  "error": "Bad Request",
  "message": "count must be between 1 and 1000",
  "path": "/api/generator/ids"
}
```

#### 示例

**请求示例**

```bash
# 批量生成 10 个订单 ID（Snowflake 策略）
curl -X GET "http://localhost:8080/api/generator/ids?type=snowflake&key=order&count=10"

# 批量生成 100 个用户 ID（Redis 号段模式）
curl -X GET "http://localhost:8080/api/generator/ids?type=redis&key=user&count=100"

# 批量生成 5 个 UUID
curl -X GET "http://localhost:8080/api/generator/ids?type=uuid&key=session&count=5"
```

**响应示例**

```json
[
  {
    "metaId": 1234567890123456789,
    "stringId": "1234567890123456789",
    "longId": 1234567890123456789
  },
  {
    "metaId": 1234567890123456790,
    "stringId": "1234567890123456790",
    "longId": 1234567890123456790
  },
  {
    "metaId": 1234567890123456791,
    "stringId": "1234567890123456791",
    "longId": 1234567890123456791
  }
]
```

---

## Dubbo RPC API

### 服务接口定义

#### 接口信息

- **接口全限定名**：`com.xy.lucky.rpc.api.leaf.ImIdDubboService`
- **服务名**：`im-leaf`
- **协议**：`dubbo`
- **版本**：`${revision}`（项目版本）

#### 方法列表

##### 1. generateId

根据 type 和 key 生成单个 ID。

**方法签名**

```java
IMetaId generateId(String type, String key);
```

**参数说明**

| 参数名  | 类型     | 必填 | 说明      | 示例值                              |
|------|--------|----|---------|----------------------------------|
| type | String | 是  | ID 生成策略 | `snowflake`、`redis`、`uid`、`uuid` |
| key  | String | 是  | 业务标识    | `order`、`user`、`message`         |

**返回值**

`IMetaId` 对象，包含生成的 ID 信息。

**异常**

- `IllegalArgumentException`：参数不合法
- `IllegalStateException`：服务内部错误（如时钟回拨）

---

##### 2. generateIds

批量获取 ID。

**方法签名**

```java
List<IMetaId> generateIds(String type, String key, Integer count);
```

**参数说明**

| 参数名   | 类型      | 必填 | 说明      | 示例值                              | 约束               |
|-------|---------|----|---------|----------------------------------|------------------|
| type  | String  | 是  | ID 生成策略 | `snowflake`、`redis`、`uid`、`uuid` | -                |
| key   | String  | 是  | 业务标识    | `order`、`user`、`message`         | -                |
| count | Integer | 是  | 生成数量    | `10`、`100`                       | 1 ≤ count ≤ 1000 |

**返回值**

`List<IMetaId>`，包含生成的 ID 列表。

**异常**

- `IllegalArgumentException`：参数不合法
- `IllegalStateException`：服务内部错误

---

##### 3. getId（泛型方法）

通用类型安全的获取 ID 方法，带缓存机制。

**方法签名**

```java
<T> T getId(String type, String key, Class<T> targetType);
```

**参数说明**

| 参数名        | 类型       | 必填 | 说明      | 示例值                              |
|------------|----------|----|---------|----------------------------------|
| type       | String   | 是  | ID 生成策略 | `snowflake`、`redis`、`uid`、`uuid` |
| key        | String   | 是  | 业务标识    | `order`、`user`、`message`         |
| targetType | Class<T> | 是  | 目标类型    | `Long.class`、`String.class`      |

**返回值**

泛型 `T`，返回指定类型的 ID。

**异常**

- `IllegalArgumentException`：参数不合法
- `ClassCastException`：类型转换失败
- `IllegalStateException`：服务内部错误

---

### 使用示例

#### 服务引用配置

**Spring Boot YAML 配置**

```yaml
dubbo:
  application:
    name: consumer-service
  registry:
    address: nacos://localhost:8848
    group: DEFAULT_GROUP
    username: nacos
    password: nacos
  consumer:
    timeout: 5000
    retries: 3
    check: true
  reference:
    com.xy.lucky.rpc.api.leaf.ImIdDubboService:
      version: ${revision}
      timeout: 5000
      retries: 3
```

#### Java 客户端调用示例

**基础用法**

```java
import com.xy.lucky.rpc.api.leaf.ImIdDubboService;
import com.xy.lucky.core.model.IMetaId;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class IdConsumerService {

    @DubboReference(version = "${revision}")
    private ImIdDubboService idDubboService;

    /**
     * 生成单个 ID
     */
    public void generateSingleId() {
        // 生成 Snowflake ID
        IMetaId snowflakeId = idDubboService.generateId("snowflake", "order");
        System.out.println("Snowflake ID: " + snowflakeId.getLongId());

        // 生成 Redis Segment ID
        IMetaId redisId = idDubboService.generateId("redis", "user");
        System.out.println("Redis ID: " + redisId.getLongId());

        // 生成 UUID
        IMetaId uuid = idDubboService.generateId("uuid", "session");
        System.out.println("UUID: " + uuid.getStringId());
    }

    /**
     * 批量生成 ID
     */
    public void generateBatchIds() {
        // 批量生成 10 个 Snowflake ID
        List<IMetaId> snowflakeIds = idDubboService.generateIds("snowflake", "order", 10);
        snowflakeIds.forEach(id ->
            System.out.println("Snowflake ID: " + id.getLongId())
        );

        // 批量生成 100 个 Redis Segment ID
        List<IMetaId> redisIds = idDubboService.generateIds("redis", "user", 100);
        redisIds.forEach(id ->
            System.out.println("Redis ID: " + id.getLongId())
        );
    }

    /**
     * 使用泛型方法获取类型安全的 ID
     */
    public void generateTypedId() {
        // 获取 Long 类型 ID
        Long userId = idDubboService.getId("redis", "user", Long.class);
        System.out.println("User ID (Long): " + userId);

        // 获取 String 类型 ID
        String sessionId = idDubboService.getId("uuid", "session", String.class);
        System.out.println("Session ID (String): " + sessionId);
    }
}
```

**完整业务场景示例**

```java
@Service
public class OrderService {

    @DubboReference(version = "${revision}")
    private ImIdDubboService idDubboService;

    /**
     * 创建订单
     */
    public Order createOrder(OrderRequest request) {
        // 生成订单 ID
        Long orderId = idDubboService.getId("snowflake", "order", Long.class);

        // 创建订单对象
        Order order = new Order();
        order.setOrderId(orderId);
        order.setUserId(request.getUserId());
        order.setAmount(request.getAmount());
        order.setStatus(OrderStatus.CREATED);

        // 保存订单...
        return order;
    }

    /**
     * 批量创建订单
     */
    public List<Order> batchCreateOrders(List<OrderRequest> requests) {
        // 批量生成订单 ID
        List<IMetaId> idMetas = idDubboService.generateIds("snowflake", "order", requests.size());

        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            OrderRequest request = requests.get(i);
            IMetaId idMeta = idMetas.get(i);

            Order order = new Order();
            order.setOrderId(idMeta.getLongId());
            order.setUserId(request.getUserId());
            order.setAmount(request.getAmount());
            order.setStatus(OrderStatus.CREATED);

            orders.add(order);
        }

        return orders;
    }
}
```

---

## 数据模型

### IMetaId

ID 信息的通用封装对象。

**类全限定名**：`com.xy.lucky.core.model.IMetaId`

**字段说明**

| 字段名      | 类型     | 必填 | 说明        | 示例值                                                            |
|----------|--------|----|-----------|----------------------------------------------------------------|
| metaId   | Object | 是  | 原始 ID 对象  | `1234567890123456789`、`"550e8400-e29b-41d4-a716-446655440000"` |
| stringId | String | 否  | 字符串格式的 ID | `"1234567890123456789"`                                        |
| longId   | Long   | 否  | 长整型格式的 ID | `1234567890123456789`                                          |

**使用建议**

- 对于需要长整型 ID 的场景（如数据库主键），使用 `longId` 字段
- 对于需要字符串 ID 的场景（如 API 响应），使用 `stringId` 字段
- 对于需要原始对象的场景，使用 `metaId` 字段

---

## 错误码说明

### HTTP 状态码

| 状态码 | 说明      | 示例场景                  |
|-----|---------|-----------------------|
| 200 | 请求成功    | ID 生成成功               |
| 400 | 请求参数错误  | type 参数不合法、count 超出范围 |
| 500 | 服务器内部错误 | 时钟回拨、Redis 连接失败       |

### 错误响应格式

```json
{
  "timestamp": "2026-02-06T10:30:00.123+08:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid type parameter: unknown",
  "path": "/api/generator/id"
}
```

### 常见错误信息

| 错误信息                                        | 原因               | 解决方案                                              |
|---------------------------------------------|------------------|---------------------------------------------------|
| Invalid type parameter                      | type 参数不是有效的策略类型 | 检查 type 参数是否为 `snowflake`、`redis`、`uid`、`uuid` 之一 |
| count must be between 1 and 1000            | count 参数超出范围     | 确保 count 参数在 1 到 1000 之间                          |
| Clock moved backwards                       | 系统时钟回拨           | 检查系统时间，确保时间同步服务正常运行                               |
| Failed to acquire distributed lock          | 无法获取分布式锁         | 检查 Redis 服务状态                                     |
| Redis connection failed                     | Redis 连接失败       | 检查 Redis 服务是否正常运行                                 |
| Segment exhausted and new segment not ready | 号段耗尽且新号段未就绪      | 增加号段步长或检查数据库连接                                    |

---

## 完整示例

### cURL 示例

```bash
#!/bin/bash

# 设置基础 URL
BASE_URL="http://localhost:8080"

# 1. 生成单个 Snowflake ID
echo "生成订单 ID（Snowflake）："
curl -X GET "${BASE_URL}/api/generator/id?type=snowflake&key=order"
echo -e "\n"

# 2. 生成单个 Redis Segment ID
echo "生成用户 ID（Redis Segment）："
curl -X GET "${BASE_URL}/api/generator/id?type=redis&key=user"
echo -e "\n"

# 3. 生成 UUID
echo "生成会话 ID（UUID）："
curl -X GET "${BASE_URL}/api/generator/id?type=uuid&key=session"
echo -e "\n"

# 4. 批量生成 10 个 Snowflake ID
echo "批量生成 10 个订单 ID（Snowflake）："
curl -X GET "${BASE_URL}/api/generator/ids?type=snowflake&key=order&count=10"
echo -e "\n"

# 5. 批量生成 50 个 Redis Segment ID
echo "批量生成 50 个用户 ID（Redis Segment）："
curl -X GET "${BASE_URL}/api/generator/ids?type=redis&key=user&count=50"
echo -e "\n"
```

### Java REST 客户端示例

```java
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.List;

public class IdApiClient {

    private final WebClient webClient;

    public IdApiClient(String baseUrl) {
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .build();
    }

    /**
     * 生成单个 ID
     */
    public Mono<IMetaId> generateId(String type, String key) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/generator/id")
                .queryParam("type", type)
                .queryParam("key", key)
                .build())
            .retrieve()
            .bodyToMono(IMetaId.class);
    }

    /**
     * 批量生成 ID
     */
    public Mono<List<IMetaId>> generateIds(String type, String key, int count) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/generator/ids")
                .queryParam("type", type)
                .queryParam("key", key)
                .queryParam("count", count)
                .build())
            .retrieve()
            .bodyToFlux(IMetaId.class)
            .collectList();
    }

    /**
     * 使用示例
     */
    public static void main(String[] args) {
        IdApiClient client = new IdApiClient("http://localhost:8080");

        // 生成单个 ID
        IMetaId id = client.generateId("snowflake", "order").block();
        System.out.println("Generated ID: " + id.getLongId());

        // 批量生成 ID
        List<IMetaId> ids = client.generateIds("snowflake", "order", 10).block();
        ids.forEach(metaId ->
            System.out.println("ID: " + metaId.getLongId())
        );
    }
}
```

---

## 附录

### 性能建议

1. **批量调用优于单个调用**：如果需要多个 ID，建议使用批量接口
2. **合理选择策略**：
    - 高性能场景：使用 `snowflake`
    - 需要 ID 连续：使用 `redis`
    - 本地临时数据：使用 `uuid`
3. **注意限流**：批量生成时单次最多 1000 个

### 安全建议

1. **生产环境配置**：
    - 启用 HTTPS
    - 配置认证和授权
    - 设置合理的限流策略
2. **日志记录**：记录 ID 生成请求，便于问题排查
3. **监控告警**：监控 QPS、延迟、错误率等指标

---

## 更多信息

- 项目主页：[README.md](./README.md)
- 帮助文档：[HELP.md](./HELP.md)
- OpenAPI 规范：[leaf-id-service.yaml](./leaf-id-service.yaml)
