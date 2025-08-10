package com.xy.connect.message.process;

import com.xy.core.model.IMessageWrap;

/**
 * 消息处理接口
 */
public interface MessageProcess {

    void dispose(IMessageWrap<Object> IMessageWrap);

}
