package com.xy.lucky.proxy;

import com.xy.lucky.spring.XSpringApplication;
import com.xy.lucky.spring.annotations.SpringApplication;
import com.xy.lucky.spring.annotations.aop.EnableAop;

@EnableAop
@SpringApplication
public class ImProxyApplication {
    public static void main(String[] args) {
        XSpringApplication.run(ImProxyApplication.class, args);
    }
}