//package com.xy.generator.core.impl;
//
//import com.xy.generator.core.IDGen;
//import lombok.extern.slf4j.Slf4j;
//
//import java.util.*;
//import java.util.concurrent.locks.ReentrantLock;
//
/// **
// * RuleBasedIDGenerator
// * <p>
// * 依据指定格式生成顺序递增的 ID，并使用号码池缓存生成结果。
// * 支持格式占位符：
// * X - 大写字母 A-Z 或数字 0-9（共 36 种）
// * L - 大写字母 A-Z（共 26 种）
// * D - 数字 0-9（共 10 种）
// * 其他字符原样输出（如 '-', '_' 等）。
// * <p>
// * 主要功能：
// * 1. 依次遍历全组合空间，生成唯一 ID。
// * 2. 号码池缓存：预生成一批 ID，取用时从池头弹出。
// * 3. 当池内剩余量低于阈值（poolSize * refillThreshold）时，自动补充至池容量。
// * 4. 线程安全：使用 ReentrantLock 保证并发环境下安全获取。
// * 5. 日志记录：使用 SLF4J 记录关键操作与异常。
// * <p>
// * 用法示例：
// * RuleBasedIDGenerator gen = new RuleBasedIDGenerator("LL-DDD-XXX", 500, 0.2);
// * String id = (String) gen.get("anyKey");
// */
//@Slf4j
/// /@Component("ruleBasedIDGen")
//public class RuleBasedIDGenerator implements IDGen {
//
//    // 全局字符集映射：X -> A-Z0-9, L -> A-Z, D -> 0-9
//    private static final Map<Character, char[]> CHAR_SETS = new HashMap<>() {{
//        put('X', "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray());
//        put('L', "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray());
//        put('D', "0123456789".toCharArray());
//    }};
//    // 格式模板，如 "LL-DDD-XXX"
//    private final String format;
//    // 号码池最大容量
//    private final int poolSize;
//    // 自动重填阈值比例（如 0.2 表示剩余低于 20% 时触发重填）
//    private final double refillThreshold;
//    // 号码池队列
//    private final List<String> pool = new LinkedList<>();
//    // 并发锁
//    private final ReentrantLock lock = new ReentrantLock();
//    // 存储格式中占位符的位置与对应字符集
//    private final List<CharacterSet> positionSets = new ArrayList<>();
//    // 当前计数器，用于依次生成序号
//    private long counter = 0;
//    // 格式的全组合总数
//    private long totalCombinations = 1;
//
//    /**
//     * 构造函数
//     *
//     * @param format          格式字符串，占位符见类注释
//     * @param poolSize        池容量
//     * @param refillThreshold 重填阈值比例
//     */
//    public RuleBasedIDGenerator(String format, int poolSize, double refillThreshold) {
//        this.format = format;
//        this.poolSize = poolSize;
//        this.refillThreshold = refillThreshold;
//        log.info("初始化 RuleBasedIDGenerator: format={}, poolSize={}, refillThreshold={}", format, poolSize, refillThreshold);
//        this.init();
//    }
//
//    /**
//     * 示例主方法，用于测试生成器功能
//     */
//    public static void main(String[] args) {
//        RuleBasedIDGenerator generator = new RuleBasedIDGenerator("LLL-DDDD-XXXX", 500, 0.2);
//        for (int i = 0; i < 100; i++) {
//            System.out.println(generator.get("testKey"));
//        }
//    }
//
//    /**
//     * 初始化方法，实现 IDGen 接口
//     *
//     * @return 初始化是否成功
//     */
//    @Override
//    public boolean init() {
//        lock.lock();
//        try {
//            log.debug("开始初始化位置映射和组合总数");
//            positionSets.clear();
//            totalCombinations = 1;
//
//            // 遍历格式，记录占位符位置并计算总组合数
//            for (int i = 0; i < format.length(); i++) {
//                char ch = format.charAt(i);
//                if (CHAR_SETS.containsKey(ch)) {
//                    char[] set = CHAR_SETS.get(ch);
//                    positionSets.add(new CharacterSet(i, set));
//                    totalCombinations *= set.length;
//                }
//            }
//            log.info("格式解析完毕，总组合数 = {}", totalCombinations);
//
//            // 首次填充号码池
//            refillPool();
//            return true;
//        } catch (Exception e) {
//            log.error("初始化失败", e);
//            return false;
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    /**
//     * 获取下一个 ID
//     *
//     * @param key 可选参数，方法签名需要，但当前实现未使用
//     * @return 生成的唯一 ID（String 类型）
//     */
//    @Override
//    public Object get(String key) {
//        lock.lock();
//        try {
//            if (pool.isEmpty()) {
//                log.warn("号码池已空，尝试重填");
//                refillPool();
//                if (pool.isEmpty()) {
//                    log.error("重填失败，暂无可用 ID");
//                    throw new RuntimeException("No more UUIDs available.");
//                }
//            }
//
//            String id = pool.remove(0);
//            log.debug("分配 ID: {}，剩余池大小: {}", id, pool.size());
//
//            // 判断是否需要重填
//            if (pool.size() < poolSize * refillThreshold) {
//                log.info("池剩余 {} 小于阈值 {}, 开始重填", pool.size(), (int) (poolSize * refillThreshold));
//                refillPool();
//            }
//
//            return id;
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    /**
//     * 重填号码池，直至达到 poolSize 或者耗尽总组合
//     */
//    private void refillPool() {
//        log.debug("开始重填号码池，当前池大小: {}", pool.size());
//        while (pool.size() < poolSize && counter < totalCombinations) {
//            String id = numToStr(counter);
//            pool.add(id);
//            counter++;
//        }
//        log.info("重填完成，当前池大小: {}, 已生成总数: {}", pool.size(), counter);
//    }
//
//    /**
//     * 将计数器数值转换为格式化字符串
//     *
//     * @param num 当前序号
//     * @return 格式化后的 ID
//     */
//    private String numToStr(long num) {
//        if (num >= totalCombinations) {
//            log.error("序号 {} 超出最大组合数 {}", num, totalCombinations);
//            throw new IllegalArgumentException("Sequence exhausted.");
//        }
//
//        char[] result = format.toCharArray();
//        ListIterator<CharacterSet> iter = positionSets.listIterator(positionSets.size());
//
//        while (iter.hasPrevious()) {
//            CharacterSet cs = iter.previous();
//            int idx = (int) (num % cs.set.length);
//            result[cs.pos] = cs.set[idx];
//            num /= cs.set.length;
//        }
//
//        return new String(result);
//    }
//
//    /**
//     * 内部类，表示格式占位符的位置和对应字符集
//     */
//    private static class CharacterSet {
//        int pos;     // 占位符在格式字符串中的索引位置
//        char[] set;  // 对应的字符集
//
//        CharacterSet(int pos, char[] set) {
//            this.pos = pos;
//            this.set = set;
//        }
//    }
//}