package com.xy.connect.message.process;

import com.xy.core.enums.IMessageType;
import com.xy.core.model.IMessageWrap;

/**
 * 消息处理接口
 * - 增加获取支持的消息类型方法，用于自动注册
 */
public interface MessageProcess<T> {

    /**
     * 处理消息
     */
    void dispose(IMessageWrap<T> messageWrap);

    /**
     * 获取此处理器支持的消息类型
     *
     * @return 消息类型
     */
    IMessageType getSupportedType();
}
