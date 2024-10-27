package com.xy.server.service;


import com.xy.imcore.model.IMSingleMessageDto;
import com.xy.imcore.model.IMessageWrap;
import com.xy.server.response.Result;

/**
 * 单聊
 */

public interface SingleChatService {

    Result send(IMSingleMessageDto IMSingleMessageDto);

}
