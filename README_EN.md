# 🍀 Lucky-Cloud (IM-Server) — High-Performance IM Backend

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-green.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud Alibaba](https://img.shields.io/badge/Spring%20Cloud%20Alibaba-2023.0.0.0-RC1-blue.svg)](https://github.com/alibaba/spring-cloud-alibaba)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[简体中文](README.md) | English (this page)

## 📖 Overview

Lucky-Cloud is a high-performance and highly available instant messaging (IM) backend built on **Spring Cloud Alibaba +
Spring Boot 3**.  
The system adopts a microservice architecture, supports massive concurrent connections, and provides a complete IM
solution including message delivery, audio/video calls, and file transfer.

## ✨ Key Features

### 🚀 Tech Architecture

- **Java 21**: Leverages the latest language features for performance and security
- **Spring Boot 3.2.4**: Native support for JDK 21, optimized for containerized deployment
- **Spring Cloud Alibaba**: Full microservice governance
- **MyBatis Plus**: ORM framework on top of MyBatis
- **Apache Dubbo**: RPC across microservice modules
- **Apache Seata**: Distributed transaction management
- **Google ProtoBuf**: Efficient serialization protocol
- **Netty**: Asynchronous networking for long connections
- **RabbitMQ**: High-availability, high-throughput message queue integration
- **MinIO**: Object storage for file upload/download/management
- **Microservice architecture**: Modular design with independent deployment and scaling
- **High concurrency**: WebSocket long connections for large-scale concurrency

### 💬 Communication Features

- **Instant messaging**: Text, images, audio, video, and files
- **Group chat**: Full group management; auto-generated group avatars
- **Audio/Video calls**: WebRTC + SRS for 1:1 and group calls
- **Message push**: Real-time push with offline message storage
- **File transfer**: Backed by MinIO

### 🔒 Security

- **Authentication**: JWT token mechanism
- **Password encryption**: RSA public-key cryptography
- **Access control**: Fine-grained permissions
- **Data security**: Sensitive data encrypted at rest

### 🎯 System Features

- **High availability**: Service discovery, load balancing, circuit breaking/fallback
- **Scalability**: Horizontal scaling, dynamic capacity expansion
- **Monitoring & alerts**: Sentinel integration for real-time monitoring
- **Configuration**: Dynamic config with hot updates
- **Dynamic data source**: Add/remove/switch data sources with health checks

## 🏗️ Architecture

### 1. im-gateway — API Gateway (Port: `9191`)

Entry point for routing, load balancing, rate limiting, and circuit breaking

- Integrates Sentinel for rate limiting and load balancing
- Service discovery/registry via Nacos; dynamic routing
- Routes long-connection requests based on user info in Redis
- Global signing verification, JWT authentication, blacklist interception, and dynamic fallback filters

### 2. im-auth — Auth Service (Port: `8084`)

User authentication and authorization

- Login/register/token generation and validation
- Provides RSA public key for password encryption during login
- SMS verification service
- Multi-strategy authentication: username/password, SMS login, QR-code scanning

### 3. im-connect — Connection Service (Ports: `19000–19002`)

WebSocket long-connection management and message delivery

- Manages client long connections with heartbeat
- Consumes messages from RabbitMQ and pushes to users
- Multi-instance deployment with automatic load balancing
- Connection limiter and message rate limiter, monitoring/logging, and virtual-thread optimizations

### 4. im-server — Business Service (Port: `8085`)

Core business logic

- Send/receive/store/query messages
- Group management, file upload, group avatar generation
- Dispatch messages to the corresponding `im-connect` instance
- Concurrency control using Redisson and Redis caching for hot data

### 5. Module Overview

- `im-ai` (port: `8088`): AI services (chat, embeddings, prompt management, tool calling)
- `im-file` (port: `8087`): MinIO-based file service supporting chunked upload, resume, image
  compression/thumbnail/watermark, NSFW image moderation
- `im-platform` (port: `8090`): Platform service (app updates and short links) with version check, presigned downloads,
  Bloom dedup + Caffeine LRU + Redis caching
- `im-leaf` (port: `8086`): Distributed ID generation (Segment, Snowflake, UID, UUID) with Nacos worker-id allocation
- `im-database` (port: `8100`): DB initializer and metadata service
- `im-meet` (WebSocket `19100`): Real-time meeting/chat service (Netty WS)
- `im-analysis` (port: `8089`): Text analysis (HanLP segmentation, keywords, dependency parsing)
- `im-proxy`: Nginx config templating to assist WS proxy and load balancing
- `im-framework`: Base aggregation (core/domain/common/crypto/spring/grpc/security/mq)

## 🔄 Message Flow

```
User sending flow:
1. Client → im-gateway → im-server
2. im-server handles business logic
3. im-server → RabbitMQ → im-connect
4. im-connect → user long connection → client

Long-connection management:
1. Client establishes WebSocket via im-gateway
2. im-connect registers user info and machine code in Redis
3. Heartbeats maintain connection state
4. On disconnect, related Redis records are cleaned up
```

## 🔄 End-to-End Message Flow (Detailed)

Client → im-gateway → im-server → RabbitMQ → im-connect → Client

### Sender (im-server)

1. **Build message DTO** (`messageId` globally unique; Snowflake or UUID recommended; `messageTempId` client-side only).
2. **Persist message + write outbox (same transaction)**:
    - Insert into message table (`im_private_message`/`im_group_message`) and an `outbox` record (store `messageId`
      /payload/targetBroker/status).
3. **Commit transaction** (DB commit means the message is reliably saved).
4. **Attempt to publish to RabbitMQ** (read from outbox or publish immediately):
    - Use Publisher Confirms (`channel.confirmSelect()`) and `mandatory` flag to ensure broker reception and
      persistence.
    - If confirm `ack`, mark outbox `SENT_TO_BROKER`.
    - If publish fails or no confirm, keep status `PENDING`; background job retries.
5. **Background compensation job**: Periodically scan outbox `PENDING/FAILED` and retry or escalate.

### Broker (RabbitMQ) Configuration Tips

- Set exchange/queue to **durable**
- Publish with `persistent=true`
- Use **publisher confirms** (lighter and recommended over transactions)
- Consider **quorum queues** or mirrored/clustered setups
- Configure **dead-letter-exchange** (DLX) for failed/unroutable messages

### Consumer (im-connect)

1. On consume, **ack appropriately** (process-first or persist-first as designed; guarantee no loss):
    - Persist then `ack`; or
    - Use manual ack: on success `ack`, on failure `nack/reject` (requeue or DLX)
2. **Idempotency**: Check whether delivered (by `messageId` in a delivery/status table); ignore duplicates
3. **Route to client**: Find the user’s WebSocket connection (from Redis registry) and push
4. **Client ACK (optional but recommended)**:
    - Client returns `DELIVERY_ACK(messageId)` to `im-connect`
    - `im-connect` updates DB (or callback to `im-server`) to mark as `DELIVERED`
    - If no ACK, retry several times then mark `UNDELIVERED` and trigger policy/alerts

### Client

- On receipt, send ACK, persist locally, update UI, and deduplicate by `messageId`

## 🛠️ Tech Stack

### Backend

- **Java 21**: Runtime
- **Spring Boot 3.2.4**: Application framework
- **Spring Cloud Alibaba 2023.0.0.0-RC1**: Microservice framework
- **Spring WebFlux**: Reactive web framework
- **MyBatis Plus**: Persistence layer
- **Netty**: Server-side long connections

### Middleware

- **Nacos**: Service discovery and configuration management
- **Redis**: Cache/session management
- **RabbitMQ**: Message queue, async communication
- **MinIO**: Object storage
- **SRS**: Streaming/WebRTC support

### Deployment

- **Docker**: Containerization
- **Nginx**: Reverse proxy/load balancer
- **JDK 21**: Java runtime

## 🚀 Quick Start

### Requirements

- **JDK**: 21.0.0+
- **Docker**: 20.10.0+
- **Docker Compose**: 2.0.0+
- **OS**: Linux/Windows/macOS

> Recommendation: Deploy on Linux for best performance.

### 1) Start Middleware Services

```bash
# Redis
docker run --name redis -p 6379:6379 -v /root/redis/conf/redis.conf:/usr/local/etc/redis/redis.conf -d redis --appendonly yes

# Nacos
docker run -itd --name nacos --env PREFER_HOST_MODE=hostname --env MODE=standalone --env NACOS_AUTH_IDENTITY_KEY=serverIdentity --env NACOS_AUTH_IDENTITY_VALUE=security --env NACOS_AUTH_TOKEN=SecretKey012345678901234567890123456789012345678901234567890123456789 -p 8848:8848 -p 9848:9848 -p 9849:9849 nacos/nacos-server:v2.2.1

# RabbitMQ
docker run -d --hostname my-rabbit --name rabbit -p 15672:15672 -p 5671-5672:5671-5672 rabbitmq
docker exec -it rabbit /bin/bash
rabbitmq-plugins enable rabbitmq_management

# MinIO
docker run -p 9000:9000 -p 9090:9090 --name minio -d --restart=always -e "MINIO_ACCESS_KEY=minioadmin" -e "MINIO_SECRET_KEY=minioadmin" -v /root/minio/data:/data -v /root/minio/config:/root/.minio minio/minio server /data --console-address ":9090" --address ":9000"

# SRS (Set CANDIDATE to your host IP)
docker run -it -p 1935:1935 -p 1985:1985 -p 8080:8080 -p 1990:1990 -p 8088:8088 --env CANDIDATE=192.168.1.9 -p 8000:8000/udp registry.cn-hangzhou.aliyuncs.com/ossrs/srs:6.0-d2

# PostgreSQL
docker run -d --name postgres -p 35432:5432 -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=postgres -v D:/Docker-vm/postgresql/vectordata:/var/lib/postgresql/data ankane/pgvector
```

### 2) Application Configuration

```yaml
# application.yml sample
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

# MinIO
minio:
  endpoint: localhost:9000
  accessKey: minioadmin
  secretKey: minioadmin
  bucket: im-files

# SRS
srs:
  webrtc: http://localhost:8080
  rtmp: rtmp://localhost:1935
```

### 3) Start Services

Two approaches:

**Manual order**

```bash
1. im-database (port: 8086)
2. im-auth (port: 8084)
3. im-server (port: 8085)
4. im-connect (ports: 19000–19002)
5. im-gateway (port: 9191)
6. im-leaf (port: 8086)
```

**Build scripts**

```bash
# Windows
deploy-all.bat

# Linux/macOS
./deploy-all.sh
```

### 4) Endpoints

- **Gateway**: http://localhost:9191
- **Auth**: http://localhost:8084
- **Business**: http://localhost:8085
- **File**: http://localhost:8087
- **ID**: http://localhost:8086
- **AI**: http://localhost:8088
- **Analysis**: http://localhost:8089
- **Platform**: http://localhost:8090
- **Nacos Console**: http://localhost:8848/nacos (nacos/nacos)
- **RabbitMQ Management**: http://localhost:15672 (guest/guest)
- **MinIO Console**: http://localhost:9090 (minioadmin/minioadmin)
- **SRS**: http://localhost:8080
- **PostgreSQL**: localhost:35432
- **Connect WebSocket**: ws://localhost:19000/im, ws://localhost:19001/im, ws://localhost:19002/im
- **Meet WebSocket**: ws://localhost:19100

## 🔧 Development Guide

### Project Structure

```
├── im-server/           # business service
├── im-gateway/          # API gateway
├── im-auth/             # authentication service
├── im-connect/          # connection service (WebSocket)
├── im-leaf/             # ID generation service
├── im-file/             # file management service
├── im-platform/         # platform service
├── im-framework/        # base framework aggregation
│   ├── im-core/         # core utilities
│   ├── im-domain/       # domain models
│   ├── im-general/      # common components
│   ├── im-grpc/         # gRPC
│   ├── im-security/     # security
│   ├── im-spring/       # spring extensions
│   └── im-common/       # utility libraries
└── docs/                # documentation
```

### Code Style

- Follow Alibaba Java Development Manual
- Unified code formatting
- Comprehensive documentation
- Unit test coverage > 80%

### API Docs

After services are up:

- **Swagger UI**: http://localhost:8085/doc.html
- **OpenAPI**: http://localhost:8085/v3/api-docs

## 🧪 Testing

```bash
# Unit tests
mvn test

# Integration tests
mvn verify

# Coverage report
mvn jacoco:report
```

## 📦 Deployment

### Docker

```bash
# Build images
mvn clean package -DskipTests

# Deploy all services with Docker Compose
docker-compose up -d
```

### Production Guidelines

- Use Nginx for reverse proxy and load balancing
- Configure SSL certificates for HTTPS
- Tune JVM parameters
- Set up logging, monitoring, and alerting

## 📊 Performance

- **Concurrent connections**: 10,000+ WebSocket connections
- **Message latency**: Average < 100ms
- **Throughput**: 10,000+ TPS
- **Response time**: 99% < 200ms

> Test environment: 8-core/16GB cloud server, JDK 21, Docker deployment

## 📝 Changelog (Overview)

- New modules
    - `im-file`: Image processing (compression, thumbnails, watermark), chunk/resume uploads, MD5 validation, NSFW image
      moderation
    - `im-platform`: App updates and short links (Bloom dedup, Caffeine LRU, Redis caching, layered visit-count
      aggregation)
    - `im-leaf`: Multiple ID strategies with Nacos worker-id allocation
    - `im-ai`: Spring AI integration (chat/embeddings/tools), improved prompt/session management
    - `im-analysis`: Text analytics: segmentation/keywords/dependency parsing
    - `im-proxy`: Nginx proxy template generation and maintenance
    - `im-meet`: Netty-based real-time meeting service (WebSocket)
- Gateway enhancements
    - Global signing, JWT auth, blacklist, dynamic fallback filters, route caching (`im-gateway`)
- Connection service enhancements
    - Connection/message rate limiter, monitoring, virtual-thread optimization, improved message pipeline (`im-connect`)
- Business service optimizations
    - Redisson-based locks, Redis hot cache, idempotency improvements, and performance logging across group/user
      operations (`im-server`)
- File service fixes
    - Fix MinIO client signing issue and improve stability (`im-file` `PearlMinioClient.java`)

## 🙏 Acknowledgements

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Cloud Alibaba](https://github.com/alibaba/spring-cloud-alibaba)
- [Nacos](https://nacos.io/)
- [Redis](https://redis.io/)
- [RabbitMQ](https://www.rabbitmq.com/)
- [MinIO](https://min.io/)
- [SRS](https://github.com/ossrs/srs)

## 🤝 Contributing

We welcome all contributions! See [CONTRIBUTING.md](CONTRIBUTING.md) for details.

### How to Contribute

1. **Fork** the project
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open a **Pull Request**

### Developer Communication

- Open issues to report problems or request features
- Join the tech community for discussion

## 📞 Contact

- **Project Home**: [https://github.com/LucklySpace/Lucky-cloud](https://github.com/LucklySpace/Lucky-cloud)
- **Issues**: [Issues](https://github.com/LucklySpace/Lucky-cloud/issues)

## 📢 Disclaimer

This project is for learning and reference only and must not be used for commercial purposes. The author is not
responsible for any direct or indirect loss caused by the use of this project.

1. Open-source learning project for IM technology study and research
2. Technical schemes and code are for reference only; no guarantee for production stability/security/reliability
3. Users assume all risks (including data loss, system damage, communication security, etc.)
4. No warranty or support is provided, and no guarantees for outcomes
5. Commercial use is at your own legal risk and responsibility

## 💬 Project Status

Actively maintained with continuous updates and improvements.
