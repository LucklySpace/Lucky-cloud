package com.xy.lucky.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "lucky.gateway.plugin")
public class GatewayPluginProperties {

    private boolean enabled = true;
    private boolean hotReload = true;
    private boolean strictVersionCheck = true;
    private int filterOrder = -250;
    private long defaultTimeoutMs = 200;
    private int defaultMaxConcurrency = 256;
    private boolean defaultFailOpen = true;
    private List<Definition> definitions = new ArrayList<>();

    @Data
    public static class Definition {
        private String id;
        private boolean enabled = true;
        private Integer order;
        private String version;
        private Long timeoutMs;
        private Integer maxConcurrency;
        private Boolean failOpen;
        private List<String> includePaths = new ArrayList<>();
        private List<String> excludePaths = new ArrayList<>();
    }
}
