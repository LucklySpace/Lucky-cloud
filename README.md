# IM-Server - 高性能即时通讯服务端

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-green.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud Alibaba](https://img.shields.io/badge/Spring%20Cloud%20Alibaba-2023.0.0.0--RC1-blue.svg)](https://github.com/alibaba/spring-cloud-alibaba)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 📖 项目简介

IM-Server 是一个基于 **Spring Boot 3 + Spring Cloud Alibaba** 构建的高性能、高可用的即时通讯服务端系统。采用微服务架构设计，支持大规模用户并发连接，提供完整的即时通讯解决方案，包括消息推送、音视频通话、文件传输等核心功能。

## ✨ 核心特性

### 🚀 技术架构
- **Java 21**: 利用最新的Java特性，提供卓越的性能和安全性
- **Spring Boot 3.2.0**: 原生支持JDK 21，优化容器化部署
- **Spring Cloud Alibaba**: 完整的微服务治理解决方案
- **微服务架构**: 模块化设计，支持独立部署和扩展
- **高并发支持**: 基于WebSocket的长连接管理，支持万级并发

### 💬 通讯功能
- **即时消息**: 支持文本、图片、语音、视频、文件等多种消息类型
- **群组聊天**: 完整的群组管理，支持群聊头像自动生成
- **音视频通话**: WebRTC + SRS 技术，支持一对一和群组通话
- **消息推送**: 实时消息推送，支持离线消息存储
- **文件传输**: 基于MinIO的文件存储和管理

### 🔒 安全特性
- **身份认证**: JWT Token 认证机制
- **密码加密**: RSA 非对称加密，确保传输安全
- **权限控制**: 细粒度的功能权限管理
- **数据安全**: 敏感数据加密存储

### 🎯 系统特性
- **高可用性**: 服务注册发现、负载均衡、熔断降级
- **可扩展性**: 水平扩展，支持动态扩容
- **监控告警**: 集成Sentinel，实时监控系统状态
- **配置管理**: 动态配置，支持热更新

## 🏗️ 系统架构

### 1. im-gateway - 网关服务 (端口: 9191)
**功能**: 系统入口，负责请求路由、负载均衡、限流熔断
- 集成Sentinel实现网关限流和负载均衡
- 通过Nacos实现服务注册与发现
- 根据Redis中的用户信息，定向转发长连接请求

### 2. im-auth - 认证服务 (端口: 8084)
**功能**: 用户身份验证和权限管理
- 用户登录、注册、令牌生成和验证
- 提供RSA公钥，用于登录时密码加密
- 手机短信验证码服务

### 3. im-connect - 连接服务 (端口: 19000-19002)
**功能**: WebSocket长连接管理和消息推送
- 管理客户端长连接，支持心跳机制
- 通过RabbitMQ接收消息并推送给用户
- 支持多实例部署，自动负载均衡

### 4. im-server - 业务服务 (端口: 8085)
**功能**: 核心业务逻辑处理
- 消息发送、接收、存储和查询
- 群组管理、文件上传、群聊头像生成
- 消息分发到对应的im-connect服务

## 🔄 消息流转

```
用户发送消息流程:
1. 客户端 → im-gateway → im-server
2. im-server 处理业务逻辑
3. im-server → RabbitMQ → im-connect
4. im-connect → 用户长连接 → 客户端

长连接管理流程:
1. 客户端通过im-gateway建立WebSocket连接
2. im-connect 注册用户信息和机器码到Redis
3. 定期心跳维持连接状态
4. 连接断开时清理Redis中的相关信息
```

## 🛠️ 技术栈

### 后端技术
- **Java 21**: 运行时环境
- **Spring Boot 3.2.0**: 应用框架
- **Spring Cloud Alibaba 2023.0.0.0-RC1**: 微服务框架
- **Spring WebFlux**: 响应式Web框架
- **MyBatis Plus**: 数据持久层框架
- **Netty**: 服务端长连接网络框架

### 中间件服务
- **Nacos**: 服务注册发现、配置管理
- **Redis**: 缓存、会话管理
- **RabbitMQ**: 消息队列、异步通信
- **MinIO**: 对象存储、文件管理
- **SRS**: 流媒体服务、WebRTC支持

### 部署环境
- **Docker**: 容器化部署
- **Nginx**: 反向代理、负载均衡
- **JDK 21**: Java运行环境

## 🚀 快速开始

### 环境要求

- **JDK**: 21.0.0+
- **Docker**: 20.10.0+
- **Docker Compose**: 2.0.0+
- **操作系统**: Linux/Windows/macOS

### 1. 启动中间件服务

```bash
# 启动 Redis
docker run --name redis -p 6379:6379 -v /root/redis/conf/redis.conf:/usr/local/etc/redis/redis.conf -d redis --appendonly yes

# 启动 Nacos
docker run -itd --name nacos --env PREFER_HOST_MODE=hostname --env MODE=standalone --env NACOS_AUTH_IDENTITY_KEY=serverIdentity --env NACOS_AUTH_IDENTITY_VALUE=security --env NACOS_AUTH_TOKEN=SecretKey012345678901234567890123456789012345678901234567890123456789 -p 8848:8848 -p 9848:9848 -p 9849:9849 nacos/nacos-server:v2.2.1

# 启动 RabbitMQ
docker run -d --hostname my-rabbit --name rabbit -p 15672:15672 -p 5671-5672:5671-5672 rabbitmq
docker exec -it rabbit /bin/bash
rabbitmq-plugins enable rabbitmq_management

# 启动 MinIO
docker run -p 9000:9000 -p 9090:9090 --name minio -d --restart=always -e "MINIO_ACCESS_KEY=minioadmin" -e "MINIO_SECRET_KEY=minioadmin" -v /root/minio/data:/data -v /root/minio/config:/root/.minio minio/minio server /data --console-address ":9090" --address ":9000"

# 启动 SRS (注意: CANDIDATE必须设置为物理机IP)
docker run -it -p 1935:1935 -p 1985:1985 -p 8080:8080 -p 1990:1990 -p 8088:8088 --env CANDIDATE=192.168.1.9 -p 8000:8000/udp registry.cn-hangzhou.aliyuncs.com/ossrs/srs:6.0-d2
```

### 2. 配置应用

```yaml
# application.yml 示例配置
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
      config:
        server-addr: localhost:8848
        file-extension: yml
  
  redis:
    host: localhost
    port: 6379
    
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

# 文件存储配置
minio:
  endpoint: localhost:9000
  accessKey: minioadmin
  secretKey: minioadmin
  bucket: im-files

# SRS配置
srs:
  webrtc: http://localhost:8080
  rtmp: rtmp://localhost:1935
```

### 3. 启动应用服务

```bash
# 按顺序启动服务
1. im-auth (端口: 8084)
2. im-server (端口: 8085)
3. im-connect (端口: 19000-19002)
4. im-gateway (端口: 9191)
```

### 4. 访问服务

- **网关服务**: http://localhost:9191
- **认证服务**: http://localhost:8084
- **业务服务**: http://localhost:8085
- **Nacos控制台**: http://localhost:8848/nacos (账号: nacos/nacos)
- **RabbitMQ管理**: http://localhost:15672 (账号: guest/guest)
- **MinIO控制台**: http://localhost:9090 (账号: minioadmin/minioadmin)
- **SRS服务**: http://localhost:8080

## 🔧 开发指南

### 项目结构

```
im-server/
├── im-gateway/           # 网关服务
├── im-auth/             # 认证服务
├── im-connect/          # 连接服务
├── im-server/           # 业务服务
├── common/              # 公共模块
│   ├── common-core/     # 核心工具
│   ├── common-redis/    # Redis工具
│   ├── common-log/      # 日志工具
│   └── common-swagger/  # API文档
└── docs/                # 项目文档
```

### 代码规范

- 遵循阿里巴巴Java开发手册
- 使用统一的代码格式化工具
- 完善的注释和文档
- 单元测试覆盖率 > 80%

### API文档

启动服务后，访问以下地址查看API文档：
- **Swagger UI**: http://localhost:8085/doc.html
- **OpenAPI**: http://localhost:8085/v3/api-docs

## 🧪 测试

```bash
# 运行单元测试
mvn test

# 运行集成测试
mvn verify

# 生成测试报告
mvn jacoco:report
```

## 📦 部署

### Docker部署

```bash
# 构建镜像
mvn clean package -DskipTests
docker build -t im-server .

# 运行容器
docker run -d -p 9191:9191 --name im-gateway im-gateway
docker run -d -p 8084:8084 --name im-auth im-auth
docker run -d -p 8085:8085 --name im-server im-server
docker run -d -p 19000:19000 --name im-connect-1 im-connect
```

### 生产环境配置

- 使用Nginx进行反向代理和负载均衡
- 配置SSL证书，支持HTTPS
- 设置合适的JVM参数
- 配置日志收集和监控告警

## 📊 性能指标

- **并发连接**: 支持10,000+ WebSocket连接
- **消息延迟**: 平均延迟 < 100ms
- **系统吞吐**: 10,000+ TPS
- **响应时间**: 99%请求 < 200ms。

## 🤝 贡献指南

我们欢迎所有形式的贡献！请查看 [CONTRIBUTING.md](CONTRIBUTING.md) 了解详情。

### 贡献方式

1. **Fork** 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 **Pull Request**

## 📄 许可证

本项目采用 [MIT 许可证](LICENSE) - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🙏 致谢

- [Spring Boot](https://spring.io/projects/spring-boot) - Java应用框架
- [Spring Cloud Alibaba](https://github.com/alibaba/spring-cloud-alibaba) - 微服务解决方案
- [Nacos](https://nacos.io/) - 服务注册发现
- [Redis](https://redis.io/) - 内存数据库
- [RabbitMQ](https://www.rabbitmq.com/) - 消息队列
- [MinIO](https://min.io/) - 对象存储
- [SRS](https://github.com/ossrs/srs) - 流媒体服务器

## 📞 联系我们

- **项目主页**: [https://github.com/dennis9486/Lynk](https://github.com/dennis9486/Lynk)
- **问题反馈**: [Issues](https://github.com/dennis9486/Lynk/issues)
- **功能建议**: [Discussions](https://github.com/your-username/im-server/discussions)
- **邮箱**: 382192293@qq.com

---

⭐ 如果这个项目对您有帮助，请给我们一个星标！
