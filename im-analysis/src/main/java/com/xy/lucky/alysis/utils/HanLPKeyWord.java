package com.xy.lucky.alysis.utils;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.model.crf.CRFLexicalAnalyzer;
import com.hankcs.hanlp.seg.Dijkstra.DijkstraSegment;
import com.hankcs.hanlp.seg.NShort.NShortSegment;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.IndexTokenizer;
import com.hankcs.hanlp.tokenizer.NLPTokenizer;
import com.hankcs.hanlp.tokenizer.SpeedTokenizer;
import com.hankcs.hanlp.tokenizer.StandardTokenizer;
import lombok.SneakyThrows;

import java.util.List;
import java.util.stream.Collectors;

/**
 * HanLPTool 封装了 HanLP 的各项中文自然语言处理功能，
 * 包括分词、命名实体识别、关键词提取、自动摘要和短语提取等。
 *
 * <p>示例用法：
 * <pre>
 *     List&lt;Term&gt; termList = HanLPTool.standardSegment("商品和服务");
 *     System.out.println(termList);
 *
 *     List&lt;Term&gt; nameList = HanLPTool.chineseNameRecognize("签约仪式前，秦光荣、李纪恒、仇和等会见了企业家。");
 *     System.out.println(nameList);
 *
 *     List&lt;String&gt; keywords = HanLPTool.extractKeyword("程序员是从事程序开发与维护的专业人员", 5);
 *     System.out.println(keywords);
 *
 *     List&lt;String&gt; summary = HanLPTool.extractSummary("算法可大致分为基本算法、数据结构的算法、数论算法……", 3);
 *     System.out.println(summary);
 *
 *     List&lt;String&gt; phraseList = HanLPTool.extractPhrase("算法工程师是利用算法处理事物的人", 10);
 *     System.out.println(phraseList);
 * </pre>
 * <p>
 * 更多详情请参见 HanLP Wiki 和相关算法详解文档。
 */
public class HanLPKeyWord {

    // -------------------- 分词 --------------------

    /**
     * 标准分词
     *
     * @param text 待分词文本
     * @return 分词结果列表（包含词及词性）
     */
    public static List<Term> standardSegment(String text) {
        return StandardTokenizer.segment(text);
    }

    /**
     * NLP 分词（包含词性标注和命名实体识别）
     *
     * @param text 待分词文本
     * @return 分词结果列表
     */
    public static List<Term> nlpSegment(String text) {
        return NLPTokenizer.segment(text);
    }

    /**
     * 索引分词（适用于搜索引擎，可获得词在文本中的偏移量）
     *
     * @param text 待分词文本
     * @return 分词结果列表
     */
    public static List<Term> indexSegment(String text) {
        return IndexTokenizer.segment(text);
    }

    // -------------------- N-最短路径分词 --------------------

    /**
     * N-最短路径分词（效果略好，但速度较慢）
     *
     * @param text 待分词文本
     * @return 分词结果列表
     */
    public static List<Term> nShortSegment(String text) {
        return new NShortSegment().enableCustomDictionary(true).seg(text);
    }

    /**
     * 最短路分词（速度较快，精度一般）
     *
     * @param text 待分词文本
     * @return 分词结果列表
     */
    public static List<Term> shortestSegment(String text) {
        return new DijkstraSegment().enableCustomDictionary(true).seg(text);
    }

    // -------------------- CRF 与 极速分词 --------------------

    /**
     * CRF 分词，适用于新词识别和较高精度需求，但开销较大
     *
     * @param text 待分词文本
     * @return 分析结果字符串
     */
    @SneakyThrows
    public static String crfSegment(String text) {
        CRFLexicalAnalyzer analyzer = new CRFLexicalAnalyzer();
        return analyzer.analyze(text).toString();
    }

    /**
     * 极速词典分词，基于 AhoCorasickDoubleArrayTrie 实现，适用于高吞吐量场合
     *
     * @param text 待分词文本
     * @return 分词结果列表
     */
    public static List<Term> speedSegment(String text) {
        return SpeedTokenizer.segment(text);
    }

    /**
     * NLP 分析：利用 NLPTokenizer 对文本进行详细分析，并翻译词性标签
     *
     * @param text 待分析文本
     * @return 分析结果字符串
     */
    public static String nlpAnalyze(String text) {
        return NLPTokenizer.analyze(text).translateLabels().text();
    }

    // -------------------- 命名实体识别 --------------------

    /**
     * 中国人名识别，默认开启人名识别功能
     *
     * @param text 待分词文本
     * @return 分词结果列表
     */
    public static List<Term> chineseNameRecognize(String text) {
        return HanLP.newSegment().enableNameRecognize(true).seg(text);
    }

    /**
     * 音译人名识别，默认开启音译人名识别功能
     *
     * @param text 待分词文本
     * @return 分词结果列表
     */
    public static List<Term> translatedNameRecognize(String text) {
        return HanLP.newSegment().enableTranslatedNameRecognize(true).seg(text);
    }

    /**
     * 日本人名识别，默认开启日本人名识别功能
     *
     * @param text 待分词文本
     * @return 分词结果列表
     */
    public static List<Term> japaneseNameRecognize(String text) {
        return HanLP.newSegment().enableJapaneseNameRecognize(true).seg(text);
    }

    /**
     * 地名识别，默认开启地名识别功能
     *
     * @param text 待分词文本
     * @return 分词结果列表
     */
    public static List<Term> placeRecognize(String text) {
        return HanLP.newSegment().enablePlaceRecognize(true).seg(text);
    }

    /**
     * 机构名识别，默认开启机构名识别功能
     *
     * @param text 待分词文本
     * @return 分词结果列表
     */
    public static List<Term> organizationRecognize(String text) {
        return HanLP.newSegment().enableOrganizationRecognize(true).seg(text);
    }

    // -------------------- 关键词、摘要与短语提取 --------------------

    /**
     * 关键词提取（基于 TextRankKeyword 算法）
     *
     * @param content 待提取关键词的文本内容
     * @param size    提取关键词的数量
     * @return 关键词列表
     */
    public static List<String> extractKeyword(String content, int size) {
        return HanLP.extractKeyword(content, size);
    }

    /**
     * 自动摘要（基于 TextRankSentence 算法）
     *
     * @param document 待摘要的文档
     * @param size     摘要句子的数量
     * @return 摘要句子列表
     */
    public static List<String> extractSummary(String document, int size) {
        return HanLP.extractSummary(document, size);
    }

    /**
     * 短语提取（基于互信息和信息熵算法）
     *
     * @param text 待提取短语的文本
     * @param size 提取短语的数量
     * @return 短语列表
     */
    public static List<String> extractPhrase(String text, int size) {
        return HanLP.extractPhrase(text, size);
    }


    public static void main(String[] args) {
        List<Term> termList = standardSegment("你今天好美");
        List<String> wordList = termList.stream()
                .map(term -> term.word)
                .collect(Collectors.toList());
        System.out.println(wordList);
    }

}
