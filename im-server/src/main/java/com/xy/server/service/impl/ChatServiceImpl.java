package com.xy.server.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.xy.domain.dto.ChatDto;
import com.xy.domain.po.ImChatPo;
import com.xy.domain.po.ImGroupPo;
import com.xy.domain.po.ImUserDataPo;
import com.xy.domain.vo.ChatVo;
import com.xy.imcore.enums.IMStatus;
import com.xy.imcore.enums.IMessageType;
import com.xy.server.api.database.chat.ImChatFeign;
import com.xy.server.api.database.group.ImGroupFeign;
import com.xy.server.api.database.user.ImUserFeign;
import com.xy.server.service.ChatService;
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
public class ChatServiceImpl implements ChatService {

    @Resource
    private ImChatFeign imChatFeign;

    @Resource
    private ImUserFeign imUserFeign;

    @Resource
    private ImGroupFeign imGroupFeign;


    /**
     * 读消息
     *
     * @param chatDto
     */
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

    /**
     * 创建会话
     *
     * @param chatDto 会话信息
     * @return
     */
    @Override
    public ChatVo create(ChatDto chatDto) {

        // 获取会话
        ImChatPo imChatPO = imChatFeign.getOne(chatDto.getFromId(), chatDto.getToId(), chatDto.getChatType());

        if (ObjectUtil.isEmpty(imChatPO)) {

            imChatPO = new ImChatPo();

            String chatId = UUID.randomUUID().toString();

            imChatPO.setChatId(chatId)
                    .setOwnerId(chatDto.getFromId())
                    .setToId(chatDto.getToId())
                    .setIsMute(IMStatus.NO.getCode())
                    .setIsTop(IMStatus.NO.getCode())
                    .setChatType(IMessageType.SINGLE_MESSAGE.getCode());

            if (imChatFeign.insert(imChatPO)) {

                log.info("会话信息插入成功,会话id:{},发送人id:{},接收人id:{}", chatId, chatDto.getFromId(), chatDto.getToId());
            } else {

                log.error("群成员信息插入失败,会话id:{},发送人id:{},接收人id:{}", chatId, chatDto.getFromId(), chatDto.getToId());
            }
        }

        ChatVo chatVo = new ChatVo();

        BeanUtils.copyProperties(imChatPO, chatVo);

        // 创建单聊会话
        if (chatDto.getChatType().equals(IMessageType.SINGLE_MESSAGE.getCode())) {

            ImUserDataPo imUserDataPo = imUserFeign.getOne(chatDto.getToId());

            chatVo.setName(imUserDataPo.getName());

            chatVo.setAvatar(imUserDataPo.getAvatar());

            chatVo.setId(imUserDataPo.getUserId());
        }

        // 创建群聊会话
        if (chatDto.getChatType().equals(IMessageType.GROUP_MESSAGE.getCode())) {

            ImGroupPo imGroupPo = imGroupFeign.getOneGroup(chatDto.getToId());

            chatVo.setName(imGroupPo.getGroupName());

            chatVo.setAvatar(imGroupPo.getAvatar());

            chatVo.setId(imGroupPo.getGroupId());
        }

        return chatVo;
    }

    /**
     * 获取会话信息
     *
     * @param fromId
     * @param toId
     * @return
     */
    @Override
    public ChatVo one(String fromId, String toId) {

//        QueryWrapper<ImChatPo> chatQuery = new QueryWrapper<>();
//
//        chatQuery.eq("owner_id", fromId)
//                .eq("to_id", toId);

        ImChatPo imChatPO = imChatFeign.getOne(fromId, toId, null);

        return getChat(imChatPO);
    }

    @Override
    public List<ChatVo> list(ChatDto chatDto) {

        List<ImChatPo> imChatPos = imChatFeign.getChatList(chatDto.getFromId(), chatDto.getSequence());

        List<CompletableFuture<ChatVo>> chatFutures = imChatPos.stream()
                .map(e -> CompletableFuture.supplyAsync(() -> getChat(e)))
                .collect(Collectors.toList());

        return chatFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    private ChatVo getChat(ImChatPo chatPo) {

        switch (IMessageType.getByCode(chatPo.getChatType())) {
            case SINGLE_MESSAGE:
                return getSingleMessageChat(chatPo);
            case GROUP_MESSAGE:
                return getGroupMessageChat(chatPo);
            default:
                return new ChatVo(); // 处理未知类型的对话
        }
    }

    private ChatVo getSingleMessageChat(ImChatPo chatPo) {

        ChatVo chatVo = new ChatVo();

//        BeanUtils.copyProperties(chatPo, chatVo);
//
//        String ownerId = chatVo.getOwnerId();
//
//        String toId = chatVo.getToId();
//
//        ImPrivateMessagePo privateMessageDto = imPrivateMessageMapper.selectLastSingleMessage(ownerId, toId);
//
//        chatVo.setMessageTime(0L);
//
//        if (ObjectUtil.isNotEmpty(privateMessageDto)) {
//
//            chatVo.setMessage(privateMessageDto.getMessageBody());
//
//            chatVo.setMessageContentType(privateMessageDto.getMessageContentType());
//
//            chatVo.setMessageTime(privateMessageDto.getMessageTime());
//        }
//
//        Integer unread = imPrivateMessageMapper.selectReadStatus(toId, ownerId, IMessageReadStatus.UNREAD.code());
//
//        if (ObjectUtil.isNotEmpty(unread)) {
//
//            chatVo.setUnread(unread);
//        }
//
//        String targetUserId = ownerId.equals(toId) ? chatVo.getOwnerId() : chatVo.getToId();
//
//        ImUserDataPo imUserDataPo = imUserFeign.getOne(targetUserId);
//
//        chatVo.setName(imUserDataPo.getName());
//
//        chatVo.setAvatar(imUserDataPo.getAvatar());
//
//        chatVo.setId(imUserDataPo.getUserId());

        return chatVo;
    }

    private ChatVo getGroupMessageChat(ImChatPo e) {

        ChatVo chatVo = new ChatVo();

//        BeanUtils.copyProperties(e, chatVo);
//
//        String ownerId = chatVo.getOwnerId();
//
//        String groupId = chatVo.getToId();
//
////        QueryWrapper<ImGroupMessage> messageQuery = new QueryWrapper<>();
////
////        messageQuery.eq("groupId", groupId);
//
//        ImGroupMessagePo IMGroupMessagePoDto = imGroupMessageMapper.selectLastGroupMessage(ownerId, groupId);
//
//        chatVo.setMessageTime(0L);
//
//        if (ObjectUtil.isNotEmpty(IMGroupMessagePoDto)) {
//
//            chatVo.setMessage(IMGroupMessagePoDto.getMessageBody());
//
//            chatVo.setMessageTime(IMGroupMessagePoDto.getMessageTime());
//        }
//
//        QueryWrapper<ImGroupMessageStatusPo> messageStatusQuery = new QueryWrapper<>();
//
//        messageStatusQuery.eq("group_id", groupId);
//
//        messageStatusQuery.eq("to_id", ownerId);
//
//        messageStatusQuery.eq("read_status", IMessageReadStatus.UNREAD.code());
//
//        List<ImGroupMessageStatusPo> imGroupMessageStatusPoList = imGroupMessageStatusMapper.selectList(messageStatusQuery);
//
//        if (ObjectUtil.isNotEmpty(imGroupMessageStatusPoList)) {
//
//            chatVo.setUnread(imGroupMessageStatusPoList.size());
//        }
//        ImGroupPo imGroupPo = imGroupMapper.selectOne(new QueryWrapper<ImGroupPo>().eq("group_id", groupId));
//
//        chatVo.setName(imGroupPo.getGroupName());
//
//        chatVo.setAvatar(imGroupPo.getAvatar());
//
//        chatVo.setId(imGroupPo.getGroupId());

        return chatVo;
    }

    /**
     * 设置单聊消息已读
     *
     * @param chatDto
     */
    private void saveSingleMessageChat(ChatDto chatDto) {
//        // 参数校验，避免空指针异常
//        if (chatDto == null || chatDto.getFromId() == null || chatDto.getToId() == null) {
//            // 此处建议使用日志记录工具记录异常信息
//            log.warn("chatDto 或相关字段为 null，无法更新消息状态");
//            return;
//        }
//
//        // 构造更新对象，将状态标记为已读
//        ImPrivateMessagePo updateMessage = new ImPrivateMessagePo();
//        updateMessage.setReadStatus(IMessageReadStatus.ALREADY_READ.code());
//
//        // 使用 LambdaUpdateWrapper 进行条件构造，字段名通过方法引用来保证类型安全
//        LambdaUpdateWrapper<ImPrivateMessagePo> updateWrapper = new LambdaUpdateWrapper<>();
//        updateWrapper.eq(ImPrivateMessagePo::getFromId, chatDto.getFromId())
//                .eq(ImPrivateMessagePo::getToId, chatDto.getToId());
//
//        // 执行更新
//        int updatedRows = imPrivateMessageMapper.update(updateMessage, updateWrapper);
//        if (updatedRows == 0) {
//            log.warn("未更新任何记录，fromId: {}, toId: {}", chatDto.getFromId(), chatDto.getToId());
//        } else {
//            log.info("成功更新 {} 条记录，fromId: {}, toId: {}", updatedRows, chatDto.getFromId(), chatDto.getToId());
//        }
    }

    /**
     * 设置群聊消息已读
     *
     * @param chatDto
     */
    private void saveGroupMessageChat(ChatDto chatDto) {
//        // 参数校验
//        if (chatDto == null || chatDto.getFromId() == null || chatDto.getToId() == null) {
//            log.warn("chatDto 或必要字段为 null，无法更新群消息状态");
//            return;
//        }
//
//        // 构造更新对象，将状态标记为已读
//        ImGroupMessageStatusPo updateStatus = new ImGroupMessageStatusPo();
//        updateStatus.setReadStatus(IMessageReadStatus.ALREADY_READ.code());
//
//        // 使用 LambdaUpdateWrapper 构造条件，避免硬编码字段名
//        LambdaUpdateWrapper<ImGroupMessageStatusPo> updateWrapper = new LambdaUpdateWrapper<>();
//        updateWrapper.eq(ImGroupMessageStatusPo::getGroupId, chatDto.getFromId())
//                .eq(ImGroupMessageStatusPo::getToId, chatDto.getToId());
//
//        // 执行更新，并记录更新结果
//        int updatedRows = imGroupMessageStatusMapper.update(updateStatus, updateWrapper);
//        if (updatedRows == 0) {
//            log.warn("未更新任何群消息记录，groupId: {}, toId: {}", chatDto.getFromId(), chatDto.getToId());
//        } else {
//            log.info("成功更新 {} 条群消息记录，groupId: {}, toId: {}", updatedRows, chatDto.getFromId(), chatDto.getToId());
//        }
    }


}
