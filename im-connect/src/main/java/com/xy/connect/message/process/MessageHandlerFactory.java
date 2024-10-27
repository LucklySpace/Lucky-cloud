package com.xy.connect.message.process;

import com.xy.connect.message.process.impl.GroupMessageProcess;
import com.xy.connect.message.process.impl.SingleMessageProcess;
import com.xy.connect.message.process.impl.VideoMessageProcess;
import com.xy.imcore.enums.IMessageType;

import java.util.HashMap;
import java.util.Map;

public class MessageHandlerFactory {

    private static final Map<Integer, MessageProcess> HANDLERS = new HashMap();

    static {
        /** 单聊消息处理handler */
        HANDLERS.put(IMessageType.SINGLE_MESSAGE.getCode(), new SingleMessageProcess());

        /** 群聊消息处理handler */
        HANDLERS.put(IMessageType.GROUP_MESSAGE.getCode(), new GroupMessageProcess());

        /** 群聊消息处理handler */
        HANDLERS.put(IMessageType.VIDEO_MESSAGE.getCode(), new VideoMessageProcess());

        /** 服务端返回的消息发送状态报告处理handler */
        // HANDLERS.put(MessageType.SERVER_MSG_SENT_STATUS_REPORT.getMsgType(), new ServerReportMessageHandler());
    }

    private MessageHandlerFactory() {

    }

    /**
     * 根据消息类型获取对应的处理handler
     *
     * @param msgType
     * @return
     */
    public static MessageProcess getHandlerByMsgType(Integer msgType) {
        return HANDLERS.get(msgType);
    }


}
