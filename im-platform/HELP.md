# 平台基础服务（im-platform）

## 模块概述

`im-platform` 是 Lucky Cloud 平台基础服务模块，提供了一系列通用的 RPC 服务接口，包括短链服务、地址服务、表情服务和通知服务。该模块采用微服务架构，基于
Dubbo RPC 协议提供服务。

## 模块结构

```
im-platform/
├── im-platform-rpc-api/        # RPC API 接口定义模块
│   └── src/main/java/com/xy/lucky/platform/rpc/api/
│       ├── address/            # 地址服务接口
│       ├── emoji/              # 表情服务接口
│       ├── notify/             # 通知服务接口
│       ├── shortlink/          # 短链服务接口
│       ├── dto/                # 数据传输对象
│       ├── vo/                 # 视图对象
│       └── enums/              # 枚举类型
└── im-platform-service/        # 服务实现模块
```

## 核心服务

### 1. 短链服务（ShortLinkDubboService）

提供短链创建、重定向、禁用和查询功能。

**应用场景**：

- 社交媒体分享链接缩短
- 营销活动链接生成
- 链接访问统计
- 二维码跳转链接

**核心功能**：

- 创建短链（支持过期时间设置）
- 短链重定向（自动统计访问次数）
- 禁用短链（防止恶意访问）
- 查询短链详细信息

### 2. 地址服务（AddressDubboService）

提供 IP 地址查询、地区信息解析等功能。

**应用场景**：

- 用户位置识别
- 就近节点选择
- 地区数据统计
- 内容区域分发

**核心功能**：

- 根据 IP 查询地区信息（支持完整的地区路径）
- 根据地区编号查询地区节点
- 解析地区路径字符串
- 格式化地区编号为完整路径

### 3. 表情服务（EmojiDubboService）

提供表情包管理、表情图片上传等功能。

**应用场景**：

- 即时通讯表情功能
- 表情包管理和分发
- 表情图片存储和访问
- 自定义表情包上传

**核心功能**：

- 创建或更新表情包
- 列出所有表情包
- 上传表情图片（支持对象存储）
- 上传表情包封面图
- 启用/禁用表情包
- 查询表情包详情（包含所有表情）
- 删除表情（可选删除对象存储文件）
- 生成唯一的表情包编码

### 4. 通知服务（NotifyDubboService）

提供邮件发送和短信发送功能。

**应用场景**：

- 用户注册验证
- 系统通知推送
- 营销消息发送
- 安全提醒
- 密码重置

**核心功能**：

- 发送邮件（支持 HTML 和纯文本）
- 发送短信（支持模板短信）

## 快速开始

### 1. 添加 Maven 依赖

在需要使用平台服务的微服务 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>com.xy.lucky</groupId>
    <artifactId>im-platform-rpc-api</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 2. 注入 Dubbo 服务

```java
@Service
public class YourService {

    @DubboReference
    private ShortLinkDubboService shortLinkService;

    @DubboReference
    private AddressDubboService addressService;

    @DubboReference
    private EmojiDubboService emojiService;

    @DubboReference
    private NotifyDubboService notifyService;
}
```

### 3. 调用服务接口

```java
// 创建短链
ShortLinkDto request = ShortLinkDto.builder()
    .originalUrl("https://example.com/very-long-url")
    .expireSeconds(86400L)
    .build();
ShortLinkVo shortLink = shortLinkService.createShortLink(request);

// 查询地区信息
AreaVo area = addressService.getAreaByIp("8.8.8.8");

// 发送邮件
EmailDto email = EmailDto.builder()
    .to("user@example.com")
    .subject("欢迎使用")
    .content("<h1>欢迎</h1><p>感谢注册</p>")
    .html(true)
    .build();
Boolean success = notifyService.sendEmail(email);
```

## 详细文档

### 模块文档

- [im-platform-rpc-api/README.md](./im-platform-rpc-api/README.md)：RPC API 模块详细说明
- [im-platform-rpc-api/API.md](./im-platform-rpc-api/API.md)：完整的 API 接口文档

### OpenAPI 规范

所有服务的 OpenAPI 3.0 规范文件位于 `im-platform-rpc-api/src/main/resources/openapi/` 目录：

- `shortlink-service.yaml`：短链服务 API 规范
- `address-service.yaml`：地址服务 API 规范
- `emoji-service.yaml`：表情服务 API 规范
- `notify-service.yaml`：通知服务 API 规范

## 技术栈

### RPC 框架

- **Dubbo 3.x**：高性能 RPC 框架
- **Nacos**：服务注册与配置中心

### 核心依赖

- **Spring Boot 3.x**：应用框架
- **Jakarta Validation**：参数校验
- **Swagger Annotations**：API 文档注解
- **Lombok**：简化代码编写
- **Jackson**：JSON 序列化/反序列化

### 数据存储

- **PostgreSQL**：关系型数据库
- **MinIO**：对象存储（表情图片）
- **Redis**：缓存服务

## 配置说明

### Dubbo 消费者配置

在 `application.yml` 中配置 Dubbo 注册中心：

```yaml
dubbo:
  application:
    name: your-service
  registry:
    address: nacos://localhost:8848
  protocol:
    name: dubbo
    port: -1
  consumer:
    check: false          # 启动时不检查服务提供者
    timeout: 5000         # 超时时间（毫秒）
    retries: 2            # 重试次数
```

### 服务配置

各服务的详细配置请参考对应的服务实现模块。

## 开发指南

### 参数校验

所有 DTO 都使用 Jakarta Validation 进行参数校验：

- `@NotBlank`：字符串不能为空
- `@Email`：邮箱格式校验
- `@Pattern`：正则表达式校验
- `@Size`：字符串长度校验

### 异常处理

服务调用可能会抛出以下异常，建议在消费端进行适当处理：

- `IllegalArgumentException`：参数非法
- `ValidationException`：参数校验失败
- Dubbo 远程调用异常

### 代码规范

- 所有 DTO 和 VO 必须实现 `Serializable` 接口
- 使用 Lombok 注解简化代码
- 添加完整的 JavaDoc 注释
- 使用 `@Schema` 注解提供详细的字段说明

## 最佳实践

### 短链服务

- 对于相同 URL，优先使用已有的短码
- 合理设置过期时间，及时清理无效短链
- 禁用操作不可逆，谨慎使用

### 地址服务

- 缓存 IP 地址查询结果，减少重复查询
- 使用地区编号查询比路径解析更高效

### 表情服务

- 表情包编码建议使用有意义的前缀
- 删除表情时根据需求选择是否删除对象存储文件
- 定期清理不用的表情包

### 通知服务

- 邮件和短信发送可能失败，需要做好异常处理
- 短信使用模板可以节省成本
- 避免频繁发送通知，防止被标记为垃圾信息

## 版本要求

- **JDK**：17 或更高版本
- **Spring Boot**：3.x
- **Dubbo**：3.x
- **Nacos**：2.x 或更高版本

## 联系方式

- 项目维护：Lucky Platform Team
- 技术支持：support@lucky.com
- 问题反馈：[GitHub Issues](https://github.com/your-org/lucky-cloud/issues)

## 更新日志

### v1.0.0 (2025-02-06)

- 初始版本发布
- 提供短链、地址、表情、通知四个核心服务
- 完整的 OpenAPI 规范文档
- 支持参数校验和异常处理