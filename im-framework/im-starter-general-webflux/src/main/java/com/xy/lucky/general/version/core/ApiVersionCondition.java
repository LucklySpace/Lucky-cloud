package com.xy.lucky.general.version.core;

import org.springframework.web.reactive.result.condition.RequestCondition;
import org.springframework.web.server.ServerWebExchange;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自定义 API 版本匹配条件（用于路由版本控制）
 * 实现 Spring WebFlux 的 RequestCondition 接口
 */
public record ApiVersionCondition(String apiVersion) implements RequestCondition<ApiVersionCondition> {

    /**
     * 匹配请求 URI 中的版本前缀（如 /v1/xxx 或 /v1.2/xxx）如: /v[1-n]/api/test or /v1.5/home/api
     * 支持整数和小数版本号
     */
    private static final Pattern VERSION_PREFIX_PATTERN = Pattern.compile("/v((\\d+\\.\\d+)|(\\d+))/");

    /**
     * 合并不同来源（类和方法）的条件，优先使用方法上的定义覆盖类上定义
     *
     * @param other 另一个版本条件
     * @return 合并后的版本条件
     */
    @Override
    public ApiVersionCondition combine(ApiVersionCondition other) {
        return new ApiVersionCondition(other.apiVersion());
    }

    /**
     * 当前条件是否与请求匹配（根据请求 URI 中的版本号匹配当前定义的版本号）
     *
     * @param exchange ServerWebExchange
     * @return 匹配的条件或null
     */
    @Override
    public ApiVersionCondition getMatchingCondition(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        Matcher matcher = VERSION_PREFIX_PATTERN.matcher(path);
        if (matcher.find()) {
            String requestVersion = matcher.group(1);
            if (compareVersion(requestVersion, this.apiVersion)) {
                return this;
            }
        }
        return null;
    }

    /**
     * 当多个版本匹配时，比较优先级。用于排序选择最近的版本
     * 版本号越大，优先级越高
     *
     * @param other    其他版本条件
     * @param exchange ServerWebExchange
     * @return 比较结果
     */
    @Override
    public int compareTo(ApiVersionCondition other, ServerWebExchange exchange) {
        return compareVersion(this.apiVersion, other.apiVersion()) ? -1 : 1;
    }

    /**
     * 版本比较方法：返回 true 表示 version1 >= version2
     */
    private boolean compareVersion(String version1, String version2) {
        String[] parts1 = normalizeVersion(version1).split("\\.");
        String[] parts2 = normalizeVersion(version2).split("\\.");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int v1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int v2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (v1 < v2) {
                return false;
            }
            if (v1 > v2) {
                return true;
            }
        }
        return true;
    }

    /**
     * 统一版本号格式（如 "1" => "1.0"）
     */
    private String normalizeVersion(String version) {
        return version.contains(".") ? version : version + ".0";
    }
}
