package com.xy.connect.message;


import com.xy.connect.config.LogConstant;
import com.xy.connect.domain.MessageEvent;
import com.xy.connect.message.process.MessageProcess;
import com.xy.connect.message.process.impl.GroupMessageProcess;
import com.xy.connect.message.process.impl.SingleMessageProcess;
import com.xy.connect.message.process.impl.VideoMessageProcess;
import com.xy.connect.utils.JacksonUtil;
import com.xy.core.enums.IMessageType;
import com.xy.core.model.IMessageWrap;
import com.xy.spring.annotations.core.Component;
import com.xy.spring.annotations.event.EventListener;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 消息处理,分发交由不同的处理器
 */
@Slf4j(topic = LogConstant.Message)
@Component
public class MessageHandler {

    private static final Map<Integer, MessageProcess> HANDLERS = new HashMap<>();

    static {
        // 初始化消息类型与处理器的映射
        // 单聊消息处理handler
        HANDLERS.put(IMessageType.SINGLE_MESSAGE.getCode(), new SingleMessageProcess());
        // 群聊消息处理handler
        HANDLERS.put(IMessageType.GROUP_MESSAGE.getCode(), new GroupMessageProcess());
        // 群聊消息处理handler
        HANDLERS.put(IMessageType.VIDEO_MESSAGE.getCode(), new VideoMessageProcess());
    }

    /**
     * 监听消息
     *
     * @param messageEvent
     */
    @EventListener(MessageEvent.class)
    public void handleMessage(MessageEvent messageEvent) {
        // 将消息体解析为通用消息包装对象
        IMessageWrap<Object> messageWrap = JacksonUtil.fromJson(
                messageEvent.getBody(),
                IMessageWrap.class
        );

        // 使用 Optional 链式处理空值及处理器获取
        Optional.ofNullable(messageWrap)
                .map(wrap -> HANDLERS.get(wrap.getCode()))
                .ifPresent(processor -> processor.dispose(messageWrap));
    }
}