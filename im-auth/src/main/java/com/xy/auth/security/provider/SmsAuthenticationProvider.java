package com.xy.auth.security.provider;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xy.auth.entity.ImUser;
import com.xy.auth.security.RSAKeyProperties;
import com.xy.auth.security.exception.CaptchaException;
import com.xy.auth.security.exception.PassWordErrorException;
import com.xy.auth.security.token.SmsAuthenticationToken;
import com.xy.auth.service.ImUserService;
import com.xy.auth.utils.RSAUtil;
import com.xy.auth.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StringUtils;

import java.security.PrivateKey;
import java.util.Base64;


@Slf4j
public class SmsAuthenticationProvider implements AuthenticationProvider {

    private ImUserService sysUserService;    //自定义user对象

    private RSAKeyProperties rsaKeyProperties;

    private RedisUtil redisUtil;


    public SmsAuthenticationProvider(ImUserService sysUserService, RSAKeyProperties rsaKeyProperties, RedisUtil redisUtil) {
        this.sysUserService = sysUserService;
        this.rsaKeyProperties = rsaKeyProperties;
        this.redisUtil = redisUtil;
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

        // 获取手机验证码缓存
        String redisCacheSmsCode = redisUtil.get("sms" + phoneNumber);

        String decryptSmsCode = decryptPassword(smsCode);

        // 如果短信验证码不一致，则抛出异常
        if (!decryptSmsCode.equals(redisCacheSmsCode)) {
            throw new CaptchaException("smsCode is error");
        }

        // 删除手机验证码
        redisUtil.del("sms" + phoneNumber);

        // 使用select方法只查询需要的字段，避免加载整个实体对象
        QueryWrapper<ImUser> wrapper = new QueryWrapper<>();
        wrapper.select("user_id", "mobile").like("mobile", phoneNumber); // 填充用户名
        ImUser user = sysUserService.getOne(wrapper);

        // 将权限装入框架验证
        SmsAuthenticationToken authenticationResult = new SmsAuthenticationToken(user.getUserId(), null);

        log.info("phone login success :{}",user.getUserId());

        return authenticationResult;
    }


    private String decryptPassword(String password) {
        try {
            //获取私钥
            PrivateKey privateKey = rsaKeyProperties.getPrivateKey();

            String privateKeyStr = new String(Base64.getEncoder().encode(privateKey.getEncoded()));

            return RSAUtil.decrypt(password, privateKeyStr);

        } catch (Exception e) {

            throw new PassWordErrorException("密码解密失败");

        }
    }


    @Override
    public boolean supports(Class<?> authentication) {
        return SmsAuthenticationToken.class.isAssignableFrom(authentication);
    }

}

