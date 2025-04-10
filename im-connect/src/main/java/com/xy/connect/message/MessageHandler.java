package com.xy.connect.message;


import com.xy.connect.config.LogConstant;
import com.xy.connect.message.process.MessageHandlerFactory;
import com.xy.connect.message.process.MessageProcess;
import com.xy.connect.utils.JsonUtil;
import com.xy.imcore.model.IMessageWrap;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * 消息处理,分发交由不同的处理器
 *
 * @author dense
 */
@Slf4j(topic = LogConstant.RABBITMQ)
public class MessageHandler {

    private final MessageHandlerFactory messageHandlerFactory;

    public MessageHandler(MessageHandlerFactory messageHandlerFactory) {
        this.messageHandlerFactory = messageHandlerFactory;
    }

    public void handlerMessage(byte[] body) {
        // 获取消息
        String messages = new String(body, StandardCharsets.UTF_8);

        // 序列化消息
        IMessageWrap<?> iMessageWrap = JsonUtil.parseObject(messages, IMessageWrap.class);

        MessageProcess messageProcess = null;

        // 根据消息类型，获取对应的处理类
        if (iMessageWrap != null) {
            messageProcess = messageHandlerFactory.getHandlerByMsgType(iMessageWrap.getCode());
        }

        // 处理消息
        if (messageProcess != null) {
            messageProcess.dispose(iMessageWrap);
        }
    }
}
