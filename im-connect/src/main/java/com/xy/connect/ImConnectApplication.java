package com.xy.connect;


import com.xy.connect.config.LogConstant;
import com.xy.connect.utils.IPAddressUtil;
import com.xy.connect.utils.MachineCodeUtils;
import com.xy.spring.XSpringApplication;
import com.xy.spring.annotations.SpringApplication;
import com.xy.spring.annotations.aop.EnableAop;
import com.xy.spring.context.ApplicationContext;
import com.xy.spring.core.ApplicationConfigLoader;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = LogConstant.Main)
@EnableAop
@SpringApplication
public class ImConnectApplication {


    public static void main(String[] args) throws Exception {

        // 获取机器码
        loadMachineCode();

        // 环境变量校验
        validateRuntimeEnvironment();

        // 日志系统初始化
        initializeLogger();

        // 启动Spring
        XSpringApplication.run(ImConnectApplication.class, args);

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
    private static void loadMachineCode() {
        // 获取机器码
        String brokerId = MachineCodeUtils.getMachineCode();

        log.info("获取机器码 ：{}", brokerId);

        ApplicationConfigLoader.put("brokerId", brokerId);
    }

}
