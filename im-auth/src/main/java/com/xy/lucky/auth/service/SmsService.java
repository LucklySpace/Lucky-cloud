package com.xy.lucky.auth.service;

public interface SmsService {

    String sendMessage(String phoneNum) throws Exception;
}
