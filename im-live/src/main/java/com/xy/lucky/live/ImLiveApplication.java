package com.xy.lucky.live;

import com.xy.lucky.spring.XSpringApplication;
import com.xy.lucky.spring.annotations.SpringApplication;
import com.xy.lucky.spring.annotations.aop.EnableAop;
import com.xy.lucky.spring.annotations.core.EnableAsync;

/**
 * 流媒体服务器启动类
 * 使用自研容器 im-spring 进行组件扫描与生命周期管理
 * - 启用 AOP 与异步支持
 */
@SpringApplication("com.xy.lucky.live")
@EnableAop
@EnableAsync
public class ImLiveApplication {

    public static void main(String[] args) {
        XSpringApplication.run(ImLiveApplication.class, args);
    }
}

