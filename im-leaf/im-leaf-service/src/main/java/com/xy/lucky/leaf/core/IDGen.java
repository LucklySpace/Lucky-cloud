package com.xy.lucky.leaf.core;

import com.xy.lucky.core.model.IMetaId;
import reactor.core.publisher.Mono;

/**
 * ID生成器接口
 * 定义ID生成器的标准接口，支持同步和异步两种方式
 */
public interface IDGen {
    /**
     * 异步获取一个唯一ID
     *
     * @param key 业务key，用于不同场景分隔
     * @return Mono包装的ID对象
     */
    Mono<IMetaId> get(String key);

    /**
     * 同步获取一个唯一ID
     *
     * @param key 业务key，用于不同场景分隔
     * @return ID对象
     */
    IMetaId getId(String key);

    /**
     * 初始化生成器
     *
     * @return 是否初始化成功
     */
    boolean init();
}