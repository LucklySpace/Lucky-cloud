package com.xy.connect.message.process.impl;

import com.xy.connect.channel.UserChannelMap;
import com.xy.connect.config.LogConstant;
import com.xy.connect.message.process.MessageProcess;
import com.xy.core.enums.IMessageType;
import com.xy.core.model.IMessageWrap;
import com.xy.spring.annotations.core.Autowired;
import com.xy.spring.annotations.core.Service;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;

/**
 * 私聊消息处理
 */
@Slf4j(topic = LogConstant.Message)
@Service("singleMessageProcess")
public class SingleMessageProcess implements MessageProcess {

    @Autowired
    private UserChannelMap userChannelMap;

    @Override
    public void dispose(IMessageWrap<Object> messageWrap) {
        // 1. 序列化获取消息
        log.info("接收到消息  接收者:{}，内容:{}", messageWrap.getIds(),
                messageWrap.getData());
        try {

            List<String> ids = messageWrap.getIds();

            for (String id : ids) {
                // 2. 获取接收者的channel
                Collection<Channel> ctxMap = userChannelMap.getChannelsByUser(id);

                for (Channel ctx : ctxMap) {

                    // 3. 推送消息到接收者
                    if (ctx != null && ctx.isOpen()) {
                        // 推送消息到用户
                        IMessageWrap<Object> wsConnMessage = IMessageWrap.builder()
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
            }
        } catch (Exception e) {
            log.error("发送异常，接收者:{}，内容:{}", messageWrap.getIds(),
                    messageWrap.getData());
        }
    }
}
