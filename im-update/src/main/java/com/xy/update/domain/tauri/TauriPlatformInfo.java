package com.xy.update.domain.tauri;

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
public class TauriPlatformInfo {

    /**
     * 更新包的数字签名，用于校验下载文件的完整性和安全性
     * 每次打包后的签名，所以每次都不一样，macOS 默认在 /src-tauri/target/release/bundle/macos/my-tauri-app.app.tar.gz.sig 这个位置，将这个文件打开，复制里面的内容替换即可
     * <p>
     * 示例："3d2f1a7b9c8e4f5a..."
     */
    private String signature;

    /**
     * 该平台对应的更新包下载地址
     * <p>
     * 示例："https://example.com/downloads/app-1.0.3-windows.zip"
     */
    private String url;
}
