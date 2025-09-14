package com.xy.update.config;

import com.xy.update.domain.tauri.TauriPlatformInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Tauri 更新配置属性（从 application.yml 或 application.properties 读取）
 * <p>
 * 说明：
 * - 使用 @ConfigurationProperties(prefix = "updater") 绑定配置
 * - 使用 @Schema 为 OpenAPI/Swagger 提供模型描述与示例，Knife4j 将显示这些信息
 */
@ConfigurationProperties(prefix = "updater")
@Schema(name = "TauriUpdaterProperties", description = "Tauri 更新配置：版本、更新说明、发布时间与各平台下载信息")
public record TauriUpdaterProperties(

        @Schema(description = "最新版本号，例如：\"1.0.3\"", example = "1.0.3")
        String version,

        @Schema(description = "更新说明（release notes）", example = "修复若干 bug；优化安装流程")
        String notes,

        @Schema(description = "发布时间，ISO 8601 格式，例如：\"2025-09-12T10:30:00Z\"", example = "2025-09-12T10:30:00Z")
        String pubDate,

        @Schema(
                description = "各平台信息，key 为平台标识（如 windows-x86_64、mac-aarch64），value 为对应平台的下载信息"
        )
        Map<String, TauriPlatformInfo> platforms

) {

    /**
     * 按平台名称返回对应平台信息。
     * 为避免出现 NullPointerException，这里在找不到时返回一个空的 Map（而非 Map.of(platform, null)）
     *
     * @param platform 平台标识，例如 "windows-x86_64"
     * @return 如果存在则返回单元素 Map；否则返回空 Map
     */
    public Map<String, TauriPlatformInfo> getPlatformInfo(String platform) {
        if (platform == null || platforms == null) {
            return Map.of();
        }
        TauriPlatformInfo info = platforms.get(platform);
        if (info == null) {
            return Map.of();
        }
        return Map.of(platform, info);
    }
}
