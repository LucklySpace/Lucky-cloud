package com.xy.auth.security.provider;

import com.xy.auth.domain.dto.ImUserDto;
import com.xy.auth.security.token.QrScanAuthenticationToken;
import com.xy.auth.service.impl.ImUserServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;


@Slf4j
public class QrScanAuthenticationProvider implements AuthenticationProvider {

    private ImUserServiceImpl userDetailsService;

    public void setUserDetailsService(ImUserServiceImpl userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        if (authentication.isAuthenticated()) {
            return authentication;
        }
        // 前端标识
        String qrcode = (String) authentication.getPrincipal();
        // 用户名
        String password = (String) authentication.getCredentials();

        ImUserDto imUserDto = userDetailsService.verifyQrPassword(qrcode, password);

        //将权限装入框架验证
        return new QrScanAuthenticationToken(imUserDto.getUserId(), null);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return QrScanAuthenticationToken.class.isAssignableFrom(authentication);
    }

}
