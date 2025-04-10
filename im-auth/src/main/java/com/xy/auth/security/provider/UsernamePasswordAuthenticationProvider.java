package com.xy.auth.security.provider;


import com.xy.auth.domain.dto.ImUserDto;
import com.xy.auth.service.impl.ImUserServiceImpl;
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
    private ImUserServiceImpl userDetailsService;

    public void setUserDetailsService(ImUserServiceImpl userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        String userId = (authentication.getPrincipal() == null) ? "NONE_PROVIDED" : authentication.getName();

        String password = (String) authentication.getCredentials();

        ImUserDto imUserDto = userDetailsService.verifyUserByUsername(userId, password);

        UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(imUserDto.getUserId(), imUserDto.getPassword(), null);

        result.setDetails(authentication.getDetails());

        log.info("form login success :{}", userId);

        return result;
    }


    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

}

