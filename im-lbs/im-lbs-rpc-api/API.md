# im-lbs-rpc-api API 详细文档

## 目录

- [位置服务 API（LocationDubboService）](#位置服务-api-locationdubboservice)
    - [updateLocation - 上报用户位置](#updatelocation---上报用户位置)
    - [searchNearby - 搜索附近用户](#searchnearby---搜索附近用户)
    - [clearInactiveUsers - 清理不活跃用户位置缓存](#clearinactiveusers---清理不活跃用户位置缓存)
- [行政区划服务 API（RegionDubboService）](#行政区划服务-api-regiondubboservice)
    - [getProvinces - 获取所有省份](#getprovinces---获取所有省份)
    - [getCities - 根据省份代码获取城市列表](#getcities---根据省份代码获取城市列表)
    - [getCounties - 根据城市代码获取区县列表](#getcounties---根据城市代码获取区县列表)
    - [getTowns - 根据区县代码获取乡镇列表](#gettowns---根据区县代码获取乡镇列表)
    - [getVillages - 根据乡镇代码获取村列表](#getvillages---根据乡镇代码获取村列表)
    - [findNearestCounty - 查找最近的区县](#findnearestcounty---查找最近的区县)
    - [searchRegions - 模糊搜索行政区划](#searchregions---模糊搜索行政区划)
    - [reverseGeocoding - 逆地理编码](#reversegeocoding---逆地理编码)
- [数据模型详解](#数据模型详解)
    - [请求参数模型（DTO）](#请求参数模型dto)
    - [响应数据模型（VO）](#响应数据模型vo)
- [错误码说明](#错误码说明)
- [调用示例](#调用示例)

---

## 位置服务 API（LocationDubboService）

### 概述

`LocationDubboService` 提供用户位置管理相关的服务，包括位置上报、附近用户搜索等功能。

**服务接口**：`com.xy.lucky.lbs.rpc.api.location.LocationDubboService`

**服务版本**：1.0.0

---

### updateLocation - 上报用户位置

#### 接口描述

上报或更新用户的实时地理位置信息。

#### 方法签名

```java
void updateLocation(String userId, LocationUpdateDto dto);
```

#### 请求参数

| 参数名    | 类型                | 必填 | 位置   | 说明            | 示例值          |
|--------|-------------------|----|------|---------------|--------------|
| userId | String            | 是  | 方法参数 | 用户ID，唯一标识一个用户 | "user123456" |
| dto    | LocationUpdateDto | 是  | 方法参数 | 位置上报请求对象      | 见下方详细说明      |

#### LocationUpdateDto 对象结构

| 字段名       | 类型     | 必填 | 说明               | 示例值        |
|-----------|--------|----|------------------|------------|
| longitude | Double | 是  | 经度，范围 -180 到 180 | 116.397128 |
| latitude  | Double | 是  | 纬度，范围 -90 到 90   | 39.916527  |

#### 返回值

无返回值（void）

**成功情况**：方法正常执行结束

**失败情况**：抛出异常

#### 异常说明

| 异常类型                     | 说明     | 可能原因               |
|--------------------------|--------|--------------------|
| IllegalArgumentException | 参数非法   | userId 为空或经纬度超出范围  |
| RuntimeException         | 服务内部错误 | Redis 连接失败、数据存储失败等 |

#### 使用示例

**Java 调用示例**

```java
@DubboReference
private LocationDubboService locationDubboService;

public void reportUserLocation() {
    String userId = "user123456";

    // 创建位置上报对象
    LocationUpdateDto dto = new LocationUpdateDto();
    dto.setLongitude(116.397128);  // 北京天安门经度
    dto.setLatitude(39.916527);    // 北京天安门纬度

    try {
        // 调用服务
        locationDubboService.updateLocation(userId, dto);
        log.info("位置上报成功：userId={}, lon={}, lat={}",
                 userId, dto.getLongitude(), dto.getLatitude());
    } catch (Exception e) {
        log.error("位置上报失败：userId=" + userId, e);
    }
}
```

**注意事项**

1. **坐标范围**：
    - 经度：-180 到 180
    - 纬度：-90 到 90
    - 超出范围会抛出 IllegalArgumentException

2. **更新频率**：
    - 建议移动距离超过 100 米时才更新
    - 避免过于频繁的上报（如每秒多次）

3. **缓存时间**：
    - 位置信息会缓存到 Redis
    - 缓存时间由服务实现方决定（建议 1 小时）

4. **数据一致性**：
    - 同一用户多次上报会覆盖旧数据
    - 最新上报的数据为当前有效位置

---

### searchNearby - 搜索附近用户

#### 接口描述

根据当前用户位置和指定的搜索条件，查找附近的用户列表，并按距离排序。

#### 方法签名

```java
List<LocationVo> searchNearby(String userId, NearbySearchDto dto);
```

#### 请求参数

| 参数名    | 类型              | 必填 | 位置   | 说明       | 示例值          |
|--------|-----------------|----|------|----------|--------------|
| userId | String          | 是  | 方法参数 | 当前用户ID   | "user123456" |
| dto    | NearbySearchDto | 是  | 方法参数 | 附近搜索请求对象 | 见下方详细说明      |

#### NearbySearchDto 对象结构

| 字段名    | 类型      | 必填 | 默认值    | 说明          | 示例值    |
|--------|---------|----|--------|-------------|--------|
| radius | Double  | 否  | 5000.0 | 搜索半径，单位：米   | 5000.0 |
| limit  | Integer | 否  | 20     | 最大返回结果数     | 20     |
| page   | Integer | 否  | 1      | 分页页码，从 1 开始 | 1      |

**参数说明**：

- **radius**：搜索半径范围
    - 最小值：100 米
    - 最大值：50000 米（50 公里）
    - 超出范围会被自动限制在有效区间内
- **limit**：每页返回数量
    - 最小值：1
    - 最大值：100
    - 实际返回数量可能小于该值
- **page**：分页页码
    - 从 1 开始
    - 过大的页码会返回空列表

#### 返回值

`List<LocationVo>`：附近用户位置信息列表，按距离从近到远排序

**LocationVo 对象结构**

| 字段名       | 类型     | 说明             | 示例值        |
|-----------|--------|----------------|------------|
| userId    | String | 用户ID           | "user789"  |
| distance  | Double | 距离当前用户的距离，单位：米 | 1234.5     |
| longitude | Double | 用户位置的经度        | 116.398234 |
| latitude  | Double | 用户位置的纬度        | 39.917123  |

**返回示例**

```json
[
  {
    "userId": "user789",
    "distance": 1234.5,
    "longitude": 116.398234,
    "latitude": 39.917123
  },
  {
    "userId": "user456",
    "distance": 2345.6,
    "longitude": 116.395123,
    "latitude": 39.915456
  }
]
```

#### 异常说明

| 异常类型                     | 说明     | 可能原因               |
|--------------------------|--------|--------------------|
| IllegalArgumentException | 参数非法   | userId 为空、搜索参数超出范围 |
| RuntimeException         | 服务内部错误 | Redis 查询失败、计算错误等   |

#### 使用示例

**Java 调用示例**

```java
@DubboReference
private LocationDubboService locationDubboService;

public List<LocationVo> findNearbyUsers() {
    String userId = "user123456";

    // 创建搜索条件
    NearbySearchDto dto = new NearbySearchDto();
    dto.setRadius(5000.0);  // 5公里范围
    dto.setLimit(20);       // 最多返回20个
    dto.setPage(1);         // 第1页

    try {
        // 调用服务
        List<LocationVo> nearbyUsers = locationDubboService.searchNearby(userId, dto);

        log.info("找到 {} 个附近用户", nearbyUsers.size());

        // 遍历结果
        for (LocationVo user : nearbyUsers) {
            log.info("用户：{}，距离：{} 米", user.getUserId(), user.getDistance());
        }

        return nearbyUsers;
    } catch (Exception e) {
        log.error("搜索附近用户失败：userId=" + userId, e);
        return Collections.emptyList();
    }
}
```

**分页查询示例**

```java
public void findAllNearbyUsers(String userId) {
    int page = 1;
    int pageSize = 20;
    boolean hasMore = true;

    while (hasMore) {
        NearbySearchDto dto = new NearbySearchDto();
        dto.setRadius(10000.0);  // 10公里范围
        dto.setLimit(pageSize);
        dto.setPage(page);

        List<LocationVo> users = locationDubboService.searchNearby(userId, dto);

        if (users.isEmpty()) {
            hasMore = false;
        } else {
            // 处理当前页数据
            processUsers(users);

            // 判断是否有下一页
            if (users.size() < pageSize) {
                hasMore = false;
            } else {
                page++;
            }
        }
    }
}
```

**注意事项**

1. **距离计算**：
    - 使用直线距离（球面距离）
    - 单位为米
    - 计算精度约 1 米

2. **结果排序**：
    - 默认按距离从近到远排序
    - 相同距离的用户按 ID 排序

3. **性能优化**：
    - 大范围搜索建议分页进行
    - 缓存常用区域的搜索结果
    - 避免频繁调用同一查询

4. **数据实时性**：
    - 搜索结果基于用户最后上报的位置
    - 位置信息有延迟（取决于上报频率）
    - 部分用户位置可能已过期

---

### clearInactiveUsers - 清理不活跃用户位置缓存

#### 接口描述

清理长期未更新位置的用户缓存数据，释放 Redis 内存空间。通常由定时任务调用。

#### 方法签名

```java
void clearInactiveUsers();
```

#### 请求参数

无参数

#### 返回值

无返回值（void）

#### 异常说明

| 异常类型             | 说明     | 可能原因             |
|------------------|--------|------------------|
| RuntimeException | 服务内部错误 | Redis 连接失败、删除失败等 |

#### 使用示例

**Java 调用示例**

```java
@DubboReference
private LocationDubboService locationDubboService;

/**
 * 定时任务：每小时清理一次不活跃用户位置
 */
@Scheduled(cron = "0 0 * * * ?")
public void cleanupInactiveUsers() {
    try {
        log.info("开始清理不活跃用户位置...");
        locationDubboService.clearInactiveUsers();
        log.info("清理完成");
    } catch (Exception e) {
        log.error("清理失败", e);
    }
}
```

**注意事项**

1. **调用频率**：
    - 建议每小时或每天执行一次
    - 避免过于频繁的清理

2. **不活跃定义**：
    - 由服务实现方定义（如超过 1 小时未更新）
    - 根据业务需求调整阈值

3. **批量删除**：
    - 可能采用批量删除方式
    - 执行时间可能较长

---

## 行政区划服务 API（RegionDubboService）

### 概述

`RegionDubboService` 提供中国行政区划查询服务，支持省、市、区县、乡镇、村五级行政区划查询，以及逆地理编码等功能。

**服务接口**：`com.xy.lucky.lbs.rpc.api.region.RegionDubboService`

**服务版本**：1.0.0

---

### getProvinces - 获取所有省份

#### 接口描述

获取中国所有省级行政区划列表，包括省、自治区、直辖市、特别行政区。

#### 方法签名

```java
List<RegionVo> getProvinces();
```

#### 请求参数

无参数

#### 返回值

`List<RegionVo>`：省份列表

**RegionVo 对象结构**

| 字段名         | 类型      | 说明          | 示例值      |
|-------------|---------|-------------|----------|
| code        | Long    | 行政区划代码（6 位） | 110000   |
| name        | String  | 名称          | "北京市"    |
| latitude    | Double  | 纬度（中心点）     | 39.9042  |
| longitude   | Double  | 经度（中心点）     | 116.4074 |
| fullAddress | String  | 完整地址        | "北京市"    |
| level       | Integer | 行政级别        | 1        |

**返回示例**

```json
[
  {
    "code": 110000,
    "name": "北京市",
    "latitude": 39.9042,
    "longitude": 116.4074,
    "fullAddress": "北京市",
    "level": 1
  },
  {
    "code": 120000,
    "name": "天津市",
    "latitude": 39.0842,
    "longitude": 117.2009,
    "fullAddress": "天津市",
    "level": 1
  }
]
```

#### 异常说明

| 异常类型             | 说明     | 可能原因     |
|------------------|--------|----------|
| RuntimeException | 服务内部错误 | 数据库查询失败等 |

#### 使用示例

```java
@DubboReference
private RegionDubboService regionDubboService;

public List<RegionVo> getAllProvinces() {
    try {
        List<RegionVo> provinces = regionDubboService.getProvinces();
        log.info("共有 {} 个省级行政区", provinces.size());
        return provinces;
    } catch (Exception e) {
        log.error("获取省份列表失败", e);
        return Collections.emptyList();
    }
}
```

---

### getCities - 根据省份代码获取城市列表

#### 接口描述

根据省份行政区划代码，查询该省下的所有地级市。

#### 方法签名

```java
List<RegionVo> getCities(Long provinceCode);
```

#### 请求参数

| 参数名          | 类型   | 必填 | 说明            | 示例值    |
|--------------|------|----|---------------|--------|
| provinceCode | Long | 是  | 省份行政区划代码（6 位） | 110000 |

#### 返回值

`List<RegionVo>`：城市列表，level=2

**返回示例**

```json
[
  {
    "code": 110100,
    "name": "北京市",
    "latitude": 39.9042,
    "longitude": 116.4074,
    "fullAddress": "北京市-北京市",
    "level": 2
  }
]
```

#### 异常说明

| 异常类型                     | 说明     | 可能原因                  |
|--------------------------|--------|-----------------------|
| IllegalArgumentException | 参数非法   | provinceCode 不存在或格式错误 |
| RuntimeException         | 服务内部错误 | 数据库查询失败等              |

#### 使用示例

```java
public List<RegionVo> getCitiesByProvince(Long provinceCode) {
    try {
        List<RegionVo> cities = regionDubboService.getCities(provinceCode);
        log.info("省份 {} 下有 {} 个城市", provinceCode, cities.size());
        return cities;
    } catch (Exception e) {
        log.error("获取城市列表失败：provinceCode=" + provinceCode, e);
        return Collections.emptyList();
    }
}
```

---

### getCounties - 根据城市代码获取区县列表

#### 接口描述

根据城市行政区划代码，查询该市下的所有区县。

#### 方法签名

```java
List<RegionVo> getCounties(Long cityCode);
```

#### 请求参数

| 参数名      | 类型   | 必填 | 说明            | 示例值    |
|----------|------|----|---------------|--------|
| cityCode | Long | 是  | 城市行政区划代码（6 位） | 110100 |

#### 返回值

`List<RegionVo>`：区县列表，level=3

**返回示例**

```json
[
  {
    "code": 110101,
    "name": "东城区",
    "latitude": 39.9165,
    "longitude": 116.4158,
    "fullAddress": "北京市-北京市-东城区",
    "level": 3
  },
  {
    "code": 110102,
    "name": "西城区",
    "latitude": 39.9137,
    "longitude": 116.3661,
    "fullAddress": "北京市-北京市-西城区",
    "level": 3
  }
]
```

#### 异常说明

| 异常类型                     | 说明     | 可能原因              |
|--------------------------|--------|-------------------|
| IllegalArgumentException | 参数非法   | cityCode 不存在或格式错误 |
| RuntimeException         | 服务内部错误 | 数据库查询失败等          |

#### 使用示例

```java
public List<RegionVo> getCountiesByCity(Long cityCode) {
    try {
        List<RegionVo> counties = regionDubboService.getCounties(cityCode);
        log.info("城市 {} 下有 {} 个区县", cityCode, counties.size());
        return counties;
    } catch (Exception e) {
        log.error("获取区县列表失败：cityCode=" + cityCode, e);
        return Collections.emptyList();
    }
}
```

---

### getTowns - 根据区县代码获取乡镇列表

#### 接口描述

根据区县行政区划代码，查询该区县下的所有乡镇、街道。

#### 方法签名

```java
List<RegionVo> getTowns(Long countyCode);
```

#### 请求参数

| 参数名        | 类型   | 必填 | 说明            | 示例值    |
|------------|------|----|---------------|--------|
| countyCode | Long | 是  | 区县行政区划代码（6 位） | 110101 |

#### 返回值

`List<RegionVo>`：乡镇列表，level=4

**返回示例**

```json
[
  {
    "code": 110101001,
    "name": "东华门街道",
    "latitude": 39.9165,
    "longitude": 116.4074,
    "fullAddress": "北京市-北京市-东城区-东华门街道",
    "level": 4
  }
]
```

#### 异常说明

| 异常类型                     | 说明     | 可能原因                |
|--------------------------|--------|---------------------|
| IllegalArgumentException | 参数非法   | countyCode 不存在或格式错误 |
| RuntimeException         | 服务内部错误 | 数据库查询失败等            |

#### 使用示例

```java
public List<RegionVo> getTownsByCounty(Long countyCode) {
    try {
        List<RegionVo> towns = regionDubboService.getTowns(countyCode);
        log.info("区县 {} 下有 {} 个乡镇", countyCode, towns.size());
        return towns;
    } catch (Exception e) {
        log.error("获取乡镇列表失败：countyCode=" + countyCode, e);
        return Collections.emptyList();
    }
}
```

---

### getVillages - 根据乡镇代码获取村列表

#### 接口描述

根据乡镇行政区划代码，查询该乡镇下的所有村、社区。

#### 方法签名

```java
List<RegionVo> getVillages(Long townCode);
```

#### 请求参数

| 参数名      | 类型   | 必填 | 说明            | 示例值       |
|----------|------|----|---------------|-----------|
| townCode | Long | 是  | 乡镇行政区划代码（9 位） | 110101001 |

#### 返回值

`List<RegionVo>`：村列表，level=5

**返回示例**

```json
[
  {
    "code": 110101001001,
    "name": "东华门社区",
    "latitude": 39.9165,
    "longitude": 116.4074,
    "fullAddress": "北京市-北京市-东城区-东华门街道-东华门社区",
    "level": 5
  }
]
```

#### 异常说明

| 异常类型                     | 说明     | 可能原因              |
|--------------------------|--------|-------------------|
| IllegalArgumentException | 参数非法   | townCode 不存在或格式错误 |
| RuntimeException         | 服务内部错误 | 数据库查询失败等          |

#### 使用示例

```java
public List<RegionVo> getVillagesByTown(Long townCode) {
    try {
        List<RegionVo> villages = regionDubboService.getVillages(townCode);
        log.info("乡镇 {} 下有 {} 个村", townCode, villages.size());
        return villages;
    } catch (Exception e) {
        log.error("获取村列表失败：townCode=" + townCode, e);
        return Collections.emptyList();
    }
}
```

---

### findNearestCounty - 查找最近的区县

#### 接口描述

根据经纬度坐标，查找距离该坐标最近的区县级行政区划。

#### 方法签名

```java
RegionVo findNearestCounty(Double lat, Double lng);
```

#### 请求参数

| 参数名 | 类型     | 必填 | 说明 | 示例值        |
|-----|--------|----|----|------------|
| lat | Double | 是  | 纬度 | 39.916527  |
| lng | Double | 是  | 经度 | 116.397128 |

#### 返回值

`RegionVo`：最近的区县信息，level=3

**返回示例**

```json
{
  "code": 110101,
  "name": "东城区",
  "latitude": 39.9165,
  "longitude": 116.4158,
  "fullAddress": "北京市-北京市-东城区",
  "level": 3
}
```

#### 异常说明

| 异常类型                     | 说明     | 可能原因       |
|--------------------------|--------|------------|
| IllegalArgumentException | 参数非法   | 经纬度超出范围    |
| RuntimeException         | 服务内部错误 | 查询失败、无法定位等 |

#### 使用示例

```java
public RegionVo findNearestCounty(Double lat, Double lng) {
    try {
        RegionVo county = regionDubboService.findNearestCounty(lat, lng);
        log.info("坐标 ({}, {}) 最近区县：{}", lat, lng, county.getName());
        return county;
    } catch (Exception e) {
        log.error("查找最近区县失败：lat=" + lat + ", lng=" + lng, e);
        return null;
    }
}
```

---

### searchRegions - 模糊搜索行政区划

#### 接口描述

根据关键词模糊搜索行政区划，支持省、市、区县、乡镇、村所有级别。

#### 方法签名

```java
List<RegionVo> searchRegions(String keyword);
```

#### 请求参数

| 参数名     | 类型     | 必填 | 说明    | 示例值  |
|---------|--------|----|-------|------|
| keyword | String | 是  | 搜索关键词 | "朝阳" |

#### 返回值

`List<RegionVo>`：匹配的行政区划列表

**返回示例**

```json
[
  {
    "code": 110105,
    "name": "朝阳区",
    "latitude": 39.9432,
    "longitude": 116.4429,
    "fullAddress": "北京市-北京市-朝阳区",
    "level": 3
  },
  {
    "code": 220104,
    "name": "朝阳区",
    "latitude": 43.8168,
    "longitude": 125.3235,
    "fullAddress": "吉林省-长春市-朝阳区",
    "level": 3
  }
]
```

#### 异常说明

| 异常类型                     | 说明     | 可能原因            |
|--------------------------|--------|-----------------|
| IllegalArgumentException | 参数非法   | keyword 为空或长度不足 |
| RuntimeException         | 服务内部错误 | 搜索失败等           |

#### 使用示例

```java
public List<RegionVo> searchRegions(String keyword) {
    try {
        List<RegionVo> regions = regionDubboService.searchRegions(keyword);
        log.info("关键词 '{}' 匹配到 {} 个结果", keyword, regions.size());
        return regions;
    } catch (Exception e) {
        log.error("搜索行政区划失败：keyword=" + keyword, e);
        return Collections.emptyList();
    }
}
```

---

### reverseGeocoding - 逆地理编码

#### 接口描述

将经纬度坐标转换为结构化的详细地址信息，包括省、市、区县、乡镇、村等完整信息。

#### 方法签名

```java
AddressVo reverseGeocoding(Double lat, Double lng);
```

#### 请求参数

| 参数名 | 类型     | 必填 | 说明 | 示例值        |
|-----|--------|----|----|------------|
| lat | Double | 是  | 纬度 | 39.916527  |
| lng | Double | 是  | 经度 | 116.397128 |

#### 返回值

`AddressVo`：详细地址信息

**AddressVo 对象结构**

| 字段名         | 类型     | 说明         | 示例值                   |
|-------------|--------|------------|-----------------------|
| province    | String | 省份         | "北京市"                 |
| city        | String | 城市         | "北京市"                 |
| district    | String | 区县         | "东城区"                 |
| town        | String | 乡镇         | "东华门街道"               |
| village     | String | 村/社区       | "东华门社区"               |
| fullAddress | String | 完整地址       | "北京市北京市东城区东华门街道东华门社区" |
| adCode      | Long   | 最细粒度行政区划代码 | 110101                |
| longitude   | Double | 经度         | 116.397128            |
| latitude    | Double | 纬度         | 39.916527             |

**返回示例**

```json
{
  "province": "北京市",
  "city": "北京市",
  "district": "东城区",
  "town": "东华门街道",
  "village": "东华门社区",
  "fullAddress": "北京市北京市东城区东华门街道东华门社区",
  "adCode": 110101,
  "longitude": 116.397128,
  "latitude": 39.916527
}
```

#### 异常说明

| 异常类型                     | 说明     | 可能原因       |
|--------------------------|--------|------------|
| IllegalArgumentException | 参数非法   | 经纬度超出范围    |
| RuntimeException         | 服务内部错误 | 查询失败、无法定位等 |

#### 使用示例

```java
public AddressVo getAddressFromCoordinate(Double lat, Double lng) {
    try {
        AddressVo address = regionDubboService.reverseGeocoding(lat, lng);
        log.info("坐标 ({}, {}) 对应地址：{}", lat, lng, address.getFullAddress());
        return address;
    } catch (Exception e) {
        log.error("逆地理编码失败：lat=" + lat + ", lng=" + lng, e);
        return null;
    }
}

/**
 * 用户打卡时获取详细地址
 */
public void checkIn(String userId, Double lat, Double lng) {
    AddressVo address = regionDubboService.reverseGeocoding(lat, lng);

    if (address != null) {
        // 保存打卡记录
        CheckInRecord record = new CheckInRecord();
        record.setUserId(userId);
        record.setLatitude(lat);
        record.setLongitude(lng);
        record.setProvince(address.getProvince());
        record.setCity(address.getCity());
        record.setDistrict(address.getDistrict());
        record.setFullAddress(address.getFullAddress());
        record.setAdCode(address.getAdCode());

        checkInRepository.save(record);
    }
}
```

---

## 数据模型详解

### 请求参数模型（DTO）

#### LocationUpdateDto

位置上报请求对象。

**类路径**：`com.xy.lucky.lbs.rpc.api.dto.LocationUpdateDto`

**字段说明**

| 字段        | 类型     | 必填 | 说明 | 取值范围       | 示例值        |
|-----------|--------|----|----|------------|------------|
| longitude | Double | 是  | 经度 | -180 ~ 180 | 116.397128 |
| latitude  | Double | 是  | 纬度 | -90 ~ 90   | 39.916527  |

**代码定义**

```java
@Data
@Schema(description = "位置上报请求")
public class LocationUpdateDto {
    @Schema(description = "经度", example = "116.397128")
    private Double longitude;

    @Schema(description = "纬度", example = "39.916527")
    private Double latitude;
}
```

#### NearbySearchDto

附近用户搜索请求对象。

**类路径**：`com.xy.lucky.lbs.rpc.api.dto.NearbySearchDto`

**字段说明**

| 字段     | 类型      | 必填 | 默认值    | 说明      | 取值范围        | 示例值    |
|--------|---------|----|--------|---------|-------------|--------|
| radius | Double  | 否  | 5000.0 | 搜索半径（米） | 100 ~ 50000 | 5000.0 |
| limit  | Integer | 否  | 20     | 最大结果数   | 1 ~ 100     | 20     |
| page   | Integer | 否  | 1      | 分页页码    | >= 1        | 1      |

**代码定义**

```java
@Data
@Schema(description = "附近用户搜索请求")
public class NearbySearchDto {
    @Schema(description = "搜索半径(米)", example = "5000")
    private Double radius;

    @Schema(description = "最大结果数", example = "20")
    private Integer limit;

    @Schema(description = "分页页码", example = "1")
    private Integer page;
}
```

---

### 响应数据模型（VO）

#### LocationVo

用户位置信息对象。

**类路径**：`com.xy.lucky.lbs.rpc.api.vo.LocationVo`

**字段说明**

| 字段        | 类型     | 说明    | 示例值          |
|-----------|--------|-------|--------------|
| userId    | String | 用户ID  | "user123456" |
| distance  | Double | 距离（米） | 1234.5       |
| longitude | Double | 经度    | 116.397128   |
| latitude  | Double | 纬度    | 39.916527    |

**代码定义**

```java
@Data
@Builder
@Schema(description = "用户位置信息")
public class LocationVo {
    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "距离(米)")
    private Double distance;

    @Schema(description = "经度")
    private Double longitude;

    @Schema(description = "纬度")
    private Double latitude;
}
```

#### RegionVo

行政区划信息对象。

**类路径**：`com.xy.lucky.lbs.rpc.api.vo.RegionVo`

**字段说明**

| 字段          | 类型      | 说明     | 示例值      |
|-------------|---------|--------|----------|
| code        | Long    | 行政区划代码 | 110000   |
| name        | String  | 名称     | "北京市"    |
| latitude    | Double  | 纬度     | 39.9042  |
| longitude   | Double  | 经度     | 116.4074 |
| fullAddress | String  | 完整地址   | "北京市"    |
| level       | Integer | 行政级别   | 1        |

**行政级别说明**

| 值 | 说明  | 示例       |
|---|-----|----------|
| 1 | 省级  | 北京市、河北省  |
| 2 | 市级  | 北京市、石家庄市 |
| 3 | 区县级 | 东城区、长安区  |
| 4 | 乡镇级 | 东华门街道    |
| 5 | 村级  | 东华门社区    |

**代码定义**

```java
@Data
@Builder
@Schema(description = "行政区划信息")
public class RegionVo {
    @Schema(description = "行政区划代码")
    private Long code;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "纬度")
    private Double latitude;

    @Schema(description = "经度")
    private Double longitude;

    @Schema(description = "完整地址")
    private String fullAddress;

    @Schema(description = "行政级别(1:省, 2:市, 3:区县, 4:乡镇, 5:村)")
    private Integer level;
}
```

#### AddressVo

完整地址信息对象。

**类路径**：`com.xy.lucky.lbs.rpc.api.vo.AddressVo`

**字段说明**

| 字段          | 类型     | 说明         | 示例值                   |
|-------------|--------|------------|-----------------------|
| province    | String | 省份         | "北京市"                 |
| city        | String | 城市         | "北京市"                 |
| district    | String | 区县         | "东城区"                 |
| town        | String | 乡镇         | "东华门街道"               |
| village     | String | 村/社区       | "东华门社区"               |
| fullAddress | String | 完整地址       | "北京市北京市东城区东华门街道东华门社区" |
| adCode      | Long   | 最细粒度行政区划代码 | 110101                |
| longitude   | Double | 经度         | 116.397128            |
| latitude    | Double | 纬度         | 39.916527             |

**代码定义**

```java
@Data
@Builder
@Schema(description = "完整地址信息")
public class AddressVo {
    @Schema(description = "省份")
    private String province;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "区县")
    private String district;

    @Schema(description = "乡镇")
    private String town;

    @Schema(description = "村/社区")
    private String village;

    @Schema(description = "完整地址")
    private String fullAddress;

    @Schema(description = "最细粒度行政区划代码")
    private Long adCode;

    @Schema(description = "经度")
    private Double longitude;

    @Schema(description = "纬度")
    private Double latitude;
}
```

---

## 错误码说明

### Dubbo 调用异常

Dubbo 调用可能抛出的通用异常：

| 异常类型                   | 错误码 | 说明       | 处理建议          |
|------------------------|-----|----------|---------------|
| RpcException           | -1  | RPC 调用失败 | 检查网络连接、服务状态   |
| TimeoutException       | -2  | 调用超时     | 增加超时时间或优化服务性能 |
| SerializationException | -3  | 序列化失败    | 检查数据类型是否兼容    |

### 业务异常

| 异常类型                     | 错误码  | 说明         | 处理建议          |
|--------------------------|------|------------|---------------|
| IllegalArgumentException | 1001 | 参数为空或无效    | 检查参数是否符合要求    |
| IllegalArgumentException | 1002 | 经纬度超出范围    | 检查经纬度值        |
| IllegalArgumentException | 1003 | 行政区划代码不存在  | 检查代码是否正确      |
| RuntimeException         | 2001 | Redis 连接失败 | 检查 Redis 服务状态 |
| RuntimeException         | 2002 | 数据库查询失败    | 检查数据库连接和查询语句  |
| RuntimeException         | 2003 | 位置信息不存在    | 该用户未上报过位置     |

### 错误处理建议

```java
try {
    // 调用服务
    locationDubboService.updateLocation(userId, dto);
} catch (IllegalArgumentException e) {
    // 参数错误，提示用户
    log.warn("参数错误：{}", e.getMessage());
    throw new BusinessException("参数错误，请检查输入");
} catch (RpcException e) {
    // RPC 调用失败，降级处理
    log.error("服务调用失败", e);
    // 使用缓存或默认值
    return getCachedData();
} catch (Exception e) {
    // 其他未知错误
    log.error("未知错误", e);
    throw new BusinessException("系统繁忙，请稍后再试");
}
```

---

## 调用示例

### 完整应用示例

#### 示例 1：用户位置管理

```java@Service
public class UserLocationService {

    @DubboReference
    private LocationDubboService locationDubboService;

    private static final double DEFAULT_RADIUS = 5000.0;  // 5公里
    private static final int DEFAULT_LIMIT = 20;

    /**
     * 用户登录后上报位置
     */
    public void reportLocationOnLogin(String userId, Double longitude, Double latitude) {
        LocationUpdateDto dto = new LocationUpdateDto();
        dto.setLongitude(longitude);
        dto.setLatitude(latitude);

        try {
            locationDubboService.updateLocation(userId, dto);
            log.info("用户 {} 位置上报成功：({}, {})", userId, longitude, latitude);
        } catch (Exception e) {
            log.error("用户 {} 位置上报失败", userId, e);
        }
    }

    /**
     * 定时上报位置（每5分钟）
     */
    @Scheduled(fixedRate = 300000)
    public void periodicLocationReport() {
        // 获取在线用户列表
        List<String> onlineUsers = getOnlineUsers();

        for (String userId : onlineUsers) {
            // 获取用户当前位置
            UserLocation location = getCurrentUserLocation(userId);
            if (location != null) {
                reportLocationOnLogin(userId, location.getLongitude(), location.getLatitude());
            }
        }
    }

    /**
     * 查找附近用户
     */
    public List<LocationVo> findNearbyUsers(String userId, Double radius, Integer limit) {
        NearbySearchDto dto = new NearbySearchDto();
        dto.setRadius(radius != null ? radius : DEFAULT_RADIUS);
        dto.setLimit(limit != null ? limit : DEFAULT_LIMIT);
        dto.setPage(1);

        try {
            return locationDubboService.searchNearby(userId, dto);
        } catch (Exception e) {
            log.error("查找附近用户失败：userId={}", userId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 分页获取附近用户
     */
    public List<LocationVo> findNearbyUsersWithPage(String userId, Integer page) {
        NearbySearchDto dto = new NearbySearchDto();
        dto.setRadius(10000.0);  // 10公里
        dto.setLimit(20);
        dto.setPage(page);

        return locationDubboService.searchNearby(userId, dto);
    }
}
```

#### 示例 2：行政区划服务

```java
@Service
public class AddressService {

    @DubboReference
    private RegionDubboService regionDubboService;

    /**
     * 获取完整行政区划选择树
     */
    public Map<String, Object> getRegionTree() {
        Map<String, Object> tree = new LinkedHashMap<>();

        // 获取所有省份
        List<RegionVo> provinces = regionDubboService.getProvinces();

        for (RegionVo province : provinces) {
            Map<String, Object> provinceData = new LinkedHashMap<>();
            provinceData.put("code", province.getCode());
            provinceData.put("name", province.getName());

            // 获取城市列表
            List<RegionVo> cities = regionDubboService.getCities(province.getCode());
            List<Map<String, Object>> cityList = new ArrayList<>();

            for (RegionVo city : cities) {
                Map<String, Object> cityData = new LinkedHashMap<>();
                cityData.put("code", city.getCode());
                cityData.put("name", city.getName());

                // 获取区县列表
                List<RegionVo> counties = regionDubboService.getCounties(city.getCode());
                List<Map<String, Object>> countyList = new ArrayList<>();

                for (RegionVo county : counties) {
                    Map<String, Object> countyData = new LinkedHashMap<>();
                    countyData.put("code", county.getCode());
                    countyData.put("name", county.getName());
                    countyList.add(countyData);
                }

                cityData.put("counties", countyList);
                cityList.add(cityData);
            }

            provinceData.put("cities", cityList);
            tree.put(province.getName(), provinceData);
        }

        return tree;
    }

    /**
     * 地址自动补全
     */
    public List<String> autoCompleteAddress(String keyword) {
        if (StringUtils.isBlank(keyword) || keyword.length() < 2) {
            return Collections.emptyList();
        }

        List<RegionVo> regions = regionDubboService.searchRegions(keyword);
        return regions.stream()
            .map(RegionVo::getFullAddress)
            .limit(10)
            .collect(Collectors.toList());
    }

    /**
     * 用户打卡
     */
    public CheckInResult checkIn(String userId, Double lat, Double lng) {
        // 获取详细地址
        AddressVo address = regionDubboService.reverseGeocoding(lat, lng);

        if (address == null) {
            return CheckInResult.fail("无法获取地址信息");
        }

        // 保存打卡记录
        CheckInRecord record = new CheckInRecord();
        record.setUserId(userId);
        record.setLatitude(lat);
        record.setLongitude(lng);
        record.setProvince(address.getProvince());
        record.setCity(address.getCity());
        record.setDistrict(address.getDistrict());
        record.setTown(address.getTown());
        record.setVillage(address.getVillage());
        record.setFullAddress(address.getFullAddress());
        record.setAdCode(address.getAdCode());
        record.setCheckInTime(LocalDateTime.now());

        checkInRepository.save(record);

        return CheckInResult.success(address);
    }

    /**
     * 判断用户是否在同一城市
     */
    public boolean isInSameCity(String user1Id, String user2Id) {
        // 获取两个用户的最后打卡记录
        CheckInRecord record1 = checkInRepository.findLatestByUserId(user1Id);
        CheckInRecord record2 = checkInRepository.findLatestByUserId(user2Id);

        if (record1 == null || record2 == null) {
            return false;
        }

        // 比较adCode的前4位（城市代码）
        String cityCode1 = String.valueOf(record1.getAdCode()).substring(0, 4);
        String cityCode2 = String.valueOf(record2.getAdCode()).substring(0, 4);

        return cityCode1.equals(cityCode2);
    }
}
```

#### 示例 3：异常处理和降级

```java
@Service
public class RobustLocationService {

    @DubboReference(check = false, timeout = 5000, retries = 2)
    private LocationDubboService locationDubboService;

    @DubboReference(check = false, timeout = 5000, retries = 2)
    private RegionDubboService regionDubboService;

    /**
     * 带降级的位置上报
     */
    public void updateLocationWithFallback(String userId, Double longitude, Double latitude) {
        try {
            LocationUpdateDto dto = new LocationUpdateDto();
            dto.setLongitude(longitude);
            dto.setLatitude(latitude);
            locationDubboService.updateLocation(userId, dto);
        } catch (RpcException e) {
            log.warn("位置服务调用失败，使用降级策略：{}", e.getMessage());
            // 降级：保存到本地缓存，稍后重试
            saveToLocalCache(userId, longitude, latitude);
        } catch (Exception e) {
            log.error("位置上报失败", e);
        }
    }

    /**
     * 带降级的附近用户搜索
     */
    public List<LocationVo> searchNearbyWithFallback(String userId) {
        try {
            NearbySearchDto dto = new NearbySearchDto();
            dto.setRadius(5000.0);
            dto.setLimit(20);
            dto.setPage(1);

            return locationDubboService.searchNearby(userId, dto);
        } catch (RpcException e) {
            log.warn("附近用户搜索失败，使用降级策略：{}", e.getMessage());
            // 降级：返回本地缓存的附近用户
            return getNearbyUsersFromLocalCache(userId);
        } catch (Exception e) {
            log.error("搜索失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 带缓存的地址查询
     */
    @Cacheable(value = "address", key = "#lat + ',' + #lng")
    public AddressVo reverseGeocodingWithCache(Double lat, Double lng) {
        try {
            return regionDubboService.reverseGeocoding(lat, lng);
        } catch (Exception e) {
            log.error("逆地理编码失败：lat={}, lng={}", lat, lng, e);
            return null;
        }
    }

    private void saveToLocalCache(String userId, Double longitude, Double latitude) {
        // 保存到本地缓存
        // ...
    }

    private List<LocationVo> getNearbyUsersFromLocalCache(String userId) {
        // 从本地缓存获取
        // ...
        return Collections.emptyList();
    }
}
```

---

**最后更新时间：2024-01-01**
