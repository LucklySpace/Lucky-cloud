package com.xy.lucky.connect.message.process.impl;

import com.xy.lucky.connect.channel.UserChannelMap;
import com.xy.lucky.connect.config.LogConstant;
import com.xy.lucky.connect.message.process.MessageProcess;
import com.xy.lucky.core.enums.IMessageType;
import com.xy.lucky.core.model.IMessageWrap;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * 私聊消息处理
 */
@Slf4j(topic = LogConstant.Message)
@Component
@RequiredArgsConstructor
public class SingleMessageProcess implements MessageProcess<Object> {

    private final UserChannelMap userChannelMap;

    @Override
    public void dispose(IMessageWrap<Object> messageWrap) {
        String typeName = getSupportedType().name();
        List<String> ids = messageWrap.getIds();
        Object data = messageWrap.getData();

        log.info("接收 [{}] 消息, 目标用户数 {}, 内容: {}", typeName, ids != null ? ids.size() : 0, data);

        if (ids == null || ids.isEmpty()) {
            log.warn("[{}] 消息目标 ID 列表为空，忽略处理", typeName);
            return;
        }

        try {
            for (String userId : ids) {
                Collection<Channel> channels = userChannelMap.getChannelsByUser(userId);
                if (channels.isEmpty()) {
                    log.debug("用户 {} 在线通道为空，无法推送 [{}] 消息", userId, typeName);
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
        } catch (Exception e) {
            log.error("[{}] 消息处理异常: userId={}, err={}", typeName, ids, e.getMessage(), e);
        }
    }

    @Override
    public IMessageType getSupportedType() {
        return IMessageType.SINGLE_MESSAGE;
    }
}
