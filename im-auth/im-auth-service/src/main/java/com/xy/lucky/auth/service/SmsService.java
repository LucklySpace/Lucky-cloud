package com.xy.lucky.auth.service;

public interface SmsService {

    Boolean sendMessage(String phoneNum, String clientIp, String deviceId) throws Exception;
}
