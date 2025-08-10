package com.xy.generator.core;

import com.xy.core.model.IMetaId;
import reactor.core.publisher.Mono;

/**
 * id 生成器接口
 *
 * @param
 */
public interface IDGen {
    /**
     * 异步获取一个唯一ID
     *
     * @param key 业务 key，用于不同场景分隔
     * @return Mono 包裹的 ID
     */
    Mono<IMetaId> get(String key);

    /**
     * 初始化生成器
     *
     * @return 是否初始化成功
     */
    boolean init();
}