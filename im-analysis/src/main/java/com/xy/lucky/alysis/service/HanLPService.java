package com.xy.lucky.alysis.service;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLSentence;
import com.hankcs.hanlp.dictionary.py.Pinyin;
import com.hankcs.hanlp.seg.common.Term;
import com.xy.lucky.alysis.utils.HanLPConversion;
import com.xy.lucky.alysis.utils.HanLPDependencyParser;
import com.xy.lucky.alysis.utils.HanLPKeyWord;
import com.xy.lucky.alysis.utils.HanLPSegmenter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * HanLPService provides natural language processing services based on HanLP library.
 * HanLPService 提供基于 HanLP 库的自然语言处理服务。
 * 
 * <p>This service includes functionalities such as:
 * 这个服务包括以下功能：
 * <ul>
 *   <li>Keyword extraction 关键词提取</li>
 *   <li>Automatic summarization 自动摘要</li>
 *   <li>Phrase extraction 短语提取</li>
 *   <li>Word segmentation 分词</li>
 *   <li>Pinyin conversion 拼音转换</li>
 *   <li>Traditional/Simplified Chinese conversion 繁简体转换</li>
 *   <li>Dependency parsing 依存句法分析</li>
 *   <li>Sentence similarity calculation 句子相似度计算</li>
 * </ul>
 *
 * @author
 * @since 1.0.0
 */
@Service
public class HanLPService {

    private static final String FILTER_TERMS = "`~!@#$^&*()=|{}':;',\\[\\].<>/?~！@#￥……&*（）——|{}【】‘；：”“'。，、？";

    /**
     * Perform frequency statistics for words.
     * 对词语进行词频统计。
     *
     * @param allWords  All unique words list 去重后所有词列表
     * @param sentWords Words to analyze 分析词列表
     * @return Frequency array 词频数组
     */
    private static int[] statistic(List<String> allWords, List<String> sentWords) {
        int[] result = new int[allWords.size()];
        for (int i = 0; i < allWords.size(); i++) {
            result[i] = Collections.frequency(sentWords, allWords.get(i));
        }
        return result;
    }

    /**
     * Merge two word lists and remove duplicates.
     * 合并两个分词结果（去重）。
     *
     * @param list1 First word list 第一个词列表
     * @param list2 Second word list 第二个词列表
     * @return Merged word list without duplicates 去重后的合并词列表
     */
    private static List<String> mergeList(List<String> list1, List<String> list2) {
        List<String> result = new ArrayList<>();
        result.addAll(list1);
        result.addAll(list2);
        return result.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Split sentence into words and filter special characters.
     * 分词（过滤特殊字符的词）。
     *
     * @param sentence Sentence to split 待分词的句子
     * @return Word list after splitting 分词后的词列表
     */
    private static List<String> getSplitWords(String sentence) {
        return HanLP.segment(sentence).stream().map(a -> a.word).filter(s -> !FILTER_TERMS.contains(s)).collect(Collectors.toList());
    }

    /**
     * Extract keywords from content.
     * 从内容中提取关键词。
     *
     * @param content Content to extract keywords from 待提取关键词的内容
     * @param size    Number of keywords to extract 提取关键词的数量
     * @return Keyword list 关键词列表
     */
    public List<String> extractKeyword(String content, int size) {
        return HanLPKeyWord.extractKeyword(content, size);
    }

    /**
     * Extract summary from document.
     * 从文档中提取摘要。
     *
     * @param document Document to extract summary from 待提取摘要的文档
     * @param size     Number of summary sentences to extract 提取摘要句子的数量
     * @return Summary sentence list 摘要句子列表
     */
    public List<String> extractSummary(String document, int size) {
        return HanLPKeyWord.extractSummary(document, size);
    }

    /**
     * Extract phrases from text.
     * 从文本中提取短语。
     *
     * @param text Text to extract phrases from 待提取短语的文本
     * @param size Number of phrases to extract 提取短语的数量
     * @return Phrase list 短语列表
     */
    public List<String> extractPhrase(String text, int size) {
        return HanLPKeyWord.extractPhrase(text, size);
    }

    /**
     * Perform standard segmentation.
     * 执行标准分词。
     *
     * @param text Text to segment 待分词的文本
     * @return Segmentation result 分词结果
     */
    public List<Term> standardSegment(String text) {
        return HanLPSegmenter.standardSegment(text);
    }

    /**
     * Perform NLP segmentation.
     * 执行NLP分词。
     *
     * @param text Text to segment 待分词的文本
     * @return Segmentation result 分词结果
     */
    public List<Term> nlpSegment(String text) {
        return HanLPSegmenter.nlpSegment(text);
    }

    /**
     * Perform index segmentation.
     * 执行索引分词。
     *
     * @param text Text to segment 待分词的文本
     * @return Segmentation result 分词结果
     */
    public List<Term> indexSegment(String text) {
        return HanLPSegmenter.indexSegment(text);
    }

    /**
     * Perform N-short segmentation.
     * 执行N-最短路径分词。
     *
     * @param text Text to segment 待分词的文本
     * @return Segmentation result 分词结果
     */
    public List<Term> nShortSegment(String text) {
        return HanLPSegmenter.nShortSegment(text);
    }

    /**
     * Perform shortest path segmentation.
     * 执行最短路分词。
     *
     * @param text Text to segment 待分词的文本
     * @return Segmentation result 分词结果
     */
    public List<Term> shortestSegment(String text) {
        return HanLPSegmenter.shortestSegment(text);
    }

    /**
     * Perform CRF segmentation.
     * 执行CRF分词。
     *
     * @param text Text to segment 待分词的文本
     * @return Segmentation result 分词结果
     */
    public String crfSegment(String text) {
        return HanLPSegmenter.crfSegment(text);
    }

    /**
     * Perform speed segmentation.
     * 执行极速分词。
     *
     * @param text Text to segment 待分词的文本
     * @return Segmentation result 分词结果
     */
    public List<Term> speedSegment(String text) {
        return HanLPSegmenter.speedSegment(text);
    }

    /**
     * Perform NLP analysis.
     * 执行NLP分析。
     *
     * @param text Text to analyze 待分析的文本
     * @return Analysis result 分析结果
     */
    public String nlpAnalyze(String text) {
        return HanLPSegmenter.nlpAnalyze(text);
    }

    /**
     * Convert Chinese characters to Pinyin.
     * 将汉字转换为拼音。
     *
     * @param text Text to convert 待转换的文本
     * @return Pinyin list 拼音列表
     */
    public List<Pinyin> convertAndPrintPinyin(String text) {
        return HanLPConversion.convertAndPrintPinyin(text);
    }

    /**
     * Convert Chinese characters to first letter of Pinyin.
     * 将汉字转换为拼音首字母。
     *
     * @param text Text to convert 待转换的文本
     * @return First letter list 首字母列表
     */
    public List<String> convertAndPinyinFirstLetter(String text) {
        return HanLPConversion.convertAndPinyinFirstLetter(text);
    }

    /**
     * Convert Simplified Chinese to Traditional Chinese.
     * 将简体中文转换为繁体中文。
     *
     * @param text Simplified Chinese text 简体中文文本
     * @return Traditional Chinese text 繁体中文文本
     */
    public String convertToTraditional(String text) {
        return HanLPConversion.convertToTraditional(text);
    }

    /**
     * Convert Traditional Chinese to Simplified Chinese.
     * 将繁体中文转换为简体中文。
     *
     * @param text Traditional Chinese text 繁体中文文本
     * @return Simplified Chinese text 简体中文文本
     */
    public String convertToSimplified(String text) {
        return HanLPConversion.convertToSimplified(text);
    }

    /**
     * Parse dependency of sentence.
     * 解析句子的依存句法。
     *
     * @param sentence Sentence to parse 待解析的句子
     * @return Dependency parsing result 依存句法解析结果
     */
    public CoNLLSentence parseDependency(String sentence) {
        return HanLPDependencyParser.parse(sentence);
    }

    /**
     * Format dependency parsing result.
     * 格式化依存句法分析结果。
     *
     * @param sentence Dependency parsing result 依存句法分析结果
     * @return Formatted string 格式化的字符串
     */
    public String formatDependency(CoNLLSentence sentence) {
        return HanLPDependencyParser.format(sentence);
    }

    /**
     * Format dependency parsing result in reverse order.
     * 逆序格式化依存句法分析结果。
     *
     * @param sentence Dependency parsing result 依存句法分析结果
     * @return Reverse formatted string 逆序格式化的字符串
     */
    public String formatDependencyReverse(CoNLLSentence sentence) {
        return HanLPDependencyParser.formatReverse(sentence);
    }

    /**
     * Traverse dependency tree upward from specified index.
     * 从指定索引开始向上遍历依存树。
     *
     * @param sentence Dependency parsing result 依存句法分析结果
     * @param index    Starting word index 起始词索引
     * @return Traversal path 遍历路径
     */
    public String traverseUp(CoNLLSentence sentence, int index) {
        return HanLPDependencyParser.traverseUp(sentence, index);
    }

    /**
     * Calculate similarity between two sentences.
     * 计算两个句子之间的相似度。
     *
     * @param sentence1 First sentence 第一个句子
     * @param sentence2 Second sentence 第二个句子
     * @return Similarity value 相似度值
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