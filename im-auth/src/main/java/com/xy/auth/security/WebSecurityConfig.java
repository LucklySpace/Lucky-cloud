package com.xy.auth.security;


import com.xy.auth.security.filter.JwtAuthencationTokenFilter;
import com.xy.auth.security.filter.UserAuthenticationFilter;
import com.xy.auth.security.handle.*;
import com.xy.auth.security.provider.QrScanAuthenticationProvider;
import com.xy.auth.security.provider.SmsAuthenticationProvider;
import com.xy.auth.security.provider.UsernamePasswordAuthenticationProvider;
import com.xy.auth.service.ImUserService;
import com.xy.auth.utils.RedisUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {

    private final ImUserService sysUserService;
    private final RSAKeyProperties rsaKeyProperties;
    private final RedisUtil redisUtil;
    private final SecurityProperties securityProperties;
    private final AuthenticationSuccessHandler authenticationSuccessHandler;
    private final AuthenticationFailHandler authenticationFailHandler;
    private final LogoutSuccessHandler logoutSuccessHandler;
    private final LoginAuthenticationHandler loginAuthenticationHandler;
    private final LoginAccessDefineHandler loginAccessDefineHandler;

    public WebSecurityConfig(
            ImUserService sysUserService,
            RSAKeyProperties rsaKeyProperties,
            RedisUtil redisUtil,
            SecurityProperties securityProperties,
            AuthenticationSuccessHandler authenticationSuccessHandler,
            AuthenticationFailHandler authenticationFailHandler,
            LogoutSuccessHandler logoutSuccessHandler,
            LoginAuthenticationHandler loginAuthenticationHandler,
            LoginAccessDefineHandler loginAccessDefineHandler) {
        this.sysUserService = sysUserService;
        this.rsaKeyProperties = rsaKeyProperties;
        this.redisUtil = redisUtil;
        this.securityProperties = securityProperties;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.authenticationFailHandler = authenticationFailHandler;
        this.logoutSuccessHandler = logoutSuccessHandler;
        this.loginAuthenticationHandler = loginAuthenticationHandler;
        this.loginAccessDefineHandler = loginAccessDefineHandler;
    }

    // 定义各种认证提供者
    @Bean
    public SmsAuthenticationProvider smsAuthenticationProvider() {
        return new SmsAuthenticationProvider(sysUserService, rsaKeyProperties, redisUtil);
    }

    @Bean
    public UsernamePasswordAuthenticationProvider usernamePasswordAuthenticationProvider() {
        return new UsernamePasswordAuthenticationProvider(sysUserService, rsaKeyProperties);
    }

    @Bean
    public QrScanAuthenticationProvider qrScanAuthenticationProvider() {
        return new QrScanAuthenticationProvider(sysUserService, redisUtil);
    }

    // 定义认证管理器 AuthenticationManager
    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(
                Arrays.asList(
                        smsAuthenticationProvider(),
                        usernamePasswordAuthenticationProvider(),
                        qrScanAuthenticationProvider()
                )
        );
    }

    // 定义用户登陆验证的过滤器
    @Bean
    public UserAuthenticationFilter userAuthenticationFilter() {
        UserAuthenticationFilter filter = new UserAuthenticationFilter();
        filter.setAuthenticationManager(authenticationManager());
        filter.setAuthenticationSuccessHandler(authenticationSuccessHandler);
        filter.setAuthenticationFailureHandler(authenticationFailHandler);
        return filter;
    }

    // jwt token 过滤器
    @Bean
    public JwtAuthencationTokenFilter authenticationJwtTokenFilter() {
        return new JwtAuthencationTokenFilter(authenticationFailHandler, securityProperties);
    }

    // 安全过滤链的配置
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 添加自定义的过滤器
                .addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(userAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)

                // 禁用 CSRF
                .csrf(AbstractHttpConfigurer::disable)

                // 会话管理策略为无状态
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 授权配置
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(securityProperties.getIgnoreUrl()).permitAll()
                        .anyRequest().authenticated()
                )

                // 异常处理
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(loginAuthenticationHandler)  // 匿名处理
                        .accessDeniedHandler(loginAccessDefineHandler)  // 无权限处理
                )

                // 处理跨域配置
                .cors(cors -> cors.configurationSource(configurationSource()))

                // 禁用 frameOptions 并允许同源使用
                .headers(headers -> headers.frameOptions().sameOrigin());

        //退出成功后的处理器
        http.logout().logoutUrl("/user/logout").logoutSuccessHandler(logoutSuccessHandler);

        // 构建过滤链并返回
        return http.build();
    }

    // 密码加密器
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 跨域配置
    private CorsConfigurationSource configurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedHeaders(Collections.singletonList("*"));
        corsConfiguration.setAllowedMethods(Collections.singletonList("*"));
        corsConfiguration.setAllowedOrigins(Collections.singletonList("*"));
        corsConfiguration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}
