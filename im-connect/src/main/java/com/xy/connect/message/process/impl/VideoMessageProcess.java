package com.xy.connect.message.process.impl;

import com.xy.connect.config.LogConstant;
import com.xy.connect.message.channels.UserChannelCtxMap;
import com.xy.connect.message.process.MessageProcess;
import com.xy.connect.utils.JsonUtil;
import com.xy.imcore.enums.IMessageType;
import com.xy.imcore.model.IMVideoMessageDto;
import com.xy.imcore.model.IMWsConnMessage;
import com.xy.imcore.model.IMessageWrap;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 视频消息处理
 */
@Slf4j(topic = LogConstant.RABBITMQ)
public class VideoMessageProcess implements MessageProcess {

    @Override
    public void dispose(IMessageWrap IMessageWrap) {

        IMVideoMessageDto messageDto = JsonUtil.convertToActualObject(IMessageWrap.getData(), IMVideoMessageDto.class);

        log.info("接收到视频消息，发送者:{},接收者:{}", messageDto.getFrom_id(), messageDto.getTo_id());
        try {

            ChannelHandlerContext ctx = UserChannelCtxMap.getChannel(messageDto.getTo_id());

            if (ctx != null && ctx.channel().isOpen()) {

                // 推送消息到用户
                IMWsConnMessage wsConnMessage = IMWsConnMessage.builder()
                        .code(IMessageType.VIDEO_MESSAGE.getCode())
                        .data(messageDto)
                        .build();
                ctx.channel().writeAndFlush(wsConnMessage);
                // 消息发送成功确认

            } else {
                // 消息推送失败确认
                log.error("未找到WS连接，发送者:{},接收者:{}，内容:{}", messageDto.getFrom_id(), messageDto.getTo_id()
                );
            }


        } catch (Exception e) {
            log.error("发送异常，发送者:{},接收者:{}，内容:{}", messageDto.getFrom_id(), messageDto.getTo_id());
        }

    }


}
