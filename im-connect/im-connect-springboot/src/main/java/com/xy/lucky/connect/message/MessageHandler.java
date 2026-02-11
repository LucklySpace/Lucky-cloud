package com.xy.lucky.connect.message;

import com.xy.lucky.connect.config.LogConstant;
import com.xy.lucky.connect.domain.MessageEvent;
import com.xy.lucky.connect.message.process.impl.*;
import com.xy.lucky.connect.utils.JacksonUtil;
import com.xy.lucky.core.enums.IMessageType;
import com.xy.lucky.core.model.IMessageWrap;
import com.xy.lucky.core.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j(topic = LogConstant.Message)
@Component
@RequiredArgsConstructor
public class MessageHandler {

    private final GroupMessageProcess groupMessageProcess;

    private final SingleMessageProcess singleMessageProcess;

    private final VideoMessageProcess videoMessageProcess;

    private final ForceLogoutProcess forceLogoutProcess;

    private final GroupOperationProcess groupOperationProcess;

    private final MessageActionProcess messageActionProcess;

    @EventListener(MessageEvent.class)
    public void handleMessage(MessageEvent messageEvent) {
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
            log.warn("未知的消息类型: code={}, body={}", messageWrap.getCode(), safeTruncate(body));
            return;
        }

        try {
            switch (msgType) {
                case SINGLE_MESSAGE -> singleMessageProcess.dispose(messageWrap);
                case GROUP_MESSAGE -> groupMessageProcess.dispose(messageWrap);
                case VIDEO_MESSAGE -> videoMessageProcess.dispose(messageWrap);
                case FORCE_LOGOUT -> forceLogoutProcess.dispose(messageWrap);
                case GROUP_OPERATION -> groupOperationProcess.dispose(messageWrap);
                case MESSAGE_OPERATION -> messageActionProcess.dispose(messageWrap);
                default -> log.warn("没有为消息类型 {} 注册处理器，忽略该消息", msgType);
            }
            log.debug("消息分发完成，type={}, requestId={}", msgType, messageWrap.getRequestId());
        } catch (Exception e) {
            log.error("处理消息时出错，type={}, requestId={}, err={}", msgType, messageWrap.getRequestId(), e.getMessage(), e);
        }
    }

    private String safeTruncate(String s) {
        if (s == null) return "<null>";
        int max = 512;
        return s.length() <= max ? s : s.substring(0, max) + "...(truncated)";
    }
}
