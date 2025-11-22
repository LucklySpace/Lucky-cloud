package com.xy.lucky.auth.service;


import com.xy.lucky.auth.domain.IMLoginRequest;
import com.xy.lucky.general.response.domain.Result;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * @author dense
 */
public interface AuthService {

    Result<?> login(IMLoginRequest imLoginRequest);

    Result<?> info(String userId);

    Result<?> isOnline(String userId);

    Result<?> refreshToken(HttpServletRequest request);

    Result<?> generateQRCode(String qrCodeId);

    Result<?> scanQRCode(Map<String, String> payload);

    Result<?> getQRCodeStatus(String qrCodeId);

    Result<?> getPublicKey();

    Result<?> sendSms(String phone);
}
