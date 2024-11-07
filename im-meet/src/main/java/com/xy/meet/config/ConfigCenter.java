package com.xy.meet.config;


import com.xy.meet.config.loader.YamlConfigLoader;
import lombok.extern.slf4j.Slf4j;

/**
 * 加载resource目录下的配置文件
 */
@Slf4j(topic = LogConstant.CONFIG)
public class ConfigCenter {

    public static IMNettyConfig nettyConfig;

    public static IMNacosConfig nacosConfig;

    public static void load() {

        nettyConfig = YamlConfigLoader.loadConfig("netty.yml", IMNettyConfig.class);

        nacosConfig = YamlConfigLoader.loadConfig("nacos.yml", IMNacosConfig.class);

        log.info("加载配置成功");
    }

}
