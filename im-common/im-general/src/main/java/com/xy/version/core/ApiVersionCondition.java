package com.xy.version.core;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.web.servlet.mvc.condition.RequestCondition;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自定义 API 版本匹配条件（用于路由版本控制）
 * 实现 Spring MVC 的 RequestCondition 接口
 *
 * @param apiVersion 当前控制器或方法声明的 API 版本（如 v1 或 v1.5）
 */

public record ApiVersionCondition(String apiVersion) implements RequestCondition<ApiVersionCondition> {

    /**
     * 匹配请求 URI 中的版本前缀（如 /v1/xxx 或 /v1.2/xxx）如: /v[1-n]/api/test or /v1.5/home/api
     * 支持整数和小数版本号
     */
    private final static Pattern VERSION_PREFIX_PATTERN = Pattern.compile("/v((\\d+\\.\\d+)|(\\d+))/");

    /**
     * 合并不同来源（类和方法）的条件，优先使用方法上的定义覆盖类上定义
     */
    @Override
    public ApiVersionCondition combine(ApiVersionCondition other) {
        return new ApiVersionCondition(other.apiVersion());
    }

    /**
     * 当前条件是否与请求匹配（根据请求 URI 中的版本号匹配当前定义的版本号）
     * 若匹配成功，则返回当前条件；否则返回 null，表示不匹配
     */
    @Override
    public ApiVersionCondition getMatchingCondition(HttpServletRequest request) {
        Matcher matcher = VERSION_PREFIX_PATTERN.matcher(request.getRequestURI());
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
     */
    @Override
    public int compareTo(ApiVersionCondition other, HttpServletRequest request) {
        return compareVersion(this.apiVersion, other.apiVersion()) ? -1 : 1;
    }

    /**
     * 版本比较方法：返回 true 表示 version1 >= version2
     * 支持整数和小数版本，如 v1 < v1.5 < v2
     */
    private boolean compareVersion(String version1, String version2) {
        String[] parts1 = normalizeVersion(version1).split("\\.");
        String[] parts2 = normalizeVersion(version2).split("\\.");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int v1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int v2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (v1 < v2) return false;
            if (v1 > v2) return true;
        }
        return true; // 版本号相等
    }

    /**
     * 统一版本号格式（如 "1" => "1.0"）
     */
    private String normalizeVersion(String version) {
        return version.contains(".") ? version : version + ".0";
    }

}