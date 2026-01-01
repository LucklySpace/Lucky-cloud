package com.xy.lucky.server.service;

import com.xy.lucky.domain.dto.FriendDto;
import com.xy.lucky.domain.dto.FriendRequestDto;
import com.xy.lucky.domain.po.ImFriendshipPo;
import com.xy.lucky.domain.po.ImFriendshipRequestPo;
import com.xy.lucky.domain.po.ImGroupPo;
import com.xy.lucky.domain.po.ImUserDataPo;
import com.xy.lucky.domain.vo.FriendVo;
import com.xy.lucky.dubbo.web.api.database.friend.ImFriendshipDubboService;
import com.xy.lucky.dubbo.web.api.database.friend.ImFriendshipRequestDubboService;
import com.xy.lucky.dubbo.web.api.database.group.ImGroupDubboService;
import com.xy.lucky.dubbo.web.api.database.user.ImUserDataDubboService;
import com.xy.lucky.general.exception.BusinessException;
import com.xy.lucky.general.exception.GlobalException;
import com.xy.lucky.server.exception.MessageException;
import com.xy.lucky.server.service.impl.RelationshipServiceImpl;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * RelationshipServiceImpl 单元测试类
 * 
 * 测试用户关系服务的核心功能，包括好友申请、审批、删除、备注修改等操作。
 * 使用 Mockito 模拟 Dubbo 服务和 Redisson 客户端。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户关系服务测试")
class RelationshipServiceImplTest {

    @Mock
    private ImFriendshipDubboService imFriendshipDubboService;

    @Mock
    private ImFriendshipRequestDubboService imFriendshipRequestDubboService;

    @Mock
    private ImUserDataDubboService imUserDataDubboService;

    @Mock
    private ImGroupDubboService imGroupDubboService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RedissonReactiveClient reactiveClient;

    @Mock
    private RLockReactive lockReactive;

    @InjectMocks
    private RelationshipServiceImpl relationshipService;

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

    // ==================== contacts 方法测试 ====================

    @Nested
    @DisplayName("contacts 方法测试")
    class ContactsMethodTests {

        @Test
        @DisplayName("当用户有好友时_contacts方法应返回好友列表")
        void contacts_WithFriends_ShouldReturnFriendList() {
            // 准备测试数据
            String ownerId = "user1";
            Long sequence = 0L;
            
            ImFriendshipPo friendship = new ImFriendshipPo();
            friendship.setOwnerId(ownerId);
            friendship.setToId("user2");
            friendship.setRemark("好友备注");
            
            ImUserDataPo userData = new ImUserDataPo();
            userData.setUserId("user2");
            userData.setName("Friend User");
            userData.setAvatar("avatar.jpg");
            
            // 模拟服务
            given(imFriendshipDubboService.queryList(ownerId, sequence))
                    .willReturn(List.of(friendship));
            given(imUserDataDubboService.queryListByIds(anyList()))
                    .willReturn(List.of(userData));
            
            // 执行测试并验证
            StepVerifier.create(relationshipService.contacts(ownerId, sequence))
                    .expectNextMatches(list -> 
                        list.size() == 1 && 
                        ((FriendVo) list.get(0)).getFriendId().equals("user2"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("当用户没有好友时_contacts方法应返回空列表")
        void contacts_WithNoFriends_ShouldReturnEmptyList() {
            // 准备测试数据
            String ownerId = "user1";
            Long sequence = 0L;
            
            // 模拟服务返回空列表
            given(imFriendshipDubboService.queryList(ownerId, sequence))
                    .willReturn(List.of());
            
            // 执行测试并验证
            StepVerifier.create(relationshipService.contacts(ownerId, sequence))
                    .expectNextMatches(List::isEmpty)
                    .verifyComplete();
        }

        @Test
        @DisplayName("当无法获取分布式锁时_contacts方法应抛出锁获取失败异常")
        void contacts_WhenLockAcquisitionFails_ShouldThrowLockError() {
            // 模拟锁获取失败
            when(lockReactive.tryLock(anyLong(), anyLong(), any())).thenReturn(Mono.just(false));
            
            // 执行测试并验证
            StepVerifier.create(relationshipService.contacts("user1", 0L))
                    .expectErrorMatches(throwable -> 
                        throwable instanceof MessageException &&
                        throwable.getMessage().contains("无法获取锁"))
                    .verify();
        }
    }

    // ==================== groups 方法测试 ====================

    @Nested
    @DisplayName("groups 方法测试")
    class GroupsMethodTests {

        @Test
        @DisplayName("当用户有群组时_groups方法应返回群组列表")
        void groups_WithGroups_ShouldReturnGroupList() {
            // 准备测试数据
            String userId = "user1";
            
            ImGroupPo group = new ImGroupPo();
            group.setGroupId("group123");
            group.setGroupName("Test Group");
            group.setAvatar("group_avatar.jpg");
            
            // 模拟服务
            given(imGroupDubboService.queryList(userId)).willReturn(List.of(group));
            
            // 执行测试并验证
            StepVerifier.create(relationshipService.groups(userId))
                    .expectNextMatches(list -> 
                        list.size() == 1)
                    .verifyComplete();
        }

        @Test
        @DisplayName("当用户没有群组时_groups方法应返回空列表")
        void groups_WithNoGroups_ShouldReturnEmptyList() {
            // 准备测试数据
            String userId = "user1";
            
            // 模拟服务返回空列表
            given(imGroupDubboService.queryList(userId)).willReturn(List.of());
            
            // 执行测试并验证
            StepVerifier.create(relationshipService.groups(userId))
                    .expectNextMatches(List::isEmpty)
                    .verifyComplete();
        }
    }

    // ==================== newFriends 方法测试 ====================

    @Nested
    @DisplayName("newFriends 方法测试")
    class NewFriendsMethodTests {

        @Test
        @DisplayName("当有好友请求时_newFriends方法应返回请求列表")
        void newFriends_WithRequests_ShouldReturnRequestList() {
            // 准备测试数据
            String userId = "user1";
            
            ImFriendshipRequestPo request = new ImFriendshipRequestPo();
            request.setId("req123");
            request.setFromId("user2");
            request.setToId(userId);
            request.setMessage("Hello, add me!");
            
            ImUserDataPo requester = new ImUserDataPo();
            requester.setUserId("user2");
            requester.setName("Requester");
            
            // 模拟服务
            given(imFriendshipRequestDubboService.queryList(userId))
                    .willReturn(List.of(request));
            given(imUserDataDubboService.queryListByIds(anyList()))
                    .willReturn(List.of(requester));
            
            // 执行测试并验证
            StepVerifier.create(relationshipService.newFriends(userId))
                    .expectNextMatches(list -> list.size() == 1)
                    .verifyComplete();
        }

        @Test
        @DisplayName("当没有好友请求时_newFriends方法应返回空列表")
        void newFriends_WithNoRequests_ShouldReturnEmptyList() {
            // 准备测试数据
            String userId = "user1";
            
            // 模拟服务返回空列表
            given(imFriendshipRequestDubboService.queryList(userId))
                    .willReturn(List.of());
            
            // 执行测试并验证
            StepVerifier.create(relationshipService.newFriends(userId))
                    .expectNextMatches(List::isEmpty)
                    .verifyComplete();
        }
    }

    // ==================== getFriendInfo 方法测试 ====================

    @Nested
    @DisplayName("getFriendInfo 方法测试")
    class GetFriendInfoMethodTests {

        @Test
        @DisplayName("当好友存在时_getFriendInfo方法应返回好友信息")
        void getFriendInfo_WithExistingFriend_ShouldReturnFriendVo() {
            // 准备测试数据
            FriendDto friendDto = new FriendDto();
            friendDto.setFromId("user1");
            friendDto.setToId("user2");
            
            ImUserDataPo userData = new ImUserDataPo();
            userData.setUserId("user2");
            userData.setName("Friend User");
            userData.setAvatar("avatar.jpg");
            
            ImFriendshipPo friendship = new ImFriendshipPo();
            friendship.setOwnerId("user1");
            friendship.setToId("user2");
            friendship.setRemark("好友备注");
            
            // 模拟服务
            given(imUserDataDubboService.queryOne("user2")).willReturn(userData);
            given(imFriendshipDubboService.queryOne("user1", "user2")).willReturn(friendship);
            
            // 执行测试并验证
            StepVerifier.create(relationshipService.getFriendInfo(friendDto))
                    .expectNextMatches(vo -> 
                        vo.getFriendId().equals("user2") && 
                        vo.getName().equals("Friend User") &&
                        vo.getFlag() == 1) // 是好友
                    .verifyComplete();
        }

        @Test
        @DisplayName("当用户存在但不是好友时_getFriendInfo方法应返回flag为0的FriendVo")
        void getFriendInfo_WithNonFriend_ShouldReturnFriendVoWithFlagZero() {
            // 准备测试数据
            FriendDto friendDto = new FriendDto();
            friendDto.setFromId("user1");
            friendDto.setToId("user2");
            
            ImUserDataPo userData = new ImUserDataPo();
            userData.setUserId("user2");
            userData.setName("Non-Friend User");
            
            // 模拟服务 - 好友关系不存在
            given(imUserDataDubboService.queryOne("user2")).willReturn(userData);
            given(imFriendshipDubboService.queryOne("user1", "user2")).willReturn(null);
            
            // 执行测试并验证
            StepVerifier.create(relationshipService.getFriendInfo(friendDto))
                    .expectNextMatches(vo -> 
                        vo.getFriendId().equals("user2") && 
                        vo.getFlag() == 0) // 不是好友
                    .verifyComplete();
        }

        @Test
        @DisplayName("当用户不存在时_getFriendInfo方法应抛出用户未找到异常")
        void getFriendInfo_WithNonExistingUser_ShouldThrowError() {
            // 准备测试数据
            FriendDto friendDto = new FriendDto();
            friendDto.setFromId("user1");
            friendDto.setToId("user999");
            
            // 模拟服务返回 null
            given(imUserDataDubboService.queryOne("user999")).willReturn(null);
            
            // 执行测试并验证
            StepVerifier.create(relationshipService.getFriendInfo(friendDto))
                    .expectError(BusinessException.class)
                    .verify();
        }

        @Test
        @DisplayName("当FriendDto为null时_getFriendInfo方法应抛出异常")
        void getFriendInfo_WithNullFriendDto_ShouldThrowError() {
            // 执行测试并验证
            StepVerifier.create(relationshipService.getFriendInfo(null))
                    .expectError(BusinessException.class)
                    .verify();
        }
    }

    // ==================== addFriend 方法测试 ====================

    @Nested
    @DisplayName("addFriend 方法测试")
    class AddFriendMethodTests {

        @Test
        @DisplayName("当首次添加好友时_addFriend方法应创建新请求并返回成功信息")
        void addFriend_WithNewRequest_ShouldCreateRequestAndReturnSuccess() {
            // 准备测试数据
            FriendRequestDto requestDto = new FriendRequestDto();
            requestDto.setFromId("user1");
            requestDto.setToId("user2");
            requestDto.setMessage("Hello!");
            
            // 模拟服务 - 不存在已有请求
            given(imFriendshipRequestDubboService.queryOne(any(ImFriendshipRequestPo.class)))
                    .willReturn(null);
            given(imFriendshipRequestDubboService.creat(any(ImFriendshipRequestPo.class)))
                    .willReturn(true);
            
            // 执行测试并验证
            StepVerifier.create(relationshipService.addFriend(requestDto))
                    .expectNextMatches(result -> result.contains("成功"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("当已存在好友请求时_addFriend方法应更新请求并返回成功信息")
        void addFriend_WithExistingRequest_ShouldUpdateRequestAndReturnSuccess() {
            // 准备测试数据
            FriendRequestDto requestDto = new FriendRequestDto();
            requestDto.setFromId("user1");
            requestDto.setToId("user2");
            requestDto.setMessage("Hello again!");
            
            ImFriendshipRequestPo existingRequest = new ImFriendshipRequestPo();
            existingRequest.setId("req123");
            existingRequest.setFromId("user1");
            existingRequest.setToId("user2");
            
            // 模拟服务 - 存在已有请求
            given(imFriendshipRequestDubboService.queryOne(any(ImFriendshipRequestPo.class)))
                    .willReturn(existingRequest);
            given(imFriendshipRequestDubboService.modify(any(ImFriendshipRequestPo.class)))
                    .willReturn(true);
            
            // 执行测试并验证
            StepVerifier.create(relationshipService.addFriend(requestDto))
                    .expectNextMatches(result -> result.contains("成功"))
                    .verifyComplete();
        }
    }

    // ==================== approveFriend 方法测试 ====================

    @Nested
    @DisplayName("approveFriend 方法测试")
    class ApproveFriendMethodTests {

        @Test
        @DisplayName("当审批通过好友请求时_approveFriend方法应创建双向好友关系")
        void approveFriend_WithApproval_ShouldCreateBidirectionalFriendship() {
            // 准备测试数据
            FriendRequestDto requestDto = new FriendRequestDto();
            requestDto.setId("req123");
            requestDto.setApproveStatus(1); // 通过
            
            ImFriendshipRequestPo request = new ImFriendshipRequestPo();
            request.setId("req123");
            request.setFromId("user1");
            request.setToId("user2");
            
            // 模拟服务
            given(imFriendshipRequestDubboService.queryOne(any(ImFriendshipRequestPo.class)))
                    .willReturn(request);
            given(imFriendshipDubboService.creat(any(ImFriendshipPo.class)))
                    .willReturn(true);
            given(imFriendshipRequestDubboService.modifyStatus(anyString(), anyInt()))
                    .willReturn(true);
            
            // 执行测试并验证
            StepVerifier.create(relationshipService.approveFriend(requestDto))
                    .verifyComplete();
            
            // 验证创建了双向好友关系
            verify(imFriendshipDubboService, times(2)).creat(any(ImFriendshipPo.class));
        }

        @Test
        @DisplayName("当好友请求不存在时_approveFriend方法应抛出请求不存在异常")
        void approveFriend_WithNonExistingRequest_ShouldThrowError() {
            // 准备测试数据
            FriendRequestDto requestDto = new FriendRequestDto();
            requestDto.setId("req999");
            requestDto.setApproveStatus(1);
            
            // 模拟服务返回 null
            given(imFriendshipRequestDubboService.queryOne(any(ImFriendshipRequestPo.class)))
                    .willReturn(null);
            
            // 执行测试并验证
            StepVerifier.create(relationshipService.approveFriend(requestDto))
                    .expectError(GlobalException.class)
                    .verify();
        }
    }

    // ==================== delFriend 方法测试 ====================

    @Nested
    @DisplayName("delFriend 方法测试")
    class DelFriendMethodTests {

        @Test
        @DisplayName("当删除好友成功时_delFriend方法应正常完成")
        void delFriend_WithValidFriend_ShouldCompleteSuccessfully() {
            // 准备测试数据
            FriendDto friendDto = new FriendDto();
            friendDto.setFromId("user1");
            friendDto.setToId("user2");
            
            // 模拟服务
            given(imFriendshipDubboService.removeOne("user1", "user2")).willReturn(true);
            
            // 执行测试并验证
            StepVerifier.create(relationshipService.delFriend(friendDto))
                    .verifyComplete();
            
            // 验证删除被调用
            verify(imFriendshipDubboService).removeOne("user1", "user2");
        }
    }

    // ==================== updateFriendRemark 方法测试 ====================

    @Nested
    @DisplayName("updateFriendRemark 方法测试")
    class UpdateFriendRemarkMethodTests {

        @Test
        @DisplayName("当更新好友备注成功时_updateFriendRemark方法应返回true")
        void updateFriendRemark_WithValidFriendship_ShouldReturnTrue() {
            // 准备测试数据
            FriendDto friendDto = new FriendDto();
            friendDto.setFromId("user1");
            friendDto.setToId("user2");
            friendDto.setRemark("新备注");
            
            ImFriendshipPo friendship = new ImFriendshipPo();
            friendship.setOwnerId("user1");
            friendship.setToId("user2");
            
            // 模拟服务
            given(imFriendshipDubboService.queryOne("user1", "user2")).willReturn(friendship);
            given(imFriendshipDubboService.modify(any(ImFriendshipPo.class))).willReturn(true);
            
            // 执行测试并验证
            StepVerifier.create(relationshipService.updateFriendRemark(friendDto))
                    .expectNext(true)
                    .verifyComplete();
        }

        @Test
        @DisplayName("当好友关系不存在时_updateFriendRemark方法应抛出关系不存在异常")
        void updateFriendRemark_WithNonExistingFriendship_ShouldThrowError() {
            // 准备测试数据
            FriendDto friendDto = new FriendDto();
            friendDto.setFromId("user1");
            friendDto.setToId("user999");
            friendDto.setRemark("新备注");
            
            // 模拟服务返回 null
            given(imFriendshipDubboService.queryOne("user1", "user999")).willReturn(null);
            
            // 执行测试并验证
            StepVerifier.create(relationshipService.updateFriendRemark(friendDto))
                    .expectError(GlobalException.class)
                    .verify();
        }

        @Test
        @DisplayName("当更新好友备注失败时_updateFriendRemark方法应返回false")
        void updateFriendRemark_WhenUpdateFails_ShouldReturnFalse() {
            // 准备测试数据
            FriendDto friendDto = new FriendDto();
            friendDto.setFromId("user1");
            friendDto.setToId("user2");
            friendDto.setRemark("新备注");
            
            ImFriendshipPo friendship = new ImFriendshipPo();
            friendship.setOwnerId("user1");
            friendship.setToId("user2");
            
            // 模拟服务更新失败
            given(imFriendshipDubboService.queryOne("user1", "user2")).willReturn(friendship);
            given(imFriendshipDubboService.modify(any(ImFriendshipPo.class))).willReturn(false);
            
            // 执行测试并验证
            StepVerifier.create(relationshipService.updateFriendRemark(friendDto))
                    .expectNext(false)
                    .verifyComplete();
        }
    }
}

