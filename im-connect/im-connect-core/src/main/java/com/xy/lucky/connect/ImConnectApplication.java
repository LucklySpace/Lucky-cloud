package com.xy.lucky.connect;


import com.xy.lucky.connect.config.LogConstant;
import com.xy.lucky.connect.utils.IPAddressUtil;
import com.xy.lucky.connect.utils.MachineCodeUtils;
import com.xy.lucky.spring.boot.SpringApplication;
import com.xy.lucky.spring.boot.annotation.SpringBootApplication;
import com.xy.lucky.spring.boot.context.ConfigurableApplicationContext;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = LogConstant.Main)
@SpringBootApplication
public class ImConnectApplication {

    public static void main(String[] args) throws Exception {

        // 获取机器码
        loadMachineCode();

        // 环境变量校验
        validateRuntimeEnvironment();

        // 日志系统初始化
        initializeLogger();

        // 启动Spring
        ConfigurableApplicationContext context = SpringApplication.run(ImConnectApplication.class, args);

        // 设置 brokerId 到环境
        context.getEnvironment();

        log.info("IM连接服务启动成功，监听地址：{}", IPAddressUtil.getLocalIp4Address());

    }

    private static void validateRuntimeEnvironment() {
        // 验证JVM版本
        String javaVersion = System.getProperty("java.version");
        if (!javaVersion.startsWith("21")) {
            throw new RuntimeException("需要Java 21运行环境，当前版本：" + javaVersion);
        }
    }

    private static void initializeLogger() {
        log.info("================================================================");
        log.info("=                  IM Connect Service Starting               =");
        log.info("================================================================");
    }


    /**
     * 获取机器码，设置队列名称和路由键
     */
    @SneakyThrows
    private static void loadMachineCode() {
        // 获取机器码
        String brokerId = MachineCodeUtils.getMachineCode();
        log.info("获取机器码 ：{}", brokerId);
        // brokerId 通过系统属性设置
        System.setProperty("brokerId", brokerId);
    }

}
