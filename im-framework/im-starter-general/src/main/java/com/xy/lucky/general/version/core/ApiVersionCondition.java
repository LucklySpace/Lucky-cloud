package com.xy.lucky.general.version.core;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.mvc.condition.RequestCondition;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自定义 API 版本匹配条件（用于路由版本控制）
 * 实现 Spring MVC 的 RequestCondition 接口
 */
public record ApiVersionCondition(String apiVersion) implements RequestCondition<ApiVersionCondition> {

    /**
     * 匹配请求 URI 中的版本前缀（如 /v1/xxx 或 /v1.2/xxx）如: /v[1-n]/api/test or /v1.5/home/api
     * 支持整数和小数版本号
     */
    private static final Pattern VERSION_PREFIX_PATTERN = Pattern.compile("/v(\\d+(?:\\.\\d+)*)/");

    /**
     * 合并不同来源（类和方法）的条件，优先使用方法上的定义覆盖类上定义
     *
     * @param other 另一个版本条件
     * @return 合并后的版本条件
     */
    @Override
    public ApiVersionCondition combine(ApiVersionCondition other) {
        // 方法上的版本注解优先级高于类上的版本注解
        return new ApiVersionCondition(other.apiVersion());
    }

    /**
     * 当前条件是否与请求匹配（根据请求 URI 中的版本号匹配当前定义的版本号）
     * 若匹配成功，则返回当前条件；否则返回 null，表示不匹配
     *
     * @param request HTTP请求
     * @return 匹配的条件或null
     */
    @Override
    public ApiVersionCondition getMatchingCondition(HttpServletRequest request) {
        Matcher matcher = VERSION_PREFIX_PATTERN.matcher(request.getRequestURI());
        if (matcher.find()) {
            String requestVersion = matcher.group(1);
            // Match when requestVersion >= apiVersion.
            if (compareVersions(requestVersion, this.apiVersion) >= 0) {
                return this;
            }
        }
        return null;
    }

    /**
     * 当多个版本匹配时，比较优先级。用于排序选择最近的版本
     * 版本号越大，优先级越高
     *
     * @param other   其他版本条件
     * @param request HTTP请求
     * @return 比较结果
     */
    @Override
    public int compareTo(ApiVersionCondition other, HttpServletRequest request) {
        // 版本号越大，优先级越高
        int cmp = compareVersions(this.apiVersion, other.apiVersion());
        if (cmp > 0) {
            return -1;
        }
        if (cmp < 0) {
            return 1;
        }
        return 0;
    }

    /**
     * Compare two version strings.
     * Returns a negative number if v1 &lt; v2, zero if equal, positive if v1 &gt; v2.
     * This method is tolerant to whitespace, leading 'v'/'V', and non-numeric suffixes.
     */
    private static int compareVersions(String v1, String v2) {
        int[] p1 = parseVersionParts(v1);
        int[] p2 = parseVersionParts(v2);

        int len = Math.max(p1.length, p2.length);
        for (int i = 0; i < len; i++) {
            int a = i < p1.length ? p1[i] : 0;
            int b = i < p2.length ? p2[i] : 0;
            if (a != b) {
                return Integer.compare(a, b);
            }
        }
        return 0;
    }

    /**
     * Parse version into integer parts.
     * <p>
     * Examples:
     * - "1" -&gt; [1]
     * - "v1.2.3" -&gt; [1, 2, 3]
     * - " 1.2-beta " -&gt; [1, 2]
     * - null/blank/invalid -&gt; [0]
     */
    private static int[] parseVersionParts(String version) {
        if (version == null) {
            return new int[]{0};
        }
        String s = version.trim();
        if (s.isEmpty()) {
            return new int[]{0};
        }

        // Drop leading 'v' or 'V' if present.
        char first = s.charAt(0);
        if (first == 'v' || first == 'V') {
            s = s.substring(1).trim();
            if (s.isEmpty()) {
                return new int[]{0};
            }
        }

        // Keep only the leading numeric-and-dot prefix, e.g. "1.2.3-rc1" -> "1.2.3".
        int end = 0;
        while (end < s.length()) {
            char c = s.charAt(end);
            if ((c >= '0' && c <= '9') || c == '.') {
                end++;
            } else {
                break;
            }
        }
        s = s.substring(0, end);

        // Remove trailing dots, e.g. "1.2." -> "1.2"
        while (s.endsWith(".")) {
            s = s.substring(0, s.length() - 1);
        }
        if (s.isEmpty()) {
            return new int[]{0};
        }

        String[] parts = s.split("\\.");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part == null || part.isEmpty()) {
                out[i] = 0;
                continue;
            }
            try {
                // Clamp extremely large numbers to Integer.MAX_VALUE.
                long val = Long.parseLong(part);
                if (val > Integer.MAX_VALUE) {
                    val = Integer.MAX_VALUE;
                }
                if (val < 0) {
                    val = 0;
                }
                out[i] = (int) val;
            } catch (NumberFormatException ex) {
                out[i] = 0;
            }
        }
        return out;
    }
}