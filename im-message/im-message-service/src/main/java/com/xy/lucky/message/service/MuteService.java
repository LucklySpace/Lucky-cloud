package com.xy.lucky.message.service;

import com.xy.lucky.utils.time.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 禁言服务
 */
@Service
@RequiredArgsConstructor
public class MuteService {
    private static final String PREFIX = "im:mute:";
    private static final long PERMANENT_SECONDS = 315360000L;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 判断某个用户是否被禁言
     *
     * @param fromId 发送消息的用户id
     * @param toId   接收消息的用户id
     * @return true 存在 false不存在
     */
    public boolean isMutedInPrivate(String fromId, String toId) {
        return anyExists(List.of(
                userGlobalKey(fromId),
                privateKey(fromId, toId)
        ));
    }

    /**
     * 判断某个用户是否被某个群组禁言
     *
     * @param groupId 群组id
     * @param userId  被禁言的用户id
     * @return true 表示被禁言，false 表示未被禁言
     */
    public boolean isMutedInGroup(String groupId, String userId) {
        return anyExists(List.of(
                userGlobalKey(userId),
                groupUserKey(groupId, userId),
                groupAllKey(groupId)
        ));
    }

    /**
     * 禁言某个用户
     *
     * @param userId       被禁言的用户id
     * @param endTimestamp 禁言到期时间戳
     */
    public void muteUserGlobal(String userId, Long endTimestamp) {
        setWithTtl(userGlobalKey(userId), endTimestamp);
    }

    /**
     * 解除某个用户的禁言
     *
     * @param userId 被解除禁言的用户id
     */
    public void unmuteUserGlobal(String userId) {
        redisTemplate.delete(userGlobalKey(userId));
    }

    /**
     * 在私聊场景下禁言目标用户
     *
     * @param fromId       发起禁言的用户id
     * @param toId         被禁言的目标用户id
     * @param endTimestamp 禁言到期时间戳
     */
    public void mutePrivate(String fromId, String toId, Long endTimestamp) {
        setWithTtl(privateKey(fromId, toId), endTimestamp);
    }

    /**
     * 在私聊场景下解除目标用户禁言
     *
     * @param fromId 发起解除禁言的用户id
     * @param toId   被解除禁言的目标用户id
     */
    public void unmutePrivate(String fromId, String toId) {
        redisTemplate.delete(privateKey(fromId, toId));
    }

    /**
     * 在指定群组内禁言某个用户
     *
     * @param groupId      禁言群组id
     * @param userId       被禁言的用户id
     * @param endTimestamp 禁言到期时间戳
     */
    public void muteUserInGroup(String groupId, String userId, Long endTimestamp) {
        setWithTtl(groupUserKey(groupId, userId), endTimestamp);
    }

    /**
     * 在指定群组内解除某个用户禁言
     *
     * @param groupId 解除禁言群组id
     * @param userId  被解除禁言的用户id
     */
    public void unmuteUserInGroup(String groupId, String userId) {
        redisTemplate.delete(groupUserKey(groupId, userId));
    }

    /**
     * 禁言某个群组的所有用户
     *
     * @param groupId      禁言群组id
     * @param endTimestamp 禁言到期时间戳
     */
    public void muteAllInGroup(String groupId, Long endTimestamp) {
        setWithTtl(groupAllKey(groupId), endTimestamp);
    }

    /**
     * 解除某个群组的所有用户的禁言
     *
     * @param groupId 解除禁言群组id
     */
    public void unmuteAllInGroup(String groupId) {
        redisTemplate.delete(groupAllKey(groupId));
    }

    /**
     * 设置某个键的过期时间
     *
     * @param key          键
     * @param endTimestamp 键的过期时间戳
     */
    private void setWithTtl(String key, Long endTimestamp) {
        redisTemplate.delete(key);
        long now = DateTimeUtils.getCurrentUTCTimestamp();
        long ttlSeconds = endTimestamp != null ? Math.max(0, (endTimestamp - now) / 1000) : PERMANENT_SECONDS;
        if (ttlSeconds <= 0) {
            return;
        }
        redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(ttlSeconds));
    }

    private String userGlobalKey(String userId) {
        return PREFIX + "u:{" + userId + "}";
    }


    private String privateKey(String fromId, String toId) {
        return PREFIX + "p:{" + fromId + "}:" + toId;
    }

    private String groupUserKey(String groupId, String userId) {
        return PREFIX + "g:{" + groupId + "}:" + userId;
    }

    private String groupAllKey(String groupId) {
        return PREFIX + "ga:{" + groupId + "}";
    }

    /**
     * 判断多个键是否存在
     *
     * @param keys 键列表
     * @return true 存在 false不存在
     */
    private boolean anyExists(List<String> keys) {
        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String key : keys) {
                connection.get(key.getBytes());
            }
            return null;
        });
        for (Object r : results) {
            if (r != null) return true;
        }
        return false;
    }
}
