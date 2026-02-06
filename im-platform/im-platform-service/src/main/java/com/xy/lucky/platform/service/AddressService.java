package com.xy.lucky.platform.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xy.lucky.address.AreaUtils;
import com.xy.lucky.address.IPUtils;
import com.xy.lucky.address.domain.Area;
import com.xy.lucky.platform.domain.vo.AreaVo;
import com.xy.lucky.platform.exception.AddressException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;


/**
 * 地址与 IP 查询服务
 * <p>
 * 核心功能：
 * - IP 到地区查询（基于 ip2region）
 * - ID/路径 到地区查询与格式化（基于内存树）
 * - Caffeine 本地缓存：LRU 策略，提升读性能
 * <p>
 */
@Slf4j
@Service
public class AddressService {

    // IP -> Area 缓存（热点 IP，短 TTL）
    private Cache<String, Area> ipAreaCache;

    // ID -> Area 缓存（常用 ID，中等 TTL）
    private Cache<Integer, Area> idAreaCache;

    // Path -> Area 缓存（路径解析，中等 TTL）
    private Cache<String, Area> pathAreaCache;

    // ID -> 格式化地址缓存（格式化结果，中等 TTL）
    private Cache<Integer, String> idFormatCache;

    @PostConstruct
    void initCaches() {
        ipAreaCache = Caffeine.newBuilder()
                .maximumSize(100_000)  // 限制大小，防止 OOM
                .expireAfterWrite(Duration.ofMinutes(10))  // 短期过期，动态 IP 变化
                .build();

        idAreaCache = Caffeine.newBuilder()
                .maximumSize(200_000)
                .expireAfterWrite(Duration.ofMinutes(30))
                .build();

        pathAreaCache = Caffeine.newBuilder()
                .maximumSize(50_000)
                .expireAfterWrite(Duration.ofMinutes(30))
                .build();

        idFormatCache = Caffeine.newBuilder()
                .maximumSize(200_000)
                .expireAfterWrite(Duration.ofMinutes(30))
                .build();
    }

    /**
     * 根据 IP 查询地区信息
     *
     * @param ip IPv4 地址（支持 localhost 转换）
     * @return 对应地区节点，或 null（如果 IP 非法或查询失败）
     */
    public AreaVo getAreaByIp(String ip) {
        String normalized = normalizeIp(ip);
        if (normalized == null) {
            log.warn("非法 IP 输入: {}", ip);
            throw new AddressException("IP 非法");
        }
        try {
            return toVo(ipAreaCache.get(normalized, k -> {
                Area area = IPUtils.getArea(k);
                log.debug("IP 查询: ip={} -> area={}", k, Optional.ofNullable(area).map(Area::getName).orElse("null"));
                return area;
            }));
        } catch (Exception e) {
            log.error("IP 查询失败: ip={}", normalized, e);
            throw new AddressException("IP 查询失败");
        }
    }

    /**
     * 根据 IP 查询地区编号
     *
     * @param ip IPv4 地址
     * @return 地区编号，或 null（如果 IP 非法或查询失败）
     */
    public Integer getAreaIdByIp(String ip) {
        String normalized = normalizeIp(ip);
        if (normalized == null) {
            log.warn("非法 IP 输入: {}", ip);
            throw new AddressException("IP 非法");
        }
        try {
            Integer id = IPUtils.getAreaId(normalized);
            log.debug("IP 查询编号: ip={} -> id={}", normalized, id);
            return id;
        } catch (Exception e) {
            log.error("IP 查询编号失败: ip={}", normalized, e);
            throw new AddressException("IP 查询编号失败");
        }
    }

    /**
     * 根据地区编号查询节点
     *
     * @param id 地区编号（非负整数）
     * @return 地区节点，或 null（如果 ID 非法或查询失败）
     */
    public AreaVo getAreaById(Integer id) {
        if (id == null || id < 0) {
            log.warn("非法 ID 输入: {}", id);
            throw new AddressException("IP 非法");
        }
        return toVo(idAreaCache.get(id, k -> {
            Area area = AreaUtils.getArea(k);
            log.debug("ID 查询: id={} -> area={}", k, Optional.ofNullable(area).map(Area::getName).orElse("null"));
            return area;
        }));
    }

    /**
     * 解析区域路径（如：河南省/郑州市/金水区）
     *
     * @param path 路径字符串
     * @return 地区节点，或 null（如果路径为空或解析失败）
     */
    public AreaVo parseArea(String path) {
        if (!StringUtils.hasText(path)) {
            log.warn("空路径输入");
            throw new AddressException("空路径输入");
        }
        String key = path.trim();
        return toVo(pathAreaCache.get(key, k -> {
            Area area = AreaUtils.parseArea(k);
            log.debug("路径解析: path={} -> area={}", k, Optional.ofNullable(area).map(Area::getName).orElse("null"));
            return area;
        }));
    }

    /**
     * 格式化地区编号为路径字符串
     *
     * @param id 地区编号
     * @return 格式化路径（如：上海/上海市/静安区），或 null（如果 ID 非法或格式化失败）
     */
    public String formatArea(Integer id) {
        if (id == null || id < 0) {
            log.warn("非法 ID 输入: {}", id);
            throw new AddressException("ID 非法");
        }
        return idFormatCache.get(id, k -> {
            String formatted = AreaUtils.format(k, "");
            log.debug("ID 格式化: id={} -> {}", k, formatted);
            return formatted;
        });
    }

    /**
     * 将 Area 转换为 AreaVo（包含拼接地址）
     *
     * @param area 地区节点
     * @return AreaVo，或 null（如果 area 为 null）
     */
    public AreaVo toVo(Area area) {
        return Optional.ofNullable(area)
                .map(a -> new AreaVo()
                        .setCode(a.getCode())
                        .setName(a.getName())
                        .setAddress(buildAddress(a)))
                .orElse(null);
    }

    /**
     * 构建地址路径：从当前节点向上遍历父节点，过滤 type > 0，按 type 升序拼接
     *
     * @param area 起始节点
     * @return 拼接地址字符串（如：省 / 市 / 区）
     */
    private String buildAddress(Area area) {
        List<Area> chain = new ArrayList<>();
        Area current = area;
        int guard = 0;  // 防止无限循环（树深度异常）
        while (current != null && guard++ < 64) {
            chain.add(current);
            current = current.getParent();
        }

        // 过滤 type > 0，按 type 升序排序
        return chain.stream()
                .filter(node -> Optional.ofNullable(node.getType()).orElse(0) > 0)
                .sorted(Comparator.comparingInt(node -> Optional.ofNullable(node.getType()).orElse(0)))
                .map(Area::getName)
                .reduce((a, b) -> a + b)
                .orElse("");
    }

    /**
     * 归一化 IP 输入（trim、localhost 转换）
     *
     * @param ip 输入 IP
     * @return 归一化 IP，或 null（如果无效）
     */
    private String normalizeIp(String ip) {
        return Optional.ofNullable(ip)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(v -> "localhost".equalsIgnoreCase(v) ? "127.0.0.1" : v)
                .orElse(null);
    }
}