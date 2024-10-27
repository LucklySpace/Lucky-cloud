package com.xy.connect.netty.process;

import com.xy.connect.config.LogConstant;
import com.xy.connect.utils.MessageUtils;
import com.xy.imcore.enums.IMessageType;
import com.xy.imcore.model.IMWsConnMessage;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = LogConstant.NETTY)
public class HeartBeatProcess implements WsProcess {

    @Override
    public void process(ChannelHandlerContext ctx, IMWsConnMessage sendInfo) {

        String token = sendInfo.getToken();

        String username = parseUsername(ctx, token);
        // 响应ws
        MessageUtils.send(ctx, sendInfo.setCode(IMessageType.HEART_BEAT.getCode()));

        log.info("用户:{} 心跳中...... ", username);
    }

}
