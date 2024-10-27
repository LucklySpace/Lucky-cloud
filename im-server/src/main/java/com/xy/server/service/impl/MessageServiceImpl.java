package com.xy.server.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.xy.imcore.enums.IMessageType;
import com.xy.imcore.model.IMGroupMessageDto;
import com.xy.imcore.model.IMSingleMessageDto;
import com.xy.server.domain.dto.ChatDto;
import com.xy.server.mapper.ImGroupMessageMapper;
import com.xy.server.mapper.ImPrivateMessageMapper;
import com.xy.server.model.ImGroupMessage;
import com.xy.server.model.ImPrivateMessage;
import com.xy.server.service.MessageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class MessageServiceImpl<T> implements MessageService {


    @Resource
    private ImPrivateMessageMapper imPrivateMessageMapper;

    @Resource
    private ImGroupMessageMapper imGroupMessageMapper;

    public Map<Integer, Object> list(ChatDto chatDto) {
        String userId = chatDto.getFrom_id();
        Long sequence = chatDto.getSequence();

        CompletableFuture<List<ImPrivateMessage>> singleMessageFuture = CompletableFuture.supplyAsync(() -> imPrivateMessageMapper.selectSingleMessage(userId, sequence));
        CompletableFuture<List<ImGroupMessage>> groupMessageFuture = CompletableFuture.supplyAsync(() -> imGroupMessageMapper.selectGroupMessage(userId, sequence));

        Map<Integer, Object> map = new HashMap<>();

        try {
            List<ImPrivateMessage> IMSingleMessageDtoList = singleMessageFuture.get();
            if (ObjectUtil.isNotEmpty(IMSingleMessageDtoList)) {
                map.put(IMessageType.SINGLE_MESSAGE.getCode(), IMSingleMessageDtoList);
            }

            List<ImGroupMessage> IMGroupMessageDtoList = groupMessageFuture.get();
            if (ObjectUtil.isNotEmpty(IMGroupMessageDtoList)) {
                map.put(IMessageType.GROUP_MESSAGE.getCode(), IMGroupMessageDtoList);
            }
        } catch (InterruptedException | ExecutionException e) {
            // 处理异常
        }

        return map;
    }

    @Override
    public List singleCheck(ChatDto chatDto) {
        List list = new ArrayList();

        switch (IMessageType.getByCode(chatDto.getChat_type())) {
            case SINGLE_MESSAGE:
                list = getSingleMessageChat(chatDto);
                break;
            case GROUP_MESSAGE:
                list = getGroupMessageChat(chatDto);
                break;
            default:
                //chatVo = new ChatSetVo(); // 处理未知类型的对话
        }
        return list;
    }

    private List getSingleMessageChat(ChatDto chatDto) {

        String fromId = chatDto.getFrom_id();

        String toId = chatDto.getTo_id();

        Long sequence = chatDto.getSequence();

        List<ImPrivateMessage> messageHistoryList = imPrivateMessageMapper.selectSingleMessageByToId(fromId, toId, sequence);

        return messageHistoryList;
    }

    private List getGroupMessageChat(ChatDto chatDto) {

        String groupId = chatDto.getFrom_id();

        String userId = chatDto.getTo_id();

        Long sequence = chatDto.getSequence();

        List<ImGroupMessage> messageHistoryList = imGroupMessageMapper.selectGroupMessageByGroupId(userId, groupId, sequence);

        return messageHistoryList;
    }

}
