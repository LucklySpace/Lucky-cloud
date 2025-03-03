package com.xy.meet;


import com.xy.meet.config.ConfigCenter;
import com.xy.meet.config.LogConstant;
import com.xy.meet.netty.service.IMeetChatServer;
import lombok.extern.slf4j.Slf4j;


@Slf4j(topic = LogConstant.MAIN)
public class StartCenter {

    public static void start() {

        log.info("IM服务启动中....");

        // 获取配置
        ConfigCenter.load();

        // 启动netty服务
        startNetty();

        log.info("IM meet服务启动成功....");
    }


    private static void startNetty() {
        new IMeetChatServer().start();
        // new AbstractRemoteServer().start();
    }


}
