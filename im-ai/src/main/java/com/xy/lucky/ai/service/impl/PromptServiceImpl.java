package com.xy.lucky.ai.service.impl;

import com.xy.lucky.ai.domain.po.ChatPromptPo;
import com.xy.lucky.ai.domain.vo.ChatPromptVo;
import com.xy.lucky.ai.mapper.ChatPromptMapper;
import com.xy.lucky.ai.repository.ChatPromptRepository;
import com.xy.lucky.ai.service.PromptService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class PromptServiceImpl implements PromptService {

    @Resource
    private ChatPromptRepository chatPromptRepository;

    @Resource
    private ChatPromptMapper chatPromptMapper;

    @Override
    public List<ChatPromptVo> list() {
        return chatPromptRepository.findAll().stream()
                .map(chatPromptMapper::toVo)
                .collect(Collectors.toList());
    }

    @Override
    public boolean add(ChatPromptVo chatPrompt) {
        if (chatPrompt == null || !containsTopicPlaceholder(chatPrompt.getPrompt())) {
            return false;
        }
        ChatPromptPo po = chatPromptMapper.toPo(chatPrompt);
        po.setId(UUID.randomUUID().toString());
        chatPromptRepository.save(po);
        return true;
    }

    @Override
    public boolean update(ChatPromptVo chatPrompt) {
        if (chatPrompt == null || chatPrompt.getId() == null) {
            return false;
        }
        if (!containsTopicPlaceholder(chatPrompt.getPrompt())) {
            return false;
        }
        return chatPromptRepository.findById(chatPrompt.getId()).map(existing -> {
            existing.setName(chatPrompt.getName())
                    .setPrompt(chatPrompt.getPrompt())
                    .setDescription(chatPrompt.getDescription());
            chatPromptRepository.save(existing);
            return true;
        }).orElse(false);
    }

    @Override
    public boolean delete(String id) {
        if (id == null) {
            return false;
        }
        return chatPromptRepository.findById(id).map(prompt -> {
            chatPromptRepository.deleteById(id);
            return true;
        }).orElse(false);
    }

    public boolean containsTopicPlaceholder(String text) {
        return text != null && text.contains("{topic}");
    }

}
