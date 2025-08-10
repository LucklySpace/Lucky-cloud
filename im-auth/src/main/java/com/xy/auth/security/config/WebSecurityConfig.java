package com.xy.auth.security.config;


import com.xy.auth.security.SecurityProperties;
import com.xy.auth.security.filter.TokenAuthenticationFilter;
import com.xy.auth.security.handle.LoginAccessDefineHandler;
import com.xy.auth.security.handle.LoginAuthenticationHandler;
import com.xy.auth.security.provider.MobileAuthenticationProvider;
import com.xy.auth.security.provider.QrScanAuthenticationProvider;
import com.xy.auth.security.provider.UsernamePasswordAuthenticationProvider;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    @Resource
    public UsernamePasswordAuthenticationProvider daoAuthenticationProvider;

    @Resource
    public QrScanAuthenticationProvider qrScanAuthenticationProvider;

    @Resource
    private LoginAuthenticationHandler loginAuthenticationHandler;

    @Resource
    private LoginAccessDefineHandler loginAccessDefineHandler;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private TokenAuthenticationFilter tokenAuthenticationFilter;

    @Resource
    private SecurityProperties securityProperties;

    @Resource
    private MobileAuthenticationProvider mobileAuthenticationProvider;

    /**
     * 定义认证管理器 AuthenticationManager
     * 集成多种认证方式（如手机号、用户名密码、扫码登录等）
     *
     * @return AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        // 创建认证提供者列表，支持多种认证方式
        List<AuthenticationProvider> authenticationProviders = new ArrayList<>();
        // 手机验证码认证
        authenticationProviders.add(mobileAuthenticationProvider);
        // 用户名密码认证
        authenticationProviders.add(daoAuthenticationProvider);
        // 二维码认证
        authenticationProviders.add(qrScanAuthenticationProvider);

        // 返回 ProviderManager，处理多种认证方式
        return new ProviderManager(authenticationProviders);
    }

    /**
     * 配置 HTTP 安全策略，包含认证、授权、跨域等配置
     *
     * @param http HttpSecurity 配置对象
     * @return SecurityFilterChain 安全过滤链
     */
    @Bean
    @Order(2) // 设置过滤链的顺序，确保该配置在默认配置之后生效
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        // 配置路径访问权限，忽略某些路径的认证
        http.authorizeHttpRequests(requestMatcherRegistry ->
                requestMatcherRegistry.requestMatchers(securityProperties.getIgnore()).permitAll() // 忽略的路径
                        .anyRequest().authenticated() // 其他路径需认证
        );

        // 禁用 CSRF 防护，因为我们使用 JWT，不需要 CSRF 保护
        http.csrf(AbstractHttpConfigurer::disable);

        // 禁用会话管理，设置为无状态，防止 Spring Security 创建会话
        http.sessionManagement(configurer ->
                configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        // 配置未授权和未登录处理
        http.exceptionHandling(customizer ->
                customizer
                        // 处理未授权访问
                        .accessDeniedHandler(loginAccessDefineHandler)
                        // 处理未登录状态（例如 JWT 校验失败）
                        .authenticationEntryPoint(loginAuthenticationHandler)
        );

        // 配置 JWT 校验过滤器，在用户名密码过滤器之前执行
        http.addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // 配置跨域支持
        http.cors(cors -> cors.configurationSource(configurationSource()));

        return http.build(); // 返回构建后的安全过滤链
    }

    /**
     * 配置跨域支持，允许所有源访问 API
     *
     * @return CorsConfigurationSource 跨域配置源
     */
    private CorsConfigurationSource configurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedHeaders(Collections.singletonList("*")); // 允许所有请求头
        corsConfiguration.setAllowedMethods(Collections.singletonList("*")); // 允许所有请求方法
        corsConfiguration.setAllowedOrigins(Collections.singletonList("*")); // 允许所有源
        corsConfiguration.setMaxAge(3600L); // 设置预检请求的缓存时间为 1 小时

        // 创建并注册跨域配置
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}