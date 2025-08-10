package com.xy.connect.message.process.impl;

import com.xy.connect.channel.UserChannelCtxMap;
import com.xy.connect.config.LogConstant;
import com.xy.connect.message.process.MessageProcess;
import com.xy.core.enums.IMessageType;
import com.xy.core.model.IMConnectMessage;
import com.xy.core.model.IMessageWrap;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 私聊消息处理
 */
@Slf4j(topic = LogConstant.Message)
public class SingleMessageProcess implements MessageProcess {

    @Override
    public void dispose(IMessageWrap<Object> messageWrap) {
        // 1. 序列化获取消息
        log.info("接收到消息  接收者:{}，内容:{}", messageWrap.getIds(),
                messageWrap.getData());
        try {

            List<String> ids = messageWrap.getIds();

            for (String id : ids) {
                // 2. 获取接收者的channel
                Channel ctx = UserChannelCtxMap.getChannel(id);

                // 3. 推送消息到接收者
                if (ctx != null && ctx.isOpen()) {
                    // 推送消息到用户
                    IMConnectMessage<Object> wsConnMessage = IMConnectMessage.builder()
                            .code(IMessageType.SINGLE_MESSAGE.getCode())
                            .data(messageWrap.getData())
                            .build();

                    // 消息发送成功确认
                    ctx.writeAndFlush(wsConnMessage);

                } else {
                    // 消息推送失败确认
                    log.error("未找到WS连接，接收者:{}，内容:{}", messageWrap.getIds(),
                            messageWrap.getData());
                }
            }
        } catch (Exception e) {
            log.error("发送异常，接收者:{}，内容:{}", messageWrap.getIds(),
                    messageWrap.getData());
        }
    }
}
