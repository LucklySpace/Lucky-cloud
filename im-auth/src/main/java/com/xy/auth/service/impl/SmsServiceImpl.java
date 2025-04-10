package com.xy.auth.service.impl;

import com.xy.auth.security.exception.AuthenticationFailException;
import com.xy.auth.service.SmsService;
import com.xy.auth.utils.RedisUtil;
import com.zhenzi.sms.ZhenziSmsClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.xy.auth.response.ResultCode.SMS_ERROR;

@Slf4j
@Service
@RefreshScope
public class SmsServiceImpl implements SmsService {

    // 短信模板ID和验证码过期时间抽取为常量
    private static final String TEMPLATE_ID = "10120";
    private static final int EXPIRE_MINUTES = 3;

    @Value("${sms.apiUrl}")
    private String apiUrl;

    @Value("${sms.appId}")
    private String appId;

    @Value("${sms.appSecret}")
    private String appSecret;

    @Resource
    private RedisUtil redisUtil;

    @Override
    public String sendMessage(String phone) throws Exception {

        // 检查手机号是否在 Redis 中存在，防止频繁请求
        if (StringUtils.hasText(redisUtil.get(phone))) {
            log.error("请求频繁，手机号: {}", phone);
            throw new AuthenticationFailException("请求频繁，请稍后再试");
        }

        // 生成六位随机验证码
        String randomCode = generateRandomCode();

        // 将验证码存入 Redis，设置过期时间为 EXPIRE_MINUTES 分钟
        redisUtil.set("sms" + phone, randomCode, EXPIRE_MINUTES, TimeUnit.MINUTES);
        log.info("手机号: {} 生成六位随机验证码: {}", phone, randomCode);

        // 发送短信
        String sendResult = sendSms(phone, randomCode);

        return sendResult;
    }

    // 抽取生成随机验证码的方法
    private String generateRandomCode() {
        return String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
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
            throw new AuthenticationFailException(SMS_ERROR);
        }
    }
}