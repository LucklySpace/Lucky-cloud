package com.xy.lucky.auth.service.impl;

import com.xy.lucky.auth.config.SmsCodeProperties;
import com.xy.lucky.auth.service.RateLimitService;
import com.xy.lucky.auth.service.SmsCodeService;
import com.xy.lucky.auth.service.SmsService;
import com.zhenzi.sms.ZhenziSmsClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


@Slf4j
@Service
@RefreshScope
public class SmsServiceImpl implements SmsService {

    private static final String TEMPLATE_ID = "10120";

    @Value("${sms.apiUrl}")
    private String apiUrl;

    @Value("${sms.appId}")
    private String appId;

    @Value("${sms.appSecret}")
    private String appSecret;

    @Resource
    private SmsCodeService smsCodeService;
    @Resource
    private RateLimitService rateLimitService;
    @Resource
    private SmsCodeProperties smsCodeProperties;

    @Override
    public Boolean sendMessage(String phone, String clientIp, String deviceId) {
        rateLimitService.allowSmsSend(phone, clientIp, deviceId);

        String plainCode = smsCodeService.generateAndStore(phone, clientIp, deviceId);
        try {
            sendSms(phone, plainCode);
        } catch (Exception e) {
            log.error("短信发送失败，手机号: {}, 错误信息: {}", phone, e.getMessage());
            smsCodeService.deleteCode(phone);
            return false;
        }
        return true;
    }

    private void sendSms(String phone, String plainCode) throws Exception {
        ZhenziSmsClient client = new ZhenziSmsClient(apiUrl, appId, appSecret);
        Map<String, Object> params = new HashMap<>();
        params.put("number", phone);
        params.put("templateId", TEMPLATE_ID);
        long minutes = Math.max(1, smsCodeProperties.getTtl().toMinutes());
        String[] templateParams = {plainCode, minutes + "分钟"};
        params.put("templateParams", templateParams);
        client.send(params);
    }
}
