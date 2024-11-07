# Lynk

**im-server**: http://localhost:8085/doc.html




## 项目环境与依赖说明文档

**本文档介绍项目所使用的主要技术栈及其版本，包括JDK、Spring Boot、Spring Cloud Alibaba的具体配置。**

---

### 1. **JDK 版本**

* **版本**：JDK 17
* **说明**：项目使用JDK 17作为运行时环境，具有增强的性能、语言特性和更高的安全性，符合最新的Java标准，确保项目在长期维护期间的稳定性与性能表现。

### 2. **Spring Boot 版本**

* **版本**：Spring Boot 3.0.3
* **主要特性**：
  * **兼容JDK 17**：Spring Boot 3.x系列全面支持JDK 17，使得项目能够利用Java最新特性。
  * **原生支持**：优化对容器化部署及云原生环境的支持。
  * **简化依赖管理**：引入最新的依赖管理，提供一站式的Spring技术栈支持。
* **配置说明**：
  * **YAML配置**：所有项目的应用配置文件采用`application.yml`格式，集中管理和组织不同环境的配置项。
  * **自动配置**：Spring Boot 3.0.3自动配置功能增强，减少了传统Spring项目中复杂的手动配置流程。
  * **默认Web服务器**：项目默认使用`Undertow`作为Web服务器，以实现高性能和低资源消耗的需求。

### 3. **Spring Cloud Alibaba 版本**

* **版本**：Spring Cloud Alibaba 2022.0.0.0-RC2
* **说明**：该版本是Spring Cloud Alibaba为Spring Cloud 2022版本提供的适配支持，全面增强了项目在分布式系统中的服务治理、配置管理等功能。
* **主要组件**：
  * **Nacos**：用于配置管理和服务发现。支持服务的自动注册、发现与动态配置管理。`im-gateway`模块依赖Nacos进行负载均衡和服务发现。
  * **Sentinel**：用于流量控制、熔断降级、系统保护。可有效保证系统的高可用性和容错性，尤其是在分布式系统中承载高并发时。
  * **RocketMQ**（如果有需要）：消息队列服务，用于实现服务之间的异步通信，确保在高负载场景下系统的可扩展性。
  * **Seata**（可选）：分布式事务管理解决方案，确保跨服务事务的一致性。
* **配置建议**：
  * **服务注册和发现**：启用Nacos作为服务注册中心，确保微服务在分布式环境中的可发现性。
  * **配置中心**：利用Nacos的动态配置管理，简化多环境下的配置管理，支持配置的热更新。

## 模块说明

**项目包含4个核心模块，各模块分工明确，以实现高效、稳定的即时通讯服务。以下是各模块的详细介绍及消息发送流程。**

---

### 1. **im-auth** - 登录鉴权模块

**服务端口**：8084

**主要功能**：

* **提供用户身份验证，确保访问的合法性。**
* **负责登录、注册、令牌生成和验证等功能。**
* **提供用户信息，提供rsa公钥，用于登录时用户密码加密。**
* **提供手机短信，用于通过手机号登录时发送相关验证码。**

**工作流程**：

* **用户的登录请求由**`im-auth`模块接收并验证身份。
* **验证通过后生成token，并将token 和 用户id  返回客户端以用于后续请求。**

---

### 2. **im-connect** - 长连接管理模块

**服务端口**：19000，19001，19002

**主要功能**：

* **负责客户端的长连接管理，确保与用户的持久连接稳定。**
* **长连接不做任何业务处理，只用于接收队列消息，并将消息发送给用户端。**
* **支持长连接的注册和心跳机制，通过RabbitMQ消息队列接收从**`im-server`转发的消息，并推送到用户的长连接。

**工作流程**：

* **客户端通过websocket请求与**`im-connect`建立长连接后，执行长连接注册并通过心跳维持连接。
* **长连接注册时会注册用户信息和机器码到redis中，后续网关会定向转发websocket请求到相应的服务**
* **每个**`im-connect`服务在启动时会生成机器码，并创建与机器码相关的RabbitMQ队列。
* **从RabbitMQ接收到的消息通过该长连接推送到用户客户端。**
* **用户断开连接会删除redis中的对应信息**

---

### 3. **im-server** - 业务逻辑处理模块

**服务端口**：8085

**主要功能**：

* **执行即时通讯的核心业务逻辑，包括消息发送、消息拉取、文件上传、群聊管理，群聊头像生成 等等。**
* **负责接收客户端的消息并分发到对应**`im-connect`服务的RabbitMQ队列。

**消息发送流程**：

1. **用户发送消息至**`im-server`模块。
2. `im-server`处理消息，并将其转发到对应的`im-connect`服务的RabbitMQ队列。
3. `im-connect`从RabbitMQ队列接收消息，并通过该用户的长连接推送到客户端。

---

### 4. **im-gateway** - 请求网关模块

**服务端口**：9191

**主要功能**：

* **作为所有客户端请求的入口，负责请求的负载均衡与转发。**
* **通过Nacos实现服务注册与发现，并支持负载均衡。**

**工作流程**：

* **所有客户端请求通过**`im-gateway`进入系统。
* `im-gateway`根据请求类型将其转发到相应的模块（如`im-auth`或`im-server`）。
* `im-gateway`根据Redis中的用户信息，将长连接请求定向转发至对应的`im-connect`服务。

**说明**：

* **集成了sentinel，可通过其来实现网关限流和负载均衡 。**

---

### 消息发送流程补充说明

1. **用户发送消息**：
   * **用户将消息发送到**`im-server`，由`im-server`进行业务处理。
2. **消息下发至`im-connect`**：
   * `im-server`将消息发送至对应的`im-connect`服务的RabbitMQ队列中。
   * **每个**`im-connect`服务在启动时生成机器码并创建与该机器码相关的RabbitMQ队列。
3. **`im-connect`处理消息**：
   * `im-connect`通过RabbitMQ接收来自`im-server`的消息，并将其通过用户长连接直接推送到客户端。
4. **长连接注册与心跳**：
   * **用户登录后，**`im-gateway`将长连接请求负载均衡到`im-connect`。
   * **客户端在**`im-connect`注册成功后，将用户信息存入Redis，并通过定期发送心跳包来维持连接。

## 中间件

**以下是项目所依赖的各个中间件的启动命令和访问信息。注意部分配置需要指定物理机的IP地址，不能使用**`localhost`等回环地址。

---

### 1. SRS（Simple Realtime Server）启动命令

> **注意**：CANDIDATE环境变量必须设置为物理机的IP地址，不能使用localhost，否则会映射到Docker镜像内的地址。

* **地址**：`localhost:8080`
* **命令:**
  ```
  docker run -it -p 1935:1935 -p 1985:1985 -p 8080:8080 -p 1990:1990 -p 8088:8088 --env CANDIDATE=192.168.1.9 -p 8000:8000/udp registry.cn-hangzhou.aliyuncs.com/ossrs/srs:6.0-d2
  ```

### 2. Redis 启动命令

* **地址**：`localhost:6379`
* **命令:**
  ```
  docker run --name redis -p 6379:6379 -v /root/redis/conf/redis.conf:/usr/local/etc/redis/redis.conf -d redis --appendonly yes
  ```

### 3. MinIO 启动命令

* **地址**：`localhost:9000`
* **控制台地址**：`localhost:9090`
* **账号**：`minioadmin`
* **密码**：`minioadmin`
* **命令**：
  
  ```
  docker run -p 9000:9000 -p 9090:9090 --name minio -d --restart=always -e "MINIO_ACCESS_KEY=minioadmin" -e "MINIO_SECRET_KEY=minioadmin" -v D:\Docker-vm\folder\minio\data:/data -v D:\Docker-vm\folder\minio\config:/root/.minio minio/minio server /data --console-address ":9090" --address ":9000"
  ```
  
  **或**
  
  ```
  docker run -p 9000:9000 -p 9090:9090 --name minio -d --restart=always -e "MINIO_ACCESS_KEY=minioadmin" -e "MINIO_SECRET_KEY=minioadmin" -v /root/minio/data:/data -v /root/minio/config:/root/.minio minio/minio server /data --console-address ":9090" --address ":9000"
  ```
  
  **或**
  
  ```
  docker run -d -p 9000:9000 -p 9090:9090 --name minio --restart=always --privileged=true -v /root/minio/data:/data -e "MINIO_ROOT_USER=minioadmin" -e "MINIO_ROOT_PASSWORD=minioadmin" minio/minio:RELEASE.2023-04-28T18-11-17Z server /data --console-address ":9090"
  ```

### 4. Nacos 启动命令

* **地址**：`localhost:8848/nacos`
* **账号**：`nacos`
* **密码**：`nacos`
* **命令:**
  ```
  docker run -itd --name nacos --env PREFER_HOST_MODE=hostname --env MODE=standalone --env NACOS_AUTH_IDENTITY_KEY=serverIdentity --env NACOS_AUTH_IDENTITY_VALUE=security --env NACOS_AUTH_TOKEN=SecretKey012345678901234567890123456789012345678901234567890123456789 -p 8848:8848 -p 9848:9848 -p 9849:9849 nacos/nacos-server:v2.2.1
  ```

### 5. RabbitMQ 启动命令

* **管理地址**：`localhost:15672`
* **连接地址**：`localhost:5672`
* **账号**：`guest`
* **密码**：`guest`
* **命令**：
  
  1. **下载镜像**：
     ```
     docker pull rabbitmq
     ```
  2. **启动容器**：
     ```
     docker run -d --hostname my-rabbit --name rabbit -p 15672:15672 -p 5671-5672:5671-5672 rabbitmq
     ```
  3. **进入容器**：
     ```
     docker exec -it rabbit /bin/bash
     ```
  4. **启动UI插件**：
     ```
     rabbitmq-plugins enable rabbitmq_management
     ```
  
  **若UI插件无效，可以参考：**[RabbitMQ UI插件失效解决](https://blog.csdn.net/qq_45369827/article/details/115921401)

## 视频通话

**视频通话使用 webrtc和 srs，其中webrtc必须使用https 或本地的localhost。所以如果要进行双向视频通话必须使用nginx反向代理到服务机上。以下为nginx 位置，注意里面的server 后面的ip 需改为 服务机的ip**

```
user  nobody;
worker_processes  1;
​
events {
    worker_connections  1024;
}
​
http {
    include       mime.types;
    default_type  application/octet-stream;
​
    sendfile        on;
    keepalive_timeout  65;
​
    server {
        listen       80;
        server_name  localhost;
​
        location / {
            root   html;
            index  index.html index.htm;
        }
​
        error_page   500 502 503 504  /50x.html;
        location = /50x.html {
            root   html;
        }
    }
}
​
stream {
    upstream backend_server_1 {
        server 192.168.1.9:9000;
    }
​
    upstream backend_server_2 {
        server 192.168.1.9:1985;
    }
​
    upstream backend_server_3 {
        server 192.168.1.9:8000;
    }
    
    upstream backend_server_4 {
        server 192.168.1.9:8080;
    }
​
    server {
        listen 9000;
        proxy_pass backend_server_1;
    }
​
    server {
        listen 1985;
        proxy_pass backend_server_2;
    }
​
    server {
        listen 8000;
        proxy_pass backend_server_3;
    }
    
     server {
        listen 8080;
        proxy_pass backend_server_4;
    }
}
```

