package com.xy.lucky.auth.security.provider;


import com.xy.lucky.domain.po.ImUserPo;
import com.xy.lucky.dubbo.web.api.database.user.ImUserDubboService;
import com.xy.lucky.general.response.domain.ResultCode;
import com.xy.lucky.security.RSAKeyProperties;
import com.xy.lucky.security.exception.AuthenticationFailException;
import com.xy.lucky.security.util.RSAUtil;
import jakarta.annotation.Resource;
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
 * 用户名密码登录
 */
@Slf4j
@Component
public class UsernamePasswordAuthenticationProvider implements AuthenticationProvider {

    @DubboReference
    private ImUserDubboService imUserDubboService;

    @Resource
    private RSAKeyProperties RSAKeyProperties;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        // 获取用户名和密码
        String userId = (String) authentication.getPrincipal();
        String password = (String) authentication.getCredentials();

        // 获取用户信息
        ImUserPo user = getUserByUserId(userId);

        // 解密密码
        String decryptedPassword = decryptPassword(password);

        // 比较密码
        if (!isPasswordValid(decryptedPassword, user.getPassword())) {
            throw new AuthenticationFailException(ResultCode.INVALID_CREDENTIALS);
        }

        // 认证成功，创建认证令牌并设置认证信息
        return createAuthenticationToken(user, authentication);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    /**
     * 获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     * @throws AuthenticationFailException 如果用户不存在
     */
    private ImUserPo getUserByUserId(String userId) {
        ImUserPo user = imUserDubboService.queryOne(userId);
        if (Objects.isNull(user)) {
            log.warn("Account not found for userId: {}", userId);
            throw new AuthenticationFailException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        return user;
    }

    /**
     * 解密密码
     *
     * @param encryptedPassword 加密的密码
     * @return 解密后的密码
     */
    private String decryptPassword(String encryptedPassword) {
        try {
            // 处理 Base64 编码问题（URL 传递时 "+" 号会被编码成空格）
            String decodedPassword = encryptedPassword.replaceAll(" ", "+");
            return RSAUtil.decrypt(decodedPassword, RSAKeyProperties.getPrivateKeyStr());
        } catch (Exception e) {
            log.error("Failed to decrypt password", e);
            throw new AuthenticationFailException(ResultCode.INVALID_CREDENTIALS);
        }
    }

    /**
     * 比较密码是否匹配
     *
     * @param decryptedPassword 解密后的密码
     * @param storedPassword    存储在数据库中的密码
     * @return true: 密码匹配, false: 密码不匹配
     */
    private boolean isPasswordValid(String decryptedPassword, String storedPassword) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.matches(decryptedPassword, storedPassword);
    }

    /**
     * 创建认证令牌
     *
     * @param user           用户信息
     * @param authentication 认证请求
     * @return 认证令牌
     */
    private Authentication createAuthenticationToken(ImUserPo user, Authentication authentication) {
        // 创建认证令牌并设置认证信息
        UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(
                user.getUserId(), user.getPassword(), null);
        result.setDetails(authentication.getDetails());

        log.info("Login success for userId: {}", user.getUserId());
        return result;
    }
}
