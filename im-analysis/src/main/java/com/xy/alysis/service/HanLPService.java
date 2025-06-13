package com.xy.alysis.service;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLSentence;
import com.hankcs.hanlp.dictionary.py.Pinyin;
import com.hankcs.hanlp.seg.common.Term;
import com.xy.alysis.utils.HanLPConversion;
import com.xy.alysis.utils.HanLPDependencyParser;
import com.xy.alysis.utils.HanLPKeyWord;
import com.xy.alysis.utils.HanLPSegmenter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
public class HanLPService {

    private static final String FILTER_TERMS = "`~!@#$^&*()=|{}':;',\\[\\].<>/?~！@#￥……&*（）——|{}【】‘；：”“'。，、？";

    /**
     * 词频分析
     *
     * @param allWords  去重后所有词列表
     * @param sentWords 分析词列表
     * @return
     */
    private static int[] statistic(List<String> allWords, List<String> sentWords) {
        int[] result = new int[allWords.size()];
        for (int i = 0; i < allWords.size(); i++) {
            result[i] = Collections.frequency(sentWords, allWords.get(i));
        }
        return result;
    }

    /**
     * 合并两个分词结果（去重）
     */
    private static List<String> mergeList(List<String> list1, List<String> list2) {
        List<String> result = new ArrayList<>();
        result.addAll(list1);
        result.addAll(list2);
        return result.stream().distinct().collect(Collectors.toList());
    }

    /**
     * 分词（过滤特殊字符的词）
     */
    private static List<String> getSplitWords(String sentence) {
        return HanLP.segment(sentence).stream().map(a -> a.word).filter(s -> !FILTER_TERMS.contains(s)).collect(Collectors.toList());
    }

    // 关键词提取
    public List<String> extractKeyword(String content, int size) {
        return HanLPKeyWord.extractKeyword(content, size);
    }

    // 自动摘要
    public List<String> extractSummary(String document, int size) {
        return HanLPKeyWord.extractSummary(document, size);
    }

    // 短语提取
    public List<String> extractPhrase(String text, int size) {
        return HanLPKeyWord.extractPhrase(text, size);
    }

    // 标准分词
    public List<Term> standardSegment(String text) {
        return HanLPSegmenter.standardSegment(text);
    }

    // NLP 分词
    public List<Term> nlpSegment(String text) {
        return HanLPSegmenter.nlpSegment(text);
    }

    // 索引分词
    public List<Term> indexSegment(String text) {
        return HanLPSegmenter.indexSegment(text);
    }

    // N-最短路径分词
    public List<Term> nShortSegment(String text) {
        return HanLPSegmenter.nShortSegment(text);
    }

    // 最短路分词
    public List<Term> shortestSegment(String text) {
        return HanLPSegmenter.shortestSegment(text);
    }

    // CRF 分词
    public String crfSegment(String text) {
        return HanLPSegmenter.crfSegment(text);
    }

    // 极速分词
    public List<Term> speedSegment(String text) {
        return HanLPSegmenter.speedSegment(text);
    }

    // NLP 分析
    public String nlpAnalyze(String text) {
        return HanLPSegmenter.nlpAnalyze(text);
    }

    // 汉字转拼音
    public List<Pinyin> convertAndPrintPinyin(String text) {
        return HanLPConversion.convertAndPrintPinyin(text);
    }

    // 汉字转拼音首字母
    public List<String> convertAndPinyinFirstLetter(String text) {
        return HanLPConversion.convertAndPinyinFirstLetter(text);
    }

    // 简体转繁体
    public String convertToTraditional(String text) {
        return HanLPConversion.convertToTraditional(text);
    }

    // 繁体转简体
    public String convertToSimplified(String text) {
        return HanLPConversion.convertToSimplified(text);
    }

    // 依存句法分析
    public CoNLLSentence parseDependency(String sentence) {
        return HanLPDependencyParser.parse(sentence);
    }

    // 格式化依存句法分析结果
    public String formatDependency(CoNLLSentence sentence) {
        return HanLPDependencyParser.format(sentence);
    }

//    public double getSimilarity(String sentence1, String sentence2) {
//
//        List<String> sent1WordList =  standardSegment(sentence1).stream()
//                .map(term -> term.word)
//                .collect(Collectors.toList());
//
//        List<String> sent2WordList =standardSegment(sentence2).stream()
//                .map(term -> term.word)
//                .collect(Collectors.toList());
//
//        List<String> allWords = mergeList(sent1WordList, sent2WordList);
//
//        int[] statistic1 = statistic(allWords, sent1WordList);
//        int[] statistic2 = statistic(allWords, sent2WordList);
//
//        double dividend = 0;
//        double divisor1 = 0;
//        double divisor2 = 0;
//
//        for (int i = 0; i < statistic1.length; i++) {
//            dividend += statistic1[i] * statistic2[i];
//            divisor1 += Math.pow(statistic1[i], 2);
//            divisor2 += Math.pow(statistic2[i], 2);
//        }
//
//        return dividend / (Math.sqrt(divisor1) * Math.sqrt(divisor2));
//    }

    // 逆序格式化依存句法分析结果
    public String formatDependencyReverse(CoNLLSentence sentence) {
        return HanLPDependencyParser.formatReverse(sentence);
    }

    // 遍历依存句法分析结果
    public String traverseUp(CoNLLSentence sentence, int index) {
        return HanLPDependencyParser.traverseUp(sentence, index);
    }

    /**
     * 相似性比较
     *
     * @param sentence1 句子1
     * @param sentence2 句子2
     * @return
     */
    public double similarity(String sentence1, String sentence2) {
        List<List<String>> wordLists = Stream.of(sentence1, sentence2)
                .map(this::standardSegment)
                .map(list -> list.stream().map(term -> term.word).collect(Collectors.toList()))
                .collect(Collectors.toList());

        List<String> allWords = mergeList(wordLists.get(0), wordLists.get(1));
        int[] statistic1 = statistic(allWords, wordLists.get(0));
        int[] statistic2 = statistic(allWords, wordLists.get(1));

        double dividend = IntStream.range(0, statistic1.length)
                .mapToDouble(i -> statistic1[i] * statistic2[i])
                .sum();
        double divisor1 = Math.sqrt(Arrays.stream(statistic1).map(i -> i * i).sum());
        double divisor2 = Math.sqrt(Arrays.stream(statistic2).map(i -> i * i).sum());

        return dividend / (divisor1 * divisor2);
    }

} 