package com.xy.lucky.message.message.offline;

import com.xy.lucky.domain.po.IMOfflineMessagePo;
import com.xy.lucky.rpc.api.database.offline.IMOfflineMessageDubboService;
import com.xy.lucky.utils.id.IdUtils;
import com.xy.lucky.utils.time.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class DatabaseOfflineMessageService implements OfflineMessageService {

    private static final int MAX_PULL_LIMIT = 500;
    private static final Duration OFFLINE_TTL = Duration.ofDays(7);

    @DubboReference
    private IMOfflineMessageDubboService offlineMessageDubboService;
    private final RedisOfflineMessageService redisOfflineMessageService;

    @Override
    public void store(String userId, OfflineMessageRecord record) {
        if (!StringUtils.hasText(userId) || record == null || !StringUtils.hasText(record.messageId()) || !StringUtils.hasText(record.payload())) {
            return;
        }
        long now = DateTimeUtils.getCurrentUTCTimestamp();
        IMOfflineMessagePo po = new IMOfflineMessagePo()
                .setId(IdUtils.snowflakeId())
                .setUserId(userId)
                .setMessageId(record.messageId())
                .setMessageType(record.messageType())
                .setPayload(record.payload())
                .setCreatedAt(now)
                .setExpireAt(now + OFFLINE_TTL.toMillis());
        try {
            Boolean created = offlineMessageDubboService.create(po);
            if (!Boolean.TRUE.equals(created)) {
                log.warn("离线消息入库返回失败: userId={}, messageId={}", userId, record.messageId());
                redisOfflineMessageService.store(userId, record);
            }
        } catch (Exception e) {
            log.error("离线消息入库失败: userId={}, messageId={}", userId, record.messageId(), e);
            redisOfflineMessageService.store(userId, record);
        }
    }

    @Override
    public List<OfflineMessageRecord> pull(String userId, int max) {
        if (!StringUtils.hasText(userId) || max <= 0) {
            return Collections.emptyList();
        }
        int pullLimit = Math.min(MAX_PULL_LIMIT, max);
        List<OfflineMessageRecord> result = new ArrayList<>(pullLimit);
        List<IMOfflineMessagePo> records;
        try {
            records = offlineMessageDubboService.pullAndRemoveByUserId(
                    userId,
                    pullLimit,
                    DateTimeUtils.getCurrentUTCTimestamp()
            );
        } catch (Exception e) {
            log.error("离线消息拉取失败: userId={}", userId, e);
            return redisOfflineMessageService.pull(userId, pullLimit);
        }
        if (!CollectionUtils.isEmpty(records)) {
            result.addAll(records.stream()
                    .filter(record -> StringUtils.hasText(record.getMessageId()) && StringUtils.hasText(record.getPayload()))
                    .map(record -> OfflineMessageRecord.builder()
                            .messageId(record.getMessageId())
                            .messageType(record.getMessageType())
                            .payload(record.getPayload())
                            .build())
                    .toList());
        }
        int remain = pullLimit - result.size();
        if (remain > 0) {
            result.addAll(redisOfflineMessageService.pull(userId, remain));
        }
        if (result.isEmpty()) {
            return Collections.emptyList();
        }
        return result;
    }
}
