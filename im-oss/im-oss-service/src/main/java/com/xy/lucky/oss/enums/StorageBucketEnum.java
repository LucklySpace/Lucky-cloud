package com.xy.lucky.oss.enums;


import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 存储桶枚举（按文件类型分桶）
 * <p>
 * 设计要点：
 * - 每个枚举项保存 code/name/扩展名集合（均为小写，不包含 '.'）
 * - 静态构建扩展名 -> 枚举 映射，查询非常快速
 * - 提供按文件名/后缀/编码查找的便捷方法
 * <p>
 * 使用示例：
 * StorageBucketEnum.fromFilename("a.pdf").ifPresent(b -> b.getCode());
 * StorageBucketEnum.getBucketCodeByFilename("photo.JPG"); // 返回 "image"
 */
@Getter
public enum StorageBucketEnum {

    DOCUMENT("document", "文档文件桶", asSet(
            "txt", "rtf", "ofd", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf",
            "csv", "xml", "json", "html", "htm", "md", "epub", "odt", "ods", "odp", "tex", "xps",
            "log", "ini", "cfg", "properties", "yaml", "yml", "ps", "srt" // 额外文本类
    )),
    PACKAGE("package", "压缩文件桶", asSet(
            "zip", "rar", "7z", "tar", "tgz", "tar.gz", "wim", "gz", "bz2", "xz", "zst"
    )),
    AUDIO("audio", "音频文件桶", asSet(
            "mp3", "wav", "flac", "aac", "ogg", "oga", "aiff", "m4a", "wma", "midi", "mid", "opus"
    )),
    VIDEO("video", "视频文件桶", asSet(
            "mp4", "m4v", "avi", "mov", "wmv", "flv", "mkv", "webm", "mpeg", "mpg", "ts", "rmvb", "3gp", "m2ts", "mts"
    )),
    IMAGE("image", "图片文件桶", asSet(
            "jpeg", "jpg", "png", "bmp", "webp", "gif", "svg", "tif", "tiff", "heic", "heif", "avif", "ico", "raw"
    )),
    INSTALLER("installer", "安装包文件桶", asSet(
            "exe", "msi", "apk", "dmg", "pkg", "appimage", "deb", "rpm", "bat", "sh"
    )),
    THUMBNAIL("thumbnail", "图片缩略图文件桶", asSet(
            "thumbnail" // 逻辑占位，如需可用实际后缀
    )),
    OTHER("other", "其他文件桶", asSet("*")); // "*" 表示任意其他类型

    // 后缀 -> 枚举 映射（静态构造）
    private static final Map<String, StorageBucketEnum> EXTENSION_MAP;
    // code -> enum 映射（加速按 code 查询）
    private static final Map<String, StorageBucketEnum> CODE_MAP;

    static {
        Map<String, StorageBucketEnum> extMap = new HashMap<>();
        for (StorageBucketEnum e : values()) {
            // 注册类型集合中的每一个后缀（跳过 "*"）
            for (String t : e.types) {
                if (t == null) continue;
                String lower = t.toLowerCase(Locale.ROOT);
                if ("*".equals(lower)) continue;
                extMap.putIfAbsent(lower, e);
            }
        }
        EXTENSION_MAP = Collections.unmodifiableMap(extMap);

        Map<String, StorageBucketEnum> codeMap = Arrays.stream(values())
                .collect(Collectors.toMap(e -> e.code, e -> e));
        CODE_MAP = Collections.unmodifiableMap(codeMap);
    }

    private final String code;
    private final String name;
    private final Set<String> types;

    StorageBucketEnum(String code, String name, Set<String> types) {
        this.code = code;
        this.name = name;
        this.types = Collections.unmodifiableSet(new LinkedHashSet<>(types));
    }

    // -------------------- 工具静态方法 --------------------

    /**
     * 把数组转换为不可变小写 Set
     */
    private static Set<String> asSet(String... arr) {
        if (arr == null || arr.length == 0) return Collections.emptySet();
        LinkedHashSet<String> s = new LinkedHashSet<>();
        for (String a : arr) {
            if (a == null) continue;
            s.add(a.toLowerCase(Locale.ROOT));
        }
        return s;
    }

    /**
     * 根据 code 获取枚举（可能返回 null）
     */
    public static StorageBucketEnum fromCode(String code) {
        if (code == null) return null;
        return CODE_MAP.get(code);
    }

    /**
     * 根据 code 获取枚举（Optional 形式）
     */
    public static Optional<StorageBucketEnum> fromCodeOptional(String code) {
        return Optional.ofNullable(fromCode(code));
    }

    /**
     * 根据文件后缀（不含点）查找桶类型（返回 Optional）
     *
     * @param suffix 文件后缀（如 "jpg" 或 "JPG"），支持 null/空
     */
    public static Optional<StorageBucketEnum> fromSuffix(String suffix) {
        if (StringUtils.isBlank(suffix)) return Optional.empty();
        String key = suffix.trim().toLowerCase(Locale.ROOT);
        StorageBucketEnum e = EXTENSION_MAP.get(key);
        return Optional.ofNullable(e);
    }

    /**
     * 根据文件名解析后缀并查找桶类型（返回 Optional）
     *
     * @param filename 文件名（可包含路径）
     */
    public static Optional<StorageBucketEnum> fromFilename(String filename) {
        String suf = getSuffix(filename);
        Optional<StorageBucketEnum> opt = fromSuffix(suf);
        if (opt.isPresent()) return opt;
        // 不在映射表中的，归为 OTHER
        return Optional.of(OTHER);
    }

    /**
     * 根据文件名直接返回桶 code（若无法识别则返回 "other"）
     */
    public static String getBucketCodeByFilename(String filename) {
        return fromFilename(filename).map(StorageBucketEnum::getCode).orElse(OTHER.code);
    }

    /**
     * 根据文件名直接返回桶名称（若无法识别则返回 OTHER 的名称）
     */
    public static String getBucketNameByFilename(String filename) {
        return fromFilename(filename).map(StorageBucketEnum::getName).orElse(OTHER.name);
    }

    /**
     * 提取文件后缀（不含点），全部转为小写；无法解析时返回 null
     */
    public static String getSuffix(String filename) {
        if (StringUtils.isBlank(filename)) return null;
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return null;
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 判断给定后缀是否属于某个桶
     *
     * @param suffix 后缀（如 "png"）
     * @param bucket 目标桶
     */
    public static boolean suffixBelongsTo(String suffix, StorageBucketEnum bucket) {
        if (suffix == null || bucket == null) return false;
        return bucket.types.contains(suffix.toLowerCase(Locale.ROOT));
    }

    // -------------------- 示例/辅助 --------------------

    @Override
    public String toString() {
        return "StorageBucketEnum{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", types=" + types +
                '}';
    }
}
