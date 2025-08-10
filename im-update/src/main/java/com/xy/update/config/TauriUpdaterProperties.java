package com.xy.update.config;


import com.xy.update.domain.tauri.TauriPlatformInfo;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Instant;
import java.util.Map;

@ConfigurationProperties(prefix = "updater")
public record TauriUpdaterProperties(

        String version,

        String notes,

        String pubDate,

        Map<String, TauriPlatformInfo> platforms
) {

    public Map<String, TauriPlatformInfo> getPlatformInfo(String platform) {
        return Map.of(platform, platforms.get(platform));
    }
}