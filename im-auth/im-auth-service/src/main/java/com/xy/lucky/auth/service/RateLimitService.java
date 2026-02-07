package com.xy.lucky.auth.service;

public interface RateLimitService {

    boolean allowSmsSend(String phone, String clientIp, String deviceId);
}

