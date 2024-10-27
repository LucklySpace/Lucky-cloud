package com.xy.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "response.interceptor")
public class ResponseInterceptorConfig {
    private List<String> ignoredUrls = new ArrayList<>();

    public List<String> getIgnoredUrls() {
        return ignoredUrls;
    }

    public void setIgnoredUrls(List<String> ignoredUrls) {
        this.ignoredUrls = ignoredUrls;
    }
}
