package com.xy.connect.netty.process;

import com.xy.connect.config.LogConstant;
import com.xy.connect.message.channels.UserChannelCtxMap;
import com.xy.connect.utils.JedisUtil;
import com.xy.connect.utils.JsonUtil;
import com.xy.connect.utils.MessageUtils;
import com.xy.imcore.enums.IMessageType;
import com.xy.imcore.model.IMRegisterUserDto;
import com.xy.imcore.model.IMWsConnMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.xy.connect.StartCenter.BROKERID;
import static com.xy.imcore.constants.Constant.IMUSERPREFIX;

@Slf4j(topic = LogConstant.NETTY)
public class LoginProcess implements WsProcess {

    // 用户登录时记录活跃数到redis 键前缀
    private final String hyperloglogKey = "IM-ACTIVE-USERS-";

    /**
     * 用户登录时保存机器码到redis
     *
     * @param ctx      channel
     * @param sendInfo 消息
     */
    @Override
    public void process(ChannelHandlerContext ctx, IMWsConnMessage sendInfo) {

        String token = sendInfo.getToken();

        // 设置用户id属性
        String userId = getUserIdFromChannel(ctx);

        // 绑定用户和channel
        UserChannelCtxMap.addChannel(userId, ctx);

        IMRegisterUserDto IMRegisterUserDto = new IMRegisterUserDto();

        IMRegisterUserDto.setUserId(userId);

        IMRegisterUserDto.setBrokerId(BROKERID);

        IMRegisterUserDto.setToken(token);

        // 保存用户信息到redis,前缀加用户名为 key； 用户名，token，长连接机器码 为 value
        JedisUtil.getInstance().set(IMUSERPREFIX + userId, JsonUtil.toJSONString(IMRegisterUserDto));

        // 响应ws
        MessageUtils.send(ctx, sendInfo.setCode(IMessageType.LOGIN.getCode()));

        // 用户上线时，记录到redis,记录日活
        addActiveUser(userId);

        log.debug("用户数：{}", UserChannelCtxMap.getAllChannel().size());

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
     * 用户上线时，记录到redis,记录日活
     * https://mp.weixin.qq.com/s/ay8YO6e6uHxkO3qR5sgVAQ
     *
     * @param userId 用户id
     */
    public void addActiveUser(String userId) {
        String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
        JedisUtil.getInstance().pfadd(hyperloglogKey + dateStr, userId);
    }


}

