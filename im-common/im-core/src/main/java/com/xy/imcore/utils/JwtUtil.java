package com.xy.imcore.utils;


import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.json.JSONObject;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTPayload;
import cn.hutool.jwt.JWTUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * jwt工具类
 */
public class JwtUtil {

    /**
     * 盐值很重要，不能泄漏，且每个项目都应该不一样，可以放到配置文件中
     */
    private static final String KEY = "Jiawa12306";

    /**
     * 创建token
     *
     * @param username
     * @return
     */
    public static String createToken(String username, Integer time, DateField datePart) {
        DateTime now = DateTime.now();
        DateTime expTime = now.offsetNew(datePart, time);
        Map<String, Object> payload = new HashMap<>();
        // 签发时间
        payload.put(JWTPayload.ISSUED_AT, now);
        // 过期时间
        payload.put(JWTPayload.EXPIRES_AT, expTime);
        // 生效时间
        payload.put(JWTPayload.NOT_BEFORE, now);
        // 内容
        payload.put("username", username);
        String token = JWTUtil.createToken(payload, KEY.getBytes());
        // log.info("生成JWT token：{}", token);
        return token;
    }

    /**
     * 校验token
     *
     * @param token
     * @return
     */
    public static boolean validate(String token) {
        JWT jwt = JWTUtil.parseToken(token);

        boolean verifyKey = jwt.setKey(KEY.getBytes()).verify();

        //validate包含了verify
        boolean validateTime = jwt.validate(0);

        // ("JWT token校验结果：{}", validateTime);
        return verifyKey && validateTime;
    }

    public static String getUsername(String token) {
        JWT jwt = JWTUtil.parseToken(token).setKey(KEY.getBytes());
        JSONObject payloads = jwt.getPayloads();
        return payloads.get("username").toString();
    }

    public static JSONObject getJSONObject(String token) {
        JWT jwt = JWTUtil.parseToken(token).setKey(KEY.getBytes());
        JSONObject payloads = jwt.getPayloads();
        payloads.remove(JWTPayload.ISSUED_AT);
        payloads.remove(JWTPayload.EXPIRES_AT);
        payloads.remove(JWTPayload.NOT_BEFORE);
        // log.info("根据token获取原始内容：{}", payloads);
        return payloads;
    }


}

