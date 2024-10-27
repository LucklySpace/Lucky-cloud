package com.xy.server.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.imcore.enums.IMStatus;
import com.xy.imcore.enums.IMessageReadStatus;
import com.xy.imcore.enums.IMessageType;
import com.xy.imcore.model.IMGroupMessageDto;
import com.xy.imcore.model.IMSingleMessageDto;
import com.xy.server.domain.dto.ChatDto;
import com.xy.server.domain.vo.ChatVo;
import com.xy.server.mapper.*;
import com.xy.server.model.*;
import com.xy.server.service.ImChatService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@Slf4j
@Service
public class ImChatServiceImpl extends ServiceImpl<ImChatMapper, ImChat>
        implements ImChatService {

    @Resource
    private ImChatMapper imChatMapper;
    @Resource
    private ImPrivateMessageMapper imPrivateMessageMapper;
    @Resource
    private ImGroupMessageStatusMapper imGroupMessageStatusMapper;
    @Resource
    private ImGroupMessageMapper imGroupMessageMapper;
    @Resource
    private ImGroupMapper imGroupMapper;
    @Resource
    private ImUserDataMapper imUserDataMapper;


    @Override
    public void read(ChatDto chatDto) {

        switch (IMessageType.getByCode(chatDto.getChat_type())) {
            case SINGLE_MESSAGE:
                saveSingleMessageChat(chatDto);
                break;
            case GROUP_MESSAGE:
                saveGroupMessageChat(chatDto);
                break;
            default:
                //chatVo = new ChatSetVo(); // 处理未知类型的对话
        }
    }

    @Override
    public ChatVo create(ChatDto chatDto) {

        ChatVo chatVo = new ChatVo();

        QueryWrapper<ImChat> chatQuery = new QueryWrapper<>();

        chatQuery.eq("owner_id", chatDto.getFrom_id());

        chatQuery.eq("to_id", chatDto.getTo_id());

        chatQuery.eq("chat_type", chatDto.getChat_type());

        ImChat imChat = imChatMapper.selectOne(chatQuery);

        if (ObjectUtil.isEmpty(imChat)) {

            imChat = new ImChat();

            String id = UUID.randomUUID().toString();

            imChat.setChat_id(id)
                    .setOwner_id(chatDto.getFrom_id())
                    .setTo_id(chatDto.getTo_id())
                    .setIs_mute(IMStatus.NO.getCode())
                    .setIs_top(IMStatus.NO.getCode())
                    .setChat_type(IMessageType.SINGLE_MESSAGE.getCode());

            imChatMapper.insert(imChat);
        }

        BeanUtils.copyProperties(imChat, chatVo);

        // 创建单聊会话
        if (chatDto.getChat_type().equals(IMessageType.SINGLE_MESSAGE.getCode())) {

            ImUserData imUserData = imUserDataMapper.selectOne(new QueryWrapper<ImUserData>().eq("user_id", chatDto.getTo_id()));

            chatVo.setName(imUserData.getName());

            chatVo.setAvatar(imUserData.getAvatar());

            chatVo.setId(imUserData.getUser_id());
        }

        // 创建群聊会话
        if (chatDto.getChat_type().equals(IMessageType.GROUP_MESSAGE.getCode())) {

            ImGroup imGroup = imGroupMapper.selectOne(new QueryWrapper<ImGroup>().eq("group_id", chatDto.getTo_id()));

            chatVo.setName(imGroup.getGroup_name());

            chatVo.setAvatar(imGroup.getAvatar());

            chatVo.setId(imGroup.getGroup_id());
        }

        return chatVo;
    }

    @Override
    public ChatVo one(String fromId, String toId) {
        QueryWrapper<ImChat> chatQuery = new QueryWrapper<>();

        chatQuery.eq("owner_id", fromId)
                .eq("to_id", toId);

        ImChat imChat = imChatMapper.selectOne(chatQuery);

        return getChat(imChat);
    }

    @Override
    public List<ChatVo> list(ChatDto chatDto) {

        QueryWrapper<ImChat> chatQuery = new QueryWrapper<>();

        chatQuery.eq("owner_id", chatDto.getFrom_id());

        chatQuery.gt("sequence", chatDto.getSequence());

        List<ImChat> imChats = imChatMapper.selectList(chatQuery);

        List<CompletableFuture<ChatVo>> chatFutures = imChats.stream()
                .map(e -> CompletableFuture.supplyAsync(() -> getChat(e)))
                .collect(Collectors.toList());

        return chatFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    private ChatVo getChat(ImChat e) {

        switch (IMessageType.getByCode(e.getChat_type())) {
            case SINGLE_MESSAGE:
                return getSingleMessageChat(e);
            case GROUP_MESSAGE:
                return getGroupMessageChat(e);
            default:
                return new ChatVo(); // 处理未知类型的对话
        }
    }

    private ChatVo getSingleMessageChat(ImChat e) {

        ChatVo chatVo = new ChatVo();

        BeanUtils.copyProperties(e, chatVo);

        String ownerId = chatVo.getOwner_id();

        String toId = chatVo.getTo_id();

        ImPrivateMessage IMSingleMessageDto = imPrivateMessageMapper.selectLastSingleMessage(ownerId, toId);

        chatVo.setMessage_time(0l);

        if (ObjectUtil.isNotEmpty(IMSingleMessageDto)) {

            chatVo.setMessage(IMSingleMessageDto.getMessage_body());

            chatVo.setMessage_time(IMSingleMessageDto.getMessage_time());
        }

        Integer unread = imPrivateMessageMapper.selectReadStatus(toId, ownerId, IMessageReadStatus.UNREAD.code());

        if (ObjectUtil.isNotEmpty(unread)) {

            chatVo.setUnread(unread);
        }

        String targetUserId = ownerId.equals(toId) ? chatVo.getOwner_id() : chatVo.getTo_id();

        ImUserData imUserData = imUserDataMapper.selectOne(new QueryWrapper<ImUserData>().eq("user_id", targetUserId));

        chatVo.setName(imUserData.getName());

        chatVo.setAvatar(imUserData.getAvatar());

        chatVo.setId(imUserData.getUser_id());

        return chatVo;
    }

    private ChatVo getGroupMessageChat(ImChat e) {

        ChatVo chatVo = new ChatVo();

        BeanUtils.copyProperties(e, chatVo);

        String ownerId = chatVo.getOwner_id();

        String group_id = chatVo.getTo_id();

//        QueryWrapper<ImGroupMessage> messageQuery = new QueryWrapper<>();
//
//        messageQuery.eq("group_id", group_id);

        ImGroupMessage IMGroupMessageDto = imGroupMessageMapper.selectLastGroupMessage(ownerId, group_id);

        chatVo.setMessage_time(0L);

        if (ObjectUtil.isNotEmpty(IMGroupMessageDto)) {

            chatVo.setMessage(IMGroupMessageDto.getMessage_body());

            chatVo.setMessage_time(IMGroupMessageDto.getMessage_time());
        }

        QueryWrapper<ImGroupMessageStatus> messageStatusQuery = new QueryWrapper<>();

        messageStatusQuery.eq("group_id", group_id);

        messageStatusQuery.eq("to_id", ownerId);

        messageStatusQuery.eq("read_status", IMessageReadStatus.UNREAD.code());

        List<ImGroupMessageStatus> imGroupMessageStatusList = imGroupMessageStatusMapper.selectList(messageStatusQuery);

        if (ObjectUtil.isNotEmpty(imGroupMessageStatusList)) {

            chatVo.setUnread(imGroupMessageStatusList.size());
        }
        ImGroup imGroup = imGroupMapper.selectOne(new QueryWrapper<ImGroup>().eq("group_id", group_id));

        chatVo.setName(imGroup.getGroup_name());

        chatVo.setAvatar(imGroup.getAvatar());

        chatVo.setId(imGroup.getGroup_id());

        return chatVo;
    }

    /**
     * 设置单聊消息已读
     *
     * @param chatDto
     */
    private void saveSingleMessageChat(ChatDto chatDto) {

        ImPrivateMessage imPrivateMessage = new ImPrivateMessage();

        imPrivateMessage.setRead_status(IMessageReadStatus.ALREADY_READ.code());

        UpdateWrapper<ImPrivateMessage> statusUpdateWrapper = new UpdateWrapper<>();

        statusUpdateWrapper.eq("from_id", chatDto.getFrom_id());

        statusUpdateWrapper.eq("to_id", chatDto.getTo_id());

        imPrivateMessageMapper.update(imPrivateMessage, statusUpdateWrapper);
    }

    /**
     * 设置群聊消息已读
     *
     * @param chatDto
     */
    private void saveGroupMessageChat(ChatDto chatDto) {

        ImGroupMessageStatus imGroupMessageStatus = new ImGroupMessageStatus();

        imGroupMessageStatus.setRead_status(IMessageReadStatus.ALREADY_READ.code());

        UpdateWrapper<ImGroupMessageStatus> statusUpdateWrapper = new UpdateWrapper<>();

        statusUpdateWrapper.eq("group_id", chatDto.getFrom_id());

        statusUpdateWrapper.eq("to_id", chatDto.getTo_id());

        imGroupMessageStatusMapper.update(imGroupMessageStatus, statusUpdateWrapper);

    }

}




