package com.xy.lucky.gateway.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

@Slf4j
public class SignUtils {

    /**
     * 计算签名：过滤掉复杂对象，只保留 string/number/boolean 字符串表示
     */
    public static String calculateSign(Map<String, ?> params, String appSecret) {
        // 1. TreeMap 排序
        SortedMap<String, String> sortedParams = new TreeMap<>();
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            String key = entry.getKey();
            if ("sign".equalsIgnoreCase(key)) continue;

            Object value = entry.getValue();
            if (value instanceof String || value instanceof Number || value instanceof Boolean) {
                sortedParams.put(key, String.valueOf(value));
            }
        }

        // 2. 拼接字符串 key1=val1&key2=val2&...&appSecret=xxx
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        sb.append("appSecret=").append(appSecret);

        // 3. 打印调试用
        log.info("后端签名前 sb: " + sb);

        // 4. MD5 转大写
        return DigestUtils.md5DigestAsHex(sb.toString().getBytes(StandardCharsets.UTF_8)).toUpperCase();
    }
}