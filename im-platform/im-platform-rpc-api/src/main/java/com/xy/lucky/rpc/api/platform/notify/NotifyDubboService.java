package com.xy.lucky.rpc.api.platform.notify;

import com.xy.lucky.rpc.api.platform.dto.EmailDto;
import com.xy.lucky.rpc.api.platform.dto.SmsDto;

/**
 * 通知服务 Dubbo 接口
 *
 * @author Lucky Platform
 * @since 1.0.0
 */
public interface NotifyDubboService {

    /**
     * 发送邮件
     *
     * @param email 邮件参数
     * @return 是否发送成功
     */
    Boolean sendEmail(EmailDto email);

    /**
     * 发送短信
     *
     * @param sms 短信参数
     * @return 是否发送成功
     */
    Boolean sendSms(SmsDto sms);
}
