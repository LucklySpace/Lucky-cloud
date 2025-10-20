package com.xy.auth.security.provider;


import com.xy.auth.domain.IMQRCode;
import com.xy.auth.security.exception.AuthenticationFailException;
import com.xy.auth.security.token.QrScanAuthenticationToken;
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
 * 二维码登录
 */
@Slf4j
@Component
public class QrScanAuthenticationProvider implements AuthenticationProvider {

    @DubboReference
    private ImUserDubboService imUserDubboService;

    @Resource
    private RedisCache redisCache;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        if (authentication.isAuthenticated()) {
            return authentication; // 如果已经认证，直接返回当前认证信息
        }

        // 获取前端传来的二维码和密码
        String qrcode = (String) authentication.getPrincipal();
        String password = (String) authentication.getCredentials();

        // 校验二维码和密码
        validateQrCodeAndPassword(qrcode, password);

        // 根据二维码信息从 Redis 获取用户信息
        IMQRCode qrCodeInfo = getQrCodeInfoFromRedis(qrcode);

        // 获取用户信息
        ImUserPo user = getUserFromQrCodeInfo(qrCodeInfo);

        // 将认证信息返回并设置到 Spring Security 上下文中
        return new QrScanAuthenticationToken(user.getUserId(), null);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return QrScanAuthenticationToken.class.isAssignableFrom(authentication); // 支持 QrScanAuthenticationToken 认证类型
    }

    /**
     * 校验二维码和密码
     *
     * @param qrcode   二维码
     * @param password 密码
     * @throws AuthenticationFailException 认证失败异常
     */
    private void validateQrCodeAndPassword(String qrcode, String password) {
        if (!StringUtils.hasText(qrcode)) {
            throw new AuthenticationFailException(ResultCode.QRCODE_IS_INVALID); // 二维码无效
        }
        if (!StringUtils.hasText(password)) {
            throw new AuthenticationFailException(ResultCode.INVALID_CREDENTIALS); // 密码无效
        }
    }

    /**
     * 从 Redis 获取二维码信息
     *
     * @param qrcode 二维码
     * @return 返回二维码信息
     * @throws AuthenticationFailException 如果二维码信息不存在或已过期
     */
    private IMQRCode getQrCodeInfoFromRedis(String qrcode) {

        String redisKey = IMConstant.QRCODE_KEY_PREFIX + qrcode;

        // 校验二维码是否存在
        if (!redisCache.hasKey(redisKey)) {
            log.warn("QR code {} is invalid or expired", qrcode);
            throw new AuthenticationFailException(ResultCode.QRCODE_IS_INVALID); // 二维码无效
        }

        // 获取 Redis 中保存的二维码信息
        return redisCache.get(redisKey);
    }

    /**
     * 根据二维码信息从用户服务中获取用户数据
     *
     * @param qrCodeInfo 二维码信息
     * @return 用户信息
     * @throws AuthenticationFailException 如果用户未找到
     */
    private ImUserPo getUserFromQrCodeInfo(IMQRCode qrCodeInfo) {
        // 校验二维码信息中的用户 ID
        String userId = qrCodeInfo.getUserId();

        if (StringUtils.isEmpty(userId)) {
            log.error("User ID is missing in QR code information");
            throw new AuthenticationFailException(ResultCode.ACCOUNT_NOT_FOUND); // 用户未找到
        }

        // 从用户服务获取用户信息
        ImUserPo user = imUserDubboService.selectOne(userId);
        if (Objects.isNull(user)) {
            log.warn("Account not found for user ID: {}", userId);
            throw new AuthenticationFailException(ResultCode.ACCOUNT_NOT_FOUND); // 账户未找到
        }

        return user;
    }
}