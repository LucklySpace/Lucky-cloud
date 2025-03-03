package com.xy.alysis.utils;


import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLSentence;
import com.hankcs.hanlp.corpus.dependency.CoNll.CoNLLWord;

/**
 * DependencyParserTool 封装了 HanLP 的依存句法分析功能，
 * 提供方便的方法来解析句子并输出依存句法信息。
 *
 * <p>示例用法：
 * <pre>
 *     String sentence = "徐先生还具体帮助他确定了把画雄鹰、松鼠和麻雀作为主攻目标。";
 *     CoNLLSentence parsed = DependencyParserTool.parse(sentence);
 *     System.out.println(DependencyParserTool.format(parsed));
 *     System.out.println(DependencyParserTool.formatReverse(parsed));
 *     System.out.println(DependencyParserTool.traverseUp(parsed, 5));
 * </pre>
 * <p>
 * 注意：部分依存句法模型（如 NeuralNetworkDependencyParser）可能需要较大的堆内存，启动时请配置 JVM 参数。
 */
public class HanLPDependencyParser {

    /**
     * 对输入的句子进行依存句法分析
     *
     * @param sentence 待分析的句子
     * @return 分析后的 CoNLLSentence 对象
     */
    public static CoNLLSentence parse(String sentence) {
        return HanLP.parseDependency(sentence);
    }

    /**
     * 格式化依存句法分析结果，返回字符串形式的依存关系信息
     *
     * @param sentence 依存句法分析后的 CoNLLSentence 对象
     * @return 格式化后的字符串结果，每行显示词与其依存关系及对应的头节点
     */
    public static String format(CoNLLSentence sentence) {
        StringBuilder sb = new StringBuilder();
        for (CoNLLWord word : sentence) {
            sb.append(String.format("%s --(%s)--> %s\n", word.LEMMA, word.DEPREL, word.HEAD.LEMMA));
        }
        return sb.toString();
    }

    /**
     * 以逆序格式化依存句法分析结果
     *
     * @param sentence 依存句法分析后的 CoNLLSentence 对象
     * @return 逆序格式化后的字符串结果
     */
    public static String formatReverse(CoNLLSentence sentence) {
        StringBuilder sb = new StringBuilder();
        CoNLLWord[] wordArray = sentence.getWordArray();
        for (int i = wordArray.length - 1; i >= 0; i--) {
            CoNLLWord word = wordArray[i];
            sb.append(String.format("%s --(%s)--> %s\n", word.LEMMA, word.DEPREL, word.HEAD.LEMMA));
        }
        return sb.toString();
    }

    /**
     * 遍历从指定索引开始的子树，一路上溯到虚根
     *
     * @param sentence 依存句法分析后的 CoNLLSentence 对象
     * @param index    起始词语在句子中的索引
     * @return 从该词一路上溯到根节点的路径描述
     */
    public static String traverseUp(CoNLLSentence sentence, int index) {
        StringBuilder sb = new StringBuilder();
        CoNLLWord[] wordArray = sentence.getWordArray();
        if (index < 0 || index >= wordArray.length) {
            return "";
        }
        CoNLLWord head = wordArray[index];
        while ((head = head.HEAD) != null) {
            if (head == CoNLLWord.ROOT) {
                sb.append(head.LEMMA);
                break;
            } else {
                sb.append(String.format("%s --(%s)--> ", head.LEMMA, head.DEPREL));
            }
        }
        return sb.toString();
    }

    // 示例 main 方法，演示依存句法分析的各项功能
//    public static void main(String[] args) {
//        String[] testCase = new String[]{
//                "徐先生还具体帮助他确定了把画雄鹰、松鼠和麻雀作为主攻目标。",
//                "刘志军案的关键人物,山西女商人丁书苗在市二中院出庭受审。",
//        };
//        for (String sentence : testCase) {
//            System.out.println("原句：" + sentence);
//            CoNLLSentence parsed = parse(sentence);
//            System.out.println("格式化输出：");
//            System.out.println(format(parsed));
//            System.out.println("逆序格式化输出：");
//            System.out.println(formatReverse(parsed));
//            // 遍历第 5 个词一路上溯到根（若索引合法）
//            System.out.println("从第5个词上溯路径：");
//            System.out.println(traverseUp(parsed, 4));
//            System.out.println("------------------------------");
//        }
//    }
}
