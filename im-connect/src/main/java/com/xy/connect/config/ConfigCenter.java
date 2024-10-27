package com.xy.connect.config;

import com.xy.connect.config.loader.YamlConfigLoader;
import lombok.extern.slf4j.Slf4j;

/**
 * 加载resource目录下的配置文件
 */
@Slf4j(topic = LogConstant.CONFIG)
public class ConfigCenter {

    public static IMNettyConfig nettyConfig;
    public static IMRabbitMQConfig mqConfig;
    public static IMNacosConfig nacosConfig;
    public static IMRedisConfig redisConfig;

    public static void load() {

        nettyConfig = YamlConfigLoader.loadConfig("netty.yml", IMNettyConfig.class);

        mqConfig = YamlConfigLoader.loadConfig("mq.yml", IMRabbitMQConfig.class);

        nacosConfig = YamlConfigLoader.loadConfig("nacos.yml", IMNacosConfig.class);

        redisConfig = YamlConfigLoader.loadConfig("redis.yml", IMRedisConfig.class);

        log.info("加载配置成功");
    }

}
