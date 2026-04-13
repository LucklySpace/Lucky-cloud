package com.xy.lucky.database.web.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.database.web.mapper.IMOfflineMessagePoMapper;
import com.xy.lucky.database.web.utils.DateTimeUtils;
import com.xy.lucky.domain.po.IMOfflineMessagePo;
import com.xy.lucky.rpc.api.database.offline.IMOfflineMessageDubboService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@DubboService
public class IMOfflineMessageService extends ServiceImpl<IMOfflineMessagePoMapper, IMOfflineMessagePo> implements IMOfflineMessageDubboService {

    private static final int DEFAULT_PULL_LIMIT = 100;
    private static final int MAX_PULL_LIMIT = 500;

    @Value("${offline.message.cleanup.enabled:true}")
    private boolean cleanupEnabled;
    @Value("${offline.message.cleanup.batch-size:1000}")
    private int cleanupBatchSize;

    @Override
    public Boolean create(IMOfflineMessagePo offlineMessagePo) {
        if (offlineMessagePo == null || !StringUtils.hasText(offlineMessagePo.getUserId()) || !StringUtils.hasText(offlineMessagePo.getMessageId())) {
            return false;
        }
        return super.save(offlineMessagePo);
    }

    @Override
    public Boolean createBatch(List<IMOfflineMessagePo> list) {
        if (CollectionUtils.isEmpty(list)) {
            return false;
        }
        return super.saveBatch(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<IMOfflineMessagePo> pullAndRemoveByUserId(String userId, Integer limit, Long nowTimestamp) {
        if (!StringUtils.hasText(userId)) {
            return Collections.emptyList();
        }
        int queryLimit = normalizeLimit(limit);
        // 兜底当前时间，避免调用方未传 now 时把过期消息误判为有效消息。
        long now = nowTimestamp == null ? DateTimeUtils.getUTCDateTime() : nowTimestamp;
        purgeExpiredByUser(userId, now);
        List<IMOfflineMessagePo> records = super.list(
                Wrappers.<IMOfflineMessagePo>lambdaQuery()
                        .eq(IMOfflineMessagePo::getUserId, userId)
                        .gt(IMOfflineMessagePo::getExpireAt, now)
                        .orderByAsc(IMOfflineMessagePo::getCreatedAt)
                        .last("for update skip locked limit " + queryLimit)
        );
        if (CollectionUtils.isEmpty(records)) {
            return Collections.emptyList();
        }
        List<Long> ids = records.stream()
                .map(IMOfflineMessagePo::getId)
                .filter(id -> id != null && id > 0)
                .toList();
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        super.remove(
                Wrappers.<IMOfflineMessagePo>lambdaQuery()
                        .eq(IMOfflineMessagePo::getUserId, userId)
                        .in(IMOfflineMessagePo::getId, ids)
        );
        return records;
    }

    private int normalizeLimit(Integer limit) {
        int value = limit == null ? DEFAULT_PULL_LIMIT : limit;
        if (value <= 0) {
            return DEFAULT_PULL_LIMIT;
        }
        return Math.min(MAX_PULL_LIMIT, value);
    }

    private void purgeExpiredByUser(String userId, long nowTimestamp) {
        if (!StringUtils.hasText(userId) || nowTimestamp <= 0) {
            return;
        }
        super.remove(
                Wrappers.<IMOfflineMessagePo>lambdaQuery()
                        .eq(IMOfflineMessagePo::getUserId, userId)
                        .le(IMOfflineMessagePo::getExpireAt, nowTimestamp)
        );
    }

    @Scheduled(
            fixedDelayString = "${offline.message.cleanup.fixed-delay-ms:600000}",
            initialDelayString = "${offline.message.cleanup.initial-delay-ms:60000}"
    )
    public void cleanupExpiredMessages() {
        if (!cleanupEnabled) {
            return;
        }
        long now = DateTimeUtils.getUTCDateTime();
        int batchSize = Math.max(100, cleanupBatchSize);
        while (true) {
            List<Long> ids = super.list(
                            Wrappers.<IMOfflineMessagePo>lambdaQuery()
                                    .select(IMOfflineMessagePo::getId)
                                    .le(IMOfflineMessagePo::getExpireAt, now)
                                    .orderByAsc(IMOfflineMessagePo::getId)
                                    .last("limit " + batchSize)
                    ).stream()
                    .map(IMOfflineMessagePo::getId)
                    .filter(id -> id != null && id > 0)
                    .toList();
            if (CollectionUtils.isEmpty(ids)) {
                return;
            }
            super.removeByIds(ids);
            if (ids.size() < batchSize) {
                return;
            }
        }
    }
}
