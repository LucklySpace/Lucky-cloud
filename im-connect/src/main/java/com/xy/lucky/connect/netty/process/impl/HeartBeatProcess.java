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
import com.xy.lucky.core.utils.JwtUtil;
import com.xy.lucky.core.utils.StringUtils;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.annotations.core.Value;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
        String deviceTypeStr = ctx.channel().attr(DEVICE_ATTR).get();

        // 1. 身份识别容错：如果属性中 userId 丢失，尝试解析 Token
        if (!StringUtils.hasText(userId) && StringUtils.hasText(token)) {
            userId = JwtUtil.getUsername(token);
        }

        if (!StringUtils.hasText(userId)) {
            log.warn("心跳处理失败：未识别的用户身份");
            ctx.close();
            return;
        }

        IMDeviceType deviceType = IMDeviceType.ofOrDefault(deviceTypeStr, IMDeviceType.WEB);

        // 2. Token 有效期检查与提醒
        Integer code = IMessageType.HEART_BEAT_SUCCESS.getCode();
        String message = "心跳成功";
        if (StringUtils.hasText(token) && tokenExpired != null && tokenExpired > 0) {
            try {
                // 如果 Token 剩余有效期小于预设阈值 (默认单位: 分钟)，通知客户端刷新
                if (JwtUtil.getRemaining(token, TimeUnit.MINUTES) <= tokenExpired) {
                    code = IMessageType.REFRESH_TOKEN.getCode();
                    message = "token 即将过期，请及时刷新";
                }
            } catch (Exception ignored) {
                // Token 解析失败或过期由网关或 AuthHandler 拦截，心跳此处不做强校验
            }
        }

        // 3. 构造并发送响应
        Map<String, String> metadata = new HashMap<>();
        metadata.put("platform", deviceType.getGroup().name());
        metadata.put("deviceType", deviceType.getType());

        sendInfo.setCode(code)
                .setToken(null)
                .setMessage(message)
                .setMetadata(metadata);

        MessageUtils.send(ctx, sendInfo);

        // 4. 续期 Redis 路由缓存
        long ttlSeconds = toSeconds(nettyProperties.getHeartBeatTime() + nettyProperties.getTimeout());
        redisTemplate.expire(IMConstant.USER_CACHE_PREFIX + userId, ttlSeconds);

        if (log.isDebugEnabled()) {
            log.debug("心跳成功: userId={}, group={}, type={}", userId, deviceType.getGroup(), deviceType.getType());
        }
    }

    private long toSeconds(long millis) {
        return Math.max(1L, (millis + 999L) / 1000L);
    }

}
