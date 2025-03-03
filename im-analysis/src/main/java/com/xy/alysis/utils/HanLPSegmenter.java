package com.xy.alysis.utils;

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

/**
 * HanLPSegmenter 是一个封装 HanLP 分词功能的工具类，提供以下分词方法：
 *
 * <ul>
 *   <li>{@link #standardSegment(String)} - 标准分词（HanLP.segment 的包装）</li>
 *   <li>{@link #nlpSegment(String)} - NLP 分词，同时标注词性和命名实体</li>
 *   <li>{@link #indexSegment(String)} - 索引分词，可获取词语在原文中的偏移量</li>
 *   <li>{@link #nShortSegment(String)} - N-最短路径分词（效果稍好，但速度稍慢）</li>
 *   <li>{@link #shortestSegment(String)} - 最短路分词（速度快，精度一般）</li>
 *   <li>{@link #crfSegment(String)} - CRF 分词，对新词识别较好，但开销较大</li>
 *   <li>{@link #speedSegment(String)} - 极速词典分词，适用于高吞吐量场合</li>
 *   <li>{@link #nlpAnalyze(String)} - 利用 NLPTokenizer 对文本进行详细分析，并翻译词性标签</li>
 * </ul>
 * <p>
 * 使用示例：
 *
 * <pre>
 * List&lt;Term&gt; termList = HanLPSegmenter.standardSegment("商品和服务");
 * System.out.println(termList);
 * System.out.println(HanLPSegmenter.nlpAnalyze("我的希望是希望张晚霞的背影被晚霞映红"));
 * </pre>
 * <p>
 * 更多分词算法详见 HanLP Wiki 文档。
 *
 * @author
 */
public class HanLPSegmenter {

    /**
     * 标准分词，包装了 HanLP.segment 方法。
     *
     * @param text 待分词文本
     * @return 分词结果列表，每个 Term 包含词和词性
     */
    public static List<Term> standardSegment(String text) {
        return StandardTokenizer.segment(text);
    }

    /**
     * NLP 分词，执行词性标注和命名实体识别。
     *
     * @param text 待分词文本
     * @return 分词结果列表
     */
    public static List<Term> nlpSegment(String text) {
        return NLPTokenizer.segment(text);
    }

    /**
     * 索引分词，适用于搜索引擎，可获得词在文本中的偏移量。
     *
     * @param text 待分词文本
     * @return 分词结果列表
     */
    public static List<Term> indexSegment(String text) {
        return IndexTokenizer.segment(text);
    }

    /**
     * N-最短路径分词，效果较好但速度较慢。
     *
     * @param text 待分词文本
     * @return 分词结果列表
     */
    public static List<Term> nShortSegment(String text) {
        NShortSegment segment = new NShortSegment();
        // 根据需求，可以禁用自定义词典或者启用专有领域词典
        segment.enableCustomDictionary(true);
        return segment.seg(text);
    }

    /**
     * 最短路分词，速度快，但在部分场景下效果略差。
     *
     * @param text 待分词文本
     * @return 分词结果列表
     */
    public static List<Term> shortestSegment(String text) {
        DijkstraSegment segment = new DijkstraSegment();
        segment.enableCustomDictionary(true);
        return segment.seg(text);
    }

    /**
     * CRF 分词，适用于新词识别，但开销较大。
     *
     * @param text 待分词文本
     * @return 分析结果字符串，包含词性信息
     */
    @SneakyThrows
    public static String crfSegment(String text) {
        CRFLexicalAnalyzer analyzer = new CRFLexicalAnalyzer();
        return analyzer.analyze(text).toString();
    }

    /**
     * 极速词典分词，基于 AhoCorasickDoubleArrayTrie 实现，适用于高吞吐量场合。
     *
     * @param text 待分词文本
     * @return 分词结果列表
     */
    public static List<Term> speedSegment(String text) {
        return SpeedTokenizer.segment(text);
    }

    /**
     * 对文本进行详细的 NLP 分析，并翻译词性标签。
     *
     * @param text 待分析文本
     * @return 翻译后的分析结果字符串
     */
    public static String nlpAnalyze(String text) {
        return NLPTokenizer.analyze(text).text();
    }

    // 示例 main 方法，用于简单测试各方法效果
    public static void main(String[] args) {
//        String text1 = "商品和服务";
//        System.out.println("标准分词：" + standardSegment(text1));
//        System.out.println("NLP分词：" + nlpSegment("我新造一个词叫幻想乡你能识别并标注正确词性吗？"));
//
//        System.out.println("索引分词：");
//        List<Term> indexTerms = indexSegment("主副食品");
//        for (Term term : indexTerms) {
//            System.out.println(term + " [" + term.offset + ":" + (term.offset + term.word.length()) + "]");
//        }
//
//        String text2 = "今天，刘志军案的关键人物,山西女商人丁书苗在市二中院出庭受审。";
//        System.out.println("N-最短分词：" + nShortSegment(text2));
//        System.out.println("最短路分词：" + shortestSegment(text2));
//
//        System.out.println("CRF 分词：" + crfSegment("微软公司於1975年由比爾·蓋茲和保羅·艾倫創立，18年啟動以智慧雲端、前端為導向的大改組。"));
//
//        String text3 = "江西鄱阳湖干枯，中国最大淡水湖变成大草原";
//        System.out.println("极速分词：" + speedSegment(text3));

        //System.out.println("NLP 分析：" + nlpAnalyze("我的希望是希望张晚霞的背影被晚霞映红"));
    }
}
