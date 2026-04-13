package com.xy.lucky.message.message.offline;

import com.xy.lucky.utils.json.JacksonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基于 Redis List 的离线消息实现，按用户维度存储并在补发时弹出。
 */
@Service
@RequiredArgsConstructor
public class RedisOfflineMessageService implements OfflineMessageService {

    private static final String OFFLINE_KEY_PREFIX = "im:offline:message:";
    private static final Duration OFFLINE_TTL = Duration.ofDays(1);
    private static final int MAX_PULL_LIMIT = 500;
    private static final DefaultRedisScript<List> BATCH_POP_SCRIPT = new DefaultRedisScript<>(
            "local key = KEYS[1]\n"
                    + "local cnt = tonumber(ARGV[1])\n"
                    + "if not cnt or cnt <= 0 then\n"
                    + "  return {}\n"
                    + "end\n"
                    + "local size = redis.call('LLEN', key)\n"
                    + "if size <= 0 then\n"
                    + "  return {}\n"
                    + "end\n"
                    + "if cnt > size then\n"
                    + "  cnt = size\n"
                    + "end\n"
                    + "local startIdx = size - cnt\n"
                    + "local values = redis.call('LRANGE', key, startIdx, size - 1)\n"
                    + "redis.call('LTRIM', key, 0, startIdx - 1)\n"
                    + "return values",
            List.class
    );

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 将离线消息写入用户对应的 Redis List，并刷新过期时间。
     *
     * @param userId 用户 ID
     * @param record 离线消息记录
     */
    @Override
    public void store(String userId, OfflineMessageRecord record) {
        if (!StringUtils.hasText(userId) || record == null) {
            return;
        }
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
        if (!StringUtils.hasText(userId) || max <= 0) {
            return records;
        }
        int pullLimit = Math.min(MAX_PULL_LIMIT, max);
        String key = buildKey(userId);
        List<?> values = redisTemplate.execute(BATCH_POP_SCRIPT, Collections.singletonList(key), String.valueOf(pullLimit));
        if (values == null || values.isEmpty()) {
            return records;
        }
        for (Object value : values) {
            addIfValidRecord(records, value);
        }
        return records;
    }

    /**
     * 仅在反序列化成功时写入结果，避免脏数据中断补发链路。
     */
    private void addIfValidRecord(List<OfflineMessageRecord> records, Object value) {
        OfflineMessageRecord record = JacksonUtils.parseObject(value, OfflineMessageRecord.class);
        if (record != null) {
            records.add(record);
        }
    }

    private String buildKey(String userId) {
        return OFFLINE_KEY_PREFIX + userId;
    }
}
