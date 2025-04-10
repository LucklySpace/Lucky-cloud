package com.xy.connect.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.xy.connect.config.loader.YamlConfigLoader;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 加载resource目录下的配置文件
 */
@Slf4j(topic = LogConstant.CONFIG)
public class ConfigCenter {

    // private static final Map<String, ConfigService> configServices = new ConcurrentHashMap<>();
    public static IMNettyConfig nettyConfig;
    public static IMRabbitMQConfig mqConfig;
    public static IMNacosConfig nacosConfig;
    public static IMRedisConfig redisConfig;

    public static void load() {
        loadStaticConfigs();
        //initNacosListeners();
        log.info("配置加载成功");
    }

    private static void loadStaticConfigs() {
        nettyConfig = YamlConfigLoader.loadConfig("netty.yml", IMNettyConfig.class);
        mqConfig = YamlConfigLoader.loadConfig("mq.yml", IMRabbitMQConfig.class);
        nacosConfig = YamlConfigLoader.loadConfig("nacos.yml", IMNacosConfig.class);
        redisConfig = YamlConfigLoader.loadConfig("redis.yml", IMRedisConfig.class);
    }

//    private static void initNacosListeners() {
//        try {
//            Arrays.asList("netty.yml", "mq.yml", "nacos.yml", "redis.yml")
//                    .forEach(configFile -> {
//                        try {
//                            ConfigService cs = NacosFactory.createConfigService(nacosConfig.getNacosConfig().getAddress()
//                                    + ":" + nacosConfig.getNacosConfig().getPort());
//                            cs.addListener(configFile, nacosConfig.getNacosConfig().getGroup(), new Listener() {
//                                @Override
//                                public void receiveConfigInfo(String configInfo) {
//                                    log.warn("检测到配置更新：{}", configFile);
//                                    loadStaticConfigs();
//                                }
//
//                                @Override
//                                public Executor getExecutor() {
//                                    return Executors.newSingleThreadExecutor();
//                                }
//                            });
//                            configServices.put(configFile, cs);
//                        } catch (NacosException e) {
//                            log.error("Nacos监听器初始化失败", e);
//                        }
//                    });
//        } catch (Exception e) {
//            log.error("动态配置监听初始化异常", e);
//        }
//    }

}
