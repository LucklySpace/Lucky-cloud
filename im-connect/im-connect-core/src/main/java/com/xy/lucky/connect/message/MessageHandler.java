package com.xy.lucky.connect.message;


import com.xy.lucky.connect.channel.UserChannelMap;
import com.xy.lucky.connect.config.LogConstant;
import com.xy.lucky.connect.domain.MessageEvent;
import com.xy.lucky.connect.utils.JacksonUtil;
import com.xy.lucky.core.enums.IMessageType;
import com.xy.lucky.core.model.IMessageWrap;
import com.xy.lucky.core.utils.StringUtils;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.annotations.core.Value;
import com.xy.lucky.spring.annotations.event.EventListener;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j(topic = LogConstant.Message)
@Component
public class MessageHandler {

    @Autowired
    private UserChannelMap userChannelMap;

    private static final EnumSet<IMessageType> FORWARD_TYPES = EnumSet.of(
            IMessageType.SINGLE_MESSAGE,
            IMessageType.GROUP_MESSAGE,
            IMessageType.VIDEO_MESSAGE,
            IMessageType.GROUP_OPERATION,
            IMessageType.MESSAGE_OPERATION
    );
    private final ConcurrentHashMap<String, Long> processedRequestIdMap = new ConcurrentHashMap<>();
    private final AtomicLong processCounter = new AtomicLong(0);

    @Value("${netty.config.mqDeduplicateWindowMs:300000}")
    private long mqDeduplicateWindowMs;

    @Value("${netty.config.mqDeduplicateMaxEntries:200000}")
    private int mqDeduplicateMaxEntries;

    @EventListener(MessageEvent.class)
    public void handleMessage(MessageEvent messageEvent) {
        try {
            String body = messageEvent.getBody();
            if (StringUtils.isBlank(body)) {
                log.warn("收到空消息体，忽略处理");
                return;
            }

            IMessageWrap<Object> messageWrap = JacksonUtil.parseObject(body, IMessageWrap.class);
            if (Objects.isNull(messageWrap)) {
                log.warn("反序列化结果为 null，body={}", safeTruncate(body));
                return;
            }
            if (isDuplicate(messageWrap.getRequestId())) {
                return;
            }

            IMessageType msgType = IMessageType.getByCode(messageWrap.getCode());
            if (Objects.isNull(msgType)) {
                log.warn("未知的消息类型 code={}, body={}", messageWrap.getCode(), safeTruncate(body));
                return;
            }

            switch (msgType) {
                case FORCE_LOGOUT -> forceLogout(messageWrap);
                default -> {
                    if (!FORWARD_TYPES.contains(msgType)) {
                        log.warn("没有为消息类型 {} 注册处理器，忽略该消息", msgType);
                        return;
                    }
                    forwardToTargets(msgType, messageWrap);
                }
            }
            log.debug("消息分发完成，type={}, requestId={}", msgType, messageWrap.getRequestId());
        } catch (Exception e) {
            log.error("处理消息时出错，err={}", e.getMessage(), e);
        }
    }

    private void forceLogout(IMessageWrap<Object> messageWrap) {
        List<String> ids = messageWrap.getIds();
        if (ids == null || ids.isEmpty()) {
            log.warn("[FORCE_LOGOUT] 消息目标 ID 列表为空，忽略处理");
            return;
        }
        String deviceType = messageWrap.getDeviceType();
        for (String userId : ids) {
            log.info("执行强制下线指令: userId={}, deviceType={}", userId, deviceType);
            userChannelMap.removeChannel(userId, deviceType, true);
        }
    }

    private void forwardToTargets(IMessageType msgType, IMessageWrap<Object> messageWrap) {
        List<String> ids = messageWrap.getIds();
        if (ids == null || ids.isEmpty()) {
            log.warn("[{}] 消息目标 ID 列表为空，忽略处理", msgType.name());
            return;
        }
        int pushCount = 0;
        for (String userId : ids) {
            Collection<Channel> channels = userChannelMap.getChannelsByUser(userId);
            if (channels.isEmpty()) {
                log.debug("用户 {} 在线通道为空，无法推送 [{}] 消息", userId, msgType.name());
                continue;
            }
            for (Channel channel : channels) {
                if (channel != null && channel.isActive()) {
                    channel.writeAndFlush(messageWrap, channel.voidPromise());
                    pushCount++;
                } else {
                    log.debug("用户 {} 的通道已失效，无法推送消息", userId);
                }
            }
        }
        log.debug("消息推送完成: type={}, targetUserCount={}, pushedChannelCount={}, requestId={}",
                msgType.name(), ids.size(), pushCount, messageWrap.getRequestId());
    }

    private String safeTruncate(String s) {
        if (s == null) return "<null>";
        final int MAX = 512;
        return s.length() <= MAX ? s : s.substring(0, MAX) + "...(truncated)";
    }

    private boolean isDuplicate(String requestId) {
        if (!StringUtils.hasText(requestId)) {
            return false;
        }
        long now = System.currentTimeMillis();
        long deduplicateWindowMs = Math.max(1000L, mqDeduplicateWindowMs);
        Long previous = processedRequestIdMap.putIfAbsent(requestId, now);
        if (previous == null) {
            cleanupProcessedRequestIds(now, deduplicateWindowMs);
            return false;
        }
        if (now - previous <= deduplicateWindowMs) {
            cleanupProcessedRequestIds(now, deduplicateWindowMs);
            return true;
        }
        processedRequestIdMap.put(requestId, now);
        cleanupProcessedRequestIds(now, deduplicateWindowMs);
        return false;
    }

    private void cleanupProcessedRequestIds(long now, long deduplicateWindowMs) {
        long count = processCounter.incrementAndGet();
        int maxEntries = Math.max(10000, mqDeduplicateMaxEntries);
        if (count % 512 != 0 && processedRequestIdMap.size() < maxEntries) {
            return;
        }
        long expireBefore = now - deduplicateWindowMs;
        processedRequestIdMap.entrySet().removeIf(entry -> {
            Long timestamp = entry.getValue();
            return timestamp == null || timestamp < expireBefore;
        });
        int overflow = processedRequestIdMap.size() - maxEntries;
        if (overflow <= 0) {
            return;
        }
        Iterator<Map.Entry<String, Long>> iterator = processedRequestIdMap.entrySet().iterator();
        while (overflow > 0 && iterator.hasNext()) {
            iterator.next();
            iterator.remove();
            overflow--;
        }
    }
}
