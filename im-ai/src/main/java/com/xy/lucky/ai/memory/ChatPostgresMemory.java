package com.xy.lucky.ai.memory;

import com.xy.lucky.ai.domain.po.ChatMessagePo;
import com.xy.lucky.ai.repository.ChatMessageRepository;
import com.xy.lucky.ai.repository.ChatSessionRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class ChatPostgresMemory implements ChatMemory {

    @Resource
    private ChatMessageRepository chatMessageRepository;

    @Resource
    private ChatSessionRepository chatSessionRepository;

    @Override
    public void add(String conversationId, Message message) {

        chatSessionRepository.findById(conversationId).ifPresentOrElse(session -> {

            String[] strs = message.getText().split("</think>");

            String text = strs.length == 2 ? strs[1] : strs[0];

            String messageId = UUID.randomUUID().toString();

            ChatMessagePo ent = new ChatMessagePo()
                    .setId(messageId)
                    .setContent(text)
                    .setType(message.getMessageType().getValue())
                    .setSession(session);

            chatMessageRepository.save(ent);
            session.setMessageCount((session.getMessageCount() == null ? 0 : session.getMessageCount()) + 1);
            session.setLastMessageAt(java.time.LocalDateTime.now());
            chatSessionRepository.save(session);

            log.info("[add] 会话 {} 存储消息到 Postgres。消息内容:{}", conversationId, ent);
        }, () -> {

            log.info("[add] 会话 {} 不存在", conversationId);
        });

    }

    @Override
    public void add(String conversationId, List<Message> messages) {

        chatSessionRepository.findById(conversationId).ifPresentOrElse(session -> {

            List<ChatMessagePo> listIn = new ArrayList<>();

                    for (Message msg : messages) {

                        String[] strs = msg.getText().split("</think>");

                        String text = strs.length == 2 ? strs[1] : strs[0];

                        String messageId = UUID.randomUUID().toString();

                        ChatMessagePo ent = new ChatMessagePo()
                                .setId(messageId)
                                .setContent(text)
                                .setType(msg.getMessageType().getValue())
                                .setSession(session);

                        listIn.add(ent);
                    }

                    chatMessageRepository.saveAll(listIn);
            session.setMessageCount((session.getMessageCount() == null ? 0 : session.getMessageCount()) + listIn.size());
            session.setLastMessageAt(LocalDateTime.now());
                    chatSessionRepository.save(session);

                    log.info("[add] 会话 {} 存储 {} 条消息到 Postgres。消息内容:{}", conversationId, listIn.size(), listIn);
                }, () -> {

                    log.info("[add] 会话 {} 不存在", conversationId);
                }
        );


    }

    @Override
    public List<Message> get(String conversationId, int lastN) {

        if (chatSessionRepository.findById(conversationId).isEmpty()) {

            log.debug("[get] 会话 {} 无历史记录。", conversationId);

            return Collections.emptyList();
        }

        // 根据会话id查询最近 lastN 条消息（按时间倒序）
        List<ChatMessagePo> messageList = chatMessageRepository.findBySession_Id(
                conversationId, PageRequest.of(0, Math.max(lastN, 1), org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")));

        List<Message> listOut = new ArrayList<>();

        if (!messageList.isEmpty()) {

            for (ChatMessagePo chatMessagePo : messageList) {

                if (MessageType.USER.getValue().equals(chatMessagePo.getType())) {

                    listOut.add(new UserMessage(chatMessagePo.getContent()));

                } else if (MessageType.ASSISTANT.getValue().equals(chatMessagePo.getType())) {

                    listOut.add(new AssistantMessage(chatMessagePo.getContent()));

                } else if (MessageType.SYSTEM.getValue().equals(chatMessagePo.getType())) {

                    listOut.add(new SystemMessage(chatMessagePo.getContent()));
                }
            }
        }

        // 转为时间正序（从老到新），便于模型理解上下文
        Collections.reverse(listOut);

        log.info("[get] 会话 {} 获取最近 {} 条消息，实际返回 {} 条。", conversationId, lastN, listOut.size());

        return listOut;
    }

    @Override
    public void clear(String conversationId) {

        // 删除消息
        chatMessageRepository.deleteBySession_Id(conversationId);

        // 删除会话
        chatSessionRepository.deleteById(conversationId);

        log.info("[delete] 会话 {} 删除。", conversationId);
    }
}
