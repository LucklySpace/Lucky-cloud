package com.xy.auth.security.provider;

import com.xy.auth.domain.dto.ImUserDto;
import com.xy.auth.security.token.MobileAuthenticationToken;
import com.xy.auth.service.impl.ImUserServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StringUtils;

/**
 * 手机验证码登录
 */
@Slf4j
public class MobileAuthenticationProvider implements AuthenticationProvider {

    private ImUserServiceImpl userDetailsService;

    public void setUserDetailsService(ImUserServiceImpl userDetailsService) {
        this.userDetailsService = userDetailsService;
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
            throw new BadCredentialsException("phoneNumber is null or empty.");
        }

        if (!StringUtils.hasText(smsCode)) {
            throw new BadCredentialsException("smsCode is null or empty.");
        }

        // 验证手机验证码 并返回用户
        ImUserDto imUserDto = userDetailsService.verifyMobileCode(phoneNumber, smsCode);

        log.info("mobile login success :{}", imUserDto.getUserId());

        return new MobileAuthenticationToken(imUserDto.getUserId(), null);
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return MobileAuthenticationToken.class.isAssignableFrom(aClass);
    }

}
