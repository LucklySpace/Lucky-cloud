# im-platform-rpc-api 接口文档

## 目录

- [1. 短链服务（ShortLinkDubboService）](#1-短链服务shortlinkdubboservice)
    - [1.1 创建短链](#11-创建短链)
    - [1.2 短链重定向](#12-短链重定向)
    - [1.3 禁用短链](#13-禁用短链)
    - [1.4 查询短链信息](#14-查询短链信息)
- [2. 地址服务（AddressDubboService）](#2-地址服务addressdubboservice)
    - [2.1 根据 IP 查询地区信息](#21-根据-ip-查询地区信息)
    - [2.2 根据 IP 查询地区编号](#22-根据-ip-查询地区编号)
    - [2.3 根据地区编号查询节点](#23-根据地区编号查询节点)
    - [2.4 解析区域路径](#24-解析区域路径)
    - [2.5 格式化地区编号为路径字符串](#25-格式化地区编号为路径字符串)
- [3. 表情服务（EmojiDubboService）](#3-表情服务emojidubboservice)
    - [3.1 创建或更新表情包](#31-创建或更新表情包)
    - [3.2 列出所有表情包](#32-列出所有表情包)
    - [3.3 上传表情图片](#33-上传表情图片)
    - [3.4 列出表情包中的所有表情](#34-列出表情包中的所有表情)
    - [3.5 上传表情包封面图](#35-上传表情包封面图)
    - [3.6 启用/禁用表情包](#36-启用禁用表情包)
    - [3.7 查询表情包详情](#37-查询表情包详情)
    - [3.8 删除表情](#38-删除表情)
    - [3.9 生成表情包编码](#39-生成表情包编码)
- [4. 通知服务（NotifyDubboService）](#4-通知服务notifydubboservice)
    - [4.1 发送邮件](#41-发送邮件)
    - [4.2 发送短信](#42-发送短信)

---

## 1. 短链服务（ShortLinkDubboService）

短链服务提供短链创建、重定向、禁用和查询功能，适用于社交媒体分享、营销活动、链接统计等场景。

### 1.1 创建短链

**接口说明**：根据原始 URL 创建短链接，支持设置过期时间，同一 URL 重复调用会返回已有的短码。

**方法签名**：

```java
ShortLinkVo createShortLink(ShortLinkDto request)
```

**请求参数**：

| 参数名           | 类型     | 必填 | 说明                | 示例值                              |
|---------------|--------|----|-------------------|----------------------------------|
| originalUrl   | String | 是  | 原始 URL，最长 2048 字符 | `"https://example.com/docs/123"` |
| expireSeconds | Long   | 否  | 过期时间（秒），不设置表示永久有效 | `86400`（24小时）                    |

**返回参数**：

| 参数名           | 类型            | 说明       |
|---------------|---------------|----------|
| originalUrl   | String        | 原始 URL   |
| expireSeconds | Long          | 过期时间（秒）  |
| shortCode     | String        | 短码       |
| shortUrl      | String        | 完整短链 URL |
| visitCount    | Integer       | 访问次数     |
| expireTime    | LocalDateTime | 过期时间     |
| enabled       | Boolean       | 是否启用     |

**请求示例**：

```java
// 创建一个 24 小时有效的短链
ShortLinkDto request = ShortLinkDto.builder()
    .originalUrl("https://example.com/very-long-url/path/to/resource")
    .expireSeconds(86400L)  // 24小时
    .build();

ShortLinkVo result = shortLinkService.createShortLink(request);
```

**返回示例**：

```json
{
  "originalUrl": "https://example.com/very-long-url/path/to/resource",
  "expireSeconds": 86400,
  "shortCode": "a1B2c3",
  "shortUrl": "https://s.example.com/api/v1/short/r/a1B2c3",
  "visitCount": 0,
  "expireTime": "2025-02-07T10:30:00",
  "enabled": true
}
```

**注意事项**：

- 如果同一 URL 已存在且未过期，会返回已有的短码，不会重复创建
- 原始 URL 最大长度为 2048 字符
- 过期时间为可选参数，不设置则永久有效

---

### 1.2 短链重定向

**接口说明**：根据短码获取原始 URL，用于实现短链重定向功能。

**方法签名**：

```java
ShortLinkVo redirect(ShortLinkRedirectDto request)
```

**请求参数**：

| 参数名       | 类型     | 必填 | 说明          | 示例值        |
|-----------|--------|----|-------------|------------|
| shortCode | String | 是  | 短码，最长 64 字符 | `"a1B2c3"` |

**返回参数**：同创建短链的返回参数

**请求示例**：

```java
ShortLinkRedirectDto request = ShortLinkRedirectDto.builder()
    .shortCode("a1B2c3")
    .build();

ShortLinkVo result = shortLinkService.redirect(request);
System.out.println("原始 URL：" + result.getOriginalUrl());
```

**返回示例**：

```json
{
  "originalUrl": "https://example.com/very-long-url/path/to/resource",
  "shortCode": "a1B2c3",
  "shortUrl": "https://s.example.com/api/v1/short/r/a1B2c3",
  "visitCount": 10,
  "enabled": true
}
```

**异常情况**：

- 短码不存在：返回 404
- 短链已过期或已禁用：返回 410

---

### 1.3 禁用短链

**接口说明**：禁用指定的短链，禁用后无法再通过短码访问原始 URL。

**方法签名**：

```java
void disable(String shortCode)
```

**请求参数**：

| 参数名       | 类型     | 必填 | 说明 | 示例值        |
|-----------|--------|----|----|------------|
| shortCode | String | 是  | 短码 | `"a1B2c3"` |

**请求示例**：

```java
shortLinkService.disable("a1B2c3");
```

**注意事项**：

- 禁用操作不可逆，请谨慎使用
- 禁用后的短链调用重定向接口会返回 410 状态

---

### 1.4 查询短链信息

**接口说明**：根据短码查询短链的详细信息，不增加访问次数。

**方法签名**：

```java
ShortLinkVo info(String shortCode)
```

**请求参数**：

| 参数名       | 类型     | 必填 | 说明 | 示例值        |
|-----------|--------|----|----|------------|
| shortCode | String | 是  | 短码 | `"a1B2c3"` |

**返回参数**：同创建短链的返回参数

**请求示例**：

```java
ShortLinkVo result = shortLinkService.info("a1B2c3");
System.out.println("短链信息：");
System.out.println("  短码：" + result.getShortCode());
System.out.println("  原始 URL：" + result.getOriginalUrl());
System.out.println("  访问次数：" + result.getVisitCount());
System.out.println("  是否启用：" + result.getEnabled());
```

**返回示例**：

```json
{
  "originalUrl": "https://example.com/very-long-url/path/to/resource",
  "expireSeconds": 86400,
  "shortCode": "a1B2c3",
  "shortUrl": "https://s.example.com/api/v1/short/r/a1B2c3",
  "visitCount": 25,
  "expireTime": "2025-02-07T10:30:00",
  "enabled": true
}
```

---

## 2. 地址服务（AddressDubboService）

地址服务提供 IP 地址查询、地区信息解析等功能，支持根据 IP 定位、地区编号查询、地区路径格式化等操作。

### 2.1 根据 IP 查询地区信息

**接口说明**：根据 IPv4 地址查询对应的地区信息，包括完整的地区路径。

**方法签名**：

```java
AreaVo getAreaByIp(String ip)
```

**请求参数**：

| 参数名 | 类型     | 必填 | 说明      | 示例值         |
|-----|--------|----|---------|-------------|
| ip  | String | 是  | IPv4 地址 | `"8.8.8.8"` |

**返回参数**：

| 参数名     | 类型      | 说明        |
|---------|---------|-----------|
| code    | Integer | 区域编号      |
| ip      | String  | IP 地址     |
| name    | String  | 区域名称      |
| address | String  | 格式化后的完整路径 |

**请求示例**：

```java
AreaVo area = addressService.getAreaByIp("8.8.8.8");
System.out.println("地区：" + area.getAddress());
```

**返回示例**：

```json
{
  "code": 110101,
  "ip": "8.8.8.8",
  "name": "东城区",
  "address": "中国/北京/北京市/东城区"
}
```

---

### 2.2 根据 IP 查询地区编号

**接口说明**：根据 IPv4 地址查询对应的地区编号，适用于需要快速获取地区 ID 的场景。

**方法签名**：

```java
Integer getAreaIdByIp(String ip)
```

**请求参数**：

| 参数名 | 类型     | 必填 | 说明      | 示例值         |
|-----|--------|----|---------|-------------|
| ip  | String | 是  | IPv4 地址 | `"8.8.8.8"` |

**返回参数**：

| 参数名     | 类型   | 说明         |
|---------|------|------------|
| Integer | 地区编号 | 如 `110101` |

**请求示例**：

```java
Integer areaId = addressService.getAreaIdByIp("8.8.8.8");
System.out.println("地区编号：" + areaId);
```

**返回示例**：

```json
110101
```

---

### 2.3 根据地区编号查询节点

**接口说明**：根据地区编号查询对应的地区节点信息。

**方法签名**：

```java
AreaVo getAreaById(Integer id)
```

**请求参数**：

| 参数名 | 类型      | 必填 | 说明   | 示例值      |
|-----|---------|----|------|----------|
| id  | Integer | 是  | 地区编号 | `110000` |

**返回参数**：同 2.1

**请求示例**：

```java
AreaVo area = addressService.getAreaById(110000);
System.out.println("地区名称：" + area.getName());
System.out.println("完整路径：" + area.getAddress());
```

**返回示例**：

```json
{
  "code": 110000,
  "name": "北京市",
  "address": "中国/北京/北京市"
}
```

---

### 2.4 解析区域路径

**接口说明**：解析区域路径字符串（如：河南省/郑州市/金水区），返回对应的地区信息。

**方法签名**：

```java
AreaVo parseArea(String path)
```

**请求参数**：

| 参数名  | 类型     | 必填 | 说明    | 示例值             |
|------|--------|----|-------|-----------------|
| path | String | 是  | 路径字符串 | `"河南省/郑州市/金水区"` |

**返回参数**：同 2.1

**请求示例**：

```java
AreaVo area = addressService.parseArea("河南省/郑州市/金水区");
System.out.println("地区编号：" + area.getCode());
System.out.println("地区名称：" + area.getName());
```

**返回示例**：

```json
{
  "code": 410105,
  "name": "金水区",
  "address": "中国/河南/郑州市/金水区"
}
```

---

### 2.5 格式化地区编号为路径字符串

**接口说明**：将地区编号格式化为完整的路径字符串，便于展示和存储。

**方法签名**：

```java
String formatArea(Integer id)
```

**请求参数**：

| 参数名 | 类型      | 必填 | 说明   | 示例值      |
|-----|---------|----|------|----------|
| id  | Integer | 是  | 地区编号 | `110101` |

**返回参数**：

| 参数名    | 类型        | 说明                  |
|--------|-----------|---------------------|
| String | 格式化后的完整路径 | 如 `"中国/北京/北京市/东城区"` |

**请求示例**：

```java
String path = addressService.formatArea(110101);
System.out.println("完整路径：" + path);
```

**返回示例**：

```
中国/北京/北京市/东城区
```

---

## 3. 表情服务（EmojiDubboService）

表情服务提供表情包管理、表情图片上传等功能，支持表情包的创建、上传、查询和启用/禁用操作。

### 3.1 创建或更新表情包

**接口说明**：创建新的表情包或更新已存在的表情包信息。

**方法签名**：

```java
EmojiPackVo upsertPack(EmojiPackDto request)
```

**请求参数**：

| 参数名         | 类型      | 必填 | 说明             | 示例值         |
|-------------|---------|----|----------------|-------------|
| code        | String  | 否  | 包编码（唯一）        | `"default"` |
| name        | String  | 是  | 包名称，最长 128 字符  | `"默认表情包"`   |
| description | String  | 否  | 包说明，最长 5000 字符 | `"系统默认表情包"` |
| enabled     | Boolean | 否  | 是否启用           | `true`      |

**返回参数**：

| 参数名         | 类型      | 说明      |
|-------------|---------|---------|
| code        | String  | 包编码（唯一） |
| name        | String  | 包名称     |
| description | String  | 包说明     |
| packId      | String  | 包 ID    |
| url         | String  | 封面图 URL |
| enabled     | Boolean | 是否启用    |

**请求示例**：

```java
EmojiPackDto request = EmojiPackDto.builder()
    .code("default")
    .name("默认表情包")
    .description("系统默认表情包，包含常用表情")
    .enabled(true)
    .build();

EmojiPackVo result = emojiService.upsertPack(request);
System.out.println("表情包 ID：" + result.getPackId());
```

**返回示例**：

```json
{
  "code": "default",
  "name": "默认表情包",
  "description": "系统默认表情包，包含常用表情",
  "packId": "pack_1234567890",
  "url": null,
  "enabled": true
}
```

---

### 3.2 列出所有表情包

**接口说明**：获取所有表情包列表，按热度降序排序。

**方法签名**：

```java
List<EmojiPackVo> listPacks()
```

**返回参数**：表情包列表

**请求示例**：

```java
List<EmojiPackVo> packs = emojiService.listPacks();
packs.forEach(pack -> {
    System.out.println("表情包：" + pack.getName());
    System.out.println("  ID：" + pack.getPackId());
    System.out.println("  启用：" + pack.getEnabled());
});
```

**返回示例**：

```json
[
  {
    "code": "default",
    "name": "默认表情包",
    "description": "系统默认表情包",
    "packId": "pack_001",
    "url": "https://example.com/covers/default.jpg",
    "enabled": true
  },
  {
    "code": "funny",
    "name": "搞笑表情包",
    "description": "搞笑表情集合",
    "packId": "pack_002",
    "url": "https://example.com/covers/funny.jpg",
    "enabled": true
  }
]
```

---

### 3.3 上传表情图片

**接口说明**：上传表情图片到指定表情包，返回表情信息和预签名下载 URL。

**方法签名**：

```java
EmojiVo uploadEmoji(String emojiId)
```

**请求参数**：

| 参数名     | 类型     | 必填 | 说明    | 示例值           |
|---------|--------|----|-------|---------------|
| emojiId | String | 是  | 表情 ID | `"emoji_001"` |

**返回参数**：

| 参数名     | 类型      | 说明          |
|---------|---------|-------------|
| packId  | String  | 所属表情包 ID    |
| name    | String  | 表情名称        |
| tags    | String  | 标签（逗号分隔）    |
| emojiId | String  | 表情 ID       |
| url     | String  | 下载 URL（预签名） |
| sort    | Integer | 顺序          |

**请求示例**：

```java
EmojiVo emoji = emojiService.uploadEmoji("emoji_001");
System.out.println("表情下载 URL：" + emoji.getUrl());
```

**返回示例**：

```json
{
  "packId": "pack_001",
  "name": "smile",
  "tags": "happy,positive",
  "emojiId": "emoji_001",
  "url": "https://example.com/emojis/smile.png?signature=...",
  "sort": 1
}
```

---

### 3.4 列出表情包中的所有表情

**接口说明**：获取指定表情包中的所有表情，按顺序升序排列。

**方法签名**：

```java
List<EmojiVo> listEmojis(String packId)
```

**请求参数**：

| 参数名    | 类型     | 必填 | 说明     | 示例值          |
|--------|--------|----|--------|--------------|
| packId | String | 是  | 表情包 ID | `"pack_001"` |

**返回参数**：表情列表

**请求示例**：

```java
List<EmojiVo> emojis = emojiService.listEmojis("pack_001");
emojis.forEach(emoji -> {
    System.out.println("表情：" + emoji.getName());
    System.out.println("  URL：" + emoji.getUrl());
});
```

**返回示例**：

```json
[
  {
    "packId": "pack_001",
    "name": "smile",
    "tags": "happy,positive",
    "emojiId": "emoji_001",
    "url": "https://example.com/emojis/smile.png",
    "sort": 1
  },
  {
    "packId": "pack_001",
    "name": "laugh",
    "tags": "happy,funny",
    "emojiId": "emoji_002",
    "url": "https://example.com/emojis/laugh.png",
    "sort": 2
  }
]
```

---

### 3.5 上传表情包封面图

**接口说明**：为指定表情包上传封面图。

**方法签名**：

```java
EmojiPackVo uploadCover(String packId, String url)
```

**请求参数**：

| 参数名    | 类型     | 必填 | 说明      | 示例值                                     |
|--------|--------|----|---------|-----------------------------------------|
| packId | String | 是  | 表情包 ID  | `"pack_001"`                            |
| url    | String | 是  | 封面图 URL | `"https://example.com/covers/pack.jpg"` |

**返回参数**：同 3.1

**请求示例**：

```java
EmojiPackVo result = emojiService.uploadCover(
    "pack_001",
    "https://example.com/covers/default.jpg"
);
System.out.println("封面图 URL：" + result.getUrl());
```

---

### 3.6 启用/禁用表情包

**接口说明**：切换表情包的启用状态，禁用后该表情包不会被客户端访问。

**方法签名**：

```java
EmojiPackVo togglePack(String packId, boolean enabled)
```

**请求参数**：

| 参数名     | 类型      | 必填 | 说明     | 示例值              |
|---------|---------|----|--------|------------------|
| packId  | String  | 是  | 表情包 ID | `"pack_001"`     |
| enabled | boolean | 是  | 是否启用   | `true` 或 `false` |

**返回参数**：同 3.1

**请求示例**：

```java
// 启用表情包
EmojiPackVo result = emojiService.togglePack("pack_001", true);

// 禁用表情包
EmojiPackVo result = emojiService.togglePack("pack_001", false);
```

---

### 3.7 查询表情包详情

**接口说明**：查询指定表情包的详细信息，包括所有表情。

**方法签名**：

```java
EmojiRespVo getPackId(String packId)
```

**请求参数**：

| 参数名    | 类型     | 必填 | 说明     | 示例值          |
|--------|--------|----|--------|--------------|
| packId | String | 是  | 表情包 ID | `"pack_001"` |

**返回参数**：

| 参数名         | 类型              | 说明      |
|-------------|-----------------|---------|
| code        | String          | 包编码（唯一） |
| name        | String          | 包名称     |
| description | String          | 包说明     |
| packId      | String          | 包 ID    |
| url         | String          | 封面图 URL |
| enabled     | Boolean         | 是否启用    |
| emojis      | List\<EmojiVo\> | 表情列表    |

**请求示例**：

```java
EmojiRespVo pack = emojiService.getPackId("pack_001");
System.out.println("表情包名称：" + pack.getName());
System.out.println("表情数量：" + pack.getEmojis().size());
```

**返回示例**：

```json
{
  "code": "default",
  "name": "默认表情包",
  "description": "系统默认表情包",
  "packId": "pack_001",
  "url": "https://example.com/covers/default.jpg",
  "enabled": true,
  "emojis": [
    {
      "packId": "pack_001",
      "name": "smile",
      "tags": "happy,positive",
      "emojiId": "emoji_001",
      "url": "https://example.com/emojis/smile.png",
      "sort": 1
    }
  ]
}
```

---

### 3.8 删除表情

**接口说明**：删除指定的表情，可选择是否同时删除对象存储中的文件。

**方法签名**：

```java
void deleteEmoji(String emojiId, boolean removeObject)
```

**请求参数**：

| 参数名          | 类型      | 必填 | 说明             | 示例值           |
|--------------|---------|----|----------------|---------------|
| emojiId      | String  | 是  | 表情 ID          | `"emoji_001"` |
| removeObject | boolean | 是  | 是否同时删除对象存储中的文件 | `true`        |

**请求示例**：

```java
// 删除表情，同时删除对象存储文件
emojiService.deleteEmoji("emoji_001", true);

// 仅删除数据库记录，保留对象存储文件
emojiService.deleteEmoji("emoji_001", false);
```

---

### 3.9 生成表情包编码

**接口说明**：生成唯一的表情包编码，用于创建新表情包时使用。

**方法签名**：

```java
String generatePackCode()
```

**返回参数**：

| 参数名    | 类型    | 说明     |
|--------|-------|--------|
| String | 表情包编码 | 唯一的短编码 |

**请求示例**：

```java
String code = emojiService.generatePackCode();
System.out.println("生成的编码：" + code);
```

**返回示例**：

```
aB3xY9
```

---

## 4. 通知服务（NotifyDubboService）

通知服务提供邮件发送和短信发送功能，支持 HTML 邮件和模板短信。

### 4.1 发送邮件

**接口说明**：发送邮件到指定收件人，支持 HTML 格式。

**方法签名**：

```java
Boolean sendEmail(EmailDto email)
```

**请求参数**：

| 参数名     | 类型      | 必填 | 说明                 | 示例值                        |
|---------|---------|----|--------------------|----------------------------|
| to      | String  | 是  | 收件人邮箱              | `"user@example.com"`       |
| subject | String  | 是  | 主题，最长 256 字符       | `"欢迎使用我们的服务"`              |
| content | String  | 是  | 内容                 | `"<h1>欢迎</h1><p>感谢注册</p>"` |
| html    | Boolean | 否  | 是否 HTML 内容，默认 true | `true`                     |

**返回参数**：

| 参数名     | 类型     | 说明                   |
|---------|--------|----------------------|
| Boolean | 是否发送成功 | `true` 成功，`false` 失败 |

**请求示例**：

```java
// 发送 HTML 邮件
EmailDto email = EmailDto.builder()
    .to("user@example.com")
    .subject("欢迎注册")
    .content("<h1>欢迎</h1><p>感谢您注册我们的服务</p>")
    .html(true)
    .build();

Boolean success = notifyService.sendEmail(email);
if (success) {
    System.out.println("邮件发送成功");
} else {
    System.out.println("邮件发送失败");
}

// 发送纯文本邮件
EmailDto textEmail = EmailDto.builder()
    .to("user@example.com")
    .subject("验证码")
    .content("您的验证码是：123456")
    .html(false)
    .build();
```

**注意事项**：

- 收件人邮箱必须符合邮箱格式规范
- 主题最长 256 字符
- 内容支持纯文本和 HTML 格式
- 如果邮件服务未正确配置，会返回 `false`

---

### 4.2 发送短信

**接口说明**：发送短信到指定手机号，支持模板短信。

**方法签名**：

```java
Boolean sendSms(SmsDto sms)
```

**请求参数**：

| 参数名            | 类型             | 必填 | 说明    | 示例值                |
|----------------|----------------|----|-------|--------------------|
| phone          | String         | 是  | 手机号   | `"+8613800138000"` |
| templateId     | String         | 否  | 模板 ID | `"12345"`          |
| templateParams | List\<String\> | 否  | 模板参数  | `["123456"]`       |

**返回参数**：

| 参数名     | 类型     | 说明                   |
|---------|--------|----------------------|
| Boolean | 是否发送成功 | `true` 成功，`false` 失败 |

**请求示例**：

```java
// 发送模板短信（验证码）
SmsDto sms = SmsDto.builder()
    .phone("+8613800138000")
    .templateId("SMS_12345")
    .templateParams(Arrays.asList("123456", "5"))  // 验证码和有效期
    .build();

Boolean success = notifyService.sendSms(sms);
if (success) {
    System.out.println("短信发送成功");
} else {
    System.out.println("短信发送失败");
}

// 发送简单短信（不支持模板的情况）
SmsDto simpleSms = SmsDto.builder()
    .phone("13800138000")
    .build();
```

**注意事项**：

- 手机号格式：支持国际号码，如 `+8613800138000`，也支持纯数字如 `13800138000`
- 手机号长度：6-18 位数字
- 模板 ID 和模板参数需要根据短信服务商的配置设置
- 如果短信服务未正确配置，会返回 `false`

---

## 附录

### 通用错误码

| 错误码 | 说明        |
|-----|-----------|
| 400 | 参数错误或校验失败 |
| 404 | 资源不存在     |
| 410 | 资源已过期或已禁用 |
| 500 | 服务器内部错误   |

### 枚举类型说明

#### NotifyType（通知类型）

| 值     | 说明   |
|-------|------|
| EMAIL | 邮件通知 |
| SMS   | 短信通知 |

#### NotifyStatus（通知状态）

| 值          | 说明   |
|------------|------|
| SUCCESS（1） | 发送成功 |
| FAILED（0）  | 发送失败 |

### 最佳实践

1. **短链服务**：
    - 对于相同 URL，优先使用已有的短码，避免重复创建
    - 合理设置过期时间，及时清理无效短链
    - 禁用操作不可逆，谨慎使用

2. **地址服务**：
    - 缓存 IP 地址查询结果，减少重复查询
    - 使用地区编号查询比路径解析更高效
    - 定期更新 IP 地址库

3. **表情服务**：
    - 表情包编码建议使用有意义的前缀，便于管理
    - 上传表情前先创建表情包
    - 删除表情时根据需求选择是否删除对象存储文件
    - 定期清理不用的表情包

4. **通知服务**：
    - 邮件和短信发送可能失败，需要做好异常处理
    - 短信使用模板可以节省成本
    - 避免频繁发送通知，防止被标记为垃圾邮件或短信
    - 敏感信息不要通过邮件或短信发送

### 服务配置建议

```yaml
dubbo:
  consumer:
    # 启动时不检查服务提供者
    check: false
    # 超时时间（毫秒）
    timeout: 5000
    # 重试次数
    retries: 2
```

### 相关文档

- [README.md](./README.md)：项目概述和快速开始
- OpenAPI 规范：`src/main/resources/openapi/`
    - shortlink-service.yaml
    - address-service.yaml
    - emoji-service.yaml
    - notify-service.yaml

---

**更新时间**：2025-02-06
**版本**：v1.0.0
