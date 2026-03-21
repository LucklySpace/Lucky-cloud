package com.xy.lucky.message.message.outbox;

import com.xy.lucky.domain.po.IMOutboxPo;
import com.xy.lucky.rpc.api.database.outbox.IMOutboxDubboService;
import com.xy.lucky.utils.id.IdUtils;
import com.xy.lucky.utils.time.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 基于 Dubbo 数据服务的 Outbox 记录实现，负责状态机驱动的状态更新。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DubboOutboxRecordService implements OutboxRecordService {

    @DubboReference
    private IMOutboxDubboService outboxDubboService;

    private final OutboxStateMachine stateMachine;

    /**
     * 创建待发送 Outbox 记录。
     *
     * @param messageId  业务消息 ID
     * @param payload    投递负载
     * @param exchange   交换机
     * @param routingKey 路由键
     * @return Outbox 主键 ID
     */
    @Override
    public Long createPending(String messageId, String payload, String exchange, String routingKey) {
        Long outboxId = IdUtils.snowflakeId();
        long now = DateTimeUtils.getCurrentUTCTimestamp();
        IMOutboxPo po = new IMOutboxPo()
                .setId(outboxId)
                .setMessageId(messageId)
                .setPayload(payload)
                .setExchange(exchange)
                .setRoutingKey(routingKey)
                .setAttempts(0)
                .setStatus(stateMachine.transit(null, OutboxEvent.CREATE).value())
                .setCreatedAt(now)
                .setUpdatedAt(now)
                .setNextTryAt(now);
        outboxDubboService.creat(po);
        return outboxId;
    }

    /**
     * 标记记录已成功发送到 Broker。
     *
     * @param outboxId Outbox 主键 ID
     * @param attempts 当前尝试次数
     */
    @Override
    public void markSent(Long outboxId, int attempts) {
        updateById(outboxId, current -> stateMachine.transit(current, OutboxEvent.BROKER_ACK), attempts, null, null);
    }

    /**
     * 标记记录待重试。
     *
     * @param outboxId  Outbox 主键 ID
     * @param attempts  当前尝试次数
     * @param nextTryAt 下次重试时间戳
     * @param reason    失败原因
     */
    @Override
    public void markPendingForRetry(Long outboxId, int attempts, long nextTryAt, String reason) {
        updateById(outboxId, current -> stateMachine.transit(current, OutboxEvent.BROKER_NACK), attempts, nextTryAt, reason);
    }

    /**
     * 标记记录进入死信状态。
     *
     * @param outboxId Outbox 主键 ID
     * @param attempts 当前尝试次数
     * @param reason   死信原因
     */
    @Override
    public void markDlx(Long outboxId, int attempts, String reason) {
        updateById(outboxId, current -> stateMachine.transit(current, OutboxEvent.RETRY_EXHAUSTED), attempts, null, reason);
    }

    /**
     * 根据业务消息 ID 标记已投递完成。
     *
     * @param messageId 业务消息 ID
     */
    @Override
    public void markDeliveredByMessageId(String messageId) {
        if (!StringUtils.hasText(messageId)) {
            return;
        }
        List<IMOutboxPo> candidates = outboxDubboService.queryByStatus(OutboxStatus.SENT.value(), 1000);
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        long now = DateTimeUtils.getCurrentUTCTimestamp();
        for (IMOutboxPo po : candidates) {
            if (!messageId.equals(po.getMessageId())) {
                continue;
            }
            OutboxStatus current = OutboxStatus.from(po.getStatus());
            OutboxStatus next = stateMachine.transit(current, OutboxEvent.CLIENT_ACK);
            IMOutboxPo update = new IMOutboxPo()
                    .setId(po.getId())
                    .setStatus(next.value())
                    .setAttempts(po.getAttempts())
                    .setUpdatedAt(now);
            outboxDubboService.modify(update);
        }
    }

    /**
     * 读取当前记录并按状态机规则更新状态。
     *
     * @param outboxId           Outbox 主键 ID
     * @param nextStateSupplier  下一个状态计算函数
     * @param attempts           尝试次数
     * @param nextTryAt          下次重试时间戳
     * @param reason             失败原因
     */
    private void updateById(Long outboxId,
                            java.util.function.Function<OutboxStatus, OutboxStatus> nextStateSupplier,
                            int attempts,
                            Long nextTryAt,
                            String reason) {
        if (outboxId == null) {
            return;
        }
        IMOutboxPo original = outboxDubboService.queryOne(outboxId);
        if (original == null) {
            return;
        }
        OutboxStatus current = OutboxStatus.from(original.getStatus());
        OutboxStatus next = nextStateSupplier.apply(current);
        long now = DateTimeUtils.getCurrentUTCTimestamp();
        IMOutboxPo update = new IMOutboxPo()
                .setId(outboxId)
                .setStatus(next.value())
                .setAttempts(Math.max(0, attempts))
                .setUpdatedAt(now);
        if (nextTryAt != null) {
            update.setNextTryAt(nextTryAt);
        }
        if (StringUtils.hasText(reason)) {
            String trimmed = reason.length() > 1024 ? reason.substring(0, 1024) : reason;
            update.setLastError(trimmed);
        }
        outboxDubboService.modify(update);
        if (next == OutboxStatus.DLX) {
            log.warn("outbox 进入 DLX: outboxId={}, messageId={}, attempts={}, reason={}",
                    outboxId, original.getMessageId(), attempts, update.getLastError());
        }
    }
}
