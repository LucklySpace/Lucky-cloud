package com.xy.connect.message.process;

import com.xy.imcore.model.IMessageWrap;

/**
 * 消息处理接口
 */
public interface MessageProcess {

    void dispose(IMessageWrap IMessageWrap);

}
