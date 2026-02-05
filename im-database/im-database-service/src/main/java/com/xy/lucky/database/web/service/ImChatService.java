package com.xy.lucky.database.web.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.lucky.database.api.chat.ImChatDubboService;
import com.xy.lucky.database.web.mapper.ImChatMapper;
import com.xy.lucky.domain.po.ImChatPo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;
import java.util.Objects;


@Slf4j
@DubboService
@RequiredArgsConstructor
public class ImChatService extends ServiceImpl<ImChatMapper, ImChatPo>
        implements ImChatDubboService {

    private final ImChatMapper imChatMapper;

    @Override
    public List<ImChatPo> queryList(String ownerId, Long sequence) {
        return imChatMapper.getChatList(ownerId, sequence);
    }

    @Override
    public ImChatPo queryOne(String ownerId, String toId, Integer chatType) {
        QueryWrapper<ImChatPo> query = new QueryWrapper<>();
        query.eq("owner_id", ownerId)
                .eq("to_id", toId);
        if (Objects.nonNull(chatType)) {
            query.eq("chat_type", chatType);
        }
        return super.getOne(query);
    }

    @Override
    public Boolean creat(ImChatPo chatPo) {
        return super.save(chatPo);
    }

    @Override
    public Boolean modify(ImChatPo chatPo) {
        return super.updateById(chatPo);
    }

    @Override
    public Boolean creatOrModify(ImChatPo chatPo) {
        return super.saveOrUpdate(chatPo);
    }

    @Override
    public Boolean removeOne(String id) {
        return super.removeById(id);
    }


//     
//    public void read(ChatDto chatDto) {
//        switch (IMessageType.getByCode(chatDto.getChatType())) {
//            case SINGLE_MESSAGE:
//                saveSingleMessageChat(chatDto);
//                break;
//            case GROUP_MESSAGE:
//                saveGroupMessageChat(chatDto);
//                break;
//            default:
//                //chatVo = new ChatSetVo(); // 处理未知类型的对话
//        }
//    }
//
//     
//    public ChatVo create(ChatDto chatDto) {
//
//        ChatVo chatVo = new ChatVo();
//
//        QueryWrapper<ImChatPo> chatQuery = new QueryWrapper<>();
//
//        chatQuery.eq("owner_id", chatDto.getFromId())
//                .eq("to_id", chatDto.getToId())
//                .eq("chat_type", chatDto.getChatType());
//
//        ImChatPo imChatPO = imChatMapper.selectOne(chatQuery);
//
//        if (ObjectUtil.isEmpty(imChatPO)) {
//
//            imChatPO = new ImChatPo();
//
//            String id = UUID.randomUUID().toString();
//
//            imChatPO.setChatId(id)
//                    .setOwnerId(chatDto.getFromId())
//                    .setToId(chatDto.getToId())
//                    .setIsMute(IMStatus.NO.getCode())
//                    .setIsTop(IMStatus.NO.getCode())
//                    .setChatType(IMessageType.SINGLE_MESSAGE.getCode());
//
//            imChatMapper.insert(imChatPO);
//        }
//
//        BeanUtils.copyProperties(imChatPO, chatVo);
//
//        // 创建单聊会话
//        if (chatDto.getChatType().equals(IMessageType.SINGLE_MESSAGE.getCode())) {
//
//            ImUserDataPo imUserDataPo = imUserDataMapper.selectOne(new QueryWrapper<ImUserDataPo>().eq("user_id", chatDto.getToId()));
//
//            chatVo.setName(imUserDataPo.getName());
//
//            chatVo.setAvatar(imUserDataPo.getAvatar());
//
//            chatVo.setId(imUserDataPo.getUserId());
//        }
//
//        // 创建群聊会话
//        if (chatDto.getChatType().equals(IMessageType.GROUP_MESSAGE.getCode())) {
//
//            ImGroupPo imGroupPo = imGroupMapper.selectOne(new QueryWrapper<ImGroupPo>().eq("group_id", chatDto.getToId()));
//
//            chatVo.setName(imGroupPo.getGroupName());
//
//            chatVo.setAvatar(imGroupPo.getAvatar());
//
//            chatVo.setId(imGroupPo.getGroupId());
//        }
//
//        return chatVo;
//    }
//
//     
//    public ChatVo one(String fromId, String toId) {
//        QueryWrapper<ImChatPo> chatQuery = new QueryWrapper<>();
//
//        chatQuery.eq("owner_id", fromId)
//                .eq("to_id", toId);
//
//        ImChatPo imChatPO = imChatMapper.selectOne(chatQuery);
//
//        return getChat(imChatPO);
//    }

//     
//    public List<ChatVo> list(ChatDto chatDto) {
//
//        QueryWrapper<ImChatPo> chatQuery = new QueryWrapper<>();
//
//        chatQuery.eq("owner_id", chatDto.getFromId());
//
//        chatQuery.gt("sequence", chatDto.getSequence());
//
//        List<ImChatPo> imChatPos = imChatMapper.selectList(chatQuery);
//
//        List<CompletableFuture<ChatVo>> chatFutures = imChatPos.stream()
//                .map(e -> CompletableFuture.supplyAsync(() -> getChat(e)))
//                .collect(Collectors.toList());
//
//        return chatFutures.stream()
//                .map(CompletableFuture::join)
//                .collect(Collectors.toList());
//    }
//
//    private ChatVo getChat(ImChatPo e) {
//
//        switch (IMessageType.getByCode(e.getChatType())) {
//            case SINGLE_MESSAGE:
//                return getSingleMessageChat(e);
//            case GROUP_MESSAGE:
//                return getGroupMessageChat(e);
//            default:
//                return new ChatVo(); // 处理未知类型的对话
//        }
//    }
//
//    private ChatVo getSingleMessageChat(ImChatPo e) {
//
//        ChatVo chatVo = new ChatVo();
//
//        BeanUtils.copyProperties(e, chatVo);
//
//        String ownerId = chatVo.getOwnerId();
//
//        String toId = chatVo.getToId();
//
//        ImSingleMessagePo singleMessageDto = imSingleMessageMapper.selectLastSingleMessage(ownerId, toId);
//
//        chatVo.setMessageTime(0L);
//
//        if (ObjectUtil.isNotEmpty(singleMessageDto)) {
//
//            chatVo.setMessage(singleMessageDto.getMessageBody());
//
//            chatVo.setMessageContentType(singleMessageDto.getMessageContentType());
//
//            chatVo.setMessageTime(singleMessageDto.getMessageTime());
//        }
//
//        Integer unread = imSingleMessageMapper.selectReadStatus(toId, ownerId, IMessageReadStatus.UNREAD.getCode());
//
//        if (ObjectUtil.isNotEmpty(unread)) {
//
//            chatVo.setUnread(unread);
//        }
//
//        String targetUserId = ownerId.equals(toId) ? chatVo.getOwnerId() : chatVo.getToId();
//
//        ImUserDataPo imUserDataPo = imUserDataMapper.selectOne(new QueryWrapper<ImUserDataPo>().eq("user_id", targetUserId));
//
//        chatVo.setName(imUserDataPo.getName());
//
//        chatVo.setAvatar(imUserDataPo.getAvatar());
//
//        chatVo.setId(imUserDataPo.getUserId());
//
//        return chatVo;
//    }
//
//    private ChatVo getGroupMessageChat(ImChatPo e) {
//
//        ChatVo chatVo = new ChatVo();
//
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
//        messageStatusQuery.eq("read_status", IMessageReadStatus.UNREAD.getCode());
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
//
//        return chatVo;
//    }
//
//    /**
//     * 设置单聊消息已读
//     *
//     * @param chatDto
//     */
//    private void saveSingleMessageChat(ChatDto chatDto) {
//        // 参数校验，避免空指针异常
//        if (chatDto == null || chatDto.getFromId() == null || chatDto.getToId() == null) {
//            // 此处建议使用日志记录工具记录异常信息
//            log.warn("chatDto 或相关字段为 null，无法更新消息状态");
//            return;
//        }
//
//        // 构造更新对象，将状态标记为已读
//        ImSingleMessagePo updateMessage = new ImSingleMessagePo();
//        updateMessage.setReadStatus(IMessageReadStatus.ALREADY_READ.getCode());
//
//        // 使用 LambdaUpdateWrapper 进行条件构造，字段名通过方法引用来保证类型安全
//        LambdaUpdateWrapper<ImSingleMessagePo> updateWrapper = new LambdaUpdateWrapper<>();
//        updateWrapper.eq(ImSingleMessagePo::getFromId, chatDto.getFromId())
//                .eq(ImSingleMessagePo::getToId, chatDto.getToId());
//
//        // 执行更新
//        int updatedRows = imSingleMessageMapper.update(updateMessage, updateWrapper);
//        if (updatedRows == 0) {
//            log.warn("未更新任何记录，fromId: {}, toId: {}", chatDto.getFromId(), chatDto.getToId());
//        } else {
//            log.info("成功更新 {} 条记录，fromId: {}, toId: {}", updatedRows, chatDto.getFromId(), chatDto.getToId());
//        }
//    }
//
//    /**
//     * 设置群聊消息已读
//     *
//     * @param chatDto
//     */
//    private void saveGroupMessageChat(ChatDto chatDto) {
//        // 参数校验
//        if (chatDto == null || chatDto.getFromId() == null || chatDto.getToId() == null) {
//            log.warn("chatDto 或必要字段为 null，无法更新群消息状态");
//            return;
//        }
//
//        // 构造更新对象，将状态标记为已读
//        ImGroupMessageStatusPo updateStatus = new ImGroupMessageStatusPo();
//        updateStatus.setReadStatus(IMessageReadStatus.ALREADY_READ.getCode());
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
//    }

}
