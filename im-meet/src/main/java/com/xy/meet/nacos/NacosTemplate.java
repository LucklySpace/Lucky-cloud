package com.xy.meet.nacos;

import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.xy.meet.utils.IPAddressUtil;
import com.xy.spring.annotations.core.Component;
import com.xy.spring.annotations.core.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class NacosTemplate {


    @Value("nacos.config.address")
    private String serverIp;

    @Value("nacos.config.port")
    private Integer serverPort;

    @Value("nacos.config.name")
    private String serviceName;

    @Value("nacos.config.version")
    private String version;

    @Value("brokerId")
    private String brokerId;

    //TIP 当前实例缓存
    private Instance currentInstance;
    // Nacos 服务对象
    private NamingService namingService;

    /**
     * 注册服务到nacos
     * https://juejin.cn/post/6844903782086606861
     *
     * @param port 端口
     */
    public void registerNacos(Integer port) {
        try {

            //TIP 获取Nacos服务地址
            String serverAddr = serverIp + ":" + serverPort;

            // 获取本机IP地址
            String ip = IPAddressUtil.getLocalIp4Address();

            // 创建Nacos实例
            currentInstance = new Instance();

            // IP地址
            currentInstance.setIp(ip);

            // 端口
            currentInstance.setPort(port);

            // 服务名
            currentInstance.setServiceName(serviceName);

            // 是否启用
            currentInstance.setEnabled(true);

            // 健康状态
            currentInstance.setHealthy(true);

            // 权重
            currentInstance.setWeight(1.0);

            // 机器码
            currentInstance.addMetadata("brokerId", brokerId);

            // 版本号
            currentInstance.addMetadata("version", version);

            // 协议
            currentInstance.addMetadata("protocol", "websocket");

            // 连接数
            currentInstance.addMetadata("connection", "0");

            // 注册服务到Nacos
            namingService = NamingFactory.createNamingService(serverAddr);

            namingService.registerInstance(serviceName, currentInstance);

            log.info("Service registered to Nacos successfully, port: {}", port);

            // 启动连接数上报任务
            //startConnectionReporter();

        } catch (Exception e) {
            log.error("Failed to register service to Nacos, port: {}", port, e);
        }
    }

}
