package com.xy.lucky.connect.netty.process.impl;


import com.xy.lucky.connect.config.LogConstant;
import com.xy.lucky.connect.config.properties.NettyProperties;
import com.xy.lucky.connect.netty.process.WebsocketProcess;
import com.xy.lucky.connect.redis.RedisTemplate;
import com.xy.lucky.connect.utils.MessageUtils;
import com.xy.lucky.core.constants.IMConstant;
import com.xy.lucky.core.enums.IMDeviceType;
import com.xy.lucky.core.enums.IMessageType;
import com.xy.lucky.core.model.IMessageWrap;
import com.xy.lucky.core.utils.StringUtils;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.annotations.core.Value;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = LogConstant.HeartBeat)
@Component
public class HeartBeatProcess implements WebsocketProcess {

    private static final AttributeKey<String> USER_ATTR = AttributeKey.valueOf(IMConstant.IM_USER);
    private static final AttributeKey<String> DEVICE_ATTR = AttributeKey.valueOf(IMConstant.IM_DEVICE_TYPE);

    @Autowired
    private NettyProperties nettyProperties;

    @Value("${auth.tokenExpired:3}")
    private Integer tokenExpired;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void process(ChannelHandlerContext ctx, IMessageWrap sendInfo) {

        String token = sendInfo.getToken();

        String userId = ctx.channel().attr(USER_ATTR).get();
        String deviceType = ctx.channel().attr(DEVICE_ATTR).get();

        if (!StringUtils.hasText(userId)) {
            userId = parseUsername(ctx, token);
        }

        // 如果token剩余时间小于有效时间，  则通知客户端刷新token
        Integer code = IMessageType.HEART_BEAT_SUCCESS.getCode();

        String message = "心跳成功";
        if (StringUtils.hasText(token) && tokenExpired != null && tokenExpired > 0) {
            try {
                if (getRemaining(token) <= tokenExpired) {
                    code = IMessageType.REFRESH_TOKEN.getCode();
                }
            } catch (Exception ignored) {
            }
        }

        if (code.equals(IMessageType.REFRESH_TOKEN.getCode())) {
            log.warn("用户:{} token已过期", userId);
            message = "token已过期";
        }

        // 响应ws
        MessageUtils.send(ctx, sendInfo.setCode(code).setMessage(message));

        long ttlSeconds = toSeconds(nettyProperties.getHeartBeatTime() + nettyProperties.getTimeout());
        redisTemplate.expire(IMConstant.USER_CACHE_PREFIX + userId, ttlSeconds);

        IMDeviceType.DeviceGroup group = IMDeviceType.getDeviceGroupOrDefault(deviceType, IMDeviceType.DeviceGroup.DESKTOP);

        log.info("platform:{} 用户:{} 心跳中", group, userId);
    }

    private long toSeconds(long millis) {
        long s = (millis + 999L) / 1000L;
        return Math.max(1L, s);
    }

}
