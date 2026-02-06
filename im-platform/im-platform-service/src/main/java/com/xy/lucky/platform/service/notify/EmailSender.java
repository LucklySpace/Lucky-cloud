package com.xy.lucky.platform.service.notify;

import com.xy.lucky.platform.domain.vo.EmailVo;

/**
 * 邮件发送接口
 */
public interface EmailSender {

    /**
     * 发送邮件
     *
     * @param email 邮件信息
     * @return 发送结果
     */
    Boolean send(EmailVo email);

}
