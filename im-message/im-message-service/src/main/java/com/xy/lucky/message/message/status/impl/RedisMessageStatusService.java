package com.xy.lucky.message.message.status.impl;

import com.xy.lucky.message.message.status.MessageStatus;
import com.xy.lucky.message.message.status.MessageStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于 Redis Hash 的消息状态存储实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisMessageStatusService implements MessageStatusService {

    private static final String STATUS_KEY_PREFIX = "im-msg-status:";
    private static final Duration STATUS_TTL = Duration.ofDays(7);

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 批量初始化消息状态为待投递。
     *
     * @param messageId 消息 ID
     * @param userIds   目标用户列表
     */
    @Override
    public void markPending(String messageId, Collection<String> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }
        String key = buildKey(messageId);
        Map<String, Integer> statusMap = new HashMap<>();
        for (String userId : userIds) {
            statusMap.put(userId, MessageStatus.PENDING.code);
        }
        redisTemplate.opsForHash().putAll(key, statusMap);
        redisTemplate.expire(key, STATUS_TTL);
    }

    /**
     * 批量标记消息投递成功。
     *
     * @param messageId 消息 ID
     * @param userIds   成功用户列表
     */
    @Override
    public void markDelivered(String messageId, Collection<String> userIds) {
        updateStatus(messageId, userIds, MessageStatus.DELIVERED.code);
    }

    /**
     * 批量标记消息投递失败。
     *
     * @param messageId 消息 ID
     * @param userIds   失败用户列表
     * @param reason    失败原因
     */
    @Override
    public void markFailed(String messageId, Collection<String> userIds, String reason) {
        updateStatus(messageId, userIds, MessageStatus.PENDING.code);
        log.warn("消息投递失败: messageId={}, userCount={}, reason={}",
                messageId, userIds == null ? 0 : userIds.size(), reason);
    }

    /**
     * 标记单个用户已确认消息。
     *
     * @param messageId 消息 ID
     * @param userId    用户 ID
     */
    @Override
    public void acknowledge(String messageId, String userId) {
        String key = buildKey(messageId);
        redisTemplate.opsForHash().put(key, userId, "ACKED");
        redisTemplate.expire(key, STATUS_TTL);
    }

    /**
     * 批量更新指定用户的消息状态。
     *
     * @param messageId 消息 ID
     * @param userIds   用户列表
     * @param status    状态值
     */
    private void updateStatus(String messageId, Collection<String> userIds, Integer status) {
        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }
        String key = buildKey(messageId);
        for (String userId : userIds) {
            redisTemplate.opsForHash().put(key, userId, status);
        }
        redisTemplate.expire(key, STATUS_TTL);
    }

    private String buildKey(String messageId) {
        return STATUS_KEY_PREFIX + messageId;
    }
}
