# im-lbs-rpc-api 模块文档

## 目录

- [模块概述](#模块概述)
- [功能特性](#功能特性)
- [技术架构](#技术架构)
- [快速开始](#快速开始)
- [模块依赖](#模块依赖)
- [核心服务](#核心服务)
- [数据模型](#数据模型)
- [配置说明](#配置说明)
- [使用示例](#使用示例)
- [常见问题](#常见问题)

---

## 模块概述

`im-lbs-rpc-api` 是 Lucky Cloud 项目中基于位置服务（Location Based Service，LBS）的 RPC API 模块。该模块提供了完整的 Dubbo
服务接口定义和 OpenAPI 3.0 规范，用于支持地理位置相关的业务功能。

### 主要功能

本模块主要提供两大核心服务：

1. **位置服务（LocationDubboService）**
    - 用户位置上报与实时更新
    - 附近用户搜索（基于地理位置的距离计算）
    - 不活跃用户位置清理

2. **行政区划服务（RegionDubboService）**
    - 全国行政区划多级查询（省/市/区县/乡镇/村）
    - 行政区域模糊搜索
    - 逆地理编码（坐标转地址）
    - 最近区县查找

### 应用场景

- 社交应用：查找附近的好友、商家、活动
- 出行服务：基于位置的路线规划、打车服务
- 本地生活：周边美食、酒店、景点推荐
- 物流配送：基于距离的配送员调度
- 位置打卡：考勤、签到、围栏管理

---

## 功能特性

### 1. 位置管理

- **实时位置上报**：支持用户实时更新自己的地理位置
- **附近用户搜索**：基于圆形范围搜索附近用户，支持自定义半径和结果数量
- **距离计算**：精确计算两个地理位置之间的直线距离
- **位置缓存**：使用 Redis 缓存用户位置信息，提高查询性能
- **不活跃用户清理**：定期清理长期未更新的位置数据

### 2. 行政区划服务

- **五级行政区划**：支持省、市、区县、乡镇、村五级查询
- **级联查询**：可按需查询任意层级的下级行政区划
- **智能搜索**：支持关键词模糊搜索行政区划
- **逆地理编码**：将经纬度坐标转换为结构化的地址信息
- **最近区县查找**：根据坐标快速定位所属的行政区划

### 3. 标准化规范

- **OpenAPI 3.0**：提供标准的 REST API 规范
- **Dubbo RPC**：支持高性能的远程服务调用
- **数据校验**：完整的参数校验和错误处理
- **文档齐全**：详细的接口文档和使用示例

---

## 技术架构

### 技术栈

- **Dubbo 3.x**：高性能 RPC 框架
- **Nacos**：服务注册与发现中心
- **Swagger/OpenAPI 3.0**：API 规范和文档生成
- **Lombok**：简化 Java 代码编写
- **Spring Boot**：应用框架

### 架构设计

```
im-lbs-rpc-api
├── dubbo-api/                 # Dubbo 服务接口定义
│   ├── LocationDubboService   # 位置服务接口
│   └── RegionDubboService     # 行政区划服务接口
├── dto/                       # 数据传输对象
│   ├── LocationUpdateDto      # 位置上报请求
│   └── NearbySearchDto        # 附近搜索请求
├── vo/                        # 视图对象
│   ├── LocationVo             # 用户位置信息
│   ├── RegionVo               # 行政区划信息
│   └── AddressVo              # 完整地址信息
└── openapi/                   # OpenAPI 规范
    ├── lbs-location-api.yaml  # 位置服务 API 规范
    └── lbs-region-api.yaml    # 行政区划服务 API 规范
```

---

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- Nacos 服务端（用于服务注册发现）
- Redis（用于位置缓存）

### 安装步骤

1. **克隆项目**

```bash
git clone https://github.com/your-org/Lucky-cloud.git
cd Lucky-cloud/im-lbs/im-lbs-rpc-api
```

2. **编译打包**

```bash
mvn clean install
```

3. **添加依赖**

在消费者服务的 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>com.xy.lucky</groupId>
    <artifactId>im-lbs-rpc-api</artifactId>
    <version>${revision}</version>
</dependency>
```

4. **配置 Dubbo 引用**

在消费者服务中配置 Dubbo 服务引用：

```yaml
dubbo:
  consumer:
    check: false
  protocol:
    name: dubbo
    port: -1
  registry:
    address: nacos://localhost:8848
```

5. **注入服务使用**

```java
@DubboReference
private LocationDubboService locationDubboService;

@DubboReference
private RegionDubboService regionDubboService;
```

---

## 模块依赖

### Maven 依赖

```xml
<dependencies>
    <!-- 核心模块 -->
    <dependency>
        <groupId>com.xy.lucky</groupId>
        <artifactId>im-starter-core</artifactId>
    </dependency>

    <dependency>
        <groupId>com.xy.lucky</groupId>
        <artifactId>im-starter-common</artifactId>
    </dependency>

    <!-- Dubbo -->
    <dependency>
        <groupId>org.apache.dubbo</groupId>
        <artifactId>dubbo-spring-boot-starter</artifactId>
    </dependency>

    <dependency>
        <groupId>org.apache.dubbo</groupId>
        <artifactId>dubbo-nacos-spring-boot-starter</artifactId>
    </dependency>

    <!-- Swagger Annotations -->
    <dependency>
        <groupId>io.swagger.core.v3</groupId>
        <artifactId>swagger-annotations</artifactId>
    </dependency>
</dependencies>
```

### 服务依赖

- **Nacos 注册中心**：服务注册与发现
- **Redis**：位置信息缓存
- **数据库**：行政区划数据存储（由服务实现方提供）

---

## 核心服务

### 1. 位置服务（LocationDubboService）

#### 接口定义

```java
public interface LocationDubboService {
    /**
     * 上报用户位置
     * @param userId 用户ID
     * @param dto 位置信息（经纬度）
     */
    void updateLocation(String userId, LocationUpdateDto dto);

    /**
     * 搜索附近用户
     * @param userId 当前用户ID
     * @param dto 搜索条件（半径、限制数、分页）
     * @return 附近用户列表
     */
    List<LocationVo> searchNearby(String userId, NearbySearchDto dto);

    /**
     * 清理不活跃用户位置缓存
     */
    void clearInactiveUsers();
}
```

#### 使用场景

- 用户登录后上报当前位置
- 社交应用查找附近好友
- 外卖/打车应用查找附近司机/商家

### 2. 行政区划服务（RegionDubboService）

#### 接口定义

```java
public interface RegionDubboService {
    /**
     * 获取所有省份
     */
    List<RegionVo> getProvinces();

    /**
     * 根据省份代码获取城市列表
     * @param provinceCode 省份代码
     */
    List<RegionVo> getCities(Long provinceCode);

    /**
     * 根据城市代码获取区县列表
     * @param cityCode 城市代码
     */
    List<RegionVo> getCounties(Long cityCode);

    /**
     * 根据区县代码获取乡镇列表
     * @param countyCode 区县代码
     */
    List<RegionVo> getTowns(Long countyCode);

    /**
     * 根据乡镇代码获取村列表
     * @param townCode 乡镇代码
     */
    List<RegionVo> getVillages(Long townCode);

    /**
     * 查找最近的区县
     * @param lat 纬度
     * @param lng 经度
     */
    RegionVo findNearestCounty(Double lat, Double lng);

    /**
     * 模糊搜索行政区划
     * @param keyword 关键词
     */
    List<RegionVo> searchRegions(String keyword);

    /**
     * 逆地理编码
     * @param lat 纬度
     * @param lng 经度
     */
    AddressVo reverseGeocoding(Double lat, Double lng);
}
```

#### 使用场景

- 用户注册时选择所在地区
- 地址填写时的级联选择
- 根据坐标获取详细地址
- 地区数据模糊搜索

---

## 数据模型

### DTO（数据传输对象）

#### LocationUpdateDto - 位置上报请求

| 字段        | 类型     | 必填 | 说明 | 示例值        |
|-----------|--------|----|----|------------|
| longitude | Double | 是  | 经度 | 116.397128 |
| latitude  | Double | 是  | 纬度 | 39.916527  |

#### NearbySearchDto - 附近用户搜索请求

| 字段     | 类型      | 必填 | 说明      | 示例值  |
|--------|---------|----|---------|------|
| radius | Double  | 否  | 搜索半径（米） | 5000 |
| limit  | Integer | 否  | 最大结果数   | 20   |
| page   | Integer | 否  | 分页页码    | 1    |

### VO（视图对象）

#### LocationVo - 用户位置信息

| 字段        | 类型     | 说明    | 示例值        |
|-----------|--------|-------|------------|
| userId    | String | 用户ID  | "user123"  |
| distance  | Double | 距离（米） | 1234.5     |
| longitude | Double | 经度    | 116.397128 |
| latitude  | Double | 纬度    | 39.916527  |

#### RegionVo - 行政区划信息

| 字段          | 类型      | 说明        | 示例值      |
|-------------|---------|-----------|----------|
| code        | Long    | 行政区划代码    | 110000   |
| name        | String  | 名称        | "北京市"    |
| latitude    | Double  | 纬度        | 39.9042  |
| longitude   | Double  | 经度        | 116.4074 |
| fullAddress | String  | 完整地址      | "北京市"    |
| level       | Integer | 行政级别（1-5） | 1        |

**行政级别说明**：

- 1：省级（省、直辖市、自治区）
- 2：市级（地级市、市辖区）
- 3：区县级（县、县级市、市辖区）
- 4：乡镇级（乡、镇、街道）
- 5：村级（村、社区）

#### AddressVo - 完整地址信息

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

---

## 配置说明

### Dubbo 配置

```yaml
dubbo:
  application:
    name: ${spring.application.name}
  protocol:
    name: dubbo
    port: -1  # -1 表示随机端口
  registry:
    address: nacos://${nacos.address:localhost:8848}
    parameters:
      namespace: ${nacos.namespace:}
  consumer:
    check: false  # 启动时不检查服务提供方
    timeout: 5000  # 调用超时时间（毫秒）
    retries: 2  # 重试次数
```

### Redis 配置

```yaml
spring:
  redis:
    host: ${redis.host:localhost}
    port: ${redis.port:6379}
    password: ${redis.password:}
    database: ${redis.database:0}
    timeout: 3000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
        max-wait: -1ms
```

### Nacos 配置

```yaml
nacos:
  address: localhost:8848
  namespace: dev
  group: DEFAULT_GROUP
```

---

## 使用示例

### 示例 1：用户位置上报

```java
@Service
public class UserService {

    @DubboReference
    private LocationDubboService locationDubboService;

    /**
     * 用户登录后上报位置
     */
    public void onUserLogin(String userId, Double longitude, Double latitude) {
        LocationUpdateDto dto = new LocationUpdateDto();
        dto.setLongitude(longitude);
        dto.setLatitude(latitude);

        locationDubboService.updateLocation(userId, dto);
    }
}
```

### 示例 2：查找附近用户

```java
@Service
public class NearbyService {

    @DubboReference
    private LocationDubboService locationDubboService;

    /**
     * 查找5公里内的用户
     */
    public List<LocationVo> findNearbyUsers(String userId) {
        NearbySearchDto dto = new NearbySearchDto();
        dto.setRadius(5000.0);  // 5公里
        dto.setLimit(20);       // 最多20个结果
        dto.setPage(1);

        return locationDubboService.searchNearby(userId, dto);
    }

    /**
     * 分页查找附近用户
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

### 示例 3：获取行政区划数据

```java
@Service
public class AddressService {

    @DubboReference
    private RegionDubboService regionDubboService;

    /**
     * 获取所有省份
     */
    public List<RegionVo> getAllProvinces() {
        return regionDubboService.getProvinces();
    }

    /**
     * 根据省份获取城市列表
     */
    public List<RegionVo> getCitiesByProvince(Long provinceCode) {
        return regionDubboService.getCities(provinceCode);
    }

    /**
     * 获取完整行政区划链路
     */
    public Map<String, Object> getFullRegionPath(Long villageCode) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 村
        RegionVo village = regionDubboService.getVillages(/* townCode */)
            .stream()
            .filter(v -> v.getCode().equals(villageCode))
            .findFirst()
            .orElse(null);

        // 继续向上查询...

        return result;
    }
}
```

### 示例 4：逆地理编码

```java
@Service
public class LocationService {

    @DubboReference
    private RegionDubboService regionDubboService;

    /**
     * 将经纬度转换为详细地址
     */
    public AddressVo getAddressFromCoordinate(Double lat, Double lng) {
        return regionDubboService.reverseGeocoding(lat, lng);
    }

    /**
     * 用户打卡时获取详细地址
     */
    public void checkIn(String userId, Double lat, Double lng) {
        // 获取详细地址
        AddressVo address = regionDubboService.reverseGeocoding(lat, lng);

        // 保存打卡记录
        CheckInRecord record = new CheckInRecord();
        record.setUserId(userId);
        record.setLatitude(lat);
        record.setLongitude(lng);
        record.setAddress(address.getFullAddress());
        record.setAdCode(address.getAdCode());

        // 保存到数据库
        checkInRepository.save(record);
    }
}
```

### 示例 5：搜索行政区划

```java
@Service
public class RegionSearchService {

    @DubboReference
    private RegionDubboService regionDubboService;

    /**
     * 搜索包含关键词的行政区划
     */
    public List<RegionVo> searchRegions(String keyword) {
        return regionDubboService.searchRegions(keyword);
    }

    /**
     * 自动补全行政区划名称
     */
    public List<String> autoCompleteRegionName(String prefix) {
        List<RegionVo> regions = regionDubboService.searchRegions(prefix);
        return regions.stream()
            .map(RegionVo::getFullAddress)
            .limit(10)
            .collect(Collectors.toList());
    }
}
```

---

## 常见问题

### Q1: 如何选择合适的搜索半径？

**A:** 搜索半径应根据实际业务场景选择：

- **步行场景**：500-1000 米
- **周边生活**：1000-3000 米
- **同城服务**：5000-10000 米
- **同城交友**：10000-50000 米

半径越大，计算越复杂，性能开销越大。建议提供范围选择器让用户自定义。

### Q2: 坐标系是什么？如何处理不同坐标系？

**A:** 本模块使用的坐标系应由服务实现方决定。常见的坐标系有：

- **WGS-84**：国际标准 GPS 坐标系
- **GCJ-02**：中国国测局坐标系（火星坐标）
- **BD-09**：百度坐标系

建议在文档中明确标注使用的坐标系，如需转换可在应用层使用坐标转换工具。

### Q3: 如何保证位置数据的实时性？

**A:** 建议采取以下策略：

1. **定期上报**：应用定期（如每 5 分钟）自动上报位置
2. **事件上报**：用户移动距离超过阈值时触发上报
3. **后台更新**：iOS 和 Android 都支持后台位置更新
4. **设置过期时间**：Redis 中设置 TTL，自动清理过期数据

### Q4: 如何处理用户隐私问题？

**A:** 建议采取以下措施：

1. **明确告知**：在隐私政策中说明位置数据用途
2. **用户授权**：获取明确的用户授权
3. **数据脱敏**：存储时可以考虑进行模糊处理
4. **提供关闭选项**：允许用户随时关闭位置服务
5. **数据加密**：传输和存储时进行加密

### Q5: 如何优化附近用户搜索性能？

**A:** 性能优化建议：

1. **使用 GeoHash**：将二维坐标转换为一维字符串，便于索引
2. **Redis GEO**：使用 Redis 的 GEO 命令实现高效的地理位置计算
3. **分页查询**：避免一次性返回过多数据
4. **缓存热点数据**：缓存常用区域的搜索结果
5. **异步更新**：位置更新采用异步方式，不阻塞主流程

### Q6: 行政区划数据如何更新？

**A:** 行政区划数据更新建议：

1. **官方数据源**：从国家统计局等官方渠道获取最新数据
2. **定期同步**：每季度或半年同步一次
3. **版本管理**：对行政区划数据进行版本管理
4. **增量更新**：只更新变更的部分，减少数据传输

### Q7: 服务调用失败如何处理？

**A:** 异常处理建议：

1. **配置重试**：Dubbo consumer 配置合理的重试次数
2. **降级策略**：服务不可用时使用本地缓存或默认值
3. **熔断保护**：使用 Sentinel 等工具实现熔断
4. **日志记录**：记录失败日志，便于排查问题
5. **监控告警**：对调用失败率进行监控和告警

### Q8: 如何测试位置服务？

**A:** 测试方法建议：

1. **模拟定位**：使用模拟定位软件修改手机位置
2. **虚拟坐标**：在测试环境使用虚拟的经纬度坐标
3. **批量测试**：准备多组测试数据，覆盖不同场景
4. **压力测试**：模拟大量用户同时上报和查询
5. **边界测试**：测试边界情况（如极地、海上等）

### Q9: 支持哪些坐标系？

**A:** 请参考服务实现方的文档。一般来说：

- 国内应用：使用 GCJ-02 坐标系
- 国际应用：使用 WGS-84 坐标系
- 百度地图：使用 BD-09 坐标系

不同坐标系之间需要进行转换。

### Q10: 如何计算两个位置之间的距离？

**A:** 本模块的 `searchNearby` 接口已经内置了距离计算功能。如果需要单独计算距离，可以使用：

1. **Haversine 公式**：考虑地球曲率的精确计算
2. **简化公式**：短距离下可以使用平面距离近似
3. **第三方库**：使用 GeoTools、Turf.js 等地理计算库

---

## 附录

### A. 相关文档

- [Dubbo 官方文档](https://dubbo.apache.org/zh/docs/)
- [OpenAPI 3.0 规范](https://swagger.io/specification/)
- [Redis GEO 命令](https://redis.io/commands/georadius/)
- [中国行政区划代码](http://www.stats.gov.cn/sj/tjbz/tjyqhdmhclh/)

### B. 版本历史

| 版本    | 日期         | 说明                 |
|-------|------------|--------------------|
| 1.0.0 | 2024-01-01 | 初始版本，支持基础位置和行政区划服务 |

### C. 联系方式

- 项目地址：https://github.com/your-org/Lucky-cloud
- 问题反馈：https://github.com/your-org/Lucky-cloud/issues
- 邮箱：support@lucky.com

---

**最后更新时间：2024-01-01**
