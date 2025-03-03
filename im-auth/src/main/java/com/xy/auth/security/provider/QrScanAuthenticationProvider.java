package com.xy.auth.security.provider;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xy.auth.entity.ImUser;
import com.xy.auth.security.token.QrScanAuthenticationToken;
import com.xy.auth.service.ImUserService;
import com.xy.auth.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.xy.auth.constant.Qrcode.QRCODE_AUTHORIZED;
import static com.xy.auth.constant.Qrcode.QRCODE_PREFIX;


@Slf4j
public class QrScanAuthenticationProvider implements AuthenticationProvider {

    private ImUserService sysUserService;    //自定义user对象

    private RedisUtil redisUtil;

    public QrScanAuthenticationProvider(ImUserService sysUserService, RedisUtil redisUtil) {
        this.sysUserService = sysUserService;
        this.redisUtil = redisUtil;
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

        String redisKey = QRCODE_PREFIX + qrcode;

        if (!redisUtil.hasKey(QRCODE_PREFIX + qrcode)) {
            throw new UsernameNotFoundException("二维码已失效");
        }

        Map<String, Object> qrCodeInfo = redisUtil.get(redisKey);

        if (!password.equals(qrCodeInfo.get("password"))) {
            throw new UsernameNotFoundException("二维码已失效");
        }

        QueryWrapper wrapper = new QueryWrapper();
        wrapper.eq("user_id", qrCodeInfo.get("userId")); //填充用户名
        ImUser user = sysUserService.getOne(wrapper);//获取用户对象

        if (user == null) { // 判断用户是否存在
            throw new UsernameNotFoundException("用户不存在");
        }

        // 设置二维码授权
        redisUtil.set(QRCODE_PREFIX + qrcode, QRCODE_AUTHORIZED, 15, TimeUnit.SECONDS);

        //将权限装入框架验证
        return new QrScanAuthenticationToken(user.getUserId(), null);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return QrScanAuthenticationToken.class.isAssignableFrom(authentication);
    }

}
