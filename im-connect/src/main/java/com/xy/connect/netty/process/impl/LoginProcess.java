package com.xy.connect.netty.process.impl;


import com.xy.connect.channel.UserChannelCtxMap;
import com.xy.connect.config.LogConstant;
import com.xy.connect.netty.process.RedisBatchManager;
import com.xy.connect.netty.process.WebsocketProcess;
import com.xy.connect.redis.RedisTemplate;
import com.xy.connect.utils.JacksonUtil;
import com.xy.connect.utils.MessageUtils;
import com.xy.imcore.enums.IMessageType;
import com.xy.imcore.model.IMConnectMessage;
import com.xy.imcore.model.IMRegisterUser;
import com.xy.spring.annotations.core.Autowired;
import com.xy.spring.annotations.core.Component;
import com.xy.spring.annotations.core.Value;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.xy.imcore.constants.IMConstant.IM_USER_PREFIX;

@Slf4j(topic = LogConstant.Netty)
@Component
public class LoginProcess implements WebsocketProcess {

    // 用户登录时记录活跃数到redis 键前缀
    private final String hyperloglogKey = "IM-ACTIVE-USERS-";

    @Value("brokerId")
    private String brokerId;

    @Autowired
    private RedisBatchManager redisBatchManager;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 用户登录时保存用户 token和 机器码 到 redis
     *
     * @param ctx      channel
     * @param sendInfo 消息
     */
    @Override
    public void process(ChannelHandlerContext ctx, IMConnectMessage sendInfo) {

        String token = sendInfo.getToken();

        // 设置用户id属性
        String userId = getUserIdFromChannel(ctx);

        // 绑定用户和channel
        UserChannelCtxMap.addChannel(userId, ctx);

        // 初始化用户对象
        IMRegisterUser imRegisterUser = new IMRegisterUser()
                // 用户id
                .setUserId(userId)
                // 用户token
                .setBrokerId(brokerId)
                // 用户token
                .setToken(token);

        redisBatchManager.onUserAdd(imRegisterUser);

        // 响应ws
        MessageUtils.send(ctx, sendInfo.setCode(IMessageType.LOGIN.getCode()));

        // 用户上线时，记录到redis,记录日活
        addActiveUser(userId);

        log.debug("用户数：{}", UserChannelCtxMap.getAllChannels().size());

        log.info("用户:{} 连接成功 ", userId);
    }

    /**
     * 获取用户id
     */
    private String getUserIdFromChannel(ChannelHandlerContext ctx) {
        AttributeKey<String> attr = AttributeKey.valueOf("userId");
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

