package com.xy.connect.message.process.impl;


import com.xy.connect.channel.UserChannelCtxMap;
import com.xy.connect.config.LogConstant;
import com.xy.connect.message.process.MessageProcess;
import com.xy.connect.utils.JacksonUtil;
import com.xy.imcore.enums.IMessageType;
import com.xy.imcore.model.IMConnectMessage;
import com.xy.imcore.model.IMGroupMessageDto;
import com.xy.imcore.model.IMessageWrap;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 群聊消息处理
 */
@Slf4j(topic = LogConstant.Message)
public class GroupMessageProcess implements MessageProcess {

    @Override
    public void dispose(IMessageWrap IMessageWrap) {
        // 1. 序列化获取消息
        IMGroupMessageDto messageDto = JacksonUtil.convertToActualObject(IMessageWrap.getData(), IMGroupMessageDto.class);

        List<String> toList = messageDto.getToList();

        log.info("接收到消息，发送者:{},接收者:{}，内容:{}", messageDto.getFromId(), toList,
                messageDto.getMessageBody());

        try {
            // 清理toList，确保后续处理不会误用
            messageDto.setToList(null);

            // 2. 遍历当前netty中存在的指定群聊用户
            for (String toId : toList) {
                // 3. 获取群聊接收者的channel
                Channel ctx = UserChannelCtxMap.getChannel(toId);
                // 4. 推送消息到接收者
                if (ctx != null && ctx.isOpen()) {
                    // 推送消息到用户
                    IMConnectMessage<Object> wsConnMessage = IMConnectMessage.builder()
                            .code(IMessageType.GROUP_MESSAGE.getCode())
                            .data(messageDto)
                            .build();
                    ctx.writeAndFlush(wsConnMessage);
                    // 消息发送成功确认

                } else {
                    // 消息推送失败确认
                    log.error("未找到WS连接，发送者:{},接收者:{}，内容:{}", messageDto.getFromId(), toId,
                            messageDto.getMessageBody());
                }
            }

        } catch (Exception e) {
            log.error("发送异常，发送者:{},接收者:{}，内容:{}", messageDto.getFromId(), messageDto.getToList(),
                    messageDto.getMessageBody());
        }


    }


}
