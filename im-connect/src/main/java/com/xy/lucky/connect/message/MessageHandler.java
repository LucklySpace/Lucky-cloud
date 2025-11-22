package com.xy.lucky.connect.message;


import com.xy.lucky.connect.config.LogConstant;
import com.xy.lucky.connect.domain.MessageEvent;
import com.xy.lucky.connect.message.process.impl.GroupMessageProcess;
import com.xy.lucky.connect.message.process.impl.SingleMessageProcess;
import com.xy.lucky.connect.message.process.impl.VideoMessageProcess;
import com.xy.lucky.connect.utils.JacksonUtil;
import com.xy.lucky.core.enums.IMessageType;
import com.xy.lucky.core.model.IMessageWrap;
import com.xy.lucky.core.utils.StringUtils;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.annotations.event.EventListener;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j(topic = LogConstant.Message)
@Component
public class MessageHandler {

    @Autowired
    GroupMessageProcess groupMessageProcess;

    @Autowired
    SingleMessageProcess singleMessageProcess;

    @Autowired
    VideoMessageProcess videoMessageProcess;

    /**
     * 监听并分发消息
     */
    @EventListener(MessageEvent.class)
    public void handleMessage(MessageEvent messageEvent) {

        final String body = messageEvent.getBody();
        if (StringUtils.isBlank(body) || body.trim().isEmpty()) {
            log.warn("收到空消息体，忽略处理");
            return;
        }

        IMessageWrap<Object> messageWrap = JacksonUtil.parseObject(body, IMessageWrap.class);

        if (Objects.isNull(messageWrap) ) {
            log.warn("反序列化结果为 null，body={}", safeTruncate(body));
            return;
        }

        IMessageType msgType = IMessageType.getByCode(messageWrap.getCode());

        if (Objects.isNull(msgType)) {
            log.warn("未知的消息类型 code={}, body={}", messageWrap.getCode(), safeTruncate(body));
            return;
        }

        try {
            // 直接 switch 分发（基于 code，避免 Map 查找）
            switch (msgType) {
                case IMessageType.GROUP_MESSAGE -> groupMessageProcess.dispose(messageWrap);
                case IMessageType.SINGLE_MESSAGE -> singleMessageProcess.dispose(messageWrap);
                case IMessageType.VIDEO_MESSAGE -> videoMessageProcess.dispose(messageWrap);
                default -> {
                    log.warn("没有为消息类型 {} 注册处理器，忽略该消息", msgType);
                    return;
                }
            }
            log.debug("消息分发完成，type={}, requestId={}", msgType, messageWrap);
        } catch (Exception e) {
            log.error("处理消息时出错，type={}, requestId={}, err={}", msgType, messageWrap, e.getMessage(), e);
        }
    }

    /**
     * 为避免日志过长，安全截断展示 body 前 N 字符（便于排查）
     */
    private String safeTruncate(String s) {
        if (s == null) return "<null>";
        final int MAX = 512;
        return s.length() <= MAX ? s : s.substring(0, MAX) + "...(truncated)";
    }
}