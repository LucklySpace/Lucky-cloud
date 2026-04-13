package com.xy.lucky.oss.util;

import com.xy.lucky.oss.config.UploadWhitelistProperties;
import com.xy.lucky.oss.enums.StorageBucketEnum;
import com.xy.lucky.oss.exception.FileException;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于文件魔数的文件类型检测与白名单校验器。
 */
@Component
@RequiredArgsConstructor
public class FileTypeDetector {

    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";
    private final Tika tika = new Tika();
    private final UploadWhitelistProperties uploadWhitelistProperties;

    /**
     * 检测上传文件类型并返回结构化结果。
     *
     * @param file 上传文件
     * @return 检测结果
     */
    public DetectedFileType detect(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileException("上传文件不能为空");
        }
        try (InputStream inputStream = new BufferedInputStream(file.getInputStream())) {
            String mimeType = normalizeMimeType(tika.detect(inputStream));
            StorageBucketEnum bucketEnum = StorageBucketEnum.fromMimeType(mimeType).orElse(StorageBucketEnum.OTHER);
            String extension = resolveExtensionByMimeType(mimeType);
            return new DetectedFileType(mimeType, extension, bucketEnum.getCode());
        } catch (IOException e) {
            throw new FileException("文件类型检测失败");
        }
    }

    /**
     * 按场景执行上传白名单校验。
     *
     * @param detectedFileType 已检测的文件类型
     * @param scene            上传场景
     */
    public void validateByScene(DetectedFileType detectedFileType, UploadScene scene) {
        Set<String> whitelist = switch (scene) {
            case FILE -> uploadWhitelistProperties.getFile();
            case IMAGE -> uploadWhitelistProperties.getImage();
            case AVATAR -> uploadWhitelistProperties.getAvatar();
            case AUDIO -> uploadWhitelistProperties.getAudio();
        };
        validateWhitelist(detectedFileType.mimeType(), whitelist, scene.getLabel());
    }

    /**
     * 白名单校验。
     *
     * @param mimeType  MIME 类型
     * @param whitelist 白名单
     * @param sceneName 场景名称
     */
    public void validateWhitelist(String mimeType, Set<String> whitelist, String sceneName) {
        Set<String> normalizedWhitelist = whitelist == null
                ? Set.of()
                : whitelist.stream()
                .map(this::normalizeMimeType)
                .collect(Collectors.toSet());
        if (normalizedWhitelist.isEmpty()) {
            throw new FileException(sceneName + "上传白名单未配置");
        }
        if (!normalizedWhitelist.contains(normalizeMimeType(mimeType))) {
            throw new FileException(sceneName + "不支持该文件类型: " + mimeType);
        }
    }

    /**
     * 归一化 MIME 类型字符串。
     *
     * @param mimeType 原始 MIME
     * @return 归一化后的 MIME
     */
    public String normalizeMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return DEFAULT_MIME_TYPE;
        }
        String lower = mimeType.trim().toLowerCase(Locale.ROOT);
        int semicolonIndex = lower.indexOf(';');
        if (semicolonIndex > 0) {
            lower = lower.substring(0, semicolonIndex).trim();
        }
        return lower.isBlank() ? DEFAULT_MIME_TYPE : lower;
    }

    /**
     * 根据 MIME 解析默认扩展名。
     *
     * @param mimeType MIME 类型
     * @return 扩展名（不含 "."）
     */
    public String resolveExtensionByMimeType(String mimeType) {
        try {
            String extension = MimeTypes.getDefaultMimeTypes()
                    .forName(normalizeMimeType(mimeType))
                    .getExtension();
            if (extension == null || extension.isBlank()) {
                return "bin";
            }
            return extension.startsWith(".") ? extension.substring(1) : extension;
        } catch (MimeTypeException e) {
            return "bin";
        }
    }

    /**
     * 文件类型检测结果。
     *
     * @param mimeType   识别出的 MIME 类型
     * @param extension  推荐扩展名（不含 "."）
     * @param bucketCode 推荐桶编码
     */
    public record DetectedFileType(String mimeType, String extension, String bucketCode) {
    }

    /**
     * 上传场景定义。
     */
    public enum UploadScene {
        FILE("文件"),
        IMAGE("图片"),
        AVATAR("头像"),
        AUDIO("音频");

        private final String label;

        UploadScene(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
