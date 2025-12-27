package com.xy.lucky.auth.service;


import com.xy.lucky.auth.domain.IMLoginRequest;
import com.xy.lucky.auth.domain.IMLoginResult;
import com.xy.lucky.auth.domain.IMQRCodeResult;
import com.xy.lucky.domain.vo.UserVo;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * @author dense
 */
public interface AuthService {

    IMLoginResult login(IMLoginRequest imLoginRequest);

    UserVo info(String userId);

    Boolean isOnline(String userId);

    Map<String, String> refreshToken(HttpServletRequest request);

    IMQRCodeResult generateQRCode(String qrCodeId);

    IMQRCodeResult scanQRCode(Map<String, String> payload);

    IMQRCodeResult getQRCodeStatus(String qrCodeId);

    Map<String, String> getPublicKey();

    String sendSms(String phone);
}
