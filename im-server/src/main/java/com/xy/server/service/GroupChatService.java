package com.xy.server.service;


import com.xy.imcore.model.IMGroupMessageDto;
import com.xy.server.domain.dto.GroupDto;
import com.xy.server.domain.dto.GroupInviteDto;
import com.xy.server.response.Result;

/**
 * 单聊
 */

public interface GroupChatService {

    Result send(IMGroupMessageDto IMGroupMessageDto);

    Result member(GroupDto groupDto);

    void quit(GroupDto groupDto);

    Result invite(GroupInviteDto groupInviteDto);

    Result info(GroupDto groupDto);
}
