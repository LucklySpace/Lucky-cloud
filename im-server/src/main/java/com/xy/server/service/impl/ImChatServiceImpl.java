package com.xy.server.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.imcore.enums.IMStatus;
import com.xy.imcore.enums.IMessageReadStatus;
import com.xy.imcore.enums.IMessageType;
import com.xy.server.domain.dto.ChatDto;
import com.xy.server.domain.po.*;
import com.xy.server.domain.vo.ChatVo;
import com.xy.server.mapper.*;
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
public class ImChatServiceImpl extends ServiceImpl<ImChatMapper, ImChatPo>
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

        switch (IMessageType.getByCode(chatDto.getChatType())) {
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

        QueryWrapper<ImChatPo> chatQuery = new QueryWrapper<>();

        chatQuery.eq("owner_id", chatDto.getFromId());

        chatQuery.eq("to_id", chatDto.getToId());

        chatQuery.eq("chat_type", chatDto.getChatType());

        ImChatPo imChatPO = imChatMapper.selectOne(chatQuery);

        if (ObjectUtil.isEmpty(imChatPO)) {

            imChatPO = new ImChatPo();

            String id = UUID.randomUUID().toString();

            imChatPO.setChatId(id)
                    .setOwnerId(chatDto.getFromId())
                    .setToId(chatDto.getToId())
                    .setIsMute(IMStatus.NO.getCode())
                    .setIsTop(IMStatus.NO.getCode())
                    .setChatType(IMessageType.SINGLE_MESSAGE.getCode());

            imChatMapper.insert(imChatPO);
        }

        BeanUtils.copyProperties(imChatPO, chatVo);

        // 创建单聊会话
        if (chatDto.getChatType().equals(IMessageType.SINGLE_MESSAGE.getCode())) {

            ImUserDataPo imUserDataPo = imUserDataMapper.selectOne(new QueryWrapper<ImUserDataPo>().eq("user_id", chatDto.getToId()));

            chatVo.setName(imUserDataPo.getName());

            chatVo.setAvatar(imUserDataPo.getAvatar());

            chatVo.setId(imUserDataPo.getUserId());
        }

        // 创建群聊会话
        if (chatDto.getChatType().equals(IMessageType.GROUP_MESSAGE.getCode())) {

            ImGroupPo imGroupPo = imGroupMapper.selectOne(new QueryWrapper<ImGroupPo>().eq("group_id", chatDto.getToId()));

            chatVo.setName(imGroupPo.getGroupName());

            chatVo.setAvatar(imGroupPo.getAvatar());

            chatVo.setId(imGroupPo.getGroupId());
        }

        return chatVo;
    }

    @Override
    public ChatVo one(String fromId, String toId) {
        QueryWrapper<ImChatPo> chatQuery = new QueryWrapper<>();

        chatQuery.eq("owner_id", fromId)
                .eq("to_id", toId);

        ImChatPo imChatPO = imChatMapper.selectOne(chatQuery);

        return getChat(imChatPO);
    }

    @Override
    public List<ChatVo> list(ChatDto chatDto) {

        QueryWrapper<ImChatPo> chatQuery = new QueryWrapper<>();

        chatQuery.eq("owner_id", chatDto.getFromId());

        chatQuery.gt("sequence", chatDto.getSequence());

        List<ImChatPo> imChatPos = imChatMapper.selectList(chatQuery);

        List<CompletableFuture<ChatVo>> chatFutures = imChatPos.stream()
                .map(e -> CompletableFuture.supplyAsync(() -> getChat(e)))
                .collect(Collectors.toList());

        return chatFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    private ChatVo getChat(ImChatPo e) {

        switch (IMessageType.getByCode(e.getChatType())) {
            case SINGLE_MESSAGE:
                return getSingleMessageChat(e);
            case GROUP_MESSAGE:
                return getGroupMessageChat(e);
            default:
                return new ChatVo(); // 处理未知类型的对话
        }
    }

    private ChatVo getSingleMessageChat(ImChatPo e) {

        ChatVo chatVo = new ChatVo();

        BeanUtils.copyProperties(e, chatVo);

        String ownerId = chatVo.getOwnerId();

        String toId = chatVo.getToId();

        ImPrivateMessagePo IMSingleMessageDto = imPrivateMessageMapper.selectLastSingleMessage(ownerId, toId);

        chatVo.setMessageTime(0L);

        if (ObjectUtil.isNotEmpty(IMSingleMessageDto)) {

            chatVo.setMessage(IMSingleMessageDto.getMessageBody());

            chatVo.setMessageContentType(IMSingleMessageDto.getMessageContentType());

            chatVo.setMessageTime(IMSingleMessageDto.getMessageTime());
        }

        Integer unread = imPrivateMessageMapper.selectReadStatus(toId, ownerId, IMessageReadStatus.UNREAD.code());

        if (ObjectUtil.isNotEmpty(unread)) {

            chatVo.setUnread(unread);
        }

        String targetUserId = ownerId.equals(toId) ? chatVo.getOwnerId() : chatVo.getToId();

        ImUserDataPo imUserDataPo = imUserDataMapper.selectOne(new QueryWrapper<ImUserDataPo>().eq("user_id", targetUserId));

        chatVo.setName(imUserDataPo.getName());

        chatVo.setAvatar(imUserDataPo.getAvatar());

        chatVo.setId(imUserDataPo.getUserId());

        return chatVo;
    }

    private ChatVo getGroupMessageChat(ImChatPo e) {

        ChatVo chatVo = new ChatVo();

        BeanUtils.copyProperties(e, chatVo);

        String ownerId = chatVo.getOwnerId();

        String groupId = chatVo.getToId();

//        QueryWrapper<ImGroupMessage> messageQuery = new QueryWrapper<>();
//
//        messageQuery.eq("groupId", groupId);

        ImGroupMessagePo IMGroupMessagePoDto = imGroupMessageMapper.selectLastGroupMessage(ownerId, groupId);

        chatVo.setMessageTime(0L);

        if (ObjectUtil.isNotEmpty(IMGroupMessagePoDto)) {

            chatVo.setMessage(IMGroupMessagePoDto.getMessageBody());

            chatVo.setMessageTime(IMGroupMessagePoDto.getMessageTime());
        }

        QueryWrapper<ImGroupMessageStatusPo> messageStatusQuery = new QueryWrapper<>();

        messageStatusQuery.eq("group_id", groupId);

        messageStatusQuery.eq("to_id", ownerId);

        messageStatusQuery.eq("read_status", IMessageReadStatus.UNREAD.code());

        List<ImGroupMessageStatusPo> imGroupMessageStatusPoList = imGroupMessageStatusMapper.selectList(messageStatusQuery);

        if (ObjectUtil.isNotEmpty(imGroupMessageStatusPoList)) {

            chatVo.setUnread(imGroupMessageStatusPoList.size());
        }
        ImGroupPo imGroupPo = imGroupMapper.selectOne(new QueryWrapper<ImGroupPo>().eq("group_id", groupId));

        chatVo.setName(imGroupPo.getGroupName());

        chatVo.setAvatar(imGroupPo.getAvatar());

        chatVo.setId(imGroupPo.getGroupId());

        return chatVo;
    }

    /**
     * 设置单聊消息已读
     *
     * @param chatDto
     */
    private void saveSingleMessageChat(ChatDto chatDto) {
        // 参数校验，避免空指针异常
        if (chatDto == null || chatDto.getFromId() == null || chatDto.getToId() == null) {
            // 此处建议使用日志记录工具记录异常信息
            log.warn("chatDto 或相关字段为 null，无法更新消息状态");
            return;
        }

        // 构造更新对象，将状态标记为已读
        ImPrivateMessagePo updateMessage = new ImPrivateMessagePo();
        updateMessage.setReadStatus(IMessageReadStatus.ALREADY_READ.code());

        // 使用 LambdaUpdateWrapper 进行条件构造，字段名通过方法引用来保证类型安全
        LambdaUpdateWrapper<ImPrivateMessagePo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ImPrivateMessagePo::getFromId, chatDto.getFromId())
                .eq(ImPrivateMessagePo::getToId, chatDto.getToId());

        // 执行更新
        int updatedRows = imPrivateMessageMapper.update(updateMessage, updateWrapper);
        if (updatedRows == 0) {
            log.warn("未更新任何记录，fromId: {}, toId: {}", chatDto.getFromId(), chatDto.getToId());
        } else {
            log.info("成功更新 {} 条记录，fromId: {}, toId: {}", updatedRows, chatDto.getFromId(), chatDto.getToId());
        }
    }

    /**
     * 设置群聊消息已读
     *
     * @param chatDto
     */
    private void saveGroupMessageChat(ChatDto chatDto) {
        // 参数校验
        if (chatDto == null || chatDto.getFromId() == null || chatDto.getToId() == null) {
            log.warn("chatDto 或必要字段为 null，无法更新群消息状态");
            return;
        }

        // 构造更新对象，将状态标记为已读
        ImGroupMessageStatusPo updateStatus = new ImGroupMessageStatusPo();
        updateStatus.setReadStatus(IMessageReadStatus.ALREADY_READ.code());

        // 使用 LambdaUpdateWrapper 构造条件，避免硬编码字段名
        LambdaUpdateWrapper<ImGroupMessageStatusPo> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ImGroupMessageStatusPo::getGroupId, chatDto.getFromId())
                .eq(ImGroupMessageStatusPo::getToId, chatDto.getToId());

        // 执行更新，并记录更新结果
        int updatedRows = imGroupMessageStatusMapper.update(updateStatus, updateWrapper);
        if (updatedRows == 0) {
            log.warn("未更新任何群消息记录，groupId: {}, toId: {}", chatDto.getFromId(), chatDto.getToId());
        } else {
            log.info("成功更新 {} 条群消息记录，groupId: {}, toId: {}", updatedRows, chatDto.getFromId(), chatDto.getToId());
        }
    }

}

