package com.xy.lucky.message.message.offline;

import com.xy.lucky.utils.json.JacksonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于 Redis List 的离线消息实现，按用户维度存储并在补发时弹出。
 */
@Service
@RequiredArgsConstructor
public class RedisOfflineMessageService implements OfflineMessageService {

    private static final String OFFLINE_KEY_PREFIX = "IM-OFFLINE-MSG:";
    private static final Duration OFFLINE_TTL = Duration.ofDays(7);

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 将离线消息写入用户对应的 Redis List，并刷新过期时间。
     *
     * @param userId 用户 ID
     * @param record 离线消息记录
     */
    @Override
    public void store(String userId, OfflineMessageRecord record) {
        String key = buildKey(userId);
        redisTemplate.opsForList().leftPush(key, JacksonUtils.toJSONString(record));
        redisTemplate.expire(key, OFFLINE_TTL);
    }

    /**
     * 按先进先出的顺序拉取离线消息，拉取后即从缓存中移除。
     *
     * @param userId 用户 ID
     * @param max    最大拉取数量
     * @return 拉取到的离线消息列表
     */
    @Override
    public List<OfflineMessageRecord> pull(String userId, int max) {
        List<OfflineMessageRecord> records = new ArrayList<>();
        if (max <= 0) {
            return records;
        }
        String key = buildKey(userId);
        for (int i = 0; i < max; i++) {
            Object value = redisTemplate.opsForList().rightPop(key);
            if (value == null) {
                break;
            }
            OfflineMessageRecord record = JacksonUtils.parseObject(value, OfflineMessageRecord.class);
            if (record != null) {
                records.add(record);
            }
        }
        return records;
    }

    private String buildKey(String userId) {
        return OFFLINE_KEY_PREFIX + userId;
    }
}
