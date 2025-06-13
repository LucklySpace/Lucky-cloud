package com.xy.connect.message.process.impl;

import com.xy.connect.channel.UserChannelCtxMap;
import com.xy.connect.config.LogConstant;
import com.xy.connect.message.process.MessageProcess;
import com.xy.connect.utils.JacksonUtil;
import com.xy.imcore.enums.IMessageType;
import com.xy.imcore.model.IMConnectMessage;
import com.xy.imcore.model.IMVideoMessageDto;
import com.xy.imcore.model.IMessageWrap;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * 视频消息处理
 */
@Slf4j(topic = LogConstant.Message)
public class VideoMessageProcess implements MessageProcess {

    @Override
    public void dispose(IMessageWrap IMessageWrap) {

        IMVideoMessageDto messageDto = JacksonUtil.convertToActualObject(IMessageWrap.getData(), IMVideoMessageDto.class);

        log.info("接收到视频消息，发送者:{},接收者:{}", messageDto.getFromId(), messageDto.getToId());
        try {

            Channel ctx = UserChannelCtxMap.getChannel(messageDto.getToId());

            if (ctx != null && ctx.isOpen()) {

                // 推送消息到用户
                IMConnectMessage wsConnMessage = IMConnectMessage.builder()
                        .code(IMessageType.VIDEO_MESSAGE.getCode())
                        .data(messageDto)
                        .build();
                ctx.writeAndFlush(wsConnMessage);
                // 消息发送成功确认

            } else {
                // 消息推送失败确认
                log.error("未找到WS连接，发送者:{},接收者:{}，内容:{}", messageDto.getFromId(), messageDto.getToId()
                );
            }


        } catch (Exception e) {
            log.error("发送异常，发送者:{},接收者:{}，内容:{}", messageDto.getFromId(), messageDto.getToId());
        }

    }


}
