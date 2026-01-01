package com.xy.lucky.server.service;

import com.xy.lucky.domain.dto.UserDto;
import com.xy.lucky.domain.po.ImUserDataPo;
import com.xy.lucky.dubbo.web.api.database.user.ImUserDataDubboService;
import com.xy.lucky.server.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * UserServiceImpl 单元测试类
 * 
 * 测试用户服务的核心功能，包括用户的增删改查操作以及分布式锁的正确处理。
 * 使用 Mockito 模拟 Dubbo 服务和 Redisson 客户端。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务测试")
class UserServiceImplTest {

    @Mock
    private ImUserDataDubboService imUserDataDubboService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RedissonReactiveClient reactiveClient;

    @Mock
    private RLockReactive lockReactive;

    @InjectMocks
    private UserServiceImpl userService;

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

    // ==================== list 方法测试 ====================

    @Test
    @DisplayName("当UserDto包含有效UserId时_list方法应返回包含该用户的列表")
    void list_WithValidUserId_ShouldReturnListContainingUser() {
        // 准备测试数据
        String userId = "user123";
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        
        ImUserDataPo mockPo = new ImUserDataPo();
        mockPo.setUserId(userId);
        mockPo.setName("Test User");
        
        // 模拟 Dubbo 服务返回
        given(imUserDataDubboService.queryOne(userId)).willReturn(mockPo);
        
        // 执行测试并验证
        StepVerifier.create(userService.list(userDto))
                .expectNextMatches(list -> 
                    list.size() == 1 && list.get(0).getUserId().equals(userId))
                .verifyComplete();
    }

    @Test
    @DisplayName("当UserDto的UserId为空时_list方法应返回空列表")
    void list_WithEmptyUserId_ShouldReturnEmptyList() {
        // 准备测试数据 - 空的 UserDto
        UserDto userDto = new UserDto();
        
        // 执行测试并验证
        StepVerifier.create(userService.list(userDto))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
    }

    @Test
    @DisplayName("当UserDto为null时_list方法应返回空列表")
    void list_WithNullUserDto_ShouldReturnEmptyList() {
        // 执行测试并验证
        StepVerifier.create(userService.list(null))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
    }

    @Test
    @DisplayName("当Dubbo服务返回null时_list方法应返回空列表")
    void list_WhenDubboServiceReturnsNull_ShouldReturnEmptyList() {
        // 准备测试数据
        String userId = "user123";
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        
        // 模拟 Dubbo 服务返回 null
        given(imUserDataDubboService.queryOne(userId)).willReturn(null);
        
        // 执行测试并验证
        StepVerifier.create(userService.list(userDto))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
    }

    // ==================== one 方法测试 ====================

    @Test
    @DisplayName("当UserId有效时_one方法应返回对应的UserVo")
    void one_WithValidUserId_ShouldReturnUserVo() {
        // 准备测试数据
        String userId = "user123";
        ImUserDataPo mockPo = new ImUserDataPo();
        mockPo.setUserId(userId);
        mockPo.setName("Test User");
        mockPo.setAvatar("avatar.jpg");
        
        // 模拟 Dubbo 服务返回
        given(imUserDataDubboService.queryOne(userId)).willReturn(mockPo);
        
        // 执行测试并验证
        StepVerifier.create(userService.one(userId))
                .expectNextMatches(vo -> 
                    vo.getUserId().equals(userId) && vo.getName().equals("Test User"))
                .verifyComplete();
    }

    @Test
    @DisplayName("当UserId为null时_one方法应抛出参数错误异常")
    void one_WithNullUserId_ShouldThrowError() {
        // 执行测试并验证
        StepVerifier.create(userService.one(null))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().contains("参数错误"))
                .verify();
    }

    @Test
    @DisplayName("当无法获取分布式锁时_one方法应抛出锁获取失败异常")
    void one_WhenLockAcquisitionFails_ShouldThrowLockError() {
        // 模拟锁获取失败
        when(lockReactive.tryLock(anyLong(), anyLong(), any())).thenReturn(Mono.just(false));
        
        // 执行测试并验证
        StepVerifier.create(userService.one("user123"))
                .expectErrorMatches(throwable -> 
                    throwable.getMessage().contains("无法获取锁"))
                .verify();
    }

    // ==================== create 方法测试 ====================

    @Test
    @DisplayName("当UserDto包含有效UserId时_create方法应成功创建用户并返回UserVo")
    void create_WithValidUserDto_ShouldCreateUserAndReturnUserVo() {
        // 准备测试数据
        String userId = "newUser123";
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        userDto.setName("New User");
        
        // 模拟 Dubbo 服务成功创建
        given(imUserDataDubboService.creat(any(ImUserDataPo.class))).willReturn(true);
        
        // 执行测试并验证
        StepVerifier.create(userService.create(userDto))
                .expectNextMatches(vo -> vo.getUserId().equals(userId))
                .verifyComplete();
    }

    @Test
    @DisplayName("当UserDto的UserId为空时_create方法应抛出UserId不能为空异常")
    void create_WithEmptyUserId_ShouldThrowError() {
        // 准备测试数据 - 没有 userId
        UserDto userDto = new UserDto();
        userDto.setName("New User");
        
        // 执行测试并验证
        StepVerifier.create(userService.create(userDto))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException)
                .verify();
    }

    @Test
    @DisplayName("当Dubbo服务创建失败时_create方法应抛出创建用户失败异常")
    void create_WhenDubboServiceFails_ShouldThrowError() {
        // 准备测试数据
        String userId = "newUser123";
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        
        // 模拟 Dubbo 服务创建失败
        given(imUserDataDubboService.creat(any(ImUserDataPo.class))).willReturn(false);
        
        // 执行测试并验证
        StepVerifier.create(userService.create(userDto))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().contains("创建用户失败"))
                .verify();
    }

    // ==================== update 方法测试 ====================

    @Test
    @DisplayName("当UserDto有效时_update方法应成功更新用户并返回true")
    void update_WithValidUserDto_ShouldUpdateUserAndReturnTrue() {
        // 准备测试数据
        String userId = "user123";
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        userDto.setName("Updated Name");
        
        // 模拟 Dubbo 服务成功更新
        given(imUserDataDubboService.modify(any(ImUserDataPo.class))).willReturn(true);
        
        // 执行测试并验证
        StepVerifier.create(userService.update(userDto))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("当UserDto为null时_update方法应抛出参数错误异常")
    void update_WithNullUserDto_ShouldThrowError() {
        // 执行测试并验证
        StepVerifier.create(userService.update(null))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().contains("参数错误"))
                .verify();
    }

    @Test
    @DisplayName("当UserDto的UserId为null时_update方法应抛出参数错误异常")
    void update_WithNullUserId_ShouldThrowError() {
        // 准备测试数据
        UserDto userDto = new UserDto();
        userDto.setName("Updated Name");
        
        // 执行测试并验证
        StepVerifier.create(userService.update(userDto))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().contains("参数错误"))
                .verify();
    }

    @Test
    @DisplayName("当Dubbo服务更新失败时_update方法应抛出更新用户失败异常")
    void update_WhenDubboServiceFails_ShouldThrowError() {
        // 准备测试数据
        String userId = "user123";
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        
        // 模拟 Dubbo 服务更新失败
        given(imUserDataDubboService.modify(any(ImUserDataPo.class))).willReturn(false);
        
        // 执行测试并验证
        StepVerifier.create(userService.update(userDto))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().contains("更新用户失败"))
                .verify();
    }

    // ==================== delete 方法测试 ====================

    @Test
    @DisplayName("当UserId有效时_delete方法应成功删除用户并返回true")
    void delete_WithValidUserId_ShouldDeleteUserAndReturnTrue() {
        // 准备测试数据
        String userId = "user123";
        
        // 模拟 Dubbo 服务成功删除
        given(imUserDataDubboService.removeOne(userId)).willReturn(true);
        
        // 执行测试并验证
        StepVerifier.create(userService.delete(userId))
                .expectNext(true)
                .verifyComplete();
        
        // 验证 Dubbo 服务被调用
        verify(imUserDataDubboService).removeOne(userId);
    }

    @Test
    @DisplayName("当UserId为null时_delete方法应抛出参数错误异常")
    void delete_WithNullUserId_ShouldThrowError() {
        // 执行测试并验证
        StepVerifier.create(userService.delete(null))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().contains("参数错误"))
                .verify();
    }

    @Test
    @DisplayName("当Dubbo服务删除失败时_delete方法应抛出删除失败异常")
    void delete_WhenDubboServiceFails_ShouldThrowError() {
        // 准备测试数据
        String userId = "user123";
        
        // 模拟 Dubbo 服务删除失败
        given(imUserDataDubboService.removeOne(userId)).willReturn(false);
        
        // 执行测试并验证
        StepVerifier.create(userService.delete(userId))
                .expectErrorMatches(throwable -> 
                    throwable instanceof RuntimeException && 
                    throwable.getMessage().contains("删除失败"))
                .verify();
    }

    @Test
    @DisplayName("当无法获取分布式锁时_delete方法应抛出锁获取失败异常")
    void delete_WhenLockAcquisitionFails_ShouldThrowLockError() {
        // 模拟锁获取失败
        when(lockReactive.tryLock(anyLong(), anyLong(), any())).thenReturn(Mono.just(false));
        
        // 执行测试并验证
        StepVerifier.create(userService.delete("user123"))
                .expectErrorMatches(throwable -> 
                    throwable.getMessage().contains("无法获取锁"))
                .verify();
    }
}

