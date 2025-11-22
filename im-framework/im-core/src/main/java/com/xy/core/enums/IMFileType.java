package com.xy.core.enums;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 文件类型枚举：每个类型绑定一组后缀
 */
@Getter
@NoArgsConstructor
public enum IMFileType {
    VIDEO("mp4", "mov", "avi", "wmv", "mkv", "mpeg", "flv", "webm"),
    MARKDOWN("md"),
    IMAGE("jpg", "jpeg", "png", "gif", "bmp", "svg", "webp"),
    PDF("pdf"),
    WORD("doc", "docx", "odt", "rtf"),
    EXCEL("xls", "xlsx"),
    POWERPOINT("ppt", "pptx"),
    OTHER();  // 默认类型，无需后缀

    private Set<String> extensions;

    IMFileType(String... exts) {
        this.extensions = new HashSet<>();
        // 存储为小写，方便匹配
        Arrays.stream(exts).map(String::toLowerCase).forEach(this.extensions::add);
    }

    /**
     * 根据文件名后缀返回对应的 FileType
     *
     * @param fileName 文件名，例如 "example.MP4"
     * @return 匹配到的 FileType，未匹配到则返回 OTHER
     */
    public static IMFileType fromFileName(String fileName) {
        if (fileName == null) {
            return OTHER;
        }
        String ext = getExtension(fileName);
        return fromExtension(ext);
    }

    /**
     * 根据扩展名（不含点）返回对应的 FileType
     *
     * @param extension 扩展名，如 "pdf"
     * @return 匹配到的 FileType，未匹配到则返回 OTHER
     */
    public static IMFileType fromExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return OTHER;
        }
        String ext = extension.toLowerCase(Locale.ROOT);
        for (IMFileType type : values()) {
            if (type.extensions.contains(ext)) {
                return type;
            }
        }
        return OTHER;
    }

    /**
     * 从文件名中提取扩展名（不含点），小写返回
     */
    private static String getExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx == -1 || idx == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 获取此枚举类型对应的字符串标识
     */
    public String getType() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
