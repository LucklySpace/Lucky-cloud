package com.xy.auth.security.provider;


import com.xy.auth.service.ImUserService;
import com.xy.domain.po.ImUserPo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * @author dense
 */
@Slf4j
public class UsernamePasswordAuthenticationProvider implements AuthenticationProvider {

    //自定义user对象
    private ImUserService userDetailsService;

    public void setUserDetailsService(ImUserService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        String userId = (String) authentication.getPrincipal();

        String password = (String) authentication.getCredentials();

        ImUserPo imUserPo = userDetailsService.verifyUserByUsername(userId, password);

        UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(imUserPo.getUserId(), imUserPo.getPassword(), null);

        result.setDetails(authentication.getDetails());

        log.info("form login success :{}", userId);

        return result;
    }


    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

}

