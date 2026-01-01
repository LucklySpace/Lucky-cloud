package com.xy.lucky.ai.service.impl;

import com.xy.lucky.ai.domain.po.ChatMessagePo;
import com.xy.lucky.ai.domain.po.ChatSessionPo;
import com.xy.lucky.ai.domain.vo.ChatMessageVo;
import com.xy.lucky.ai.domain.vo.ChatSessionVo;
import com.xy.lucky.ai.mapper.ChatMessageMapper;
import com.xy.lucky.ai.mapper.ChatSessionMapper;
import com.xy.lucky.ai.repository.ChatMessageRepository;
import com.xy.lucky.ai.repository.ChatSessionRepository;
import com.xy.lucky.ai.service.ChatSessionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatSessionServiceImpl implements ChatSessionService {

    @Resource
    private ChatSessionRepository chatSessionRepository;

    @Resource
    private ChatMessageRepository chatMessageRepository;

    @Resource
    private ChatSessionMapper chatSessionMapper;

    @Resource
    private ChatMessageMapper chatMessageMapper;

    @Override
    public List<ChatSessionVo> listByUser(String userId) {
        return chatSessionRepository.findByUserId(userId).stream()
                .map(chatSessionMapper::toVo)
                .collect(Collectors.toList());
    }

    @Override
    public ChatSessionVo getWithMessages(String sessionId) {
        ChatSessionPo session = chatSessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return null;
        }
        List<ChatMessagePo> messageList = chatMessageRepository.findBySession_IdOrderByCreatedAtDesc(sessionId);
        List<ChatMessageVo> messages = messageList.stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(chatMessageMapper::toVo)
                .collect(Collectors.toList());
        ChatSessionVo vo = chatSessionMapper.toVo(session);
        vo.setMessages(messages);
        return vo;
    }

    @Override
    public ChatSessionVo create(String userId) {
        ChatSessionPo session = new ChatSessionPo();
        session.setId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setCreatedAt(LocalDateTime.now());
        session.setStatus("active");
        session.setMessageCount(0);
        chatSessionRepository.save(session);
        return chatSessionMapper.toVo(session);
    }

    @Override
    public void delete(String sessionId) {
        chatMessageRepository.deleteBySession_Id(sessionId);
        chatSessionRepository.deleteById(sessionId);
    }

}
