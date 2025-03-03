package com.xy.server.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.xy.imcore.enums.IMessageType;
import com.xy.server.domain.dto.ChatDto;
import com.xy.server.domain.po.ImGroupMessagePo;
import com.xy.server.domain.po.ImPrivateMessagePo;
import com.xy.server.mapper.ImGroupMessageMapper;
import com.xy.server.mapper.ImPrivateMessageMapper;
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

    @Override
    public Map<Integer, Object> list(ChatDto chatDto) {
        String userId = chatDto.getFromId();
        Long sequence = chatDto.getSequence();

        CompletableFuture<List<ImPrivateMessagePo>> singleMessageFuture = CompletableFuture.supplyAsync(() -> imPrivateMessageMapper.selectSingleMessage(userId, sequence));
        CompletableFuture<List<ImGroupMessagePo>> groupMessageFuture = CompletableFuture.supplyAsync(() -> imGroupMessageMapper.selectGroupMessage(userId, sequence));

        Map<Integer, Object> map = new HashMap<>();

        try {
            List<ImPrivateMessagePo> IMSingleMessageDtoList = singleMessageFuture.get();
            if (ObjectUtil.isNotEmpty(IMSingleMessageDtoList)) {
                map.put(IMessageType.SINGLE_MESSAGE.getCode(), IMSingleMessageDtoList);
            }

            List<ImGroupMessagePo> IMGroupMessagePoDtoList = groupMessageFuture.get();
            if (ObjectUtil.isNotEmpty(IMGroupMessagePoDtoList)) {
                map.put(IMessageType.GROUP_MESSAGE.getCode(), IMGroupMessagePoDtoList);
            }
        } catch (InterruptedException | ExecutionException e) {
            // 处理异常
        }

        return map;
    }

    @Override
    public List singleCheck(ChatDto chatDto) {
        List list = new ArrayList();
        switch (IMessageType.getByCode(chatDto.getChatType())) {
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

        String fromId = chatDto.getFromId();

        String toId = chatDto.getToId();

        Long sequence = chatDto.getSequence();

        List<ImPrivateMessagePo> messageHistoryList = imPrivateMessageMapper.selectSingleMessageByToId(fromId, toId, sequence);

        return messageHistoryList;
    }

    private List getGroupMessageChat(ChatDto chatDto) {

        String groupId = chatDto.getFromId();

        String userId = chatDto.getToId();

        Long sequence = chatDto.getSequence();

        List<ImGroupMessagePo> messageHistoryList = imGroupMessageMapper.selectGroupMessageByGroupId(userId, groupId, sequence);

        return messageHistoryList;
    }

}
