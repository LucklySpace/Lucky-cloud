package com.xy.auth.security.config;


import com.xy.auth.security.CustomAuthenticationEntryPoint;
import com.xy.auth.security.SecurityProperties;
import com.xy.auth.security.filter.TokenAuthenticationFilter;
import com.xy.auth.security.provider.MobileAuthenticationProvider;
import com.xy.auth.security.provider.QrScanAuthenticationProvider;
import com.xy.auth.security.provider.UsernamePasswordAuthenticationProvider;
import com.xy.auth.service.ImUserService;
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
    private CustomAuthenticationEntryPoint myAuthenticationEntryPoint;

    @Resource
    private ImUserService customUserDetailsService;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private TokenAuthenticationFilter tokenAuthenticationFilter;

    @Resource
    private SecurityProperties securityProperties;


    /**
     * 手机验证码登录
     *
     * @return
     */
    @Bean
    public MobileAuthenticationProvider mobileAuthenticationProvider() {
        MobileAuthenticationProvider mobileAuthenticationProvider = new MobileAuthenticationProvider();
        mobileAuthenticationProvider.setUserDetailsService(customUserDetailsService);
        return mobileAuthenticationProvider;
    }

    /**
     * 用户名密码登录
     *
     * @return
     */
    @Bean
    public UsernamePasswordAuthenticationProvider daoAuthenticationProvider() {
        UsernamePasswordAuthenticationProvider daoAuthenticationProvider = new UsernamePasswordAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(customUserDetailsService);
        return daoAuthenticationProvider;
    }

    /**
     * 扫码登录
     *
     * @return
     */
    @Bean
    public QrScanAuthenticationProvider qrScanAuthenticationProvider() {
        QrScanAuthenticationProvider qrScanAuthenticationProvider = new QrScanAuthenticationProvider();
        qrScanAuthenticationProvider.setUserDetailsService(customUserDetailsService);
        return qrScanAuthenticationProvider;
    }

    /**
     * 定义认证管理器AuthenticationManager
     *
     * @return AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        List<AuthenticationProvider> authenticationProviders = new ArrayList<>();
        authenticationProviders.add(mobileAuthenticationProvider());
        authenticationProviders.add(daoAuthenticationProvider());
        authenticationProviders.add(qrScanAuthenticationProvider());
        return new ProviderManager(authenticationProviders);
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http)
            throws Exception {
        http
                // 添加过滤器
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                //.addFilterBefore(userAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)

                // 授权配置
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(securityProperties.getIgnoreUrl()).permitAll()
                        .anyRequest().authenticated()
                )


//                .authorizeHttpRequests((authorize) ->
//                        authorize.requestMatchers(new AntPathRequestMatcher("/login/**")).permitAll()
//                                .anyRequest().authenticated())
                // 禁用 CSRF
                .csrf(AbstractHttpConfigurer::disable)

                // 会话管理策略为无状态
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 异常处理
                .exceptionHandling(
                        configure -> {
                            configure.authenticationEntryPoint(myAuthenticationEntryPoint);
                        })

                // 处理跨域配置
                .cors(cors -> cors.configurationSource(configurationSource()));

        return http.build();
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
