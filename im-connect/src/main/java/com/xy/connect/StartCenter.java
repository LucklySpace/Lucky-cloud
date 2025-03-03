package com.xy.connect;


import com.alibaba.csp.sentinel.init.InitExecutor;
import com.xy.connect.config.ConfigCenter;
import com.xy.connect.config.LogConstant;
import com.xy.connect.message.MessageHandler;
import com.xy.connect.mq.RabbitMQHandler;
import com.xy.connect.netty.service.AbstractRemoteServer;
import com.xy.connect.utils.MachineCodeUtils;
import lombok.extern.slf4j.Slf4j;


@Slf4j(topic = LogConstant.MAIN)
public class StartCenter {

    public static String BROKERID = "123456";
    public static String ROUTINGKEY = "IM-123456";

    public static void start() {

        log.info("IM服务启动中....");

        // 设置机器码
        getMachineCode();

        ConfigCenter.load();

        // 初始化mq，并进行监听
        startMq();

        // 启动netty服务
        startNetty();

        // 启动sentinel
        //startSentinel();

        log.info("IM服务启动成功....");
    }


    public static void startSentinel() {
        try {
            System.setProperty("project.name", ConfigCenter.nacosConfig.getNacosConfig().getName());
            // 启动 Sentinel
            InitExecutor.doInit();
        } catch (Exception e) {
            log.error("sentinel启动失败", e);
        }
    }


    private static void startNetty() {
        new AbstractRemoteServer().start();
    }

    /**
     * 创建并监听队列
     */
    private static void startMq() {
        new RabbitMQHandler(
                ConfigCenter.mqConfig.getRabbitMQ().getAddress(),
                ConfigCenter.mqConfig.getRabbitMQ().getPort(),
                ConfigCenter.mqConfig.getRabbitMQ().getUsername(),
                ConfigCenter.mqConfig.getRabbitMQ().getPassword(),
                ConfigCenter.mqConfig.getRabbitMQ().getVirtual(),
                BROKERID,
                new MessageHandler()
        );
    }


    /**
     * 获取机器码，设置队列名称和路由键
     */
    private static void getMachineCode() {
        // 获取机器码
        String brokerId = MachineCodeUtils.getMachineCode();

        log.info("获取机器码 ：{}", brokerId);

        BROKERID = brokerId;

        ROUTINGKEY = com.xy.imcore.constants.Constant.ROUTERKEYPREFIX + brokerId;
    }


}
