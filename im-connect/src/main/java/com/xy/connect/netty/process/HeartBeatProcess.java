package com.xy.connect.netty.process;

import com.xy.connect.config.ConfigCenter;
import com.xy.connect.config.LogConstant;
import com.xy.connect.utils.JedisUtil;
import com.xy.connect.utils.JsonUtil;
import com.xy.connect.utils.MessageUtils;
import com.xy.imcore.enums.IMessageType;
import com.xy.imcore.model.IMWsConnMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import static com.xy.imcore.constants.Constant.IMUSERPREFIX;

@Slf4j(topic = LogConstant.NETTY)
public class HeartBeatProcess implements WsProcess {

    @Override
    public void process(ChannelHandlerContext ctx, IMWsConnMessage sendInfo) {

        String token = sendInfo.getToken();

        String userId = parseUsername(ctx, token);

        // 响应ws
        MessageUtils.send(ctx, sendInfo.setCode(IMessageType.HEART_BEAT.getCode()));

        // 设置心跳时间  用于 用户 redis 信息续期
        JedisUtil.getInstance().expire(IMUSERPREFIX + userId, ConfigCenter.nettyConfig.getNettyConfig().getHeartBeatTime() * 2);

        log.info("用户:{} 心跳中...... ", userId);
    }


}
