package com.xy.lucky.platform.service.notify.provider;

import com.xy.lucky.platform.config.SmsProperties;
import com.xy.lucky.platform.domain.vo.SmsVo;
import com.xy.lucky.platform.service.notify.SmsSender;
import com.zhenzi.sms.ZhenziSmsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class ZhenziSmsSender implements SmsSender {

    private final SmsProperties.Zhenzi zhenzi;

    @Override
    public Boolean send(SmsVo sms) {
        try {
            ZhenziSmsClient client = new ZhenziSmsClient(zhenzi.getApiUrl(), zhenzi.getAppId(), zhenzi.getAppSecret());
            Map<String, Object> params = new HashMap<>();
            params.put("number", sms.getPhone());
            String templateId = sms.getTemplateId() != null ? sms.getTemplateId() : zhenzi.getDefaultTemplateId();
            params.put("templateId", templateId);
            if (sms.getTemplateParams() != null) {
                params.put("templateParams", sms.getTemplateParams().toArray(new String[0]));
            }
            client.send(params);
            log.info("短信已发送至 {}", sms.getPhone());
            return true;
        } catch (Exception e) {
            log.error("短信发送失败: {}", e.getMessage(), e);
            return false;
        }
    }
}
