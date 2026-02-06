# im-oss-rpc-api 模块

## 模块概述

`im-oss-rpc-api` 是对象存储服务（Object Storage Service）的 RPC API
模块，提供统一的文件存储接口定义和数据模型。该模块为文件上传、下载、分片传输、媒体处理等核心功能提供类型安全的 API 定义。

### 核心功能

- **文件存储管理**：支持任意类型文件的存储和管理
- **分片上传**：大文件分片上传，支持断点续传
- **媒体处理**：图片压缩、缩略图生成、水印添加
- **分桶存储**：按文件类型自动分配存储桶
- **类型安全**：完整的 DTO/VO/枚举定义

### 技术栈

- **Java 21**
- **Spring Boot 3.5.9**
- **Dubbo**：RPC 服务框架
- **Swagger/OpenAPI 3.0**：API 文档规范
- **Jackson**：JSON 序列化

---

## 模块架构

```
im-oss-rpc-api/
├── src/main/java/com/xy/lucky/api/
│   ├── dto/                    # 数据传输对象
│   │   ├── OssFileDto.java                    # 文件上传信息
│   │   ├── FileDownloadRangeDto.java          # 下载范围
│   │   └── OssFileMediaInfoDto.java           # 媒体处理参数
│   ├── vo/                     # 视图对象
│   │   ├── FileVo.java                        # 文件信息
│   │   ├── FileChunkVo.java                   # 分片上传信息
│   │   └── FileUploadProgressVo.java          # 上传进度
│   └── enums/                  # 枚举定义
│       ├── StorageBucketEnum.java             # 存储桶枚举
│       └── BoolEnum.java                      # 布尔枚举
└── src/main/resources/
    └── openapi/               # OpenAPI 规范文件
        ├── oss-file-api.yaml                 # 文件服务 API
        └── oss-media-api.yaml                # 媒体服务 API
```

---

## 核心组件详解

### 1. 数据传输对象（DTO）

#### OssFileDto - 文件上传信息

文件上传时的核心数据结构，包含文件基本信息和分片上传参数。

```java
@Data
@Builder
public class OssFileDto {
    private String uploadId;        // 分片上传的 uploadId
    private String bucketName;      // 桶名称
    private String identifier;      // 文件唯一标识（MD5）
    private String fileName;        // 文件名
    private String fileType;        // 文件类型
    private String objectKey;       // 文件的 key
    private String contentType;     // 文件 MIME 类型
    private Long fileSize;          // 文件大小（字节）
    private Long partSize;          // 每个分片大小
    private Integer partNum;        // 分片数量
}
```

**字段说明：**

| 字段            | 类型      | 必填    | 说明                                 |
|---------------|---------|-------|------------------------------------|
| `uploadId`    | String  | 否     | 分片上传的唯一标识，由 OSS 服务生成               |
| `bucketName`  | String  | 否     | 存储桶名称，可通过 `StorageBucketEnum` 自动确定 |
| `identifier`  | String  | **是** | 文件的 MD5 值，用于秒传和去重                  |
| `fileName`    | String  | **是** | 原始文件名                              |
| `fileType`    | String  | 否     | 文件扩展名                              |
| `objectKey`   | String  | 否     | OSS 中的对象键（存储路径）                    |
| `contentType` | String  | 否     | MIME 类型，如 `image/jpeg`             |
| `fileSize`    | Long    | 否     | 文件总大小（字节）                          |
| `partSize`    | Long    | 否     | 每个分片的大小（字节）                        |
| `partNum`     | Integer | **是** | 分片总数，最小值为 1                        |

**使用示例：**

```java
// 构建分片上传请求
OssFileDto dto = OssFileDto.builder()
    .identifier("d41d8cd98f00b204e9800998ecf8427e")  // MD5
    .fileName("large-video.mp4")
    .contentType("video/mp4")
    .fileSize(536870912L)  // 512MB
    .partNum(512)          // 分为 512 个分片
    .partSize(1048576L)    // 每个分片 1MB
    .build();
```

---

#### FileDownloadRangeDto - 下载范围

支持断点续传和分片下载的参数定义。

```java
@Data
@Builder
public class FileDownloadRangeDto {
    private Long start;       // 开始位置
    private Long end;         // 结束位置
    private Long fileSize;    // 文件总大小
}
```

**HTTP Range 请求格式：**

```
Range: bytes=0-1023          # 下载前 1KB
Range: bytes=1024-2047       # 下载第 2KB
Range: bytes=0-              # 从 0 开始下载到结尾
Range: bytes=-512            # 下载最后 512 字节
```

**使用示例：**

```java
// 解析 Range 请求头
String rangeHeader = "bytes=0-1048575";  // 前 1MB
FileDownloadRangeDto range = parseRange(rangeHeader);
// 结果: start=0, end=1048575, fileSize=...
```

---

#### OssFileMediaInfoDto - 媒体处理参数

图片处理操作的参数配置。

```java
@Data
@Builder
public class OssFileMediaInfoDto {
    private Integer width;                  // 宽度
    private Integer height;                 // 高度
    private String watermarkPath;           // 水印图片地址
    private Positions watermarkPosition;    // 水印位置
    private Float opacity;                  // 透明度（0.0-1.0）
    private Double scale;                   // 放大倍数
    private Double ratio;                   // 比例
    private String format;                  // 输出格式
}
```

**水印位置选项：**

| 位置值             | 说明      |
|-----------------|---------|
| `TOP_LEFT`      | 左上角     |
| `TOP_CENTER`    | 顶部居中    |
| `TOP_RIGHT`     | 右上角     |
| `CENTER`        | 正中心     |
| `BOTTOM_LEFT`   | 左下角     |
| `BOTTOM_CENTER` | 底部居中    |
| `BOTTOM_RIGHT`  | 右下角（默认） |

**使用示例：**

```java
// 生成带水印的缩略图
OssFileMediaInfoDto mediaInfo = OssFileMediaInfoDto.builder()
    .width(200)
    .height(200)
    .watermarkPath("/watermarks/logo.png")
    .watermarkPosition(Positions.BOTTOM_RIGHT)
    .opacity(0.5f)
    .format("png")
    .build();
```

---

### 2. 视图对象（VO）

#### FileVo - 文件信息

文件上传或查询后返回的文件信息。

```java
@Data
@Accessors(chain = true)
@Builder
public class FileVo {
    private String identifier;       // 文件 MD5
    private String name;             // 文件名称
    private Long size;               // 文件大小（字节）
    private String type;             // 文件类型
    private String path;             // 文件访问地址
    private String thumbnailPath;    // 缩略图地址（图片类）
}
```

**返回示例（JSON）：**

```json
{
  "identifier": "d41d8cd98f00b204e9800998ecf8427e",
  "name": "photo.jpg",
  "size": 2048576,
  "type": "image/jpeg",
  "path": "https://cdn.example.com/image/photo.jpg",
  "thumbnailPath": "https://cdn.example.com/thumbnail/photo_200x200.jpg"
}
```

---

#### FileChunkVo - 分片上传信息

分片上传初始化后返回的上传地址和 ID。

```java
@Data
@Builder
public class FileChunkVo {
    private Map<String, String> uploadUrl;  // 上传地址Map
    private String uploadId;                 // 上传 ID
}
```

**字段详解：**

- **uploadUrl**：Map 结构，key 为分片号（1, 2, 3...），value 为该分片的预签名上传 URL
- **uploadId**：分片上传的唯一标识，后续合并分片时需要

**返回示例（JSON）：**

```json
{
  "uploadUrl": {
    "1": "https://oss.example.com/file?partNumber=1&uploadId=xxx&signature=yyy",
    "2": "https://oss.example.com/file?partNumber=2&uploadId=xxx&signature=zzz",
    "3": "https://oss.example.com/file?partNumber=3&uploadId=xxx&signature=aaa"
  },
  "uploadId": "12345678-1234-1234-1234-123456789abc"
}
```

---

#### FileUploadProgressVo - 上传进度

查询文件上传进度时返回的详细信息。

```java
@Data
@Builder
public class FileUploadProgressVo {
    private Integer isNew;                 // 是否为新文件（1=是，0=否）
    private Integer isFinish;              // 是否已完成上传（1=是，0=否）
    private String path;                   // 文件地址
    private String uploadId;               // 上传 ID
    private Map<String, String> undoneChunkMap;  // 未上传的分片
}
```

**字段详解：**

| 字段               | 说明               | 示例值                                |
|------------------|------------------|------------------------------------|
| `isNew`          | 系统是否从未见过此文件      | `1`（新文件），`0`（已上传过）                 |
| `isFinish`       | 是否已完成所有分片上传和合并   | `1`（已完成），`0`（进行中）                  |
| `path`           | 文件的访问地址          | `https://cdn.example.com/file.pdf` |
| `uploadId`       | 当前上传任务 ID        | `uuid-xxx`                         |
| `undoneChunkMap` | 未完成上传的分片号及上传 URL | `{"3": "url3", "5": "url5"}`       |

**典型场景：**

1. **首次上传**：`isNew=1, isFinish=0, undoneChunkMap={全部分片}`
2. **部分上传**：`isNew=0, isFinish=0, undoneChunkMap={剩余分片}`
3. **已完成**：`isNew=0, isFinish=1, undoneChunkMap=null, path=访问地址`

---

### 3. 枚举类型

#### StorageBucketEnum - 存储桶枚举

按文件类型自动分配存储桶的枚举定义。

```java
@Getter
public enum StorageBucketEnum {
    DOCUMENT("document", "文档文件桶",
        asSet("txt", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", ...)),

    PACKAGE("package", "压缩文件桶",
        asSet("zip", "rar", "7z", "tar", "gz", ...)),

    AUDIO("audio", "音频文件桶",
        asSet("mp3", "wav", "flac", "aac", "ogg", ...)),

    VIDEO("video", "视频文件桶",
        asSet("mp4", "avi", "mov", "mkv", "flv", ...)),

    IMAGE("image", "图片文件桶",
        asSet("jpg", "png", "gif", "bmp", "webp", "svg", ...)),

    INSTALLER("installer", "安装包文件桶",
        asSet("exe", "msi", "apk", "dmg", ...)),

    THUMBNAIL("thumbnail", "图片缩略图文件桶",
        asSet("thumbnail")),

    OTHER("other", "其他文件桶",
        asSet("*"));

    // 枚举属性
    private final String code;       // 桶编码
    private final String name;       // 桶名称
    private final Set<String> types; // 支持的文件扩展名集合
}
```

**支持的文件类型：**

| 桶类型       | 编码        | 支持的扩展名（部分）                                                          |
|-----------|-----------|---------------------------------------------------------------------|
| DOCUMENT  | document  | txt, pdf, doc, docx, xls, xlsx, ppt, pptx, csv, xml, json, html, md |
| PACKAGE   | package   | zip, rar, 7z, tar, gz, bz2, xz                                      |
| AUDIO     | audio     | mp3, wav, flac, aac, ogg, m4a, wma, midi, opus                      |
| VIDEO     | video     | mp4, avi, mov, mkv, flv, webm, mpeg, rmvb, 3gp                      |
| IMAGE     | image     | jpg, png, gif, bmp, webp, svg, tiff, heic, avif, ico, raw           |
| INSTALLER | installer | exe, msi, apk, dmg, pkg, deb, rpm, bat, sh                          |
| THUMBNAIL | thumbnail | thumbnail                                                           |
| OTHER     | other     | *（任意其他类型）                                                           |

**工具方法：**

```java
// 根据文件名获取桶编码
String bucketCode = StorageBucketEnum.getBucketCodeByFilename("photo.jpg");
// 返回: "image"

// 根据文件名获取桶名称
String bucketName = StorageBucketEnum.getBucketNameByFilename("document.pdf");
// 返回: "文档文件桶"

// 根据文件名获取枚举
Optional<StorageBucketEnum> bucket = StorageBucketEnum.fromFilename("video.mp4");
// 返回: Optional[VIDEO]

// 根据后缀判断是否属于某个桶
boolean isImage = StorageBucketEnum.suffixBelongsTo("png", StorageBucketEnum.IMAGE);
// 返回: true

// 提取文件后缀
String suffix = StorageBucketEnum.getSuffix("archive.tar.gz");
// 返回: "gz"

// 根据 code 获取枚举
StorageBucketEnum bucket = StorageBucketEnum.fromCode("audio");
// 返回: AUDIO
```

**设计要点：**

1. **大小写不敏感**：所有文件扩展名映射均转为小写，支持 `JPG`、`Jpg`、`jpg`
2. **高性能查询**：使用静态 HashMap 缓存扩展名映射，查询时间复杂度 O(1)
3. **自动归类**：无法识别的文件类型自动归为 `OTHER`
4. **不可变集合**：扩展名集合在构造后不可修改，线程安全

---

#### BoolEnum - 布尔枚举

数据库层面的布尔值表示。

```java
@Getter
public enum BoolEnum {
    TRUE(1, "是"),
    FALSE(0, "否");

    private final Integer code;
    private final String desc;
}
```

**使用场景：**

- 数据库字段类型为 `TINYINT` 时，使用 `1/0` 代替 `TRUE/FALSE`
- 避免不同数据库对布尔类型的兼容性问题

---

## Maven 依赖

```xml
<dependency>
    <groupId>com.xy.lucky</groupId>
    <artifactId>im-oss-rpc-api</artifactId>
    <version>${project.version}</version>
</dependency>
```

**传递依赖：**

```xml
<!-- 核心工具 -->
<dependency>
    <groupId>com.xy.lucky</groupId>
    <artifactId>im-starter-core</artifactId>
</dependency>

<!-- 公共模块 -->
<dependency>
    <groupId>com.xy.lucky</groupId>
    <artifactId>im-starter-common</artifactId>
</dependency>

<!-- Dubbo RPC -->
<dependency>
    <groupId>com.xy.lucky</groupId>
    <artifactId>im-starter-dubbo</artifactId>
</dependency>

<!-- Swagger 注解 -->
<dependency>
    <groupId>io.swagger.core.v3</groupId>
    <artifactId>swagger-annotations</artifactId>
</dependency>
```

---

## 使用示例

### 1. 文件上传

```java
import com.xy.lucky.api.dto.OssFileDto;
import com.xy.lucky.api.vo.FileVo;
import com.xy.lucky.api.enums.StorageBucketEnum;

public class FileUploadExample {

    public void uploadFile() {
        // 1. 确定存储桶
        String fileName = "contract.pdf";
        String bucketCode = StorageBucketEnum.getBucketCodeByFilename(fileName);
        // 返回: "document"

        // 2. 构建上传请求
        OssFileDto request = OssFileDto.builder()
            .identifier("a1b2c3d4e5f6...")  // MD5
            .fileName(fileName)
            .bucketName(bucketCode)
            .contentType("application/pdf")
            .fileSize(2048576L)
            .build();

        // 3. 调用上传接口
        // FileVo result = ossFileService.uploadFile(request.getIdentifier(), file);
    }
}
```

---

### 2. 分片上传

```java
import com.xy.lucky.api.dto.OssFileDto;
import com.xy.lucky.api.vo.FileChunkVo;
import com.xy.lucky.api.vo.FileUploadProgressVo;
import com.xy.lucky.api.vo.FileVo;

public class MultipartUploadExample {

    public void uploadLargeFile() {
        String identifier = "abc123...";  // MD5
        int partCount = 100;

        // 1. 检查上传进度
        FileUploadProgressVo progress = checkProgress(identifier);
        if (progress.getIsFinish() == 1) {
            // 文件已存在，秒传
            System.out.println("文件已存在: " + progress.getPath());
            return;
        }

        // 2. 初始化分片上传
        OssFileDto request = OssFileDto.builder()
            .identifier(identifier)
            .fileName("large-video.mp4")
            .bucketName(StorageBucketEnum.getBucketCodeByFilename("video.mp4"))
            .fileSize(104857600L)  // 100MB
            .partNum(partCount)
            .partSize(1048576L)    // 1MB per chunk
            .build();

        FileChunkVo chunkInfo = initUpload(request);

        // 3. 上传分片（使用返回的预签名 URL）
        chunkInfo.getUploadUrl().forEach((partNumber, uploadUrl) -> {
            uploadPart(uploadUrl, partData);
        });

        // 4. 合并分片
        FileVo result = mergeUpload(identifier);
        System.out.println("上传完成: " + result.getPath());
    }

    private FileUploadProgressVo checkProgress(String identifier) {
        // 调用 GET /api/file/multipart/check?identifier=xxx
        return null;
    }

    private FileChunkVo initUpload(OssFileDto request) {
        // 调用 POST /api/file/multipart/init
        return null;
    }

    private void uploadPart(String url, byte[] data) {
        // 使用 HTTP PUT 上传到预签名 URL
    }

    private FileVo mergeUpload(String identifier) {
        // 调用 GET /api/file/multipart/merge?identifier=xxx
        return null;
    }
}
```

---

### 3. 图片处理

```java
import com.xy.lucky.api.dto.OssFileMediaInfoDto;
import net.coobird.thumbnailator.geometry.Positions;

public class ImageProcessingExample {

    public void processImage() {
        // 构建图片处理参数
        OssFileMediaInfoDto mediaInfo = OssFileMediaInfoDto.builder()
            // 缩放尺寸
            .width(800)
            .height(600)

            // 水印配置
            .watermarkPath("/watermarks/logo.png")
            .watermarkPosition(Positions.BOTTOM_RIGHT)
            .opacity(0.5f)  // 50% 透明度
            .ratio(0.3)     // 水印占图片 30%

            // 输出格式
            .format("png")
            .build();

        // 调用图片处理接口
        // FileVo result = mediaService.processImage(identifier, mediaInfo);
    }
}
```

---

### 4. 断点续传下载

```java
import com.xy.lucky.api.dto.FileDownloadRangeDto;

public class ResumableDownloadExample {

    public void downloadWithResume() {
        String identifier = "file-md5-123";
        long downloadedBytes = 1048576;  // 已下载 1MB

        // 构建 Range 请求
        String rangeHeader = String.format("bytes=%d-", downloadedBytes);

        // 发起下载请求
        // ResponseEntity<?> response = fileService.download(identifier, rangeHeader);

        // 响应状态码：
        // - 200 OK: 完整文件
        // - 206 Partial Content: 部分内容（断点续传）
        // - 416 Range Not Satisfiable: 范围无效
    }
}
```

---

## API 规范文件

模块包含完整的 OpenAPI 3.0 规范文件，位于 `src/main/resources/openapi/` 目录：

### 1. oss-file-api.yaml

文件管理服务 API，包括：

- **分片上传进度查询**：`GET /api/file/multipart/check`
- **初始化分片上传**：`POST /api/file/multipart/init`
- **合并分片**：`GET /api/file/multipart/merge`
- **文件存在性检查**：`GET /api/file/multipart/isExits`
- **文件上传**：`POST /api/file/upload`
- **文件下载**：`GET /api/file/download`
- **获取文件 MD5**：`GET /api/file/md5`

### 2. oss-media-api.yaml

媒体处理服务 API，包括：

- **图片上传**：`POST /api/media/image/upload`
- **头像上传**：`POST /api/media/avatar/upload`
- **图片处理**：`POST /api/media/image/process`

**使用 OpenAPI 文件：**

```bash
# 导入到 Swagger UI
# 访问: https://editor.swagger.io/
# 粘贴 yaml 内容即可查看交互式文档

# 生成客户端 SDK
# 使用 openapi-generator-maven-plugin
```

---

## 设计原则

### 1. 类型安全

所有 API 参数和返回值都有明确的类型定义，避免运行时错误。

### 2. 不可变性

DTO/VO 对象使用 `@Builder` 模式，创建后不可修改，线程安全。

### 3. 验证注解

关键字段使用 Jakarta Validation 注解，自动验证参数合法性。

```java
@NotBlank(message = "请输入文件md5值")
private String identifier;

@NotNull
@Min(1)
private Integer partNum;
```

### 4. JSON 优化

使用 `@JsonInclude(JsonInclude.Include.NON_NULL)` 忽略 null 字段，减小传输体积。

---

## 最佳实践

### 1. 文件唯一性

使用 MD5 作为文件唯一标识，实现：

- **秒传**：文件已存在时直接返回地址，无需重复上传
- **去重**：相同文件只存储一份，节省存储空间
- **完整性校验**：上传完成后验证 MD5，确保文件未损坏

### 2. 分片上传策略

| 文件大小        | 建议分片大小 | 分片数量   |
|-------------|--------|--------|
| < 5MB       | 不分片    | 1      |
| 5MB - 100MB | 1MB    | 5-100  |
| 100MB - 1GB | 5MB    | 20-200 |
| > 1GB       | 10MB   | 100+   |

**计算公式：**

```java
// 推荐分片大小（1MB - 10MB）
long partSize = Math.max(1024 * 1024, Math.min(fileSize / 100, 10 * 1024 * 1024));

// 分片数量
int partNum = (int) Math.ceil((double) fileSize / partSize);
```

### 3. 存储桶策略

- **按类型分桶**：不同文件类型存入不同桶，便于管理和计费
- **自动分配**：使用 `StorageBucketEnum` 自动识别文件类型
- **自定义桶名**：支持手动指定桶名称（如用户头像独立桶）

### 4. 错误处理

```java
try {
    FileVo result = ossFileService.uploadFile(identifier, file);
} catch (FileException e) {
    // 业务异常：文件不存在、上传失败等
    log.error("文件上传失败: {}", e.getMessage());
} catch (Exception e) {
    // 系统异常：网络错误、存储服务不可用等
    log.error("系统异常", e);
}
```

---

## 常见问题

### Q1: MD5 如何计算？

```java
import java.security.MessageDigest;
import java.io.InputStream;

public static String calculateMD5(InputStream is) throws Exception {
    MessageDigest md = MessageDigest.getInstance("MD5");
    byte[] buffer = new byte[8192];
    int len;

    while ((len = is.read(buffer)) != -1) {
        md.update(buffer, 0, len);
    }

    byte[] digest = md.digest();
    StringBuilder sb = new StringBuilder();

    for (byte b : digest) {
        sb.append(String.format("%02x", b));
    }

    return sb.toString();
}
```

### Q2: 如何处理文件名重复？

使用 MD5 作为唯一标识，原始文件名作为展示名称：

```java
String objectKey = String.format("%s/%s", md5SubDir, originalFileName);
// 示例: "d41d8cd98f00b204e9800998ecf8427e/photo.jpg"
```

### Q3: 分片上传超时怎么办？

分片上传使用预签名 URL，客户端直接上传到 OSS，不经过应用服务器：

- 优点：减轻服务器压力，支持并发上传
- 超时处理：重新查询进度，获取剩余分片的 URL 继续上传

### Q4: 如何限制文件类型？

在应用层验证：

```java
String fileName = "file.exe";
String bucketCode = StorageBucketEnum.getBucketCodeByFilename(fileName);

if ("installer".equals(bucketCode) || "other".equals(bucketCode)) {
    throw new FileException("不允许上传此类型文件");
}
```

---

## 版本兼容性

| 组件          | 版本要求   |
|-------------|--------|
| Java        | 21+    |
| Spring Boot | 3.5.9+ |
| Dubbo       | 3.x    |
| Swagger     | 3.0.0+ |

---

## 相关文档

- [API.md](./API.md) - 详细的 API 接口文档
- [HELP.md](./HELP.md) - 快速参考指南
- [oss-file-api.yaml](./src/main/resources/openapi/oss-file-api.yaml) - 文件服务 OpenAPI 规范
- [oss-media-api.yaml](./src/main/resources/openapi/oss-media-api.yaml) - 媒体服务 OpenAPI 规范

---

## 更新日志

### v1.0.0 (2025-02-06)

- 初始版本发布
- 完整的 DTO/VO/枚举定义
- 支持 S3 协议兼容的对象存储服务
- 分片上传、断点续传、媒体处理功能
- OpenAPI 3.0 规范文件
