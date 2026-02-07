package com.xy.lucky.chat.domain.mapper;

import com.xy.lucky.domain.po.ImGroupPo;
import com.xy.lucky.chat.domain.dto.GroupDto;
import com.xy.lucky.chat.domain.vo.GroupVo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 群组相关实体映射
 */
@Mapper
public interface GroupBeanMapper {

    GroupBeanMapper INSTANCE = Mappers.getMapper(GroupBeanMapper.class);

    /**
     * ImGroupPo -> GroupVo
     */
    GroupVo toGroupVo(ImGroupPo imGroupPo);

    /**
     * GroupDto -> ImGroupPo
     */
    ImGroupPo toImGroupPo(GroupDto groupDto);

    /**
     * ImGroupPo -> GroupDto
     */
    GroupDto toGroupDto(ImGroupPo imGroupPo);

    /**
     * List<ImGroupPo> -> List<GroupVo>
     */
    List<GroupVo> toGroupVoList(List<ImGroupPo> imGroupPos);
}