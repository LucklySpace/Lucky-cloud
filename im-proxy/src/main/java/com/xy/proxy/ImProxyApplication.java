package com.xy.proxy;

import com.xy.spring.XSpringApplication;
import com.xy.spring.annotations.SpringApplication;
import com.xy.spring.annotations.aop.EnableAop;

@EnableAop
@SpringApplication
public class ImProxyApplication {
    public static void main(String[] args) {
        XSpringApplication.run(ImProxyApplication.class, args);
    }
}