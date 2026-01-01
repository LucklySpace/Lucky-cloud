package com.xy.lucky.platform.service.notify;

import com.xy.lucky.platform.domain.vo.SmsVo;

/**
 * 短信发送接口
 */
public interface SmsSender {

    /**
     * 发送短信
     *
     * @param sms 短信信息
     * @return 发送结果
     */
    Boolean send(SmsVo sms);
}
