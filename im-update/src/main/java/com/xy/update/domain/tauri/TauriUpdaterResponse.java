package com.xy.update.domain.tauri;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * Tauri 更新响应对象
 * <p>
 * 用于封装从 Tauri 更新服务获取的版本、更新说明、发布时间以及各平台下载信息。
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "TauriUpdaterResponse", description = "Tauri 更新检查响应：包含版本号、更新说明、发布日与各平台下载信息")
public class TauriUpdaterResponse {

    /**
     * 最新版本号，例如："1.0.3"
     */
    @Schema(description = "最新版本号，例如：\"1.0.3\"", example = "1.0.3")
    private String version;

    /**
     * 更新说明（更新日志），描述本次版本的主要改动内容
     */
    @Schema(description = "更新说明（更新日志），描述本次版本的主要改动内容", example = "修复若干 bug，优化安装体验。")
    private String notes;

    /**
     * 发布日期，格式示例："2025-07-17T10:30:00Z"
     * <p>
     * 注意：字段在 JSON 中为 snake_case（pub_date），因此使用 @JsonProperty 绑定序列化名称。
     */
    @JsonProperty("pub_date")
    @Schema(description = "发布日期，ISO 8601 格式，例如：\"2025-07-17T10:30:00Z\"", example = "2025-07-17T10:30:00Z")
    private String pub_date;

    /**
     * 各平台下载信息，key 为平台名称（如 "windows", "macos", "linux"），
     * value 为对应平台的下载详情（TauriPlatformInfo）
     * <p>
     * 在 OpenAPI 中使用 additionalProperties 指定 Map 值的类型为 TauriPlatformInfo，
     * UI 中会展示 platforms 的键值结构以及嵌套模型字段。
     */
    @Schema(
            description = "各平台下载信息，key 为平台名称（如 windows-x86_64、mac-aarch64），value 为对应平台的下载详情"
    )
    private Map<String, TauriPlatformInfo> platforms;
}
