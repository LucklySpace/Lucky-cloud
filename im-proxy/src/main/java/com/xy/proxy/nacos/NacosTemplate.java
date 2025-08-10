package com.xy.proxy.nacos;

import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.xy.proxy.utils.IPAddressUtil;
import com.xy.spring.annotations.core.Component;
import com.xy.spring.annotations.core.Value;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

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

    //TIP 当前实例缓存
    private Instance currentInstance;

    // Nacos 服务对象
    private NamingService namingService;

    /**
     * 注册服务到nacos
     * https://juejin.cn/post/6844903782086606861
     *
     * @param
     */
    public void registerNacos() {

        Integer port =2222;

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

            // 版本号
            currentInstance.addMetadata("version", version);

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

    @SneakyThrows
    public List<Instance> getAllInstances(String name){
        return namingService.getAllInstances(name);
    }


    public NamingService getNamingService(){
        return this.namingService;
    }


}
