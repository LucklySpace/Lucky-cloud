package com.xy.generator.controller;

import com.xy.generator.core.IDGen;
import com.xy.generator.utils.StrategyContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * id 接口
 */
@RestController
@RequestMapping("/api/{version}/generator")
public class IdController {

    private final StrategyContext<IDGen<?>> strategyContext;

    /**
     * 构造器注入：所有 IDGen 实现通过 Map 注入，key 对应请求参数 type
     */
    public IdController(
            @Qualifier("snowflakeIDGen") IDGen<?> snowflakeGen,
            @Qualifier("redisSegmentIDGen") IDGen<?> redisGen,
            @Qualifier("uidIDGen") IDGen<?> uidGen,
            @Qualifier("uuidIDGen") IDGen<?> uuidGen

    ) {
        this.strategyContext = new StrategyContext<>();
        // 在构造时统一注册并初始化
        this.strategyContext
                .register("snowflake", snowflakeGen)
                .register("redis", redisGen)
                .register("uid", uidGen)
                .register("uuid", uuidGen);

        // 一次性调用 init，确保各实现完成内部准备
        this.strategyContext.getAllStrategies().values().forEach(IDGen::init);
    }

    /**
     * 根据 type 和 key 异步返回 ID
     *
     * @param type 策略类型：snowflake | redis | uid
     * @param key  业务标识
     */
    @GetMapping("/id")
    public Mono<?> getId(@RequestParam("type") String type,
                         @RequestParam("key") String key) {

        IDGen<?> strategy = strategyContext.getStrategy(type);
        if (strategy == null) {
            return Mono.error(new IllegalArgumentException(
                    "Unknown IDGen type: " + type));
        }
        return strategy.get(key);
    }

    /**
     * 批量获取id
     *
     * @param type  策略类型：snowflake | redis | uid | uuid
     * @param key   业务标识
     * @param count 获取数量
     */
    @GetMapping("/ids")
    public List<Object> getBatchIds(@RequestParam("type") String type,
                                    @RequestParam("key") String key, @RequestParam("count") Integer count) {
        return IntStream.range(0, count)
                .mapToObj(i -> getId(type, key))
                .collect(Collectors.toList());
    }

}