package com.xy.connect.message;


import com.xy.connect.config.LogConstant;
import com.xy.connect.message.process.MessageProcess;
import com.xy.connect.message.process.impl.GroupMessageProcess;
import com.xy.connect.message.process.impl.SingleMessageProcess;
import com.xy.connect.message.process.impl.VideoMessageProcess;
import com.xy.connect.utils.JsonUtil;
import com.xy.imcore.enums.IMessageType;
import com.xy.imcore.model.IMessageWrap;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 消息处理,分发交由不同的处理器
 */
@Slf4j(topic = LogConstant.RABBITMQ)
public class MessageHandler {

    public void handlerMessage(byte[] body) {
        // 获取消息
        String messages = new String(body, StandardCharsets.UTF_8);

        // 序列化消息
        IMessageWrap IMessageWrap = JsonUtil.parseObject(messages, IMessageWrap.class);

        MessageProcess messageProcess = null;

        // 根据消息类型，获取对应的处理类
        switch (Objects.requireNonNull(IMessageType.getByCode(IMessageWrap.getCode()))) {
            case SINGLE_MESSAGE:
                messageProcess = new SingleMessageProcess();
                break;
            case GROUP_MESSAGE:
                messageProcess = new GroupMessageProcess();
                break;
            case VIDEO_MESSAGE:
                messageProcess = new VideoMessageProcess();
                break;
        }

        // 处理消息
        messageProcess.dispose(IMessageWrap);
    }
}
