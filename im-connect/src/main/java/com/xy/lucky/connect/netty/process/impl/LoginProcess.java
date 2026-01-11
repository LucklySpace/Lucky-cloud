package com.xy.lucky.connect.netty.process.impl;


import com.xy.lucky.connect.channel.UserChannelMap;
import com.xy.lucky.connect.config.LogConstant;
import com.xy.lucky.connect.config.properties.NettyProperties;
import com.xy.lucky.connect.netty.process.WebsocketProcess;
import com.xy.lucky.connect.redis.RedisTemplate;
import com.xy.lucky.connect.utils.JacksonUtil;
import com.xy.lucky.connect.utils.MessageUtils;
import com.xy.lucky.core.constants.IMConstant;
import com.xy.lucky.core.enums.IMDeviceType;
import com.xy.lucky.core.enums.IMessageType;
import com.xy.lucky.core.model.IMRegisterUser;
import com.xy.lucky.core.model.IMessageWrap;
import com.xy.lucky.core.utils.StringUtils;
import com.xy.lucky.spring.annotations.core.Autowired;
import com.xy.lucky.spring.annotations.core.Component;
import com.xy.lucky.spring.annotations.core.Value;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.xy.lucky.core.constants.IMConstant.USER_CACHE_PREFIX;

@Slf4j(topic = LogConstant.Login)
@Component
public class LoginProcess implements WebsocketProcess {

    private static final AttributeKey<String> USER_ATTR = AttributeKey.valueOf(IMConstant.IM_USER);
    private static final AttributeKey<String> DEVICE_ATTR = AttributeKey.valueOf(IMConstant.IM_DEVICE_TYPE);


    // 用户登录时记录活跃数到redis 键前缀
    private final String hyperloglogKey = "IM-ACTIVE-USERS-";

    @Value("${brokerId}")
    private String brokerId;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserChannelMap userChannelMap;

    @Autowired
    private NettyProperties nettyProperties;

    /**
     * 用户登录时保存用户 token、机器码 和 deviceType 到 redis
     *
     * @param ctx      channel
     * @param sendInfo 消息
     */
    @Override
    public void process(ChannelHandlerContext ctx, IMessageWrap sendInfo) throws Exception {

        String token = sendInfo.getToken();

        // 设置用户id属性
        String userId = ctx.channel().attr(USER_ATTR).get();

        String deviceType = sendInfo.getDeviceType();

        String routeKey = USER_CACHE_PREFIX + userId;

        Object existing = redisTemplate.get(routeKey);

        if (Objects.nonNull(existing)) {
            IMRegisterUser old = JacksonUtil.parseObject(existing, IMRegisterUser.class);
            if (old != null && StringUtils.hasText(old.getBrokerId()) && !old.getBrokerId().equals(brokerId)) {
                IMessageWrap<Object> kick = new IMessageWrap<>()
                        .setCode(IMessageType.FORCE_LOGOUT.getCode())
                        .setIds(List.of(userId))
                        .setMessage("同端登录，已被踢下线");
                ctx.channel().writeAndFlush(kick);
            }
        }

        // 绑定用户、设备和channel（自动处理旧连接踢掉）
        userChannelMap.addChannel(userId, ctx, deviceType);

        long ttlSeconds = toSeconds(nettyProperties.getHeartBeatTime() + nettyProperties.getTimeout());

        // 初始化用户对象
        IMRegisterUser imRegisterUser = new IMRegisterUser()
                // 用户id
                .setUserId(userId)
                // 用户token
                .setBrokerId(brokerId)
                // 用户token
                .setToken(token);
                // 设备类型
//                .setDrivers(buildDrivers(sendInfo, userId));

        String json = JacksonUtil.toJSONString(imRegisterUser);

        redisTemplate.setEx(USER_CACHE_PREFIX + userId, json, ttlSeconds);
        redisTemplate.setEx(routeKey, json, ttlSeconds);

        // 响应ws
        MessageUtils.send(ctx, sendInfo.setCode(IMessageType.REGISTER_SUCCESS.getCode()));

        // 用户上线时，记录到redis,记录日活（按 userId，不分设备）
        addActiveUser(userId);

        log.info("用户:{} {} 设备连接成功", userId, deviceType);
    }

    public Map<String, IMRegisterUser.Driver> buildDrivers(IMessageWrap sendInfo, String userId) {
        String deviceType = sendInfo.getDeviceType();
        //IMDeviceType imDeviceType = IMDeviceType.ofOrDefault(deviceType, IMDeviceType.WIN);

        IMDeviceType.DeviceGroup group = IMDeviceType.getDeviceGroupOrDefault(deviceType, IMDeviceType.DeviceGroup.DESKTOP);

//        // 获取或创建用户映射
//        IMUserChannel imUserChannel = userChannelMap.getUserChannels().computeIfAbsent(userId, k -> {
//            IMUserChannel u = new IMUserChannel();
//            u.setUserId(userId);
//            u.setUserChannelMap(new ConcurrentHashMap<>());
//            return u;
//        });
//
//
//        if (imUserChannel.getUserChannelMap().containsKey(group)) {
//            IMUserChannel.UserChannel userChannel = imUserChannel.getUserChannelMap().get(group);
//            if (userChannel.getChannel() != null) {
//                log.warn("用户:{} {} 设备已存在，强制下线", userId, deviceType);
//                // 旧设备已存在，强制下线
//                userChannel.getChannel().close();
//            }
//            imUserChannel.getUserChannelMap().put(group, new IMUserChannel.UserChannel(userChannel.getChannelId(), imDeviceType, group, null));
//        }


        return Map.of(group.name(), new IMRegisterUser.Driver(sendInfo.getRequestId(), deviceType));
    }

    /**
     * 使用HyperLogLog存储用户活跃信息  用户上线时，记录到redis,记录日活
     * https://mp.weixin.qq.com/s/ay8YO6e6uHxkO3qR5sgVAQ
     *
     * @param userId 用户id
     */
    public void addActiveUser(String userId) {
        //String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
        // redisTemplate.pfadd(hyperloglogKey + dateStr, userId);
    }

    private long toSeconds(long millis) {
        long s = (millis + 999L) / 1000L;
        return Math.max(1L, s);
    }
}
