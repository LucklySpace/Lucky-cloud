package com.xy.connect.message.process.impl;

import com.xy.connect.channel.UserChannelCtxMap;
import com.xy.connect.config.LogConstant;
import com.xy.connect.message.process.MessageProcess;
import com.xy.connect.utils.JacksonUtil;
import com.xy.imcore.enums.IMessageType;
import com.xy.imcore.model.IMConnectMessage;
import com.xy.imcore.model.IMPrivateMessageDto;
import com.xy.imcore.model.IMessageWrap;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * 私聊消息处理
 */
@Slf4j(topic = LogConstant.Message)
public class SingleMessageProcess implements MessageProcess {

    @Override
    public void dispose(IMessageWrap IMessageWrap) {
        // 1. 序列化获取消息
        IMPrivateMessageDto messageDto = JacksonUtil.convertToActualObject(IMessageWrap.getData(), IMPrivateMessageDto.class);

        log.info("接收到消息，发送者:{},接收者:{}，内容:{}", messageDto.getFromId(), messageDto.getToId(),
                JacksonUtil.toJson(messageDto.getMessageBody()));
        try {
            // 2. 获取接收者的channel
            Channel ctx = UserChannelCtxMap.getChannel(messageDto.getToId());

            // 3. 推送消息到接收者
            if (ctx != null && ctx.isOpen()) {
                // 推送消息到用户
                IMConnectMessage<Object> wsConnMessage = IMConnectMessage.builder()
                        .code(IMessageType.SINGLE_MESSAGE.getCode())
                        .data(messageDto)
                        .build();

                // 消息发送成功确认
                ctx.writeAndFlush(wsConnMessage);

            } else {
                // 消息推送失败确认
                log.error("未找到WS连接，发送者:{},接收者:{}，内容:{}", messageDto.getFromId(), messageDto.getToId(),
                        messageDto.getMessageBody());
            }

        } catch (Exception e) {
            log.error("发送异常，发送者:{},接收者:{}，内容:{}", messageDto.getFromId(), messageDto.getToId(),
                    messageDto.getMessageBody());
        }

    }


}
