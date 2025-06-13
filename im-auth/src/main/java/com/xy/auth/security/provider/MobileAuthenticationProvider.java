package com.xy.auth.security.provider;


import com.xy.auth.security.exception.AuthenticationFailException;
import com.xy.auth.security.token.MobileAuthenticationToken;
import com.xy.auth.service.ImUserService;
import com.xy.domain.po.ImUserPo;
import com.xy.response.domain.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StringUtils;

/**
 * 手机验证码登录
 */
@Slf4j
public class MobileAuthenticationProvider implements AuthenticationProvider {

    private ImUserService imUserService;

    public void setUserDetailsService(ImUserService imUserService) {
        this.imUserService = imUserService;
    }


    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        if (authentication.isAuthenticated()) {
            return authentication;
        }

        // 获取手机号码
        String phoneNumber = (String) authentication.getPrincipal();

        // 获取短信验证码
        String smsCode = (String) authentication.getCredentials();

        if (!StringUtils.hasText(phoneNumber)) {
            // phoneNumber is null or empty.
            throw new AuthenticationFailException(ResultCode.VALIDATION_INCOMPLETE);
        }

        if (!StringUtils.hasText(smsCode)) {
            // smsCode is null or empty.
            throw new AuthenticationFailException(ResultCode.VALIDATION_INCOMPLETE);
        }

        // 验证手机验证码 并返回用户
        ImUserPo imUserPo = imUserService.verifyMobileCode(phoneNumber, smsCode);

        log.info("mobile login success :{}", imUserPo.getUserId());

        return new MobileAuthenticationToken(imUserPo.getUserId(), null);
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return MobileAuthenticationToken.class.isAssignableFrom(aClass);
    }

}
