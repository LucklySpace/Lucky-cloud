# 平台基础服务

用于 应用的自动更新服务。

## 功能特性

- 版本信息发布和管理
- 应用更新包上传和存储（基于 MinIO）
- 客户端更新检查和下载服务
- 支持多平台（Windows、macOS、Linux）

## 主要接口

1. `GET /api/v1/tauri/update/latest` - 获取最新版本信息
2. `GET /api/v1/tauri/update/download/{fileName}` - 下载更新文件
3. `POST /api/v1/tauri/update/releases` - 发布新版本
4. `POST /api/v1/tauri/update/releases/assets` - 上传版本资产文件

## 技术栈

- Spring Boot 3.x
- Spring Data JPA (PostgreSQL)
- MinIO 对象存储
- Knife4j API 文档
- Nacos 服务注册与配置中心