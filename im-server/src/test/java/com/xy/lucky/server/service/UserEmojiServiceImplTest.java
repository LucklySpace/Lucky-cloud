package com.xy.lucky.server.service;

import com.xy.lucky.dubbo.web.api.database.emoji.ImUserEmojiPackDubboService;
import com.xy.lucky.server.service.impl.UserEmojiServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * UserEmojiServiceImpl 单元测试类
 * 
 * 测试用户表情包服务的核心功能，包括表情包列表查询、绑定和解绑操作。
 * 使用 Mockito 模拟 Dubbo 服务依赖。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户表情包服务测试")
class UserEmojiServiceImplTest {

    @Mock
    private ImUserEmojiPackDubboService dubboService;

    @InjectMocks
    private UserEmojiServiceImpl userEmojiService;

    // ==================== listPackIds 方法测试 ====================

    @Nested
    @DisplayName("listPackIds 方法测试")
    class ListPackIdsMethodTests {

        @Test
        @DisplayName("当用户有绑定表情包时_listPackIds方法应返回表情包编码列表")
        void listPackIds_WithBoundPacks_ShouldReturnPackIdList() {
            // 准备测试数据
            String userId = "user1";
            List<String> expectedPackIds = Arrays.asList("pack1", "pack2", "pack3");
            
            // 模拟服务
            given(dubboService.listPackIds(userId)).willReturn(expectedPackIds);
            
            // 执行测试并验证
            StepVerifier.create(userEmojiService.listPackIds(userId))
                    .expectNextMatches(packIds -> 
                        packIds.size() == 3 &&
                        packIds.contains("pack1") &&
                        packIds.contains("pack2") &&
                        packIds.contains("pack3"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("当用户没有绑定表情包时_listPackIds方法应返回空列表")
        void listPackIds_WithNoBoundPacks_ShouldReturnEmptyList() {
            // 准备测试数据
            String userId = "user1";
            
            // 模拟服务返回空列表
            given(dubboService.listPackIds(userId)).willReturn(List.of());
            
            // 执行测试并验证
            StepVerifier.create(userEmojiService.listPackIds(userId))
                    .expectNextMatches(List::isEmpty)
                    .verifyComplete();
        }
    }

    // ==================== bindPack 方法测试 ====================

    @Nested
    @DisplayName("bindPack 方法测试")
    class BindPackMethodTests {

        @Test
        @DisplayName("当绑定表情包成功时_bindPack方法应返回true")
        void bindPack_WithValidParams_ShouldReturnTrue() {
            // 准备测试数据
            String userId = "user1";
            String packId = "pack1";
            
            // 模拟服务
            given(dubboService.bindPack(userId, packId)).willReturn(true);
            
            // 执行测试并验证
            StepVerifier.create(userEmojiService.bindPack(userId, packId))
                    .expectNext(true)
                    .verifyComplete();
            
            // 验证 Dubbo 服务被正确调用
            verify(dubboService).bindPack(userId, packId);
        }

        @Test
        @DisplayName("当绑定表情包失败时_bindPack方法应返回false")
        void bindPack_WhenBindFails_ShouldReturnFalse() {
            // 准备测试数据
            String userId = "user1";
            String packId = "pack1";
            
            // 模拟服务返回失败
            given(dubboService.bindPack(userId, packId)).willReturn(false);
            
            // 执行测试并验证
            StepVerifier.create(userEmojiService.bindPack(userId, packId))
                    .expectNext(false)
                    .verifyComplete();
        }

        @Test
        @DisplayName("当重复绑定同一表情包时_bindPack方法应根据服务返回结果处理")
        void bindPack_WithDuplicateBind_ShouldReturnServiceResult() {
            // 准备测试数据
            String userId = "user1";
            String packId = "pack1";
            
            // 模拟服务 - 重复绑定可能返回 false 或 true，取决于业务逻辑
            given(dubboService.bindPack(userId, packId)).willReturn(true);
            
            // 执行测试并验证
            StepVerifier.create(userEmojiService.bindPack(userId, packId))
                    .expectNext(true)
                    .verifyComplete();
        }
    }

    // ==================== unbindPack 方法测试 ====================

    @Nested
    @DisplayName("unbindPack 方法测试")
    class UnbindPackMethodTests {

        @Test
        @DisplayName("当解绑表情包成功时_unbindPack方法应返回true")
        void unbindPack_WithValidParams_ShouldReturnTrue() {
            // 准备测试数据
            String userId = "user1";
            String packId = "pack1";
            
            // 模拟服务
            given(dubboService.unbindPack(userId, packId)).willReturn(true);
            
            // 执行测试并验证
            StepVerifier.create(userEmojiService.unbindPack(userId, packId))
                    .expectNext(true)
                    .verifyComplete();
            
            // 验证 Dubbo 服务被正确调用
            verify(dubboService).unbindPack(userId, packId);
        }

        @Test
        @DisplayName("当解绑表情包失败时_unbindPack方法应返回false")
        void unbindPack_WhenUnbindFails_ShouldReturnFalse() {
            // 准备测试数据
            String userId = "user1";
            String packId = "pack1";
            
            // 模拟服务返回失败
            given(dubboService.unbindPack(userId, packId)).willReturn(false);
            
            // 执行测试并验证
            StepVerifier.create(userEmojiService.unbindPack(userId, packId))
                    .expectNext(false)
                    .verifyComplete();
        }

        @Test
        @DisplayName("当解绑未绑定的表情包时_unbindPack方法应返回服务返回的结果")
        void unbindPack_WithUnboundPack_ShouldReturnServiceResult() {
            // 准备测试数据
            String userId = "user1";
            String packId = "pack999"; // 未绑定的表情包
            
            // 模拟服务 - 解绑未绑定的包可能返回 false
            given(dubboService.unbindPack(userId, packId)).willReturn(false);
            
            // 执行测试并验证
            StepVerifier.create(userEmojiService.unbindPack(userId, packId))
                    .expectNext(false)
                    .verifyComplete();
        }
    }

    // ==================== 异常处理测试 ====================

    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("当Dubbo服务抛出异常时_listPackIds方法应传播异常")
        void listPackIds_WhenDubboServiceThrows_ShouldPropagateError() {
            // 准备测试数据
            String userId = "user1";
            
            // 模拟服务抛出异常
            given(dubboService.listPackIds(userId)).willThrow(new RuntimeException("Dubbo service error"));
            
            // 执行测试并验证
            StepVerifier.create(userEmojiService.listPackIds(userId))
                    .expectError(RuntimeException.class)
                    .verify();
        }

        @Test
        @DisplayName("当Dubbo服务抛出异常时_bindPack方法应传播异常")
        void bindPack_WhenDubboServiceThrows_ShouldPropagateError() {
            // 准备测试数据
            String userId = "user1";
            String packId = "pack1";
            
            // 模拟服务抛出异常
            given(dubboService.bindPack(userId, packId)).willThrow(new RuntimeException("Dubbo service error"));
            
            // 执行测试并验证
            StepVerifier.create(userEmojiService.bindPack(userId, packId))
                    .expectError(RuntimeException.class)
                    .verify();
        }

        @Test
        @DisplayName("当Dubbo服务抛出异常时_unbindPack方法应传播异常")
        void unbindPack_WhenDubboServiceThrows_ShouldPropagateError() {
            // 准备测试数据
            String userId = "user1";
            String packId = "pack1";
            
            // 模拟服务抛出异常
            given(dubboService.unbindPack(userId, packId)).willThrow(new RuntimeException("Dubbo service error"));
            
            // 执行测试并验证
            StepVerifier.create(userEmojiService.unbindPack(userId, packId))
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }
}

