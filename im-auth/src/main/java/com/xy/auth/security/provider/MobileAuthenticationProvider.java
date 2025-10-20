package com.xy.auth.security.provider;


import com.xy.auth.security.IMRSAKeyProperties;
import com.xy.auth.security.exception.AuthenticationFailException;
import com.xy.auth.security.token.MobileAuthenticationToken;
import com.xy.auth.utils.RSAUtil;
import com.xy.auth.utils.RedisCache;
import com.xy.core.constants.IMConstant;
import com.xy.domain.po.ImUserPo;
import com.xy.dubbo.api.database.user.ImUserDubboService;
import com.xy.general.response.domain.ResultCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 手机验证码登录
 */
@Slf4j
@Component
public class MobileAuthenticationProvider implements AuthenticationProvider {

    @DubboReference
    private ImUserDubboService imUserDubboService;

    @Resource
    private IMRSAKeyProperties IMRSAKeyProperties;

    @Resource
    private RedisCache redisCache;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        // 检查是否已认证
        if (authentication.isAuthenticated()) {
            return authentication;
        }

        // 获取手机号码和验证码
        String phoneNumber = (String) authentication.getPrincipal();
        String smsCode = (String) authentication.getCredentials();

        // 校验参数完整性
        validateInput(phoneNumber, smsCode);

        // 校验短信验证码
        if (!validateSmsCode(phoneNumber, smsCode)) {
            throw new AuthenticationFailException(ResultCode.CAPTCHA_ERROR);
        }

        // 查询用户信息
        ImUserPo user = getUserByPhoneNumber(phoneNumber);

        // 如果用户不存在，抛出异常
        if (Objects.isNull(user)) {
            log.warn("Account not found for phone number: {}", phoneNumber);
            throw new AuthenticationFailException(ResultCode.ACCOUNT_NOT_FOUND);
        }

        log.info("Mobile login success: {}", user.getUserId());

        // 返回认证信息
        return new MobileAuthenticationToken(user.getUserId(), null);
    }

    /**
     * 校验输入的手机号码和短信验证码是否有效
     *
     * @param phoneNumber 手机号码
     * @param smsCode     短信验证码
     */
    private void validateInput(String phoneNumber, String smsCode) {

        if (!StringUtils.hasText(phoneNumber)) {
            throw new AuthenticationFailException(ResultCode.VALIDATION_INCOMPLETE);
        }

        if (!StringUtils.hasText(smsCode)) {
            throw new AuthenticationFailException(ResultCode.VALIDATION_INCOMPLETE);
        }
    }

    /**
     * 校验短信验证码是否正确
     *
     * @param phoneNumber 手机号码
     * @param smsCode     加密后的短信验证码
     * @return true: 校验通过，false: 校验失败
     */
    private boolean validateSmsCode(String phoneNumber, String smsCode) {

        String redisCacheSmsCode = redisCache.get(IMConstant.SMS_KEY_PREFIX + phoneNumber);

        if (redisCacheSmsCode == null) {
            // 没有缓存的验证码，表示验证码已过期或未发送
            throw new AuthenticationFailException(ResultCode.NOT_FOUND);
        }

        // 解密并验证验证码
        String decryptSmsCode = decryptPassword(smsCode);

        if (!decryptSmsCode.equals(redisCacheSmsCode)) {
            return false;
        }

        // 验证通过后删除缓存中的验证码
        redisCache.del(IMConstant.SMS_KEY_PREFIX + phoneNumber);

        return true;
    }

    /**
     * 通过手机号获取用户信息
     *
     * @param phoneNumber 手机号码
     * @return 用户信息
     */
    private ImUserPo getUserByPhoneNumber(String phoneNumber) {
        return imUserDubboService.selectOneByMobile(phoneNumber);
    }

    /**
     * 使用 RSA 解密短信验证码
     *
     * @param password 加密的验证码
     * @return 解密后的验证码
     */
    public String decryptPassword(String password) {
        try {
            // Base64 编码时使用加号，URL 中的加号会被当成空格，需要替换
            String str = password.replaceAll(" ", "+");

            return RSAUtil.decrypt(str, IMRSAKeyProperties.getPrivateKeyStr());

        } catch (Exception e) {

            // 解密失败，抛出认证失败异常
            log.error("Failed to decrypt SMS code", e);

            throw new AuthenticationFailException(ResultCode.INVALID_CREDENTIALS);
        }
    }


    @Override
    public boolean supports(Class<?> authentication) {
        return MobileAuthenticationToken.class.isAssignableFrom(authentication);
    }
}