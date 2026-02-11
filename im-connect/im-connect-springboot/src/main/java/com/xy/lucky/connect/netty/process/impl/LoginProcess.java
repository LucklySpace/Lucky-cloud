package com.xy.lucky.connect.netty.process.impl;


import com.xy.lucky.connect.channel.UserChannelMap;
import com.xy.lucky.connect.config.LogConstant;
import com.xy.lucky.connect.config.properties.NettyProperties;
import com.xy.lucky.connect.netty.process.WebsocketProcess;
import com.xy.lucky.connect.utils.JacksonUtil;
import com.xy.lucky.connect.utils.MessageUtils;
import com.xy.lucky.core.constants.IMConstant;
import com.xy.lucky.core.enums.IMDeviceType;
import com.xy.lucky.core.enums.IMessageType;
import com.xy.lucky.core.model.IMRegisterUser;
import com.xy.lucky.core.model.IMessageWrap;
import com.xy.lucky.core.utils.StringUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.xy.lucky.core.constants.IMConstant.USER_CACHE_PREFIX;

@Slf4j(topic = LogConstant.Login)
@Component
public class LoginProcess implements WebsocketProcess {

    private static final AttributeKey<String> USER_ATTR = AttributeKey.valueOf(IMConstant.IM_USER);
    private static final AttributeKey<String> DEVICE_ATTR = AttributeKey.valueOf(IMConstant.IM_DEVICE_TYPE);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 用户登录时记录活跃数到redis 键前缀
    private static final String ACTIVE_USERS_PREFIX = "IM-ACTIVE-USERS-";

    @Value("${brokerId}")
    private String brokerId;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserChannelMap userChannelMap;

    @Autowired
    private NettyProperties nettyProperties;


    /**
     * 用户登录处理逻辑
     * 1. 解析设备类型
     * 2. 处理同组设备冲突（踢出旧连接）
     * 3. 绑定本地 Channel 映射
     * 4. 同步更新 Redis 注册信息
     * 5. 响应客户端并记录日活
     */
    @Override
    public void process(ChannelHandlerContext ctx, IMessageWrap sendInfo) throws Exception {
        String userId = ctx.channel().attr(USER_ATTR).get();
        String token = sendInfo.getToken();
        IMDeviceType imDeviceType = resolveDeviceType(ctx, sendInfo);

        log.debug("开始处理用户登录 userId={}, deviceType={}, group={}", userId, imDeviceType.getType(), imDeviceType.getGroup());

        // 1. 处理同组冲突并绑定 Channel
        // UserChannelMap.addChannel 内部已处理本地同组互斥逻辑
        userChannelMap.addChannel(userId, ctx.channel(), imDeviceType);

        // 2. 更新 Redis 全局注册信息（支持多端在线状态同步）
        updateRedisRegistration(userId, token, imDeviceType, sendInfo.getRequestId());

        // 3. 构建并返回成功消息
        sendLoginSuccessResponse(ctx, sendInfo, imDeviceType);

        // 4. 异步记录日活统计
        addActiveUser(userId);

        log.info("用户登录处理完成: userId={}, group={}, type={}", userId, imDeviceType.getGroup(), imDeviceType.getType());
    }

    /**
     * 解析设备类型
     */
    private IMDeviceType resolveDeviceType(ChannelHandlerContext ctx, IMessageWrap sendInfo) {
        String type = StringUtils.hasText(sendInfo.getDeviceType())
                ? sendInfo.getDeviceType()
                : ctx.channel().attr(DEVICE_ATTR).get();
        return IMDeviceType.ofOrDefault(type, IMDeviceType.WEB);
    }

    /**
     * 同步更新 Redis 注册信息
     */
    private void updateRedisRegistration(String userId, String token, IMDeviceType deviceType, String requestId) {
        String routeKey = USER_CACHE_PREFIX + userId;
        long ttlSeconds = toSeconds(nettyProperties.getHeartBeatTime() + nettyProperties.getTimeout());

        // 使用 Optional 简化获取与初始化逻辑
        IMRegisterUser registerUser = Optional.ofNullable(stringRedisTemplate.opsForValue().get(routeKey))
                .map(json -> JacksonUtil.parseObject(json, IMRegisterUser.class))
                .orElseGet(() -> new IMRegisterUser().setUserId(userId).setDrivers(new HashMap<>()));

        // 更新用户全局信息与当前设备驱动信息
        registerUser.setToken(token)
                .setBrokerId(brokerId)
                .getDrivers()
                .put(deviceType.getGroup().name(), new IMRegisterUser.Driver(requestId, deviceType.getType()));

        stringRedisTemplate.opsForValue().set(routeKey, JacksonUtil.toJSONString(registerUser), java.time.Duration.ofSeconds(ttlSeconds));
    }

    /**
     * 构建并返回成功消息
     */
    private void sendLoginSuccessResponse(ChannelHandlerContext ctx, IMessageWrap sendInfo, IMDeviceType deviceType) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("platform", deviceType.getGroup().name());
        metadata.put("deviceType", deviceType.getType());
        metadata.put("brokerId", brokerId);

        sendInfo.setCode(IMessageType.REGISTER_SUCCESS.getCode())
                .setMessage("登录成功")
                .setToken(null)
                .setMetadata(metadata)
                .setDeviceType(deviceType.getType());

        MessageUtils.send(ctx, sendInfo);
    }

    /**
     * 使用 HyperLogLog 存储日活跃用户
     */
    public void addActiveUser(String userId) {
        String key = ACTIVE_USERS_PREFIX + LocalDate.now().format(DATE_FORMATTER);
        stringRedisTemplate.opsForHyperLogLog().add(key, userId);
        stringRedisTemplate.expire(key, java.time.Duration.ofDays(30));
    }

    private long toSeconds(long millis) {
        return Math.max(1L, (millis + 999L) / 1000L);
    }
}
