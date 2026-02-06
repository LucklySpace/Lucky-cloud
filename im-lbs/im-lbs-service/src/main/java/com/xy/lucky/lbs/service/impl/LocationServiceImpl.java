package com.xy.lucky.lbs.service.impl;

import ch.hsr.geohash.GeoHash;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xy.lucky.lbs.converter.LocationConverter;
import com.xy.lucky.lbs.domain.dto.LocationUpdateDto;
import com.xy.lucky.lbs.domain.dto.NearbySearchDto;
import com.xy.lucky.lbs.domain.po.LocationHistoryPo;
import com.xy.lucky.lbs.domain.vo.LocationVo;
import com.xy.lucky.lbs.mapper.LocationHistoryMapper;
import com.xy.lucky.lbs.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 位置服务实现类
 * 负责用户位置的上报、存储、附近的人搜索以及不活跃用户清理
 * 采用 Redis Geo + PostGIS 的冷热分离架构
 *
 * @author lucky
 * @since 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private static final String KEY_PREFIX = "im:lbs:geo:";
    private static final String USER_PARTITION_KEY = "im:lbs:user:partition";
    private static final String USER_LAST_ACTIVE_KEY = "im:lbs:user:last_active";
    // GeoHash 精度 4 字符，误差约 20km，适合做大致的分区键
    private static final int PARTITION_CHARS = 4;
    private final StringRedisTemplate redisTemplate;
    private final LocationHistoryMapper locationHistoryMapper;
    private final LocationConverter converter;

    /**
     * 更新用户位置
     * 1. 计算 GeoHash 分区
     * 2. 维护用户与分区的映射关系
     * 3. 写入 Redis Geo 数据结构 (热数据)
     * 4. 更新用户活跃时间
     * 5. 异步持久化到数据库 (冷数据)
     *
     * @param userId 用户ID
     * @param dto    位置信息
     */
    @Override
    public void updateLocation(String userId, LocationUpdateDto dto) {
        // 1. 计算 GeoHash 用于分区，避免单 Key 热点问题
        GeoHash geoHash = GeoHash.withCharacterPrecision(dto.getLatitude(), dto.getLongitude(), PARTITION_CHARS);
        String newPartitionKey = KEY_PREFIX + geoHash.toBase32();

        // 2. 处理分区变更：如果用户跨越了分区，需要从旧分区移除
        String oldPartitionKey = (String) redisTemplate.opsForHash().get(USER_PARTITION_KEY, userId);
        if (oldPartitionKey != null && !oldPartitionKey.equals(newPartitionKey)) {
            try {
                redisTemplate.opsForGeo().remove(oldPartitionKey, userId);
            } catch (Exception e) {
                log.warn("移除旧位置失败 user {}", userId, e);
            }
        }
        redisTemplate.opsForHash().put(USER_PARTITION_KEY, userId, newPartitionKey);

        // 3. 写入 Redis Geo
        redisTemplate.opsForGeo().add(newPartitionKey, new Point(dto.getLongitude(), dto.getLatitude()), userId);

        // 4. 更新活跃时间戳用于清理任务
        redisTemplate.opsForZSet().add(USER_LAST_ACTIVE_KEY, userId, System.currentTimeMillis());

        // 5. 异步持久化到 PostGIS，用于历史轨迹查询或降级兜底
        saveToHistoryAsync(userId, dto, geoHash.toBase32());
    }

    /**
     * 异步保存位置历史
     *
     * @param userId  用户ID
     * @param dto     位置信息
     * @param geohash GeoHash值
     */
    @Async
    public void saveToHistoryAsync(String userId, LocationUpdateDto dto, String geohash) {
        try {
            LocationHistoryPo history = converter.toPo(dto);
            history.setUserId(userId);
            history.setGeohash(geohash);
            history.setCreateTime(LocalDateTime.now());
            locationHistoryMapper.insert(history);
        } catch (Exception e) {
            log.error("保存用户位置历史失败 user {}", userId, e);
        }
    }

    /**
     * 搜索附近的人
     * 1. 确定用户所在分区及邻域分区
     * 2. 并行搜索所有相关分区
     * 3. 聚合结果并按距离排序
     * 4. Sentinel 熔断降级保护
     *
     * @param userId 用户ID
     * @param dto    搜索条件
     * @return 附近的人列表
     */
    @Override
    @SentinelResource(value = "searchNearby", fallback = "searchNearbyFallback")
    public List<LocationVo> searchNearby(String userId, NearbySearchDto dto) {
        // 1. 获取用户所在分区
        String partitionKey = (String) redisTemplate.opsForHash().get(USER_PARTITION_KEY, userId);
        if (partitionKey == null) {
            return Collections.emptyList();
        }

        // 2. 获取用户当前位置作为搜索中心点
        List<Point> positions = redisTemplate.opsForGeo().position(partitionKey, userId);
        if (positions == null || positions.isEmpty() || positions.getFirst() == null) {
            return Collections.emptyList();
        }
        Point center = positions.getFirst();

        // 3. 确定要搜索的分区 (中心分区 + 8个邻域分区)
        GeoHash centerHash = GeoHash.withCharacterPrecision(center.getY(), center.getX(), PARTITION_CHARS);
        Set<String> keysToCheck = new HashSet<>();
        keysToCheck.add(KEY_PREFIX + centerHash.toBase32());
        for (GeoHash neighbor : centerHash.getAdjacent()) {
            keysToCheck.add(KEY_PREFIX + neighbor.toBase32());
        }

        // 4. 并行搜索多个 Redis Key，提高响应速度
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> allResults = Collections.synchronizedList(new ArrayList<>());

        keysToCheck.parallelStream().forEach(key -> {
            try {
                // 使用 GEOSEARCH 命令 (Redis 6.2+)
                GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo().search(
                        key,
                        GeoReference.fromCoordinate(center),
                        new Distance(dto.getRadius(), Metrics.KILOMETERS), // 使用公里作为单位
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                                .limit(dto.getLimit())
                                .includeDistance()
                                .includeCoordinates() // 获取坐标以便返回
                );

                if (results != null) {
                    allResults.addAll(results.getContent());
                }
            } catch (Exception e) {
                log.warn("搜索分区失败 key {}", key, e);
            }
        });

        // 5. 聚合结果、过滤自身、排序、截取
        return allResults.stream()
                .filter(r -> !r.getContent().getName().equals(userId))
                .sorted(Comparator.comparingDouble(r -> r.getDistance().getValue()))
                .limit(dto.getLimit())
                .map(r -> {
                    Point point = r.getContent().getPoint();
                    return LocationVo.builder()
                            .userId(r.getContent().getName())
                            .distance(r.getDistance().getValue())
                            // 坐标保留两位小数
                            .longitude(point != null ? Math.round(point.getX() * 100.0) / 100.0 : null)
                            .latitude(point != null ? Math.round(point.getY() * 100.0) / 100.0 : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 附近的人搜索降级方案
     * 当 Redis 不可用时，降级查询 PostGIS 数据库
     *
     * @param userId 用户ID
     * @param dto    搜索条件
     * @param t      异常信息
     * @return 附近的人列表
     */
    public List<LocationVo> searchNearbyFallback(String userId, NearbySearchDto dto, Throwable t) {
        log.error("Redis 搜索失败，降级查询数据库 user {}", userId, t);

        // 获取用户最新位置
        LocationHistoryPo lastLoc = locationHistoryMapper.selectOne(
                new QueryWrapper<LocationHistoryPo>()
                        .eq("user_id", userId)
                        .orderByDesc("create_time")
                        .last("LIMIT 1")
        );

        if (lastLoc == null) return Collections.emptyList();

        // 使用 PostGIS 空间查询
        List<LocationHistoryPo> historyList = locationHistoryMapper.searchNearbyHistory(
                lastLoc.getLongitude(), lastLoc.getLatitude(), dto.getRadius(), dto.getLimit());

        return historyList.stream().map(converter::toVo).collect(Collectors.toList());
    }

    /**
     * 定时清理不活跃用户
     * 每 5 分钟执行一次，清理 30 分钟未更新位置的用户缓存
     * 释放 Redis 内存资源
     */
    @Override
    @Scheduled(cron = "0 */5 * * * ?")
    public void clearInactiveUsers() {
        long threshold = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30);
        Set<String> inactiveUsers = redisTemplate.opsForZSet().rangeByScore(USER_LAST_ACTIVE_KEY, 0, threshold);

        if (inactiveUsers == null || inactiveUsers.isEmpty()) {
            return;
        }

        log.info("开始清理 {} 个不活跃用户", inactiveUsers.size());

        for (String userId : inactiveUsers) {
            String partitionKey = (String) redisTemplate.opsForHash().get(USER_PARTITION_KEY, userId);
            if (partitionKey != null) {
                redisTemplate.opsForGeo().remove(partitionKey, userId);
                redisTemplate.opsForHash().delete(USER_PARTITION_KEY, userId);
            }
        }

        // 从活跃度 ZSET 中移除
        redisTemplate.opsForZSet().removeRangeByScore(USER_LAST_ACTIVE_KEY, 0, threshold);
    }
}
