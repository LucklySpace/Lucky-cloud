package com.xy.connect.message.process.impl;


import com.xy.connect.config.LogConstant;
import com.xy.connect.message.channels.UserChannelCtxMap;
import com.xy.connect.message.process.MessageProcess;
import com.xy.connect.utils.JsonUtil;
import com.xy.imcore.enums.IMessageType;
import com.xy.imcore.model.IMGroupMessageDto;
import com.xy.imcore.model.IMWsConnMessage;
import com.xy.imcore.model.IMessageWrap;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 群聊消息处理
 */
@Slf4j(topic = LogConstant.RABBITMQ)
public class GroupMessageProcess implements MessageProcess {

    @Override
    public void dispose(IMessageWrap IMessageWrap) {
        // 1. 序列化获取消息
        IMGroupMessageDto messageDto = JsonUtil.convertToActualObject(IMessageWrap.getData(), IMGroupMessageDto.class);

        List<String> to_List = messageDto.getTo_List();

        log.info("接收到消息，发送者:{},接收者:{}，内容:{}", messageDto.getFrom_id(), to_List,
                messageDto.getMessage_body());

        try {
            // 清理to_List，确保后续处理不会误用
            messageDto.setTo_List(null);

            // 2. 遍历当前netty中存在的指定群聊用户
            for (String to_id : to_List) {
                // 3. 获取群聊接收者的channel
                ChannelHandlerContext ctx = UserChannelCtxMap.getChannel(to_id);
                // 4. 推送消息到接收者
                if (ctx != null && ctx.channel().isOpen()) {
                    // 推送消息到用户
                    IMWsConnMessage<Object> wsConnMessage = IMWsConnMessage.builder()
                            .code(IMessageType.GROUP_MESSAGE.getCode())
                            .data(messageDto)
                            .build();
                    ctx.channel().writeAndFlush(wsConnMessage);
                    // 消息发送成功确认

                } else {
                    // 消息推送失败确认
                    log.error("未找到WS连接，发送者:{},接收者:{}，内容:{}", messageDto.getFrom_id(), to_id,
                            messageDto.getMessage_body());
                }
            }

        } catch (Exception e) {
            log.error("发送异常，发送者:{},接收者:{}，内容:{}", messageDto.getFrom_id(), messageDto.getTo_List(),
                    messageDto.getMessage_body());
        }


    }


}
