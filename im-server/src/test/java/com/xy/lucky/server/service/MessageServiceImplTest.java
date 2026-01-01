package com.xy.lucky.server.service;

import com.xy.lucky.core.enums.IMessageType;
import com.xy.lucky.core.model.IMGroupMessage;
import com.xy.lucky.core.model.IMRegisterUser;
import com.xy.lucky.core.model.IMSingleMessage;
import com.xy.lucky.core.model.IMVideoMessage;
import com.xy.lucky.core.model.IMessageAction;
import com.xy.lucky.domain.dto.ChatDto;
import com.xy.lucky.domain.po.ImGroupMessagePo;
import com.xy.lucky.domain.po.ImSingleMessagePo;
import com.xy.lucky.dubbo.web.api.database.chat.ImChatDubboService;
import com.xy.lucky.dubbo.web.api.database.group.ImGroupMemberDubboService;
import com.xy.lucky.dubbo.web.api.database.message.ImGroupMessageDubboService;
import com.xy.lucky.dubbo.web.api.database.message.ImSingleMessageDubboService;
import com.xy.lucky.dubbo.web.api.database.outbox.IMOutboxDubboService;
import com.xy.lucky.dubbo.web.api.id.ImIdDubboService;
import com.xy.lucky.mq.rabbit.core.RabbitTemplateFactory;
import com.xy.lucky.server.exception.MessageException;
import com.xy.lucky.server.service.impl.MessageServiceImpl;
import com.xy.lucky.server.utils.RedisUtil;
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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * MessageServiceImpl 单元测试类
 * 
 * 测试消息服务的核心功能，包括单聊/群聊消息发送、视频消息、撤回消息等操作。
 * 使用 Mockito 模拟 Dubbo 服务、Redis、RabbitMQ 等依赖。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("消息服务测试")
class MessageServiceImplTest {

    @Mock
    private ImChatDubboService imChatDubboService;

    @Mock
    private ImGroupMemberDubboService imGroupMemberDubboService;

    @Mock
    private ImSingleMessageDubboService imSingleMessageDubboService;

    @Mock
    private ImGroupMessageDubboService imGroupMessageDubboService;

    @Mock
    private IMOutboxDubboService imOutboxDubboService;

    @Mock
    private ImIdDubboService imIdDubboService;

    @Mock
    private RedisUtil redisUtil;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RedissonReactiveClient reactiveClient;

    @Mock
    private RLockReactive lockReactive;

    @Mock
    private RabbitTemplateFactory rabbitTemplateFactory;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private MessageServiceImpl messageService;

    /**
     * 每个测试方法执行前的初始化操作
     * 配置 Redisson 分布式锁和 RabbitMQ 模拟行为
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

    // ==================== sendSingleMessage 方法测试 ====================

    @Nested
    @DisplayName("sendSingleMessage 方法测试")
    class SendSingleMessageTests {
        @Test
        @DisplayName("当无法获取分布式锁时_sendSingleMessage应抛出锁获取失败异常")
        void sendSingleMessage_WhenLockAcquisitionFails_ShouldThrowLockError() {
            // 准备测试数据
            IMSingleMessage message = IMSingleMessage.builder()
                    .fromId("user1")
                    .toId("user2")
                    .messageTempId("temp123")
                    .build();
            
            // 模拟锁获取失败
            when(lockReactive.tryLock(anyLong(), anyLong(), any())).thenReturn(Mono.just(false));
            
            // 执行测试并验证
            StepVerifier.create(messageService.sendSingleMessage(message))
                    .expectErrorMatches(throwable -> 
                        throwable instanceof MessageException &&
                        throwable.getMessage().contains("无法获取锁"))
                    .verify();
        }
    }

    // ==================== sendGroupMessage 方法测试 ====================

    @Nested
    @DisplayName("sendGroupMessage 方法测试")
    class SendGroupMessageTests {

        @Test
        @DisplayName("当无法获取分布式锁时_sendGroupMessage应抛出锁获取失败异常")
        void sendGroupMessage_WhenLockAcquisitionFails_ShouldThrowLockError() {
            // 准备测试数据
            IMGroupMessage message = IMGroupMessage.builder()
                    .fromId("user1")
                    .groupId("group123")
                    .messageTempId("temp123")
                    .build();
            
            // 模拟锁获取失败
            when(lockReactive.tryLock(anyLong(), anyLong(), any())).thenReturn(Mono.just(false));
            
            // 执行测试并验证
            StepVerifier.create(messageService.sendGroupMessage(message))
                    .expectErrorMatches(throwable -> 
                        throwable instanceof MessageException &&
                        throwable.getMessage().contains("无法获取锁"))
                    .verify();
        }
    }

    // ==================== sendVideoMessage 方法测试 ====================

    @Nested
    @DisplayName("sendVideoMessage 方法测试")
    class SendVideoMessageTests {
        @Test
        @DisplayName("当发送视频消息且接收者离线时_应静默完成不抛出异常")
        void sendVideoMessage_WithOfflineReceiver_ShouldCompleteWithoutError() {
            // 准备测试数据
            IMVideoMessage videoMessage = new IMVideoMessage();
            videoMessage.setFromId("user1");
            videoMessage.setToId("user2");
            
            // 模拟服务 - 接收者离线
            given(redisUtil.get(anyString())).willReturn(Mono.just(new IMRegisterUser()));
            
            // 执行测试并验证
            StepVerifier.create(messageService.sendVideoMessage(videoMessage))
                    .verifyComplete();
        }
    }

    // ==================== recallMessage 方法测试 ====================

    @Nested
    @DisplayName("recallMessage 方法测试")
    class RecallMessageTests {

        @Test
        @DisplayName("当撤回自己发送的未超时消息时_应成功撤回")
        void recallMessage_WithOwnMessageWithinTimeout_ShouldRecallSuccessfully() {
            // 准备测试数据
            IMessageAction action = new IMessageAction();
            action.setMessageId("msg123");
            action.setOperatorId("user1");
            
            ImSingleMessagePo messagePo = new ImSingleMessagePo();
            messagePo.setMessageId("msg123");
            messagePo.setFromId("user1");
            messagePo.setToId("user2");
            messagePo.setMessageTime(System.currentTimeMillis()); // 刚发送的消息
            
            // 模拟服务
            given(imSingleMessageDubboService.queryOne("msg123")).willReturn(messagePo);
            given(imSingleMessageDubboService.modify(any(ImSingleMessagePo.class))).willReturn(true);
            given(redisUtil.get(anyString())).willReturn(Mono.just(new IMRegisterUser()));
            
            // 执行测试并验证
            StepVerifier.create(messageService.recallMessage(action))
                    .verifyComplete();
        }

        @Test
        @DisplayName("当撤回他人发送的消息时_应抛出无权撤回异常")
        void recallMessage_WithOthersMessage_ShouldThrowUnauthorizedError() {
            // 准备测试数据
            IMessageAction action = new IMessageAction();
            action.setMessageId("msg123");
            action.setOperatorId("user2"); // 不是发送者
            
            ImSingleMessagePo messagePo = new ImSingleMessagePo();
            messagePo.setMessageId("msg123");
            messagePo.setFromId("user1"); // 真正的发送者
            messagePo.setToId("user2");
            messagePo.setMessageTime(System.currentTimeMillis());
            
            // 模拟服务
            given(imSingleMessageDubboService.queryOne("msg123")).willReturn(messagePo);
            
            // 执行测试并验证
            StepVerifier.create(messageService.recallMessage(action))
                    .expectErrorMatches(throwable -> 
                        throwable instanceof MessageException &&
                        throwable.getMessage().contains("无权撤回"))
                    .verify();
        }

        @Test
        @DisplayName("当撤回超过2分钟的消息时_应抛出超时无法撤回异常")
        void recallMessage_WithExpiredMessage_ShouldThrowTimeoutError() {
            // 准备测试数据
            IMessageAction action = new IMessageAction();
            action.setMessageId("msg123");
            action.setOperatorId("user1");
            
            ImSingleMessagePo messagePo = new ImSingleMessagePo();
            messagePo.setMessageId("msg123");
            messagePo.setFromId("user1");
            messagePo.setToId("user2");
            messagePo.setMessageTime(System.currentTimeMillis() - 3 * 60 * 1000); // 3分钟前
            
            // 模拟服务
            given(imSingleMessageDubboService.queryOne("msg123")).willReturn(messagePo);
            
            // 执行测试并验证
            StepVerifier.create(messageService.recallMessage(action))
                    .expectErrorMatches(throwable -> 
                        throwable instanceof MessageException &&
                        throwable.getMessage().contains("超过2分钟"))
                    .verify();
        }
    }

    // ==================== list 方法测试 ====================

    @Nested
    @DisplayName("list 方法测试")
    class ListMethodTests {

        @Test
        @DisplayName("当用户有单聊和群聊消息时_list方法应返回包含两种消息类型的Map")
        void list_WithBothMessageTypes_ShouldReturnMapWithBothTypes() {
            // 准备测试数据
            ChatDto chatDto = new ChatDto();
            chatDto.setFromId("user1");
            chatDto.setSequence(0L);
            
            ImSingleMessagePo singleMessage = new ImSingleMessagePo();
            singleMessage.setMessageId("single123");
            
            ImGroupMessagePo groupMessage = new ImGroupMessagePo();
            groupMessage.setMessageId("group123");
            
            // 模拟服务
            given(imSingleMessageDubboService.queryList("user1", 0L))
                    .willReturn(List.of(singleMessage));
            given(imGroupMessageDubboService.queryList("user1", 0L))
                    .willReturn(List.of(groupMessage));
            
            // 执行测试并验证
            StepVerifier.create(messageService.list(chatDto))
                    .expectNextMatches(result -> 
                        result.containsKey(IMessageType.SINGLE_MESSAGE.getCode()) &&
                        result.containsKey(IMessageType.GROUP_MESSAGE.getCode()))
                    .verifyComplete();
        }

        @Test
        @DisplayName("当用户只有单聊消息时_list方法应只返回单聊消息")
        void list_WithOnlySingleMessages_ShouldReturnOnlySingleMessages() {
            // 准备测试数据
            ChatDto chatDto = new ChatDto();
            chatDto.setFromId("user1");
            chatDto.setSequence(0L);
            
            ImSingleMessagePo singleMessage = new ImSingleMessagePo();
            singleMessage.setMessageId("single123");
            
            // 模拟服务
            given(imSingleMessageDubboService.queryList("user1", 0L))
                    .willReturn(List.of(singleMessage));
            given(imGroupMessageDubboService.queryList("user1", 0L))
                    .willReturn(List.of());
            
            // 执行测试并验证
            StepVerifier.create(messageService.list(chatDto))
                    .expectNextMatches(result -> 
                        result.containsKey(IMessageType.SINGLE_MESSAGE.getCode()) &&
                        !result.containsKey(IMessageType.GROUP_MESSAGE.getCode()))
                    .verifyComplete();
        }

        @Test
        @DisplayName("当用户没有任何消息时_list方法应返回空Map")
        void list_WithNoMessages_ShouldReturnEmptyMap() {
            // 准备测试数据
            ChatDto chatDto = new ChatDto();
            chatDto.setFromId("user1");
            chatDto.setSequence(0L);
            
            // 模拟服务 - 没有任何消息
            given(imSingleMessageDubboService.queryList("user1", 0L)).willReturn(List.of());
            given(imGroupMessageDubboService.queryList("user1", 0L)).willReturn(List.of());
            
            // 执行测试并验证
            StepVerifier.create(messageService.list(chatDto))
                    .expectNextMatches(Map::isEmpty)
                    .verifyComplete();
        }
    }
}

