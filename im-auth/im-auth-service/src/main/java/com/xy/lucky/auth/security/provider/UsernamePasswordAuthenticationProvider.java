package com.xy.lucky.auth.security.provider;

import com.xy.lucky.auth.security.helper.CryptoHelper;
import com.xy.lucky.domain.po.ImUserPo;
import com.xy.lucky.general.response.domain.ResultCode;
import com.xy.lucky.rpc.api.database.user.ImUserDubboService;
import com.xy.lucky.security.exception.AuthenticationFailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 用户名密码认证提供者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsernamePasswordAuthenticationProvider implements AuthenticationProvider {

    @DubboReference
    private ImUserDubboService imUserDubboService;

    private final CryptoHelper cryptoHelper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String userId = (String) authentication.getPrincipal();
        String encryptedPassword = (String) authentication.getCredentials();

        ImUserPo user = getUserByUserId(userId);
        String decryptedPassword = cryptoHelper.decrypt(encryptedPassword);

        if (!passwordEncoder.matches(decryptedPassword, user.getPassword())) {
            log.warn("密码错误: userId={}", userId);
            throw new AuthenticationFailException(ResultCode.INVALID_CREDENTIALS);
        }

        log.info("用户登录成功: userId={}", userId);
        return createAuthenticationToken(user, authentication);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private ImUserPo getUserByUserId(String userId) {
        ImUserPo user = imUserDubboService.queryOne(userId);
        if (Objects.isNull(user)) {
            log.warn("用户不存在: userId={}", userId);
            throw new AuthenticationFailException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        return user;
    }

    private Authentication createAuthenticationToken(ImUserPo user, Authentication authentication) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                user.getUserId(), user.getPassword(), null);
        token.setDetails(authentication.getDetails());
        return token;
    }
}
