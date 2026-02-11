package com.xy.lucky.connect.message.process.impl;

import com.xy.lucky.connect.channel.UserChannelMap;
import com.xy.lucky.connect.message.process.MessageProcess;
import com.xy.lucky.core.enums.IMessageType;
import com.xy.lucky.core.model.IMessageWrap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 强制下线消息处理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ForceLogoutProcess implements MessageProcess<Object> {

    private final UserChannelMap userChannelMap;

    @Override
    public void dispose(IMessageWrap<Object> messageWrap) {
        List<String> ids = messageWrap.getIds();
        if (ids == null || ids.isEmpty()) {
            return;
        }

        String deviceType = messageWrap.getDeviceType();
        for (String userId : ids) {
            log.info("执行强制下线指令: userId={}, deviceType={}", userId, deviceType);
            // 移除并关闭特定设备的本地连接
            userChannelMap.removeChannel(userId, deviceType, true);
        }
    }

    @Override
    public IMessageType getSupportedType() {
        return IMessageType.FORCE_LOGOUT;
    }
}
