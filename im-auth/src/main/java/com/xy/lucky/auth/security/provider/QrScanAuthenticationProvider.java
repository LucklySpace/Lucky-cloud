package com.xy.lucky.auth.security.provider;

import com.xy.lucky.api.user.ImUserDubboService;
import com.xy.lucky.auth.domain.QRCode;
import com.xy.lucky.auth.security.token.QrScanAuthenticationToken;
import com.xy.lucky.auth.utils.RedisCache;
import com.xy.lucky.core.constants.IMConstant;
import com.xy.lucky.domain.po.ImUserPo;
import com.xy.lucky.general.response.domain.ResultCode;
import com.xy.lucky.security.exception.AuthenticationFailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 二维码扫码认证提供者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QrScanAuthenticationProvider implements AuthenticationProvider {

    @DubboReference
    private ImUserDubboService imUserDubboService;

    private final RedisCache redisCache;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (authentication.isAuthenticated()) {
            return authentication;
        }

        String qrCodeId = (String) authentication.getPrincipal();
        String password = (String) authentication.getCredentials();

        validateInput(qrCodeId, password);
        QRCode qrCodeInfo = getQrCodeInfo(qrCodeId);
        ImUserPo user = getUserFromQrCode(qrCodeInfo);

        log.info("二维码登录成功: userId={}", user.getUserId());
        return new QrScanAuthenticationToken(user.getUserId(), null);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return QrScanAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private void validateInput(String qrCodeId, String password) {
        if (!StringUtils.hasText(qrCodeId)) {
            throw new AuthenticationFailException(ResultCode.QRCODE_IS_INVALID);
        }
        if (!StringUtils.hasText(password)) {
            throw new AuthenticationFailException(ResultCode.INVALID_CREDENTIALS);
        }
    }

    private QRCode getQrCodeInfo(String qrCodeId) {
        String redisKey = IMConstant.QRCODE_KEY_PREFIX + qrCodeId;
        if (!redisCache.hasKey(redisKey)) {
            log.warn("二维码无效或已过期: qrCodeId={}", qrCodeId);
            throw new AuthenticationFailException(ResultCode.QRCODE_IS_INVALID);
        }
        return redisCache.get(redisKey);
    }

    private ImUserPo getUserFromQrCode(QRCode qrCodeInfo) {
        String userId = qrCodeInfo.getUserId();
        if (!StringUtils.hasText(userId)) {
            log.error("二维码信息中缺少用户ID");
            throw new AuthenticationFailException(ResultCode.ACCOUNT_NOT_FOUND);
        }

        ImUserPo user = imUserDubboService.queryOne(userId);
        if (Objects.isNull(user)) {
            log.warn("用户不存在: userId={}", userId);
            throw new AuthenticationFailException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        return user;
    }
}