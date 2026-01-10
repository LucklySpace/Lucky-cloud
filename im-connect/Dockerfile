# ============================================
# IM Connect 服务 Docker 构建文件
# ============================================
# 构建命令:
#   docker build -t im-connect:1.0.0 .
#
# 运行命令 (默认端口):
#   docker run -d --name im-connect -p 19000:19000 -p 19001:19001 -p 19002:19002 im-connect:1.0.0
#
# 运行命令 (自定义端口映射，解决端口冲突):
#   docker run -d --name im-connect -p 29000:19000 -p 29001:19001 -p 29002:19002 im-connect:1.0.0
#
# 查看运行中的容器:
#   docker ps
#
# 停止并删除旧容器 (解决端口冲突):
#   docker stop im-connect && docker rm im-connect
#
# 查看日志:
#   docker logs -f im-connect
# ============================================

# 使用官方 OpenJDK 21 镜像作为基础镜像
FROM eclipse-temurin:21-jre

# 维护者信息
LABEL maintainer="Lucky Team"
LABEL version="1.0.0"
LABEL description="IM Connect"

# 设置工作目录
WORKDIR /app

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# JVM 配置（可通过环境变量覆盖）
ENV JAVA_OPTS="-Xmx512M -Xms256M -XX:+UseG1GC -XX:MaxGCPauseMillis=200"


# 复制 jar 文件到容器中
COPY target/*.jar app.jar

# 暴露端口（WebSocket）
EXPOSE 19000 19001 19002

# 运行应用程序
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
