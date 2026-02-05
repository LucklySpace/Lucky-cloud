package com.xy.lucky.leaf.service;

import com.xy.lucky.api.ImIdDubboService;
import com.xy.lucky.core.model.IMetaId;
import com.xy.lucky.leaf.core.IDGen;
import com.xy.lucky.leaf.model.IdMetaInfo;
import com.xy.lucky.leaf.repository.IdMetaInfoRepository;
import com.xy.lucky.leaf.utils.StrategyContext;
import org.apache.dubbo.config.annotation.DubboService;
import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * ID服务类
 * 提供多种ID生成策略的统一入口
 */
@DubboService
public class IdService implements ImIdDubboService {

    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;

    private final IdMetaInfoRepository idMetaInfoRepo;

    /**
     * 策略上下文
     */
    private final StrategyContext<IDGen> strategyContext;

    /**
     * 构造函数，注入各种ID生成策略实现
     *
     * @param snowflakeGen Snowflake算法实现
     * @param redisGen     Redis段模式实现
     * @param uidGen       UID实现
     * @param uuidGen      UUID实现
     */

    public IdService(
            @Autowired @Qualifier("snowflakeIDGen") IDGen snowflakeGen,
            @Autowired @Qualifier("redisSegmentIDGen") IDGen redisGen,
            @Autowired @Qualifier("uidIDGen") IDGen uidGen,
            @Autowired @Qualifier("uuidIDGen") IDGen uuidGen, ReactiveRedisTemplate<String, Object> reactiveRedisTemplate, IdMetaInfoRepository idMetaInfoRepo) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.idMetaInfoRepo = idMetaInfoRepo;

        this.strategyContext = new StrategyContext<>();
        // 在构造时统一注册
        this.strategyContext
                .register("snowflake", snowflakeGen)
                .register("redis", redisGen)
                .register("uid", uidGen)
                .register("uuid", uuidGen);

        // 一次性调用 init，确保各实现完成内部准备
        this.strategyContext.getAllStrategies().values().forEach(IDGen::init);

    }

    /**
     * 根据类型和业务标识生成单个ID
     *
     * @param type 策略类型
     * @param key  业务标识
     * @return 生成的ID对象
     */
    public IMetaId generateId(String type, String key) {
        return strategyContext.getStrategy(type).getId(key);
    }

    /**
     * 根据类型和业务标识批量生成ID
     *
     * @param type  策略类型
     * @param key   业务标识
     * @param count 生成数量
     * @return 生成的ID列表
     */
    public List<IMetaId> generateIds(String type, String key, Integer count) {
        List<IMetaId> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            IMetaId id = generateId(type, key);
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    /**
     * 获取用户ID（号段模式+Redis原子操作）
     *
     * @return 生成的用户ID
     */
    public String generateUserId() {
        String type = "user";
        String key = "id:segment:" + type;
        Long increment = reactiveRedisTemplate.opsForValue().increment(key, 1).block(Duration.ofSeconds(2));

        if (increment == null) {
            throw new ServiceException("Redis ID生成失败");
        }

        // 号段用尽时从数据库加载新号段
        if (increment % 1000 == 0) {
            loadNewSegment(type);
        }

        return "UID" + System.currentTimeMillis() / 1000 + String.format("%06d", increment % 1000);
    }

    /**
     * 生成消息ID（Redis原子递增）
     *
     * @param machineId 机器ID
     * @return 生成的消息ID
     */
    public String generateMsgId(int machineId) {
        String key = "id:msg:" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long seq = reactiveRedisTemplate.opsForValue().increment(key).block(Duration.ofSeconds(2));
        if (seq == null) {
            seq = 0L;
        }
        return String.format("MSG%02d%tM%S%04d", machineId, System.currentTimeMillis(), seq % 10000);
    }

    /**
     * 加载新号段到Redis
     *
     * @param id ID类型标识
     */
    @Transactional
    public void loadNewSegment(String id) {
        IdMetaInfo meta = idMetaInfoRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("无效ID类型"));

        long newMaxId = meta.getMaxId() + meta.getStep();
        reactiveRedisTemplate.opsForValue().set("id:segment:" + id.toLowerCase(), newMaxId).block(Duration.ofSeconds(2));
        meta.setMaxId(newMaxId);
        idMetaInfoRepo.save(meta);
    }
}
