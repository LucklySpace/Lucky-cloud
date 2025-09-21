package com.xy.server.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.xy.core.enums.IMStatus;
import com.xy.core.enums.IMemberStatus;
import com.xy.core.enums.IMessageContentType;
import com.xy.core.enums.IMessageType;
import com.xy.core.model.IMGroupMessage;
import com.xy.core.model.IMessageDto;
import com.xy.domain.dto.GroupDto;
import com.xy.domain.dto.GroupInviteDto;
import com.xy.domain.po.ImGroupMemberPo;
import com.xy.domain.po.ImGroupPo;
import com.xy.domain.po.ImUserDataPo;
import com.xy.domain.vo.GroupMemberVo;
import com.xy.general.response.domain.Result;
import com.xy.general.response.domain.ResultCode;
import com.xy.server.api.database.group.ImGroupFeign;
import com.xy.server.api.database.user.ImUserFeign;
import com.xy.server.api.id.IdGeneratorConstant;
import com.xy.server.api.id.ImIdGeneratorFeign;
import com.xy.server.exception.GlobalException;
import com.xy.server.service.FileService;
import com.xy.server.service.GroupService;
import com.xy.server.service.MessageService;
import com.xy.server.utils.RedisUtil;
import com.xy.utils.DateTimeUtil;
import com.xy.utils.GroupHeadImageUtil;
import jakarta.annotation.Resource;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Slf4j
@Service
public class GroupServiceImpl implements GroupService {

    // 头像工具类，用于生成九宫格
    GroupHeadImageUtil groupHeadImageUtil = new GroupHeadImageUtil();

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private ImGroupFeign imGroupFeign;

    @Resource
    private ImUserFeign imUserFeign;

    @Resource
    private ImIdGeneratorFeign imIdGeneratorFeign;

    @Resource
    private FileService fileService;

    @Resource
    private MessageService messageService;

    /**
     * 获取群成员信息
     *
     * @param groupDto 群信息
     * @return 群成员集合
     */
    @Override
    public Result<?> getGroupMembers(GroupDto groupDto) {
        // 查询群成员列表
        List<ImGroupMemberPo> imGroupMemberPos = imGroupFeign.getGroupMemberList(groupDto.getGroupId());

        // 提取成员ID列表
        List<String> memberIdList = imGroupMemberPos.stream()
                .map(ImGroupMemberPo::getMemberId)
                .collect(Collectors.toList());

        // 根据成员ID列表查询用户信息，并将其存储到 Map 中以便快速查找
        Map<String, ImUserDataPo> userDataMap = imUserFeign.getUserByIds(memberIdList)
                .stream()
                .collect(Collectors.toMap(ImUserDataPo::getUserId, Function.identity()));

        // 构建成员信息的 Map
        Map<String, GroupMemberVo> groupMemberVoMap = new HashMap<>();

        for (ImGroupMemberPo imGroupMemberPo : imGroupMemberPos) {

            ImUserDataPo imUserDataPo = userDataMap.get(imGroupMemberPo.getMemberId());

            if (imUserDataPo != null) {
                GroupMemberVo groupMemberVo = new GroupMemberVo();

                BeanUtils.copyProperties(imUserDataPo, groupMemberVo);

//                groupMemberVo.setRole(imGroupMemberPo.getRole())
//                        .setMute(imGroupMemberPo.getMute())
//                        .setAlias(imGroupMemberPo.getAlias())
//                       .setJoinType(imGroupMemberPo.getJoinType());

                groupMemberVoMap.put(imUserDataPo.getUserId(), groupMemberVo);
            }
        }

        return Result.success(groupMemberVoMap);
    }

    /**
     * 退出群聊
     *
     * @param groupDto 退出成员信息
     */
    @Override
    public void quitGroup(GroupDto groupDto) {

        String groupId = groupDto.getGroupId();

        String userId = groupDto.getUserId();

        // 查询群成员关系
        ImGroupMemberPo imGroupMemberPo = imGroupFeign.getOneMember(groupId, userId);

        // 获取角色
        Integer role = imGroupMemberPo.getRole();

        // 判断是否群主
        if (role.equals(IMemberStatus.GROUP_OWNER.getCode())) {
            throw new GlobalException(ResultCode.FAIL, "群主不可退出群聊");
        }

//        // 删除群成员关系
//        if (imGroupFeign.deleteById(imGroupMemberPo.getGroupMemberId())) {
//
//            log.info("退出群聊成功，群聊id:{},用户id:{}", groupId, userId);
//        } else {
//
//            log.error("退出群聊失败，群聊id:{},用户id:{}", groupId, userId);
//        }

    }

    /**
     * 邀请成员
     *
     * @param groupInviteDto
     */
    @Override
    public Result inviteGroup(GroupInviteDto groupInviteDto) {

        Integer type = groupInviteDto.getType();

        // 新建群聊
        if (type.equals(IMessageType.SINGLE_MESSAGE.getCode())) {
            return createGroup(groupInviteDto);
        }

        // 邀请入群
        if (type.equals(IMessageType.GROUP_MESSAGE.getCode())) {
            return groupInvite(groupInviteDto);
        }

        return null;
    }

    /**
     * 创建新群
     *
     * @param dto
     * @return 群聊id
     */
    public Result<String> createGroup(@NonNull GroupInviteDto dto) {
        // 1. 校验参数
        if (dto.getMemberIds() == null || dto.getMemberIds().isEmpty()) {
            throw new GlobalException(ResultCode.FAIL, "至少需要一个被邀请人");
        }

        // 2. 生成群ID与默认群名称
        String groupId = imIdGeneratorFeign.getId(
                IdGeneratorConstant.uuid,
                IdGeneratorConstant.group_message_id,
                String.class
        );

        String groupName = "默认群聊-" + RandomUtil.randomNumbers(6);

        // 3. 获取当前 UTC 时间戳
        long now = DateTimeUtil.getCurrentUTCTimestamp();

        String userId = dto.getUserId();
        List<String> friendIds = dto.getMemberIds();

        // 4. 构造群成员列表
        List<ImGroupMemberPo> members = new ArrayList<>();

        // 4.1 群主
        members.add(buildMember(groupId, userId, IMemberStatus.GROUP_OWNER, now));

        // 4.2 普通成员
        friendIds.forEach(fid ->
                members.add(buildMember(groupId, fid, IMemberStatus.NORMAL, now))
        );

        // 5. 批量插入成员关系
        boolean membersOk = imGroupFeign.groupMessageMemberBatchInsert(members);

        if (!membersOk) {
            log.error("群成员插入失败, groupId={}, owner={}, members={}", groupId, userId, friendIds);
            throw new GlobalException(ResultCode.FAIL, "群成员插入失败");
        }

        // 6. 构造群信息并插入
        ImGroupPo group = new ImGroupPo()
                // 群id
                .setGroupId(groupId)
                // 群主
                .setOwnerId(userId)
                // 群类型
                .setGroupType(1)
                // 群名称
                .setGroupName(groupName)
                .setApplyJoinType(1)
                // 群头像
                .setAvatar(generateGroupAvatar(groupId))
                // 群状态
                .setStatus(IMStatus.YES.getCode())
                // 创建时间
                .setCreateTime(now)
                // 删除标志
                .setDelFlag(IMStatus.YES.getCode());

        boolean groupOk = imGroupFeign.insert(group);

        if (!groupOk) {
            log.error("群信息插入失败, groupId={}, owner={}, members={}", groupId, userId, friendIds);
            throw new GlobalException(ResultCode.FAIL, "群信息插入失败");
        }
        log.info("新建群聊成功, groupId={}, owner={}, members={}", groupId, userId, friendIds);

        // 发送系统群聊邀请消息,系统消息默认用户000000
        //IMGroupMessage IMGroupMessageDto = systemMessage(groupId, "加入群聊,请尽情聊天吧");
        // 发送群聊消息
        messageService.sendGroupMessage(systemMessage(groupId, "已加入群聊,请尽情聊天吧"));

        return Result.success(groupId);
    }

    /**
     * 群聊邀请
     */
    public Result<String> groupInvite(@NonNull GroupInviteDto dto) {

        // 1. 确定群ID（允许新建群）
        String groupId = StringUtils.hasText(dto.getGroupId())
                ? dto.getGroupId()
                : UUID.randomUUID().toString();

        // 邀请者id
        String inviter = dto.getUserId();

        // 被邀请
        List<String> invitees = Optional.ofNullable(dto.getMemberIds())
                .orElseGet(Collections::emptyList);

        // 2. 获取现有群成员
        Set<String> existing = imGroupFeign.getGroupMemberList(groupId).stream()
                .map(ImGroupMemberPo::getMemberId)
                .collect(Collectors.toSet());

        // 3. 校验邀请者必须已在群中
        if (!existing.contains(inviter)) {
            throw new GlobalException(ResultCode.FAIL, "用户不在该群组中，不可邀请新成员");
        }

        // 4. 过滤出真正的新成员
        List<String> newMembers = invitees.stream()
                .filter(id -> !existing.contains(id))
                .distinct()
                .collect(Collectors.toList());

        // 5. 如果有新成员则批量插入
        if (!newMembers.isEmpty()) {
            long now = DateTimeUtil.getCurrentUTCTimestamp();

            List<ImGroupMemberPo> memberPos = newMembers.stream()
                    .map(memberId -> buildMember(groupId, memberId, IMemberStatus.NORMAL, now))
                    .collect(Collectors.toList());

            boolean ok = imGroupFeign.groupMessageMemberBatchInsert(memberPos);
            if (!ok) {
                log.error("群成员插入失败，groupId={}, inviter={}, invitees={}", groupId, inviter, newMembers);
                throw new GlobalException(ResultCode.FAIL, "新增群成员失败");
            }
            log.info("群成员插入成功，groupId={}, inviter={}, newMembers={}", groupId, inviter, newMembers);
        }

        // 6. 更新群信息，小于10人换重设头像
        if (existing.size() < 10) {
            ImGroupPo update = new ImGroupPo()
                    .setGroupId(groupId)
                    .setAvatar(generateGroupAvatar(groupId));

            boolean updated = imGroupFeign.updateById(update);
            if (updated) {
                log.info("群信息更新成功，groupId={}", groupId);
            } else {
                log.warn("群信息更新失败，groupId={}", groupId);
            }
        }

        return Result.success(groupId);
    }

    /**
     * 构造一个群成员记录
     */
    private ImGroupMemberPo buildMember(
            String groupId,
            String memberId,
            IMemberStatus role,
            long joinTime
    ) {
        return new ImGroupMemberPo()
                // 群id
                .setGroupId(groupId)
                // 成员表id
                .setGroupMemberId(IdUtil.getSnowflakeNextIdStr())
                // 成员id
                .setMemberId(memberId)
                // 角色
                .setRole(role.getCode())
                // 是否禁言
                .setMute(IMStatus.YES.getCode())
                // 删除标志
                .setDelFlag(IMStatus.YES.getCode())
                // 加入时间
                .setJoinTime(joinTime);
    }

    /**
     * 系统消息
     *
     * @param groupId 群聊id
     * @param message 消息内容
     * @return
     */
    public IMGroupMessage systemMessage(String groupId, String message) {
        IMGroupMessage imGroupMessage = new IMGroupMessage();
        imGroupMessage.setGroupId(groupId)
                .setFromId("000000") // 系统消息默认用户000000
                .setMessageContentType(IMessageContentType.TIP.getCode()) // 系统消息
                .setMessageBody(new IMessageDto.TextMessageBody().setText(message)); // 群聊邀请消息
        return imGroupMessage;
    }


    /**
     * 获取群信息
     *
     * @param groupDto 群信息
     * @return
     */
    @Override
    public Result<?> groupInfo(@NonNull GroupDto groupDto) {
        return Result.success(imGroupFeign.getOne(groupDto.getGroupId()));
    }

    /**
     * 生成群聊头像
     *
     * @param groupId 群聊id
     * @return 头像url
     */
    public String generateGroupAvatar(String groupId) {

        // 随机查询 9 个群成员头像
        List<String> avatarList = imGroupFeign.getNinePeopleAvatar(groupId);

        // 生成群聊头像
        File groupHead = groupHeadImageUtil.getCombinationOfhead(avatarList, "defaultGroupHead" + groupId);

        // 转化文件
        MultipartFile multipartFile = fileService.fileToImageMultipartFile(groupHead);

        // TODO  临时方案 后面要改 上传文件
        return fileService.uploadFile(multipartFile).getPath();
    }

}
