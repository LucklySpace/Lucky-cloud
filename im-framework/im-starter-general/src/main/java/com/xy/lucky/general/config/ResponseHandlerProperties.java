package com.xy.lucky.general.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "response.handler")
public class ResponseHandlerProperties {

    /**
     * 不需要进行 Result 封装的路径模式列表
     */
    private List<String> excludePaths = new ArrayList<>();
}
