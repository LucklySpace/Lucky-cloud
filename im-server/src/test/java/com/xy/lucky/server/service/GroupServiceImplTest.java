package com.xy.lucky.server.service;

import com.xy.lucky.core.enums.IMemberStatus;
import com.xy.lucky.domain.dto.GroupDto;
import com.xy.lucky.domain.dto.GroupInviteDto;
import com.xy.lucky.domain.dto.GroupMemberDto;
import com.xy.lucky.domain.po.ImGroupMemberPo;
import com.xy.lucky.domain.po.ImGroupPo;
import com.xy.lucky.domain.po.ImUserDataPo;
import com.xy.lucky.dubbo.web.api.database.group.ImGroupDubboService;
import com.xy.lucky.dubbo.web.api.database.group.ImGroupInviteRequestDubboService;
import com.xy.lucky.dubbo.web.api.database.group.ImGroupMemberDubboService;
import com.xy.lucky.dubbo.web.api.database.user.ImUserDataDubboService;
import com.xy.lucky.dubbo.web.api.id.ImIdDubboService;
import com.xy.lucky.server.exception.GroupException;
import com.xy.lucky.server.service.impl.GroupServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLockReactive;
import org.redisson.api.RMapCacheReactive;
import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonReactiveClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * GroupServiceImpl 单元测试类
 *
 * 测试群组服务的核心功能，包括群组创建、邀请、审批、退出、信息更新等操作。
 * 使用 Mockito 模拟 Dubbo 服务和 Redisson 客户端。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("群组服务测试")
class GroupServiceImplTest {

    @Mock
    private ImUserDataDubboService imUserDataDubboService;

    @Mock
    private ImGroupDubboService imGroupDubboService;

    @Mock
    private ImGroupMemberDubboService imGroupMemberDubboService;

    @Mock
    private ImGroupInviteRequestDubboService imGroupInviteRequestDubboService;

    @Mock
    private ImIdDubboService imIdDubboService;

    @Mock
    private MessageService messageService;

    @Mock
    private FileService fileService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RedissonReactiveClient reactiveClient;

    @Mock
    private RLockReactive lockReactive;

    @Mock
    private RMapCacheReactive<String, Object> mapCacheReactive;

    @InjectMocks
    private GroupServiceImpl groupService;

    /**
     * 每个测试方法执行前的初始化操作
     * 配置 Redisson 分布式锁和缓存的模拟行为
     */
    @BeforeEach
    void setUp() {
        // 模拟 Redisson 响应式客户端和分布式锁
        lenient().when(redissonClient.reactive()).thenReturn(reactiveClient);
        lenient().when(reactiveClient.getLock(anyString())).thenReturn(lockReactive);
        lenient().when(lockReactive.tryLock(anyLong(), anyLong(), any())).thenReturn(Mono.just(true));
        lenient().when(lockReactive.isHeldByThread(anyLong())).thenReturn(Mono.just(true));
        lenient().when(lockReactive.unlock()).thenReturn(Mono.empty());

        // 模拟 Redis 缓存
        lenient().when(reactiveClient.<String, Object>getMapCache(anyString()))
                .thenReturn(mapCacheReactive);
        lenient().when(mapCacheReactive.get(anyString())).thenReturn(Mono.empty());
        lenient().when(mapCacheReactive.fastPut(anyString(), any(), anyLong(), any())).thenReturn(Mono.just(true));
    }

    // ==================== getGroupMembers 方法测试 ====================

    @Nested
    @DisplayName("getGroupMembers 方法测试")
    class GetGroupMembersMethodTests {

        @Test
        @DisplayName("当群组有成员时_getGroupMembers方法应返回成员映射")
        void getGroupMembers_WithMembers_ShouldReturnMemberMap() {
            // 准备测试数据
            GroupDto groupDto = new GroupDto();
            groupDto.setGroupId("group123");

            ImGroupMemberPo member1 = new ImGroupMemberPo();
            member1.setGroupMemberId("gm1");
            member1.setGroupId("group123");
            member1.setMemberId("user1");
            member1.setRole(IMemberStatus.GROUP_OWNER.getCode());

            ImGroupMemberPo member2 = new ImGroupMemberPo();
            member2.setGroupMemberId("gm2");
            member2.setGroupId("group123");
            member2.setMemberId("user2");
            member2.setRole(IMemberStatus.NORMAL.getCode());

            ImUserDataPo user1 = new ImUserDataPo();
            user1.setUserId("user1");
            user1.setName("Owner");

            ImUserDataPo user2 = new ImUserDataPo();
            user2.setUserId("user2");
            user2.setName("Member");

            // 模拟服务
            given(imGroupMemberDubboService.queryList("group123"))
                    .willReturn(Arrays.asList(member1, member2));
            given(imUserDataDubboService.queryListByIds(anyList()))
                    .willReturn(Arrays.asList(user1, user2));

            // 执行测试并验证
            StepVerifier.create(groupService.getGroupMembers(groupDto))
                    .expectNextMatches(map ->
                        map.size() == 2 &&
                        map.containsKey("user1") &&
                        map.containsKey("user2"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("当群组没有成员时_getGroupMembers方法应返回空Map")
        void getGroupMembers_WithNoMembers_ShouldReturnEmptyMap() {
            // 准备测试数据
            GroupDto groupDto = new GroupDto();
            groupDto.setGroupId("group123");

            // 模拟服务返回空列表
            given(imGroupMemberDubboService.queryList("group123")).willReturn(List.of());

            // 执行测试并验证
            StepVerifier.create(groupService.getGroupMembers(groupDto))
                    .expectNextMatches(Map::isEmpty)
                    .verifyComplete();
        }
    }

    // ==================== quitGroup 方法测试 ====================

    @Nested
    @DisplayName("quitGroup 方法测试")
    class QuitGroupMethodTests {

        @Test
        @DisplayName("当普通成员退出群聊时_quitGroup方法应成功完成")
        void quitGroup_WithNormalMember_ShouldCompleteSuccessfully() {
            // 准备测试数据
            GroupDto groupDto = new GroupDto();
            groupDto.setGroupId("group123");
            groupDto.setUserId("user2");

            ImGroupMemberPo member = new ImGroupMemberPo();
            member.setGroupMemberId("gm2");
            member.setMemberId("user2");
            member.setRole(IMemberStatus.NORMAL.getCode());

            // 模拟服务
            given(imGroupMemberDubboService.queryOne("group123", "user2")).willReturn(member);
            given(imGroupMemberDubboService.removeOne("gm2")).willReturn(true);

            // 执行测试并验证
            StepVerifier.create(groupService.quitGroup(groupDto))
                    .verifyComplete();
        }

        @Test
        @DisplayName("当群主尝试退出群聊时_quitGroup方法应抛出群主不可退出异常")
        void quitGroup_WithGroupOwner_ShouldThrowError() {
            // 准备测试数据
            GroupDto groupDto = new GroupDto();
            groupDto.setGroupId("group123");
            groupDto.setUserId("user1");

            ImGroupMemberPo owner = new ImGroupMemberPo();
            owner.setGroupMemberId("gm1");
            owner.setMemberId("user1");
            owner.setRole(IMemberStatus.GROUP_OWNER.getCode());

            // 模拟服务
            given(imGroupMemberDubboService.queryOne("group123", "user1")).willReturn(owner);

            // 执行测试并验证
            StepVerifier.create(groupService.quitGroup(groupDto))
                    .expectErrorMatches(throwable ->
                        throwable instanceof GroupException &&
                        throwable.getMessage().contains("群主不可退出"))
                    .verify();
        }

        @Test
        @DisplayName("当用户不在群聊中时_quitGroup方法应抛出用户不在群聊中异常")
        void quitGroup_WithNonMember_ShouldThrowError() {
            // 准备测试数据
            GroupDto groupDto = new GroupDto();
            groupDto.setGroupId("group123");
            groupDto.setUserId("user999");

            // 模拟服务返回 null
            given(imGroupMemberDubboService.queryOne("group123", "user999")).willReturn(null);

            // 执行测试并验证
            StepVerifier.create(groupService.quitGroup(groupDto))
                    .expectErrorMatches(throwable ->
                        throwable instanceof GroupException &&
                        throwable.getMessage().contains("不在群聊中"))
                    .verify();
        }
    }

    // ==================== inviteGroup 方法测试 ====================

    @Nested
    @DisplayName("inviteGroup 方法测试")
    class InviteGroupMethodTests {
        @Test
        @DisplayName("当邀请类型为无效值时_inviteGroup方法应抛出无效邀请类型异常")
        void inviteGroup_WithInvalidType_ShouldThrowError() {
            // 准备测试数据
            GroupInviteDto dto = new GroupInviteDto();
            dto.setType(999); // 无效类型
            dto.setUserId("user1");

            // 执行测试并验证
            StepVerifier.create(groupService.inviteGroup(dto))
                    .expectErrorMatches(throwable ->
                        throwable instanceof GroupException &&
                        throwable.getMessage().contains("无效邀请类型"))
                    .verify();
        }
    }

    // ==================== groupInfo 方法测试 ====================

    @Nested
    @DisplayName("groupInfo 方法测试")
    class GroupInfoMethodTests {

        @Test
        @DisplayName("当群组信息在缓存中时_groupInfo方法应从缓存返回")
        void groupInfo_WithCachedInfo_ShouldReturnFromCache() {
            // 准备测试数据
            GroupDto groupDto = new GroupDto();
            groupDto.setGroupId("group123");

            ImGroupPo cachedGroup = new ImGroupPo();
            cachedGroup.setGroupId("group123");
            cachedGroup.setGroupName("Cached Group");

            // 模拟缓存命中
            when(mapCacheReactive.get("group123")).thenReturn(Mono.just(cachedGroup));

            // 执行测试并验证
            StepVerifier.create(groupService.groupInfo(groupDto))
                    .expectNextMatches(group ->
                        group.getGroupId().equals("group123") &&
                        group.getGroupName().equals("Cached Group"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("当群组信息不在缓存中时_groupInfo方法应从数据库查询并缓存")
        void groupInfo_WithNoCachedInfo_ShouldQueryFromDbAndCache() {
            // 准备测试数据
            GroupDto groupDto = new GroupDto();
            groupDto.setGroupId("group123");

            ImGroupPo dbGroup = new ImGroupPo();
            dbGroup.setGroupId("group123");
            dbGroup.setGroupName("DB Group");

            // 模拟缓存未命中，从数据库查询
            when(mapCacheReactive.get("group123")).thenReturn(Mono.empty());
            given(imGroupDubboService.queryOne("group123")).willReturn(dbGroup);

            // 执行测试并验证
            StepVerifier.create(groupService.groupInfo(groupDto))
                    .expectNextMatches(group ->
                        group.getGroupId().equals("group123") &&
                        group.getGroupName().equals("DB Group"))
                    .verifyComplete();
        }
    }

    // ==================== updateGroupInfo 方法测试 ====================

    @Nested
    @DisplayName("updateGroupInfo 方法测试")
    class UpdateGroupInfoMethodTests {

        @Test
        @DisplayName("当更新群组信息成功时_updateGroupInfo方法应返回true")
        void updateGroupInfo_WithValidData_ShouldReturnTrue() {
            // 准备测试数据
            GroupDto groupDto = new GroupDto();
            groupDto.setGroupId("group123");
            groupDto.setGroupName("New Group Name");

            ImGroupPo existingGroup = new ImGroupPo();
            existingGroup.setGroupId("group123");
            existingGroup.setGroupName("Old Name");

            // 模拟服务
            given(imGroupDubboService.queryOne("group123")).willReturn(existingGroup);
            given(imGroupDubboService.modify(any(ImGroupPo.class))).willReturn(true);

            // 执行测试并验证
            StepVerifier.create(groupService.updateGroupInfo(groupDto))
                    .expectNext(true)
                    .verifyComplete();
        }

        @Test
        @DisplayName("当群组不存在时_updateGroupInfo方法应抛出群组不存在异常")
        void updateGroupInfo_WithNonExistingGroup_ShouldThrowError() {
            // 准备测试数据
            GroupDto groupDto = new GroupDto();
            groupDto.setGroupId("group999");

            // 模拟服务返回 null
            given(imGroupDubboService.queryOne("group999")).willReturn(null);

            // 执行测试并验证
            StepVerifier.create(groupService.updateGroupInfo(groupDto))
                    .expectErrorMatches(throwable ->
                        throwable instanceof GroupException &&
                        throwable.getMessage().contains("群组不存在"))
                    .verify();
        }

        @Test
        @DisplayName("当更新群组信息失败时_updateGroupInfo方法应抛出更新失败异常")
        void updateGroupInfo_WhenUpdateFails_ShouldThrowError() {
            // 准备测试数据
            GroupDto groupDto = new GroupDto();
            groupDto.setGroupId("group123");
            groupDto.setGroupName("New Name");

            ImGroupPo existingGroup = new ImGroupPo();
            existingGroup.setGroupId("group123");

            // 模拟服务更新失败
            given(imGroupDubboService.queryOne("group123")).willReturn(existingGroup);
            given(imGroupDubboService.modify(any(ImGroupPo.class))).willReturn(false);

            // 执行测试并验证
            StepVerifier.create(groupService.updateGroupInfo(groupDto))
                    .expectErrorMatches(throwable ->
                        throwable instanceof GroupException &&
                        throwable.getMessage().contains("更新失败"))
                    .verify();
        }
    }

    // ==================== updateGroupMember 方法测试 ====================

    @Nested
    @DisplayName("updateGroupMember 方法测试")
    class UpdateGroupMemberMethodTests {

        @Test
        @DisplayName("当更新群成员信息成功时_updateGroupMember方法应返回true")
        void updateGroupMember_WithValidData_ShouldReturnTrue() {
            // 准备测试数据
            GroupMemberDto memberDto = new GroupMemberDto();
            memberDto.setGroupId("group123");
            memberDto.setUserId("user1");
            memberDto.setAlias("New Alias");

            ImGroupMemberPo member = new ImGroupMemberPo();
            member.setGroupMemberId("gm1");
            member.setMemberId("user1");

            // 模拟服务
            given(imGroupMemberDubboService.queryOne("group123", "user1")).willReturn(member);
            given(imGroupMemberDubboService.modify(any(ImGroupMemberPo.class))).willReturn(true);

            // 执行测试并验证
            StepVerifier.create(groupService.updateGroupMember(memberDto))
                    .expectNext(true)
                    .verifyComplete();
        }

        @Test
        @DisplayName("当用户不在群聊中时_updateGroupMember方法应抛出用户不在群聊中异常")
        void updateGroupMember_WithNonMember_ShouldThrowError() {
            // 准备测试数据
            GroupMemberDto memberDto = new GroupMemberDto();
            memberDto.setGroupId("group123");
            memberDto.setUserId("user999");

            // 模拟服务返回 null
            given(imGroupMemberDubboService.queryOne("group123", "user999")).willReturn(null);

            // 执行测试并验证
            StepVerifier.create(groupService.updateGroupMember(memberDto))
                    .expectErrorMatches(throwable ->
                        throwable instanceof GroupException &&
                        throwable.getMessage().contains("不在群聊中"))
                    .verify();
        }

        @Test
        @DisplayName("当更新群成员信息失败时_updateGroupMember方法应抛出更新失败异常")
        void updateGroupMember_WhenUpdateFails_ShouldThrowError() {
            // 准备测试数据
            GroupMemberDto memberDto = new GroupMemberDto();
            memberDto.setGroupId("group123");
            memberDto.setUserId("user1");
            memberDto.setAlias("New Alias");

            ImGroupMemberPo member = new ImGroupMemberPo();
            member.setGroupMemberId("gm1");

            // 模拟服务更新失败
            given(imGroupMemberDubboService.queryOne("group123", "user1")).willReturn(member);
            given(imGroupMemberDubboService.modify(any(ImGroupMemberPo.class))).willReturn(false);

            // 执行测试并验证
            StepVerifier.create(groupService.updateGroupMember(memberDto))
                    .expectErrorMatches(throwable ->
                        throwable instanceof GroupException &&
                        throwable.getMessage().contains("更新失败"))
                    .verify();
        }
    }
}

