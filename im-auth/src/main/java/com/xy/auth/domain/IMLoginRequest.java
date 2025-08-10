package com.xy.auth.domain;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 登录请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IMLoginRequest implements UserDetails {

    @Schema(description = "用户名、手机号或二维码字符串")
    private String principal;

    @Schema(description = "加密后的密码、验证码或二维码登录密码")
    private String credentials;

    @Schema(description = "认证类型", example = "form | sms | scan")
    private String authType;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getUsername() {
        return this.principal;
    }

    @Override
    public String getPassword() {
        return this.credentials;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}