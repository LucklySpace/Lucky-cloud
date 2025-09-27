package com.xy.server.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.xy.core.constants.IMConstant;
import com.xy.core.enums.*;
import com.xy.core.model.IMGroupMessage;
import com.xy.core.model.IMSingleMessage;
import com.xy.core.model.IMessage;
import com.xy.domain.dto.GroupDto;
import com.xy.domain.dto.GroupInviteDto;
import com.xy.domain.po.ImGroupInviteRequestPo;
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

        // 创建群聊
        if (type.equals(IMessageType.CREATE_GROUP.getCode())) {
            return createGroup(groupInviteDto);
        }

        // 邀请入群
        if (type.equals(IMessageType.GROUP_INVITE.getCode())) {
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
                // 群加入方式
                .setApplyJoinType(ImGroupJoinStatus.FREE.getCode())
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
    public Result groupInvite(@NonNull GroupInviteDto dto) {

        // 1. 确定群ID（允许新建群）
        String groupId = StringUtils.hasText(dto.getGroupId())
                ? dto.getGroupId()
                : UUID.randomUUID().toString();

        // 邀请者id
        String inviterId = dto.getUserId();

        // 被邀请者
        List<String> invitees = Optional.ofNullable(dto.getMemberIds())
                .orElseGet(Collections::emptyList);

        // 2. 获取现有群成员
        Set<String> existing = imGroupFeign.getGroupMemberList(groupId).stream()
                .map(ImGroupMemberPo::getMemberId)
                .collect(Collectors.toSet());

        // 3. 校验邀请者必须已在群中
        if (!existing.contains(inviterId)) {
            throw new GlobalException(ResultCode.FAIL, "用户不在该群组中，不可邀请新成员");
        }

        // 4. 过滤出真正的新成员
        List<String> newMembers = invitees.stream()
                .filter(id -> !existing.contains(id))
                .distinct()
                .collect(Collectors.toList());

        if (newMembers.isEmpty()) {
            // 没有需要邀请的新成员，直接返回
            return Result.success(groupId);
        }

        // 5. 构建并保存邀请请求记录到数据库（ImGroupInviteRequestPo）
        long now = DateTimeUtil.getCurrentUTCTimestamp();
        long expireSeconds = 7L * 24 * 3600; // 默认 7 天过期（按秒）
        long expireTime = now + expireSeconds;

        // 尝试查询群信息以确定 verifierId（默认群主）
        ImGroupPo groupPo = imGroupFeign.getOneGroup(groupId);
        String defaultVerifierId = null;
        if (groupPo != null && StringUtils.hasText(groupPo.getOwnerId())) {
            defaultVerifierId = groupPo.getOwnerId();
        }

        List<ImGroupInviteRequestPo> inviteRequestPos = new ArrayList<>(newMembers.size());

        for (String toId : newMembers) {

            String requestId = imIdGeneratorFeign.getId(
                    IdGeneratorConstant.uuid,
                    IdGeneratorConstant.group_invite_id,
                    String.class
            );

            ImGroupInviteRequestPo po = new ImGroupInviteRequestPo();
            po.setRequestId(requestId);
            po.setGroupId(groupId);
            po.setFromId(inviterId);
            po.setToId(toId);
            po.setVerifierId(defaultVerifierId);
            po.setVerifierStatus(0);       // 0: 待处理（群主/管理员）
            po.setMessage(dto.getMessage()); // 邀请验证信息（可为空）
            po.setApproveStatus(0);         // 0: 待被邀请人处理
            po.setAddSource(dto.getAddSource());
            po.setExpireTime(expireTime);
            po.setCreateTime(now);
            po.setDelFlag(1);
            // version 字段由 DB/ORM 自动维护（不在此设置）
            inviteRequestPos.add(po);
        }

        try {
            Boolean dbOk = imGroupFeign.groupInviteSaveOrUpdateBatch(inviteRequestPos);

            // 获取邀请者信息
            ImUserDataPo inviterInfo = imUserFeign.getOne(inviterId);

            for (ImGroupInviteRequestPo inviteRequestPo : inviteRequestPos) {
                // 构造单聊消息对象
                IMSingleMessage singleMessage = IMSingleMessage.builder()
                        .messageTempId(UUID.randomUUID().toString())
                        .fromId(inviteRequestPo.getFromId())
                        .toId(inviteRequestPo.getToId())
                        .messageContentType(8) // 群聊邀请消息类型
                        .messageTime(DateTimeUtil.getCurrentUTCTimestamp())
                        .messageType(IMessageType.SINGLE_MESSAGE.getCode())
                        .build();

                // 构造群聊邀请消息内容
                IMessage.MessageBody messageBody = new IMessage.GroupInviteMessageBody()
                        .setRequestId(inviteRequestPo.getRequestId())
                        .setInviterId(inviteRequestPo.getFromId())
                        .setGroupId(groupId)
                        .setUserId(inviteRequestPo.getToId())
                        .setGroupAvatar(groupPo != null ? groupPo.getAvatar() : "")
                        .setGroupName(groupPo != null ? groupPo.getGroupName() : "默认群聊")
                        .setInviterName(inviterInfo != null ? inviterInfo.getName() : "")
                        .setApproveStatus(0) // 邀请状态 1-待处理
                        ;


                singleMessage.setMessageBody(messageBody);

                // 发送单聊消息
                messageService.sendSingleMessage(singleMessage);
            }

            if (dbOk == null || !dbOk) {
                log.error("保存邀请请求到 DB 失败, groupId={}, inviter={}, invitees={}", groupId, inviterId, newMembers);
                return Result.failed("保存邀请请求失败");
            }
        } catch (Exception e) {
            log.error("调用 imGroupFeign.groupInviteSaveOrUpdateBatch 异常, groupId={}, inviter={}, invitees={}", groupId, inviterId, newMembers, e);
            return Result.failed("保存邀请请求异常");
        }

//        // 6. 保存成功后发送私聊邀请消息给被邀请人（保持原有行为）
//        if (!newMembers.isEmpty()) {
//            sendGroupInviteMessages(groupId, inviter, newMembers);
//        }

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
                .setFromId(IMConstant.SYSTEM) // 系统消息默认用户000000
                .setMessageContentType(IMessageContentType.TIP.getCode()) // 系统消息
                .setMessageBody(new IMessage.TextMessageBody().setText(message)); // 群聊邀请消息
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
        return Result.success(imGroupFeign.getOneGroup(groupDto.getGroupId()));
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

    @Override
    public Result approveGroupInvite(GroupInviteDto groupInviteDto) {

        String groupId = groupInviteDto.getGroupId();
        String userId = groupInviteDto.getUserId();
        String inviterId = groupInviteDto.getInviterId();
        Integer approveStatus = groupInviteDto.getApproveStatus();

        if (!StringUtils.hasText(groupId) || !StringUtils.hasText(userId) || approveStatus == null) {
            log.info("用户信息不完整，groupId={}, userId={}", groupId, userId);
            return Result.success("信息不完整");
        }

        // 状态映射 0:待处理, 1:同意, 2:拒绝
        if (Integer.valueOf(0).equals(approveStatus)) {
            log.info("用户待处理群聊邀请，groupId={}, userId={}", groupId, userId);
            return Result.success("待处理群聊邀请");
        }
        if (Integer.valueOf(2).equals(approveStatus)) {
            log.info("用户拒绝群聊邀请，groupId={}, userId={}", groupId, userId);
            return Result.success("已拒绝群聊邀请");
        }

        // --- 用户已同意邀请，进入后续流程 ---
        ImGroupPo groupPo = imGroupFeign.getOneGroup(groupId);

        if (Objects.isNull(groupPo)) {
            log.info("群不存在, groupId={}", groupId);
            return Result.success("群不存在");
        }

        // 1) 若群禁止加入 -> 直接返回
        if (Objects.equals(groupPo.getApplyJoinType(), ImGroupJoinStatus.BAN.getCode())) {
            log.info("群不允许加入，groupId={}, userId={}", groupId, userId);
            return Result.success("群不允许加入");
        }

        // 2) 若群需要群主/管理员审核 -> 发送验证消息到群主/管理员
        if (Objects.equals(groupPo.getApplyJoinType(), ImGroupJoinStatus.APPROVE.getCode())) {
            try {
                sendJoinApprovalRequestToAdmins(groupId, inviterId, userId, groupPo);
                log.info("已向群主/管理员发送入群验证请求，groupId={}, userId={}", groupId, userId);
                return Result.success("已发送入群验证请求，等待群主/管理员审核");
            } catch (Exception e) {
                log.error("发送入群验证请求失败，groupId={}, userId={}", groupId, userId, e);
                return Result.failed("发送验证请求失败，请重试");
            }
        }

        // 3) 群允许直接加入（FREE_JOIN 等） -> 直接加入
        // 检查是否已在群中
        ImGroupMemberPo member = imGroupFeign.getOneMember(groupId, userId);
        if (Objects.nonNull(member) && Objects.equals(member.getRole(), IMemberStatus.NORMAL.getCode())) {
            log.warn("用户已加入群聊，groupId={}, userId={}", groupId, userId);
            return Result.failed("用户已加入群聊");
        }

        // 添加群成员
        if (Objects.isNull(member)) {
            long now = DateTimeUtil.getCurrentUTCTimestamp();
            ImGroupMemberPo newMember = buildMember(groupId, userId, IMemberStatus.NORMAL, now);
            boolean result = imGroupFeign.groupMessageMemberBatchInsert(List.of(newMember));
            if (!result) {
                log.error("用户加入群聊失败，groupId={}, userId={}", groupId, userId);
                return Result.failed("加入群聊失败");
            }
        } else {
            // 若存在但不是 NORMAL，则可能需要提升角色逻辑，这里视需求而定
            log.warn("用户已在群聊中（非普通成员），groupId={}, userId={}", groupId, userId);
            return Result.failed("用户已在群聊中");
        }

        // 更新群头像（如果成员少于10人）
        List<ImGroupMemberPo> members = imGroupFeign.getGroupMemberList(groupId);
        if (members.size() < 10) {
            ImGroupPo update = new ImGroupPo()
                    .setGroupId(groupId)
                    .setAvatar(generateGroupAvatar(groupId));
            imGroupFeign.updateById(update);
        }

        // 4) 发送系统消息通知群成员新成员加入
        ImUserDataPo inviteeInfo = imUserFeign.getOne(userId);
        ImUserDataPo inviterInfo = imUserFeign.getOne(inviterId);

        String message = "\"" + (inviterInfo != null ? inviterInfo.getName() : inviterId)
                + "\" 邀请 \""
                + (inviteeInfo != null ? inviteeInfo.getName() : userId)
                + "\" 加入群聊";

        messageService.sendGroupMessage(systemMessage(groupId, message));

        return Result.success("成功加入群聊");
    }

    /**
     * 向群主/管理员发送入群验证私信（当群设置需要审批时）
     * <p>
     * 注意：
     * - 该方法会查询群成员并挑选出群主和管理员（role == GROUP_OWNER 或 role == MANAGER）
     * - 然后为每个管理员发送一条私聊消息，通知有新成员请求入群
     * - 你可以根据项目实际的消息体类型调整 IMessageDto.* 的使用
     */
    private void sendJoinApprovalRequestToAdmins(String groupId, String inviterId, String inviteeId, ImGroupPo groupPo) {
        // 获取群成员并挑选管理员（群主 + 管理员）
        List<ImGroupMemberPo> members = imGroupFeign.getGroupMemberList(groupId);

        List<String> adminIds = members.stream()
                .filter(m -> Objects.equals(m.getRole(), IMemberStatus.GROUP_OWNER.getCode())
                        || Objects.equals(m.getRole(), IMemberStatus.ADMIN.getCode()))
                .map(ImGroupMemberPo::getMemberId)
                .distinct()
                .collect(Collectors.toList());

        // 如果没找到管理员，兜底使用群 owner 字段
        if (adminIds.isEmpty() && StringUtils.hasText(groupPo.getOwnerId())) {
            adminIds = List.of(groupPo.getOwnerId());
        }

        // 获取 inviter / invitee 的用户信息（可用于消息展示）
        ImUserDataPo inviterInfo = imUserFeign.getOne(inviterId);
        ImUserDataPo inviteeInfo = imUserFeign.getOne(inviteeId);

        for (String adminId : adminIds) {
            IMSingleMessage singleMessage = IMSingleMessage.builder()
                    .messageTempId(UUID.randomUUID().toString())
                    .fromId(inviterId) // 可以使用 inviter 作为 fromId，也可以使用系统用户按需求
                    .toId(adminId)
                    // 这里复用一个业务类型（请根据项目的 IMessageContentType 添加合适类型）
                    .messageContentType(IMessageContentType.GROUP_JOIN_APPROVE.getCode())
                    .messageTime(DateTimeUtil.getCurrentUTCTimestamp())
                    .build();

            // 构造消息体（尽量包含必要字段，后端/前端可据此渲染审批 UI）
            IMessage.MessageBody body = new IMessage.GroupInviteMessageBody()
                    .setInviterId(inviterId)
                    .setGroupId(groupId)
                    .setUserId(inviteeId)
                    .setGroupAvatar(groupPo != null ? groupPo.getAvatar() : "")
                    .setGroupName(groupPo != null ? groupPo.getGroupName() : "")
                    .setInviterName(inviterInfo != null ? inviterInfo.getName() : "")
                    .setApproveStatus(0) // 0
                    ;

            // 如果有自定义字段可扩展（例如：setApplyUserId / setApplyUserName / setReason）
            try {
                // 尝试把 invitee info 注入到 messageBody（如果对应 DTO 支持）
                // 以下为示例，视 IMessageDto.GroupInviteMessageBody 实现而定：
                // ((IMessageDto.GroupInviteMessageBody) body).setApplyUserId(inviteeId).setApplyUserName(inviteeInfo != null ? inviteeInfo.getName() : "");
            } catch (Throwable ignore) {
            }

            singleMessage.setMessageBody(body);

            // 发送私信给管理员
            messageService.sendSingleMessage(singleMessage);
        }
    }

    /**
     * 发送群聊邀请消息给被邀请的用户
     *
     * @param groupId   群ID
     * @param inviterId 邀请者ID
     * @param invitees  被邀请者列表
     */
    private void sendGroupInviteMessages(String groupId, String inviterId, List<String> invitees) {
        try {
            // 获取群信息
            ImGroupPo groupInfo = imGroupFeign.getOneGroup(groupId);

            // 获取邀请者信息
            ImUserDataPo inviterInfo = imUserFeign.getOne(inviterId);

            // 构造邀请消息体
            for (String inviteeId : invitees) {
                // 构造单聊消息对象
                IMSingleMessage singleMessage = IMSingleMessage.builder()
                        .messageTempId(UUID.randomUUID().toString())
                        .fromId(inviterId)
                        .toId(inviteeId)
                        .messageContentType(8) // 群聊邀请消息类型
                        .messageTime(DateTimeUtil.getCurrentUTCTimestamp())
                        .build();

                // 构造群聊邀请消息内容
                IMessage.MessageBody messageBody = new IMessage.GroupInviteMessageBody()
                        .setInviterId(inviterId)
                        .setGroupId(groupId)
                        .setGroupAvatar(groupInfo != null ? groupInfo.getAvatar() : "")
                        .setGroupName(groupInfo != null ? groupInfo.getGroupName() : "默认群聊")
                        .setInviterName(inviterInfo != null ? inviterInfo.getName() : "")
                        .setApproveStatus(1);// 邀请状态 1-待处理


                singleMessage.setMessageBody(messageBody);

                // 发送单聊消息
                messageService.sendSingleMessage(singleMessage);
            }
        } catch (Exception e) {
            log.error("发送群聊邀请消息失败，groupId={}, inviterId={}, invitees={}", groupId, inviterId, invitees, e);
        }
    }
}