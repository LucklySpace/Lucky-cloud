package com.xy.lucky.server.service;


import com.xy.lucky.domain.po.ImGroupPo;
import com.xy.lucky.server.domain.dto.GroupDto;
import com.xy.lucky.server.domain.dto.GroupInviteDto;
import com.xy.lucky.server.domain.dto.GroupMemberDto;

import java.util.Map;

/**
 * 群组服务接口
 *
 * @author xy
 */
public interface GroupService {

    /**
     * 获取群成员列表
     */
    Map<?, ?> getGroupMembers(GroupDto groupDto);

    /**
     * 退出群聊
     */
    void quitGroup(GroupDto groupDto);

    /**
     * 邀请/创建群组
     */
    String inviteGroup(GroupInviteDto groupInviteDto);

    /**
     * 获取群信息
     */
    ImGroupPo groupInfo(GroupDto groupDto);

    /**
     * 更新群信息
     */
    Boolean updateGroupInfo(GroupDto groupDto);

    /**
     * 审批群邀请
     */
    String approveGroupInvite(GroupInviteDto groupInviteDto);

    /**
     * 更新群成员信息
     */
    Boolean updateGroupMember(GroupMemberDto groupMemberDto);

    // ==================== 群管理操作 ====================

    /**
     * 踢出群成员
     *
     * @param dto 包含 groupId、userId（操作者）、targetUserId（被踢人）
     */
    void kickMember(GroupMemberDto dto);

    /**
     * 设置/取消管理员
     *
     * @param dto 包含 groupId、userId（群主）、targetUserId（目标成员）、role
     */
    void setAdmin(GroupMemberDto dto);

    /**
     * 移交群主
     *
     * @param dto 包含 groupId、userId（当前群主）、targetUserId（新群主）
     */
    void transferOwner(GroupMemberDto dto);

    /**
     * 设置群加入方式
     *
     * @param dto 包含 groupId、userId（管理员）、applyJoinType
     */
    void setJoinMode(GroupDto dto);

    /**
     * 禁言/取消禁言成员
     *
     * @param dto 包含 groupId、userId（操作者）、targetUserId（被禁言人）、mute
     */
    void muteMember(GroupMemberDto dto);

    /**
     * 全员禁言/取消全员禁言
     *
     * @param dto 包含 groupId、userId（管理员）、muteAll
     */
    void muteAll(GroupDto dto);

    /**
     * 解散群组
     *
     * @param dto 包含 groupId、userId（群主）
     */
    void dismissGroup(GroupDto dto);

    /**
     * 设置群公告
     *
     * @param dto 包含 groupId、userId（管理员）、notification
     */
    void setAnnouncement(GroupDto dto);
}
