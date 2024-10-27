package com.xy.auth.security.provider;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xy.auth.entity.ImUser;
import com.xy.auth.security.RSAKeyProperties;
import com.xy.auth.security.exception.PassWordErrorException;
import com.xy.auth.service.ImUserService;
import com.xy.auth.utils.RSAUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.PrivateKey;
import java.util.Base64;

@Slf4j
public class UsernamePasswordAuthenticationProvider implements AuthenticationProvider {

    private ImUserService sysUserService;    //自定义user对象

    private RSAKeyProperties rsaKeyProperties;


    public UsernamePasswordAuthenticationProvider(ImUserService sysUserService, RSAKeyProperties rsaKeyProperties) {
        this.sysUserService = sysUserService;
        this.rsaKeyProperties = rsaKeyProperties;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = (authentication.getPrincipal() == null) ? "NONE_PROVIDED" : authentication.getName();
        String password = (String) authentication.getCredentials();

        // 使用select方法只查询需要的字段，避免加载整个实体对象
        QueryWrapper<ImUser> wrapper = new QueryWrapper<>();
        wrapper.select("user_id", "password").eq("user_id", username); // 填充用户名
        ImUser user = sysUserService.getOne(wrapper);

        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        String decryptedPassword = decryptPassword(password);

        // 使用更轻量级的密码比对方式,BCryptPasswordEncoder的matches方法
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        if (!passwordEncoder.matches(decryptedPassword, user.getPassword())) {
            throw new BadCredentialsException("Password does not match");
        }

        UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(username, password, null);

        result.setDetails(authentication.getDetails());

        return result;
    }

    private String decryptPassword(String password) {
        try {
            //获取私钥
            return RSAUtil.decrypt(password, rsaKeyProperties.getPrivateKeyStr());

        } catch (Exception e) {
            log.error("Failed to decrypt password");
            throw new PassWordErrorException("用户名或密码错误");
        }
    }


    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

}
