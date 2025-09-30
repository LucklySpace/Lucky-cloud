package com.xy.generator.controller.grpc;

import com.xy.core.model.IMetaId;
import com.xy.generator.core.IDGen;
import com.xy.generator.utils.StrategyContext;
import com.xy.grpc.server.annotation.GrpcRoute;
import com.xy.grpc.server.annotation.GrpcRouteMapping;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * gRPC ID生成控制器
 * 提供基于不同策略的ID生成服务
 */
@GrpcRouteMapping("/generator")
public class IdGrpcController {

    private final StrategyContext<IDGen> strategyContext;

    /**
     * 构造器注入：所有 IDGen 实现通过 Map 注入，key 对应请求参数 type
     */
    public IdGrpcController(
            @Qualifier("snowflakeIDGen") IDGen snowflakeGen,
            @Qualifier("redisSegmentIDGen") IDGen redisGen,
            @Qualifier("uidIDGen") IDGen uidGen,
            @Qualifier("uuidIDGen") IDGen uuidGen
    ) {
        this.strategyContext = new StrategyContext<>();
        // 在构造时统一注册
        this.strategyContext
                .register("snowflake", snowflakeGen)
                .register("redis", redisGen)
                .register("uid", uidGen)
                .register("uuid", uuidGen);
    }

    /**
     * 初始化方法：在Bean创建后初始化所有策略
     */
    @PostConstruct
    public void init() {
        // 一次性调用 init，确保各实现完成内部准备
        this.strategyContext.getAllStrategies().values().forEach(IDGen::init);
    }

    /**
     * 根据 type 和 key 异步返回 ID
     *
     * @return 生成的ID
     */
    @GrpcRoute("/id")
    public IMetaId getId(String type, String key) {
        IDGen strategy = strategyContext.getStrategy(type);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown IDGen type: " + type);
        }
        return strategy.getId(key);
    }

//    /**
//     * 批量获取ID
//     * @return ID列表
//     */
//    @GrpcRoute("/ids")
//    public List<IMetaId> getBatchIds(Object[] params) {
//        String type = (String) params[0];
//        String key = (String) params[1];
//        Integer count = (Integer) params[2];
//
//        return IntStream.range(0, count)
//                .mapToObj(i -> getId(new Object[]{type, key}))
//                .collect(Collectors.toList());
//    }
}