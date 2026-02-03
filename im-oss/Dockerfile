# 使用官方 OpenJDK 21 镜像作为基础镜像
FROM openjdk:21-jdk-slim

# 设置工作目录
WORKDIR /app

# 复制 jar 文件到容器中
COPY target/*.jar app.jar

# 暴露端口
EXPOSE 8083

# 运行应用程序
ENTRYPOINT ["java", "-jar", "app.jar"]