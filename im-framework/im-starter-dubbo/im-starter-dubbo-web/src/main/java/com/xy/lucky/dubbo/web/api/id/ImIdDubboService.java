package com.xy.lucky.dubbo.web.api.id;

import com.xy.lucky.core.model.IMetaId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * ID生成接口
 */
public interface ImIdDubboService {

    Logger logger = LoggerFactory.getLogger(ImIdDubboService.class);

    /**
     * 根据 type 和 key 异步返回 ID
     *
     * @param type 策略类型：snowflake | redis | uid | uuid
     * @param key  业务标识
     * @return ID对象
     */
    IMetaId generateId(String type, String key);


    /**
     * 批量获取ID
     *
     * @param type  策略类型：snowflake | redis | uid | uuid
     * @param key   业务标识
     * @param count 获取数量
     * @return ID列表
     */
    List<IMetaId> generateIds(String type, String key, Integer count);


    /**
     * 通用类型安全的获取 ID 方法，带缓存机制
     *
     * @param type       策略类型
     * @param key        业务 key
     * @param targetType 目标类型
     * @param <T>        泛型类型
     * @return 返回泛型类型的 ID
     */
    default <T> T getId(String type, String key, Class<T> targetType) {

        IMetaId iMetaId = generateId(type, key);

        Object metaId = iMetaId.getMetaId();

        logger.debug("id类型：{}  id键：{}  请求获取的id：{}", type, key, metaId);

        return targetType.cast(metaId);
    }

}
