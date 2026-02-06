# im-platform-rpc-api

## 项目简介

`im-platform-rpc-api` 是 Lucky Cloud 平台基础服务的 RPC API 模块，提供了一组基于 Dubbo
的分布式服务接口。该模块定义了平台核心功能的接口规范、数据传输对象（DTO）和视图对象（VO），供微服务架构中的各个服务消费。

## 模块特性

- **RPC 接口定义**：基于 Dubbo 的高性能 RPC 服务接口
- **完整的数据模型**：包含请求 DTO 和响应 VO 的完整定义
- **OpenAPI 规范**：提供标准的 OpenAPI 3.0 规范文档
- **类型安全**：使用 Java 强类型系统，支持参数校验
- **易于集成**：通过 Maven 依赖即可快速集成到各个微服务

## 技术栈

- **Java 17+**
- **Dubbo 3.x**：高性能 RPC 框架
- **Jakarta Validation**：参数校验
- **Swagger Annotations**：API 文档注解
- **Lombok**：简化代码编写
- **Jackson**：JSON 序列化/反序列化

## 核心服务

本模块提供了以下 4 个核心服务的 RPC 接口：

### 1. 短链服务（ShortLinkDubboService）

提供短链创建、重定向、禁用和查询功能。

**主要功能**：

- 创建短链（支持过期时间设置）
- 短链重定向（获取原始 URL）
- 禁用短链
- 查询短链详细信息

**典型场景**：

- 社交媒体分享链接缩短
- 营销活动链接生成
- 链接访问统计

### 2. 地址服务（AddressDubboService）

提供 IP 地址查询、地区信息解析等功能。

**主要功能**：

- 根据 IP 查询地区信息
- 根据地区编号查询地区节点
- 解析地区路径字符串
- 格式化地区编号为完整路径

**典型场景**：

- 用户位置识别
- 就近节点选择
- 地区数据统计

### 3. 表情服务（EmojiDubboService）

提供表情包管理、表情图片上传等功能。

**主要功能**：

- 创建或更新表情包
- 列出所有表情包
- 上传表情图片
- 上传表情包封面图
- 启用/禁用表情包
- 查询表情包详情
- 删除表情
- 生成表情包编码

**典型场景**：

- 即时通讯表情功能
- 表情包管理和分发
- 表情图片存储和访问

### 4. 通知服务（NotifyDubboService）

提供邮件发送和短信发送功能。

**主要功能**：

- 发送邮件（支持 HTML）
- 发送短信（支持模板）

**典型场景**：

- 用户注册验证
- 系统通知推送
- 营销消息发送
- 安全提醒

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

### 2. 配置 Dubbo 引用

在 Spring Boot 应用中通过 `@DubboReference` 注入服务：

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

    public void yourMethod() {
        // 创建短链
        ShortLinkDto request = ShortLinkDto.builder()
            .originalUrl("https://example.com/very-long-url")
            .expireSeconds(86400L)
            .build();

        ShortLinkVo shortLink = shortLinkService.createShortLink(request);
        System.out.println("短链创建成功：" + shortLink.getShortUrl());
    }
}
```

### 3. 配置 Dubbo 注册中心

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
    check: false
```

## 数据模型说明

### DTO（数据传输对象）

DTO 用于客户端向服务端发送请求，包含参数校验注解。

**示例**：

- `ShortLinkDto`：短链创建请求
- `ShortLinkRedirectDto`：短链重定向请求
- `EmailDto`：邮件发送请求
- `SmsDto`：短信发送请求
- `EmojiPackDto`：表情包创建请求

### VO（视图对象）

VO 用于服务端向客户端返回数据，不包含业务逻辑。

**示例**：

- `ShortLinkVo`：短链信息响应
- `AreaVo`：地区信息响应
- `EmojiPackVo`：表情包元信息响应
- `EmojiVo`：表情条目响应
- `EmojiRespVo`：表情包详情响应

### 枚举类型

- `NotifyType`：通知类型（EMAIL、SMS）
- `NotifyStatus`：通知状态（SUCCESS、FAILED）

## API 文档

详细的 API 文档请参考：

- [API.md](./API.md)：完整的 API 使用说明和示例
- OpenAPI 规范文件位于 `src/main/resources/openapi/` 目录

## 目录结构

```
im-platform-rpc-api/
├── src/main/java/com/xy/lucky/platform/rpc/api/
│   ├── address/           # 地址服务
│   │   └── AddressDubboService.java
│   ├── dto/               # 数据传输对象
│   │   ├── ShortLinkDto.java
│   │   ├── ShortLinkRedirectDto.java
│   │   ├── EmailDto.java
│   │   ├── SmsDto.java
│   │   └── EmojiPackDto.java
│   ├── emoji/             # 表情服务
│   │   └── EmojiDubboService.java
│   ├── enums/             # 枚举类型
│   │   ├── NotifyType.java
│   │   └── NotifyStatus.java
│   ├── notify/            # 通知服务
│   │   └── NotifyDubboService.java
│   ├── shortlink/         # 短链服务
│   │   └── ShortLinkDubboService.java
│   └── vo/                # 视图对象
│       ├── ShortLinkVo.java
│       ├── AreaVo.java
│       ├── EmojiPackVo.java
│       ├── EmojiVo.java
│       └── EmojiRespVo.java
├── src/main/resources/
│   └── openapi/           # OpenAPI 规范文件
│       ├── shortlink-service.yaml
│       ├── address-service.yaml
│       ├── emoji-service.yaml
│       └── notify-service.yaml
├── pom.xml
└── README.md
```

## 参数校验

本模块使用 Jakarta Validation 进行参数校验：

**常用校验注解**：

- `@NotBlank`：字符串不能为空
- `@Email`：邮箱格式校验
- `@Pattern`：正则表达式校验
- `@Size`：字符串长度校验
- `@Schema`：Swagger 文档注解

**示例**：

```java
@Schema(name = "EmailDto", description = "邮件发送请求")
public class EmailDto implements Serializable {

    @NotBlank(message = "to 不能为空")
    @Email(message = "to 格式错误")
    private String to;

    @NotBlank(message = "subject 不能为空")
    @Size(max = 256, message = "subject 最长 256 字符")
    private String subject;
}
```

## 服务异常处理

服务调用可能会抛出以下异常：

- `IllegalArgumentException`：参数非法
- `NullPointerException`：空指针异常
- `ValidationException`：参数校验失败
- Dubbo 远程调用异常

建议在消费端进行适当的异常处理：

```java
try {
    ShortLinkVo result = shortLinkService.createShortLink(request);
} catch (ValidationException e) {
    // 处理参数校验异常
    log.error("参数校验失败：{}", e.getMessage());
} catch (Exception e) {
    // 处理其他异常
    log.error("短链创建失败", e);
}
```

## 版本要求

- **JDK**：17 或更高版本
- **Spring Boot**：3.x
- **Dubbo**：3.x
- **Nacos**：2.x 或更高版本（作为注册中心）

## 开发指南

### 添加新的 RPC 接口

1. 在对应的包中创建服务接口（如 `shortlink/`）
2. 使用 `@Schema` 注解添加 Swagger 文档
3. 定义所需的 DTO 和 VO
4. 在 `src/main/resources/openapi/` 中创建对应的 OpenAPI 规范文件

### 代码规范

- 所有 DTO 和 VO 必须实现 `Serializable` 接口
- 使用 Lombok 注解简化代码（`@Data`、`@Builder`、`@NoArgsConstructor`、`@AllArgsConstructor`）
- 添加完整的 JavaDoc 注释
- 使用 `@Schema` 注解提供详细的字段说明

## 常见问题

### 1. Dubbo 服务无法连接

检查注册中心配置是否正确，确保服务提供者已启动。

### 2. 参数校验失败

检查请求参数是否符合校验规则，查看错误信息定位问题。

### 3. 序列化异常

确保所有 DTO 和 VO 实现了 `Serializable` 接口，并且有无参构造函数。

## 许可证

Copyright © 2025 Lucky Platform. All rights reserved.

## 联系方式

- 项目维护：Lucky Platform Team
- 技术支持：support@lucky.com
- 问题反馈：[GitHub Issues](https://github.com/your-org/lucky-cloud/issues)
