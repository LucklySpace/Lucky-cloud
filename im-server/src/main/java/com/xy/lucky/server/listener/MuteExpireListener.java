package com.xy.lucky.server.listener;

import com.xy.lucky.core.constants.IMConstant;
import com.xy.lucky.core.enums.IMessageContentType;
import com.xy.lucky.core.enums.IMessageReadStatus;
import com.xy.lucky.core.enums.IMessageType;
import com.xy.lucky.core.model.IMGroupMessage;
import com.xy.lucky.core.model.IMessage;
import com.xy.lucky.server.service.MessageService;
import com.xy.lucky.utils.time.DateTimeUtils;
import jakarta.annotation.Resource;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * 监听禁言到期
 */
@Component
public class MuteExpireListener extends KeyExpirationEventMessageListener {

    private static final String PREFIX = "im:mute:";
    private static final String GROUP_ALL = PREFIX + "ga:";
    private static final String GROUP_USER = PREFIX + "g:";

    @Resource
    private MessageService messageService;

    public MuteExpireListener(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
    }

    /**
     * 监听到期
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String key = message.toString();
        if (!key.startsWith(PREFIX)) {
            return;
        }

        if (key.startsWith(GROUP_ALL)) {
            handleGroupAllUnmute(key.substring(GROUP_ALL.length()));
            return;
        }

        if (key.startsWith(GROUP_USER)) {
            handleGroupUserUnmute(key.substring(GROUP_USER.length()));
        }
    }

    /**
     * 监听群组全体禁言到期
     */
    private void handleGroupAllUnmute(String raw) {
        sendUnmuteGroupNotice(extractId(raw), null);
    }

    /**
     * 监听群组用户禁言到期
     */
    private void handleGroupUserUnmute(String raw) {
        int idx = raw.indexOf(':');
        if (idx < 0) {
            return;
        }
        String groupId = extractId(raw.substring(0, idx));
        String userId = raw.substring(idx + 1);
        sendUnmuteGroupNotice(groupId, userId);
    }


    /**
     * 发送群禁言解除通知
     *
     * @param groupId 群组id
     * @param userId  用户id
     */
    private void sendUnmuteGroupNotice(String groupId, String userId) {
        messageService.sendGroupMessage(
                IMGroupMessage.builder()
                        .groupId(groupId)
                        .fromId(IMConstant.SYSTEM)
                        .messageType(IMessageType.GROUP_MESSAGE.getCode())
                        .messageContentType(IMessageContentType.TIP.getCode())
                        .messageTime(DateTimeUtils.getCurrentUTCTimestamp())
                        .readStatus(IMessageReadStatus.UNREAD.getCode())
                        .messageBody(new IMessage.TextMessageBody()
                                .setText(userId == null ? "群禁言已解除" : "用户已解除禁言"))
                        .build()
        );
    }

    /**
     * 解析 {groupId} 或普通 groupId
     */
    private String extractId(String value) {
        if (value.length() > 2 && value.charAt(0) == '{') {
            int end = value.indexOf('}');
            if (end > 1) {
                return value.substring(1, end);
            }
        }
        return value;
    }
}

