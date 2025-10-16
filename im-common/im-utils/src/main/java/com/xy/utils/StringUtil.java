package com.xy.utils;

import java.util.Arrays;

public class StringUtil {

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

    public static String replace(String template, String placeholder,
                                 String replacement) {
        return replace(template, placeholder, replacement, false);
    }

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

    public static boolean hasText(String str) {
        return hasText((CharSequence) str);
    }

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

}
