package com.xy.lucky.connect.netty.process.impl;


import com.xy.lucky.connect.config.LogConstant;
import com.xy.lucky.connect.netty.process.WebsocketProcess;
import com.xy.lucky.connect.redis.RedisTemplate;
import com.xy.lucky.connect.utils.MessageUtils;
import com.xy.lucky.core.enums.IMessageType;
import com.xy.lucky.core.model.IMessageWrap;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.annotations.core.Value;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = LogConstant.HeartBeat)
@Component
public class HeartBeatProcess implements WebsocketProcess {

    @Value("netty.config.heartBeatTime")
    private Integer heartBeatTime;

    @Value("auth.tokenExpired")
    private Integer tokenExpired;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void process(ChannelHandlerContext ctx, IMessageWrap sendInfo) {

        String token = sendInfo.getToken();

        String userId = parseUsername(ctx, token);

        // 如果token剩余时间小于有效时间，  则通知客户端刷新token
        Integer code = getRemaining(token) <= tokenExpired ? IMessageType.REFRESHTOKEN.getCode() : IMessageType.HEART_BEAT.getCode();

        if (code.equals(IMessageType.REFRESHTOKEN.getCode())) {
            log.warn("用户:{} token已过期", userId);
        }

        String message = code.equals(IMessageType.REFRESHTOKEN.getCode()) ? "token已过期" : "心跳成功";

        // 响应ws
        MessageUtils.send(ctx, sendInfo.setCode(code).setMessage(message));

        //redisTemplate.expire(USER_CACHE_PREFIX + userId , heartBeatTime * 2);

        log.info("用户:{} 心跳中...... ", userId);
    }


}
