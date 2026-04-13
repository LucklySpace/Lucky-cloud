package com.xy.lucky.oss.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 上传白名单配置。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "upload.whitelist")
public class UploadWhitelistProperties {

    /**
     * 通用文件上传白名单（MIME）。
     */
    private Set<String> file = new LinkedHashSet<>(Set.of(
            "application/pdf",
            "text/plain",
            "text/csv",
            "application/json",
            "application/xml",
            "application/zip",
            "application/x-7z-compressed",
            "application/x-rar-compressed",
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "audio/mpeg",
            "audio/wav",
            "audio/x-wav",
            "audio/mp4",
            "video/mp4",
            "video/webm"
    ));

    /**
     * 图片上传白名单（MIME）。
     */
    private Set<String> image = new LinkedHashSet<>(Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/bmp"
    ));

    /**
     * 头像上传白名单（MIME）。
     */
    private Set<String> avatar = new LinkedHashSet<>(Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    ));

    /**
     * 音频上传白名单（MIME）。
     */
    private Set<String> audio = new LinkedHashSet<>(Set.of(
            "audio/mpeg",
            "audio/wav",
            "audio/x-wav",
            "audio/mp4",
            "audio/ogg"
    ));
}
