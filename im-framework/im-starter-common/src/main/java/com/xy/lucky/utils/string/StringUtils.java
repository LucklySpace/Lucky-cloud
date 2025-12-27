package com.xy.lucky.utils.string;

import java.util.Arrays;
import java.util.Objects;

public class StringUtils {

    public static String C_SLASH = "/";

    public static boolean hasLength(CharSequence str) {
        return ((str != null) && (str.length() > 0));
    }

    public static boolean hasLength(String str) {
        return hasLength((CharSequence) str);
    }

    public static boolean checkEmpty(String str) {
        return ((str == null) || ("".equals(str.trim())));
    }

    public static boolean isBlank(String str) {
        return str == null || str.trim().length() == 0;
    }


    public static boolean isNotBlank(String str) {
        return str != null && str.trim().length() != 0;
    }

    /**
     * 判断字符串是否包含字符
     *
     * @param str 要检查的字符串
     * @return 如果字符串包含字符则返回true，否则返回false
     */
    public static boolean hasText(CharSequence str) {
        if (!(hasLength(str))) {
            return false;
        }

        int strLen = str.length();
        for (int i = 0; i < strLen; ++i) {
            if (!(Character.isWhitespace(str.charAt(i)))) {
                return true;
            }
        }

        return false;
    }

    /**
     * 替换字符串中的占位符
     *
     * @param template    要处理的字符串
     * @param placeholder 要替换的占位符
     * @param replacement 替换后的字符串
     * @return 处理后的字符串
     */
    public static String replace(String template, String placeholder,
                                 String replacement) {
        return replace(template, placeholder, replacement, false);
    }

    /**
     * 替换字符串中的占位符
     *
     * @param template    要处理的字符串
     * @param placeholder 要替换的占位符
     * @param replacement 替换后的字符串
     * @param wholeWords  是否只替换完整的单词
     * @return 处理后的字符串
     */
    public static String replace(String template, String placeholder,
                                 String replacement, boolean wholeWords) {
        int loc = template.indexOf(placeholder);
        if (loc < 0) {
            return template;
        }

        boolean actuallyReplace = (!(wholeWords))
                || (loc + placeholder.length() == template.length())
                || (!(Character.isJavaIdentifierPart(template.charAt(loc
                + placeholder.length()))));

        String actualReplacement = (actuallyReplace) ? replacement
                : placeholder;

        return new StringBuffer(template.substring(0, loc))
                .append(actualReplacement)
                .append(replace(template.substring(loc + placeholder.length()),
                        placeholder, replacement, wholeWords)).toString();
    }

    /**
     * 删除字符串中的空白字符
     *
     * @param str 要处理的字符串
     * @return 处理后的字符串
     */
    public static String trimWhitespace(String str) {
        if (!(hasLength(str))) {
            return str;
        }

        StringBuilder sb = new StringBuilder(str);
        while ((sb.length() > 0) && (Character.isWhitespace(sb.charAt(0)))) {
            sb.deleteCharAt(0);
        }

        while ((sb.length() > 0) && (Character.isWhitespace(sb.charAt(sb.length() - 1)))) {
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
    }

    /**
     * 判断字符串是否包含字符
     *
     * @param str 要检查的字符串
     * @return 如果字符串包含空白字符则返回true，否则返回false
     */
    public static boolean hasText(String str) {
        return hasText((CharSequence) str);
    }

    /**
     * 判断字符串是否包含指定的索引
     *
     * @param index 要检查的索引
     * @param group 索引组
     * @return 如果字符串包含指定的索引则返回true，否则返回false
     */
    public static boolean isInGroup(String index, String group) {
        if (isBlank(index) || isBlank(group)) {
            return false;
        }

        String[] ss = group.split(",");
        for (String s : ss) {
            if (s.equals(index)) {
                return true;
            }
        }
        return false;
    }

    public static String headToUpperCase(String str) {
        if (isBlank(str)) {
            return str;
        }

        if (str.length() == 1) {
            return str.toUpperCase();
        }

        String a = str.substring(0, 1);
        String b = str.substring(1);

        return a.toUpperCase() + b;
    }

    public static String getString(int num) {
        char[] array = new char[num];
        Arrays.fill(array, '0');
        String result = new String(array);
        return result;
    }

    /**
     * 格式化字符串，支持类似"{}/{}/{}"的占位符
     * <p>
     * 示例: StringUtils.format("{}/{}/{}", "http://localhost:9000", "mybucket", "myfile.txt")
     * 返回: "http://localhost:9000/mybucket/myfile.txt"
     *
     * @param format 格式字符串，包含占位符
     * @param args   要替换占位符的参数
     * @return 格式化后的字符串
     */
    public static String format(String format, Object... args) {
        if (format == null) {
            return null;
        }

        if (args == null) {
            return format;
        }

        StringBuilder result = new StringBuilder(format.length() + 50);
        int argIndex = 0;

        for (int i = 0; i < format.length(); i++) {
            if (format.charAt(i) == '{' && i + 1 < format.length() && format.charAt(i + 1) == '}') {
                // 找到一个 "{}" 占位符
                if (argIndex < args.length) {
                    result.append(args[argIndex]);
                    argIndex++;
                } else {
                    // 参数不足，保留原占位符
                    result.append("{}");
                }
                i++; // 跳过下一个 '}' 字符
            } else if (format.charAt(i) == '\\' && i + 1 < format.length() && format.charAt(i + 1) == '{') {
                // 转义的左花括号
                result.append('{');
                i++; // 跳过下一个 '{' 字符
            } else {
                result.append(format.charAt(i));
            }
        }

        return result.toString();
    }

    /**
     * 检查字符串是否以指定的后缀结尾
     *
     * @param str    要检查的字符串
     * @param suffix 后缀
     * @return 如果字符串以指定后缀结尾则返回true，否则返回false
     */
    public static boolean endsWith(String str, String suffix) {
        if (str == null || suffix == null) {
            return false;
        }

        if (suffix.length() > str.length()) {
            return false;
        }

        return str.regionMatches(str.length() - suffix.length(), suffix, 0, suffix.length());
    }

    /**
     * 比较两个字符串是否相等
     *
     * @param str   字符串1
     * @param other 字符串2
     * @return 如果两个字符串相等则返回true，否则返回false
     */
    public static boolean equals(String str, String other) {
        return Objects.equals(str, other);
    }

    public static boolean endWith(String path, String cSlash) {
        return path.endsWith(cSlash);
    }
}
