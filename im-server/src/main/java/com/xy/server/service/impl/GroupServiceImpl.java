package com.xy.server.service.impl;

import cn.hutool.core.util.IdUtil;
import com.xy.domain.dto.GroupDto;
import com.xy.domain.dto.GroupInviteDto;
import com.xy.domain.po.ImGroupMemberPo;
import com.xy.domain.po.ImGroupPo;
import com.xy.domain.po.ImUserDataPo;
import com.xy.domain.vo.GroupMemberVo;
import com.xy.imcore.enums.IMStatus;
import com.xy.imcore.enums.IMemberStatus;
import com.xy.imcore.enums.IMessageContentType;
import com.xy.imcore.enums.IMessageType;
import com.xy.imcore.model.IMGroupMessageDto;
import com.xy.imcore.model.IMessageDto;
import com.xy.response.domain.Result;
import com.xy.response.domain.ResultCode;
import com.xy.server.api.database.group.ImGroupFeign;
import com.xy.server.api.database.user.ImUserFeign;
import com.xy.server.exception.GlobalException;
import com.xy.server.service.FileService;
import com.xy.server.service.GroupService;
import com.xy.server.utils.RedisUtil;
import com.xy.utils.DateTimeUtil;
import com.xy.utils.GroupHeadImageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
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


    //
//
//    @Resource
//    private ImGroupMessageMapper imGroupMessageMapper;
//    @Resource
//    private ImGroupService imGroupService;
//
//    @Resource
//    private ImGroupMemberService imGroupMemberService;
//
//    @Resource
//    private ImUserDataMapper imUserDataMapper;
    @Resource
    private FileService fileService;


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
    @Transactional
    public Result inviteGroup(GroupInviteDto groupInviteDto) {

        Integer type = groupInviteDto.getType();

        // 新建群聊
        if (type.equals(IMessageType.SINGLE_MESSAGE.getCode())) {
            return createGroup(groupInviteDto);
        }

        //
        if (type.equals(IMessageType.GROUP_MESSAGE.getCode())) {
            return groupInvite(groupInviteDto);
        }
        return null;
    }

    public Result createGroup(GroupInviteDto groupInviteDto) {

        // 群成员id
        String groupId = UUID.randomUUID().toString();

        //
        String code = String.valueOf((int) ((Math.random() * 9 + 1) * Math.pow(10, 5)));

        // 邀请人
        String userId = groupInviteDto.getUserId();

        // 被邀请人
        List<String> friendIds = groupInviteDto.getMemberIds();

        // 加入时间
        Long joinTime = DateTimeUtil.getCurrentUTCTimestamp();

        // 保存群成员关系
        List<ImGroupMemberPo> imGroupMemberPoList = new ArrayList<>();

        // 邀请者默认为群主
        ImGroupMemberPo imGroupMemberPo = new ImGroupMemberPo().setGroupId(groupId)
                .setGroupMemberId(String.valueOf(IdUtil.getSnowflakeNextId()))
                .setMemberId(userId)
                .setRole(IMemberStatus.GROUP_OWNER.getCode())
                .setMute(IMStatus.YES.getCode())
                .setJoinTime(joinTime);

        imGroupMemberPoList.add(imGroupMemberPo);

        // 被邀请者默认为普通成员
        for (String friendId : friendIds) {
            ImGroupMemberPo groupMember = new ImGroupMemberPo();
            groupMember.setGroupId(groupId)
                    .setGroupMemberId(String.valueOf(IdUtil.getSnowflakeNextId()))
                    .setMemberId(friendId)
                    .setRole(IMemberStatus.NORMAL.getCode())
                    .setMute(IMStatus.YES.getCode())
                    .setJoinTime(joinTime);

            imGroupMemberPoList.add(groupMember);
        }

        // 批量插入成员信息
        if (imGroupFeign.groupMessageMemberBatchInsert(imGroupMemberPoList)) {
            log.info("群成员信息插入成功, 群聊id:{},用户id:{},新成员id:{}", groupId, userId, friendIds.toString());
        } else {
            log.error("群成员信息插入失败, 群聊id:{},用户id:{},新成员id:{}", groupId, userId, friendIds.toString());
        }

        // 保存群聊
        ImGroupPo imGroupPo = new ImGroupPo();
        imGroupPo.setGroupId(groupId)
                .setOwnerId(userId)
                .setGroupName("默认群聊-" + code)
                .setAvatar(generateGroupAvatar(groupId))
                .setStatus(IMStatus.YES.getCode())
                .setCreateTime(joinTime);

        if (imGroupFeign.insert(imGroupPo)) {
            log.info("群信息插入成功, 群聊id:{},用户id:{},新成员id:{}", groupId, userId, friendIds.toString());
        } else {
            log.error("群信息插入失败, 群聊id:{},用户id:{},新成员id:{}", groupId, userId, friendIds.toString());
        }

//        // 发送系统群聊邀请消息,系统消息默认用户000000
//        IMGroupMessageDto IMGroupMessageDto = (IMGroupMessageDto) systemMessage(groupId, "加入群聊,请尽情聊天吧");
//
//        // 发送群聊消息
//        //groupSend(IMGroupMessageDto);

        log.info("新建群聊，群聊id:{},用户id:{},新成员id:{}", groupId, userId, friendIds.toString());

        return Result.success(groupId);
    }


    /**
     * 系统消息
     *
     * @param groupId 群聊id
     * @param message 消息内容
     * @return
     */
    public IMGroupMessageDto systemMessage(String groupId, String message) {
        IMGroupMessageDto imGroupMessageDto = new IMGroupMessageDto();
        imGroupMessageDto.setGroupId(groupId)
                .setFromId("000000") // 系统消息默认用户000000
                .setMessageContentType(IMessageContentType.TIP.getCode()) // 系统消息
                .setMessageBody(new IMessageDto.TextMessageBody().setMessage(message)); // 群聊邀请消息
        return imGroupMessageDto;
    }


    /**
     * 群聊邀请
     *
     * @param groupInviteDto
     * @return
     */
    public Result groupInvite(GroupInviteDto groupInviteDto) {

        // 群id
        String groupId = groupInviteDto.getGroupId();

        if (!StringUtils.hasText(groupInviteDto.getGroupId())) {
            groupId = UUID.randomUUID().toString();
        }

        // 邀请人
        String userId = groupInviteDto.getUserId();

        // 被邀请人
        List<String> friendIds = groupInviteDto.getMemberIds();

        // 查询群成员列表并转换为 Set
        Set<String> memberIdSet = imGroupFeign.getGroupMemberList(groupId)
                .stream()
                .map(ImGroupMemberPo::getMemberId)
                .collect(Collectors.toSet());

        if (!memberIdSet.contains(userId)) {
            throw new GlobalException(ResultCode.FAIL, "用户不在该群组中，不可邀请");
        }

        // 过滤出不在群组中的新成员
        List<String> newMemberList = friendIds.stream()
                .filter(friendId -> !memberIdSet.contains(friendId))
                .collect(Collectors.toList());

        // 若有新成员，则添加到群组中
        if (!CollectionUtils.isEmpty(newMemberList)) {

            // 创建群成员信息
            List<ImGroupMemberPo> newGroupMemberList = createNewGroupMembers(groupId, newMemberList);

            if (imGroupFeign.groupMessageMemberBatchInsert(newGroupMemberList)) {

                log.info("群成员信息插入成功, 群聊id:{},用户id:{},新成员id:{}", groupId, userId, friendIds.toString());
            } else {

                log.error("群成员信息插入失败, 群聊id:{},用户id:{},新成员id:{}", groupId, userId, friendIds.toString());
            }

        }

        // 更新群信息
        ImGroupPo imGroupPo = new ImGroupPo();
        imGroupPo.setGroupId(groupId)
                .setAvatar(generateGroupAvatar(groupId));

        if (imGroupFeign.updateById(imGroupPo)) {

        }

        log.info("邀请成员，群聊id:{},用户id:{},新成员id:{}", groupId, userId, newMemberList.toString());

        return Result.success(groupId);
    }


    /**
     * 创建新成员信息集合
     *
     * @param groupId       群id
     * @param newMemberList 新成员用户id
     * @return 新成员信息集合
     */
    private List<ImGroupMemberPo> createNewGroupMembers(String groupId, List<String> newMemberList) {

        Long joinTime = new Date().getTime();

        return newMemberList.stream()
                .map(memberId -> {
                    ImGroupMemberPo imGroupMemberPo = new ImGroupMemberPo();
                    imGroupMemberPo.setGroupId(groupId); // 群聊id
                    imGroupMemberPo.setMemberId(memberId);  // 成员id
                    imGroupMemberPo.setRole(IMemberStatus.NORMAL.getCode()); // 成员角色
                    imGroupMemberPo.setMute(IMStatus.YES.getCode()); // 是否禁言
                    imGroupMemberPo.setJoinTime(joinTime); // 加入时间
                    return imGroupMemberPo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取群信息
     *
     * @param groupDto 群信息
     * @return
     */
    @Override
    public Result<?> groupInfo(GroupDto groupDto) {

        // 群聊id
        String groupId = groupDto.getGroupId();

        return Result.success(imGroupFeign.getOneGroup(groupId));
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

        // 上传文件
        return fileService.uploadFile(multipartFile);
    }

}
