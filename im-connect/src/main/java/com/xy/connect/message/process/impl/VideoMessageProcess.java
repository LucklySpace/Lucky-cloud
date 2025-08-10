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
 * 视频消息处理
 */
@Slf4j(topic = LogConstant.Message)
public class VideoMessageProcess implements MessageProcess {

    @Override
    public void dispose(IMessageWrap<Object> messageWrap) {

        log.info("接收到视频消息，接收者:{}  内容:{}", messageWrap.getIds(), messageWrap.getData());

        try {
            List<String> ids = messageWrap.getIds();

            for (String id : ids) {

                Channel ctx = UserChannelCtxMap.getChannel(id);

                if (ctx != null && ctx.isOpen()) {

                    // 推送消息到用户
                    IMConnectMessage wsConnMessage = IMConnectMessage.builder()
                            .code(IMessageType.VIDEO_MESSAGE.getCode())
                            .data(messageWrap.getData())
                            .build();
                    ctx.writeAndFlush(wsConnMessage);
                    // 消息发送成功确认
                } else {
                    // 消息推送失败确认
                    log.error("未找到WS连接，接收者:{}，内容:{}", messageWrap.getIds(), messageWrap.getData());
                }
            }
        } catch (Exception e) {
            log.error("发送异常，接收者:{}，内容:{}", messageWrap.getIds(), messageWrap.getData());
        }

    }


}
