package com.xy.lucky.auth.security.provider;

import com.xy.lucky.auth.security.helper.CryptoHelper;
import com.xy.lucky.auth.security.token.MobileAuthenticationToken;
import com.xy.lucky.auth.utils.RedisCache;
import com.xy.lucky.core.constants.IMConstant;
import com.xy.lucky.domain.po.ImUserPo;
import com.xy.lucky.dubbo.web.api.database.user.ImUserDubboService;
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
 * 手机验证码认证提供者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MobileAuthenticationProvider implements AuthenticationProvider {

    @DubboReference
    private ImUserDubboService imUserDubboService;

    private final CryptoHelper cryptoHelper;
    private final RedisCache redisCache;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (authentication.isAuthenticated()) {
            return authentication;
        }

        String phoneNumber = (String) authentication.getPrincipal();
        String encryptedSmsCode = (String) authentication.getCredentials();

        validateInput(phoneNumber, encryptedSmsCode);
        validateSmsCode(phoneNumber, encryptedSmsCode);

        ImUserPo user = getUserByPhoneNumber(phoneNumber);
        if (Objects.isNull(user)) {
            log.warn("用户不存在: phone={}", phoneNumber);
            throw new AuthenticationFailException(ResultCode.ACCOUNT_NOT_FOUND);
        }

        log.info("手机号登录成功: userId={}", user.getUserId());
        return new MobileAuthenticationToken(user.getUserId(), null);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return MobileAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private void validateInput(String phoneNumber, String smsCode) {
        if (!StringUtils.hasText(phoneNumber) || !StringUtils.hasText(smsCode)) {
            throw new AuthenticationFailException(ResultCode.VALIDATION_INCOMPLETE);
        }
    }

    private void validateSmsCode(String phoneNumber, String encryptedSmsCode) {
        String cacheKey = IMConstant.SMS_KEY_PREFIX + phoneNumber;
        String cachedCode = redisCache.get(cacheKey);

        if (cachedCode == null) {
            log.warn("验证码不存在或已过期: phone={}", phoneNumber);
            throw new AuthenticationFailException(ResultCode.CAPTCHA_ERROR);
        }

        String decryptedCode = cryptoHelper.decrypt(encryptedSmsCode);
        if (!decryptedCode.equals(cachedCode)) {
            log.warn("验证码错误: phone={}", phoneNumber);
            throw new AuthenticationFailException(ResultCode.CAPTCHA_ERROR);
        }

        redisCache.del(cacheKey);
    }

    private ImUserPo getUserByPhoneNumber(String phoneNumber) {
        return imUserDubboService.queryOneByMobile(phoneNumber);
    }
}

