package com.xy.lucky.chat.domain.mapper;

import com.xy.lucky.chat.domain.dto.GroupMemberDto;
import com.xy.lucky.chat.domain.vo.GroupMemberVo;
import com.xy.lucky.domain.po.ImGroupMemberPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

/**
 * 群成员相关实体映射
 */
@Mapper(componentModel = "spring")
public interface GroupMemberBeanMapper {

    /**
     * ImGroupMemberPo -> GroupMemberVo
     */
    @Mappings({
            @Mapping(source = "memberId", target = "userId")
    })
    GroupMemberVo toGroupMemberVo(ImGroupMemberPo imGroupMemberPo);

    /**
     * GroupMemberDto -> ImGroupMemberPo
     */
    @Mappings({
            @Mapping(source = "userId", target = "memberId")
    })
    ImGroupMemberPo toImGroupMemberPo(GroupMemberDto groupMemberDto);

    /**
     * ImGroupMemberPo -> GroupMemberDto
     */
    @Mappings({
            @Mapping(source = "memberId", target = "userId")
    })
    GroupMemberDto toGroupMemberDto(ImGroupMemberPo imGroupMemberPo);

    /**
     * List<ImGroupMemberPo> -> List<GroupMemberVo>
     */
    List<GroupMemberVo> toGroupMemberVoList(List<ImGroupMemberPo> imGroupMemberPos);
}