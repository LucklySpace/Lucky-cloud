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
import com.xy.lucky.spring.annotations.event.EventListener;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

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

    @EventListener(MessageEvent.class)
    public void handleMessage(MessageEvent messageEvent) {
        try {
            String body = messageEvent.getBody();
            if (StringUtils.isBlank(body) || body.trim().isEmpty()) {
                log.warn("收到空消息体，忽略处理");
                return;
            }

            IMessageWrap<Object> messageWrap = JacksonUtil.parseObject(body, IMessageWrap.class);
            if (Objects.isNull(messageWrap)) {
                log.warn("反序列化结果为 null，body={}", safeTruncate(body));
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
            log.debug("消息分发完成，type={}, requestId={}", msgType, messageWrap);
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
        log.info("接收到 [{}] 消息, 目标用户数: {}, 内容: {}", msgType.name(), ids != null ? ids.size() : 0, messageWrap.getData());
        if (ids == null || ids.isEmpty()) {
            log.warn("[{}] 消息目标 ID 列表为空，忽略处理", msgType.name());
            return;
        }
        for (String userId : ids) {
            Collection<Channel> channels = userChannelMap.getChannelsByUser(userId);
            if (channels.isEmpty()) {
                log.debug("用户 {} 在线通道为空，无法推送 [{}] 消息", userId, msgType.name());
                continue;
            }
            for (Channel channel : channels) {
                if (channel != null && channel.isActive()) {
                    channel.writeAndFlush(messageWrap);
                } else {
                    log.warn("用户 {} 的通道已失效，无法推送消息", userId);
                }
            }
        }
    }

    private String safeTruncate(String s) {
        if (s == null) return "<null>";
        final int MAX = 512;
        return s.length() <= MAX ? s : s.substring(0, MAX) + "...(truncated)";
    }
}
