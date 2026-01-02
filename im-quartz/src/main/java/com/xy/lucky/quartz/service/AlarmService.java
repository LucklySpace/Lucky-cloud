package com.xy.lucky.quartz.service;

public interface AlarmService {
    /**
     * 发送报警
     *
     * @param email   接收人邮箱
     * @param subject 主题
     * @param content 内容
     * @return 是否发送成功
     */
    boolean sendAlarm(String email, String subject, String content);
}
