package com.xy.lucky.message.rpc;

import com.xy.lucky.message.service.MessageService;
import com.xy.lucky.core.model.IMGroupAction;
import com.xy.lucky.core.model.IMGroupMessage;
import com.xy.lucky.core.model.IMSingleMessage;
import com.xy.lucky.rpc.api.message.MessageDubboService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * 消息 Dubbo 服务实现，对外暴露消息发送相关 RPC 能力。
 */
@DubboService
@RequiredArgsConstructor
public class MessageDubboServiceImpl implements MessageDubboService {

    private final MessageService messageService;

    /**
     * 发送单聊消息。
     *
     * @param dto 单聊消息内容
     * @return 发送结果
     */
    @Override
    public IMSingleMessage sendSingleMessage(IMSingleMessage dto) {
        return messageService.sendSingleMessage(dto);
    }

    /**
     * 发送群聊消息。
     *
     * @param dto 群聊消息内容
     * @return 发送结果
     */
    @Override
    public IMGroupMessage sendGroupMessage(IMGroupMessage dto) {
        return messageService.sendGroupMessage(dto);
    }

    /**
     * 发送群组操作消息。
     *
     * @param dto 群组操作内容
     */
    @Override
    public void sendGroupAction(IMGroupAction dto) {
        messageService.sendGroupAction(dto);
    }
}
