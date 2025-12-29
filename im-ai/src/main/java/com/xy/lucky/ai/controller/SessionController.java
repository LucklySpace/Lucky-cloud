package com.xy.lucky.ai.controller;

import com.xy.lucky.ai.domain.vo.ChatSessionVo;
import com.xy.lucky.ai.service.ChatSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SessionController 提供会话管理接口：
 * - 列出所有会话
 * - 获取会话详情
 * - 创建新会话
 * - 删除会话
 */
@Slf4j
@RestController
@RequestMapping({"/api/session", "/api/{version}/ai/session"})
@RequiredArgsConstructor
@Tag(name = "session", description = "会话管理")
public class SessionController {

    private final ChatSessionService chatSessionService;

    /**
     * 列出所有会话的基本信息（ID 和名称）
     *
     * @return 会话列表
     */
    @GetMapping("/list")
    @Operation(summary = "列出所有会话")
    public List<ChatSessionVo> listSessions(@RequestParam("userId") String userId) {
        List<ChatSessionVo> sessions = chatSessionService.listByUser(userId);
        log.info("[listSessions] 用户 {} 返回 {} 个会话", userId, sessions.size());
        return sessions;
    }

    /**
     * 获取指定会话的详细信息（包括名称）
     *
     * @param sessionId 会话ID
     * @return 会话信息
     */
    @GetMapping("/{sessionId}")
    @Operation(summary = "获取会话详情")
    public ChatSessionVo getSession(@PathVariable("sessionId") String sessionId) {
        log.info("[getSession] 获取会话 {} 的详情", sessionId);
        return chatSessionService.getWithMessages(sessionId);
    }

    /**
     * 创建一个新会话，返回新会话ID
     *
     * @param userId 用户id
     * @return 创建的会话ID
     */
    @Operation(summary = "创建会话")
    @GetMapping("/create")
    public ChatSessionVo createSession(@RequestParam("userId") String userId) {
        Assert.notNull(userId, "userId不能为空");
        ChatSessionVo vo = chatSessionService.create(userId);
        log.info("[createSession] 用户: {} 创建会话: {}", userId, vo.getId());
        return vo;
    }

    /**
     * 删除指定会话
     *
     * @param sessionId 会话ID
     * @return 无内容
     */
    @DeleteMapping("/delete")
    @Operation(summary = "删除会话")
    public void deleteSession(
            @RequestParam String sessionId) {
        chatSessionService.delete(sessionId);
        log.info("[deleteSession] 已删除会话 {}", sessionId);
    }
}
