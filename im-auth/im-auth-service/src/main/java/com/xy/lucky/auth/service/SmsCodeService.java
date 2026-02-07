package com.xy.lucky.auth.service;

public interface SmsCodeService {

    String generateAndStore(String phone, String clientIp, String deviceId);

    VerifyResult verifyAndConsume(String phone, String plainCode, String deviceId, String clientIp);

    void deleteCode(String phone);

    enum VerifyResult {
        OK,
        NOT_FOUND,
        WRONG_CODE,
        DEVICE_MISMATCH,
        IP_MISMATCH,
        LOCKED
    }
}
