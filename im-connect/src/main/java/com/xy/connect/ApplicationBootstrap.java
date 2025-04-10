package com.xy.connect;

import com.xy.connect.config.ConfigCenter;
import com.xy.connect.config.LogConstant;
import com.xy.connect.message.MessageHandler;
import com.xy.connect.message.process.MessageHandlerFactory;
import com.xy.connect.mq.RabbitMQHandler;
import com.xy.connect.netty.service.AbstractRemoteServer;
import com.xy.connect.utils.IPAddressUtil;
import com.xy.connect.utils.MachineCodeUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = LogConstant.MAIN)
public class ApplicationBootstrap {

    public static String BROKERID = "123456";
    public static String ROUTINGKEY = "IM-123456";
    private AbstractRemoteServer abstractRemoteServer;
    private RabbitMQHandler rabbitMQHandler;

    /**
     * 获取机器码，设置队列名称和路由键
     */
    private static void loadMachineCode() {
        // 获取机器码
        String brokerId = MachineCodeUtils.getMachineCode();

        log.info("获取机器码 ：{}", brokerId);

        BROKERID = brokerId;

        ROUTINGKEY = com.xy.imcore.constants.Constant.ROUTERKEYPREFIX + brokerId;
    }

    public void start() {
        try {
            // 1. 环境准备阶段
            prepareEnvironment();

            // 2. 配置加载阶段
            loadConfiguration();

            // 3. 加载机器码
            loadMachineCode();

            // 4. 服务启动阶段
            startupServer();

            log.info("IM连接服务启动成功，监听地址：{}", IPAddressUtil.getLocalIp4Address());

            // 添加关闭钩子实现优雅停机
            addShutdownHook();

        } catch (Exception e) {
            log.error("服务启动失败", e);
            System.exit(1);
        }
    }

    private void prepareEnvironment() {
        // 环境变量校验
        validateRuntimeEnvironment();
        // 日志系统初始化
        initializeLogger();
    }

    private void loadConfiguration() {
        // 加载Netty配置
        ConfigCenter.load();
    }

    private void startupServer() {
        // 创建并启动Netty服务
        abstractRemoteServer = new AbstractRemoteServer();

        abstractRemoteServer.start();

        rabbitMQHandler = new RabbitMQHandler(
                ConfigCenter.mqConfig.getRabbitMQ().getAddress(),
                ConfigCenter.mqConfig.getRabbitMQ().getPort(),
                ConfigCenter.mqConfig.getRabbitMQ().getUsername(),
                ConfigCenter.mqConfig.getRabbitMQ().getPassword(),
                ConfigCenter.mqConfig.getRabbitMQ().getVirtual(),
                BROKERID,
                new MessageHandler(new MessageHandlerFactory())
        );
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("开始执行优雅关闭...");
            try {
                if (abstractRemoteServer != null) {
                    abstractRemoteServer.stop();
                }
                log.info("服务已安全停止");
            } catch (Exception e) {
                log.error("关闭过程中发生异常", e);
            }
        }));
    }

    private void validateRuntimeEnvironment() {
        // 验证JVM版本
        String javaVersion = System.getProperty("java.version");
        if (!javaVersion.startsWith("21")) {
            throw new RuntimeException("需要Java 21运行环境，当前版本：" + javaVersion);
        }
    }

    private void initializeLogger() {
        log.info("================================================================");
        log.info("=                  IM Connect Service Starting               =");
        log.info("================================================================");
    }

}