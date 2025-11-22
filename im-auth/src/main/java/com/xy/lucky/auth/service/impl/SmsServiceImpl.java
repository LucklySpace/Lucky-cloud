package com.xy.lucky.auth.service.impl;

import com.xy.lucky.auth.security.exception.AuthenticationFailException;
import com.xy.lucky.auth.service.SmsService;
import com.xy.lucky.auth.utils.RedisCache;
import com.xy.lucky.general.response.domain.ResultCode;
import com.zhenzi.sms.ZhenziSmsClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
@RefreshScope
public class SmsServiceImpl implements SmsService {

    // 短信模板ID和验证码过期时间抽取为常量
    private static final String TEMPLATE_ID = "10120";
    private static final int EXPIRE_MINUTES = 3;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${sms.apiUrl}")
    private String apiUrl;

    @Value("${sms.appId}")
    private String appId;

    @Value("${sms.appSecret}")
    private String appSecret;

    @Resource
    private RedisCache redisCache;

    @Override
    public String sendMessage(String phone) throws Exception {

        // 检查手机号是否在 Redis 中存在，防止频繁请求
        if (StringUtils.hasText(redisCache.get(phone))) {
            log.error("请求频繁，手机号: {}", phone);
            throw new AuthenticationFailException("请求频繁，请稍后再试");
        }

        // 生成六位随机验证码
        String randomCode = generateRandomCode();

        // 将验证码存入 Redis，设置过期时间为 EXPIRE_MINUTES 分钟
        redisCache.set("sms" + phone, randomCode, EXPIRE_MINUTES, TimeUnit.MINUTES);
        log.info("手机号: {} 生成六位随机验证码: {}", phone, randomCode);

        return sendSms(phone, randomCode);
    }

    // 抽取生成6位随机验证码的方法
    public  String generateRandomCode() {
        int number = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", number);
    }

    // 抽取发送短信的逻辑为独立的方法
    private String sendSms(String phone, String randomCode) throws Exception {
        try {
            // 初始化短信客户端
            ZhenziSmsClient client = new ZhenziSmsClient(apiUrl, appId, appSecret);

            // 设置短信参数
            Map<String, Object> params = new HashMap<>();
            params.put("number", phone);  // 收件人手机号
            params.put("templateId", TEMPLATE_ID);  // 短信模板ID

            // 设置模板参数
            String[] templateParams = {randomCode, EXPIRE_MINUTES + "分钟"};
            params.put("templateParams", templateParams);

            // 发送短信并返回结果
            return client.send(params);
        } catch (Exception e) {
            log.error("短信发送失败，手机号: {}, 错误信息: {}", phone, e.getMessage());
            throw new AuthenticationFailException(ResultCode.SMS_ERROR);
        }
    }
}