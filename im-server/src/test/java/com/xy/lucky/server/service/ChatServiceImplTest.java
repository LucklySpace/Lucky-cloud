package com.xy.lucky.server.service;

import com.xy.lucky.core.enums.IMessageType;
import com.xy.lucky.domain.dto.ChatDto;
import com.xy.lucky.domain.po.ImChatPo;
import com.xy.lucky.domain.po.ImGroupPo;
import com.xy.lucky.domain.po.ImSingleMessagePo;
import com.xy.lucky.domain.po.ImUserDataPo;
import com.xy.lucky.domain.vo.ChatVo;
import com.xy.lucky.dubbo.web.api.database.chat.ImChatDubboService;
import com.xy.lucky.dubbo.web.api.database.group.ImGroupDubboService;
import com.xy.lucky.dubbo.web.api.database.message.ImGroupMessageDubboService;
import com.xy.lucky.dubbo.web.api.database.message.ImSingleMessageDubboService;
import com.xy.lucky.dubbo.web.api.database.user.ImUserDataDubboService;
import com.xy.lucky.server.exception.ChatException;
import com.xy.lucky.server.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonReactiveClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * ChatServiceImpl 单元测试类
 * <p>
 * 测试会话服务的核心功能，包括会话创建、查询、已读状态更新等操作。
 * 使用 Mockito 模拟 Dubbo 服务和 Redisson 客户端。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("会话服务测试")
class ChatServiceImplTest {

    @Mock
    private ImChatDubboService imChatDubboService;

    @Mock
    private ImUserDataDubboService imUserDataDubboService;

    @Mock
    private ImGroupDubboService imGroupDubboService;

    @Mock
    private ImSingleMessageDubboService imSingleMessageDubboService;

    @Mock
    private ImGroupMessageDubboService imGroupMessageDubboService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RedissonReactiveClient reactiveClient;

    @Mock
    private RLockReactive lockReactive;

    @InjectMocks
    private ChatServiceImpl chatService;

    /**
     * 每个测试方法执行前的初始化操作
     * 配置 Redisson 分布式锁的模拟行为
     */
    @BeforeEach
    void setUp() {
        // 模拟 Redisson 响应式客户端和分布式锁
        lenient().when(redissonClient.reactive()).thenReturn(reactiveClient);
        lenient().when(reactiveClient.getLock(anyString())).thenReturn(lockReactive);
        lenient().when(lockReactive.tryLock(anyLong(), anyLong(), any())).thenReturn(Mono.just(true));
        lenient().when(lockReactive.isHeldByThread(anyLong())).thenReturn(Mono.just(true));
        lenient().when(lockReactive.unlock()).thenReturn(Mono.empty());
    }

    // ==================== read 方法测试 ====================

    @Nested
    @DisplayName("read 方法测试")
    class ReadMethodTests {

        @Test
        @DisplayName("当ChatDto参数为null时_read方法应抛出参数错误异常")
        void read_WithNullChatDto_ShouldThrowChatException() {
            // 执行测试并验证
            StepVerifier.create(chatService.read(null))
                    .expectError(ChatException.class)
                    .verify();
        }

        @Test
        @DisplayName("当ChatDto缺少必要字段时_read方法应抛出参数错误异常")
        void read_WithMissingRequiredFields_ShouldThrowChatException() {
            // 准备测试数据 - 缺少 toId
            ChatDto chatDto = new ChatDto();
            chatDto.setFromId("user1");
            chatDto.setChatType(IMessageType.SINGLE_MESSAGE.getCode());

            // 执行测试并验证
            StepVerifier.create(chatService.read(chatDto))
                    .expectError(ChatException.class)
                    .verify();
        }

        @Test
        @DisplayName("当ChatDto包含不支持的消息类型时_read方法应抛出异常")
        void read_WithUnsupportedChatType_ShouldThrowChatException() {
            // 准备测试数据 - 不支持的消息类型
            ChatDto chatDto = new ChatDto();
            chatDto.setFromId("user1");
            chatDto.setToId("user2");
            chatDto.setChatType(999); // 不支持的类型

            // 执行测试并验证
            StepVerifier.create(chatService.read(chatDto))
                    .expectError(ChatException.class)
                    .verify();
        }

        @Test
        @DisplayName("当单聊消息已读状态更新成功时_read方法应正常完成")
        void read_WithValidSingleMessageDto_ShouldCompleteSuccessfully() {
            // 准备测试数据
            ChatDto chatDto = new ChatDto();
            chatDto.setFromId("user1");
            chatDto.setToId("user2");
            chatDto.setChatType(IMessageType.SINGLE_MESSAGE.getCode());

            // 模拟 Dubbo 服务
            given(imSingleMessageDubboService.modify(any(ImSingleMessagePo.class))).willReturn(true);

            // 执行测试并验证
            StepVerifier.create(chatService.read(chatDto))
                    .verifyComplete();
        }

        @Test
        @DisplayName("当无法获取分布式锁时_read方法应抛出锁获取失败异常")
        void read_WhenLockAcquisitionFails_ShouldThrowLockError() {
            // 准备测试数据
            ChatDto chatDto = new ChatDto();
            chatDto.setFromId("user1");
            chatDto.setToId("user2");
            chatDto.setChatType(IMessageType.SINGLE_MESSAGE.getCode());

            // 模拟锁获取失败
            when(lockReactive.tryLock(anyLong(), anyLong(), any())).thenReturn(Mono.just(false));

            // 执行测试并验证
            StepVerifier.create(chatService.read(chatDto))
                    .expectErrorMatches(throwable ->
                            throwable.getMessage().contains("无法获取锁"))
                    .verify();
        }
    }

    // ==================== create 方法测试 ====================

    @Nested
    @DisplayName("create 方法测试")
    class CreateMethodTests {

        @Test
        @DisplayName("当ChatDto参数为null时_create方法应抛出参数错误异常")
        void create_WithNullChatDto_ShouldThrowChatException() {
            // 执行测试并验证
            StepVerifier.create(chatService.create(null))
                    .expectError(ChatException.class)
                    .verify();
        }

        @Test
        @DisplayName("当会话已存在时_create方法应直接返回现有会话的ChatVo")
        void create_WhenChatExists_ShouldReturnExistingChatVo() {
            // 准备测试数据
            ChatDto chatDto = new ChatDto();
            chatDto.setFromId("user1");
            chatDto.setToId("user2");
            chatDto.setChatType(IMessageType.SINGLE_MESSAGE.getCode());

            ImChatPo existingChat = new ImChatPo();
            existingChat.setChatId("chat123");
            existingChat.setOwnerId("user1");
            existingChat.setToId("user2");
            existingChat.setChatType(IMessageType.SINGLE_MESSAGE.getCode());

            ImUserDataPo mockUser = new ImUserDataPo();
            mockUser.setUserId("user2");
            mockUser.setName("Test User");

            // 模拟 Dubbo 服务 - 会话已存在
            given(imChatDubboService.queryOne(anyString(), anyString(), anyInt())).willReturn(existingChat);
            given(imUserDataDubboService.queryOne("user2")).willReturn(mockUser);

            // 执行测试并验证
            StepVerifier.create(chatService.create(chatDto))
                    .expectNextMatches(chatVo ->
                            chatVo.getChatId().equals("chat123"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("当创建群聊会话时_create方法应正确获取群组信息")
        void create_WithGroupChatType_ShouldFetchGroupInfo() {
            // 准备测试数据
            ChatDto chatDto = new ChatDto();
            chatDto.setFromId("user1");
            chatDto.setToId("group123");
            chatDto.setChatType(IMessageType.GROUP_MESSAGE.getCode());

            ImChatPo existingChat = new ImChatPo();
            existingChat.setChatId("chat456");
            existingChat.setOwnerId("user1");
            existingChat.setToId("group123");
            existingChat.setChatType(IMessageType.GROUP_MESSAGE.getCode());

            ImGroupPo mockGroup = new ImGroupPo();
            mockGroup.setGroupId("group123");
            mockGroup.setGroupName("Test Group");
            mockGroup.setAvatar("group_avatar.jpg");

            // 模拟 Dubbo 服务
            given(imChatDubboService.queryOne(anyString(), anyString(), anyInt())).willReturn(existingChat);
            given(imGroupDubboService.queryOne("group123")).willReturn(mockGroup);

            // 执行测试并验证
            StepVerifier.create(chatService.create(chatDto))
                    .expectNextMatches(chatVo ->
                            chatVo.getName().equals("Test Group"))
                    .verifyComplete();
        }
    }

    // ==================== one 方法测试 ====================

    @Nested
    @DisplayName("one 方法测试")
    class OneMethodTests {

        @Test
        @DisplayName("当ownerId为null时_one方法应抛出参数错误异常")
        void one_WithNullOwnerId_ShouldThrowChatException() {
            // 执行测试并验证
            StepVerifier.create(chatService.one(null, "user2"))
                    .expectError(ChatException.class)
                    .verify();
        }

        @Test
        @DisplayName("当toId为null时_one方法应抛出参数错误异常")
        void one_WithNullToId_ShouldThrowChatException() {
            // 执行测试并验证
            StepVerifier.create(chatService.one("user1", null))
                    .expectError(ChatException.class)
                    .verify();
        }

        @Test
        @DisplayName("当会话存在时_one方法应返回包含消息信息的ChatVo")
        void one_WithExistingChat_ShouldReturnChatVoWithMessageInfo() {
            // 准备测试数据
            String ownerId = "user1";
            String toId = "user2";

            ImChatPo chatPo = new ImChatPo();
            chatPo.setChatId("chat123");
            chatPo.setOwnerId(ownerId);
            chatPo.setToId(toId);
            chatPo.setChatType(IMessageType.SINGLE_MESSAGE.getCode());

            ImSingleMessagePo lastMessage = new ImSingleMessagePo();
            lastMessage.setMessageId("msg123");
            lastMessage.setMessageBody("Hello");
            lastMessage.setMessageTime(System.currentTimeMillis());

            ImUserDataPo userData = new ImUserDataPo();
            userData.setUserId(toId);
            userData.setName("Test User");

            // 模拟 Dubbo 服务
            given(imChatDubboService.queryOne(ownerId, toId, null)).willReturn(chatPo);
            given(imSingleMessageDubboService.queryLast(ownerId, toId)).willReturn(lastMessage);
            given(imSingleMessageDubboService.queryReadStatus(anyString(), anyString(), anyInt())).willReturn(5);
            given(imUserDataDubboService.queryOne(toId)).willReturn(userData);

            // 执行测试并验证
            StepVerifier.create(chatService.one(ownerId, toId))
                    .expectNextMatches(chatVo ->
                            chatVo.getChatId().equals("chat123") &&
                                    chatVo.getUnread() == 5)
                    .verifyComplete();
        }

        // ==================== list 方法测试 ====================

        @Nested
        @DisplayName("list 方法测试")
        class ListMethodTests {

            @Test
            @DisplayName("当ChatDto参数为null时_list方法应抛出参数错误异常")
            void list_WithNullChatDto_ShouldThrowChatException() {
                // 执行测试并验证
                StepVerifier.create(chatService.list(null))
                        .expectError(ChatException.class)
                        .verify();
            }

            @Test
            @DisplayName("当ChatDto缺少fromId时_list方法应抛出参数错误异常")
            void list_WithMissingFromId_ShouldThrowChatException() {
                // 准备测试数据
                ChatDto chatDto = new ChatDto();

                // 执行测试并验证
                StepVerifier.create(chatService.list(chatDto))
                        .expectError(ChatException.class)
                        .verify();
            }

            @Test
            @DisplayName("当用户有多个会话时_list方法应返回完整的会话列表")
            void list_WithMultipleChats_ShouldReturnCompleteList() {
                // 准备测试数据
                ChatDto chatDto = new ChatDto();
                chatDto.setFromId("user1");
                chatDto.setSequence(0L);

                ImChatPo chat1 = new ImChatPo();
                chat1.setChatId("chat1");
                chat1.setOwnerId("user1");
                chat1.setToId("user2");
                chat1.setChatType(IMessageType.SINGLE_MESSAGE.getCode());

                ImChatPo chat2 = new ImChatPo();
                chat2.setChatId("chat2");
                chat2.setOwnerId("user1");
                chat2.setToId("user3");
                chat2.setChatType(IMessageType.SINGLE_MESSAGE.getCode());

                ImUserDataPo user2 = new ImUserDataPo();
                user2.setUserId("user2");
                user2.setName("User 2");

                ImUserDataPo user3 = new ImUserDataPo();
                user3.setUserId("user3");
                user3.setName("User 3");

                // 模拟 Dubbo 服务
                given(imChatDubboService.queryList("user1", 0L)).willReturn(Arrays.asList(chat1, chat2));
                given(imSingleMessageDubboService.queryLast(anyString(), anyString())).willReturn(new ImSingleMessagePo());
                given(imSingleMessageDubboService.queryReadStatus(anyString(), anyString(), anyInt())).willReturn(0);
                given(imUserDataDubboService.queryOne("user2")).willReturn(user2);
                given(imUserDataDubboService.queryOne("user3")).willReturn(user3);

                // 执行测试并验证
                StepVerifier.create(chatService.list(chatDto))
                        .expectNextMatches(list -> list.size() == 2)
                        .verifyComplete();
            }

            @Test
            @DisplayName("当用户没有会话时_list方法应返回空列表")
            void list_WithNoChats_ShouldReturnEmptyList() {
                // 准备测试数据
                ChatDto chatDto = new ChatDto();
                chatDto.setFromId("user1");
                chatDto.setSequence(0L);

                // 模拟 Dubbo 服务返回空列表
                given(imChatDubboService.queryList("user1", 0L)).willReturn(List.of());

                // 执行测试并验证
                StepVerifier.create(chatService.list(chatDto))
                        .expectNextMatches(List::isEmpty)
                        .verifyComplete();
            }
        }
    }
}

