package com.xy.ai.controller;

import com.xy.ai.domain.ChatSession;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * SessionController 提供会话管理接口：
 * - 列出所有会话
 * - 获取会话详情
 * - 创建新会话
 * - 删除会话
 */
@Slf4j
@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
public class SessionController {

    // 简化示例：内存存储会话列表，生产请替换为数据库或持久化存储
    private final Map<String, String> sessions = new LinkedHashMap<>();

    /**
     * 列出所有会话的基本信息（ID 和名称）
     *
     * @return 会话列表
     */
    @GetMapping("/list")
    public ResponseEntity<List<Map<String, String>>> listSessions() {
        List<Map<String, String>> result = new ArrayList<>();
        sessions.forEach((id, name) -> result.add(Map.of("sessionId", id, "name", name)));
        log.info("[listSessions] 返回 {} 个会话", result.size());
        return ResponseEntity.ok(result);
    }

    /**
     * 获取指定会话的详细信息（包括名称）
     *
     * @param sessionId 会话ID
     * @return 会话信息
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, String>> getSession(
            @PathVariable String sessionId) {
        String name = sessions.get(sessionId);
        if (name == null) {
            log.warn("[getSession] 会话 {} 不存在", sessionId);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("sessionId", sessionId, "name", name));
    }

    /**
     * 创建一个新会话，返回新会话ID
     *
     * @param userId 用户id
     * @return 创建的会话ID
     */
    @Operation(summary = "创建会话")
    @GetMapping("/create")
    public ResponseEntity<ChatSession> createSession(@RequestParam String userId) {
        Assert.notNull(userId, "userId不能为空");
        String sessionId = UUID.randomUUID().toString();
        log.info("[createSession] 用户: {} 创建会话: {}", userId, sessionId);
        ChatSession sessionDto = new ChatSession()
                .setId(sessionId)
                .setUserId(userId);
        return ResponseEntity.ok(sessionDto);
    }

    /**
     * 删除指定会话
     *
     * @param sessionId 会话ID
     * @return 无内容
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteSession(
            @RequestParam String sessionId) {
        sessions.remove(sessionId);
        log.info("[deleteSession] 已删除会话 {}", sessionId);
        return ResponseEntity.noContent().build();
    }
}
