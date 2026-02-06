# im-oss-rpc-api API 接口文档

## 文档说明

本文档详细描述 `im-oss-rpc-api` 模块提供的所有 REST API 接口。所有接口均遵循 RESTful 设计规范，支持文件上传、下载、分片传输、媒体处理等功能。

### 基础信息

- **基础路径**：`/api` 或 `/api/{version}`
- **内容类型**：`application/json`（除文件上传外）
- **字符编码**：UTF-8
- **认证方式**：OAuth 2.0 Bearer Token

### 通用响应格式

#### 成功响应

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    ...
  }
}
```

#### 错误响应

```json
{
  "code": 400,
  "message": "请求参数错误",
  "data": null
}
```

#### 业务异常

| HTTP 状态码 | 错误代码                   | 说明       |
|----------|------------------------|----------|
| 400      | INVALID_PARAMETER      | 请求参数不合法  |
| 404      | FILE_NOT_FOUND         | 文件不存在    |
| 409      | FILE_CONFLICT          | 文件已存在    |
| 413      | FILE_TOO_LARGE         | 文件大小超过限制 |
| 415      | UNSUPPORTED_MEDIA_TYPE | 不支持的文件类型 |
| 500      | INTERNAL_ERROR         | 服务器内部错误  |

---

## 文件管理 API

### 1. 校验文件是否存在（获取上传进度）

获取文件的上传进度，支持秒传和断点续传。

#### 接口信息

- **接口路径**：`/api/file/multipart/check`
- **请求方法**：`GET`
- **接口描述**：根据文件 MD5 查询上传进度
- **是否需要认证**：是

#### 请求参数

| 参数名        | 类型     | 位置    | 必填 | 说明        |
|------------|--------|-------|----|-----------|
| identifier | string | query | 是  | 文件的 MD5 值 |

#### 响应数据

**类型**：[FileUploadProgressVo](#fileuploadprogressvo-上传进度)

**示例**：

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "isNew": 0,
    "isFinish": 0,
    "path": null,
    "uploadId": "12345678-1234-1234-1234-123456789abc",
    "undoneChunkMap": {
      "3": "https://oss.example.com/upload?partNumber=3&uploadId=xxx",
      "5": "https://oss.example.com/upload?partNumber=5&uploadId=xxx"
    }
  }
}
```

#### 响应状态说明

| isNew | isFinish | 说明   | 推荐操作                       |
|-------|----------|------|----------------------------|
| 1     | 0        | 首次上传 | 初始化分片上传                    |
| 0     | 0        | 部分上传 | 使用 undoneChunkMap 继续上传剩余分片 |
| 0     | 1        | 已完成  | 直接使用 path 获取文件             |

#### 请求示例

```bash
# curl
curl -X GET "https://api.example.com/api/file/multipart/check?identifier=d41d8cd98f00b204e9800998ecf8427e" \
  -H "Authorization: Bearer YOUR_TOKEN"

# JavaScript (fetch)
fetch('https://api.example.com/api/file/multipart/check?identifier=d41d8cd98f00b204e9800998ecf8427e', {
  headers: {
    'Authorization': 'Bearer YOUR_TOKEN'
  }
})
.then(response => response.json())
.then(data => console.log(data));
```

---

### 2. 初始化分片上传

初始化一个大文件的分片上传任务，生成预签名上传 URL。

#### 接口信息

- **接口路径**：`/api/file/multipart/init`
- **请求方法**：`POST`
- **接口描述**：初始化分片上传任务
- **是否需要认证**：是

#### 请求参数

**请求体**：[OssFileDto](#ossfiledto-文件上传信息)

| 字段          | 类型      | 必填 | 说明              |
|-------------|---------|----|-----------------|
| identifier  | string  | 是  | 文件的 MD5 值       |
| fileName    | string  | 是  | 文件名             |
| fileSize    | long    | 是  | 文件大小（字节）        |
| partNum     | integer | 是  | 分片数量（最小值：1）     |
| partSize    | long    | 否  | 每个分片大小（字节）      |
| contentType | string  | 否  | 文件 MIME 类型      |
| bucketName  | string  | 否  | 存储桶名称（不指定则自动分配） |
| fileType    | string  | 否  | 文件类型扩展名         |

**请求示例**：

```json
{
  "identifier": "d41d8cd98f00b204e9800998ecf8427e",
  "fileName": "large-video.mp4",
  "fileSize": 104857600,
  "partNum": 100,
  "partSize": 1048576,
  "contentType": "video/mp4"
}
```

#### 响应数据

**类型**：[FileChunkVo](#filechunkvo-分片信息)

**示例**：

```json
{
  "code": 200,
  "message": "初始化成功",
  "data": {
    "uploadUrl": {
      "1": "https://oss.example.com/video.mp4?partNumber=1&uploadId=123&signature=abc",
      "2": "https://oss.example.com/video.mp4?partNumber=2&uploadId=123&signature=def",
      "3": "https://oss.example.com/video.mp4?partNumber=3&uploadId=123&signature=ghi"
    },
    "uploadId": "12345678-1234-1234-1234-123456789abc"
  }
}
```

#### 使用流程

1. 客户端调用此接口获取分片上传 URL
2. 使用返回的 `uploadUrl` 并发上传所有分片（HTTP PUT）
3. 所有分片上传完成后，调用合并分片接口

#### 请求示例

```bash
# curl
curl -X POST "https://api.example.com/api/file/multipart/init" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "identifier": "d41d8cd98f00b204e9800998ecf8427e",
    "fileName": "large-video.mp4",
    "fileSize": 104857600,
    "partNum": 100,
    "partSize": 1048576,
    "contentType": "video/mp4"
  }'

# JavaScript (fetch)
fetch('https://api.example.com/api/file/multipart/init', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer YOUR_TOKEN'
  },
  body: JSON.stringify({
    identifier: 'd41d8cd98f00b204e9800998ecf8427e',
    fileName: 'large-video.mp4',
    fileSize: 104857600,
    partNum: 100,
    partSize: 1048576,
    contentType: 'video/mp4'
  })
})
.then(response => response.json())
.then(data => {
  data.data.uploadUrl.forEach((partNumber, url) => {
    uploadPart(partNumber, url);
  });
});
```

---

### 3. 合并分片

完成分片上传后，将所有分片合并为一个完整文件。

#### 接口信息

- **接口路径**：`/api/file/multipart/merge`
- **请求方法**：`GET`
- **接口描述**：合并分片上传任务
- **是否需要认证**：是

#### 请求参数

| 参数名        | 类型     | 位置    | 必填 | 说明        |
|------------|--------|-------|----|-----------|
| identifier | string | query | 是  | 文件的 MD5 值 |

#### 响应数据

**类型**：[FileVo](#filevo-文件信息)

**示例**：

```json
{
  "code": 200,
  "message": "合并成功",
  "data": {
    "identifier": "d41d8cd98f00b204e9800998ecf8427e",
    "name": "large-video.mp4",
    "size": 104857600,
    "type": "video/mp4",
    "path": "https://cdn.example.com/video/large-video.mp4"
  }
}
```

#### 注意事项

- 所有分片必须上传完成才能合并
- 合并操作会校验文件的 MD5 值
- 合并成功后，分片上传 ID 失效

#### 请求示例

```bash
# curl
curl -X GET "https://api.example.com/api/file/multipart/merge?identifier=d41d8cd98f00b204e9800998ecf8427e" \
  -H "Authorization: Bearer YOUR_TOKEN"

# JavaScript (fetch)
fetch('https://api.example.com/api/file/multipart/merge?identifier=d41d8cd98f00b204e9800998ecf8427e', {
  headers: {
    'Authorization': 'Bearer YOUR_TOKEN'
  }
})
.then(response => response.json())
.then(data => console.log('文件地址:', data.data.path));
```

---

### 4. 判断文件是否存在

检查文件是否已存在，实现秒传功能。

#### 接口信息

- **接口路径**：`/api/file/multipart/isExits`
- **请求方法**：`GET`
- **接口描述**：检查文件是否存在
- **是否需要认证**：是

#### 请求参数

| 参数名        | 类型     | 位置    | 必填 | 说明        |
|------------|--------|-------|----|-----------|
| identifier | string | query | 是  | 文件的 MD5 值 |

#### 响应数据

**类型**：[FileVo](#filevo-文件信息)

**成功响应（文件存在）**：

```json
{
  "code": 200,
  "message": "文件已存在",
  "data": {
    "identifier": "d41d8cd98f00b204e9800998ecf8427e",
    "name": "photo.jpg",
    "size": 2048576,
    "type": "image/jpeg",
    "path": "https://cdn.example.com/images/photo.jpg"
  }
}
```

**文件不存在**：

```json
{
  "code": 404,
  "message": "文件不存在",
  "data": null
}
```

#### 请求示例

```bash
# curl
curl -X GET "https://api.example.com/api/file/multipart/isExits?identifier=d41d8cd98f00b204e9800998ecf8427e" \
  -H "Authorization: Bearer YOUR_TOKEN"

# JavaScript (fetch)
fetch('https://api.example.com/api/file/multipart/isExits?identifier=d41d8cd98f00b204e9800998ecf8427e', {
  headers: {
    'Authorization': 'Bearer YOUR_TOKEN'
  }
})
.then(response => {
  if (response.ok) {
    return response.json();
  } else {
    throw new Error('文件不存在，需要上传');
  }
})
.then(data => {
  console.log('文件已存在，秒传:', data.data.path);
});
```

---

### 5. 上传文件

上传普通文件（不分片）。

#### 接口信息

- **接口路径**：`/api/file/upload`
- **请求方法**：`POST`
- **接口描述**：上传文件
- **是否需要认证**：是
- **Content-Type**：`multipart/form-data`

#### 请求参数

| 参数名        | 类型     | 位置        | 必填 | 说明        |
|------------|--------|-----------|----|-----------|
| identifier | string | query     | 是  | 文件的 MD5 值 |
| file       | file   | form-data | 是  | 文件内容      |

#### 响应数据

**类型**：[FileVo](#filevo-文件信息)

**示例**：

```json
{
  "code": 200,
  "message": "上传成功",
  "data": {
    "identifier": "d41d8cd98f00b204e9800998ecf8427e",
    "name": "document.pdf",
    "size": 2048576,
    "type": "application/pdf",
    "path": "https://cdn.example.com/docs/document.pdf"
  }
}
```

#### 文件大小限制

| 用户类型   | 限制    |
|--------|-------|
| 普通用户   | 100MB |
| VIP 用户 | 500MB |
| 企业用户   | 2GB   |

#### 请求示例

```bash
# curl
curl -X POST "https://api.example.com/api/file/upload?identifier=d41d8cd98f00b204e9800998ecf8427e" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@/path/to/document.pdf"

# JavaScript (FormData)
const formData = new FormData();
formData.append('file', fileInput.files[0]);

fetch(`https://api.example.com/api/file/upload?identifier=${md5Value}`, {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer YOUR_TOKEN'
  },
  body: formData
})
.then(response => response.json())
.then(data => console.log('上传成功:', data.data.path));
```

---

### 6. 文件下载

下载文件，支持断点续传。

#### 接口信息

- **接口路径**：`/api/file/download`
- **请求方法**：`GET`
- **接口描述**：下载文件
- **是否需要认证**：是

#### 请求参数

| 参数名        | 类型     | 位置     | 必填 | 说明         |
|------------|--------|--------|----|------------|
| identifier | string | query  | 是  | 文件的 MD5 值  |
| Range      | string | header | 否  | 下载范围（断点续传） |

#### Range 头格式

```
Range: bytes=0-1023          # 下载前 1KB
Range: bytes=1024-2047       # 下载第 2KB
Range: bytes=0-              # 从 0 开始下载到结尾
Range: bytes=-512            # 下载最后 512 字节
```

#### 响应数据

**类型**：二进制流（`application/octet-stream`）

**响应头**：

| 头名称                 | 说明         | 示例                                    |
|---------------------|------------|---------------------------------------|
| Content-Type        | 文件 MIME 类型 | `application/pdf`                     |
| Content-Length      | 文件大小       | `2048576`                             |
| Content-Disposition | 下载文件名      | `attachment; filename="document.pdf"` |
| Accept-Ranges       | 支持断点续传     | `bytes`                               |
| Content-Range       | 当前返回的范围    | `bytes 0-1023/2048576`                |

**HTTP 状态码**：

| 状态码                       | 说明         |
|---------------------------|------------|
| 200 OK                    | 完整文件下载     |
| 206 Partial Content       | 部分内容（断点续传） |
| 404 Not Found             | 文件不存在      |
| 416 Range Not Satisfiable | 范围无效       |

#### 请求示例

```bash
# 完整下载
curl -X GET "https://api.example.com/api/file/download?identifier=d41d8cd98f00b204e9800998ecf8427e" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -O -J

# 断点续传（从 1MB 位置继续）
curl -X GET "https://api.example.com/api/file/download?identifier=d41d8cd98f00b204e9800998ecf8427e" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Range: bytes=1048576-" \
  -O -J

# JavaScript (fetch，支持断点续传)
async function downloadWithResume(identifier, startByte = 0) {
  const headers = {
    'Authorization': 'Bearer YOUR_TOKEN'
  };

  if (startByte > 0) {
    headers['Range'] = `bytes=${startByte}-`;
  }

  const response = await fetch(`https://api.example.com/api/file/download?identifier=${identifier}`, {
    headers
  });

  if (response.status === 206) {
    // 部分内容（断点续传）
    const contentRange = response.headers.get('Content-Range');
    console.log('续传范围:', contentRange);
  }

  const blob = await response.blob();
  return blob;
}
```

---

### 7. 获取文件 MD5

计算并返回文件的 MD5 值。

#### 接口信息

- **接口路径**：`/api/file/md5`
- **请求方法**：`GET`
- **接口描述**：获取文件 MD5 值
- **是否需要认证**：是
- **Content-Type**：`multipart/form-data`

#### 请求参数

| 参数名  | 类型   | 位置        | 必填 | 说明   |
|------|------|-----------|----|------|
| file | file | form-data | 是  | 文件内容 |

#### 响应数据

**类型**：[FileVo](#filevo-文件信息)

**示例**：

```json
{
  "code": 200,
  "message": "计算成功",
  "data": {
    "identifier": "d41d8cd98f00b204e9800998ecf8427e",
    "name": "photo.jpg",
    "size": 2048576,
    "type": null,
    "path": null
  }
}
```

#### 注意事项

- 此接口仅计算 MD5，不保存文件
- 适用于客户端在分片上传前计算文件哈希值
- 大文件计算可能需要较长时间

#### 请求示例

```bash
# curl
curl -X GET "https://api.example.com/api/file/md5" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@/path/to/photo.jpg"

# JavaScript (FormData)
const formData = new FormData();
formData.append('file', fileInput.files[0]);

fetch('https://api.example.com/api/file/md5', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer YOUR_TOKEN'
  },
  body: formData
})
.then(response => response.json())
.then(data => {
  console.log('文件 MD5:', data.data.identifier);
  console.log('文件大小:', data.data.size);
});
```

---

## 媒体管理 API

### 8. 上传图片

上传图片文件，支持自动压缩和缩略图生成。

#### 接口信息

- **接口路径**：`/api/media/image/upload`
- **请求方法**：`POST`
- **接口描述**：上传图片文件
- **是否需要认证**：是
- **Content-Type**：`multipart/form-data`

#### 请求参数

| 参数名        | 类型     | 位置        | 必填 | 说明        |
|------------|--------|-----------|----|-----------|
| identifier | string | query     | 是  | 文件的 MD5 值 |
| file       | file   | form-data | 是  | 图片文件      |

#### 支持的图片格式

| 格式   | MIME 类型         |
|------|-----------------|
| JPEG | `image/jpeg`    |
| PNG  | `image/png`     |
| GIF  | `image/gif`     |
| WebP | `image/webp`    |
| BMP  | `image/bmp`     |
| SVG  | `image/svg+xml` |
| TIFF | `image/tiff`    |

#### 响应数据

**类型**：[FileVo](#filevo-文件信息)

**示例**：

```json
{
  "code": 200,
  "message": "上传成功",
  "data": {
    "identifier": "d41d8cd98f00b204e9800998ecf8427e",
    "name": "photo.jpg",
    "size": 2048576,
    "type": "image/jpeg",
    "path": "https://cdn.example.com/images/photo.jpg",
    "thumbnailPath": "https://cdn.example.com/thumbnails/photo_200x200.jpg"
  }
}
```

#### 图片处理

上传的图片会自动进行以下处理：

1. **格式转换**：非 JPEG/PNG 格式转换为 JPEG
2. **压缩**：自动压缩至合适大小（最大 2MB）
3. **缩略图**：自动生成 200x200 缩略图
4. **元数据清除**：清除 EXIF 等敏感信息

#### 请求示例

```bash
# curl
curl -X POST "https://api.example.com/api/media/image/upload?identifier=d41d8cd98f00b204e9800998ecf8427e" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@/path/to/photo.jpg"

# JavaScript (FormData)
const formData = new FormData();
formData.append('file', fileInput.files[0]);

fetch(`https://api.example.com/api/media/image/upload?identifier=${md5Value}`, {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer YOUR_TOKEN'
  },
  body: formData
})
.then(response => response.json())
.then(data => {
  console.log('原图:', data.data.path);
  console.log('缩略图:', data.data.thumbnailPath);
});
```

---

### 9. 上传头像

上传用户头像，支持自动裁剪和压缩。

#### 接口信息

- **接口路径**：`/api/media/avatar/upload`
- **请求方法**：`POST`
- **接口描述**：上传头像文件
- **是否需要认证**：是
- **Content-Type**：`multipart/form-data`

#### 请求参数

| 参数名        | 类型     | 位置        | 必填 | 说明        |
|------------|--------|-----------|----|-----------|
| identifier | string | query     | 是  | 文件的 MD5 值 |
| file       | file   | form-data | 是  | 头像文件      |

#### 头像规格

| 属性 | 要求              |
|----|-----------------|
| 尺寸 | 正方形（推荐 500x500） |
| 格式 | JPEG / PNG      |
| 大小 | 最大 5MB          |
| 内容 | 不得包含违规内容        |

#### 响应数据

**类型**：[FileVo](#filevo-文件信息)

**示例**：

```json
{
  "code": 200,
  "message": "上传成功",
  "data": {
    "identifier": "d41d8cd98f00b204e9800998ecf8427e",
    "name": "avatar.jpg",
    "size": 102400,
    "type": "image/jpeg",
    "path": "https://cdn.example.com/avatars/user123/avatar.jpg",
    "thumbnailPath": "https://cdn.example.com/avatars/user123/avatar_100x100.jpg"
  }
}
```

#### 头像处理

1. **居中裁剪**：自动裁剪为正方形
2. **多尺寸生成**：
    - 原图
    - 大图：200x200
    - 中图：100x100
    - 小图：50x50
3. **质量优化**：压缩至合适大小
4. **CDN 缓存**：自动上传至 CDN 加速

#### 请求示例

```bash
# curl
curl -X POST "https://api.example.com/api/media/avatar/upload?identifier=d41d8cd98f00b204e9800998ecf8427e" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@/path/to/avatar.jpg"

# JavaScript (FormData)
const formData = new FormData();
formData.append('file', avatarFileInput.files[0]);

fetch(`https://api.example.com/api/media/avatar/upload?identifier=${md5Value}`, {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer YOUR_TOKEN'
  },
  body: formData
})
.then(response => response.json())
.then(data => {
  // 更新用户头像
  updateUserAvatar(data.data.path);
});
```

---

## 数据模型

### FileVo - 文件信息

文件上传或查询后返回的文件信息。

| 字段            | 类型     | 说明             |
|---------------|--------|----------------|
| identifier    | string | 文件 MD5 值（唯一标识） |
| name          | string | 文件名称           |
| size          | long   | 文件大小（字节）       |
| type          | string | 文件类型（MIME 类型）  |
| path          | string | 文件访问地址（完整 URL） |
| thumbnailPath | string | 缩略图地址（图片类文件）   |

**示例**：

```json
{
  "identifier": "d41d8cd98f00b204e9800998ecf8427e",
  "name": "photo.jpg",
  "size": 2048576,
  "type": "image/jpeg",
  "path": "https://cdn.example.com/images/photo.jpg",
  "thumbnailPath": "https://cdn.example.com/thumbnails/photo_200x200.jpg"
}
```

---

### FileChunkVo - 分片信息

分片上传初始化后返回的上传地址和 ID。

| 字段        | 类型                    | 说明                          |
|-----------|-----------------------|-----------------------------|
| uploadUrl | map\<string, string\> | 分片上传 URL Map（分片号 -> 上传 URL） |
| uploadId  | string                | 分片上传唯一标识                    |

**uploadUrl 说明**：

- **Key**：分片号（从 1 开始）
- **Value**：该分片的预签名上传 URL

**示例**：

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

### FileUploadProgressVo - 上传进度

查询文件上传进度时返回的详细信息。

| 字段             | 类型                    | 说明                      |
|----------------|-----------------------|-------------------------|
| isNew          | integer               | 是否为新文件（1=是，0=否）         |
| isFinish       | integer               | 是否已完成上传（1=是，0=否）        |
| path           | string                | 文件访问地址（已完成时返回）          |
| uploadId       | string                | 上传任务 ID（进行中时返回）         |
| undoneChunkMap | map\<string, string\> | 未完成上传的分片（分片号 -> 上传 URL） |

**示例**：

```json
{
  "isNew": 0,
  "isFinish": 0,
  "path": null,
  "uploadId": "12345678-1234-1234-1234-123456789abc",
  "undoneChunkMap": {
    "3": "https://oss.example.com/upload?partNumber=3&uploadId=xxx",
    "5": "https://oss.example.com/upload?partNumber=5&uploadId=xxx"
  }
}
```

**状态说明**：

| isNew | isFinish | path | undoneChunkMap | 说明             |
|-------|----------|------|----------------|----------------|
| 1     | 0        | null | 全部分片           | 首次上传           |
| 0     | 0        | null | 部分分片           | 部分上传，需继续上传剩余分片 |
| 0     | 1        | URL  | null           | 已完成，可直接使用 path |

---

### OssFileDto - 文件上传信息

文件上传时的核心数据结构。

| 字段          | 类型      | 必填    | 说明              |
|-------------|---------|-------|-----------------|
| identifier  | string  | **是** | 文件唯一标识（MD5）     |
| fileName    | string  | **是** | 文件名             |
| fileSize    | long    | 否     | 文件大小（字节）        |
| partNum     | integer | **是** | 分片数量（最小值：1）     |
| partSize    | long    | 否     | 每个分片大小（字节）      |
| contentType | string  | 否     | 文件 MIME 类型      |
| bucketName  | string  | 否     | 存储桶名称（不指定则自动分配） |
| fileType    | string  | 否     | 文件类型扩展名         |
| objectKey   | string  | 否     | 文件在 OSS 中的对象键   |
| uploadId    | string  | 否     | 分片上传的 uploadId  |

**示例**：

```json
{
  "identifier": "d41d8cd98f00b204e9800998ecf8427e",
  "fileName": "large-video.mp4",
  "fileSize": 104857600,
  "partNum": 100,
  "partSize": 1048576,
  "contentType": "video/mp4"
}
```

---

### OssFileMediaInfoDto - 媒体处理参数

图片处理操作的参数配置。

| 字段                | 类型      | 默认值          | 说明           |
|-------------------|---------|--------------|--------------|
| width             | integer | null         | 目标宽度（像素）     |
| height            | integer | null         | 目标高度（像素）     |
| watermarkPath     | string  | null         | 水印图片地址       |
| watermarkPosition | string  | BOTTOM_RIGHT | 水印位置         |
| opacity           | float   | 0.5          | 透明度（0.0-1.0） |
| scale             | double  | 0.5          | 水印放大倍数       |
| ratio             | double  | 0.3          | 水印占图片比例      |
| format            | string  | png          | 输出格式         |

**watermarkPosition 可选值**：

- `TOP_LEFT`：左上角
- `TOP_CENTER`：顶部居中
- `TOP_RIGHT`：右上角
- `CENTER`：正中心
- `BOTTOM_LEFT`：左下角
- `BOTTOM_CENTER`：底边居中
- `BOTTOM_RIGHT`：右下角

**示例**：

```json
{
  "width": 800,
  "height": 600,
  "watermarkPath": "/watermarks/logo.png",
  "watermarkPosition": "BOTTOM_RIGHT",
  "opacity": 0.5,
  "ratio": 0.3,
  "format": "png"
}
```

---

### FileDownloadRangeDto - 下载范围

支持断点续传和分片下载的参数定义。

| 字段       | 类型   | 说明        |
|----------|------|-----------|
| start    | long | 开始位置（字节）  |
| end      | long | 结束位置（字节）  |
| fileSize | long | 文件总大小（字节） |

**示例**：

```json
{
  "start": 0,
  "end": 1048575,
  "fileSize": 10485760
}
```

---

## 错误码参考

### 客户端错误（4xx）

| 错误码 | 错误信息                   | 说明        | 解决方案               |
|-----|------------------------|-----------|--------------------|
| 400 | INVALID_PARAMETER      | 请求参数不合法   | 检查请求参数格式和必填项       |
| 400 | INVALID_FILE_TYPE      | 不支持的文件类型  | 检查文件扩展名是否在允许列表中    |
| 400 | INVALID_MD5            | MD5 值格式错误 | 确保是 32 位十六进制字符串    |
| 404 | FILE_NOT_FOUND         | 文件不存在     | 检查 identifier 是否正确 |
| 409 | FILE_CONFLICT          | 文件已存在     | 使用秒传功能或更换文件        |
| 413 | FILE_TOO_LARGE         | 文件大小超过限制  | 压缩文件或使用分片上传        |
| 415 | UNSUPPORTED_MEDIA_TYPE | 不支持的媒体类型  | 转换文件格式             |

### 服务器错误（5xx）

| 错误码 | 错误信息                | 说明      | 解决方案        |
|-----|---------------------|---------|-------------|
| 500 | INTERNAL_ERROR      | 服务器内部错误 | 联系管理员或稍后重试  |
| 503 | SERVICE_UNAVAILABLE | 服务暂时不可用 | 稍后重试        |
| 504 | GATEWAY_TIMEOUT     | 上传超时    | 检查网络或减小分片大小 |

---

## 使用场景示例

### 场景 1：小文件直接上传

适用于小于 5MB 的文件，一次性上传完成。

```javascript
async function uploadSmallFile(file) {
    // 1. 计算文件 MD5
    const md5 = await calculateMD5(file);

    // 2. 检查文件是否已存在（秒传）
    const existResponse = await fetch(`/api/file/multipart/isExits?identifier=${md5}`, {
        headers: {'Authorization': 'Bearer TOKEN'}
    });

    if (existResponse.ok) {
        const existData = await existResponse.json();
        console.log('秒传成功:', existData.data.path);
        return existData.data;
    }

    // 3. 上传文件
    const formData = new FormData();
    formData.append('file', file);

    const uploadResponse = await fetch(`/api/file/upload?identifier=${md5}`, {
        method: 'POST',
        headers: {'Authorization': 'Bearer TOKEN'},
        body: formData
    });

    const uploadData = await uploadResponse.json();
    console.log('上传成功:', uploadData.data.path);
    return uploadData.data;
}
```

---

### 场景 2：大文件分片上传

适用于大于 5MB 的文件，支持断点续传。

```javascript
async function uploadLargeFile(file) {
    const CHUNK_SIZE = 5 * 1024 * 1024; // 5MB
    const md5 = await calculateMD5(file);
    const totalChunks = Math.ceil(file.size / CHUNK_SIZE);

    // 1. 检查上传进度
    const progressResponse = await fetch(`/api/file/multipart/check?identifier=${md5}`, {
        headers: {'Authorization': 'Bearer TOKEN'}
    });
    const progressData = await progressResponse.json();

    if (progressData.data.isFinish === 1) {
        console.log('文件已上传:', progressData.data.path);
        return progressData.data;
    }

    // 2. 初始化分片上传（如果是首次上传）
    let uploadId = progressData.data.uploadId;
    let undoneChunks = progressData.data.undoneChunkMap;

    if (!uploadId && !undoneChunks) {
        const initResponse = await fetch('/api/file/multipart/init', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer TOKEN'
            },
            body: JSON.stringify({
                identifier: md5,
                fileName: file.name,
                fileSize: file.size,
                partNum: totalChunks,
                partSize: CHUNK_SIZE,
                contentType: file.type
            })
        });
        const initData = await initResponse.json();
        uploadId = initData.data.uploadId;
        undoneChunks = initData.data.uploadUrl;
    }

    // 3. 并发上传分片
    const uploadPromises = Object.entries(undoneChunks).map(([partNumber, url]) => {
        const start = (partNumber - 1) * CHUNK_SIZE;
        const end = Math.min(start + CHUNK_SIZE, file.size);
        const chunk = file.slice(start, end);

        return fetch(url, {
            method: 'PUT',
            body: chunk
        });
    });

    await Promise.all(uploadPromises);

    // 4. 合并分片
    const mergeResponse = await fetch(`/api/file/multipart/merge?identifier=${md5}`, {
        headers: {'Authorization': 'Bearer TOKEN'}
    });
    const mergeData = await mergeResponse.json();

    console.log('上传成功:', mergeData.data.path);
    return mergeData.data;
}
```

---

### 场景 3：断点续传下载

支持网络中断后继续下载。

```javascript
async function downloadWithResume(identifier, savePath) {
    let downloadedBytes = 0;

    // 检查本地是否有未完成的下载
    const localFile = await checkLocalFile(savePath);
    if (localFile) {
        downloadedBytes = localFile.size;
    }

    // 设置 Range 请求头
    const headers = {
        'Authorization': 'Bearer TOKEN'
    };
    if (downloadedBytes > 0) {
        headers['Range'] = `bytes=${downloadedBytes}-`;
    }

    // 发起下载请求
    const response = await fetch(`/api/file/download?identifier=${identifier}`, {
        headers
    });

    if (response.status === 416) {
        // 范围无效，可能是本地文件已完整
        console.log('文件已完整下载');
        return;
    }

    // 获取文件总大小
    const contentRange = response.headers.get('Content-Range');
    const totalSize = parseInt(contentRange.split('/')[1]);

    // 保存文件
    const blob = await response.blob();
    await appendToFile(savePath, blob);

    console.log(`下载进度: ${Math.min(downloadedBytes + blob.size, totalSize) / totalSize * 100}%`);
}
```

---

## 附录

### 文件类型对照表

| 扩展名         | MIME 类型                                                                 | 存储桶      |
|-------------|-------------------------------------------------------------------------|----------|
| .jpg, .jpeg | image/jpeg                                                              | image    |
| .png        | image/png                                                               | image    |
| .gif        | image/gif                                                               | image    |
| .pdf        | application/pdf                                                         | document |
| .doc        | application/msword                                                      | document |
| .docx       | application/vnd.openxmlformats-officedocument.wordprocessingml.document | document |
| .xls        | application/vnd.ms-excel                                                | document |
| .xlsx       | application/vnd.openxmlformats-officedocument.spreadsheetml.sheet       | document |
| .mp4        | video/mp4                                                               | video    |
| .mp3        | audio/mpeg                                                              | audio    |
| .zip        | application/zip                                                         | package  |

### 常用 HTTP 状态码

| 状态码 | 说明                    | 使用场景       |
|-----|-----------------------|------------|
| 200 | OK                    | 请求成功       |
| 206 | Partial Content       | 断点续传       |
| 400 | Bad Request           | 参数错误       |
| 404 | Not Found             | 文件不存在      |
| 416 | Range Not Satisfiable | Range 范围无效 |
| 500 | Internal Server Error | 服务器错误      |

---

## 更新日志

### v1.0.0 (2025-02-06)

- 初始版本发布
- 完整的文件管理 API
- 分片上传和断点续传
- 媒体处理 API
- 完善的错误处理和状态码
