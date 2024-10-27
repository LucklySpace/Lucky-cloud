package com.xy.auth.service;

public interface SmsService {

    String sendMessage(String phoneNum) throws Exception;
}
