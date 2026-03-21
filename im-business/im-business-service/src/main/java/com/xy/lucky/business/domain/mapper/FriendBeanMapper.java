package com.xy.lucky.business.domain.mapper;

import com.xy.lucky.business.domain.dto.FriendDto;
import com.xy.lucky.business.domain.vo.FriendVo;
import com.xy.lucky.domain.po.ImFriendshipPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

/**
 * 好友相关实体映射
 */
@Mapper(componentModel = "spring")
public interface FriendBeanMapper {

    /**
     * ImFriendshipPo -> FriendVo
     */
    @Mappings({
            @Mapping(source = "ownerId", target = "userId"),
            @Mapping(source = "toId", target = "friendId")
    })
    FriendVo toFriendVo(ImFriendshipPo imFriendshipPo);

    /**
     * FriendDto -> ImFriendshipPo
     */
    @Mappings({
            @Mapping(source = "fromId", target = "ownerId")
    })
    ImFriendshipPo toImFriendshipPo(FriendDto friendDto);

    /**
     * ImFriendshipPo -> FriendDto
     */
    @Mappings({
            @Mapping(source = "ownerId", target = "fromId")
    })
    FriendDto toFriendDto(ImFriendshipPo imFriendshipPo);

    /**
     * List<ImFriendshipPo> -> List<FriendVo>
     */
    List<FriendVo> toFriendVoList(List<ImFriendshipPo> imFriendshipPos);
}