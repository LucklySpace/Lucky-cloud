package com.xy.alysis.utils;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.dictionary.py.Pinyin;

import java.util.List;
import java.util.stream.Collectors;

/**
 * HanLPConversionTool 封装了汉字拼音转换与简繁转换的功能。
 *
 * <p>示例用法：
 * <pre>
 *     // 拼音转换示例
 *     String text = "重载不是重任";
 *     HanLPConversionTool.convertAndPrintPinyin(text);
 *
 *     // 简体转繁体
 *     String simplified = "用笔记本电脑写程序";
 *     String traditional = HanLPConversionTool.convertToTraditional(simplified);
 *     System.out.println("繁体：" + traditional);
 *
 *     // 繁体转简体
 *     String traditionalText = "「以後等妳當上皇后，就能買士多啤梨慶祝了」";
 *     String simplifiedText = HanLPConversionTool.convertToSimplified(traditionalText);
 *     System.out.println("简体：" + simplifiedText);
 * </pre>
 * <p>
 * HanLP 不仅支持基本的汉字转拼音，还能实现声母、韵母、音调、符号音调以及输入法头等功能；
 * 同时，HanLP 也提供了简繁转换功能，能够区分一些简繁歧义词。
 */
public class HanLPConversion {

    /**
     * 将汉字转换为拼音列表，并打印出不同形式的拼音信息。
     *
     * @param text 待转换的汉字文本
     */
    public static List<Pinyin> convertAndPrintPinyin(String text) {
        List<Pinyin> pinyinList = HanLP.convertToPinyinList(text);

        System.out.print("原文,");
        for (char c : text.toCharArray()) {
            System.out.printf("%c,", c);
        }
        System.out.println();

        System.out.print("拼音（数字音调）,");
        for (Pinyin pinyin : pinyinList) {
            System.out.printf("%s,", pinyin);
        }
        System.out.println();

        System.out.print("拼音（符号音调）,");
        for (Pinyin pinyin : pinyinList) {
            System.out.printf("%s,", pinyin.getPinyinWithToneMark());
        }
        System.out.println();

        System.out.print("拼音（无音调）,");
        for (Pinyin pinyin : pinyinList) {
            System.out.printf("%s,", pinyin.getPinyinWithoutTone());
        }
        System.out.println();

        System.out.print("声调,");
        for (Pinyin pinyin : pinyinList) {
            System.out.printf("%s,", pinyin.getTone());
        }
        System.out.println();

        System.out.print("声母,");
        for (Pinyin pinyin : pinyinList) {
            System.out.printf("%s,", pinyin.getShengmu());
        }
        System.out.println();

        System.out.print("韵母,");
        for (Pinyin pinyin : pinyinList) {
            System.out.printf("%s,", pinyin.getYunmu());
        }
        System.out.println();

        System.out.print("输入法头,");
        for (Pinyin pinyin : pinyinList) {
            System.out.printf("%s,", pinyin.getHead());
        }
        System.out.println();
        return pinyinList;
    }

    /**
     * 汉字转中文首字母。
     *
     * @param text 简体中文文本
     * @return 转换后的字母
     */
    public static List<String> convertAndPinyinFirstLetter(String text) {
        return HanLP.convertToPinyinList(text).stream()
                .map(pinyin -> String.valueOf(pinyin.getFirstChar()))
                .collect(Collectors.toList());
    }

    /**
     * 将简体中文转换为繁体中文。
     *
     * @param text 简体中文文本
     * @return 转换后的繁体中文文本
     */
    public static String convertToTraditional(String text) {
        return HanLP.convertToTraditionalChinese(text);
    }

    /**
     * 将繁体中文转换为简体中文。
     *
     * @param text 繁体中文文本
     * @return 转换后的简体中文文本
     */
    public static String convertToSimplified(String text) {
        return HanLP.convertToSimplifiedChinese(text);
    }

    // 示例 main 方法
    public static void main(String[] args) {
        String text = "重载不是重任";
        System.out.println("=== 拼音转换 ===");
        convertAndPrintPinyin(text);

        System.out.println("=== 简体转繁体 ===");
        String simplified = "用笔记本电脑写程序";
        System.out.println("简体: " + simplified);
        System.out.println("繁体: " + convertToTraditional(simplified));

        System.out.println("=== 繁体转简体 ===");
        String traditionalText = "「以後等妳當上皇后，就能買士多啤梨慶祝了」";
        System.out.println("繁体: " + traditionalText);
        System.out.println("简体: " + convertToSimplified(traditionalText));
    }
}
