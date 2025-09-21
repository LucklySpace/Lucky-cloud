package com.xy.connect.netty.process.impl;


import com.xy.connect.channel.UserChannelMap;
import com.xy.connect.config.LogConstant;
import com.xy.connect.netty.process.WebsocketProcess;
import com.xy.connect.redis.RedisTemplate;
import com.xy.connect.utils.JacksonUtil;
import com.xy.connect.utils.MessageUtils;
import com.xy.core.constants.IMConstant;
import com.xy.core.enums.IMessageType;
import com.xy.core.model.IMConnectMessage;
import com.xy.core.model.IMRegisterUser;
import com.xy.core.utils.StringUtils;
import com.xy.spring.annotations.core.Autowired;
import com.xy.spring.annotations.core.Component;
import com.xy.spring.annotations.core.Value;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.xy.core.constants.IMConstant.USER_CACHE_PREFIX;

@Slf4j(topic = LogConstant.Login)
@Component
public class LoginProcess implements WebsocketProcess {

    private static final AttributeKey<String> DEVICE_TYPE_ATTR_KEY = AttributeKey.valueOf(IMConstant.IM_DEVICE_TYPE);

    // 用户登录时记录活跃数到redis 键前缀
    private final String hyperloglogKey = "IM-ACTIVE-USERS-";

    @Value("${brokerId}")
    private String brokerId;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserChannelMap userChannelMap;

    /**
     * 心跳时间（秒），从配置中读取
     * 实际 Redis key 的 TTL 设置为 heartBeatTime * 2
     */
    @Value("${netty.config.heartBeatTime}")
    private Integer heartBeatTime;

    /**
     * 多端支持开关
     */
    @Value("${netty.multi-device.enabled:false}")
    private boolean multiDeviceEnabled;

    /**
     * 用户登录时保存用户 token、机器码 和 deviceType 到 redis
     *
     * @param ctx      channel
     * @param sendInfo 消息
     */
    @Override
    public void process(ChannelHandlerContext ctx, IMConnectMessage sendInfo) {

        String token = sendInfo.getToken();

        // 设置用户id属性
        String userId = getUserIdFromChannel(ctx);

        // 提取 deviceType（优先从消息 metadata，其次从 channel attr 或默认 "default"）
        String deviceType = extractDeviceType(sendInfo, ctx);

        // 多端验证：检查是否已存在同设备类型的连接
        if (multiDeviceEnabled && hasExistingDeviceConnection(userId, deviceType, ctx)) {

            log.warn("用户 {} 已存在 {} 设备连接，拒绝重复登录", userId, deviceType);

            MessageUtils.send(ctx, sendInfo.setCode(IMessageType.ERROR.getCode()).setMessage("用户已登录"));

            ctx.close(); // 关闭当前连接

            return;
        }

        // 绑定用户、设备和channel（自动处理旧连接踢掉）
        userChannelMap.addChannel(userId, ctx, deviceType);

        // 初始化用户对象
        IMRegisterUser imRegisterUser = new IMRegisterUser()
                // 用户id
                .setUserId(userId)
                // 用户token
                .setBrokerId(brokerId)
                // 用户token
                .setToken(token)
                // 设备类型
                .setDeviceType(deviceType);

        redisTemplate.setEx(USER_CACHE_PREFIX + userId, JacksonUtil.toJson(imRegisterUser), heartBeatTime * 2);

        // 响应ws
        MessageUtils.send(ctx, sendInfo.setCode(IMessageType.LOGIN.getCode()));

        // 用户上线时，记录到redis,记录日活（按 userId，不分设备）
        addActiveUser(userId);

        log.info("用户:{} {} 设备连接成功", userId, deviceType);
    }

    /**
     * 提取 deviceType
     * - 优先从消息 metadata（如 sendInfo.getMetadata().get("deviceType")）
     * - 其次从 channel attr
     * - 默认 "default"
     */
    private String extractDeviceType(IMConnectMessage sendInfo, ChannelHandlerContext ctx) {
        // 从消息 metadata 提取
        String deviceType = sendInfo.getDeviceType();
        if (!StringUtils.hasText(deviceType)) {
            // 从 channel attr 提取（如果已设置）
            deviceType = ctx.channel().attr(DEVICE_TYPE_ATTR_KEY).get();
        }
        return deviceType != null && !deviceType.isEmpty() ? deviceType : "default";
    }

    /**
     * 检查用户是否已存在同设备类型的连接（多端验证）
     * - 单端模式：忽略
     * - 多端模式：检查 userChannelMap.getChannel(userId, deviceType) 是否存在且活跃
     */
    private boolean hasExistingDeviceConnection(String userId, String deviceType, ChannelHandlerContext ctx) {
        if (!multiDeviceEnabled) {
            return false; // 单端不验证
        }
        Channel existing = userChannelMap.getChannel(userId, deviceType);
        if (existing != null && existing.isActive() && !existing.equals(ctx.channel())) {
            // 已存在活跃连接，踢掉旧的（可选：或直接拒绝新连接）
            existing.close(); // 踢掉旧连接
            log.info("踢掉用户 {} 的旧 {} 设备连接", userId, deviceType);
            return true;
        }
        return false;
    }

    /**
     * 获取用户id
     */
    private String getUserIdFromChannel(ChannelHandlerContext ctx) {
        AttributeKey<String> attr = AttributeKey.valueOf(IMConstant.IM_USER);
        return ctx.channel().attr(attr).get();
    }

    /**
     * 使用HyperLogLog存储用户活跃信息  用户上线时，记录到redis,记录日活
     * https://mp.weixin.qq.com/s/ay8YO6e6uHxkO3qR5sgVAQ
     *
     * @param userId 用户id
     */
    public void addActiveUser(String userId) {
        String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
        redisTemplate.pfadd(hyperloglogKey + dateStr, userId);
    }
}