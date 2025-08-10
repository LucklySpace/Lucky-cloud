package com.xy.ai.service.impl;

import com.xy.ai.domain.ChatPrompt;
import com.xy.ai.repository.ChatPromptRepository;
import com.xy.ai.service.PromptService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class PromptServiceImpl implements PromptService {

    @Resource
    private ChatPromptRepository chatPromptRepository;

    /**
     * 列出所有 Prompt 配置
     *
     * @return Prompt 列表
     */
    @Override
    public List<ChatPrompt> list() {
        return chatPromptRepository.findAll();
    }

    /**
     * 新增一个 Prompt 配置，只有当 prompt 文本中包含 "{topic}" 占位符时才允许添加
     *
     * @param chatPrompt 待添加的 Prompt 实体
     * @return 添加成功返回 true，否则返回 false
     */
    @Override
    public boolean add(ChatPrompt chatPrompt) {
        if (chatPrompt == null || !containsTopicPlaceholder(chatPrompt.getPrompt())) {
            log.error("提示词添加失败：Prompt 为空或不包含 {topic} 占位符");
            return false;
        }
        String id = UUID.randomUUID().toString();
        chatPrompt.setId(id);
        chatPromptRepository.save(chatPrompt);
        log.info("提示词添加成功，id={} prompt={}", id, chatPrompt.getPrompt());
        return true;
    }

    /**
     * 更新一个已有的 Prompt 配置，只有当 prompt 文本中包含 "{topic}" 占位符且记录存在时才允许更新
     *
     * @param chatPrompt 包含 id 与新内容的 Prompt 实体
     * @return 更新成功返回 true，否则返回 false
     */
    @Override
    public boolean update(ChatPrompt chatPrompt) {
        if (chatPrompt == null || chatPrompt.getId() == null) {
            log.error("提示词更新失败：参数为空或 id 为空");
            return false;
        }
        if (!containsTopicPlaceholder(chatPrompt.getPrompt())) {
            log.error("提示词更新失败：Prompt 不包含 {topic} 占位符");
            return false;
        }
        return chatPromptRepository.findById(chatPrompt.getId()).map(existing -> {
            // 仅复制可更新字段
            existing.setName(chatPrompt.getName())
                    .setPrompt(chatPrompt.getPrompt())
                    .setDescription(chatPrompt.getDescription());
            chatPromptRepository.save(existing);
            log.info("提示词更新成功，id={}", chatPrompt.getId());
            return true;
        }).orElseGet(() -> {
            log.error("提示词更新失败：id={} 不存在", chatPrompt.getId());
            return false;
        });
    }

    /**
     * 删除一个 Prompt 配置
     *
     * @param id 要删除的 Prompt ID
     * @return 删除成功返回 true，否则返回 false
     */
    @Override
    public boolean delete(String id) {
        if (id == null) {
            log.error("提示词删除失败：id 为空");
            return false;
        }
        return chatPromptRepository.findById(id).map(prompt -> {
            chatPromptRepository.deleteById(id);
            log.info("提示词删除成功，id={}", id);
            return true;
        }).orElseGet(() -> {
            log.error("提示词删除失败：id={} 不存在", id);
            return false;
        });
    }

    /**
     * 判断字符串中是否包含 "{topic}" 占位符
     *
     * @param text 待检查文本
     * @return 包含则返回 true，否则 false
     */
    public boolean containsTopicPlaceholder(String text) {
        return text != null && text.contains("{topic}");
    }
}