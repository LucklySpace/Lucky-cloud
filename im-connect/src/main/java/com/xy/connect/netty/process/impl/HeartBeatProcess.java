package com.xy.connect.netty.process.impl;


import com.xy.connect.config.LogConstant;
import com.xy.connect.netty.process.RedisBatchManager;
import com.xy.connect.netty.process.WebsocketProcess;
import com.xy.connect.utils.MessageUtils;
import com.xy.imcore.enums.IMessageType;
import com.xy.imcore.model.IMConnectMessage;
import com.xy.spring.annotations.core.Autowired;
import com.xy.spring.annotations.core.Component;
import com.xy.spring.annotations.core.Value;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = LogConstant.Netty)
@Component
public class HeartBeatProcess implements WebsocketProcess {

    @Value("netty.config.heartBeatTime")
    private Integer heartBeatTime;

    @Value("auth.tokenExpired")
    private Integer tokenExpired;

    @Autowired
    private RedisBatchManager redisBatchManager;

    @Override
    public void process(ChannelHandlerContext ctx, IMConnectMessage sendInfo) {

        String token = sendInfo.getToken();

        String userId = parseUsername(ctx, token);

        // 如果token剩余时间小于有效时间，  则通知客户端刷新token
        Integer code = getRemaining(token) <= tokenExpired ? IMessageType.REFRESHTOKEN.getCode() : IMessageType.HEART_BEAT.getCode();

        // 响应ws
        MessageUtils.send(ctx, sendInfo.setCode(code));

        // 推入心跳处理队列（延迟批量续期）
        redisBatchManager.onUserHeartbeat(userId);

        log.info("用户:{} 心跳中...... ", userId);
    }


}
