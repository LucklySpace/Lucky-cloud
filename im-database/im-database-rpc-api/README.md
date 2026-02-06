# IM Database RPC API

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-green.svg)](https://www.oracle.com/java/)
[![Dubbo](https://img.shields.io/badge/Dubbo-3.x-orange.svg)](https://dubbo.apache.org/)

## 项目简介

`im-database-rpc-api` 是即时通讯（IM）系统的数据库 RPC API 模块，提供了一套基于 Dubbo RPC 的高性能、可扩展的数据库访问接口。该模块定义了
IM 系统核心业务的数据访问契约，包括用户管理、好友关系、群组管理、消息存储、会话管理等功能。

## 核心特性

### 全面覆盖 IM 业务场景

- **用户服务**：用户基础信息管理、用户扩展信息管理
- **好友服务**：好友关系管理、好友请求管理、好友分组管理
- **群组服务**：群组管理、群成员管理、群邀请管理
- **消息服务**：单聊消息管理、群聊消息管理、消息状态跟踪
- **会话服务**：会话列表管理、会话信息更新
- **表情服务**：用户表情包管理
- **出站服务**：消息出站队列管理，保证消息可靠投递
- **认证服务**：令牌持久化管理

### 高性能设计

- **Dubbo RPC 框架**：基于 Dubbo 3.x，支持高性能的远程方法调用
- **序列化优化**：支持多种序列化协议（Hessian、Protobuf 等）
- **连接池管理**：自动管理连接池，减少连接建立开销
- **异步调用支持**：支持异步调用模式，提升系统吞吐量

### 可扩展架构

- **模块化设计**：各服务模块独立定义，职责清晰
- **版本兼容**：接口设计考虑向后兼容性
- **扩展字段支持**：所有实体均包含 extra 扩展字段，支持业务扩展

## 模块结构

```
im-database-rpc-api/
├── src/main/java/com/xy/lucky/api/
│   ├── auth/                      # 认证服务
│   │   └── ImAuthTokenDubboService.java
│   ├── chat/                      # 会话服务
│   │   └── ImChatDubboService.java
│   ├── emoji/                     # 表情服务
│   │   └── ImUserEmojiPackDubboService.java
│   ├── friend/                    # 好友服务
│   │   ├── ImFriendshipDubboService.java
│   │   └── ImFriendshipRequestDubboService.java
│   ├── group/                     # 群组服务
│   │   ├── ImGroupDubboService.java
│   │   ├── ImGroupInviteRequestDubboService.java
│   │   └── ImGroupMemberDubboService.java
│   ├── message/                   # 消息服务
│   │   ├── ImGroupMessageDubboService.java
│   │   └── ImSingleMessageDubboService.java
│   ├── outbox/                    # 出站服务
│   │   └── IMOutboxDubboService.java
│   └── user/                      # 用户服务
│       ├── ImUserDubboService.java
│       └── ImUserDataDubboService.java
└── src/main/resources/
    └── openapi/                   # OpenAPI 规范文件
        ├── auth-service.yaml
        ├── chat-service.yaml
        ├── emoji-service.yaml
        ├── friend-service.yaml
        ├── group-service.yaml
        ├── message-service.yaml
        ├── outbox-service.yaml
        └── user-service.yaml
```

## 服务列表

### 1. 认证服务 (ImAuthTokenDubboService)

提供令牌的持久化管理功能，用于 OAuth2 认证流程。

**主要功能**：

- 保存令牌元信息
- 标记刷新令牌已使用
- 撤销访问令牌
- 撤销刷新令牌

### 2. 会话服务 (ImChatDubboService)

管理用户会话信息，包括单聊会话和群聊会话。

**主要功能**：

- 查询用户会话列表
- 查询单个会话信息
- 创建或更新会话
- 删除会话

### 3. 表情服务 (ImUserEmojiPackDubboService)

管理用户表情包的绑定与解绑。

**主要功能**：

- 查询用户表情包列表
- 绑定表情包
- 批量绑定表情包
- 解绑表情包

### 4. 好友服务

#### 4.1 好友关系服务 (ImFriendshipDubboService)

管理用户之间的好友关系。

**主要功能**：

- 查询好友列表（支持增量同步）
- 查询单个好友关系
- 批量查询好友关系
- 创建/更新/删除好友关系

#### 4.2 好友请求服务 (ImFriendshipRequestDubboService)

管理好友添加请求流程。

**主要功能**：

- 查询好友请求列表
- 创建好友请求
- 处理好友请求
- 更新请求状态

### 5. 群组服务

#### 5.1 群组信息服务 (ImGroupDubboService)

管理群组基础信息。

**主要功能**：

- 查询用户群组列表
- 查询群组详情
- 创建/更新/删除群组
- 批量创建群组

#### 5.2 群成员服务 (ImGroupMemberDubboService)

管理群组成员信息。

**主要功能**：

- 查询群成员列表
- 按角色查询成员
- 添加/移除群成员
- 批量管理群成员
- 统计群成员数量
- 获取九宫格头像

#### 5.3 群邀请服务 (ImGroupInviteRequestDubboService)

管理群组邀请请求。

**主要功能**：

- 查询群邀请列表
- 创建群邀请请求
- 处理群邀请请求
- 批量创建邀请

### 6. 消息服务

#### 6.1 单聊消息服务 (ImSingleMessageDubboService)

管理单聊消息的存储与查询。

**主要功能**：

- 查询单聊消息列表（支持增量同步）
- 查询单条消息详情
- 发送单聊消息
- 批量发送消息
- 查询最后一条消息
- 查询消息已读状态

#### 6.2 群聊消息服务 (ImGroupMessageDubboService)

管理群聊消息的存储与查询。

**主要功能**：

- 查询群聊消息列表（支持增量同步）
- 查询单条群消息详情
- 发送群聊消息
- 批量发送群消息
- 查询最后一条群消息
- 查询群消息已读状态

### 7. 出站服务 (IMOutboxDubboService)

管理消息出站队列，实现消息的可靠投递（基于 Outbox 模式）。

**主要功能**：

- 查询出站消息列表
- 创建出站消息
- 批量创建消息
- 更新消息状态
- 查询待发送消息
- 标记消息发送失败

### 8. 用户服务

#### 8.1 用户基础信息服务 (ImUserDubboService)

管理用户基础账号信息。

**主要功能**：

- 查询用户列表
- 查询用户详情
- 创建/更新/删除用户
- 批量创建用户
- 根据手机号查询用户

#### 8.2 用户扩展信息服务 (ImUserDataDubboService)

管理用户扩展信息（昵称、头像、签名等）。

**主要功能**：

- 查询用户扩展信息
- 创建/更新用户扩展信息
- 批量创建用户信息
- 关键词搜索用户
- 批量查询用户信息

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- Dubbo 3.x
- Zookeeper / Nacos（注册中心）

### Maven 依赖

```xml
<dependency>
    <groupId>com.xy.lucky</groupId>
    <artifactId>im-database-rpc-api</artifactId>
    <version>${latest.version}</version>
</dependency>
```

### 服务引用示例

```java
import org.apache.dubbo.config.annotation.DubboReference;

public class ChatService {

    @DubboReference
    private ImUserDubboService imUserDubboService;

    @DubboReference
    private ImSingleMessageDubboService imSingleMessageDubboService;

    public void sendMessage(String fromId, String toId, Object messageBody) {
        // 查询用户信息
        ImUserPo fromUser = imUserDubboService.queryOne(fromId);
        ImUserPo toUser = imUserDubboService.queryOne(toId);

        // 构建消息
        ImSingleMessagePo message = new ImSingleMessagePo();
        message.setFromId(fromId);
        message.setToId(toId);
        message.setMessageBody(messageBody);
        message.setMessageTime(System.currentTimeMillis());
        message.setMessageContentType(1);

        // 发送消息
        imSingleMessageDubboService.creat(message);
    }
}
```

### Dubbo 配置示例

```yaml
dubbo:
  application:
    name: im-chat-service
  protocol:
    name: dubbo
    port: -1
  registry:
    address: nacos://localhost:8848
  consumer:
    timeout: 5000
    retries: 2
    check: false
```

## 核心概念

### 增量同步

消息和好友列表服务支持增量同步机制，通过 `sequence` 字段实现：

- 客户端记录最后一次同步的 sequence 值
- 下次同步时传递该 sequence 值
- 服务端返回 sequence 之后的所有变更数据
- 实现高效的数据增量拉取

```java
// 示例：增量同步单聊消息
Long lastSequence = getLastSyncSequence();
List<ImSingleMessagePo> messages = imSingleMessageDubboService.queryList(userId, lastSequence);
```

### 消息序列号

所有消息都包含 `sequence` 字段，用于：

- 消息排序：确保消息按时间顺序展示
- 增量同步：实现增量拉取机制
- 去重判断：客户端可基于 sequence 判断是否重复消息
- 消息可靠性：sequence 连续性可检测消息丢失

### 消息已读状态

单聊和群聊消息都支持已读状态管理：

- 单聊：发送方查询与某好友的已读状态
- 群聊：成员查询自己在群组的已读位置
- 已读状态通过独立的状态码标识

### 出站模式 (Outbox Pattern)

出站服务实现了 Outbox 模式，保证消息可靠投递：

1. 业务操作和保存出站消息在同一本地事务
2. 后台任务定期扫描待发送的出站消息
3. 发送成功后更新消息状态
4. 失败消息按重试策略重试
5. 超过重试次数标记为失败状态

## 数据模型

### 实体关系

```
ImUserPo (用户基础信息)
    ↓
ImUserDataPo (用户扩展信息)

ImFriendshipPo (好友关系)
    ↑
ImFriendshipRequestPo (好友请求)

ImFriendshipGroupPo (好友分组)
    ↑
ImFriendshipGroupMemberPo (好友分组成员)

ImGroupPo (群组信息)
    ↑
ImGroupMemberPo (群成员)
    ↑
ImGroupInviteRequestPo (群邀请请求)

ImSingleMessagePo (单聊消息)

ImGroupMessagePo (群聊消息)
    ↓
ImGroupMessageStatusPo (群消息已读状态)

ImChatPo (会话信息)

ImUserEmojiPackPo (用户表情包)

IMOutboxPo (出站消息)

ImAuthTokenPo (认证令牌)
```

## 性能优化建议

### 1. 批量操作

优先使用批量接口，减少网络调用次数：

```java
// 推荐：批量创建用户
List<ImUserPo> users = Arrays.asList(user1, user2, user3);
imUserDubboService.creatBatch(users);

// 不推荐：逐个创建
imUserDubboService.creat(user1);
imUserDubboService.creat(user2);
imUserDubboService.creat(user3);
```

### 2. 增量同步

使用增量同步机制，减少数据传输量：

```java
// 记录本地 sequence
Long lastSequence = localStorage.getLastSequence();

// 增量拉取
List<ImSingleMessagePo> messages = messageService.queryList(userId, lastSequence);

// 更新本地 sequence
if (!messages.isEmpty()) {
    localStorage.updateSequence(messages.get(messages.size() - 1).getSequence());
}
```

### 3. 异步调用

对于非关键路径，使用异步调用：

```java
@DubboReference(async = true)
private ImSingleMessageDubboService messageService;

public void sendNotification(String userId, String content) {
    ImSingleMessagePo message = buildMessage(userId, content);
    CompletableFuture<Boolean> future = messageService.creat(message);
    future.thenAccept(success -> {
        if (!success) {
            log.error("消息发送失败");
        }
    });
}
```

### 4. 缓存策略

对频繁查询的数据进行缓存：

```java
@Cacheable(value = "user", key = "#userId")
public ImUserPo getUser(String userId) {
    return imUserDubboService.queryOne(userId);
}
```

## 错误处理

### 异常类型

```java
// 业务异常
throw new BusinessException("用户不存在");

// 权限异常
throw new ForbiddenException("无权访问该群组");

// 参数异常
throw new IllegalArgumentException("用户ID不能为空");
```

### 重试机制

```java
@DubboReference(retries = 3, timeout = 3000)
private ImUserDubboService userService;
```

## 文档资源

- [API 详细文档](API.md) - 所有服务的接口详细说明
- [OpenAPI 规范](src/main/resources/openapi/) - OpenAPI 3.0 规范文件
- [使用指南](HELP.md) - 常见问题和使用指南

## 版本历史

- **v2.0.0** (2024-01) - 重构为 Dubbo 3.x，优化接口设计
- **v1.5.0** (2023-06) - 新增响应式支持
- **v1.0.0** (2023-01) - 初始版本发布

## 技术支持

如有问题或建议，请通过以下方式联系：

- 提交 Issue
- 发送邮件至技术支持团队
- 查看项目 Wiki

## 许可证

本项目采用 MIT 许可证，详见 [LICENSE](LICENSE) 文件。
