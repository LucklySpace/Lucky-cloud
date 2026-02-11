package com.xy.lucky.connect.netty.process.impl;


import com.xy.lucky.connect.config.LogConstant;
import com.xy.lucky.connect.config.properties.NettyProperties;
import com.xy.lucky.connect.netty.process.WebsocketProcess;
import com.xy.lucky.connect.utils.MessageUtils;
import com.xy.lucky.core.constants.IMConstant;
import com.xy.lucky.core.enums.IMDeviceType;
import com.xy.lucky.core.enums.IMessageType;
import com.xy.lucky.core.model.IMessageWrap;
import com.xy.lucky.core.utils.JwtUtil;
import com.xy.lucky.core.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j(topic = LogConstant.HeartBeat)
@Component
public class HeartBeatProcess implements WebsocketProcess {

    private static final AttributeKey<String> USER_ATTR = AttributeKey.valueOf(IMConstant.IM_USER);
    private static final AttributeKey<String> DEVICE_ATTR = AttributeKey.valueOf(IMConstant.IM_DEVICE_TYPE);

    @Value("${auth.tokenExpired:3}")
    private Integer tokenExpired;

    @Autowired
    private NettyProperties nettyProperties;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${brokerId:}")
    private String brokerId;


    @Override
    public void process(ChannelHandlerContext ctx, IMessageWrap sendInfo) {
        String token = sendInfo.getToken();
        String userId = ctx.channel().attr(USER_ATTR).get();
        String deviceTypeStr = ctx.channel().attr(DEVICE_ATTR).get();
        if (!StringUtils.hasText(userId) && StringUtils.hasText(token)) {
            userId = JwtUtil.getUsername(token);
        }
        if (!StringUtils.hasText(userId)) {
            log.warn("心跳处理失败：未识别的用户身份");
            ctx.close();
            return;
        }
        IMDeviceType deviceType = IMDeviceType.ofOrDefault(deviceTypeStr, IMDeviceType.WEB);
        Integer code = IMessageType.HEART_BEAT_SUCCESS.getCode();
        String message = "心跳成功";
        if (StringUtils.hasText(token) && tokenExpired != null && tokenExpired > 0) {
            try {
                if (JwtUtil.getRemaining(token, java.util.concurrent.TimeUnit.MINUTES) <= tokenExpired) {
                    code = IMessageType.REFRESH_TOKEN.getCode();
                    message = "token 即将过期，请及时刷新";
                }
            } catch (Exception ignored) {
            }
        }
        java.util.Map<String, String> metadata = new java.util.HashMap<>();
        metadata.put("平台", deviceType.getGroup().name());
        metadata.put("设备类型", deviceType.getType());
        if (StringUtils.hasText(brokerId)) {
            metadata.put("brokerId", brokerId);
        }
        sendInfo.setCode(code)
                .setMessage(message)
                .setToken(null)
                .setDeviceType(deviceType.getType())
                .setMetadata(metadata);
        MessageUtils.send(ctx, sendInfo);
        long ttlSeconds = Math.max(1L, (nettyProperties.getHeartBeatTime() + nettyProperties.getTimeout() + 999L) / 1000L);
        stringRedisTemplate.expire(IMConstant.USER_CACHE_PREFIX + userId, java.time.Duration.ofSeconds(ttlSeconds));
        log.debug("心跳成功: userId={}, group={}, type={}", userId, deviceType.getGroup(), deviceType.getType());
    }
}
