package com.xy.lucky.domain.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 群邀请相关实体映射
 */
@Mapper
public interface GroupInviteBeanMapper {

    GroupInviteBeanMapper INSTANCE = Mappers.getMapper(GroupInviteBeanMapper.class);

    /**
     * GroupInviteDto -> ImGroupInviteRequestPo
     */
//    @Mappings({
//        @Mapping(source = "groupId", target = "groupId"),
//        @Mapping(source = "userId", target = "fromId"),
//        @Mapping(source = "memberIds", target = "toId"),
//        @Mapping(source = "message", target = "message"),
//        @Mapping(source = "addSource", target = "addSource"),
//        @Mapping(source = "approveStatus", target = "approveStatus")
//    })
//    ImGroupInviteRequestPo toImGroupInviteRequestPo(GroupInviteDto groupInviteDto);

    /**
     * ImGroupInviteRequestPo -> GroupInviteDto
     */
//    @Mappings({
//        @Mapping(source = "groupId", target = "groupId"),
//        @Mapping(source = "fromId", target = "userId"),
//        @Mapping(source = "toId", target = "memberIds"),
//        @Mapping(source = "message", target = "message"),
//        @Mapping(source = "addSource", target = "addSource"),
//        @Mapping(source = "approveStatus", target = "approveStatus")
//    })
//    GroupInviteDto toGroupInviteDto(ImGroupInviteRequestPo imGroupInviteRequestPo);
}