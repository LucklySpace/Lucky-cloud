//package com.xy.ai.memory;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.chat.memory.ChatMemory;
//import org.springframework.ai.chat.messages.*;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Component;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//
//
///**
// * ChatRedisMemory 是基于 Redis 的聊天上下文记忆实现。
// * 它负责将用户与 AI 的对话内容缓存到 Redis，支持添加、获取和清除历史。
// */
//@Slf4j
//@Component
//public class ChatRedisMemory implements ChatMemory {
//
//    private static final String KEY_PREFIX = "chat:history:";
//
//    private final RedisTemplate<String, Object> redisTemplate;
//
//    public ChatRedisMemory(RedisTemplate<String, Object> redisTemplate) {
//        this.redisTemplate = redisTemplate;
//    }
//
//    /**
//     * 将消息列表添加到指定会话ID的 Redis 列表中，并设置过期时间。
//     *
//     * @param conversationId 会话ID
//     * @param messages       要添加的消息列表
//     */
//    @Override
//    public void add(String conversationId, List<Message> messages) {
//        String key = KEY_PREFIX + conversationId;
////        List<AiSession> listIn = new ArrayList<>();
////
////        for (Message msg : messages) {
////            String[] strs = msg.getText().split("</think>");
////            String text = strs.length == 2 ? strs[1] : strs[0];
////
////            AiSession ent = new AiSession();
////            ent.setSessionId(conversationId);
////            ent.setType(msg.getMessageType().getValue());
////            ent.setText(text);
////            listIn.add(ent);
////        }
//
////        redisTemplate.opsForList().rightPushAll(key, listIn.toArray());
////        redisTemplate.expire(key, 30, TimeUnit.MINUTES);
////        log.debug("[add] 会话 {} 存储 {} 条消息到 Redis。", conversationId, listIn.size());
//    }
//
//    /**
//     * 从指定会话中获取最后 N 条消息。
//     *
//     * @param conversationId 会话ID
//     * @param lastN          获取数量（倒数）
//     * @return 消息列表
//     */
//    @Override
//    public List<Message> get(String conversationId, int lastN) {
//        String key = KEY_PREFIX + conversationId;
//        Long size = redisTemplate.opsForList().size(key);
//        if (size == null || size == 0) {
//            log.debug("[get] 会话 {} 无历史记录。", conversationId);
//            return Collections.emptyList();
//        }
//
//        int start = Math.max(0, (int) (size - lastN));
//        List<Object> listTmp = redisTemplate.opsForList().range(key, start, -1);
//        List<Message> listOut = new ArrayList<>();
//        ObjectMapper objectMapper = new ObjectMapper();
//
//        if (listTmp != null) {
//            for (Object obj : listTmp) {
//                AiSession aiSession = objectMapper.convertValue(obj, AiSession.class);
//                if (MessageType.USER.getValue().equals(aiSession.getType())) {
//                    listOut.add(new UserMessage(aiSession.getText()));
//                } else if (MessageType.ASSISTANT.getValue().equals(aiSession.getType())) {
//                    listOut.add(new AssistantMessage(aiSession.getText()));
//                } else if (MessageType.SYSTEM.getValue().equals(aiSession.getType())) {
//                    listOut.add(new SystemMessage(aiSession.getText()));
//                }
//            }
//        }
//
//        log.debug("[get] 会话 {} 获取最近 {} 条消息，实际返回 {} 条。", conversationId, lastN, listOut.size());
//        return listOut;
//    }
//
//    /**
//     * 清除指定会话ID的所有缓存记录。
//     *
//     * @param conversationId 会话ID
//     */
//    @Override
//    public void clear(String conversationId) {
//        redisTemplate.delete(KEY_PREFIX + conversationId);
//        log.debug("[clear] 会话 {} 的记录已清除。", conversationId);
//    }
//}