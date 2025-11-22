package com.xy.lucky.update.domain.tauri;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 平台下载信息对象
 * <p>
 * 用于封装单个平台（如 Windows、macOS、Linux）下的更新签名和下载链接。
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "TauriPlatformInfo", description = "单个平台的更新信息：包含数字签名与下载地址")
public class TauriPlatformInfo {

    /**
     * 更新包的数字签名，用于校验下载文件的完整性和安全性
     * 每次打包后的签名，所以每次都不一样，macOS 默认在 /src-tauri/target/release/bundle/macos/my-tauri-app.app.tar.gz.sig 这个位置，将这个文件打开，复制里面的内容替换即可
     */
    @Schema(
            description = "更新包的数字签名，用于校验下载文件的完整性和安全性。例如 macOS 签名文件的内容。",
            example = "3d2f1a7b9c8e4f5a6b7c8d9e0f1a2b3c4d5e6f7a"
    )
    private String signature;

    /**
     * 该平台对应的更新包下载地址
     */
    @Schema(
            description = "该平台对应的更新包下载地址（支持 http/https）。前端可直接使用该 URL 触发下载。",
            example = "https://example.com/downloads/app-1.0.3-windows-x86_64.msi"
    )
    private String url;
}
