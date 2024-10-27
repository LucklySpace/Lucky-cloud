package com.xy.auth.security.provider;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xy.auth.entity.ImUser;
import com.xy.auth.security.token.QrScanAuthenticationToken;
import com.xy.auth.service.ImUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;


@Slf4j
public class QrScanAuthenticationProvider implements AuthenticationProvider {

    private ImUserService sysUserService;    //自定义user对象

    public QrScanAuthenticationProvider(ImUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        if (authentication.isAuthenticated()) {
            return authentication;
        }

        //获取手机号码
        String username = (String) authentication.getPrincipal();

        QueryWrapper wrapper = new QueryWrapper();
        wrapper.eq("user_id", username); //填充用户名
        ImUser user = sysUserService.getOne(wrapper);//获取用户对象

        if (user == null) { // 判断用户是否存在
            throw new UsernameNotFoundException("用户不存在");
        }

        //将权限装入框架验证

        return new QrScanAuthenticationToken(user.getUser_name(), null);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return QrScanAuthenticationToken.class.isAssignableFrom(authentication);
    }

}
