package com.xy.domain.mapper;

import com.xy.domain.dto.GroupMemberDto;
import com.xy.domain.po.ImGroupMemberPo;
import com.xy.domain.vo.GroupMemberVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 群成员相关实体映射
 */
@Mapper
public interface GroupMemberBeanMapper {

    GroupMemberBeanMapper INSTANCE = Mappers.getMapper(GroupMemberBeanMapper.class);

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