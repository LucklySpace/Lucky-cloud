package com.xy.lucky.leaf.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用策略上下文工具类（支持泛型、链式注册、线程安全）
 *
 * @param <T> 策略接口类型
 */
@Slf4j
public class StrategyContext<T> {

    // 策略注册容器
    private final Map<String, T> strategyMap = new ConcurrentHashMap<>();

    /**
     * 注册一个策略实现
     *
     * @param type     策略标识
     * @param strategy 策略实例
     * @return 当前上下文对象（支持链式调用）
     * @throws IllegalArgumentException 当策略类型或实现为空时抛出
     */
    public StrategyContext<T> register(String type, T strategy) {
        if (type == null || strategy == null) {
            throw new IllegalArgumentException("策略类型和实现不能为空");
        }
        strategyMap.put(type, strategy);
        if (log.isInfoEnabled()) {
            log.info("策略注册成功: [{}] -> {}", type, strategy.getClass().getSimpleName());
        }
        return this;
    }

    /**
     * 获取策略对象（类型安全）
     *
     * @param type 策略标识
     * @return 策略实例（若无则抛出异常）
     * @throws IllegalArgumentException 当未找到指定策略类型时抛出
     */
    public T getStrategy(String type) {
        T strategy = strategyMap.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("未找到策略类型: " + type);
        }
        return strategy;
    }

    /**
     * 判断策略是否存在
     *
     * @param type 策略标识
     * @return true 存在；false 不存在
     */
    public boolean contains(String type) {
        return strategyMap.containsKey(type);
    }

    /**
     * 获取已注册策略数量
     *
     * @return 已注册策略数量
     */
    public int size() {
        return strategyMap.size();
    }

    /**
     * 获取所有已注册策略类型
     *
     * @return 所有已注册策略的映射
     */
    public Map<String, T> getAllStrategies() {
        return strategyMap;
    }
}