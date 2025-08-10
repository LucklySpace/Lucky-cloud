package com.xy.update.domain.tauri;

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
public class TauriUpdaterResponse {

    /**
     * 最新版本号，例如："1.0.3"
     */
    private String version;

    /**
     * 更新说明（更新日志），描述本次版本的主要改动内容
     */
    private String notes;

    /**
     * 发布日期，格式示例："2025-07-17T10:30:00Z"
     */
    private String pub_date;

    /**
     * 各平台下载信息，key 为平台名称（如 "windows", "macos", "linux"），
     * value 为对应平台的下载详情
     */
    private Map<String, TauriPlatformInfo> platforms;
}
